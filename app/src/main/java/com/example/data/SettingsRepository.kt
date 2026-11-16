package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CompressionPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chotoSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "choto_settings")

data class UserSettings(
    val defaultPreset: CompressionPreset = CompressionPreset.BALANCED,
    val customSaveFolderUri: String? = null,
    val customSaveFolderName: String? = null,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val DEFAULT_PRESET = stringPreferencesKey("default_preset")
        val SAVE_FOLDER_URI = stringPreferencesKey("save_folder_uri")
        val SAVE_FOLDER_NAME = stringPreferencesKey("save_folder_name")
    }

    val settingsFlow: Flow<UserSettings> = context.chotoSettingsDataStore.data.map { prefs ->
        val presetStr = prefs[Keys.DEFAULT_PRESET]
        val preset = runCatching {
            if (presetStr != null) CompressionPreset.valueOf(presetStr) else CompressionPreset.BALANCED
        }.getOrDefault(CompressionPreset.BALANCED)
        UserSettings(
            defaultPreset = preset,
            customSaveFolderUri = prefs[Keys.SAVE_FOLDER_URI],
            customSaveFolderName = prefs[Keys.SAVE_FOLDER_NAME],
        )
    }

    suspend fun setDefaultPreset(preset: CompressionPreset) {
        val safePreset = if (preset == CompressionPreset.CUSTOM) CompressionPreset.BALANCED else preset
        context.chotoSettingsDataStore.edit { prefs ->
            prefs[Keys.DEFAULT_PRESET] = safePreset.name
        }
    }

    suspend fun setCustomSaveFolder(uriString: String?, displayName: String?) {
        context.chotoSettingsDataStore.edit { prefs ->
            if (uriString.isNullOrBlank()) {
                prefs.remove(Keys.SAVE_FOLDER_URI)
                prefs.remove(Keys.SAVE_FOLDER_NAME)
            } else {
                prefs[Keys.SAVE_FOLDER_URI] = uriString
                prefs[Keys.SAVE_FOLDER_NAME] = displayName ?: "নির্বাচিত ফোল্ডার"
            }
        }
    }
}
