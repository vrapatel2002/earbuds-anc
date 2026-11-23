package dev.vratik.anc

import android.content.Context
import android.content.SharedPreferences
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Opt-in live log stream so Claude can watch the app from chat.
 * Posts batched log lines + state changes to a random, per-install ntfy.sh topic.
 * Off by default. Read with: curl -s "https://ntfy.sh/<topic>/json?poll=1&since=10m"
 * Logs contain no serials, MACs or personal data — just protocol traffic and state.
 */
object LiveDebug {
    private lateinit var prefs: SharedPreferences
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = ArrayList<String>()

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled
    lateinit var topic: String
        private set

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("live_debug", Context.MODE_PRIVATE)
        topic = prefs.getString("topic", null) ?: ("budsanc-" + UUID.randomUUID().toString().replace("-", "").take(20))
            .also { prefs.edit().putString("topic", it).apply() }
        _enabled.value = prefs.getBoolean("enabled", false)

        scope.launch { // state snapshots
            Buds.ui.collect { if (_enabled.value) offer("STATE $it") } // StateFlow already dedupes
        }
        scope.launch { // flush every 2 s
            while (true) {
                delay(2000)
                flush()
            }
        }
    }

    fun setEnabled(on: Boolean) {
        prefs.edit().putBoolean("enabled", on).apply()
        _enabled.value = on
        if (on) offer("live debug ON — v${BuildConfig.VERSION_NAME}, ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE}")
    }

    fun offer(line: String) {
        if (!_enabled.value) return
        synchronized(queue) { queue += line }
    }

    private fun flush() {
        val batch = synchronized(queue) { queue.toList().also { queue.clear() } }
        if (batch.isEmpty()) return
        // ntfy messages cap at 4 KB — chunk.
        batch.joinToString("\n").chunked(3800).forEach { post(it) }
    }

    private fun post(body: String) {
        runCatching {
            val c = URL("https://ntfy.sh/$topic").openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.doOutput = true
            c.connectTimeout = 5000
            c.readTimeout = 5000
            c.outputStream.use { it.write(body.toByteArray()) }
            c.responseCode
            c.disconnect()
        }.onFailure { android.util.Log.d("BudsANC", "live debug post failed: ${it.message}") }
    }
}
