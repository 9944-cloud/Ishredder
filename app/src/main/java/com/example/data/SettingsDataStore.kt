package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ishredder_settings")

data class AppSettings(
    val passcodeEnabled: Boolean = false,
    val passcodeHash: String = "",
    val languageCode: String = "en",
    val skipScan: Boolean = false,
    val defaultMethodId: String = "bsi_vsitr",
    val freeSpaceOverwriteMb: Long = 128L, // 0L means Full Safe Available Space
    val enableErasureReport: Boolean = true,
    val thumbnailPreview: Boolean = true,
    val showHiddenFiles: Boolean = false,
    val usdtMerchantAddress: String = "",
    val uiThemeStyleId: String = "m3_expressive",
    val dynamicColorEnabled: Boolean = false,
    val oledModeEnabled: Boolean = false,
    val uiAccentId: String = "os_default",
    val autoShredEnabled: Boolean = false,
    val autoShredIntervalHours: Int = 24,
    val autoShredIncludeClipboard: Boolean = false,
    val autoShredMethodId: String = "q_shred_2026",
    val lastAutoShredMillis: Long = 0L
)

enum class AppLanguage(val code: String, val label: String) {
    ENGLISH("en", "English"),
    GERMAN("de", "Deutsch"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français"),
    PORTUGUESE("pt", "Português")
}

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val PASSCODE_ENABLED = booleanPreferencesKey("passcode_enabled")
        val PASSCODE_HASH = stringPreferencesKey("passcode_hash")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val SKIP_SCAN = booleanPreferencesKey("skip_scan")
        val DEFAULT_METHOD_ID = stringPreferencesKey("default_method_id")
        val FREE_SPACE_OVERWRITE_MB = longPreferencesKey("free_space_overwrite_mb")
        val ENABLE_ERASURE_REPORT = booleanPreferencesKey("enable_erasure_report")
        val THUMBNAIL_PREVIEW = booleanPreferencesKey("thumbnail_preview")
        val SHOW_HIDDEN_FILES = booleanPreferencesKey("show_hidden_files")
        val USDT_MERCHANT_ADDRESS = stringPreferencesKey("usdt_merchant_address")
        val UI_THEME_STYLE_ID = stringPreferencesKey("ui_theme_style_id")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val OLED_MODE_ENABLED = booleanPreferencesKey("oled_mode_enabled")
        val UI_ACCENT_ID = stringPreferencesKey("ui_accent_id")
        val AUTO_SHRED_ENABLED = booleanPreferencesKey("auto_shred_enabled")
        val AUTO_SHRED_INTERVAL_HOURS = intPreferencesKey("auto_shred_interval_hours")
        val AUTO_SHRED_INCLUDE_CLIPBOARD = booleanPreferencesKey("auto_shred_include_clipboard")
        val AUTO_SHRED_METHOD_ID = stringPreferencesKey("auto_shred_method_id")
        val LAST_AUTO_SHRED_MILLIS = longPreferencesKey("last_auto_shred_millis")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            passcodeEnabled = prefs[Keys.PASSCODE_ENABLED] ?: false,
            passcodeHash = prefs[Keys.PASSCODE_HASH] ?: "",
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
            skipScan = prefs[Keys.SKIP_SCAN] ?: false,
            defaultMethodId = prefs[Keys.DEFAULT_METHOD_ID] ?: "bsi_vsitr",
            freeSpaceOverwriteMb = prefs[Keys.FREE_SPACE_OVERWRITE_MB] ?: 128L,
            enableErasureReport = prefs[Keys.ENABLE_ERASURE_REPORT] ?: true,
            thumbnailPreview = prefs[Keys.THUMBNAIL_PREVIEW] ?: true,
            showHiddenFiles = prefs[Keys.SHOW_HIDDEN_FILES] ?: false,
            usdtMerchantAddress = prefs[Keys.USDT_MERCHANT_ADDRESS] ?: "",
            uiThemeStyleId = prefs[Keys.UI_THEME_STYLE_ID] ?: "m3_expressive",
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR_ENABLED] ?: false,
            oledModeEnabled = prefs[Keys.OLED_MODE_ENABLED] ?: false,
            uiAccentId = prefs[Keys.UI_ACCENT_ID] ?: "os_default",
            autoShredEnabled = prefs[Keys.AUTO_SHRED_ENABLED] ?: false,
            autoShredIntervalHours = prefs[Keys.AUTO_SHRED_INTERVAL_HOURS] ?: 24,
            autoShredIncludeClipboard = prefs[Keys.AUTO_SHRED_INCLUDE_CLIPBOARD] ?: false,
            autoShredMethodId = prefs[Keys.AUTO_SHRED_METHOD_ID] ?: "q_shred_2026",
            lastAutoShredMillis = prefs[Keys.LAST_AUTO_SHRED_MILLIS] ?: 0L
        )
    }

    suspend fun setPasscode(enabled: Boolean, pin: String? = null) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.PASSCODE_ENABLED] = enabled
            if (pin != null) {
                prefs[Keys.PASSCODE_HASH] = hashPin(pin)
            } else if (!enabled) {
                prefs[Keys.PASSCODE_HASH] = ""
            }
        }
    }

    suspend fun setLanguage(code: String) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE_CODE] = code }
    }

    suspend fun setSkipScan(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SKIP_SCAN] = enabled }
    }

    suspend fun setDefaultMethod(methodId: String) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_METHOD_ID] = methodId }
    }

    suspend fun setFreeSpaceOverwriteMb(mb: Long) {
        context.settingsDataStore.edit { it[Keys.FREE_SPACE_OVERWRITE_MB] = mb }
    }

    suspend fun setEnableErasureReport(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ENABLE_ERASURE_REPORT] = enabled }
    }

    suspend fun setThumbnailPreview(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.THUMBNAIL_PREVIEW] = enabled }
    }

    suspend fun setShowHiddenFiles(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SHOW_HIDDEN_FILES] = enabled }
    }

    suspend fun setUsdtMerchantAddress(address: String) {
        context.settingsDataStore.edit { it[Keys.USDT_MERCHANT_ADDRESS] = address.trim() }
    }

    suspend fun setUiThemeStyle(styleId: String) {
        context.settingsDataStore.edit { it[Keys.UI_THEME_STYLE_ID] = styleId }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DYNAMIC_COLOR_ENABLED] = enabled }
    }

    suspend fun setOledModeEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.OLED_MODE_ENABLED] = enabled }
    }

    suspend fun setUiAccentId(accentId: String) {
        context.settingsDataStore.edit { it[Keys.UI_ACCENT_ID] = accentId }
    }

    suspend fun setAutoShredConfig(
        enabled: Boolean,
        intervalHours: Int,
        includeClipboard: Boolean,
        methodId: String
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.AUTO_SHRED_ENABLED] = enabled
            prefs[Keys.AUTO_SHRED_INTERVAL_HOURS] = intervalHours
            prefs[Keys.AUTO_SHRED_INCLUDE_CLIPBOARD] = includeClipboard
            prefs[Keys.AUTO_SHRED_METHOD_ID] = methodId
        }
    }

    suspend fun recordAutoShredRun(timestampMillis: Long) {
        context.settingsDataStore.edit { it[Keys.LAST_AUTO_SHRED_MILLIS] = timestampMillis }
    }

    suspend fun resetToDefaults() {
        context.settingsDataStore.edit { it.clear() }
    }

    companion object {
        fun hashPin(pin: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest("ishredder_excellent_salt:$pin".toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
