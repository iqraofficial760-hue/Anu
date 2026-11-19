package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.BuildConfig

object AppInstallerManager {
    private const val PREFS_NAME = "anu_live_installer_prefs"
    private const val KEY_DOWNLOAD_URL = "apk_download_url"
    const val DEFAULT_PLACEHOLDER_URL = "https://github.com"

    fun getAppVersionLabel(): String = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"

    fun getApplicationId(): String = BuildConfig.APPLICATION_ID

    fun getSavedDownloadUrl(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DOWNLOAD_URL, "").orEmpty()
    }

    fun saveDownloadUrl(context: Context, rawUrl: String) {
        val trimmed = rawUrl.trim()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DOWNLOAD_URL, trimmed).apply()
    }

    /**
     * Play-policy compliant external download/update flow:
     * opens the user's APK/Releases/Store URL via Intent.ACTION_VIEW in the browser
     * without requiring the restricted REQUEST_INSTALL_PACKAGES permission.
     */
    fun openDownloadLink(context: Context, rawUrl: String): Result<Unit> {
        val trimmed = rawUrl.trim()
        val normalized = when {
            trimmed.isBlank() -> return Result.failure(IllegalArgumentException("Please enter your GitHub Releases or APK download URL first."))
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "https://$trimmed"
        }
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun shareInstallLink(context: Context, rawUrl: String) {
        val trimmed = rawUrl.trim()
        val shareText = if (trimmed.isNotBlank()) {
            "Join me on ANU LIVE! Download & install the latest version here: $trimmed"
        } else {
            "Join me on ANU LIVE (${getAppVersionLabel()}) — Live Streaming, Multi-Seat Party Rooms & Luxury Gifts!"
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Install ANU LIVE")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(sendIntent, "Share ANU LIVE Installer").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun openAppSystemInfo(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
