package com.pixelgallery.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgallery.mobile.ui.theme.PixelAccentError
import com.pixelgallery.mobile.ui.theme.PixelBlueSecondary
import com.pixelgallery.mobile.ui.theme.PixelDarkSurface
import com.pixelgallery.mobile.ui.theme.PixelDarkSurfaceVariant
import com.pixelgallery.mobile.ui.theme.PixelTealPrimary
import com.pixelgallery.mobile.ui.theme.PixelTextPrimary
import com.pixelgallery.mobile.ui.theme.PixelTextSecondary
import com.pixelgallery.mobile.ui.theme.PixelTextTertiary
import com.pixelgallery.mobile.viewmodel.MobileUiState
import com.pixelgallery.shared.protocol.TransferConstants

@Composable
fun SettingsScreen(
    uiState: MobileUiState,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = PixelTextPrimary,
            fontWeight = FontWeight.Bold
        )

        // Transfer Protocol Settings
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PixelDarkSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Transfer Protocol",
                    style = MaterialTheme.typography.titleLarge,
                    color = PixelTextPrimary
                )

                SettingRow(
                    icon = Icons.Default.Speed,
                    title = "Lossless Transfer Engine",
                    subtitle = "Transfers exact bit-for-bit camera originals without downscaling"
                )

                SettingRow(
                    icon = Icons.Default.Lock,
                    title = "SHA-256 Verification",
                    subtitle = "Cryptographic integrity verified on both phone and watch"
                )

                SettingRow(
                    icon = Icons.Default.NetworkCheck,
                    title = "Transfer Port",
                    subtitle = "Port ${TransferConstants.DEFAULT_PORT} (Local Wi-Fi only, no cloud routing)"
                )
            }
        }

        // Active Connection Info
        if (uiState.connectedWatch != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PixelDarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Connected Watch",
                        style = MaterialTheme.typography.titleLarge,
                        color = PixelTextPrimary
                    )

                    SettingRow(
                        icon = Icons.Default.Watch,
                        title = uiState.connectedWatch.deviceName,
                        subtitle = "IP: ${uiState.connectedWatch.host}:${uiState.connectedWatch.port}"
                    )

                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(containerColor = PixelAccentError),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Disconnect Watch")
                    }
                }
            }
        }

        // About PixelGallery & Developer Credits
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PixelDarkSurface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PixelTealPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "About PixelGallery",
                        style = MaterialTheme.typography.titleLarge,
                        color = PixelTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "An open-source gallery for Wear OS, featuring wireless photo transfers, offline viewing, and advanced zoom controls.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelTextSecondary
                )

                HorizontalDivider(color = PixelDarkSurfaceVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Developer", style = MaterialTheme.typography.bodyMedium, color = PixelTextTertiary)
                    Text("nikolopikolo", style = MaterialTheme.typography.bodyMedium, color = PixelTealPrimary, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Platform", style = MaterialTheme.typography.bodyMedium, color = PixelTextTertiary)
                    Text("Android / Wear OS", style = MaterialTheme.typography.bodyMedium, color = PixelTextPrimary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Version", style = MaterialTheme.typography.bodyMedium, color = PixelTextTertiary)
                    Text("1.0.0", style = MaterialTheme.typography.bodyMedium, color = PixelTextPrimary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("License", style = MaterialTheme.typography.bodyMedium, color = PixelTextTertiary)
                    Text("MIT License", style = MaterialTheme.typography.bodyMedium, color = PixelTextPrimary)
                }

                // Clickable GitHub Link Button
                Button(
                    onClick = { uriHandler.openUri("https://github.com/nikolopikolo") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PixelDarkSurfaceVariant,
                        contentColor = PixelBlueSecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "GitHub: github.com/nikolopikolo",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PixelDarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PixelTealPrimary, modifier = Modifier.size(18.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = PixelTextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = PixelTextSecondary)
        }
    }
}
