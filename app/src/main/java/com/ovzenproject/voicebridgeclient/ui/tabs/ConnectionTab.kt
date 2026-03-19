package com.ovzenproject.voicebridgeclient.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ovzenproject.voicebridgeclient.ui.components.CustomPasswordVisualTransformation

@Composable
fun ConnectionTab(
    serverIp: String,
    onServerIpChange: (String) -> Unit,
    serverPort: String,
    onServerPortChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isConnected: Boolean,
    permissionDenied: Boolean,
    muteEnabled: Boolean,
    onMuteChange: (Boolean) -> Unit,
    onConnectClick: () -> Unit,
    volumeLevel: Float = 0f,
    latency: Long = 0,
    onShowHistory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = serverIp,
                    onValueChange = onServerIpChange,
                    label = { Text("🌐 IP сервера") },
                    modifier = Modifier.weight(1f),
                    enabled = !isConnected
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onShowHistory,
                    enabled = !isConnected
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "История серверов")
                }
            }
        }
        item {
            OutlinedTextField(
                value = serverPort,
                onValueChange = onServerPortChange,
                label = { Text("🔌 Порт") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isConnected
            )
        }
        item {
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("🔑 Пароль") },
                visualTransformation = CustomPasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isConnected
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (muteEnabled) "🔇 Микрофон выключен" else "🎤 Микрофон включён")
                        Switch(
                            checked = muteEnabled,
                            onCheckedChange = onMuteChange
                        )
                    }
                    if (isConnected && !muteEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Уровень громкости:", style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(
                            progress = { volumeLevel.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                    }
                    if (isConnected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Задержка: ${latency} мс", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            FilledTonalButton(
                onClick = onConnectClick,
                enabled = !permissionDenied,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(if (isConnected) "❌ Отключиться" else "✅ Подключиться")
            }
        }
    }
}