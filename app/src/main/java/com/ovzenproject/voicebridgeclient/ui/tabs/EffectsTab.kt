package com.ovzenproject.voicebridgeclient.ui.tabs
import com.ovzenproject.voicebridgeclient.models.AdminState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EffectsTab(
    effect: String,
    onEffectChange: (String) -> Unit
) {
    val effects = listOf(
        "none" to "🚫 Без эффекта",
        "pitch_up" to "⬆️ Повышение тона",
        "pitch_down" to "⬇️ Понижение тона",
        "echo" to "🔊 Эхо",
        "robot" to "🤖 Робот",
        "chorus" to "🎶 Хорус"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Выберите эффект:", style = MaterialTheme.typography.titleMedium)
        }
        items(effects) { (key, label) ->
            FilledTonalButton(
                onClick = { onEffectChange(key) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (effect == key) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(label)
            }
        }
    }
}