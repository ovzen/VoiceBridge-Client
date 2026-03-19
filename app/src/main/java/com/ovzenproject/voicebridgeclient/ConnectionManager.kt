package com.ovzenproject.voicebridgeclient

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.URI
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.*

class ConnectionManager(
    private val context: Context,
    private val serverIp: String,
    private val serverPort: String,
    private val password: String,
    private val deviceName: String,
    private val settingsManager: SettingsManager,
    private val onConnected: () -> Unit,
    private val onDisconnected: () -> Unit,
    private val onError: (String) -> Unit
) {
    private var webSocket: WebSocketClient? = null
    private var authComplete = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private var sendCounter = 0
    private var debugMode: Boolean = settingsManager.isDebugMode()
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelay = 2000L // 2 секунды
    private var pingJob: Job? = null
    private var lastPingTime = 0L
    var onLatencyUpdate: ((Long) -> Unit)? = null

    // Коллбэки для административных команд
    var onAdminLoginResult: ((Boolean, String?) -> Unit)? = null
    var onClientListResult: ((List<Pair<String, String>>) -> Unit)? = null
    var onBanResult: ((Boolean, String) -> Unit)? = null

    init {
        localBroadcastManager = LocalBroadcastManager.getInstance(context)
    }

    private fun sendLog(message: String) {
        if (!debugMode) return
        val intent = Intent(AudioCaptureService.ACTION_LOG).putExtra(AudioCaptureService.EXTRA_LOG, "[CM] $message")
        localBroadcastManager.sendBroadcast(intent)
    }

    fun connect() {
        if (webSocket?.isOpen == true) {
            sendLog("Already connected")
            return
        }
        reconnectAttempts = 0
        performConnection()
    }

    private fun performConnection() {
        try {
            val uri = URI("wss://$serverIp:$serverPort")
            sendLog("Connecting to $uri (attempt ${reconnectAttempts + 1})")

            webSocket = object : WebSocketClient(uri) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    sendLog("WebSocket opened, waiting for auth challenge")
                    reconnectAttempts = 0 // сброс попыток при успехе
                    startPing()
                }

                override fun onMessage(message: String) {
                    handleJsonMessage(message)
                }

                override fun onMessage(bytes: ByteBuffer?) {
                    // Игнорируем бинарные сообщения (это аудио)
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    sendLog("WebSocket closed: $code $reason")
                    authComplete = false
                    stopPing()
                    onDisconnected()
                    // Автопереподключение, если не было явной команды на отключение
                    if (reconnectAttempts < maxReconnectAttempts) {
                        reconnectAttempts++
                        Handler(Looper.getMainLooper()).postDelayed({
                            performConnection()
                        }, reconnectDelay)
                    } else {
                        sendLog("Max reconnection attempts reached, giving up")
                        // Не вызываем onError, просто логируем
                    }
                }

                override fun onError(ex: Exception?) {
                    sendLog("WebSocket error: ${ex?.message}")
                    // Не вызываем onError сразу, дадим шанс на переподключение
                }
            }

            setupSSL()
            webSocket?.connect()
        } catch (e: Exception) {
            sendLog("💥 CRASH in connect(): ${e.message}")
            onError("Connection setup crashed: ${e.message}")
        }
    }

    private fun setupSSL() {
        try {
            if (settingsManager.isSecureMode()) {
                val pem = settingsManager.getServerCertificate()
                if (pem != null) {
                    val certificateFactory = CertificateFactory.getInstance("X.509")
                    val certInputStream = ByteArrayInputStream(pem.toByteArray(Charsets.UTF_8))
                    val certificate = certificateFactory.generateCertificate(certInputStream)

                    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                        load(null)
                        setCertificateEntry("server", certificate)
                    }

                    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    trustManagerFactory.init(keyStore)

                    val sslContext = SSLContext.getInstance("TLS")
                    sslContext.init(null, trustManagerFactory.trustManagers, null)

                    webSocket?.setSocketFactory(sslContext.socketFactory)
                    sendLog("Custom SSL factory set (secure mode)")
                } else {
                    sendLog("Secure mode but no certificate saved!")
                    onError("No certificate saved for secure mode")
                }
            } else {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())

                webSocket?.setSocketFactory(sslContext.socketFactory)
                sendLog("Trust-all SSL factory set (insecure mode)")
            }
        } catch (e: Exception) {
            sendLog("❌ SSL setup error: ${e.message}")
            onError("SSL setup failed: ${e.message}")
        }
    }

    private fun startPing() {
        pingJob = scope.launch {
            while (isActive && webSocket?.isOpen == true) {
                delay(5000) // каждые 5 секунд
                if (webSocket?.isOpen == true) {
                    lastPingTime = System.currentTimeMillis()
                    val pingMsg = JSONObject().apply {
                        put("type", "ping")
                        put("timestamp", lastPingTime)
                    }.toString()
                    webSocket?.send(pingMsg)
                }
            }
        }
    }

    private fun stopPing() {
        pingJob?.cancel()
        pingJob = null
    }

    private fun handleJsonMessage(message: String) {
        try {
            val json = JSONObject(message)
            when (json.getString("type")) {
                "auth_challenge" -> {
                    val serverNonce = json.getString("nonce")
                    val clientNonce = generateNonce()
                    val hmac = hmacSha256(password, "$clientNonce:$serverNonce")
                    val response = JSONObject().apply {
                        put("type", "auth_response")
                        put("client_nonce", clientNonce)
                        put("hmac", hmac)
                    }.toString()
                    sendLog("Sending auth_response")
                    webSocket?.send(response)
                }
                "auth_success" -> {
                    sendLog("Authentication successful")
                    authComplete = true
                    val initMsg = JSONObject().apply {
                        put("type", "init")
                        put("device_name", deviceName)
                    }.toString()
                    sendLog("Sending init: $initMsg")
                    webSocket?.send(initMsg)
                    onConnected()
                }
                "pong" -> {
                    val serverTimestamp = json.getLong("timestamp")
                    val latency = System.currentTimeMillis() - serverTimestamp
                    onLatencyUpdate?.invoke(latency)
                }
                "admin_response" -> {
                    handleAdminResponse(json)
                }
                "error" -> {
                    val errorMsg = json.optString("message", "Authentication failed")
                    sendLog("Server error: $errorMsg")
                    onError(errorMsg)
                    webSocket?.close()
                }
            }
        } catch (e: Exception) {
            sendLog("Invalid server message: ${e.message}")
            onError("Invalid server message: ${e.message}")
        }
    }

    private fun handleAdminResponse(json: JSONObject) {
        val command = json.getString("command")
        when (command) {
            "login" -> {
                val success = json.getBoolean("success")
                val message = json.optString("message")
                onAdminLoginResult?.invoke(success, message)
            }
            "get_clients" -> {
                val clientsArray = json.getJSONArray("clients")
                val clients = mutableListOf<Pair<String, String>>()
                for (i in 0 until clientsArray.length()) {
                    val clientObj = clientsArray.getJSONObject(i)
                    val ip = clientObj.getString("ip")
                    val deviceName = clientObj.getString("device_name")
                    clients.add(Pair(ip, deviceName))
                }
                onClientListResult?.invoke(clients)
            }
            "ban" -> {
                val success = json.getBoolean("success")
                val message = json.optString("message")
                onBanResult?.invoke(success, message)
            }
        }
    }

    private fun generateNonce(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256(key: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    fun sendAudioData(data: ByteArray) {
        if (sendCounter % 50 == 0 && debugMode) {
            sendLog("sendAudioData called, authComplete=$authComplete, isOpen=${webSocket?.isOpen}")
        }
        if (webSocket != null && webSocket?.isOpen == true && authComplete) {
            webSocket?.send(data)
            if (sendCounter % 50 == 0 && debugMode) {
                sendLog("✅ Data sent, size=${data.size}")
            }
        } else {
            if (sendCounter % 50 == 0 && debugMode) {
                sendLog("❌ Cannot send audio, socket not ready")
            }
        }
        sendCounter++
    }

    // Административные команды
    fun adminLogin(adminPassword: String) {
        if (webSocket?.isOpen != true || !authComplete) {
            onError("Not connected to server")
            return
        }
        val cmd = JSONObject().apply {
            put("type", "admin")
            put("command", "login")
            put("password", adminPassword)
        }.toString()
        webSocket?.send(cmd)
    }

    fun getClients() {
        if (webSocket?.isOpen != true || !authComplete) {
            onError("Not connected to server")
            return
        }
        val cmd = JSONObject().apply {
            put("type", "admin")
            put("command", "get_clients")
        }.toString()
        webSocket?.send(cmd)
    }

    fun banClient(ip: String) {
        if (webSocket?.isOpen != true || !authComplete) {
            onError("Not connected to server")
            return
        }
        val cmd = JSONObject().apply {
            put("type", "admin")
            put("command", "ban")
            put("ip", ip)
        }.toString()
        webSocket?.send(cmd)
    }

    fun disconnect() {
        sendLog("Disconnect called")
        stopPing()
        reconnectAttempts = maxReconnectAttempts // запретить авто-переподключение
        try {
            webSocket?.close()
        } catch (e: Exception) {
            sendLog("Error closing socket: ${e.message}")
        }
        scope.cancel()
    }
}