package com.pixelgallery.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixelgallery.mobile.ui.theme.PixelDarkBackground
import com.pixelgallery.mobile.ui.theme.PixelDarkSurface
import com.pixelgallery.mobile.ui.theme.PixelDarkSurfaceHigh
import com.pixelgallery.mobile.ui.theme.PixelDarkSurfaceVariant
import com.pixelgallery.mobile.ui.theme.PixelTealPrimary
import com.pixelgallery.mobile.ui.theme.PixelTextPrimary
import com.pixelgallery.mobile.ui.theme.PixelTextSecondary
import com.pixelgallery.mobile.ui.theme.PixelTextTertiary
import com.pixelgallery.shared.model.WatchDeviceInfo
import com.pixelgallery.shared.protocol.TransferConstants

@Composable
fun DeviceDiscoveryDialog(
    isOpen: Boolean,
    isDiscovering: Boolean,
    devices: List<WatchDeviceInfo>,
    isConnecting: Boolean,
    onConnect: (WatchDeviceInfo) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Close") }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nearby Pixel Watches", style = MaterialTheme.typography.titleLarge)
                if (isDiscovering) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PixelTealPrimary)
                } else {
                    IconButtonCard(
                        icon = Icons.Default.Refresh,
                        contentDescription = "Scan again",
                        onClick = onRefresh
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ensure your Pixel Watch 3 is connected to the same Wi-Fi network and has PixelGallery open.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelTextSecondary
                )

                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = PixelTextTertiary, modifier = Modifier.size(36.dp))
                            Text("Searching for Pixel Watch on Wi-Fi...", style = MaterialTheme.typography.bodyMedium, color = PixelTextTertiary)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(devices) { device ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = PixelDarkSurfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(PixelTealPrimary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Watch,
                                                contentDescription = null,
                                                tint = PixelTealPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f, fill = false)) {
                                            Text(
                                                text = device.deviceName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${device.host}:${device.port}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = PixelTextSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { onConnect(device) },
                                        enabled = !isConnecting,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PixelTealPrimary,
                                            contentColor = Color.Black
                                        ),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "Connect",
                                            maxLines = 1,
                                            softWrap = false,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = PixelDarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ManualConnectDialog(
    isOpen: Boolean,
    isConnecting: Boolean,
    onConnectManual: (host: String, port: String, pin: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    var ipAddress by remember { mutableStateOf("192.168.1.") }
    var port by remember { mutableStateOf(TransferConstants.DEFAULT_PORT.toString()) }
    var pin by remember { mutableStateOf(TransferConstants.DEFAULT_PAIRING_PIN) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual IP Connection", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Enter the IP address shown on your Pixel Watch 3 screen:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelTextSecondary
                )

                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("Watch IP Address") },
                    placeholder = { Text("192.168.1.150") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("Pairing PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConnectManual(ipAddress, port, pin) },
                enabled = ipAddress.isNotBlank() && !isConnecting,
                colors = ButtonDefaults.buttonColors(containerColor = PixelTealPrimary, contentColor = Color.Black)
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                } else {
                    Text("Connect")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = PixelDarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}
