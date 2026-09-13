package com.memory.app.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.memory.app.navigation.Screen
import com.memory.app.ui.contacts.ContactsScreen
import com.memory.app.ui.contacts.ContactsViewModel
import com.memory.app.ui.keypad.KeypadScreen
import com.memory.app.ui.keypad.KeypadViewModel
import com.memory.app.ui.memory.ManualImportScreen
import com.memory.app.ui.memory.RecordingDetailScreen
import com.memory.app.ui.memory.RecordingDiagnosticsScreen
import com.memory.app.ui.memory.RecordingsScreen
import com.memory.app.ui.memory.RecordingsViewModel
import com.memory.app.ui.recents.CallDetailScreen
import com.memory.app.ui.recents.RecentsScreen
import com.memory.app.ui.recents.RecentsViewModel
import com.memory.app.ui.settings.SettingsScreen

enum class BottomTab(
    val title: String,
    val icon: ImageVector,
    val screen: Screen
) {
    RECENTS("Recents", Icons.Rounded.History, Screen.Recents),
    CONTACTS("Contacts", Icons.Rounded.Contacts, Screen.Contacts),
    KEYPAD("Keypad", Icons.Rounded.Dialpad, Screen.Keypad),
    MEMORY("Memory", Icons.Rounded.Psychology, Screen.Memory)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val backStack = rememberNavBackStack(Screen.Recents)
    val currentScreen = backStack.lastOrNull() ?: Screen.Recents

    val recentsViewModel: RecentsViewModel = hiltViewModel()
    val contactsViewModel: ContactsViewModel = hiltViewModel()
    val keypadViewModel: KeypadViewModel = hiltViewModel()
    val recordingsViewModel: RecordingsViewModel = hiltViewModel()

    val isBottomTabScreen = currentScreen in listOf(
        Screen.Recents,
        Screen.Contacts,
        Screen.Keypad,
        Screen.Memory
    )

    Scaffold(
        topBar = {
            if (isBottomTabScreen) {
                TopAppBar(
                    title = {
                        val title = when (currentScreen) {
                            Screen.Recents -> "Recents"
                            Screen.Contacts -> "Contacts"
                            Screen.Keypad -> "Keypad"
                            Screen.Memory -> "Memory"
                            else -> "Memory"
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { backStack.add(Screen.Settings) }) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isBottomTabScreen) {
                NavigationBar {
                    BottomTab.entries.forEach { tab ->
                        val selected = currentScreen == tab.screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    backStack.clear()
                                    backStack.add(tab.screen)
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            entryProvider = entryProvider {
                entry<Screen.Recents> {
                    RecentsScreen(
                        viewModel = recentsViewModel,
                        onCallDetailClick = { callId ->
                            backStack.add(Screen.CallDetail(callId))
                        }
                    )
                }
                entry<Screen.Contacts> {
                    ContactsScreen(
                        viewModel = contactsViewModel
                    )
                }
                entry<Screen.Keypad> {
                    KeypadScreen(
                        viewModel = keypadViewModel
                    )
                }
                entry<Screen.Memory> {
                    RecordingsScreen(
                        viewModel = recordingsViewModel,
                        onRecordingClick = { recordingId ->
                            backStack.add(Screen.RecordingDetail(recordingId))
                        },
                        onImportClick = {
                            backStack.add(Screen.ManualImport)
                        }
                    )
                }
                entry<Screen.Settings> {
                    SettingsScreen(
                        onBackClick = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onDiagnosticsClick = { backStack.add(Screen.RecordingDiagnostics) },
                        viewModel = recordingsViewModel
                    )
                }
                entry<Screen.CallDetail> { route ->
                    CallDetailScreen(
                        callId = route.callId,
                        viewModel = recentsViewModel,
                        recordingsViewModel = recordingsViewModel,
                        onBackClick = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onRecordingClick = { recordingId ->
                            backStack.add(Screen.RecordingDetail(recordingId))
                        },
                        onImportClick = {
                            backStack.add(Screen.ManualImport)
                        }
                    )
                }
                entry<Screen.RecordingDetail> { route ->
                    RecordingDetailScreen(
                        recordingId = route.recordingId,
                        viewModel = recordingsViewModel,
                        onBackClick = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }
                entry<Screen.ManualImport> {
                    ManualImportScreen(
                        viewModel = recordingsViewModel,
                        onBackClick = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }
                entry<Screen.RecordingDiagnostics> {
                    RecordingDiagnosticsScreen(
                        viewModel = recordingsViewModel,
                        onBackClick = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }
            }
        )
    }
}
