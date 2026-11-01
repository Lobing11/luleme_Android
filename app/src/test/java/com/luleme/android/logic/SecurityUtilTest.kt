package com.luleme.android.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilTest {
    @Test
    fun hashPin_is_deterministic() {
        val salt = "test-salt"
        val a = hashPin("1234", salt)
        val b = hashPin("1234", salt)
        assertEquals(a, b)
        assertEquals(64, a.length)
    }

    @Test
    fun verifyPin_works() {
        val salt = randomSalt()
        val hash = hashPin("567890", salt)
        assertTrue(verifyPin("567890", salt, hash))
        assertFalse(verifyPin("000000", salt, hash))
    }

    @Test
    fun different_salts_differ() {
        assertNotEquals(hashPin("1234", "a"), hashPin("1234", "b"))
    }
}
