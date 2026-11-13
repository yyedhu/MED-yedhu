package com.example.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberFormatterTest {

    @Test
    fun `sanitizeLocalNumber strips pasted 91 or leading zero for India`() {
        assertEquals("9876543210", PhoneNumberFormatter.sanitizeLocalNumber("+91 98765 43210", "+91"))
        assertEquals("9876543210", PhoneNumberFormatter.sanitizeLocalNumber("09876543210", "+91"))
        assertEquals("9876543210", PhoneNumberFormatter.sanitizeLocalNumber("98765-43210", "+91"))
    }

    @Test
    fun `formatToE164 produces clean E164 phone number`() {
        assertEquals("+919876543210", PhoneNumberFormatter.formatToE164("+91", "9876543210"))
        assertEquals("+919876543210", PhoneNumberFormatter.formatToE164("91", "09876543210"))
        assertEquals("+14155551234", PhoneNumberFormatter.formatToE164("+1", "4155551234"))
    }

    @Test
    fun `isValidMobileNumber validates Indian 10 digit and international numbers`() {
        assertTrue(PhoneNumberFormatter.isValidMobileNumber("+91", "9876543210"))
        assertFalse(PhoneNumberFormatter.isValidMobileNumber("+91", "98765"))
        assertFalse(PhoneNumberFormatter.isValidMobileNumber("+91", ""))
        assertTrue(PhoneNumberFormatter.isValidMobileNumber("+1", "4155551234"))
        assertFalse(PhoneNumberFormatter.isValidMobileNumber("+1", "123"))
    }

    @Test
    fun `isValidOtpCode requires exact 6 digits`() {
        assertTrue(PhoneNumberFormatter.isValidOtpCode("123456"))
        assertTrue(PhoneNumberFormatter.isValidOtpCode(" 004291 "))
        assertFalse(PhoneNumberFormatter.isValidOtpCode("12345"))
        assertFalse(PhoneNumberFormatter.isValidOtpCode("1234567"))
        assertFalse(PhoneNumberFormatter.isValidOtpCode("12ab56"))
    }

    @Test
    fun `maskE164 obscures middle digits`() {
        assertEquals("+91 98••••3210", PhoneNumberFormatter.maskE164("+919876543210"))
        assertEquals("+141••••1234", PhoneNumberFormatter.maskE164("+14155551234"))
    }
}
