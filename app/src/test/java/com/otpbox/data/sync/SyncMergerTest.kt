package com.otpbox.data.sync

import com.otpbox.domain.model.OtpEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergerTest {

    private fun entry(id: String, updatedAt: Long, deleted: Boolean = false, issuer: String = "I") =
        OtpEntry(id = id, issuer = issuer, account = "a", secret = "JBSWY3DPEHPK3PXP",
            deleted = deleted, updatedAt = updatedAt)

    @Test
    fun unionOfDistinctIds() {
        val merged = SyncMerger.merge(
            local = listOf(entry("1", 10)),
            remote = listOf(entry("2", 10))
        )
        assertEquals(setOf("1", "2"), merged.map { it.id }.toSet())
    }

    @Test
    fun newerUpdatedAtWins() {
        val merged = SyncMerger.merge(
            local = listOf(entry("1", 10, issuer = "OLD")),
            remote = listOf(entry("1", 20, issuer = "NEW"))
        )
        assertEquals(1, merged.size)
        assertEquals("NEW", merged[0].issuer)
    }

    @Test
    fun olderRemoteDoesNotOverwriteLocal() {
        val merged = SyncMerger.merge(
            local = listOf(entry("1", 30, issuer = "LOCAL")),
            remote = listOf(entry("1", 20, issuer = "REMOTE"))
        )
        assertEquals("LOCAL", merged[0].issuer)
    }

    @Test
    fun newerTombstoneKeepsEntryDeleted() {
        val merged = SyncMerger.merge(
            local = listOf(entry("1", 10, deleted = false)),
            remote = listOf(entry("1", 20, deleted = true))
        )
        assertTrue(merged[0].deleted)
    }

    @Test
    fun newerLiveBeatsOldTombstone() {
        val merged = SyncMerger.merge(
            local = listOf(entry("1", 30, deleted = false)),
            remote = listOf(entry("1", 20, deleted = true))
        )
        assertFalse(merged[0].deleted)
    }
}
