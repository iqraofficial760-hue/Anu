package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.AppInstallerManager
import com.example.ui.theme.PartyGoldDim
import com.example.ui.theme.RoyalPlumSurface
import com.example.ui.theme.SoftLavender

@Composable
fun InstallUpdateDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var downloadUrl by remember { mutableStateOf(AppInstallerManager.getSavedDownloadUrl(context)) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(LuxuryObsidianBlack, Color(0xFF0A2419), LuxuryObsidianBlack)
                    )
                )
                .border(1.5.dp, LuxuryRoyalGold, RoundedCornerShape(22.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Install & Update",
                    style = MaterialTheme.typography.titleLarge,
                    color = LuxuryRoyalGold,
                    fontWeight = FontWeight.ExtraBold
                )
                LuxuryBadgePill(
                    text = AppInstallerManager.getAppVersionLabel(),
                    emeraldAccent = true
                )
            }

            Surface(
                color = RoyalPlumSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, PartyGoldDim, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "ANU LIVE Installer Center",
                        style = MaterialTheme.typography.labelLarge,
                        color = LuxuryEmeraldGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Package: ${AppInstallerManager.getApplicationId()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftLavender
                    )
                    Text(
                        text = "Paste your GitHub Releases / APK link below to download and install updates in one tap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }

            OutlinedTextField(
                value = downloadUrl,
                onValueChange = {
                    downloadUrl = it.take(300)
                    statusMessage = null
                },
                label = { Text("APK / GitHub Releases URL") },
                placeholder = { Text("https://github.com/user/anu-live/releases") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxuryEmeraldGreen,
                    unfocusedBorderColor = PartyGoldDim
                ),
                modifier = Modifier.fillMaxWidth().testTag("install_url_input")
            )

            Button(
                onClick = {
                    AppInstallerManager.saveDownloadUrl(context, downloadUrl)
                    val res = AppInstallerManager.openDownloadLink(context, downloadUrl)
                    statusMessage = if (res.isSuccess) {
                        "Opening installer download in browser..."
                    } else {
                        res.exceptionOrNull()?.message ?: "Unable to open download URL."
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuxuryEmeraldGreen,
                    contentColor = LuxuryObsidianBlack
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("install_download_button")
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download & Install Update", fontWeight = FontWeight.ExtraBold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        AppInstallerManager.saveDownloadUrl(context, downloadUrl)
                        AppInstallerManager.shareInstallLink(context, downloadUrl)
                    },
                    border = BorderStroke(1.dp, LuxuryRoyalGold),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("share_installer_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = LuxuryRoyalGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", color = LuxuryRoyalGold, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { AppInstallerManager.openAppSystemInfo(context) },
                    border = BorderStroke(1.dp, PartyGoldDim),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("app_info_button")
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("App Info", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            if (statusMessage != null) {
                Text(
                    text = statusMessage!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = LuxuryRoyalGold
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = SoftLavender, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
