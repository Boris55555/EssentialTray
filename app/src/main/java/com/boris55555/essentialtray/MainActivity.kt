package com.boris55555.essentialtray

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {
    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var pickWidgetLauncher: ActivityResultLauncher<Intent>
    private lateinit var bindWidgetLauncher: ActivityResultLauncher<Intent>
    private lateinit var viewModel: LauncherViewModel
    private var navController: NavHostController? = null

    private val profileReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (::viewModel.isInitialized) {
                viewModel.refreshApps()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(android.content.Intent.ACTION_MANAGED_PROFILE_REMOVED)
            addAction(android.content.Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(android.content.Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(android.content.Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                addAction(android.content.Intent.ACTION_PROFILE_ACCESSIBLE)
                addAction(android.content.Intent.ACTION_PROFILE_INACCESSIBLE)
            }
        }
        registerReceiver(profileReceiver, filter)

        appWidgetHost = AppWidgetHost(this, 1024)
        appWidgetManager = AppWidgetManager.getInstance(this)

        pickWidgetLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val widgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                if (widgetId != -1) {
                    val info = appWidgetManager.getAppWidgetInfo(widgetId)
                    viewModel.addWidget(widgetId, info?.provider?.className)
                }
            } else {
                val widgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                if (widgetId != -1) appWidgetHost.deleteAppWidgetId(widgetId)
            }
        }

        bindWidgetLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val widgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                if (widgetId != -1) {
                    val info = appWidgetManager.getAppWidgetInfo(widgetId)
                    viewModel.addWidget(widgetId, info?.provider?.className)
                }
            }
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    viewModel = viewModel()
                    val fontColorInt by viewModel.fontColor.collectAsState()
                    val themeColor = Color(fontColorInt)
                    
                    val currentNavController = rememberNavController()
                    navController = currentNavController
                    
                    LauncherNavHost(
                        navController = currentNavController, 
                        viewModel = viewModel, 
                        appWidgetHost = appWidgetHost,
                        onSelectWidget = { providerInfo ->
                            val widgetId = appWidgetHost.allocateAppWidgetId()
                            val allowed = appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, providerInfo.provider)
                            if (allowed) {
                                viewModel.addWidget(widgetId, providerInfo.provider.className)
                                currentNavController.popBackStack()
                            } else {
                                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
                                }
                                bindWidgetLauncher.launch(intent)
                                currentNavController.popBackStack()
                            }
                        },
                        onPickWidgetNative = {
                            val appWidgetId = appWidgetHost.allocateAppWidgetId()
                            val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            pickWidgetLauncher.launch(pickIntent)
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appWidgetHost.startListening()
    }

    override fun onRestart() {
        super.onRestart()
        val route = navController?.currentBackStackEntry?.destination?.route
        if (route == "widgets" || route == "top_apps" || route == "all_apps") {
            navController?.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.refreshApps()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::viewModel.isInitialized) {
            viewModel.refreshApps()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(profileReceiver)
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost.stopListening()
    }
}

