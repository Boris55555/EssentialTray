package com.boris55555.essentialtray

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LauncherWidget(
    val id: Int, 
    val height: Int = 150,
    val providerName: String? = null
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LauncherRepository(application)

    val installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val cameraApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val mapsApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val calendarApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val availableWidgets = MutableStateFlow<List<WidgetProviderInfo>>(emptyList())
    
    val favorites = repository.favoritesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val maxFavorites = repository.maxFavoritesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8)
    val showCameraButton = repository.showCameraButtonFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val showMapsButton = repository.showMapsButtonFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val selectedCameraPackage = repository.selectedCameraPackageFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val selectedMapsPackage = repository.selectedMapsPackageFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val selectedCalendarPackage = repository.selectedCalendarPackageFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val enableWidgets = repository.enableWidgetsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val widgets: StateFlow<List<LauncherWidget>> = repository.widgetDataFlow.map { data ->
        data?.split(",")?.filter { it.isNotEmpty() }?.map {
            val parts = it.split(":")
            LauncherWidget(parts[0].toInt(), parts.getOrNull(1)?.toInt() ?: 150, parts.getOrNull(2))
        } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enableTopApps = repository.enableTopAppsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val dateFormat = repository.dateFormatFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "dd.MM.yyyy")
    val threeFingerTapEnabled = repository.threeFingerTapFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val showSettingsButton = repository.showSettingsIconFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val showPrivateSpace = repository.showPrivateSpaceFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val showNotifications = repository.showNotificationsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val doubleTapSleepEnabled = repository.doubleTapSleepFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val fontColor = repository.fontColorFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), android.graphics.Color.WHITE)
    val enableCalendarEvents = repository.enableCalendarEventsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val todayEvents = MutableStateFlow<List<String>>(emptyList())
    val hiddenApps = repository.hiddenAppsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val hiddenFromPopular = repository.hiddenFromPopularFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val longPressActionEnabled = repository.longPressActionFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val customLabels = repository.customLabelsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val appTags = repository.appTagsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val allUniqueTags: StateFlow<Set<String>> = appTags.map { it.values.flatten().toSet() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val enableQuotes = repository.enableQuotesFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val quotes = repository.quotesFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val currentQuote = repository.quoteStateFlow.map { if (it.quote.isNullOrEmpty()) null else it.quote }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val showAllAppsIcon = repository.showAllAppsIconFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val switchCameraMaps = repository.switchCameraMapsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val showWidgetsIcon = repository.showWidgetsIconFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val showPopularIcon = repository.showPopularIconFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val splitPrivateSpace = repository.splitPrivateSpaceFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val notificationCounts = NotificationListener.notifications
    val isPrivateSpaceLocked = MutableStateFlow(false)
    val hasPrivateSpace = MutableStateFlow(false)

    val topApps: StateFlow<List<AppInfo>> = combine(
        installedApps, repository.usageStatsFlow, favorites, customLabels, hiddenApps,
        selectedCameraPackage, selectedMapsPackage, hiddenFromPopular, appTags,
        showPrivateSpace, isPrivateSpaceLocked
    ) { args ->
        @Suppress("UNCHECKED_CAST") val apps = args[0] as List<AppInfo>
        @Suppress("UNCHECKED_CAST") val stats = args[1] as Map<String, Int>
        @Suppress("UNCHECKED_CAST") val favs = args[2] as List<String>
        @Suppress("UNCHECKED_CAST") val labels = args[3] as Map<String, String>
        @Suppress("UNCHECKED_CAST") val hidden = args[4] as Set<String>
        @Suppress("UNCHECKED_CAST") val camera = args[5] as String?
        @Suppress("UNCHECKED_CAST") val maps = args[6] as String?
        @Suppress("UNCHECKED_CAST") val hiddenPop = args[7] as Set<String>
        @Suppress("UNCHECKED_CAST") val tags = args[8] as Map<String, List<String>>
        @Suppress("UNCHECKED_CAST") val showPrivate = args[9] as Boolean
        @Suppress("UNCHECKED_CAST") val isLocked = args[10] as Boolean
        
        stats.entries.filter { !favs.contains(it.key) && !hidden.contains(it.key) && !hiddenPop.contains(it.key) && it.key != camera && it.key != maps }
            .sortedByDescending { it.value }.take(12).mapNotNull { entry ->
                apps.find { it.packageName == entry.key }?.let { app ->
                    if (app.isPrivate && (!showPrivate || isLocked)) return@mapNotNull null
                    val renamed = labels[app.packageName]?.let { app.copy(label = it) } ?: app
                    renamed.copy(tags = tags[app.packageName] ?: emptyList())
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteApps: StateFlow<List<AppInfo>> = combine(
        installedApps, favorites, maxFavorites, customLabels, appTags,
        showPrivateSpace, isPrivateSpaceLocked
    ) { args ->
        @Suppress("UNCHECKED_CAST") val apps = args[0] as List<AppInfo>
        @Suppress("UNCHECKED_CAST") val favs = args[1] as List<String>
        @Suppress("UNCHECKED_CAST") val max = args[2] as Int
        @Suppress("UNCHECKED_CAST") val labels = args[3] as Map<String, String>
        @Suppress("UNCHECKED_CAST") val tags = args[4] as Map<String, List<String>>
        @Suppress("UNCHECKED_CAST") val showPrivate = args[5] as Boolean
        @Suppress("UNCHECKED_CAST") val isLocked = args[6] as Boolean
        
        favs.mapNotNull { pkg -> 
            apps.find { it.packageName == pkg }?.let { app ->
                if (app.isPrivate && (!showPrivate || isLocked)) return@mapNotNull null
                val renamed = labels[pkg]?.let { app.copy(label = it) } ?: app
                renamed.copy(tags = tags[pkg] ?: emptyList())
            }
        }.take(max)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayApps: StateFlow<List<AppInfo>> = combine(
        installedApps, customLabels, hiddenApps, appTags, showPrivateSpace, isPrivateSpaceLocked
    ) { args ->
        val apps = args[0] as List<AppInfo>
        val labels = args[1] as Map<String, String>
        val hidden = args[2] as Set<String>
        val tags = args[3] as Map<String, List<String>>
        val showPrivate = args[4] as Boolean
        val isLocked = args[5] as Boolean

        apps.filter { app ->
            if (hidden.contains(app.packageName)) return@filter false
            if (app.isPrivate && (!showPrivate || isLocked)) return@filter false
            true
        }.map { app ->
            val renamed = labels[app.packageName]?.let { app.copy(label = it) } ?: app
            renamed.copy(tags = tags[app.packageName] ?: emptyList())
        }.sortedBy { it.label.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHiddenApps: StateFlow<List<AppInfo>> = combine(installedApps, customLabels, hiddenApps) { apps, labels, hidden ->
        apps.filter { hidden.contains(it.packageName) }
            .map { app -> labels[app.packageName]?.let { app.copy(label = it) } ?: app }
            .sortedBy { it.label.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { refreshApps() }

    fun refreshApps() {
        viewModelScope.launch {
            installedApps.value = repository.getInstalledApps()
            cameraApps.value = repository.getCameraApps()
            mapsApps.value = repository.getMapsApps()
            calendarApps.value = repository.getCalendarApps()
            availableWidgets.value = repository.getAvailableWidgets()
            isPrivateSpaceLocked.value = repository.isPrivateSpaceLocked()
            hasPrivateSpace.value = repository.getPrivateProfileHandle() != null
            if (enableCalendarEvents.value) todayEvents.value = repository.getTodayEvents()
            
            if (enableQuotes.value) {
                // Use repository flow directly to avoid empty stateIn issues
                val currentList = repository.quotesFlow.first()
                val state = repository.quoteStateFlow.first()
                val now = System.currentTimeMillis()
                
                if (currentList.isEmpty()) {
                    if (!state.quote.isNullOrEmpty()) repository.updateQuoteState(null, 0L, 0L)
                    return@launch
                }
                
                val currentIsDeleted = !state.quote.isNullOrEmpty() && !currentList.contains(state.quote)
                val intervalExpired = now > (state.lastTime + state.nextInterval)
                
                if (currentIsDeleted || intervalExpired || state.quote.isNullOrEmpty()) {
                    val possibleQuotes = if (currentList.size > 1) {
                        currentList.filter { it != state.quote }
                    } else {
                        currentList
                    }
                    val newQuote = possibleQuotes.random()
                    val newInterval = (3600000L..21600000L).random()
                    repository.updateQuoteState(newQuote, now, newInterval)
                }
            }
        }
    }

    fun toggleFavorite(packageName: String) {
        viewModelScope.launch {
            val current = favorites.value.toMutableList()
            if (current.contains(packageName)) current.remove(packageName) else current.add(packageName)
            repository.saveFavorites(current)
        }
    }
    fun setMaxFavorites(max: Int) = viewModelScope.launch { repository.saveMaxFavorites(max) }
    fun setShowCameraButton(show: Boolean) = viewModelScope.launch { repository.setShowCameraButton(show) }
    fun setShowMapsButton(show: Boolean) = viewModelScope.launch { repository.setShowMapsButton(show) }
    fun setCameraPackage(pkg: String) = viewModelScope.launch { repository.setCameraPackage(pkg) }
    fun setMapsPackage(pkg: String) = viewModelScope.launch { repository.setMapsPackage(pkg) }
    fun setCalendarPackage(pkg: String) = viewModelScope.launch { repository.setCalendarPackage(pkg) }
    fun setEnableWidgets(enable: Boolean) = viewModelScope.launch { repository.setEnableWidgets(enable) }
    fun setEnableTopApps(enable: Boolean) = viewModelScope.launch { repository.setEnableTopApps(enable) }
    fun setDateFormat(format: String) = viewModelScope.launch { repository.setDateFormat(format) }
    fun renameApp(packageName: String, newName: String) = viewModelScope.launch { repository.setCustomLabel(packageName, newName) }
    fun setAppTags(packageName: String, tags: List<String>) = viewModelScope.launch { repository.setAppTags(packageName, tags) }
    fun toggleAppVisibility(packageName: String) = viewModelScope.launch { repository.toggleHiddenApp(packageName) }
    fun togglePopularVisibility(packageName: String) = viewModelScope.launch { repository.toggleHiddenFromPopular(packageName) }
    fun canUninstall(packageName: String): Boolean = repository.canUninstall(packageName)
    fun uninstallApp(packageName: String) = repository.uninstallApp(packageName)
    fun setThreeFingerTapEnabled(enabled: Boolean) = viewModelScope.launch { repository.setThreeFingerTap(enabled) }
    fun setShowSettingsButton(show: Boolean) = viewModelScope.launch { repository.setShowSettingsIcon(show) }
    fun setShowPrivateSpace(show: Boolean) = viewModelScope.launch { repository.setShowPrivateSpace(show) }
    fun setShowNotifications(show: Boolean) = viewModelScope.launch { repository.setShowNotifications(show) }
    fun setLongPressActionEnabled(enabled: Boolean) = viewModelScope.launch { repository.setLongPressAction(enabled) }
    fun setDoubleTapSleepEnabled(enabled: Boolean) = viewModelScope.launch { repository.setDoubleTapSleep(enabled) }
    fun setFontColor(color: Int) = viewModelScope.launch { repository.setFontColor(color) }
    fun setEnableQuotes(enabled: Boolean) = viewModelScope.launch { repository.setEnableQuotes(enabled) }
    fun setEnableCalendarEvents(enabled: Boolean) = viewModelScope.launch { repository.setEnableCalendarEvents(enabled) }
    fun setShowAllAppsIcon(show: Boolean) = viewModelScope.launch { repository.setShowAllAppsIcon(show) }
    fun setSwitchCameraMapsEnabled(switch: Boolean) = viewModelScope.launch { repository.setSwitchCameraMaps(switch) }
    fun setShowWidgetsIconEnabled(show: Boolean) = viewModelScope.launch { repository.setShowWidgetsIcon(show) }
    fun setShowPopularIconEnabled(show: Boolean) = viewModelScope.launch { repository.setShowPopularIcon(show) }
    fun setSplitPrivateSpaceEnabled(split: Boolean) = viewModelScope.launch { repository.setSplitPrivateSpace(split) }
    fun saveQuotes(list: List<String>) = viewModelScope.launch { 
        repository.saveQuotes(list)
        refreshApps()
    }

    fun expandNotifications() = repository.expandNotifications()
    fun lockScreen() = TrayAccessibilityService.instance?.lockScreen()
    fun requestUnlockPrivateSpace() {
        repository.requestUnlockPrivateSpace()
        viewModelScope.launch {
            // Rapid refreshes to catch the state change as soon as possible
            val delays = listOf(500L, 1000L, 2000L, 3000L)
            delays.forEach {
                kotlinx.coroutines.delay(it)
                refreshApps()
            }
        }
    }

    fun addWidget(id: Int, providerName: String? = null) {
        viewModelScope.launch {
            val current = widgets.value.toMutableList()
            current.add(LauncherWidget(id, providerName = providerName))
            saveWidgets(current)
        }
    }
    fun removeWidget(id: Int) = viewModelScope.launch { saveWidgets(widgets.value.filter { it.id != id }) }
    fun updateWidgetHeight(id: Int, newHeight: Int) = viewModelScope.launch { saveWidgets(widgets.value.map { if (it.id == id) it.copy(height = newHeight.coerceAtLeast(100)) else it }) }
    fun moveWidget(from: Int, to: Int) = viewModelScope.launch {
        val current = widgets.value.toMutableList()
        if (from in current.indices && to in current.indices) {
            current.add(to, current.removeAt(from))
            saveWidgets(current)
        }
    }
    private suspend fun saveWidgets(list: List<LauncherWidget>) = repository.saveWidgetData(list.joinToString(",") { "${it.id}:${it.height}:${it.providerName ?: ""}" })

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            repository.incrementUsage(packageName)
            repository.launchApp(packageName)
        }
    }
}
