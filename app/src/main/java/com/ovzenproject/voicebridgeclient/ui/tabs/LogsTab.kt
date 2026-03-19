package com.ovzenproject.voicebridgeclient.ui.tabs

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun LogsTab(logText: String, onClear: () -> Unit) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📋 Лог:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = onClear) { Text("🗑️ Очистить") }
            FilledTonalButton(
                onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, logText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Поделиться логами"))
                }
            ) {
                Text("📤 Поделиться")
            }
        }
        Text(logText, modifier = Modifier.fillMaxWidth())
    }
}