@Composable
fun LauncherNavHost(
    navController: NavHostController, 
    viewModel: LauncherViewModel,
    appWidgetHost: AppWidgetHost,
    onSelectWidget: (AppWidgetProviderInfo) -> Unit,
    onPickWidgetNative: () -> Unit
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val enableWidgets by viewModel.enableWidgets.collectAsState()
            val enableTopApps by viewModel.enableTopApps.collectAsState()
            HomeScreen(
                viewModel = viewModel,
                onSwipeUp = { navController.navigate("all_apps") },
                onSwipeRight = { 
                    if (enableWidgets) {
                        navController.navigate("widgets") 
                    }
                },
                onSwipeLeft = {
                    if (enableTopApps) {
                        navController.navigate("top_apps")
                    }
                },
                onOpenSettings = { navController.navigate("settings") },
                onOpenWidgets = { navController.navigate("widgets") },
                onOpenPopular = { navController.navigate("top_apps") }
            )
        }
        composable("widgets") {
            WidgetScreen(
                viewModel = viewModel,
                onSwipeLeft = { navController.popBackStack() },
                appWidgetHost = appWidgetHost
            )
        }
        composable("top_apps") {
            TopAppsScreen(
                viewModel = viewModel,
                onSwipeRight = { navController.popBackStack() }
            )
        }
        composable("all_apps") {
            AllAppsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToFavorites = { navController.navigate("settings/favorites") },
                onNavigateToCamera = { navController.navigate("settings/camera") },
                onNavigateToMaps = { navController.navigate("settings/maps") },
                onNavigateToCalendar = { navController.navigate("settings/calendar") },
                onNavigateToDateFormat = { navController.navigate("settings/date_format") },
                onNavigateToHiddenApps = { navController.navigate("settings/hidden_apps") },
                onNavigateToFontColor = { navController.navigate("settings/font_color") },
                onNavigateToPermissions = { navController.navigate("settings/permissions") },
                onNavigateToQuotes = { navController.navigate("settings/quotes") },
                onPickWidget = { navController.navigate("settings/widget_picker") },
                viewModel = viewModel
            )
        }
        composable("settings/quotes") {
            QuotesSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings/permissions") {
            PermissionsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings/font_color") {
            FontColorSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings/hidden_apps") {
            HiddenAppsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings/date_format") {
            DateFormatSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings/widget_picker") {
            WidgetPickerScreen(
                viewModel = viewModel,
                onSelect = onSelectWidget,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings/favorites") {
            FavoritesSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings/camera") {
            AppSelectionScreen(
                title = "Select Camera App",
                appsFlow = viewModel.cameraApps,
                selectedPackageFlow = viewModel.selectedCameraPackage,
                onSelect = { viewModel.setCameraPackage(it) },
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
        composable("settings/maps") {
            AppSelectionScreen(
                title = "Select Maps App",
                appsFlow = viewModel.mapsApps,
                selectedPackageFlow = viewModel.selectedMapsPackage,
                onSelect = { viewModel.setMapsPackage(it) },
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
        composable("settings/calendar") {
            AppSelectionScreen(
                title = "Select Calendar App",
                appsFlow = viewModel.calendarApps,
                selectedPackageFlow = viewModel.selectedCalendarPackage,
                onSelect = { viewModel.setCalendarPackage(it) },
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onSwipeUp: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenPopular: () -> Unit
) {
    val favorites by viewModel.favoriteApps.collectAsState()
    val showCamera by viewModel.showCameraButton.collectAsState()
    val showMaps by viewModel.showMapsButton.collectAsState()
    val selectedCamera by viewModel.selectedCameraPackage.collectAsState()
    val selectedMaps by viewModel.selectedMapsPackage.collectAsState()
    val selectedCalendar by viewModel.selectedCalendarPackage.collectAsState()
    val dateFormat by viewModel.dateFormat.collectAsState()
    val threeFingerTapEnabled by viewModel.threeFingerTapEnabled.collectAsState()
    val showSettingsButton by viewModel.showSettingsButton.collectAsState()
    val longPressEnabled by viewModel.longPressActionEnabled.collectAsState()
    val doubleTapSleepEnabled by viewModel.doubleTapSleepEnabled.collectAsState()
    val showAllAppsIcon by viewModel.showAllAppsIcon.collectAsState()
    val enableCalendarEvents by viewModel.enableCalendarEvents.collectAsState()
    val todayEvents by viewModel.todayEvents.collectAsState()
    val enableQuotes by viewModel.enableQuotes.collectAsState()
    val currentQuote by viewModel.currentQuote.collectAsState()
    val switchCameraMaps by viewModel.switchCameraMaps.collectAsState()
    val showWidgetsIcon by viewModel.showWidgetsIcon.collectAsState()
    val showPopularIcon by viewModel.showPopularIcon.collectAsState()
    val enableWidgets by viewModel.enableWidgets.collectAsState()
    val enableTopApps by viewModel.enableTopApps.collectAsState()
    val fontColorInt by viewModel.fontColor.collectAsState()
    val themeColor = Color(fontColorInt)
    
    val dateText = remember(dateFormat) {
        try {
            java.text.SimpleDateFormat(dateFormat, java.util.Locale.getDefault()).format(java.util.Date())
        } catch (e: Exception) {
            "Error in format"
        }
    }

    var offsetY by remember { mutableFloatStateOf(0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .draggable(
                state = rememberDraggableState { delta ->
                    offsetY += delta
                },
                orientation = Orientation.Vertical,
                onDragStopped = {
                    if (offsetY < -200) {
                        onSwipeUp()
                    }
                    offsetY = 0f
                }
            )
            .draggable(
                state = rememberDraggableState { delta ->
                    offsetX += delta
                },
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    if (offsetX > 200) {
                        onSwipeRight()
                    } else if (offsetX < -200) {
                        onSwipeLeft()
                    }
                    offsetX = 0f
                }
            )
            .pointerInput(threeFingerTapEnabled, longPressEnabled, doubleTapSleepEnabled) {
                if (threeFingerTapEnabled || longPressEnabled || doubleTapSleepEnabled) {
                    // We use a single pointerInput to manage both gesture types to avoid conflicts
                    coroutineScope {
                        if (longPressEnabled || doubleTapSleepEnabled) {
                            launch {
                                detectTapGestures(
                                    onLongPress = {
                                        if (longPressEnabled) viewModel.expandNotifications()
                                    },
                                    onDoubleTap = {
                                        if (doubleTapSleepEnabled) viewModel.lockScreen()
                                    }
                                )
                            }
                        }
                        
                        if (threeFingerTapEnabled) {
                            launch {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.size == 3 && event.changes.any { it.pressed && !it.previousPressed }) {
                                            onOpenSettings()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Text(
            text = dateText,
            color = themeColor,
            fontSize = 24.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
                .clickable { selectedCalendar?.let { viewModel.launchApp(it) } }
        )

        if (enableCalendarEvents && todayEvents.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp, start = 32.dp, end = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                todayEvents.take(3).forEach { event ->
                    Text(
                        text = event,
                        color = themeColor.copy(alpha = 0.7f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                if (todayEvents.size > 3) {
                    Text(
                        text = "+ ${todayEvents.size - 3} more",
                        color = themeColor.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        } else if (enableQuotes && currentQuote != null) {
            Text(
                text = currentQuote!!,
                color = themeColor.copy(alpha = 0.6f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp, start = 48.dp, end = 48.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            favorites.forEach { app ->
                val notificationCounts by viewModel.notificationCounts.collectAsState()
                val showNotifications by viewModel.showNotifications.collectAsState()
                val count = notificationCounts[app.packageName] ?: 0
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.label,
                        color = themeColor,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .clickable { viewModel.launchApp(app.packageName) }
                    )
                    if (app.isPrivate) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Private",
                            tint = themeColor.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 8.dp).size(24.dp)
                        )
                    }
                    if (showNotifications && count > 0) {
                        Text(
                            text = " ($count)",
                            color = themeColor.copy(alpha = 0.5f),
                            fontSize = 24.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        if (showSettingsButton) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = themeColor.copy(alpha = 0.3f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (showWidgetsIcon && enableWidgets) {
                    IconButton(onClick = onOpenWidgets, modifier = Modifier.size(56.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = themeColor.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                            Icon(imageVector = Icons.Default.Widgets, contentDescription = "Widgets", tint = themeColor.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                val show = if (switchCameraMaps) showCamera else showMaps
                val selected = if (switchCameraMaps) selectedCamera else selectedMaps
                val icon = if (switchCameraMaps) Icons.Default.CameraAlt else Icons.Default.Map
                val desc = if (switchCameraMaps) "Camera" else "Maps"

                if (show) {
                    IconButton(
                        onClick = { selected?.let { viewModel.launchApp(it) } },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(imageVector = icon, contentDescription = desc, tint = themeColor, modifier = Modifier.size(32.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.size(56.dp))
                }
            }

            if (showAllAppsIcon) {
                IconButton(
                    onClick = onSwipeUp,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps, 
                        contentDescription = "All Apps", 
                        tint = themeColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                // To keep spacing consistent if middle is hidden but others visible
                Spacer(modifier = Modifier.size(56.dp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (showPopularIcon && enableTopApps) {
                    IconButton(onClick = onOpenPopular, modifier = Modifier.size(56.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = "Popular", tint = themeColor.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = themeColor.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                val show = if (switchCameraMaps) showMaps else showCamera
                val selected = if (switchCameraMaps) selectedMaps else selectedCamera
                val icon = if (switchCameraMaps) Icons.Default.Map else Icons.Default.CameraAlt
                val desc = if (switchCameraMaps) "Maps" else "Camera"

                if (show) {
                    IconButton(
                        onClick = { selected?.let { viewModel.launchApp(it) } },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(imageVector = icon, contentDescription = desc, tint = themeColor, modifier = Modifier.size(32.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.size(56.dp))
                }
            }
        }
    }
}

@Composable
fun WidgetScreen(
    viewModel: LauncherViewModel,
    onSwipeLeft: () -> Unit,
    appWidgetHost: AppWidgetHost
) {
    val widgets by viewModel.widgets.collectAsState()
    val fontColorInt by viewModel.fontColor.collectAsState()
    val themeColor = Color(fontColorInt)
    
    var offsetX by remember { mutableFloatStateOf(0f) }
    var globalEditMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            // Removed navigationBarsPadding to bring widgets closer to bottom if needed, 
            // but statusBarsPadding is kept for top. 
            // To bring closer to top, we can use a smaller padding in Column.
            .draggable(
                state = rememberDraggableState { delta ->
                    offsetX += delta
                },
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    if (offsetX < -200) {
                        onSwipeLeft()
                    }
                    offsetX = 0f
                }
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 8.dp), // Reduced top padding
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Widgets", color = themeColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { globalEditMode = !globalEditMode }) {
                    Icon(
                        imageVector = if (globalEditMode) Icons.Default.Close else Icons.Default.Settings,
                        contentDescription = "Toggle Edit Mode",
                        tint = if (globalEditMode) themeColor else themeColor.copy(alpha = 0.5f)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 8.dp) // Removed horizontal padding
            ) {
                itemsIndexed(widgets) { index, widget ->
                    WidgetContainer(
                        widget = widget,
                        index = index,
                        totalCount = widgets.size,
                        appWidgetHost = appWidgetHost,
                        isEditModeGlobal = globalEditMode,
                        onResize = { newHeight -> viewModel.updateWidgetHeight(widget.id, newHeight) },
                        onDelete = { viewModel.removeWidget(widget.id) },
                        onMove = { from, to -> viewModel.moveWidget(from, to) },
                        themeColor = themeColor
                    )
                    Spacer(modifier = Modifier.height(8.dp)) // Reduced spacer
                }
            }
        }
    }
}

@Composable
fun WidgetContainer(
    widget: LauncherWidget,
    index: Int,
    totalCount: Int,
    appWidgetHost: AppWidgetHost,
    isEditModeGlobal: Boolean,
    onResize: (Int) -> Unit,
    onDelete: () -> Unit,
    onMove: (Int, Int) -> Unit,
    themeColor: Color = Color.White
) {
    val context = LocalContext.current
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val info = remember(widget.id) { appWidgetManager.getAppWidgetInfo(widget.id) }
    var height by remember { mutableStateOf(widget.height.dp) }
    var isActivatedLocal by remember { mutableStateOf(false) }
    
    val isActivated = isEditModeGlobal || isActivatedLocal

    if (info != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isActivated) themeColor.copy(alpha = 0.1f) else Color.Transparent)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { isActivatedLocal = true },
                        onTap = { if (isActivatedLocal) isActivatedLocal = false }
                    )
                }
                .padding(vertical = 4.dp) // Only vertical padding for the container
        ) {
            if (isActivated) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (index > 0) {
                        IconButton(onClick = { onMove(index, index - 1) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = Color.Gray)
                        }
                    }
                    if (index < totalCount - 1) {
                        IconButton(onClick = { onMove(index, index + 1) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = Color.Gray)
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.6f))
                    }
                    if (!isEditModeGlobal) {
                        IconButton(onClick = { isActivatedLocal = false }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Done", tint = themeColor)
                        }
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
            ) {
                AndroidView(
                    factory = { ctx ->
                        appWidgetHost.createView(ctx, widget.id, info).apply {
                            setAppWidget(widget.id, info)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isActivated) {
                    // Resize handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .align(Alignment.BottomCenter)
                            .background(Color.White.copy(alpha = 0.05f))
                            .draggable(
                                state = rememberDraggableState { delta ->
                                    val newHeight = (height.value + delta).coerceIn(100f, 800f)
                                    height = newHeight.dp
                                },
                                orientation = Orientation.Vertical,
                                onDragStopped = {
                                    onResize(height.value.toInt())
                                }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp, 4.dp)
                                .background(Color.Gray.copy(alpha = 0.5f))
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopAppsScreen(
    viewModel: LauncherViewModel,
    onSwipeRight: () -> Unit
) {
    val topApps by viewModel.topApps.collectAsState()
    val fontColorInt by viewModel.fontColor.collectAsState()
    val themeColor = Color(fontColorInt)
    
    var offsetX by remember { mutableFloatStateOf(0f) }
    var selectedAppForHide by remember { mutableStateOf<AppInfo?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .draggable(
                state = rememberDraggableState { delta ->
                    offsetX += delta
                },
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    if (offsetX > 200) {
                        onSwipeRight()
                    }
                    offsetX = 0f
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Popular", 
                color = themeColor, 
                fontSize = 24.sp, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            if (topApps.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Start using apps to see them here", color = Color.Gray, fontSize = 18.sp)
                }
            } else {
                topApps.forEach { app ->
                    val notificationCounts by viewModel.notificationCounts.collectAsState()
                    val showNotifications by viewModel.showNotifications.collectAsState()
                    val count = notificationCounts[app.packageName] ?: 0
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.label,
                            color = themeColor,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .pointerInput(app) {
                                    detectTapGestures(
                                        onTap = { viewModel.launchApp(app.packageName) },
                                        onLongPress = { selectedAppForHide = app }
                                    )
                                }
                        )
                        if (app.isPrivate) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Private",
                                tint = themeColor.copy(alpha = 0.5f),
                                modifier = Modifier.padding(start = 8.dp).size(20.dp)
                            )
                        }
                        if (showNotifications && count > 0) {
                            Text(
                                text = " ($count)",
                                color = themeColor.copy(alpha = 0.5f),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        if (selectedAppForHide != null) {
            AlertDialog(
                onDismissRequest = { selectedAppForHide = null },
                containerColor = Color(0xFF1A1A1A),
                title = { Text("Hide from popular apps", color = themeColor) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.togglePopularVisibility(selectedAppForHide!!.packageName)
                        selectedAppForHide = null
                    }) {
                        Text("Ok", color = themeColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedAppForHide = null }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
fun AppList(
    apps: List<AppInfo>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    themeColor: Color,
    viewModel: LauncherViewModel,
    context: android.content.Context,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditApp: (AppInfo) -> Unit
) {
    val scope = rememberCoroutineScope()
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 32.dp, 
                end = 56.dp, 
                top = 16.dp, 
                bottom = 16.dp
            )
        ) {
            items(apps) { app ->
                val notificationCounts by viewModel.notificationCounts.collectAsState()
                val showNotifications by viewModel.showNotifications.collectAsState()
                val count = notificationCounts[app.packageName] ?: 0
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.label,
                        color = themeColor,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .pointerInput(app) {
                                detectTapGestures(
                                    onTap = {
                                    if (app.packageName == context.packageName) {
                                        onOpenSettings()
                                    } else {
                                        viewModel.launchApp(app.packageName)
                                        onBack()
                                    }
                                },
                                    onLongPress = {
                                        onEditApp(app)
                                    }
                                )
                            }
                            .padding(vertical = 12.dp)
                    )
                    if (app.isPrivate) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Private",
                            tint = themeColor.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 8.dp).size(16.dp)
                        )
                    }
                    if (showNotifications && count > 0) {
                        Text(
                            text = " ($count)",
                            color = themeColor.copy(alpha = 0.5f),
                            fontSize = 24.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        // Custom Scrollbar
        if (apps.size > 1) {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visibleItems = layoutInfo.visibleItemsInfo.size
            
            if (totalItems > visibleItems) {
                val scrollbarAreaHeight = remember { mutableStateOf(0f) }
                
                // Calculate thumb size and position
                val thumbHeightPercent = (visibleItems.toFloat() / totalItems.toFloat()).coerceIn(0.1f, 1f)
                
                val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
                val scrollPercent = if (firstVisibleItem != null) {
                    val itemOffset = firstVisibleItem.offset.toFloat()
                    val itemHeight = firstVisibleItem.size.toFloat()
                    val offsetInItem = (-itemOffset / itemHeight).coerceIn(0f, 1f)
                    (firstVisibleItem.index.toFloat() + offsetInItem) / totalItems.toFloat()
                } else 0f

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .fillMaxHeight(0.9f)
                        .width(20.dp) // Wider touch area
                        .onGloballyPositioned { scrollbarAreaHeight.value = it.size.height.toFloat() }
                        .pointerInput(totalItems) {
                            detectTapGestures { offset ->
                                val clickedPercent = (offset.y / size.height).coerceIn(0f, 1f)
                                val targetIndex = (clickedPercent * totalItems).toInt().coerceIn(0, totalItems - 1)
                                scope.launch { listState.scrollToItem(targetIndex) }
                            }
                        }
                        .pointerInput(totalItems) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val dragPercent = (change.position.y / size.height).coerceIn(0f, 1f)
                                val targetIndex = (dragPercent * totalItems).toInt().coerceIn(0, totalItems - 1)
                                scope.launch { listState.scrollToItem(targetIndex) }
                            }
                        }
                ) {
                    // Track
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(2.dp)
                            .background(Color.Gray.copy(alpha = 0.2f))
                            .align(Alignment.Center)
                    )
                    // Thumb
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .fillMaxHeight(thumbHeightPercent)
                            .graphicsLayer {
                                translationY = scrollPercent * scrollbarAreaHeight.value
                            }
                            .background(themeColor.copy(alpha = 0.6f), shape = androidx.compose.foundation.shape.CircleShape)
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
fun AllAppsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val apps by viewModel.displayApps.collectAsState()
    val hiddenApps by viewModel.hiddenApps.collectAsState()
    val fontColorInt by viewModel.fontColor.collectAsState()
    val themeColor = Color(fontColorInt)
    
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    var selectedAppForEdit by remember { mutableStateOf<AppInfo?>(null) }
    var newLabel by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }
    var currentAppTags by remember { mutableStateOf(emptyList<String>()) }
    var searchQuery by remember { mutableStateOf("") }
    
    val allUniqueTags by viewModel.allUniqueTags.collectAsState()

    androidx.compose.runtime.LaunchedEffect(selectedAppForEdit) {
        if (selectedAppForEdit != null) {
            currentAppTags = selectedAppForEdit!!.tags
        }
    }
    
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isEmpty()) apps
        else apps.filter { 
            it.label.contains(searchQuery, ignoreCase = true) || 
            it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    val alphabet = remember(apps) {
        apps.mapNotNull { it.label.firstOrNull()?.uppercase()?.toString() }
            .distinct()
            .sorted()
    }
    var sidebarTouchY by remember { mutableStateOf<Float?>(null) }
    var sidebarHeight by remember { mutableStateOf(1f) }
    
    val showPrivateSpace by viewModel.showPrivateSpace.collectAsState()
    val splitPrivateSpace by viewModel.splitPrivateSpace.collectAsState()
    val hasPrivateSpace by viewModel.hasPrivateSpace.collectAsState()
    val isLocked by viewModel.isPrivateSpaceLocked.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshApps()
    }

    val normalApps = remember(filteredApps, splitPrivateSpace) {
        if (splitPrivateSpace) filteredApps.filter { !it.isPrivate } else filteredApps
    }
    val privateApps = remember(filteredApps, splitPrivateSpace) {
        if (splitPrivateSpace) filteredApps.filter { it.isPrivate } else emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                if (splitPrivateSpace && !isLocked && privateApps.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1.2f)) {
                            AppList(
                                apps = normalApps,
                                listState = listState,
                                themeColor = themeColor,
                                viewModel = viewModel,
                                context = context,
                                onBack = onBack,
                                onOpenSettings = onOpenSettings,
                                onEditApp = { app: AppInfo ->
                                    selectedAppForEdit = app
                                    newLabel = app.label
                                }
                            )
                        }
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
                        Box(modifier = Modifier.weight(0.8f)) {
                            AppList(
                                apps = privateApps,
                                listState = androidx.compose.foundation.lazy.rememberLazyListState(),
                                themeColor = themeColor,
                                viewModel = viewModel,
                                context = context,
                                onBack = onBack,
                                onOpenSettings = onOpenSettings,
                                onEditApp = { app: AppInfo ->
                                    selectedAppForEdit = app
                                    newLabel = app.label
                                }
                            )
                        }
                    }
                } else {
                    AppList(
                        apps = filteredApps,
                        listState = listState,
                        themeColor = themeColor,
                        viewModel = viewModel,
                        context = context,
                        onBack = onBack,
                        onOpenSettings = onOpenSettings,
                        onEditApp = { app: AppInfo ->
                            selectedAppForEdit = app
                            newLabel = app.label
                        }
                    )
                }

                // Alphabet sidebar (only show if not searching or if search is empty and not split)
                if (searchQuery.isEmpty() && !splitPrivateSpace) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                            .fillMaxHeight()
                            .width(40.dp)
                            .onGloballyPositioned { sidebarHeight = it.size.height.toFloat() }
                            .pointerInput(apps) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.first()
                                        if (change.pressed) {
                                            sidebarTouchY = change.position.y
                                            
                                            // Adjust for the 0.8f scale of the internal column
                                            val adjustedY = sidebarTouchY!! - (sidebarHeight * 0.1f)
                                            val columnHeight = sidebarHeight * 0.8f
                                            
                                            val letterIndex = ((adjustedY / columnHeight) * alphabet.size)
                                                .toInt()
                                                .coerceIn(0, alphabet.size - 1)
                                            val letter = alphabet[letterIndex]
                                            val scrollIndex = filteredApps.indexOfFirst { it.label.uppercase().startsWith(letter) }
                                            if (scrollIndex != -1) {
                                                scope.launch { listState.scrollToItem(scrollIndex) }
                                            }
                                        } else {
                                            sidebarTouchY = null
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxHeight(0.8f) // Avoid overlapping with search bar
                        ) {
                            val itemHeight = (sidebarHeight * 0.8f) / alphabet.size
                            alphabet.forEachIndexed { index, letter ->
                                Text(
                                    text = letter,
                                    color = themeColor.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .graphicsLayer {
                                            sidebarTouchY?.let { touchY ->
                                                // Center of the item in the 0.8f column
                                                val itemCenterY = (sidebarHeight * 0.1f) + index * itemHeight + itemHeight / 2
                                                val distance = (touchY - itemCenterY).absoluteValue
                                                val threshold = 150f
                                                if (distance < threshold) {
                                                    val factor = (1f - distance / threshold)
                                                    translationX = -factor * 60f // Move even more left
                                                    scaleX = 1f + factor * 0.8f // Larger scale
                                                    scaleY = 1f + factor * 0.8f
                                                    alpha = 0.5f + factor * 0.5f
                                                }
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Search bar at the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showPrivateSpace && (hasPrivateSpace || android.os.Build.VERSION.SDK_INT >= 35)) {
                    IconButton(
                        onClick = { viewModel.requestUnlockPrivateSpace() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Private Space",
                            tint = themeColor.copy(alpha = 0.8f)
                        )
                    }
                }
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search apps...", color = themeColor.copy(alpha = 0.3f)) },
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = themeColor.copy(alpha = 0.5f)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = themeColor,
                        unfocusedTextColor = themeColor,
                        focusedBorderColor = themeColor.copy(alpha = 0.5f),
                        unfocusedBorderColor = themeColor.copy(alpha = 0.1f)
                    )
                )
            }
        }

        if (selectedAppForEdit != null) {
            val context = LocalContext.current
            AlertDialog(
                onDismissRequest = { selectedAppForEdit = null },
                containerColor = Color(0xFF1A1A1A),
                title = { 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Edit App", color = themeColor)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isHidden = hiddenApps.contains(selectedAppForEdit!!.packageName)
                            IconButton(
                                onClick = {
                                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.fromParts("package", selectedAppForEdit!!.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "App Info",
                                    tint = themeColor.copy(alpha = 0.9f)
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.toggleAppVisibility(selectedAppForEdit!!.packageName)
                                }
                            ) {
                                Icon(
                                    imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Visibility",
                                    tint = themeColor.copy(alpha = 0.9f)
                                )
                            }

                            if (viewModel.canUninstall(selectedAppForEdit!!.packageName)) {
                                IconButton(
                                    onClick = {
                                        val pkg = selectedAppForEdit?.packageName
                                        if (pkg != null) {
                                            val uri = android.net.Uri.fromParts("package", pkg, null)
                                            val uninstallIntent = Intent(Intent.ACTION_DELETE, uri)
                                            // Standard way to launch uninstaller from Activity
                                            context.startActivity(uninstallIntent)
                                        }
                                        selectedAppForEdit = null
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Uninstall",
                                        tint = themeColor.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newLabel,
                            onValueChange = { newLabel = it },
                            label = { Text("App Name", color = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = themeColor,
                                unfocusedTextColor = themeColor,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Tags (space separated)", color = Color.Gray, fontSize = 12.sp)
                        
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { input ->
                                if (input.endsWith(" ")) {
                                    val newTag = input.trim()
                                    if (newTag.isNotEmpty() && !currentAppTags.contains(newTag)) {
                                        currentAppTags = currentAppTags + newTag
                                    }
                                    tagInput = ""
                                } else {
                                    tagInput = input
                                }
                            },
                            label = { Text("Add tag...", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = themeColor,
                                unfocusedTextColor = themeColor,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        
                        // Current tags display
                        if (currentAppTags.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                currentAppTags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .background(themeColor.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .clickable { 
                                                currentAppTags = currentAppTags - tag
                                            }
                                    ) {
                                        Text(tag, color = themeColor, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Suggestions
                        val lastTag = tagInput.trim()
                        if (lastTag.isNotEmpty()) {
                            val suggestions = allUniqueTags.filter { it.startsWith(lastTag, ignoreCase = true) && !currentAppTags.contains(it) }
                            if (suggestions.isNotEmpty()) {
                                Text("Suggestions:", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    suggestions.take(3).forEach { suggestion ->
                                        Text(
                                            text = suggestion,
                                            color = themeColor,
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .background(themeColor.copy(alpha = 0.2f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                                .clickable {
                                                    currentAppTags = currentAppTags + suggestion
                                                    tagInput = ""
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.renameApp(selectedAppForEdit!!.packageName, newLabel)
                        // Process tags: current tags + any remaining text in tagInput
                        val finalTags = (currentAppTags + tagInput.trim().split(" ")).filter { it.isNotBlank() }.distinct()
                        viewModel.setAppTags(selectedAppForEdit!!.packageName, finalTags)
                        
                        selectedAppForEdit = null
                        tagInput = ""
                        currentAppTags = emptyList()
                    }) {
                        Text("Save", color = themeColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedAppForEdit = null }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToMaps: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToDateFormat: () -> Unit,
    onNavigateToHiddenApps: () -> Unit,
    onNavigateToFontColor: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToQuotes: () -> Unit,
    onPickWidget: () -> Unit,
    viewModel: LauncherViewModel
) {
    val showCamera by viewModel.showCameraButton.collectAsState()
    val showMaps by viewModel.showMapsButton.collectAsState()
    val enableWidgets by viewModel.enableWidgets.collectAsState()
    val showNotifications by viewModel.showNotifications.collectAsState()
    val selectedCamera by viewModel.selectedCameraPackage.collectAsState()
    val selectedMaps by viewModel.selectedMapsPackage.collectAsState()
    val selectedCalendar by viewModel.selectedCalendarPackage.collectAsState()
    val enableCalendarEvents by viewModel.enableCalendarEvents.collectAsState()
    val currentDateFormat by viewModel.dateFormat.collectAsState()
    val apps by viewModel.installedApps.collectAsState()
    val fontColorInt by viewModel.fontColor.collectAsState()
    val themeColor = Color(fontColorInt)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Control Panel", color = themeColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        SettingsMenuItem("Favorites", "Manage home screen apps", onNavigateToFavorites, themeColor = themeColor)
        
        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
        
        SettingsMenuItem("Hidden Apps", "Manage hidden applications", onNavigateToHiddenApps, themeColor = themeColor)
        
        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        SettingsMenuItem("Permissions", "Manage app permissions", onNavigateToPermissions, themeColor = themeColor)
        
        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
        
        SettingsMenuItem("Date Format", currentDateFormat, onNavigateToDateFormat, themeColor = themeColor)

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        SettingsMenuItem("Font Color", "Choose theme color", onNavigateToFontColor, themeColor = themeColor)

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
        
        SettingsToggleItem(
            title = "Show Notifications",
            checked = showNotifications,
            onCheckedChange = { viewModel.setShowNotifications(it) },
            themeColor = themeColor
        )

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        SettingsToggleItem(
            title = "Show Camera Button",
            checked = showCamera,
            onCheckedChange = { viewModel.setShowCameraButton(it) },
            themeColor = themeColor
        )
        if (showCamera) {
            val appName = apps.find { it.packageName == selectedCamera }?.label ?: "Select app"
            SettingsMenuItem("Camera App", appName, onNavigateToCamera, indent = true, themeColor = themeColor)
        }

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        SettingsToggleItem(
            title = "Show Maps Button",
            checked = showMaps,
            onCheckedChange = { viewModel.setShowMapsButton(it) },
            themeColor = themeColor
        )
        if (showMaps) {
            val appName = apps.find { it.packageName == selectedMaps }?.label ?: "Select app"
            SettingsMenuItem("Maps App", appName, onNavigateToMaps, indent = true, themeColor = themeColor)
        }

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        SettingsToggleItem(
            title = "Enable Widgets (Swipe Right)",
            checked = enableWidgets,
            onCheckedChange = { viewModel.setEnableWidgets(it) },
            themeColor = themeColor
        )
        if (enableWidgets) {
            SettingsMenuItem("Add Widgets +", "Add a new widget to the widget screen", onPickWidget, indent = true, themeColor = themeColor)
        }

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        val enableTopApps by viewModel.enableTopApps.collectAsState()
        SettingsToggleItem(
            title = "Enable Top Apps (Swipe Left)",
            checked = enableTopApps,
            onCheckedChange = { viewModel.setEnableTopApps(it) },
            themeColor = themeColor
        )

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        val showPopularIcon by viewModel.showPopularIcon.collectAsState()
        SettingsToggleItem(
            title = "Show Popular Apps Button",
            checked = showPopularIcon,
            onCheckedChange = { viewModel.setShowPopularIconEnabled(it) },
            themeColor = themeColor
        )

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        val showWidgetsIcon by viewModel.showWidgetsIcon.collectAsState()
        SettingsToggleItem(
            title = "Show Widgets Screen Button",
            checked = showWidgetsIcon,
            onCheckedChange = { viewModel.setShowWidgetsIconEnabled(it) },
            themeColor = themeColor
        )

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        val showPrivateSpace by viewModel.showPrivateSpace.collectAsState()
        SettingsToggleItem(
            title = "Show Private Space (Android 15+)",
            checked = showPrivateSpace,
            onCheckedChange = { viewModel.setShowPrivateSpace(it) },
            themeColor = themeColor
        )

        if (showPrivateSpace) {
            val splitPrivateSpace by viewModel.splitPrivateSpace.collectAsState()
            SettingsToggleItem(
                title = "Split Private Space Apps",
                checked = splitPrivateSpace,
                onCheckedChange = { viewModel.setSplitPrivateSpaceEnabled(it) },
                indent = true,
                themeColor = themeColor
            )
        }

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        val threeFingerTapEnabled by viewModel.threeFingerTapEnabled.collectAsState()
        SettingsToggleItem(
            title = "Enable Three Finger Tap Settings",
            checked = threeFingerTapEnabled,
            onCheckedChange = { viewModel.setThreeFingerTapEnabled(it) },
            themeColor = themeColor
        )

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        val showSettingsButton by viewModel.showSettingsButton.collectAsState()
        SettingsToggleItem(
            title = "Show Control Panel Icon on Home",
            checked = showSettingsButton,
            onCheckedChange = { viewModel.setShowSettingsButton(it) },
            themeColor = themeColor
        )

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        val showAllAppsIconSetting by viewModel.showAllAppsIcon.collectAsState()
        SettingsToggleItem(
            title = "Show All Apps Icon on Home",
            checked = showAllAppsIconSetting,
            onCheckedChange = { viewModel.setShowAllAppsIcon(it) },
            themeColor = themeColor
        )

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        val switchCameraMapsSetting by viewModel.switchCameraMaps.collectAsState()
        SettingsToggleItem(
            title = "Switch Camera & Maps buttons",
            checked = switchCameraMapsSetting,
            onCheckedChange = { viewModel.setSwitchCameraMapsEnabled(it) },
            themeColor = themeColor
        )

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        val longPressEnabled by viewModel.longPressActionEnabled.collectAsState()
        SettingsToggleItem(
            title = "Enable Long Press to Show Notifications",
            checked = longPressEnabled,
            onCheckedChange = { viewModel.setLongPressActionEnabled(it) },
            themeColor = themeColor
        )

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        val doubleTapSleepEnabled by viewModel.doubleTapSleepEnabled.collectAsState()
        SettingsToggleItem(
            title = "Double Tap to Sleep",
            checked = doubleTapSleepEnabled,
            onCheckedChange = { viewModel.setDoubleTapSleepEnabled(it) },
            themeColor = themeColor
        )

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        val enableCalendarEvents by viewModel.enableCalendarEvents.collectAsState()
        SettingsToggleItem(
            title = "Enable Calendar Text",
            checked = enableCalendarEvents,
            onCheckedChange = { viewModel.setEnableCalendarEvents(it) },
            themeColor = themeColor
        )

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        val enableQuotes by viewModel.enableQuotes.collectAsState()
        SettingsToggleItem(
            title = "Enable Quotes",
            checked = enableQuotes,
            onCheckedChange = { viewModel.setEnableQuotes(it) },
            themeColor = themeColor
        )
        if (enableQuotes) {
            SettingsMenuItem("Add Quotes", "Manage your personal quotes", onNavigateToQuotes, indent = true, themeColor = themeColor)
        }

        HorizontalDivider(color = themeColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        val appName = apps.find { it.packageName == selectedCalendar }?.label ?: "Select app"
        SettingsMenuItem("Calendar App", appName, onNavigateToCalendar, themeColor = themeColor)

        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            "Back", 
            color = themeColor, 
            modifier = Modifier
                .clickable { onBack() }
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun SettingsMenuItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    indent: Boolean = false,
    themeColor: Color = Color.White
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
            .padding(start = if (indent) 32.dp else 0.dp)
    ) {
        Text(title, color = themeColor, fontSize = 18.sp)
        Text(subtitle, color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    indent: Boolean = false,
    themeColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .padding(start = if (indent) 32.dp else 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = themeColor, fontSize = 18.sp)
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = themeColor,
                checkedTrackColor = themeColor.copy(alpha = 0.2f),
                checkedBorderColor = themeColor,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun PermissionsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val fontColorInt by viewModel.fontColor.collectAsState()
    val themeColor = Color(fontColorInt)

    // Helper to check if Notification Access is granted
    fun isNotificationServiceEnabled(): Boolean {
        val pkgName = context.packageName
        val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat?.contains(pkgName) == true
    }

    // Helper to check if Accessibility Service is enabled
    fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = ComponentName(context, TrayAccessibilityService::class.java)
        val enabledServices = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(expectedComponentName.flattenToString()) == true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Text("Permissions", color = themeColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(32.dp))

        PermissionItem(
            title = "Notification Access",
            description = "Needed for notification counters",
            isGranted = isNotificationServiceEnabled(),
            onAction = {
                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                context.startActivity(intent)
            },
            themeColor = themeColor
        )

        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

        PermissionItem(
            title = "Accessibility Service",
            description = "Needed for double tap to sleep",
            isGranted = isAccessibilityServiceEnabled(),
            onAction = {
                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            },
            themeColor = themeColor
        )

        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

        PermissionItem(
            title = "Calendar Access",
            description = "Needed for today's events text",
            isGranted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED,
            onAction = {
                // Request permission (simple way for this context)
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            themeColor = themeColor
        )

        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

        PermissionItem(
            title = "Home Screen (Launcher)",
            description = "Set as default home app",
            isGranted = true, // If we are here, we are running, but ideally check if default
            onAction = {
                val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                context.startActivity(intent)
            },
            themeColor = themeColor
        )

        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            "Back", 
            color = themeColor, 
            modifier = Modifier
                .clickable { onBack() }
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onAction: () -> Unit,
    themeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAction() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = themeColor, fontSize = 18.sp)
            Text(description, color = Color.Gray, fontSize = 14.sp)
        }
        Text(
            text = if (isGranted) "Granted" else "Grant",
            color = if (isGranted) Color.Green else Color.Red,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FontColorSettingsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val fontColorInt by viewModel.fontColor.collectAsState()
    val themeColor = Color(fontColorInt)
    
    val colors = listOf(
        "White" to android.graphics.Color.WHITE,
        "Silver" to android.graphics.Color.LTGRAY,
        "Gray" to android.graphics.Color.GRAY,
        "Red" to android.graphics.Color.RED,
        "Pink" to android.graphics.Color.parseColor("#FFC0CB"),
        "Orange" to android.graphics.Color.parseColor("#FFA500"),
        "Yellow" to android.graphics.Color.YELLOW,
        "Green" to android.graphics.Color.GREEN,
        "Blue" to android.graphics.Color.BLUE,
        "Cyan" to android.graphics.Color.CYAN,
        "Magenta" to android.graphics.Color.MAGENTA,
        "Mint" to android.graphics.Color.parseColor("#98FF98"),
        "Lavender" to android.graphics.Color.parseColor("#E6E6FA")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Text("Font Color", color = themeColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(colors) { (name, colorInt) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setFontColor(colorInt) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(colorInt))
                    )
                    Text(
                        text = name, 
                        color = themeColor, 
                        modifier = Modifier.padding(start = 16.dp),
                        fontWeight = if (fontColorInt == colorInt) FontWeight.Bold else FontWeight.Normal
                    )
                    if (fontColorInt == colorInt) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Selected", color = themeColor, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(color = Color.DarkGray)
            }
        }
        
        Text(
            "Back", 
            color = themeColor, 
            modifier = Modifier
                .clickable { onBack() }
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun DateFormatSettingsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val currentFormat by viewModel.dateFormat.collectAsState()
    val fontColorInt by viewModel.fontColor.collectAsState()
    val themeColor = Color(fontColorInt)
    
    val formats = listOf(
        "dd.MM.yyyy",
        "d.M.yyyy",
        "MM/dd/yyyy",
        "yyyy-MM-dd",
        "EEEE, d. MMMM",
        "E, d. MMM"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Text("Date Format", color = themeColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(formats) { format ->
                val example = remember {
                    try {
                        java.text.SimpleDateFormat(format, java.util.Locale.getDefault()).format(java.util.Date())
                    } catch (ignore: Exception) {
                        "Error"
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            viewModel.setDateFormat(format)
                            onBack()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = currentFormat == format,
                        onCheckedChange = { 
                            viewModel.setDateFormat(format)
                            onBack()
                        },
                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                            checkedColor = themeColor,
                            checkmarkColor = Color.Black
                        )
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(text = format, color = themeColor, fontSize = 16.sp)
                        Text(text = example, color = Color.Gray, fontSize = 14.sp)
                    }
                }
                HorizontalDivider(color = Color.DarkGray)
            }
        }
        
        Text(
            "Back", 
            color = themeColor, 
            modifier = Modifier
                .clickable { onBack() }
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun HiddenAppsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val hiddenApps by viewModel.allHiddenApps.collectAsState()
    val hiddenFromPopular by viewModel.hiddenFromPopular.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val fontColorInt by viewModel.fontColor.collectAsState()
    val themeColor = Color(fontColorInt)
    
    val allHiddenAppsList = remember(hiddenApps, hiddenFromPopular, installedApps) {
        val list = mutableListOf<Pair<AppInfo, String>>() // App to Type
        hiddenApps.forEach { app -> list.add(app to "all") }
        hiddenFromPopular.forEach { pkg -> 
            installedApps.find { it.packageName == pkg }?.let { app ->
                if (!hiddenApps.any { it.packageName == pkg }) {
                    list.add(app to "popular")
                }
            }
        }
        list.sortedBy { it.first.label.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Text("Hidden Apps", color = themeColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (allHiddenAppsList.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No hidden apps", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(allHiddenAppsList) { (app, type) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                if (type == "all") {
                                    viewModel.toggleAppVisibility(app.packageName)
                                } else {
                                    viewModel.togglePopularVisibility(app.packageName)
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (type == "all") {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Hidden",
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                "Top",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                modifier = Modifier.width(32.dp)
                            )
                        }
                        Text(
                            text = app.label, 
                            color = themeColor, 
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                    HorizontalDivider(color = Color.DarkGray)
                }
            }
        }
        
        Text(
            "Back", 
            color = themeColor, 
            modifier = Modifier
                .clickable { onBack() }
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun QuotesSettingsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val quotes by viewModel.quotes.collectAsState()
    val fontColorInt by viewModel.fontColor.collectAsState()
    val themeColor = Color(fontColorInt)
    var newQuote by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Text("Quotes", color = themeColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newQuote,
                onValueChange = { newQuote = it },
                label = { Text("Add new quote", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = themeColor,
                    unfocusedTextColor = themeColor,
                    focusedBorderColor = themeColor,
                    unfocusedBorderColor = Color.Gray
                )
            )
            IconButton(onClick = {
                if (newQuote.isNotBlank()) {
                    val newList = quotes + newQuote.trim()
                    viewModel.saveQuotes(newList)
                    newQuote = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = themeColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(quotes) { quote ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = quote,
                        color = themeColor,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        val newList = quotes.toMutableList().apply { remove(quote) }
                        viewModel.saveQuotes(newList)
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
                HorizontalDivider(color = Color.DarkGray)
            }
        }

        Text(
            "Back", 
            color = themeColor, 
            modifier = Modifier
                .clickable { onBack() }
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun FavoritesSettingsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val apps by viewModel.installedApps.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val fontColorInt by viewModel.fontColor.collectAsState()
    val themeColor = Color(fontColorInt)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Text("Favorites", color = themeColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Choose max 8 apps", color = themeColor, fontSize = 18.sp)
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(apps) { app ->
                val isChecked = favorites.contains(app.packageName)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            if (isChecked || favorites.size < 8) {
                                viewModel.toggleFavorite(app.packageName)
                            }
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { 
                            if (isChecked || favorites.size < 8) {
                                viewModel.toggleFavorite(app.packageName)
                            }
                        },
                        enabled = isChecked || favorites.size < 8,
                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                            checkedColor = themeColor,
                            checkmarkColor = Color.Black
                        )
                    )
                    Text(
                        text = app.label, 
                        color = if (isChecked || favorites.size < 8) themeColor else Color.Gray, 
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                HorizontalDivider(color = Color.DarkGray)
            }
        }
        
        Text(
            "Back", 
            color = themeColor, 
            modifier = Modifier
                .clickable { onBack() }
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun WidgetPickerScreen(
    viewModel: LauncherViewModel,
    onSelect: (AppWidgetProviderInfo) -> Unit,
    onBack: () -> Unit
) {
    val availableWidgets by viewModel.availableWidgets.collectAsState()
    val addedWidgets by viewModel.widgets.collectAsState()
    val fontColorInt by viewModel.fontColor.collectAsState()
    val themeColor = Color(fontColorInt)

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshApps()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Text("Add Widget", color = themeColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(availableWidgets) { widget ->
                val isAdded = addedWidgets.any { it.providerName == widget.providerName }
                
                WidgetPickerItem(widget, isAdded, onClick = { onSelect(widget.info) }, themeColor = themeColor)
                HorizontalDivider(color = Color.DarkGray)
            }
        }
        
        Text(
            "Back", 
            color = themeColor, 
            modifier = Modifier
                .clickable { onBack() }
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun WidgetPickerItem(widget: WidgetProviderInfo, isAdded: Boolean, onClick: () -> Unit, themeColor: Color = Color.White) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Preview or Icon
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color.DarkGray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            val drawable = widget.previewImage ?: widget.icon
            if (drawable != null) {
                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            setImageDrawable(drawable)
                        }
                    },
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
        
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(text = widget.label, color = themeColor, fontSize = 16.sp)
            Text(text = widget.packageName, color = Color.Gray, fontSize = 12.sp)
        }

        if (isAdded) {
            Text(
                text = "Added", 
                color = themeColor.copy(alpha = 0.5f), 
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun AppSelectionScreen(
    title: String,
    appsFlow: kotlinx.coroutines.flow.StateFlow<List<AppInfo>>,
    selectedPackageFlow: kotlinx.coroutines.flow.StateFlow<String?>,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: LauncherViewModel // Need viewModel for font color
) {
    val apps by appsFlow.collectAsState()
    val selectedPackage by selectedPackageFlow.collectAsState()
    val fontColorInt by viewModel.fontColor.collectAsState()
    val themeColor = Color(fontColorInt)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Text(title, color = themeColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (apps.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No compatible apps found", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(apps) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                onSelect(app.packageName)
                                onBack()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedPackage == app.packageName,
                            onCheckedChange = { 
                                onSelect(app.packageName)
                                onBack()
                            },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = themeColor,
                                checkmarkColor = Color.Black
                            )
                        )
                        Text(text = app.label, color = themeColor, modifier = Modifier.padding(start = 8.dp))
                    }
                    HorizontalDivider(color = Color.DarkGray)
                }
            }
        }
        
        Text(
            "Back", 
            color = themeColor,
            modifier = Modifier
                .clickable { onBack() }
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}
