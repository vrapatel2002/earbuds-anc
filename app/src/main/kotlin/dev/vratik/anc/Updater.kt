package dev.vratik.anc

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Self-updater. CI publishes a GitHub Release per `vX.Y.Z` tag with the signed APK attached.
 * Same applicationId + same signing key + higher versionCode = in-place update, data kept.
 * NOTE: needs the repo (or a releases repo) to be public — the app has no GitHub token.
 */
object Updater {
    data class Release(val version: String, val notes: String, val apkUrl: String)

    suspend fun latest(): Result<Release> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest")
                .openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode == 404) error("No release found (repo private or nothing published yet)")
            if (conn.responseCode != 200) error("GitHub returned ${conn.responseCode}")
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val assets = json.getJSONArray("assets")
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                .first { it.getString("name").endsWith(".apk") }
            Release(
                version = json.getString("tag_name").removePrefix("v"),
                notes = json.optString("body", ""),
                apkUrl = apk.getString("browser_download_url"),
            )
        }
    }

    fun isNewer(remote: String, local: String = BuildConfig.VERSION_NAME.substringBefore("-")): Boolean {
        fun parts(v: String) = v.split(".").map { it.toIntOrNull() ?: 0 } + listOf(0, 0, 0)
        val r = parts(remote); val l = parts(local)
        for (i in 0 until 3) if (r[i] != l[i]) return r[i] > l[i]
        return false
    }

    /** Downloads the APK and hands it to PackageInstaller. Android shows one confirm tap if required. */
    suspend fun install(ctx: Context, rel: Release) = withContext(Dispatchers.IO) {
        AppLog.d("downloading ${rel.version}")
        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            URL(rel.apkUrl).openStream().use { input ->
                session.openWrite("app.apk", 0, -1).use { out ->
                    input.copyTo(out)
                    session.fsync(out)
                }
            }
            val pi = PendingIntent.getBroadcast(
                ctx, id, Intent(ctx, UpdateReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(pi.intentSender)
        }
        AppLog.d("install session $id committed")
    }
}

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)
        AppLog.d("install status $status ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""}")
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @Suppress("DEPRECATION")
            val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
            ctx.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
