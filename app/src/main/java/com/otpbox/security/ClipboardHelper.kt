package com.otpbox.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle

/**
 * Copies sensitive text to the clipboard with two protections:
 *  - On Android 13+ the clip is flagged EXTRA_IS_SENSITIVE so it is excluded
 *    from clipboard history / previews.
 *  - After 30s the clipboard is cleared, but ONLY if it still holds exactly
 *    the value we copied (so we never wipe something the user copied later).
 */
object ClipboardHelper {

    private const val CLEAR_DELAY_MS = 30_000L

    fun copySecret(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText(label, text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        cm.setPrimaryClip(clip)
        scheduleClear(context, text)
    }

    fun copyPlain(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun scheduleClear(context: Context, expected: String) {
        Handler(Looper.getMainLooper()).postDelayed({
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return@postDelayed
            val current = cm.primaryClip?.getItemAt(0)?.text?.toString()
            if (current == expected) cm.clearPrimaryClip()
        }, CLEAR_DELAY_MS)
    }
}
