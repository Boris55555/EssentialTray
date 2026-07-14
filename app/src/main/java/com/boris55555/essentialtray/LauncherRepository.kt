package com.boris55555.essentialtray

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.os.UserManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable? = null,
    val userHandle: UserHandle? = null,
    val tags: List<String> = emptyList(),
    val isPrivate: Boolean = false
)

data class WidgetProviderInfo(
    val label: String,
    val providerName: String,
    val packageName: String,
    val previewImage: Drawable?,
    val icon: Drawable?,
    val info: AppWidgetProviderInfo
)

class LauncherRepository(private val context: Context) {

    private val FAVORITES_KEY = stringSetPreferencesKey("favorites")
    private val MAX_FAVORITES_KEY = intPreferencesKey("max_favorites")
    private val SHOW_CAMERA_KEY = booleanPreferencesKey("show_camera")
    private val SHOW_MAPS_KEY = booleanPreferencesKey("show_maps")
    private val CAMERA_PKG_SINGLE_KEY = stringPreferencesKey("camera_pkg")
    private val MAPS_PKG_KEY = stringPreferencesKey("maps_pkg")
    private val ENABLE_WIDGETS_KEY = booleanPreferencesKey("enable_widgets")
    private val WIDGET_DATA_KEY = stringPreferencesKey("widget_data")
    private val ENABLE_TOP_APPS_KEY = booleanPreferencesKey("enable_top_apps")
    private val USAGE_STATS_KEY = stringPreferencesKey("usage_stats")
    private val DATE_FORMAT_KEY = stringPreferencesKey("date_format")
    private val CUSTOM_LABELS_KEY = stringSetPreferencesKey("custom_labels")
    private val HIDDEN_APPS_KEY = stringSetPreferencesKey("hidden_apps")
    private val APP_TAGS_KEY = stringSetPreferencesKey("app_tags")
    private val THREE_FINGER_TAP_KEY = booleanPreferencesKey("three_finger_tap")
    private val SHOW_SETTINGS_ICON_KEY = booleanPreferencesKey("show_settings_icon")
    private val LONG_PRESS_ACTION_KEY = booleanPreferencesKey("long_press_action")
    private val SHOW_PRIVATE_SPACE_KEY = booleanPreferencesKey("show_private_space")
    private val SHOW_NOTIFICATIONS_KEY = booleanPreferencesKey("show_notifications")
    private val DOUBLE_TAP_SLEEP_KEY = booleanPreferencesKey("double_tap_sleep")
    private val HIDDEN_FROM_POPULAR_KEY = stringSetPreferencesKey("hidden_from_popular")
    private val FONT_COLOR_KEY = intPreferencesKey("font_color")
    private val ENABLE_CALENDAR_EVENTS_KEY = booleanPreferencesKey("enable_calendar_events")
    private val CALENDAR_PKG_KEY = stringPreferencesKey("calendar_pkg")
    private val ENABLE_QUOTES_KEY = booleanPreferencesKey("enable_quotes")
    private val QUOTES_KEY = stringSetPreferencesKey("quotes")
    private val LAST_QUOTE_KEY = stringPreferencesKey("last_quote")
    private val LAST_QUOTE_TIME_KEY = longPreferencesKey("last_quote_time")
    private val NEXT_QUOTE_INTERVAL_KEY = longPreferencesKey("next_quote_interval")
    private val SHOW_ALL_APPS_ICON_KEY = booleanPreferencesKey("show_all_apps_icon")
    private val SWITCH_CAMERA_MAPS_KEY = booleanPreferencesKey("switch_camera_maps")
    private val SHOW_WIDGETS_ICON_KEY = booleanPreferencesKey("show_widgets_icon")
    private val SHOW_POPULAR_ICON_KEY = booleanPreferencesKey("show_popular_icon")
    private val SPLIT_PRIVATE_SPACE_KEY = booleanPreferencesKey("split_private_space")

