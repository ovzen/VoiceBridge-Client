package com.ovzenproject.voicebridgeclient.models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

object ConnectionStatus {
    val log = mutableStateListOf<String>()

    private val _isConnected = mutableStateOf(false)
    var isConnected: Boolean
        get() = _isConnected.value
        set(value) { _isConnected.value = value }

    fun addLog(message: String) {
        log.add(message)
        if (log.size > 200) log.removeAt(0)
    }

    fun clearLog() {
        log.clear()
    }
}