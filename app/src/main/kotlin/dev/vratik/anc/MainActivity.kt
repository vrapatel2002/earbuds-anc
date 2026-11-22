package dev.vratik.anc

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vratik.anc.proto.AncMode
import dev.vratik.anc.proto.Battery
import dev.vratik.anc.proto.ListeningMode
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) { Screen() }
            }
        }
    }

    @Composable
    private fun Screen() {
        val ui by Buds.ui.collectAsStateWithLifecycle()
        val perms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
            if (r[Manifest.permission.BLUETOOTH_CONNECT] == true) BudsService.start(this)
            else AppLog.d("Bluetooth permission denied")
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Buds ANC", style = MaterialTheme.typography.headlineMedium)
            CrashCard()

            Section(ui.deviceName ?: "CMF Buds Pro 2") {
                Text(ui.status)
                if (ui.model != null || ui.firmware != null) Text("Model ${ui.model ?: "?"} · FW ${ui.firmware ?: "?"}")
                Text("L ${bat(ui.battery.left)}   R ${bat(ui.battery.right)}   Case ${bat(ui.battery.case)}")
                ui.wear?.let { Text("In ear: L ${if (it.leftInEar) "yes" else "no"} · R ${if (it.rightInEar) "yes" else "no"}") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!ui.connected) Button(onClick = {
                        perms.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS))
                    }) { Text("Connect") }
                    else OutlinedButton(onClick = { Buds.refresh() }) { Text("Refresh") }
                }
            }

            Section("Noise control") {
                Chips(AncMode.entries.filter { it.wire != null }, ui.anc, ui.connected) { Buds.setAnc(it) }
                Text("Adaptive: waiting on a Bluetooth capture to learn its code.", style = MaterialTheme.typography.bodySmall)
            }

            Section("EQ preset") {
                Chips(ListeningMode.entries.filter { it != ListeningMode.CUSTOM }, ui.listening, ui.connected) { Buds.setListening(it) }
            }

            UpdateCard()
            LogCard()
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun <T : Enum<T>> Chips(items: List<T>, selected: T?, enabled: Boolean, onPick: (T) -> Unit) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { m ->
                FilterChip(
                    selected = m == selected,
                    enabled = enabled,
                    onClick = { onPick(m) },
                    label = { Text(m.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                )
            }
        }
    }

    @Composable
    private fun UpdateCard() {
        val scope = rememberCoroutineScope()
        var msg by remember { mutableStateOf("Version ${BuildConfig.VERSION_NAME}") }
        var pending by remember { mutableStateOf<Updater.Release?>(null) }
        Section("Updates") {
            Text(msg)
            pending?.let { if (it.notes.isNotBlank()) Text(it.notes.take(600), style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    msg = "Checking…"
                    scope.launch {
                        Updater.latest().onSuccess { r ->
                            if (Updater.isNewer(r.version)) { pending = r; msg = "Update ${r.version} available" }
                            else msg = "Up to date (${BuildConfig.VERSION_NAME})"
                        }.onFailure { msg = "Update check failed: ${it.message}" }
                    }
                }) { Text("Check for updates") }
                pending?.let { r ->
                    Button(onClick = {
                        if (!packageManager.canRequestPackageInstalls()) {
                            startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                            msg = "Allow installs from this app, then tap Update again"
                        } else {
                            msg = "Downloading ${r.version}…"
                            scope.launch { runCatching { Updater.install(this@MainActivity, r) }.onFailure { msg = "Install failed: ${it.message}" } }
                        }
                    }) { Text("Update") }
                }
            }
        }
    }

    @Composable
    private fun CrashCard() {
        val file = App.crashFile(application)
        var visible by remember { mutableStateOf(file.exists()) }
        if (!visible) return
        Section("The app crashed last time") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val body = file.readText().take(6000)
                    val url = "https://github.com/${BuildConfig.UPDATE_REPO}/issues/new?labels=crash" +
                        "&title=" + Uri.encode("Crash in ${BuildConfig.VERSION_NAME}") +
                        "&body=" + Uri.encode("```\n$body\n```")
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    file.delete(); visible = false
                }) { Text("Report on GitHub") }
                OutlinedButton(onClick = { file.delete(); visible = false }) { Text("Dismiss") }
            }
        }
    }

    @Composable
    private fun LogCard() {
        val lines by AppLog.lines.collectAsStateWithLifecycle()
        Section("Log") {
            OutlinedButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "Buds ANC ${BuildConfig.VERSION_NAME}\n" + lines.joinToString("\n"))
                startActivity(Intent.createChooser(send, "Export log"))
            }) { Text("Export log") }
            Text(lines.takeLast(40).joinToString("\n"), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
    }

    @Composable
    private fun Section(title: String, content: @Composable () -> Unit) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                content()
            }
        }
    }

    private fun bat(b: Battery?) = b?.let { "${it.percent}%${if (it.charging) "⚡" else ""}" } ?: "—"
}