    val favoritesFlow: Flow<List<String>> = context.dataStore.data.map { it[FAVORITES_KEY]?.toList() ?: emptyList() }
    val maxFavoritesFlow: Flow<Int> = context.dataStore.data.map { it[MAX_FAVORITES_KEY] ?: 8 }
    val showCameraButtonFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_CAMERA_KEY] ?: false }
    val showMapsButtonFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_MAPS_KEY] ?: false }
    val selectedCameraPackageFlow: Flow<String?> = context.dataStore.data.map { it[CAMERA_PKG_SINGLE_KEY] }
    val selectedMapsPackageFlow: Flow<String?> = context.dataStore.data.map { it[MAPS_PKG_KEY] }
    val enableWidgetsFlow: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_WIDGETS_KEY] ?: false }
    val widgetDataFlow: Flow<String?> = context.dataStore.data.map { it[WIDGET_DATA_KEY] }
    val enableTopAppsFlow: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_TOP_APPS_KEY] ?: false }
    val usageStatsFlow: Flow<Map<String, Int>> = context.dataStore.data.map { preferences ->
        val data = preferences[USAGE_STATS_KEY] ?: ""
        data.split(",").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            parts[0] to (parts[1].toIntOrNull() ?: 0)
        }
    }
    val dateFormatFlow: Flow<String> = context.dataStore.data.map { it[DATE_FORMAT_KEY] ?: "dd.MM.yyyy" }
    val customLabelsFlow: Flow<Map<String, String>> = context.dataStore.data.map { preferences ->
        val data = preferences[CUSTOM_LABELS_KEY] ?: emptySet()
        data.associate {
            val parts = it.split("|", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else "" to ""
        }.filter { it.key.isNotEmpty() }
    }
    val hiddenAppsFlow: Flow<Set<String>> = context.dataStore.data.map { it[HIDDEN_APPS_KEY] ?: emptySet() }
    val appTagsFlow: Flow<Map<String, List<String>>> = context.dataStore.data.map { preferences ->
        val data = preferences[APP_TAGS_KEY] ?: emptySet()
        data.associate {
            val parts = it.split("|", limit = 2)
            if (parts.size == 2) {
                parts[0] to parts[1].split(",").filter { t -> t.isNotBlank() }
            } else "" to emptyList()
        }.filter { it.key.isNotEmpty() }
    }
    val threeFingerTapFlow: Flow<Boolean> = context.dataStore.data.map { it[THREE_FINGER_TAP_KEY] ?: false }
    val showSettingsIconFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_SETTINGS_ICON_KEY] ?: true }
    val longPressActionFlow: Flow<Boolean> = context.dataStore.data.map { it[LONG_PRESS_ACTION_KEY] ?: false }
    val showNotificationsFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_NOTIFICATIONS_KEY] ?: false }
    val doubleTapSleepFlow: Flow<Boolean> = context.dataStore.data.map { it[DOUBLE_TAP_SLEEP_KEY] ?: false }
    val showPrivateSpaceFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_PRIVATE_SPACE_KEY] ?: true }
    val hiddenFromPopularFlow: Flow<Set<String>> = context.dataStore.data.map { it[HIDDEN_FROM_POPULAR_KEY] ?: emptySet() }
    val fontColorFlow: Flow<Int> = context.dataStore.data.map { it[FONT_COLOR_KEY] ?: android.graphics.Color.WHITE }
    val enableCalendarEventsFlow: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_CALENDAR_EVENTS_KEY] ?: false }
    val selectedCalendarPackageFlow: Flow<String?> = context.dataStore.data.map { it[CALENDAR_PKG_KEY] }

    val enableQuotesFlow: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_QUOTES_KEY] ?: false }
    val quotesFlow: Flow<List<String>> = context.dataStore.data.map { it[QUOTES_KEY]?.toList() ?: emptyList() }
    
    data class QuoteState(val quote: String?, val lastTime: Long, val nextInterval: Long)
    val quoteStateFlow: Flow<QuoteState> = context.dataStore.data.map {
        QuoteState(
            it[LAST_QUOTE_KEY],
            it[LAST_QUOTE_TIME_KEY] ?: 0L,
            it[NEXT_QUOTE_INTERVAL_KEY] ?: 0L
        )
    }

    val showAllAppsIconFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_ALL_APPS_ICON_KEY] ?: true }
    val switchCameraMapsFlow: Flow<Boolean> = context.dataStore.data.map { it[SWITCH_CAMERA_MAPS_KEY] ?: false }
    val showWidgetsIconFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_WIDGETS_ICON_KEY] ?: false }
    val showPopularIconFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_POPULAR_ICON_KEY] ?: false }
    val splitPrivateSpaceFlow: Flow<Boolean> = context.dataStore.data.map { it[SPLIT_PRIVATE_SPACE_KEY] ?: false }

    suspend fun saveFavorites(favorites: List<String>) {
        context.dataStore.edit { it[FAVORITES_KEY] = favorites.toSet() }
    }
    suspend fun saveMaxFavorites(max: Int) {
        context.dataStore.edit { it[MAX_FAVORITES_KEY] = max.coerceIn(1, 8) }
    }
    suspend fun setShowCameraButton(show: Boolean) {
        context.dataStore.edit { it[SHOW_CAMERA_KEY] = show }
    }
    suspend fun setShowMapsButton(show: Boolean) {
        context.dataStore.edit { it[SHOW_MAPS_KEY] = show }
    }
    suspend fun setCameraPackage(pkg: String) {
        context.dataStore.edit { it[CAMERA_PKG_SINGLE_KEY] = pkg }
    }
    suspend fun setMapsPackage(pkg: String) {
        context.dataStore.edit { it[MAPS_PKG_KEY] = pkg }
    }
    suspend fun setEnableWidgets(enable: Boolean) {
        context.dataStore.edit { it[ENABLE_WIDGETS_KEY] = enable }
    }
    suspend fun saveWidgetData(data: String) {
        context.dataStore.edit { it[WIDGET_DATA_KEY] = data }
    }
    suspend fun setEnableTopApps(enable: Boolean) {
        context.dataStore.edit { it[ENABLE_TOP_APPS_KEY] = enable }
    }
    suspend fun setDateFormat(format: String) {
        context.dataStore.edit { it[DATE_FORMAT_KEY] = format }
    }
    suspend fun setCustomLabel(packageName: String, label: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[CUSTOM_LABELS_KEY]?.toMutableSet() ?: mutableSetOf()
            current.removeAll { it.startsWith("$packageName|") }
            if (label.isNotEmpty()) current.add("$packageName|$label")
            preferences[CUSTOM_LABELS_KEY] = current
        }
    }
    suspend fun toggleHiddenApp(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[HIDDEN_APPS_KEY]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) current.remove(packageName) else current.add(packageName)
            preferences[HIDDEN_APPS_KEY] = current
        }
    }
    suspend fun setAppTags(packageName: String, tags: List<String>) {
        context.dataStore.edit { preferences ->
            val current = preferences[APP_TAGS_KEY]?.toMutableSet() ?: mutableSetOf()
            current.removeAll { it.startsWith("$packageName|") }
            if (tags.isNotEmpty()) current.add("$packageName|${tags.joinToString(",")}")
            preferences[APP_TAGS_KEY] = current
        }
    }
    suspend fun setThreeFingerTap(enabled: Boolean) {
        context.dataStore.edit { it[THREE_FINGER_TAP_KEY] = enabled }
    }
    suspend fun setShowSettingsIcon(show: Boolean) {
        context.dataStore.edit { it[SHOW_SETTINGS_ICON_KEY] = show }
    }
    suspend fun setLongPressAction(enabled: Boolean) {
        context.dataStore.edit { it[LONG_PRESS_ACTION_KEY] = enabled }
    }
    suspend fun setShowPrivateSpace(show: Boolean) {
        context.dataStore.edit { it[SHOW_PRIVATE_SPACE_KEY] = show }
    }
    suspend fun setShowNotifications(show: Boolean) {
        context.dataStore.edit { it[SHOW_NOTIFICATIONS_KEY] = show }
    }
    suspend fun setDoubleTapSleep(enabled: Boolean) {
        context.dataStore.edit { it[DOUBLE_TAP_SLEEP_KEY] = enabled }
    }
    suspend fun toggleHiddenFromPopular(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[HIDDEN_FROM_POPULAR_KEY]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) current.remove(packageName) else current.add(packageName)
            preferences[HIDDEN_FROM_POPULAR_KEY] = current
        }
    }
    suspend fun setFontColor(color: Int) {
        context.dataStore.edit { it[FONT_COLOR_KEY] = color }
    }
    suspend fun setEnableCalendarEvents(enabled: Boolean) {
        context.dataStore.edit { it[ENABLE_CALENDAR_EVENTS_KEY] = enabled }
    }
    suspend fun setCalendarPackage(pkg: String) {
        context.dataStore.edit { it[CALENDAR_PKG_KEY] = pkg }
    }
    suspend fun setEnableQuotes(enabled: Boolean) {
        context.dataStore.edit { it[ENABLE_QUOTES_KEY] = enabled }
    }
    suspend fun saveQuotes(quotes: List<String>) {
        context.dataStore.edit { it[QUOTES_KEY] = quotes.toSet() }
    }

    suspend fun updateQuoteState(quote: String?, time: Long, interval: Long) {
        context.dataStore.edit {
            it[LAST_QUOTE_KEY] = quote ?: ""
            it[LAST_QUOTE_TIME_KEY] = time
            it[NEXT_QUOTE_INTERVAL_KEY] = interval
        }
    }

    suspend fun setShowAllAppsIcon(show: Boolean) {
        context.dataStore.edit { it[SHOW_ALL_APPS_ICON_KEY] = show }
    }
    suspend fun setSwitchCameraMaps(switch: Boolean) {
        context.dataStore.edit { it[SWITCH_CAMERA_MAPS_KEY] = switch }
    }
    suspend fun setShowWidgetsIcon(show: Boolean) {
        context.dataStore.edit { it[SHOW_WIDGETS_ICON_KEY] = show }
    }
    suspend fun setShowPopularIcon(show: Boolean) {
        context.dataStore.edit { it[SHOW_POPULAR_ICON_KEY] = show }
    }
    suspend fun setSplitPrivateSpace(split: Boolean) {
        context.dataStore.edit { it[SPLIT_PRIVATE_SPACE_KEY] = split }
    }
    suspend fun incrementUsage(packageName: String) {
        context.dataStore.edit { preferences ->
            val data = preferences[USAGE_STATS_KEY] ?: ""
            val stats = data.split(",").filter { it.contains(":") }.associate {
                val parts = it.split(":")
                parts[0] to (parts[1].toIntOrNull() ?: 0)
            }.toMutableMap()
            stats[packageName] = (stats[packageName] ?: 0) + 1
            preferences[USAGE_STATS_KEY] = stats.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    fun getInstalledApps(): List<AppInfo> {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        
        val allApps = mutableListOf<AppInfo>()
        
        val profiles = try {
            userManager.userProfiles
        } catch (e: Exception) {
            emptyList()
        }

        for (user in profiles) {
            try {
                val isPrivate = if (android.os.Build.VERSION.SDK_INT >= 35) {
                    try {
                        val info = launcherApps.getLauncherUserInfo(user)
                        info != null && android.os.UserManager.USER_TYPE_PROFILE_PRIVATE.equals(info.userType, ignoreCase = true)
                    } catch (e: Exception) { false }
                } else false
                
                val activities = launcherApps.getActivityList(null, user)
                for (activity in activities) {
                    allApps.add(AppInfo(
                        label = activity.label.toString(), 
                        packageName = activity.applicationInfo.packageName, 
                        icon = activity.getIcon(0), 
                        userHandle = user,
                        isPrivate = isPrivate
                    ))
                }
            } catch (e: Exception) {
                // Profile might be locked
            }
        }
        return allApps.distinctBy { "${it.packageName}_${userManager.getSerialNumberForUser(it.userHandle!!)}" }.sortedBy { it.label.lowercase() }
    }

    fun getCameraApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        return pm.queryIntentActivities(intent, 0).map { resolveInfo ->
            AppInfo(label = resolveInfo.loadLabel(pm).toString(), packageName = resolveInfo.activityInfo.packageName)
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }

    fun getMapsApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0"))
        return pm.queryIntentActivities(intent, 0)
            .map { resolveInfo -> AppInfo(label = resolveInfo.loadLabel(pm).toString(), packageName = resolveInfo.activityInfo.packageName) }
            .filter { app ->
                val label = app.label.lowercase()
                val pkg = app.packageName.lowercase()
                !label.contains("lisää sijainniksi") && !label.contains("breezy weather") &&
                !pkg.contains("threema") && !pkg.contains("messenger") && !pkg.contains("whatsapp") &&
                !pkg.contains("telegram") && !pkg.contains("signal")
            }
            .distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }

    fun getCalendarApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
        val apps = pm.queryIntentActivities(intent, 0).map { resolveInfo ->
            AppInfo(label = resolveInfo.loadLabel(pm).toString(), packageName = resolveInfo.activityInfo.packageName)
        }.toMutableList()
        if (apps.isEmpty()) {
            return getInstalledApps().filter { val l = it.label.lowercase(); l.contains("calendar") || l.contains("kalenteri") }
        }
        return apps.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }

    fun getAvailableWidgets(): List<WidgetProviderInfo> {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val pm = context.packageManager
        return appWidgetManager.installedProviders.map { info ->
            WidgetProviderInfo(label = info.loadLabel(pm), providerName = info.provider.className, packageName = info.provider.packageName, previewImage = info.loadPreviewImage(context, 0), icon = info.loadIcon(context, 0), info = info)
        }.sortedBy { it.label.lowercase() }
    }

    fun launchApp(packageName: String) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        for (user in userManager.userProfiles) {
            val activities = launcherApps.getActivityList(packageName, user)
            if (activities.isNotEmpty()) {
                launcherApps.startMainActivity(activities[0].componentName, user, null, null)
                return
            }
        }
    }

    fun canUninstall(packageName: String): Boolean {
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
        } catch (e: Exception) { false }
    }

    fun uninstallApp(packageName: String) {
        try {
            val uri = android.net.Uri.fromParts("package", packageName, null)
            val intent = Intent(Intent.ACTION_DELETE, uri)
            context.startActivity(intent)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getPrivateProfileHandle(): UserHandle? {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        
        val profiles = try {
            userManager.userProfiles
        } catch (e: Exception) {
            emptyList()
        }

        return profiles.find { user ->
            if (android.os.Build.VERSION.SDK_INT >= 35) {
                try {
                    val info = launcherApps.getLauncherUserInfo(user)
                    info != null && android.os.UserManager.USER_TYPE_PROFILE_PRIVATE.equals(info.userType, ignoreCase = true)
                } catch (e: Exception) {
                    false
                }
            } else false
        }
    }

    fun isPrivateSpaceLocked(): Boolean {
        val handle = getPrivateProfileHandle() ?: return false
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        return userManager.isQuietModeEnabled(handle)
    }

    fun requestUnlockPrivateSpace() {
        val handle = getPrivateProfileHandle() ?: return
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val isLocked = userManager.isQuietModeEnabled(handle)
        
        try {
            userManager.requestQuietModeEnabled(!isLocked, handle)
        } catch (e: Exception) {
            if (isLocked) {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val activities = launcherApps.getActivityList(null, handle)
                if (activities.isNotEmpty()) launcherApps.startMainActivity(activities[0].componentName, handle, null, null)
            }
        }
    }

    fun expandNotifications() {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val expandMethod = statusBarManager.getMethod("expandNotificationsPanel")
            expandMethod.invoke(statusBarService)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getTodayEvents(): List<String> {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) return emptyList()
        val events = mutableListOf<String>()
        val now = java.util.Calendar.getInstance()
        val startOfDay = now.clone() as java.util.Calendar
        startOfDay.set(java.util.Calendar.HOUR_OF_DAY, 0)
        startOfDay.set(java.util.Calendar.MINUTE, 0)
        startOfDay.set(java.util.Calendar.SECOND, 0)
        val endOfDay = now.clone() as java.util.Calendar
        endOfDay.set(java.util.Calendar.HOUR_OF_DAY, 23)
        endOfDay.set(java.util.Calendar.MINUTE, 59)
        endOfDay.set(java.util.Calendar.SECOND, 59)
        
        val currentTime = System.currentTimeMillis()

        val projection = arrayOf(
            android.provider.CalendarContract.Events.TITLE,
            android.provider.CalendarContract.Events.DTSTART,
            android.provider.CalendarContract.Events.DTEND,
            android.provider.CalendarContract.Events.ALL_DAY
        )
        // Selection: Ends in the future AND starts before the end of today
        val selection = "${android.provider.CalendarContract.Events.DTEND} > ? AND ${android.provider.CalendarContract.Events.DTSTART} <= ?"
        val selectionArgs = arrayOf(currentTime.toString(), endOfDay.timeInMillis.toString())
        
        try {
            context.contentResolver.query(
                android.provider.CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${android.provider.CalendarContract.Events.DTSTART} ASC"
            )?.use { cursor ->
                val titleIndex = cursor.getColumnIndex(android.provider.CalendarContract.Events.TITLE)
                val allDayIndex = cursor.getColumnIndex(android.provider.CalendarContract.Events.ALL_DAY)
                while (cursor.moveToNext()) {
                    val isAllDay = cursor.getInt(allDayIndex) != 0
                    // All day events are special (start/end at UTC 00:00), we usually want to show them all day
                    events.add(cursor.getString(titleIndex))
                }
            }
        } catch (e: Exception) {
e.printStackTrace() }
        return events
    }
}
