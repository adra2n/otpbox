package com.otpbox.data.sync

import com.otpbox.data.backup.BackupContent
import com.otpbox.data.backup.BackupEncryptor
import com.otpbox.data.crypto.SecurePrefs
import com.otpbox.data.repo.OtpRepository
import com.otpbox.data.repo.PasswordRepository
import com.otpbox.domain.model.PasswordEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface SyncResult {
    data class Success(val message: String, val count: Int) : SyncResult
    data class Error(val message: String) : SyncResult
}

/**
 * Orchestrates manual push/pull of the encrypted backup to a GitHub gist.
 * The gist only ever stores the encrypted envelope.
 */
class SyncManager(
    private val api: GitHubApi,
    private val repository: OtpRepository,
    private val passwordRepository: PasswordRepository,
    private val encryptor: BackupEncryptor,
    private val securePrefs: SecurePrefs
) {

    private fun authHeader(pat: String) = "token $pat"

    suspend fun push(password: String): SyncResult = withContext(Dispatchers.IO) {
        val pat = securePrefs.githubPat
            ?: return@withContext SyncResult.Error("GitHub token not set")
        if (password.isBlank()) return@withContext SyncResult.Error("Backup password not set")

        try {
            val entries = repository.getAllIncludingDeleted()
            val passwords = passwordRepository.getAllIncludingDeleted()
            val envelope = encryptor.encrypt(
                BackupContent(entries = entries, passwords = passwords), password
            )
            val files = mapOf(GitHubApi.FILE_NAME to GistFile(envelope))
            val request = GistRequest(GitHubApi.DESCRIPTION, isPublic = false, files = files)

            val existingId = securePrefs.gistId
            val response = if (existingId.isNullOrBlank()) {
                api.createGist(authHeader(pat), request).also { securePrefs.gistId = it.id }
            } else {
                api.updateGist(authHeader(pat), existingId, request)
            }
            securePrefs.lastSyncAt = System.currentTimeMillis()
            SyncResult.Success(
                "Pushed ${entries.size} entries and ${passwords.size} passwords to gist ${response.id}",
                entries.size + passwords.size
            )
        } catch (e: Exception) {
            SyncResult.Error(mapError(e))
        }
    }

    suspend fun pull(password: String): SyncResult = withContext(Dispatchers.IO) {
        val pat = securePrefs.githubPat
            ?: return@withContext SyncResult.Error("GitHub token not set")
        val gistId = securePrefs.gistId
        if (gistId.isNullOrBlank()) return@withContext SyncResult.Error("No gist to pull; push first")
        if (password.isBlank()) return@withContext SyncResult.Error("Backup password not set")

        try {
            val gist = api.getGist(authHeader(pat), gistId)
            val content = gist.files[GitHubApi.FILE_NAME]?.content
                ?: return@withContext SyncResult.Error("Backup file not found in gist")
            val envelope = encryptor.decrypt(content, password)
            val remote = envelope.entries
            val remotePasswords = envelope.passwords
            val local = repository.getAllIncludingDeleted()
            val merged = SyncMerger.merge(local, remote)
            repository.replaceAll(merged)
            val localPasswords = passwordRepository.getAllIncludingDeleted()
            val mergedPasswords = SyncMerger.merge(localPasswords, remotePasswords)
            passwordRepository.replaceAll(mergedPasswords)
            securePrefs.lastSyncAt = System.currentTimeMillis()
            val active = merged.count { !it.deleted }
            val activePw = mergedPasswords.count { !it.deleted }
            SyncResult.Success(
                "Pulled and merged ($active active entries, $activePw active passwords)",
                active + activePw
            )
        } catch (e: BackupEncryptor.WrongPasswordException) {
            SyncResult.Error("Wrong backup password")
        } catch (e: Exception) {
            SyncResult.Error(mapError(e))
        }
    }

    private fun mapError(e: Exception): String {
        val msg = e.message ?: e.javaClass.simpleName
        return when {
            msg.contains("401") -> "Invalid GitHub token (401)"
            msg.contains("404") -> "Gist not found (404)"
            msg.contains("Unable to resolve host") || msg.contains("timeout") ->
                "Network error, check connection"
            else -> "Sync failed: $msg"
        }
    }
}
