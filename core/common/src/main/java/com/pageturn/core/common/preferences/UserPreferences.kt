package com.pageturn.core.common.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pageturn_preferences")

data class UserSettings(
    val fontSizeSp: Int,
    val fontFamily: String,
    val readingTheme: String,
    val autoSync: Boolean = true,
    val dailyNotify: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val reminderIntervalMins: Int = 1440,
    val reminderMode: String = "daily",
    val reminderIntervalVal: Int = 1,
    val reminderIntervalUnit: String = "hours"
)

data class UserProfile(
    val name: String,
    val email: String,
    val bio: String
)

@Singleton
class UserPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fontSizeKey = intPreferencesKey("font_size")
    private val fontFamilyKey = stringPreferencesKey("font_family")
    private val readingThemeKey = stringPreferencesKey("reading_theme")
    private val autoSyncKey = booleanPreferencesKey("auto_sync")
    private val dailyNotifyKey = booleanPreferencesKey("daily_notify")
    private val reminderHourKey = intPreferencesKey("reminder_hour")
    private val reminderMinuteKey = intPreferencesKey("reminder_minute")
    private val reminderIntervalMinsKey = intPreferencesKey("reminder_interval_mins")
    private val reminderModeKey = stringPreferencesKey("reminder_mode")
    private val reminderIntervalValKey = intPreferencesKey("reminder_interval_val")
    private val reminderIntervalUnitKey = stringPreferencesKey("reminder_interval_unit")
    
    private val userNameKey = stringPreferencesKey("user_name")
    private val userEmailKey = stringPreferencesKey("user_email")
    private val userBioKey = stringPreferencesKey("user_bio")
    private val favoriteBookIdsKey = stringSetPreferencesKey("favorite_book_ids")
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val userCollectionsKey = stringPreferencesKey("user_collections_data")

    val userSettings: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        UserSettings(
            fontSizeSp = preferences[fontSizeKey] ?: 16,
            fontFamily = preferences[fontFamilyKey] ?: "serif",
            readingTheme = preferences[readingThemeKey] ?: "light",
            autoSync = preferences[autoSyncKey] ?: true,
            dailyNotify = preferences[dailyNotifyKey] ?: false,
            reminderHour = preferences[reminderHourKey] ?: 20,
            reminderMinute = preferences[reminderMinuteKey] ?: 0,
            reminderIntervalMins = preferences[reminderIntervalMinsKey] ?: 1440,
            reminderMode = preferences[reminderModeKey] ?: "daily",
            reminderIntervalVal = preferences[reminderIntervalValKey] ?: 1,
            reminderIntervalUnit = preferences[reminderIntervalUnitKey] ?: "hours"
        )
    }

    val userProfile: Flow<UserProfile> = context.dataStore.data.map { preferences ->
        UserProfile(
            name = preferences[userNameKey] ?: "Người dùng PageTurn",
            email = preferences[userEmailKey] ?: "user@pageturn.com",
            bio = preferences[userBioKey] ?: "Người yêu sách & độc giả trung thành"
        )
    }

    val favoriteBookIds: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[favoriteBookIdsKey] ?: emptySet()
    }

    val userCollections: Flow<Map<String, Set<String>>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[userCollectionsKey] ?: ""
        parseCollectionsJson(jsonStr)
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[accessTokenKey]
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[refreshTokenKey]
    }

    suspend fun saveTokens(access: String, refresh: String) {
        context.dataStore.edit { preferences ->
            preferences[accessTokenKey] = access
            preferences[refreshTokenKey] = refresh
        }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(accessTokenKey)
            preferences.remove(refreshTokenKey)
        }
    }

    suspend fun setFontSize(sizeSp: Int) {
        context.dataStore.edit { preferences ->
            preferences[fontSizeKey] = sizeSp
        }
    }

    suspend fun setFontFamily(family: String) {
        context.dataStore.edit { preferences ->
            preferences[fontFamilyKey] = family
        }
    }

    suspend fun setReadingTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[readingThemeKey] = theme
        }
    }

    suspend fun setAutoSync(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[autoSyncKey] = enabled
        }
    }

    suspend fun setDailyNotify(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[dailyNotifyKey] = enabled
        }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[reminderHourKey] = hour
            preferences[reminderMinuteKey] = minute
        }
    }

    suspend fun setReminderInterval(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[reminderIntervalMinsKey] = minutes
        }
    }

    suspend fun setReminderMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[reminderModeKey] = mode
        }
    }

    suspend fun setReminderIntervalVal(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[reminderIntervalValKey] = value
        }
    }

    suspend fun setReminderIntervalUnit(unit: String) {
        context.dataStore.edit { preferences ->
            preferences[reminderIntervalUnitKey] = unit
        }
    }

    suspend fun updateUserProfile(name: String, email: String, bio: String) {
        context.dataStore.edit { preferences ->
            preferences[userNameKey] = name
            preferences[userEmailKey] = email
            preferences[userBioKey] = bio
        }
    }

    suspend fun toggleFavorite(bookId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[favoriteBookIdsKey] ?: emptySet()
            if (current.contains(bookId)) {
                preferences[favoriteBookIdsKey] = current - bookId
            } else {
                preferences[favoriteBookIdsKey] = current + bookId
            }
        }
    }

    suspend fun createCollection(name: String) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[userCollectionsKey] ?: ""
            val map = parseCollectionsJson(jsonStr).toMutableMap()
            if (!map.containsKey(name)) {
                map[name] = emptySet()
                preferences[userCollectionsKey] = serializeCollectionsMap(map)
            }
        }
    }

    suspend fun deleteCollection(name: String) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[userCollectionsKey] ?: ""
            val map = parseCollectionsJson(jsonStr).toMutableMap()
            if (map.containsKey(name)) {
                map.remove(name)
                preferences[userCollectionsKey] = serializeCollectionsMap(map)
            }
        }
    }

    suspend fun toggleBookInCollection(collectionName: String, bookId: String) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[userCollectionsKey] ?: ""
            val map = parseCollectionsJson(jsonStr).toMutableMap()
            val set = (map[collectionName] ?: emptySet()).toMutableSet()
            if (set.contains(bookId)) {
                set.remove(bookId)
            } else {
                set.add(bookId)
            }
            map[collectionName] = set
            preferences[userCollectionsKey] = serializeCollectionsMap(map)
        }
    }

    private fun parseCollectionsJson(jsonStr: String): Map<String, Set<String>> {
        if (jsonStr.isBlank()) {
            return mapOf(
                "Yêu thích nhất" to emptySet(),
                "Đang đọc dở" to emptySet(),
                "Sách hay nên đọc" to emptySet()
            )
        }
        return try {
            val json = org.json.JSONObject(jsonStr)
            val map = mutableMapOf<String, Set<String>>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val arr = json.optJSONArray(key)
                val set = mutableSetOf<String>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        set.add(arr.optString(i))
                    }
                }
                map[key] = set
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun serializeCollectionsMap(map: Map<String, Set<String>>): String {
        val json = org.json.JSONObject()
        for ((key, set) in map) {
            val arr = org.json.JSONArray()
            for (id in set) {
                arr.put(id)
            }
            json.put(key, arr)
        }
        return json.toString()
    }
}
