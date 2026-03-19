package com.ovzenproject.voicebridgeclient.models

import androidx.compose.runtime.mutableStateOf

object AdminState {
    private val _isAuthenticated = mutableStateOf(false)
    var isAuthenticated: Boolean
        get() = _isAuthenticated.value
        set(value) {
            _isAuthenticated.value = value
            onChange?.invoke()
        }

    private val _loginMessage = mutableStateOf("")
    var loginMessage: String
        get() = _loginMessage.value
        set(value) {
            _loginMessage.value = value
            onChange?.invoke()
        }

    private val _clientList = mutableStateOf(emptyList<Pair<String, String>>())
    var clientList: List<Pair<String, String>>
        get() = _clientList.value
        set(value) {
            _clientList.value = value
            onChange?.invoke()
        }

    private var onChange: (() -> Unit)? = null

    fun setOnChangeListener(listener: (() -> Unit)?) {
        onChange = listener
    }
}