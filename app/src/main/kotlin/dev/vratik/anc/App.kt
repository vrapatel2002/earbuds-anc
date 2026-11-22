package dev.vratik.anc

import android.app.Application
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            // Saved crash is offered as a GitHub issue on next launch.
            runCatching {
                crashFile(this).writeText(
                    "version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
                        "device: ${android.os.Build.MODEL} / Android ${android.os.Build.VERSION.RELEASE}\n" +
                        "thread: ${thread.name}\n\n" + e.stackTraceToString() +
                        "\n\n--- last log lines ---\n" + AppLog.lines.value.takeLast(80).joinToString("\n")
                )
            }
            previous?.uncaughtException(thread, e)
        }
        AppLog.d("app start v${BuildConfig.VERSION_NAME}")
    }

    companion object {
        fun crashFile(app: Application) = File(app.filesDir, "last_crash.txt")
    }
}

/** In-memory ring log shown in the app and attached to crash reports. */
object AppLog {
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines

    @Synchronized
    fun d(msg: String) {
        android.util.Log.d("BudsANC", msg)
        _lines.value = (_lines.value + "${fmt.format(Date())} $msg").takeLast(500)
    }
}

fun ByteArray.hex() = joinToString(" ") { "%02x".format(it) }
