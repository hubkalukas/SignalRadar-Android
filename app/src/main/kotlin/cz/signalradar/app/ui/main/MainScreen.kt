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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import cz.signalradar.app.ui.theme.AmberSignal
import cz.signalradar.app.ui.theme.CardBorder
import cz.signalradar.app.ui.theme.CardSurface
import cz.signalradar.app.ui.theme.CardSurfaceElevated
import cz.signalradar.app.ui.theme.CyanElectric
import cz.signalradar.app.ui.theme.DeepSlate
import cz.signalradar.app.ui.theme.EmeraldPulse
import cz.signalradar.app.ui.theme.ObsidianBg
import cz.signalradar.app.ui.theme.SignalRadarTheme
import cz.signalradar.app.ui.theme.TextMuted
import cz.signalradar.app.ui.theme.TextPrimary
import cz.signalradar.app.ui.theme.TextSecondary
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
        val anyLocationOrScan = result.values.any { it } || viewModel.hasPermissions()
        viewModel.onPermissionsResult(anyLocationOrScan)
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF09101E), ObsidianBg, Color(0xFF04070D))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TopHeaderBar(
                status = state.status,
                onToggleScan = onToggleScan,
                onToggleDemo = onToggleDemo,
                onManualPulse = onManualPulse
            )

            ViewModeSegmentedBar(
                selected = state.viewMode,
                onSelect = onViewModeChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            TelemetrySummaryStrip(
                bleCount = state.bleCount,
                wifiCount = state.wifiCount,
                closestMeters = state.closestDistanceMeters,
                status = state.status,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (!state.status.hasPermissions && !state.status.isDemoMode) {
                PermissionCard(
                    onGrant = onRequestPermissions,
                    onTryDemo = onStartDemo,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else if (!state.status.statusMessage.isNullOrBlank()) {
                StatusHintBanner(
                    message = state.status.statusMessage,
                    isDemo = state.status.isDemoMode,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            when (state.viewMode) {
                ViewMode.RADAR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SearchAndFilterRow(
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

                        // Compact closest-device quick strip under full radar
                        state.devices.firstOrNull()?.let { nearest ->
                            DeviceSignalCard(
                                device = nearest,
                                isSelected = nearest.id == state.selectedDevice?.id,
                                onClick = { onSelectDevice(nearest) }
                            )
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
                            bottom = if (state.selectedDevice != null) 28.dp else 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            }
                        }

                        item(key = "search_filters") {
                            SearchAndFilterRow(
                                searchQuery = state.searchQuery,
                                filter = state.filter,
                                namedOnly = state.namedOnly,
                                environment = state.environment,
                                onSearchChange = onSearchChange,
                                onFilterChange = onFilterChange,
                                onToggleNamedOnly = onToggleNamedOnly,
                                onEnvironmentChange = onEnvironmentChange
                            )
                        }

                        item(key = "section_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ŽIVÁ ZAŘÍZENÍ (${state.devices.size})",
                                    color = TextMuted,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp
                                )
                                Text(
                                    text = "Seřazeno od nejbližšího",
                                    color = TextMuted,
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        if (state.devices.isEmpty()) {
                            item(key = "empty_state") {
                                EmptySignalsCard(
                                    isScanning = state.status.isScanning,
                                    hasQuery = state.searchQuery.isNotBlank() || state.namedOnly,
                                    onTryDemo = onStartDemo
                                )
                            }
                        } else {
                            items(
                                items = state.devices,
                                key = { it.id }
                            ) { device ->
                                DeviceSignalCard(
                                    device = device,
                                    isSelected = device.id == state.selectedDevice?.id,
                                    onClick = { onSelectDevice(device) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Handheld Lock-On Overlay Sheet
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
                        .background(Color.Black.copy(alpha = 0.56f))
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
private fun TopHeaderBar(
    status: ScannerStatus,
    onToggleScan: () -> Unit,
    onToggleDemo: () -> Unit,
    onManualPulse: () -> Unit
) {
    val pulseAnim = rememberInfiniteTransition(label = "liveDot")
    val dotAlpha by pulseAnim.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CyanElectric.copy(alpha = 0.12f))
                .border(1.dp, CyanElectric.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Radar,
                contentDescription = "SignalRadar",
                tint = CyanElectric,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SignalRadar",
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val liveColor = when {
                    !status.isScanning -> TextMuted
                    status.isDemoMode -> AmberSignal
                    else -> EmeraldPulse
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .alpha(if (status.isScanning) dotAlpha else 0.5f)
                        .clip(CircleShape)
                        .background(liveColor)
                )
                Text(
                    text = when {
                        !status.isScanning -> "POZASTAVENO"
                        status.isDemoMode -> "DEMO · ${String.format("%.1f", status.sampleIntervalMs)} ms"
                        else -> "LIVE · ${String.format("%.1f", status.sampleIntervalMs)} ms"
                    },
                    color = liveColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Demo / Live quick switch pill
        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (status.isDemoMode) AmberSignal.copy(alpha = 0.16f) else CardSurface)
                .border(
                    1.dp,
                    if (status.isDemoMode) AmberSignal.copy(alpha = 0.7f) else CardBorder,
                    RoundedCornerShape(10.dp)
                )
                .clickable(onClick = onToggleDemo)
                .padding(horizontal = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (status.isDemoMode) "DEMO" else "LIVE",
                color = if (status.isDemoMode) AmberSignal else TextSecondary,
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(8.dp))

        // Manual scan pulse button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                .clickable(onClick = onManualPulse),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Ruční sken",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        // Play / Pause scan button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (status.isScanning) CyanElectric.copy(alpha = 0.16f) else CardSurface)
                .border(
                    1.dp,
                    if (status.isScanning) CyanElectric.copy(alpha = 0.65f) else CardBorder,
                    RoundedCornerShape(10.dp)
                )
                .clickable(onClick = onToggleScan),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (status.isScanning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (status.isScanning) "Pozastavit" else "Spustit",
                tint = if (status.isScanning) CyanElectric else TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ViewModeSegmentedBar(
    selected: ViewMode,
    onSelect: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        ViewMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (isSelected) CardSurfaceElevated else Color.Transparent
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) CyanElectric.copy(alpha = 0.55f) else Color.Transparent,
                        shape = RoundedCornerShape(11.dp)
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TelemetrySummaryStrip(
    bleCount: Int,
    wifiCount: Int,
    closestMeters: Float?,
    status: ScannerStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatPill(
            label = "BLUETOOTH",
            value = "$bleCount",
            accent = CyanElectric,
            modifier = Modifier.weight(1f)
        )
        StatPill(
            label = "WI-FI",
            value = "$wifiCount",
            accent = EmeraldPulse,
            modifier = Modifier.weight(1f)
        )
        StatPill(
            label = "NEJBLIŽŠÍ",
            value = closestMeters?.let { SignalMath.formatDistance(it) } ?: "—",
            accent = if (closestMeters != null && closestMeters < 2f) EmeraldPulse else TextPrimary,
            modifier = Modifier.weight(1.15f)
        )
        StatPill(
            label = "PAKETŮ",
            value = formatCompactCount(status.totalPackets),
            accent = TextSecondary,
            modifier = Modifier.weight(0.95f)
        )
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = accent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(fontFeatureSettings = "tnum"),
            maxLines = 1
        )
    }
}

@Composable
private fun SearchAndFilterRow(
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
        // Manual search field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Vyhledat",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Hledat zařízení, výrobce nebo MAC…",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 13.sp
                    ),
                    cursorBrush = SolidColor(CyanElectric),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (searchQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Vymazat",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onSearchChange("") }
                )
            }
        }

        // Filter & environment pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SignalFilter.entries.forEach { item ->
                val selected = item == filter
                val chipColor = when (item) {
                    SignalFilter.ALL -> CyanElectric
                    SignalFilter.BLE -> CyanElectric
                    SignalFilter.WIFI -> EmeraldPulse
                }
                FilterChipPill(
                    label = item.label,
                    selected = selected,
                    accent = chipColor,
                    onClick = { onFilterChange(item) }
                )
            }

            FilterChipPill(
                label = "Jen s názvem",
                selected = namedOnly,
                accent = AmberSignal,
                onClick = onToggleNamedOnly
            )

            // Cycle environment profile chip
            FilterChipPill(
                label = environment.shortLabel,
                selected = false,
                accent = CyanElectric,
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
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.copy(alpha = 0.16f) else CardSurface)
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.75f) else CardBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) accent else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun DeviceSignalCard(
    device: DetectedDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val typeAccent = if (device.type == SignalType.BLE) CyanElectric else EmeraldPulse
    val qualityColor = signalColorForQuality(device.signalQuality)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) CardSurfaceElevated else CardSurface)
            .border(
                width = if (isSelected) 1.4.dp else 1.dp,
                color = if (isSelected) typeAccent.copy(alpha = 0.8f) else CardBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio badge icon
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(typeAccent.copy(alpha = 0.12f))
                    .border(1.dp, typeAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (device.type == SignalType.BLE) Icons.Rounded.Bluetooth else Icons.Rounded.Wifi,
                    contentDescription = device.type.label,
                    tint = typeAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(11.dp))

            // Device name & metadata
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = device.displayName,
                        color = TextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeAccent.copy(alpha = 0.13f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = device.bandLabel,
                            color = typeAccent,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        if (!device.vendor.isNullOrBlank()) {
                            append(device.vendor)
                            append(" · ")
                        }
                        append(device.id)
                    },
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // Live mini sparkline
            SignalSparkline(
                samples = device.history,
                accentColor = qualityColor,
                modifier = Modifier
                    .width(56.dp)
                    .height(28.dp)
            )

            Spacer(Modifier.width(10.dp))

            // Distance (meters) & dBm column
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = SignalMath.formatDistance(device.distanceMeters),
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
                Text(
                    text = "${device.filteredRssi.roundToInt()} dBm",
                    color = qualityColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(9.dp))

        // Sleek signal strength bar + proximity badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DeepSlate)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (device.signalQuality / 100f).coerceIn(0.04f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(qualityColor.copy(alpha = 0.6f), qualityColor)
                            )
                        )
                )
            }
            Text(
                text = "${device.proximityBand.label} · ${device.signalQuality} %",
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Icon(
                imageVector = Icons.Rounded.MyLocation,
                contentDescription = "Zaměřit",
                tint = TextMuted,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun PermissionCard(
    onGrant: () -> Unit,
    onTryDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .border(1.dp, CyanElectric.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Sensors,
                contentDescription = null,
                tint = CyanElectric,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Povolit živý radar Bluetooth & Wi-Fi",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Android vyžaduje oprávnění pro okolní zařízení a polohu, aby bylo možné nepřetržitě číst sílu signálu (dBm) a odhadovat vzdálenost v metrech.",
            color = TextSecondary,
            fontSize = 12.5.sp,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanElectric,
                    contentColor = ObsidianBg
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Povolit skenování", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onTryDemo,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Text("Demo", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StatusHintBanner(
    message: String,
    isDemo: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = if (isDemo) AmberSignal else CyanElectric
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = message,
            color = accent,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptySignalsCard(
    isScanning: Boolean,
    hasQuery: Boolean,
    onTryDemo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Radar,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(34.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when {
                hasQuery -> "Žádné zařízení neodpovídá filtru"
                isScanning -> "Naslouchám okolním signálům…"
                else -> "Skenování je pozastaveno"
            },
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (hasQuery) {
                "Zkus upravit vyhledávaný text nebo přepnout filtr."
            } else {
                "Ujisti se, že máš zapnuté Bluetooth, Wi-Fi a Polohu — nebo vyzkoušej Demo režim."
            },
            color = TextSecondary,
            fontSize = 12.sp
        )
        if (!hasQuery) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onTryDemo,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanElectric.copy(alpha = 0.5f))
            ) {
                Text("Spustit Demo signály", color = CyanElectric, fontSize = 12.sp)
            }
        }
    }
}

private fun formatCompactCount(count: Long): String = when {
    count >= 1_000_000L -> String.format("%.1fM", count / 1_000_000f)
    count >= 1_000L -> String.format("%.1fk", count / 1_000f)
    else -> count.toString()
}

// ============================================================================
// Compose Previews for visual verification
// ============================================================================

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
            id = "50:C7:BF:3A:90:12",
            displayName = "Domov_5G_Ultra",
            rawName = "Domov_5G_Ultra",
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
            id = "24:0A:C4:88:12:EF",
            displayName = "ESP32-Radar-Lab",
            rawName = "ESP32-Radar-Lab",
            vendor = "Espressif (ESP32)",
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
            id = "18:E8:29:71:C2:08",
            displayName = "Studio_Mesh_6G",
            rawName = "Studio_Mesh_6G",
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

@Preview(showBackground = true, widthDp = 412, heightDp = 915, name = "Combined Radar + Dashboard")
@Composable
fun SignalRadarCombinedPreview() {
    val devices = samplePreviewDevices()
    SignalRadarTheme {
        Surface(color = ObsidianBg) {
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
                        sampleIntervalMs = 0.5f,
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

@Preview(showBackground = true, widthDp = 412, heightDp = 915, name = "Handheld Target Lock")
@Composable
fun SignalRadarTargetLockPreview() {
    val devices = samplePreviewDevices()
    SignalRadarTheme {
        Surface(color = ObsidianBg) {
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
                        sampleIntervalMs = 0.5f,
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
