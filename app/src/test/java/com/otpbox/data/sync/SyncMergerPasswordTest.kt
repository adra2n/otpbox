package com.otpbox.data.sync

import com.otpbox.domain.model.PasswordEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergerPasswordTest {

    private fun entry(id: String, updatedAt: Long, deleted: Boolean = false, title: String = "T") =
        PasswordEntry(
            id = id,
            title = title,
            username = "u",
            password = "p",
            deleted = deleted,
            updatedAt = updatedAt
        )

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
            local = listOf(entry("1", 10, title = "OLD")),
            remote = listOf(entry("1", 20, title = "NEW"))
        )
        assertEquals(1, merged.size)
        assertEquals("NEW", merged[0].title)
    }

    @Test
    fun olderRemoteDoesNotOverwriteLocal() {
        val merged = SyncMerger.merge(
            local = listOf(entry("1", 30, title = "LOCAL")),
            remote = listOf(entry("1", 20, title = "REMOTE"))
        )
        assertEquals("LOCAL", merged[0].title)
    }

    @Test
    fun newerTombstoneKeepsEntryDeleted() {
        val merged = SyncMerger.merge(
            local = listOf(entry("1", 10, deleted = false)),
            remote = listOf(entry("1", 20, deleted = true))
        )
        assertTrue(merged[0].deleted)
    }
}
