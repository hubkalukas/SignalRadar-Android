package cz.signalradar.app.ui.main

import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.signalradar.app.model.DetectedDevice
import cz.signalradar.app.model.EnvironmentProfile
import cz.signalradar.app.model.ScannerStatus
import cz.signalradar.app.model.SignalFilter
import cz.signalradar.app.model.SignalType
import cz.signalradar.app.model.ViewMode
import cz.signalradar.app.scanner.SignalMath
import cz.signalradar.app.ui.components.SignalRadarScope
import cz.signalradar.app.ui.components.SignalSparkline
import cz.signalradar.app.ui.components.TargetLockPanel
import cz.signalradar.app.ui.theme.AccentAmber
import cz.signalradar.app.ui.theme.AccentBlue
import cz.signalradar.app.ui.theme.AccentGreen
import cz.signalradar.app.ui.theme.BgControl
import cz.signalradar.app.ui.theme.BgElevated
import cz.signalradar.app.ui.theme.BgPrimary
import cz.signalradar.app.ui.theme.BgSurface
import cz.signalradar.app.ui.theme.Hairline
import cz.signalradar.app.ui.theme.InkPrimary
import cz.signalradar.app.ui.theme.InkSecondary
import cz.signalradar.app.ui.theme.InkTertiary
import cz.signalradar.app.ui.theme.SignalRadarTheme
import cz.signalradar.app.ui.theme.signalColorForQuality
import kotlin.math.roundToInt

@Composable
fun SignalRadarRoute(
    viewModel: SignalRadarViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.any { it } || viewModel.hasPermissions()
        viewModel.onPermissionsResult(granted)
    }

    LaunchedEffect(Unit) {
        if (viewModel.hasPermissions()) {
            viewModel.startIfPermitted()
        }
    }

    SignalRadarScreen(
        state = state,
        onRequestPermissions = {
            permissionLauncher.launch(viewModel.requiredPermissions())
        },
        onToggleScan = viewModel::toggleScanning,
        onToggleDemo = viewModel::toggleDemoMode,
        onStartDemo = viewModel::startDemoMode,
        onManualPulse = viewModel::triggerManualPulse,
        onSearchChange = viewModel::setSearchQuery,
        onFilterChange = viewModel::setFilter,
        onToggleNamedOnly = viewModel::toggleNamedOnly,
        onViewModeChange = viewModel::setViewMode,
        onEnvironmentChange = viewModel::setEnvironment,
        onCycleRadarRange = viewModel::cycleRadarRange,
        onSelectDevice = viewModel::selectDevice,
        onToggleHaptics = viewModel::toggleHaptics
    )
}

