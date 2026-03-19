package com.ovzenproject.voicebridgeclient.ui.tabs
import com.ovzenproject.voicebridgeclient.models.AdminState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdminTab(
    isConnected: Boolean,
    isAuthenticated: Boolean,
    loginMessage: String,
    adminPassword: String,
    onAdminPasswordChange: (String) -> Unit,
    onAdminLogin: () -> Unit,
    clientList: List<Pair<String, String>>,
    onBanClient: (String) -> Unit,
    onRefreshClients: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isConnected) {
            item {
                Text("⚠️ Для администрирования необходимо подключиться к серверу.", color = MaterialTheme.colorScheme.error)
            }
        }
        if (!isAuthenticated) {
            item {
                OutlinedTextField(
                    value = adminPassword,
                    onValueChange = onAdminPasswordChange,
                    label = { Text("🔑 Пароль администратора") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isConnected
                )
            }
            item {
                FilledTonalButton(
                    onClick = onAdminLogin,
                    enabled = isConnected && adminPassword.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Войти как администратор")
                }
            }
            if (loginMessage.isNotEmpty()) {
                item {
                    Text(loginMessage, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Список подключённых клиентов:", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = onRefreshClients) {
                        Text("Обновить")
                    }
                }
            }
            if (clientList.isEmpty()) {
                item {
                    Text("Нет активных клиентов.")
                }
            } else {
                items(clientList) { (ip, deviceName) ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(deviceName, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text(ip, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { onBanClient(ip) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text("Забанить")
                            }
                        }
                    }
                }
            }
        }
    }
}