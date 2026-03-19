package com.ovzenproject.voicebridgeclient

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.accompanist.permissions.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

import com.ovzenproject.voicebridgeclient.models.AdminState
import com.ovzenproject.voicebridgeclient.models.ConnectionStatus
import com.ovzenproject.voicebridgeclient.ui.components.AppTab
import com.ovzenproject.voicebridgeclient.ui.tabs.*

class MainActivity : ComponentActivity() {
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private val _volumeLevel = mutableStateOf(0f)
    private val _latency = mutableStateOf(0L)

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AudioCaptureService.ACTION_CONNECTED -> {
                    ConnectionStatus.isConnected = true
                    showToast(context, "Подключено к серверу")
                }
                AudioCaptureService.ACTION_DISCONNECTED -> {
                    ConnectionStatus.isConnected = false
                    showToast(context, "Отключено от сервера")
                }
                AudioCaptureService.ACTION_ERROR -> {
                    val error = intent.getStringExtra(AudioCaptureService.EXTRA_ERROR) ?: "Unknown error"
                    ConnectionStatus.addLog("❌ Ошибка: $error")
                    showToast(context, "Ошибка: $error")
                }
                AudioCaptureService.ACTION_LOG -> {
                    val logMsg = intent.getStringExtra(AudioCaptureService.EXTRA_LOG) ?: ""
                    ConnectionStatus.addLog(logMsg)
                }
                AudioCaptureService.ACTION_ADMIN_RESULT -> handleAdminResult(intent)
                AudioCaptureService.ACTION_VOLUME_LEVEL -> {
                    _volumeLevel.value = intent.getFloatExtra(AudioCaptureService.EXTRA_VOLUME, 0f)
                }
                AudioCaptureService.ACTION_LATENCY -> {
                    _latency.value = intent.getLongExtra(AudioCaptureService.EXTRA_LATENCY, 0)
                }
            }
        }

        private fun handleAdminResult(intent: Intent) {
            val command = intent.getStringExtra(AudioCaptureService.EXTRA_ADMIN_COMMAND)
            when (command) {
                "login" -> {
                    val success = intent.getBooleanExtra(AudioCaptureService.EXTRA_ADMIN_SUCCESS, false)
                    val message = intent.getStringExtra(AudioCaptureService.EXTRA_ADMIN_MESSAGE) ?: ""
                    if (success) {
                        AdminState.isAuthenticated = true
                        AdminState.loginMessage = "Успешный вход"
                        val cmdIntent = Intent("ADMIN_COMMAND").apply {
                            putExtra("command", "get_clients")
                        }
                        LocalBroadcastManager.getInstance(this@MainActivity).sendBroadcast(cmdIntent)
                    } else {
                        AdminState.loginMessage = "Ошибка: $message"
                    }
                }
                "get_clients" -> {
                    val clientsJson = intent.getStringExtra(AudioCaptureService.EXTRA_CLIENTS) ?: "[]"
                    val clientsArray = JSONArray(clientsJson)
                    val clients = mutableListOf<Pair<String, String>>()
                    for (i in 0 until clientsArray.length()) {
                        val obj = clientsArray.getJSONObject(i)
                        clients.add(Pair(obj.getString("ip"), obj.getString("device_name")))
                    }
                    AdminState.clientList = clients
                }
                "ban" -> {
                    val success = intent.getBooleanExtra(AudioCaptureService.EXTRA_ADMIN_SUCCESS, false)
                    val message = intent.getStringExtra(AudioCaptureService.EXTRA_ADMIN_MESSAGE) ?: ""
                    if (success) {
                        val cmdIntent = Intent("ADMIN_COMMAND").apply {
                            putExtra("command", "get_clients")
                        }
                        LocalBroadcastManager.getInstance(this@MainActivity).sendBroadcast(cmdIntent)
                    }
                    Toast.makeText(this@MainActivity, if (success) "Клиент забанен" else "Ошибка: $message", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun showToast(context: Context?, message: String) {
            if (context != null) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.registerReceiver(
            statusReceiver,
            IntentFilter().apply {
                addAction(AudioCaptureService.ACTION_CONNECTED)
                addAction(AudioCaptureService.ACTION_DISCONNECTED)
                addAction(AudioCaptureService.ACTION_ERROR)
                addAction(AudioCaptureService.ACTION_LOG)
                addAction(AudioCaptureService.ACTION_ADMIN_RESULT)
                addAction(AudioCaptureService.ACTION_VOLUME_LEVEL)
                addAction(AudioCaptureService.ACTION_LATENCY)
            }
        )

        setContent {
            val settingsManager = SettingsManager(this)
            val themeSetting = settingsManager.getTheme()
            val useDarkTheme = when (themeSetting) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MaterialTheme(
                colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                VoiceBridgeApp(
                    settingsManager = settingsManager,
                    volumeLevel = _volumeLevel.value,
                    latency = _latency.value
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager.unregisterReceiver(statusReceiver)
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VoiceBridgeApp(
    settingsManager: SettingsManager,
    volumeLevel: Float,
    latency: Long
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serverIp by remember { mutableStateOf("") }
    var serverPort by remember { mutableStateOf("8765") }
    var password by remember { mutableStateOf("") }
    var effect by remember { mutableStateOf("none") }
    var secureMode by remember { mutableStateOf(settingsManager.isSecureMode()) }
    var sampleBits by remember { mutableStateOf(settingsManager.getSampleBits()) }
    var themeSetting by remember { mutableStateOf(settingsManager.getTheme()) }
    var debugMode by remember { mutableStateOf(settingsManager.isDebugMode()) }
    var muteEnabled by remember { mutableStateOf(settingsManager.isMuteEnabled()) }
    var adminPassword by remember { mutableStateOf("") }

    var selectedTab by remember { mutableStateOf(AppTab.CONNECTION) }

    // Загрузка сохранённых настроек при первом запуске
    LaunchedEffect(Unit) {
        settingsManager.getServerIp()?.let { serverIp = it }
        serverPort = settingsManager.getServerPort()
        settingsManager.getPassword()?.let { password = it }
        effect = settingsManager.getEffect()
        secureMode = settingsManager.isSecureMode()
        sampleBits = settingsManager.getSampleBits()
        themeSetting = settingsManager.getTheme()
        debugMode = settingsManager.isDebugMode()
        muteEnabled = settingsManager.isMuteEnabled()
        val lastTabIndex = settingsManager.getLastTab()
        selectedTab = AppTab.values()[lastTabIndex]
    }

    val recordAudioPermissionState = rememberPermissionState(
        permission = android.Manifest.permission.RECORD_AUDIO
    )

    LaunchedEffect(Unit) {
        if (!recordAudioPermissionState.status.isGranted) {
            recordAudioPermissionState.launchPermissionRequest()
        }
    }

    val permissionDenied = !recordAudioPermissionState.status.isGranted
    if (permissionDenied) {
        LaunchedEffect(Unit) {
            ConnectionStatus.addLog("❌ Нет разрешения на запись аудио.")
        }
    }

    fun showManualCertificateDialog() {
        val alert = AlertDialog.Builder(context)
        val input = EditText(context)
        input.hint = "Вставьте PEM сертификата"
        alert.setTitle("Ввести сертификат вручную")
        alert.setView(input)
        alert.setPositiveButton("Сохранить") { _, _ ->
            val pem = input.text.toString().trim()
            if (pem.isNotEmpty()) {
                scope.launch {
                    settingsManager.saveServerCertificate(pem)
                    ConnectionStatus.addLog("✅ Сертификат сохранён вручную")
                }
            } else {
                Toast.makeText(context, "PEM не может быть пустым", Toast.LENGTH_SHORT).show()
            }
        }
        alert.setNegativeButton("Отмена", null)
        alert.show()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val pem = reader.readText()
                        settingsManager.saveServerCertificate(pem)
                        ConnectionStatus.addLog("✅ Сертификат загружен из файла")
                    }
                } catch (e: Exception) {
                    ConnectionStatus.addLog("❌ Ошибка загрузки сертификата: ${e.message}")
                }
            }
        }
    }

    fun showServerHistoryDialog() {
        val history = settingsManager.getServerHistory()
        if (history.isEmpty()) {
            Toast.makeText(context, "История пуста", Toast.LENGTH_SHORT).show()
            return
        }
        val items = history.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle("Выберите сервер из истории")
            .setItems(items) { _, which ->
                val selected = history[which]
                val parts = selected.split(":")
                if (parts.size == 2) {
                    serverIp = parts[0]
                    serverPort = parts[1]
                    scope.launch {
                        settingsManager.saveServerIp(parts[0])
                        settingsManager.saveServerPort(parts[1])
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    fun resetSettings() {
        scope.launch {
            settingsManager.resetAllSettings()
            (context as? MainActivity)?.recreate()
        }
    }

    fun openVkLink() {
        val url = "https://vk.com/ovzen"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    fun openGitHubReleases() {
        val url = "https://github.com/ovzen/VoiceBridge-Client/releases/latest"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VoiceBridge Client by ovzen") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = { openVkLink() },
                    ) {
                        Text(
                            text = "Разработано vk.com/ovzen",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    TextButton(
                        onClick = { openGitHubReleases() },
                    ) {
                        Text(
                            text = "⭐ Проверить обновления",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (ConnectionStatus.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = if (ConnectionStatus.isConnected) "🟢 Подключено к серверу" else "🔴 Не подключено",
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            TabRow(selectedTabIndex = selectedTab.ordinal) {
                AppTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            scope.launch { settingsManager.setLastTab(tab.ordinal) }
                        },
                        text = { Text("${tab.icon} ${tab.title}") }
                    )
                }
            }

            when (selectedTab) {
                AppTab.CONNECTION -> ConnectionTab(
                    serverIp = serverIp,
                    onServerIpChange = {
                        serverIp = it
                        scope.launch { settingsManager.saveServerIp(it) }
                    },
                    serverPort = serverPort,
                    onServerPortChange = {
                        serverPort = it
                        scope.launch { settingsManager.saveServerPort(it) }
                    },
                    password = password,
                    onPasswordChange = {
                        password = it
                        scope.launch { settingsManager.savePassword(it) }
                    },
                    isConnected = ConnectionStatus.isConnected,
                    permissionDenied = permissionDenied,
                    muteEnabled = muteEnabled,
                    onMuteChange = { newMute ->
                        muteEnabled = newMute
                        scope.launch { settingsManager.setMuteEnabled(newMute) }
                        val intent = Intent("CHANGE_MUTE").putExtra("mute", newMute)
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    },
                    onConnectClick = {
                        if (!ConnectionStatus.isConnected) {
                            if (!recordAudioPermissionState.status.isGranted) {
                                ConnectionStatus.addLog("❌ Нет разрешения на запись аудио. Запросите разрешение.")
                                recordAudioPermissionState.launchPermissionRequest()
                                return@ConnectionTab
                            }
                            val intent = Intent(context, AudioCaptureService::class.java).apply {
                                putExtra("SERVER_IP", serverIp)
                                putExtra("SERVER_PORT", serverPort)
                                putExtra("PASSWORD", password)
                                putExtra("DEVICE_NAME", Build.MODEL)
                                putExtra("EFFECT", effect)
                            }
                            context.startService(intent)
                            if (debugMode) {
                                ConnectionStatus.addLog("🔌 Попытка подключения к $serverIp:$serverPort...")
                            }
                            scope.launch { settingsManager.addServerToHistory(serverIp, serverPort) }
                        } else {
                            context.stopService(Intent(context, AudioCaptureService::class.java))
                            ConnectionStatus.isConnected = false
                        }
                    },
                    volumeLevel = volumeLevel,
                    latency = latency,
                    onShowHistory = { showServerHistoryDialog() }
                )

                AppTab.EFFECTS -> EffectsTab(
                    effect = effect,
                    onEffectChange = { newEffect ->
                        effect = newEffect
                        scope.launch { settingsManager.saveEffect(newEffect) }
                        val intent = Intent("CHANGE_EFFECT").putExtra("effect", newEffect)
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    }
                )

                AppTab.LOGS -> LogsTab(
                    logText = ConnectionStatus.log.joinToString("\n"),
                    onClear = { ConnectionStatus.clearLog() }
                )

                AppTab.SETTINGS -> SettingsTab(
                    secureMode = secureMode,
                    onSecureModeChange = {
                        secureMode = it
                        scope.launch { settingsManager.setSecureMode(it) }
                        if (it && settingsManager.getServerCertificate() == null) {
                            ConnectionStatus.addLog("⚠️ Безопасный режим включён, но сертификат не загружен")
                        }
                    },
                    sampleBits = sampleBits,
                    onSampleBitsChange = { bits ->
                        sampleBits = bits
                        scope.launch { settingsManager.setSampleBits(bits) }
                        ConnectionStatus.addLog("🔧 Битность изменена на $bits бит (для вступления в силу переподключитесь)")
                    },
                    themeSetting = themeSetting,
                    onThemeChange = { theme ->
                        themeSetting = theme
                        scope.launch { settingsManager.setTheme(theme) }
                        scope.launch { settingsManager.setLastTab(selectedTab.ordinal) }
                        (context as? MainActivity)?.recreate()
                    },
                    debugMode = debugMode,
                    onDebugModeChange = {
                        debugMode = it
                        scope.launch { settingsManager.setDebugMode(it) }
                        ConnectionStatus.addLog(if (it) "🐛 Режим отладки включён" else "🐛 Режим отладки отключён")
                    },
                    onUpdateCertificate = { showManualCertificateDialog() },
                    onLoadCertificateFromFile = { filePickerLauncher.launch("*/*") },
                    onResetSettings = { resetSettings() }
                )

                AppTab.ADMIN -> AdminTab(
                    isConnected = ConnectionStatus.isConnected,
                    isAuthenticated = AdminState.isAuthenticated,
                    loginMessage = AdminState.loginMessage,
                    adminPassword = adminPassword,
                    onAdminPasswordChange = { adminPassword = it },
                    onAdminLogin = {
                        val cmdIntent = Intent("ADMIN_COMMAND").apply {
                            putExtra("command", "login")
                            putExtra("password", adminPassword)
                        }
                        LocalBroadcastManager.getInstance(context).sendBroadcast(cmdIntent)
                    },
                    clientList = AdminState.clientList,
                    onBanClient = { ip ->
                        val cmdIntent = Intent("ADMIN_COMMAND").apply {
                            putExtra("command", "ban")
                            putExtra("ip", ip)
                        }
                        LocalBroadcastManager.getInstance(context).sendBroadcast(cmdIntent)
                    },
                    onRefreshClients = {
                        val cmdIntent = Intent("ADMIN_COMMAND").apply {
                            putExtra("command", "get_clients")
                        }
                        LocalBroadcastManager.getInstance(context).sendBroadcast(cmdIntent)
                    }
                )
            }
        }
    }
}