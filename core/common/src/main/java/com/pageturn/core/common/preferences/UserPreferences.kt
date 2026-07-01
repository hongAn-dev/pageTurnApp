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
    val dailyNotify: Boolean = false
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
    
    private val userNameKey = stringPreferencesKey("user_name")
    private val userEmailKey = stringPreferencesKey("user_email")
    private val userBioKey = stringPreferencesKey("user_bio")
    private val favoriteBookIdsKey = stringSetPreferencesKey("favorite_book_ids")
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")

    val userSettings: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        UserSettings(
            fontSizeSp = preferences[fontSizeKey] ?: 16,
            fontFamily = preferences[fontFamilyKey] ?: "serif",
            readingTheme = preferences[readingThemeKey] ?: "warm",
            autoSync = preferences[autoSyncKey] ?: true,
            dailyNotify = preferences[dailyNotifyKey] ?: false
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
}
