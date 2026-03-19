package com.ovzenproject.voicebridgeclient

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SettingsManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "secure_prefs"
        private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        private fun getEncryptedPrefs(context: Context) =
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
    }

    // ---- PEM сертификат ----
    fun saveServerCertificate(pem: String) {
        getEncryptedPrefs(context).edit().putString("server_cert_pem", pem).apply()
    }

    fun getServerCertificate(): String? {
        return getEncryptedPrefs(context).getString("server_cert_pem", null)
    }

    // ---- Режим безопасного подключения ----
    fun setSecureMode(enabled: Boolean) {
        getEncryptedPrefs(context).edit().putBoolean("secure_mode", enabled).apply()
    }

    fun isSecureMode(): Boolean {
        return getEncryptedPrefs(context).getBoolean("secure_mode", true)
    }

    // ---- Битность аудио ----
    fun setSampleBits(bits: Int) {
        getEncryptedPrefs(context).edit().putInt("sample_bits", bits).apply()
    }

    fun getSampleBits(): Int {
        return getEncryptedPrefs(context).getInt("sample_bits", 16)
    }

    // ---- Тема ----
    fun setTheme(theme: Int) {
        getEncryptedPrefs(context).edit().putInt("theme", theme).apply()
    }

    fun getTheme(): Int {
        return getEncryptedPrefs(context).getInt("theme", 0)
    }

    // ---- Режим отладки ----
    fun setDebugMode(enabled: Boolean) {
        getEncryptedPrefs(context).edit().putBoolean("debug_mode", enabled).apply()
    }

    fun isDebugMode(): Boolean {
        return getEncryptedPrefs(context).getBoolean("debug_mode", true)
    }

    // ---- Mute микрофона ----
    fun setMuteEnabled(enabled: Boolean) {
        getEncryptedPrefs(context).edit().putBoolean("mute", enabled).apply()
    }

    fun isMuteEnabled(): Boolean {
        return getEncryptedPrefs(context).getBoolean("mute", false)
    }

    // ---- Admin пароль (сохраняется, но не используется напрямую) ----
    fun setAdminPassword(pass: String) {
        getEncryptedPrefs(context).edit().putString("admin_password", pass).apply()
    }

    fun getAdminPassword(): String? {
        return getEncryptedPrefs(context).getString("admin_password", null)
    }

    // ---- Текущая вкладка ----
    fun setLastTab(tabIndex: Int) {
        getEncryptedPrefs(context).edit().putInt("last_tab", tabIndex).apply()
    }

    fun getLastTab(): Int {
        return getEncryptedPrefs(context).getInt("last_tab", 0)
    }

    // ---- Остальные настройки ----
    fun saveServerIp(ip: String) {
        getEncryptedPrefs(context).edit().putString("server_ip", ip).apply()
    }

    fun getServerIp(): String? {
        return getEncryptedPrefs(context).getString("server_ip", null)
    }

    fun saveServerPort(port: String) {
        getEncryptedPrefs(context).edit().putString("server_port", port).apply()
    }

    fun getServerPort(): String {
        return getEncryptedPrefs(context).getString("server_port", "8765") ?: "8765"
    }

    fun savePassword(pass: String) {
        getEncryptedPrefs(context).edit().putString("password", pass).apply()
    }

    fun getPassword(): String? {
        return getEncryptedPrefs(context).getString("password", null)
    }

    fun saveEffect(effect: String) {
        getEncryptedPrefs(context).edit().putString("effect", effect).apply()
    }

    fun getEffect(): String {
        return getEncryptedPrefs(context).getString("effect", "none") ?: "none"
    }

    // ---- Сброс всех настроек ----
    fun resetAllSettings() {
        getEncryptedPrefs(context).edit().clear().apply()
    }

    // ---- История серверов ----
    fun addServerToHistory(ip: String, port: String) {
        val history = getServerHistory().toMutableList()
        val entry = "$ip:$port"
        history.remove(entry) // чтобы переместить в начало
        history.add(0, entry)
        if (history.size > 10) history.removeAt(history.size - 1)
        val json = Gson().toJson(history)
        getEncryptedPrefs(context).edit().putString("server_history", json).apply()
    }

    fun getServerHistory(): List<String> {
        val json = getEncryptedPrefs(context).getString("server_history", "[]") ?: "[]"
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun clearServerHistory() {
        getEncryptedPrefs(context).edit().remove("server_history").apply()
    }
}