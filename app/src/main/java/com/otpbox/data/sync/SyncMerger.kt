package com.otpbox.data.sync

import com.otpbox.domain.model.OtpEntry
import com.otpbox.domain.model.PasswordEntry

/**
 * Merges local and remote entry sets for GitHub sync. Union by id; for a given
 * id the entry with the greater updatedAt wins (a newer tombstone therefore
 * keeps a deleted entry deleted, preventing resurrection).
 */
object SyncMerger {

    fun merge(local: List<OtpEntry>, remote: List<OtpEntry>): List<OtpEntry> {
        val byId = HashMap<String, OtpEntry>(local.size + remote.size)
        for (entry in local) byId[entry.id] = entry
        for (entry in remote) {
            val existing = byId[entry.id]
            if (existing == null || entry.updatedAt > existing.updatedAt) {
                byId[entry.id] = entry
            }
        }
        return byId.values.sortedWith(
            compareBy({ it.sortOrder }, { it.issuer.lowercase() })
        )
    }

    fun merge(local: List<PasswordEntry>, remote: List<PasswordEntry>): List<PasswordEntry> {
        val byId = HashMap<String, PasswordEntry>(local.size + remote.size)
        for (e in local) byId[e.id] = e
        for (e in remote) {
            val existing = byId[e.id]
            if (existing == null || e.updatedAt > existing.updatedAt) byId[e.id] = e
        }
        return byId.values.sortedWith(compareBy({ it.sortOrder }, { it.title.lowercase() }))
    }
}
