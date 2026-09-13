package com.memory.app.ui.keypad

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class KeypadViewModelTest {

    private lateinit var viewModel: KeypadViewModel

    @Before
    fun setUp() {
        viewModel = KeypadViewModel()
    }

    @Test
    fun appendDigit_updatesPhoneNumber() {
        viewModel.appendDigit("1")
        viewModel.appendDigit("2")
        viewModel.appendDigit("3")

        assertEquals("123", viewModel.phoneNumber.value)
    }

    @Test
    fun deleteLastDigit_removesLastCharacter() {
        viewModel.appendDigit("5")
        viewModel.appendDigit("5")
        viewModel.appendDigit("5")
        viewModel.deleteLastDigit()

        assertEquals("55", viewModel.phoneNumber.value)
    }

    @Test
    fun clear_resetsPhoneNumber() {
        viewModel.appendDigit("9")
        viewModel.appendDigit("1")
        viewModel.appendDigit("1")
        viewModel.clear()

        assertEquals("", viewModel.phoneNumber.value)
    }
}
