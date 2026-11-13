package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreSearchFilterTest {

    @Test
    fun searchStores_matchesByStoreName_caseInsensitive() {
        val results = PharmacyCatalog.searchStores("apollo")
        assertEquals(1, results.size)
        assertEquals("store_apollo_care", results.first().id)
    }

    @Test
    fun searchStores_matchesByKochiLocation() {
        val edappallyResults = PharmacyCatalog.searchStores("Edappally")
        assertEquals(1, edappallyResults.size)
        assertEquals("GreenCross 24x7 Medical", edappallyResults.first().name)

        val mgRoadResults = PharmacyCatalog.searchStores("MG Road")
        assertEquals(1, mgRoadResults.size)
        assertEquals("Apollo Care Pharmacy", mgRoadResults.first().name)

        val kakkanadResults = PharmacyCatalog.searchStores("kakkanad")
        assertEquals(1, kakkanadResults.size)
        assertEquals("MedPlus Express Chemist", kakkanadResults.first().name)
    }

    @Test
    fun searchStores_supportsMultiWordTokensAcrossNameAndLocation() {
        val results = PharmacyCatalog.searchStores("wellspring kaloor")
        assertEquals(1, results.size)
        assertEquals("store_wellspring_health", results.first().id)
    }

    @Test
    fun searchStores_returnsEmptyWhenNothingMatches() {
        val results = PharmacyCatalog.searchStores("trivandrum nonexistent")
        assertTrue(results.isEmpty())
    }

    @Test
    fun searchStores_blankQueryReturnsAllStores() {
        val results = PharmacyCatalog.searchStores("   ")
        assertEquals(PharmacyCatalog.stores.size, results.size)
    }
}