@Composable
fun SignalRadarScreen(
    state: SignalRadarUiState,
    onRequestPermissions: () -> Unit,
    onToggleScan: () -> Unit,
    onToggleDemo: () -> Unit,
    onStartDemo: () -> Unit,
    onManualPulse: () -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterChange: (SignalFilter) -> Unit,
    onToggleNamedOnly: () -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onEnvironmentChange: (EnvironmentProfile) -> Unit,
    onCycleRadarRange: () -> Unit,
    onSelectDevice: (DetectedDevice?) -> Unit,
    onToggleHaptics: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            HeaderBar(
                state = state,
                onToggleScan = onToggleScan,
                onToggleDemo = onToggleDemo,
                onManualPulse = onManualPulse
            )

            ViewModeSwitch(
                selected = state.viewMode,
                onSelect = onViewModeChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (!state.status.hasPermissions && !state.status.isDemoMode) {
                PermissionNotice(
                    onGrant = onRequestPermissions,
                    onTryDemo = onStartDemo,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else if (!state.status.statusMessage.isNullOrBlank()) {
                NoticeStrip(
                    message = state.status.statusMessage,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            when (state.viewMode) {
                ViewMode.RADAR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SearchAndFilters(
                            searchQuery = state.searchQuery,
                            filter = state.filter,
                            namedOnly = state.namedOnly,
                            environment = state.environment,
                            onSearchChange = onSearchChange,
                            onFilterChange = onFilterChange,
                            onToggleNamedOnly = onToggleNamedOnly,
                            onEnvironmentChange = onEnvironmentChange
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            SignalRadarScope(
                                devices = state.devices,
                                selectedDeviceId = state.selectedDevice?.id,
                                maxRangeMeters = state.radarRangeMeters,
                                isScanning = state.status.isScanning,
                                onSelectDevice = { onSelectDevice(it) },
                                onCycleRange = onCycleRadarRange
                            )
                        }

                        state.devices.firstOrNull()?.let { nearest ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(BgElevated)
                                    .border(0.5.dp, Hairline, RoundedCornerShape(16.dp))
                            ) {
                                DeviceRow(
                                    device = nearest,
                                    isSelected = nearest.id == state.selectedDevice?.id,
                                    onClick = { onSelectDevice(nearest) }
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }

                ViewMode.COMBINED, ViewMode.DASHBOARD -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 24.dp
                        )
                    ) {
                        if (state.viewMode == ViewMode.COMBINED) {
                            item(key = "radar_scope") {
                                SignalRadarScope(
                                    devices = state.devices,
                                    selectedDeviceId = state.selectedDevice?.id,
                                    maxRangeMeters = state.radarRangeMeters,
                                    isScanning = state.status.isScanning,
                                    onSelectDevice = { onSelectDevice(it) },
                                    onCycleRange = onCycleRadarRange
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }

                        item(key = "search_filters") {
                            SearchAndFilters(
                                searchQuery = state.searchQuery,
                                filter = state.filter,
                                namedOnly = state.namedOnly,
                                environment = state.environment,
                                onSearchChange = onSearchChange,
                                onFilterChange = onFilterChange,
                                onToggleNamedOnly = onToggleNamedOnly,
                                onEnvironmentChange = onEnvironmentChange
                            )
                            Spacer(Modifier.height(14.dp))
                        }

                        item(key = "list_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "V okolí (${state.devices.size})",
                                    color = InkSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "podle vzdálenosti",
                                    color = InkTertiary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (state.devices.isEmpty()) {
                            item(key = "empty_state") {
                                EmptyStateCard(
                                    isScanning = state.status.isScanning,
                                    hasQuery = state.searchQuery.isNotBlank() || state.namedOnly,
                                    onTryDemo = onStartDemo
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = state.devices,
                                key = { _, item -> item.id }
                            ) { index, device ->
                                val isFirst = index == 0
                                val isLast = index == state.devices.lastIndex
                                val shape = RoundedCornerShape(
                                    topStart = if (isFirst) 16.dp else 0.dp,
                                    topEnd = if (isFirst) 16.dp else 0.dp,
                                    bottomStart = if (isLast) 16.dp else 0.dp,
                                    bottomEnd = if (isLast) 16.dp else 0.dp
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shape)
                                        .background(BgElevated)
                                ) {
                                    DeviceRow(
                                        device = device,
                                        isSelected = device.id == state.selectedDevice?.id,
                                        onClick = { onSelectDevice(device) }
                                    )
                                    if (!isLast) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 60.dp, end = 14.dp),
                                            thickness = 0.5.dp,
                                            color = Hairline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.selectedDevice != null,
            enter = fadeIn(tween(180)) + slideInVertically(tween(240)) { it / 2 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            state.selectedDevice?.let { locked ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.58f))
                        .clickable { onSelectDevice(null) },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .clickable(enabled = false) {}
                    ) {
                        TargetLockPanel(
                            device = locked,
                            environment = state.environment,
                            hapticsEnabled = state.hapticsEnabled,
                            onToggleHaptics = onToggleHaptics,
                            onSelectEnvironment = onEnvironmentChange,
                            onDismiss = { onSelectDevice(null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderBar(
    state: SignalRadarUiState,
    onToggleScan: () -> Unit,
    onToggleDemo: () -> Unit,
    onManualPulse: () -> Unit
) {
    val pulseAnim = rememberInfiniteTransition(label = "scanDot")
    val dotAlpha by pulseAnim.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SignalRadar",
                color = InkPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val dotColor = when {
                    !state.status.isScanning -> InkTertiary
                    state.status.isDemoMode -> AccentAmber
                    else -> AccentGreen
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .alpha(if (state.status.isScanning) dotAlpha else 0.5f)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                val summary = buildString {
                    append(
                        when {
                            !state.status.isScanning -> "Pozastaveno"
                            state.status.isDemoMode -> "Ukázka"
                            else -> "Skenuje"
                        }
                    )
                    if (state.allDeviceCount > 0) {
                        append(" · ${state.bleCount} BT, ${state.wifiCount} Wi-Fi")
                    }
                    state.closestDistanceMeters?.let {
                        append(" · ${SignalMath.formatDistance(it)}")
                    }
                }
                Text(
                    text = summary,
                    color = InkSecondary,
                    fontSize = 12.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (state.status.isDemoMode) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgControl)
                    .clickable(onClick = onToggleDemo)
                    .padding(horizontal = 9.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Ukázka",
                    color = AccentAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BgElevated)
                .clickable(onClick = onManualPulse),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Obnovit",
                tint = InkSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (state.status.isScanning) InkPrimary else BgControl)
                .clickable(onClick = onToggleScan),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (state.status.isScanning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (state.status.isScanning) "Pozastavit" else "Spustit",
                tint = if (state.status.isScanning) BgPrimary else InkPrimary,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun ViewModeSwitch(
    selected: ViewMode,
    onSelect: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgElevated)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ViewMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) BgControl else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    color = if (isSelected) InkPrimary else InkSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SearchAndFilters(
    searchQuery: String,
    filter: SignalFilter,
    namedOnly: Boolean,
    environment: EnvironmentProfile,
    onSearchChange: (String) -> Unit,
    onFilterChange: (SignalFilter) -> Unit,
    onToggleNamedOnly: () -> Unit,
    onEnvironmentChange: (EnvironmentProfile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BgElevated)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Hledat",
                tint = InkTertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Hledat název, výrobce nebo MAC",
                        color = InkTertiary,
                        fontSize = 13.5.sp
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = InkPrimary,
                        fontSize = 13.5.sp
                    ),
                    cursorBrush = SolidColor(InkPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (searchQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Vymazat",
                    tint = InkSecondary,
                    modifier = Modifier
                        .size(17.dp)
                        .clickable { onSearchChange("") }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SignalFilter.entries.forEach { item ->
                FilterPill(
                    label = item.label,
                    selected = item == filter,
                    onClick = { onFilterChange(item) }
                )
            }

            FilterPill(
                label = "S názvem",
                selected = namedOnly,
                onClick = onToggleNamedOnly
            )

            FilterPill(
                label = environment.label,
                selected = false,
                onClick = {
                    val values = EnvironmentProfile.entries
                    val next = values[(values.indexOf(environment) + 1) % values.size]
                    onEnvironmentChange(next)
                }
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) InkPrimary else BgElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) BgPrimary else InkSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun DeviceRow(
    device: DetectedDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val typeColor = if (device.type == SignalType.BLE) AccentBlue else AccentGreen
    val qualityColor = signalColorForQuality(device.signalQuality)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) BgSurface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(typeColor.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (device.type == SignalType.BLE) Icons.Rounded.Bluetooth else Icons.Rounded.Wifi,
                contentDescription = device.type.label,
                tint = typeColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.displayName,
                color = InkPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    if (!device.vendor.isNullOrBlank()) {
                        append(device.vendor)
                        append(" · ")
                    }
                    append(device.bandLabel)
                },
                color = InkTertiary,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        SignalSparkline(
            samples = device.history,
            accentColor = qualityColor,
            modifier = Modifier
                .width(50.dp)
                .height(24.dp)
        )

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = SignalMath.formatDistance(device.distanceMeters),
                color = InkPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            Text(
                text = "${device.filteredRssi.roundToInt()} dBm",
                color = qualityColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun PermissionNotice(
    onGrant: () -> Unit,
    onTryDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BgElevated)
            .border(0.5.dp, Hairline, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Přístup k Bluetooth a Wi-Fi",
            color = InkPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Android potřebuje oprávnění pro okolní zařízení a polohu, jinak systém nevrací sílu signálu.",
            color = InkSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(
                    containerColor = InkPrimary,
                    contentColor = BgPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Povolit", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
            TextButton(onClick = onTryDemo) {
                Text("Zkusit ukázku", color = InkSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun NoticeStrip(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BgElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = message,
            color = InkSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun EmptyStateCard(
    isScanning: Boolean,
    hasQuery: Boolean,
    onTryDemo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BgElevated)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                hasQuery -> "Nic nenalezeno"
                isScanning -> "Hledám signály v okolí…"
                else -> "Skenování je pozastavené"
            },
            color = InkPrimary,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (hasQuery) {
                "Zkus jiný výraz nebo vypni filtr."
            } else {
                "Zkontroluj zapnuté Bluetooth, Wi-Fi a polohu."
            },
            color = InkSecondary,
            fontSize = 12.5.sp
        )
        if (!hasQuery) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onTryDemo) {
                Text("Zobrazit ukázková data", color = AccentBlue, fontSize = 12.5.sp)
            }
        }
    }
}

private fun samplePreviewDevices(): List<DetectedDevice> {
    val now = SystemClock.elapsedRealtimeNanos()
    return listOf(
        DetectedDevice(
            id = "A4:83:E7:19:4C:01",
            displayName = "AirPods Pro 2",
            rawName = "AirPods Pro 2",
            vendor = "Apple",
            type = SignalType.BLE,
            rawRssi = -49,
            filteredRssi = -50.2f,
            txPower = -59,
            frequencyMhz = 2402,
            distanceMeters = 0.45f,
            signalQuality = 88,
            angleDegrees = 38f,
            lastSeenNanos = now,
            packetCount = 412,
            packetsPerSecond = 18.4f,
            history = listOf(-56f, -54f, -53f, -52f, -50f, -51f, -49f, -50f, -48f, -50f),
            bandLabel = "BLE"
        ),
        DetectedDevice(
            id = "24:0A:C4:88:12:EF",
            displayName = "ESP32-Radar",
            rawName = "ESP32-Radar",
            vendor = "Espressif",
            type = SignalType.BLE,
            rawRssi = -63,
            filteredRssi = -62.5f,
            txPower = -58,
            frequencyMhz = 2402,
            distanceMeters = 1.5f,
            signalQuality = 64,
            angleDegrees = 225f,
            lastSeenNanos = now,
            packetCount = 294,
            packetsPerSecond = 12.1f,
            history = listOf(-67f, -66f, -64f, -65f, -63f, -62f, -63f, -61f, -62f),
            bandLabel = "BLE"
        ),
        DetectedDevice(
            id = "50:C7:BF:3A:90:12",
            displayName = "Domov_5G",
            rawName = "Domov_5G",
            vendor = "TP-Link",
            type = SignalType.WIFI,
            rawRssi = -53,
            filteredRssi = -53.8f,
            txPower = null,
            frequencyMhz = 5180,
            distanceMeters = 2.4f,
            signalQuality = 78,
            angleDegrees = 142f,
            lastSeenNanos = now,
            packetCount = 128,
            packetsPerSecond = 4.2f,
            history = listOf(-56f, -55f, -54f, -55f, -54f, -53f, -54f, -53f, -54f),
            bandLabel = "5 GHz"
        ),
        DetectedDevice(
            id = "18:E8:29:71:C2:08",
            displayName = "Studio_6G",
            rawName = "Studio_6G",
            vendor = "Ubiquiti",
            type = SignalType.WIFI,
            rawRssi = -69,
            filteredRssi = -68.4f,
            txPower = null,
            frequencyMhz = 5955,
            distanceMeters = 7.2f,
            signalQuality = 49,
            angleDegrees = 310f,
            lastSeenNanos = now,
            packetCount = 87,
            packetsPerSecond = 3.0f,
            history = listOf(-71f, -70f, -69f, -68f, -69f, -68f, -69f, -68f),
            bandLabel = "6 GHz"
        )
    )
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915, name = "Main Overview")
@Composable
fun SignalRadarCombinedPreview() {
    val devices = samplePreviewDevices()
    SignalRadarTheme {
        Surface(color = BgPrimary) {
            SignalRadarScreen(
                state = SignalRadarUiState(
                    devices = devices,
                    allDeviceCount = devices.size,
                    bleCount = 2,
                    wifiCount = 2,
                    closestDistanceMeters = 0.45f,
                    status = ScannerStatus(
                        isScanning = true,
                        hasPermissions = true,
                        bleActive = true,
                        wifiActive = true,
                        totalPackets = 1482
                    ),
                    viewMode = ViewMode.COMBINED
                ),
                onRequestPermissions = {},
                onToggleScan = {},
                onToggleDemo = {},
                onStartDemo = {},
                onManualPulse = {},
                onSearchChange = {},
                onFilterChange = {},
                onToggleNamedOnly = {},
                onViewModeChange = {},
                onEnvironmentChange = {},
                onCycleRadarRange = {},
                onSelectDevice = {},
                onToggleHaptics = {}
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915, name = "Target Lock")
@Composable
fun SignalRadarTargetLockPreview() {
    val devices = samplePreviewDevices()
    SignalRadarTheme {
        Surface(color = BgPrimary) {
            SignalRadarScreen(
                state = SignalRadarUiState(
                    devices = devices,
                    allDeviceCount = devices.size,
                    bleCount = 2,
                    wifiCount = 2,
                    closestDistanceMeters = 0.45f,
                    status = ScannerStatus(
                        isScanning = true,
                        hasPermissions = true,
                        bleActive = true,
                        wifiActive = true,
                        totalPackets = 1482
                    ),
                    viewMode = ViewMode.COMBINED,
                    selectedDevice = devices.first(),
                    hapticsEnabled = true
                ),
                onRequestPermissions = {},
                onToggleScan = {},
                onToggleDemo = {},
                onStartDemo = {},
                onManualPulse = {},
                onSearchChange = {},
                onFilterChange = {},
                onToggleNamedOnly = {},
                onViewModeChange = {},
                onEnvironmentChange = {},
                onCycleRadarRange = {},
                onSelectDevice = {},
                onToggleHaptics = {}
            )
        }
    }
}
