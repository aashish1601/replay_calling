require('dotenv').config();
const express = require('express');
const cors = require('cors');
const twilio = require('twilio');

const app = express();
const port = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Twilio Client Setup
const accountSid = process.env.TWILIO_ACCOUNT_SID;
const authToken = process.env.TWILIO_AUTH_TOKEN;
const twilioPhoneNumber = process.env.TWILIO_PHONE_NUMBER;
const client = twilio(accountSid, authToken);

// Root endpoint for health check
app.get('/', (req, res) => {
  res.send('Replay Calling Twilio Backend is running! 🚀');
});

/**
 * 1. Start the call process.
 * Android app calls this endpoint when user taps "Record & Call".
 */
app.post('/calls/start', async (req, res) => {
  try {
    const { userPhone, contactPhone, userId } = req.body;

    if (!userPhone || !contactPhone) {
      return res.status(400).json({ success: false, message: 'userPhone and contactPhone are required' });
    }

    console.log(`Starting call bridge: Twilio will call User (${userPhone}), then bridge to Contact (${contactPhone})`);

    // The base URL of your deployed server (set this in Railway environment variables)
    // If not set, it will attempt to use the host header, which works best in production environments.
    const baseUrl = process.env.BASE_URL || `https://${req.headers.host}`;
    
    // We pass the contactPhone and userPhone as query parameters to the bridge webhook
    const bridgeUrl = `${baseUrl}/twiml/bridge?contactPhone=${encodeURIComponent(contactPhone)}&userPhone=${encodeURIComponent(userPhone)}`;

    // 1. Tell Twilio to call the App User first.
    const call = await client.calls.create({
      url: bridgeUrl,
      to: userPhone,
      from: twilioPhoneNumber,
      statusCallback: `${baseUrl}/calls/status`,
      statusCallbackEvent: ['completed'],
      statusCallbackMethod: 'POST'
    });

    res.json({ 
      success: true, 
      callSid: call.sid,
      message: 'Call initiated. Twilio is calling the user now.'
    });

  } catch (error) {
    console.error('Error starting call:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

/**
 * 2. Bridge the call to the contact.
 * Twilio hits this webhook when the App User answers their phone.
 */
app.post('/twiml/bridge', (req, res) => {
  const contactPhone = req.query.contactPhone;
  let userPhone = req.query.userPhone;

  // Sometimes Express decodes '+' as a space if not perfectly encoded by the client, so we fix it:
  if (userPhone && userPhone.startsWith(' ')) {
    userPhone = '+' + userPhone.trim();
  }

  console.log(`User answered. Generating TwiML to dial Contact (${contactPhone}) with Caller ID (${userPhone}) and record.`);

  const VoiceResponse = twilio.twiml.VoiceResponse;
  const response = new VoiceResponse();

  // Dial the contact and start recording as soon as they answer
  const dial = response.dial({
    record: 'record-from-answer',
    callerId: userPhone || twilioPhoneNumber 
  });
  dial.number(contactPhone);

  res.type('text/xml');
  res.send(response.toString());
});

/**
 * 3. Status Callback
 * Twilio hits this when the call completely ends.
 */
app.post('/calls/status', (req, res) => {
  const { CallSid, CallStatus, RecordingUrl, RecordingDuration } = req.body;
  
  console.log(`Call ${CallSid} ended with status: ${CallStatus}`);
  
  if (RecordingUrl) {
    console.log(`Recording available at: ${RecordingUrl}`);
    console.log(`Recording duration: ${RecordingDuration} seconds`);
    // Here you could save the RecordingUrl to a database, 
    // or send it to Deepgram for AI transcription!
  }

  res.sendStatus(200);
});

app.listen(port, () => {
  console.log(`Server listening on port ${port}`);
});
