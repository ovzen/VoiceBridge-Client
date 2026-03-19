package com.ovzenproject.voicebridgeclient

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class AudioCaptureService : Service() {
    private val CHANNEL_ID = "VoiceBridgeChannel"
    private val NOTIFICATION_ID = 1
    private var audioRecord: AudioRecord? = null
    private var isCapturing = false
    private lateinit var connectionManager: ConnectionManager
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var effect: String = "none"
    private var sampleBits: Int = 16
    private lateinit var settingsManager: SettingsManager
    private var debugMode: Boolean = true
    private var muteEnabled: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val ACTION_CONNECTED = "com.ovzenproject.voicebridgeclient.CONNECTED"
        const val ACTION_DISCONNECTED = "com.ovzenproject.voicebridgeclient.DISCONNECTED"
        const val ACTION_ERROR = "com.ovzenproject.voicebridgeclient.ERROR"
        const val ACTION_LOG = "com.ovzenproject.voicebridgeclient.LOG"
        const val ACTION_ADMIN_RESULT = "com.ovzenproject.voicebridgeclient.ADMIN_RESULT"
        const val ACTION_VOLUME_LEVEL = "com.ovzenproject.voicebridgeclient.VOLUME_LEVEL"
        const val ACTION_LATENCY = "com.ovzenproject.voicebridgeclient.LATENCY"
        const val EXTRA_ERROR = "error_message"
        const val EXTRA_LOG = "log_message"
        const val EXTRA_ADMIN_COMMAND = "admin_command"
        const val EXTRA_ADMIN_SUCCESS = "admin_success"
        const val EXTRA_ADMIN_MESSAGE = "admin_message"
        const val EXTRA_CLIENTS = "clients"
        const val EXTRA_VOLUME = "volume_level"
        const val EXTRA_LATENCY = "latency"
    }

    private val effectChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "CHANGE_EFFECT" -> {
                    val newEffect = intent.getStringExtra("effect") ?: return
                    effect = newEffect
                    sendLog("Эффект изменён на: $effect")
                }
                "CHANGE_MUTE" -> {
                    muteEnabled = intent.getBooleanExtra("mute", false)
                    sendLog(if (muteEnabled) "🎤 Микрофон выключен" else "🎤 Микрофон включён")
                }
            }
        }
    }

    private val adminCommandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "ADMIN_COMMAND") {
                val command = intent.getStringExtra("command")
                when (command) {
                    "login" -> {
                        val adminPassword = intent.getStringExtra("password") ?: return
                        adminLogin(adminPassword)
                    }
                    "get_clients" -> getClients()
                    "ban" -> {
                        val ip = intent.getStringExtra("ip") ?: return
                        banClient(ip)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
        debugMode = settingsManager.isDebugMode()
        muteEnabled = settingsManager.isMuteEnabled()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        LocalBroadcastManager.getInstance(this).registerReceiver(
            effectChangeReceiver,
            IntentFilter().apply {
                addAction("CHANGE_EFFECT")
                addAction("CHANGE_MUTE")
            }
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            adminCommandReceiver,
            IntentFilter("ADMIN_COMMAND")
        )
        sendLog("Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val serverIp = intent?.getStringExtra("SERVER_IP") ?: return START_NOT_STICKY
            val serverPort = intent?.getStringExtra("SERVER_PORT") ?: "8765"
            val password = intent?.getStringExtra("PASSWORD") ?: ""
            val deviceName = intent?.getStringExtra("DEVICE_NAME") ?: "Android"
            effect = intent?.getStringExtra("EFFECT") ?: "none"

            sampleBits = settingsManager.getSampleBits()
            sendLog("onStartCommand: $serverIp:$serverPort, device=$deviceName, effect=$effect, bits=$sampleBits, mute=$muteEnabled")

            connectionManager = ConnectionManager(
                this,
                serverIp, serverPort, password, deviceName,
                settingsManager,
                onConnected = {
                    sendLog("onConnected called")
                    sendBroadcast(ACTION_CONNECTED)
                    startCapture()
                },
                onDisconnected = {
                    sendLog("onDisconnected called")
                    sendBroadcast(ACTION_DISCONNECTED)
                    stopCapture()
                },
                onError = { error ->
                    sendLog("Connection error: $error")
                    sendBroadcast(ACTION_ERROR, error)
                    mainHandler.postDelayed({
                        stopForeground(true)
                        stopSelf()
                    }, 100)
                }
            )

            // Привязываем коллбэки для admin-команд
            connectionManager.onAdminLoginResult = { success, message ->
                val resultIntent = Intent(ACTION_ADMIN_RESULT).apply {
                    putExtra(EXTRA_ADMIN_COMMAND, "login")
                    putExtra(EXTRA_ADMIN_SUCCESS, success)
                    putExtra(EXTRA_ADMIN_MESSAGE, message ?: "")
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent)
            }
            connectionManager.onClientListResult = { clients ->
                val clientsJson = org.json.JSONArray()
                clients.forEach { (ip, name) ->
                    val obj = org.json.JSONObject().apply {
                        put("ip", ip)
                        put("device_name", name)
                    }
                    clientsJson.put(obj)
                }
                val resultIntent = Intent(ACTION_ADMIN_RESULT).apply {
                    putExtra(EXTRA_ADMIN_COMMAND, "get_clients")
                    putExtra(EXTRA_CLIENTS, clientsJson.toString())
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent)
            }
            connectionManager.onBanResult = { success, message ->
                val resultIntent = Intent(ACTION_ADMIN_RESULT).apply {
                    putExtra(EXTRA_ADMIN_COMMAND, "ban")
                    putExtra(EXTRA_ADMIN_SUCCESS, success)
                    putExtra(EXTRA_ADMIN_MESSAGE, message)
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent)
            }

            // Коллбэк для latency
            connectionManager.onLatencyUpdate = { latency ->
                val intent = Intent(ACTION_LATENCY).putExtra(EXTRA_LATENCY, latency)
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            }

            connectionManager.connect()
        } catch (e: Exception) {
            sendLog("💥 CRASH in onStartCommand: ${e.message}")
            e.printStackTrace()
            mainHandler.postDelayed({
                stopForeground(true)
                stopSelf()
            }, 100)
        }
        return START_STICKY
    }

    // Публичные методы для вызова извне (через broadcast)
    fun adminLogin(adminPassword: String) {
        if (::connectionManager.isInitialized) {
            connectionManager.adminLogin(adminPassword)
        } else {
            sendLog("connectionManager not initialized")
        }
    }

    fun getClients() {
        if (::connectionManager.isInitialized) {
            connectionManager.getClients()
        } else {
            sendLog("connectionManager not initialized")
        }
    }

    fun banClient(ip: String) {
        if (::connectionManager.isInitialized) {
            connectionManager.banClient(ip)
        } else {
            sendLog("connectionManager not initialized")
        }
    }

    private fun sendLog(message: String) {
        if (!debugMode) return
        val intent = Intent(ACTION_LOG).putExtra(EXTRA_LOG, message)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendBroadcast(action: String, errorMessage: String? = null) {
        val intent = Intent(action)
        if (errorMessage != null) {
            intent.putExtra(EXTRA_ERROR, errorMessage)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun startCapture() {
        sendLog("startCapture() called, sampleBits=$sampleBits")
        try {
            acquireWakeLock()

            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = when (sampleBits) {
                24 -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        6
                    } else {
                        sendLog("⚠️ 24‑бит не поддерживается на Android < 12, используется 16 бит")
                        AudioFormat.ENCODING_PCM_16BIT
                    }
                }
                else -> AudioFormat.ENCODING_PCM_16BIT
            }

            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                sendLog("❌ AudioRecord не инициализирован")
                releaseWakeLock()
                mainHandler.postDelayed({
                    stopForeground(true)
                    stopSelf()
                }, 100)
                return
            }

            audioRecord?.startRecording()
            isCapturing = true
            sendLog("✅ Recording started, bufferSize=$bufferSize bytes")

            var packetCount = 0
            scope.launch {
                if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                    val shortBuffer = ShortArray(bufferSize / 2)
                    while (isCapturing) {
                        try {
                            val shortsRead = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: 0
                            if (shortsRead > 0) {
                                // Вычисляем RMS для индикатора громкости
                                var sum = 0.0
                                for (i in 0 until shortsRead) {
                                    val sample = shortBuffer[i] / 32768.0f
                                    sum += sample * sample
                                }
                                val rms = sqrt(sum / shortsRead).toFloat()
                                sendVolumeLevel(rms)

                                if (packetCount % 50 == 0 && debugMode) {
                                    sendLog("📥 read $shortsRead shorts (packet $packetCount)")
                                }
                                val floatArray = FloatArray(shortsRead)
                                for (i in 0 until shortsRead) {
                                    floatArray[i] = shortBuffer[i] / 32768.0f
                                }
                                val processed = if (muteEnabled) {
                                    FloatArray(floatArray.size) { 0f }
                                } else {
                                    VoiceEffects.applyEffect(floatArray, effect)
                                }
                                val outBuffer = ByteBuffer.allocate(processed.size * 4).order(ByteOrder.LITTLE_ENDIAN)
                                outBuffer.asFloatBuffer().put(processed)
                                connectionManager.sendAudioData(outBuffer.array())
                                packetCount++
                            } else {
                                if (packetCount % 50 == 0 && debugMode) sendLog("⚠️ read 0 shorts")
                                delay(10)
                            }
                        } catch (e: Exception) {
                            if (debugMode) sendLog("💥 Ошибка в цикле: ${e.message}")
                            e.printStackTrace()
                            delay(100)
                        }
                    }
                } else {
                    val byteBuffer = ByteArray(bufferSize)
                    while (isCapturing) {
                        try {
                            val bytesRead = audioRecord?.read(byteBuffer, 0, byteBuffer.size) ?: 0
                            if (bytesRead >= 3) {
                                val samples = bytesRead / 3
                                // Вычисляем RMS для 24 бит
                                var sum = 0.0
                                val floatArray = FloatArray(samples)
                                for (i in 0 until samples) {
                                    val idx = i * 3
                                    val sample24 = (byteBuffer[idx].toInt() and 0xFF) or
                                            ((byteBuffer[idx + 1].toInt() and 0xFF) shl 8) or
                                            ((byteBuffer[idx + 2].toInt() and 0xFF) shl 16)
                                    val sample32 = if (sample24 and 0x800000 != 0) sample24 or 0xFF000000.toInt() else sample24
                                    val floatSample = sample32 / 8388608.0f
                                    floatArray[i] = floatSample
                                    sum += floatSample * floatSample
                                }
                                val rms = sqrt(sum / samples).toFloat()
                                sendVolumeLevel(rms)

                                if (packetCount % 50 == 0 && debugMode) {
                                    sendLog("📥 read $bytesRead bytes ($samples samples) (packet $packetCount)")
                                }
                                val processed = if (muteEnabled) {
                                    FloatArray(floatArray.size) { 0f }
                                } else {
                                    VoiceEffects.applyEffect(floatArray, effect)
                                }
                                val outBuffer = ByteBuffer.allocate(processed.size * 4).order(ByteOrder.LITTLE_ENDIAN)
                                outBuffer.asFloatBuffer().put(processed)
                                connectionManager.sendAudioData(outBuffer.array())
                                packetCount++
                            } else {
                                if (packetCount % 50 == 0 && debugMode) sendLog("⚠️ read $bytesRead bytes (<3)")
                                delay(10)
                            }
                        } catch (e: Exception) {
                            if (debugMode) sendLog("💥 Ошибка в цикле: ${e.message}")
                            e.printStackTrace()
                            delay(100)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            sendLog("❌ Нет разрешения на запись аудио")
            releaseWakeLock()
            mainHandler.postDelayed({
                stopForeground(true)
                stopSelf()
            }, 100)
        } catch (e: Exception) {
            sendLog("❌ Ошибка инициализации AudioRecord: ${e.message}")
            e.printStackTrace()
            releaseWakeLock()
            mainHandler.postDelayed({
                stopForeground(true)
                stopSelf()
            }, 100)
        }
    }

    private fun sendVolumeLevel(level: Float) {
        val intent = Intent(ACTION_VOLUME_LEVEL).putExtra(EXTRA_VOLUME, level)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun stopCapture() {
        sendLog("stopCapture() called")
        isCapturing = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            sendLog("Ошибка при остановке записи: ${e.message}")
        }
        audioRecord = null
        releaseWakeLock()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "VoiceBridge::AudioCaptureWakeLock"
            )
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire()
            sendLog("🔋 WakeLock acquired")
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            sendLog("🔋 WakeLock released")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sendLog("onDestroy")
        stopForeground(true)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(effectChangeReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(adminCommandReceiver)
        stopCapture()
        if (::connectionManager.isInitialized) {
            try {
                connectionManager.disconnect()
            } catch (e: Exception) {
                sendLog("Error disconnecting: ${e.message}")
            }
        } else {
            sendLog("connectionManager not initialized, skipping disconnect")
        }
        scope.cancel()
        releaseWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VoiceBridge Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VoiceBridge Client")
            .setContentText("Передача аудио на сервер...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }
}