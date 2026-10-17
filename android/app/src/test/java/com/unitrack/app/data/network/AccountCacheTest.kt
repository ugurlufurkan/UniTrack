package com.unitrack.app.data.network

import com.unitrack.app.data.session.readThroughAccountCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class AccountCacheTest {
    @Test fun cachedAcademicDataIsNeverSharedBetweenAccounts() = runBlocking {
        var owner = "first-student"
        val values = mutableMapOf<String, String>()
        suspend fun read(network: suspend () -> String) = readThroughAccountCache("courses", { owner }, network, { values[it] }, { key, value -> values[key] = value })
        assertEquals("private courses", read { "private courses" })
        assertEquals("private courses", read { throw IOException("offline") })
        owner = "second-student"
        try { read { throw IOException("offline") }; fail("another student's cache must not be returned") }
        catch (_: IOException) { }
        assertEquals("second student's courses", read { "second student's courses" })
        owner = "first-student"
        assertEquals("private courses", read { throw IOException("offline") })
    }

    @Test fun delayedNetworkResultsCannotPopulateANewerAccountsCache() = runBlocking {
        var owner: String? = "first-student"
        val values = mutableMapOf<String, String>()
        try {
            readThroughAccountCache("courses", { owner }, { owner = "second-student"; "old private courses" }, { values[it] }, { key, value -> values[key] = value })
            fail("an account change must cancel delivery of the old response")
        } catch (_: CancellationException) { }
        assertTrue(values.isEmpty())
    }
}
