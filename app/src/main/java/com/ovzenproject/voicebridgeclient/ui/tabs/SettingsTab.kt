package com.ovzenproject.voicebridgeclient.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsTab(
    secureMode: Boolean,
    onSecureModeChange: (Boolean) -> Unit,
    sampleBits: Int,
    onSampleBitsChange: (Int) -> Unit,
    themeSetting: Int,
    onThemeChange: (Int) -> Unit,
    debugMode: Boolean,
    onDebugModeChange: (Boolean) -> Unit,
    onUpdateCertificate: () -> Unit,
    onLoadCertificateFromFile: () -> Unit,
    onResetSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔐 Безопасный режим")
                        Switch(
                            checked = secureMode,
                            onCheckedChange = onSecureModeChange
                        )
                    }
                    Text("Сертификат сервера:", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onUpdateCertificate,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📝 Ввести вручную")
                        }
                        FilledTonalButton(
                            onClick = onLoadCertificateFromFile,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📂 Загрузить из файла")
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🎛️ Битность аудио", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = sampleBits == 16,
                            onClick = { onSampleBitsChange(16) }
                        )
                        Text("16 бит", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = sampleBits == 24,
                            onClick = { onSampleBitsChange(24) }
                        )
                        Text("24 бита (Android 12+)", modifier = Modifier.padding(start = 8.dp))
                    }
                    Text("Изменение вступит в силу после переподключения.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🎨 Тема", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = themeSetting == 0,
                            onClick = { onThemeChange(0) }
                        )
                        Text("🌓 Системная", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = themeSetting == 1,
                            onClick = { onThemeChange(1) }
                        )
                        Text("☀️ Светлая", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = themeSetting == 2,
                            onClick = { onThemeChange(2) }
                        )
                        Text("🌙 Тёмная", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🐛 Режим отладки")
                        Switch(
                            checked = debugMode,
                            onCheckedChange = onDebugModeChange
                        )
                    }
                    Text("Отключите для повышения производительности.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onResetSettings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔄 Сбросить все настройки")
                    }
                }
            }
        }
    }
}