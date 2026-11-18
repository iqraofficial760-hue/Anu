package com.example.data

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class TermuxBridgeStatus(
    val connected: Boolean,
    val statusCode: Int = 0,
    val latencyMs: Long = 0L,
    val message: String
)

object TermuxBridgeManager {
    private const val PREFS_NAME = "anu_live_termux_bridge"
    private const val KEY_BASE_URL = "termux_base_url"
    const val DEFAULT_URL = "http://127.0.0.1:8080"

    const val CMD_LAUNCH_APP = "am start -n com.aistudio.partylive.vxqkrm/com.example.MainActivity"
    const val CMD_OPEN_PARTY = "am start -a android.intent.action.VIEW -d \"anulive://party\""
    const val CMD_OPEN_ME = "am start -a android.intent.action.VIEW -d \"anulive://me\""
    const val CMD_OPEN_TASKS = "am start -a android.intent.action.VIEW -d \"anulive://tasks\""
    const val CMD_LOGCAT = "adb logcat | grep -E \"com.example|AnuLive\""
    const val CMD_TERMUX_SERVER = "node -e \"require('http').createServer((q,s)=>{let b='';q.on('data',c=>b+=c);q.on('end',()=>{console.log(q.method,q.url,b);s.writeHead(200,{'Content-Type':'application/json'});s.end(JSON.stringify({ok:true,app:'ANU_LIVE_TERMUX'}));});}).listen(8080,'127.0.0.1',()=>console.log('ANU LIVE Termux Bridge listening on :8080'));\""

    fun getSavedUrl(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BASE_URL, DEFAULT_URL)?.ifBlank { DEFAULT_URL } ?: DEFAULT_URL
    }

    fun saveUrl(context: Context, url: String) {
        val normalized = normalizeUrl(url)
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_URL, normalized).apply()
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isBlank()) return DEFAULT_URL
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
    }

    fun openTermuxApp(context: Context): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.termux") ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }

    suspend fun testConnection(rawUrl: String): TermuxBridgeStatus = withContext(Dispatchers.IO) {
        val baseUrl = normalizeUrl(rawUrl)
        val startMs = System.currentTimeMillis()
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/health")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
            }
            val code = conn.responseCode
            val latency = System.currentTimeMillis() - startMs
            if (code in 200..299) {
                TermuxBridgeStatus(
                    connected = true,
                    statusCode = code,
                    latencyMs = latency,
                    message = "Termux Connected (HTTP $code • ${latency}ms)"
                )
            } else {
                TermuxBridgeStatus(
                    connected = false,
                    statusCode = code,
                    latencyMs = latency,
                    message = "Termux responded HTTP $code (${latency}ms)"
                )
            }
        } catch (e: Exception) {
            TermuxBridgeStatus(
                connected = false,
                message = "Not reachable at $baseUrl (${e.javaClass.simpleName}: ${e.localizedMessage ?: "start server in Termux"})"
            )
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun sendEvent(
        rawUrl: String,
        eventType: String,
        userId: Long,
        displayName: String,
        coinBalance: Long
    ): TermuxBridgeStatus = withContext(Dispatchers.IO) {
        val baseUrl = normalizeUrl(rawUrl)
        val startMs = System.currentTimeMillis()
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/event")
            val escapedName = displayName.replace("\\", "\\\\").replace("\"", "\\\"")
            val payload = """{"app":"ANU_LIVE","event":"$eventType","userId":$userId,"displayName":"$escapedName","coins":$coinBalance,"ts":${System.currentTimeMillis()}}"""
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("Content-Type", "application/json")
            }
            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload)
                writer.flush()
            }
            val code = conn.responseCode
            val latency = System.currentTimeMillis() - startMs
            if (code in 200..299) {
                TermuxBridgeStatus(
                    connected = true,
                    statusCode = code,
                    latencyMs = latency,
                    message = "Event '$eventType' sent to Termux (${latency}ms)"
                )
            } else {
                TermuxBridgeStatus(
                    connected = false,
                    statusCode = code,
                    latencyMs = latency,
                    message = "Termux returned HTTP $code"
                )
            }
        } catch (e: Exception) {
            TermuxBridgeStatus(
                connected = false,
                message = "Failed to send event to $baseUrl (${e.localizedMessage ?: "offline"})"
            )
        } finally {
            conn?.disconnect()
        }
    }
}
