package com.otpbox.data.crypto

import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.DateFormat
import java.util.Date

/**
 * Installs a last-resort uncaught-exception handler that appends the stack
 * trace to a local file (filesDir/crash_log.txt). Authenticator data lives in
 * an encrypted DB, so a silent crash during migration / sync / crypto is hard
 * to diagnose; this gives a readable trail without sending anything off-device.
 */
object CrashLogger {

    private const val FILE_NAME = "crash_log.txt"
    private const val MAX_BYTES = 200_000L

    fun install(application: android.app.Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(application, thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    @Synchronized
    private fun write(application: android.app.Application, thread: Thread, throwable: Throwable) {
        val file = File(application.filesDir, FILE_NAME)
        runCatching {
            if (file.exists() && file.length() > MAX_BYTES) {
                file.renameTo(File(application.filesDir, "${FILE_NAME}.old"))
            }
        }
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stamp = DateFormat.getDateTimeInstance().format(Date())
        file.appendText(
            """
            === $stamp | thread=${thread.name} | ${throwable.javaClass.name}: ${throwable.message}
            ${sw.toString()}
            """.trimIndent()
        )
        Log.e("CrashLogger", "uncaught exception logged", throwable)
    }
}
