package com.Ben.closetstylist.ui.suggest

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.Ben.closetstylist.BuildConfig
import com.Ben.closetstylist.R
import com.Ben.closetstylist.domain.StylistPersona
import com.Ben.closetstylist.domain.WeatherInfo
import com.Ben.closetstylist.ui.common.SkeletonOutfitCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SuggestScreen(
    viewModel: SuggestViewModel,
    onNavigateToCloset: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val debugOverride by viewModel.debugWeatherOverride.collectAsState()
    val selectedPersona by viewModel.selectedPersona.collectAsState()
    val weatherSummary by viewModel.weatherSummary.collectAsState()
    val hasInspiration by viewModel.hasInspiration.collectAsState()
    val itemCount by viewModel.itemCount.collectAsState()
    val refreshReason by viewModel.refreshReason.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val woreMessage = stringResource(R.string.suggest_wore_snackbar)
    val dismissedMessage = stringResource(R.string.suggest_dismissed_snackbar)
    val rejectedMessage = stringResource(R.string.suggest_rejected_snackbar)
    val undoLabel = stringResource(R.string.suggest_undo)
    val permissionDeniedMessage = stringResource(R.string.error_suggest_location_permission)

    LaunchedEffect(Unit) {
        viewModel.feedbackEvent.collect { result ->
            val message = when (result) {
                is FeedbackResult.Wore -> woreMessage
                is FeedbackResult.Dismissed -> dismissedMessage
                is FeedbackResult.RejectedCombo -> rejectedMessage
            }
            val snackResult = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (snackResult == SnackbarResult.ActionPerformed) {
                viewModel.undoFeedback(result)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.generateSuggestions()
        } else {
            scope.launch { snackbarHostState.showSnackbar(permissionDeniedMessage) }
        }
    }

    fun requestAndGenerate() {
        if (BuildConfig.DEBUG && debugOverride != null) {
            viewModel.generateSuggestions()
            return
        }
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            viewModel.generateSuggestions()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            if (weatherSummary.isNotBlank()) {
                WeatherStrip(
                    summary = weatherSummary,
                    isStale = uiState.weatherStale,
                    onClick = { requestAndGenerate() },
                )
                Spacer(Modifier.height(8.dp))
            }

            PersonaPicker(selected = selectedPersona, onSelect = viewModel::selectPersona)

            Spacer(Modifier.height(12.dp))

            // Tip banner when inspiration gallery is empty and closet has items
            if (!hasInspiration && itemCount > 0 && uiState.outfits.isEmpty() && !uiState.isLoading) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.suggest_tip_inspiration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = { requestAndGenerate() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (uiState.isLoading) R.string.suggest_generating_button else R.string.suggest_generate_button))
            }

            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(8.dp))
                DebugWeatherPanel(
                    activeOverride = debugOverride,
                    onActivate = viewModel::setDebugWeather,
                    onClear = viewModel::clearDebugWeather,
                )
            }

            when {
                uiState.isLoading -> {
                    Spacer(Modifier.height(16.dp))
                    if (uiState.loadingCaption.isNotBlank()) {
                        Text(
                            uiState.loadingCaption,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(3) { SkeletonOutfitCard() }
                    }
                }

                uiState.awaitingWeatherChoice -> {
                    Spacer(Modifier.height(24.dp))
                    WeatherPresetPicker(
                        onSelect = { viewModel.selectWeatherPreset(it) },
                    )
                }

                uiState.error != null -> {
                    val isEmptyCloset = uiState.error == stringResource(R.string.error_suggest_empty_closet)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 24.dp),
                        ) {
                            Text(
                                uiState.error ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                            if (isEmptyCloset) {
                                Button(onClick = onNavigateToCloset) {
                                    Text(stringResource(R.string.suggest_go_to_closet))
                                }
                            } else {
                                OutlinedButton(onClick = { requestAndGenerate() }) {
                                    Text(stringResource(R.string.suggest_retry))
                                }
                            }
                        }
                    }
                }

                uiState.outfits.isNotEmpty() -> {
                    Spacer(Modifier.height(8.dp))

                    // Staggered reveal: cards become visible with 80ms delay each.
                    var visibleCount by remember(uiState.outfits) { mutableIntStateOf(0) }
                    LaunchedEffect(uiState.outfits) {
                        if (uiState.outfits.isNotEmpty()) {
                            uiState.outfits.indices.forEach { i ->
                                delay(i * 80L)
                                visibleCount = i + 1
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        uiState.outfits.forEachIndexed { index, outfit ->
                            AnimatedVisibility(
                                visible = index < visibleCount && outfit.cardKey !in uiState.collapsedKeys,
                                enter = fadeIn(animationSpec = tween(300)),
                                exit = shrinkVertically() + fadeOut(),
                            ) {
                                OutfitCard(
                                    outfit = outfit,
                                    outfitNumber = index + 1,
                                    onWore = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.onWore(outfit)
                                    },
                                    onDismissed = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.onDismissed(outfit)
                                    },
                                    onRejectedCombo = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.onRejectedCombo(outfit)
                                    },
                                )
                            }
                        }

                        // Refresh with reason
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = refreshReason,
                            onValueChange = viewModel::onRefreshReasonChange,
                            placeholder = { Text(stringResource(R.string.suggest_refresh_reason_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedButton(
                            onClick = { requestAndGenerate() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.suggest_refresh))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.suggest_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherStrip(
    summary: String,
    isStale: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(summary, style = MaterialTheme.typography.labelMedium)
                if (isStale) {
                    Text(
                        stringResource(R.string.suggest_weather_stale),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun WeatherPresetPicker(
    onSelect: (WeatherInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presets = remember {
        listOf(
            WeatherInfo(2.0, "cold", "") to R.string.weather_cold,
            WeatherInfo(14.0, "mild", "") to R.string.weather_mild,
            WeatherInfo(22.0, "warm", "") to R.string.weather_warm,
            WeatherInfo(30.0, "hot", "") to R.string.weather_hot,
        )
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.suggest_pick_weather),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            presets.forEach { (weather, labelRes) ->
                OutlinedButton(
                    onClick = { onSelect(weather) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    Text(
                        stringResource(labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonaPicker(
    selected: StylistPersona,
    onSelect: (StylistPersona) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        items(StylistPersona.entries) { persona ->
            FilterChip(
                selected = persona == selected,
                onClick = { onSelect(persona) },
                label = { Text(persona.displayName()) },
            )
        }
    }
}

@Composable
private fun DebugWeatherPanel(
    activeOverride: WeatherInfo?,
    onActivate: (temp: Double, condition: String) -> Unit,
    onClear: () -> Unit,
) {
    var tempInput by remember { mutableStateOf("22") }
    var conditionInput by remember { mutableStateOf("clear sky") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "DEBUG — weather override",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
            if (activeOverride != null) {
                Text(
                    "Active: ${activeOverride.summary()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(
                    onClick = onClear,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("Clear override", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = tempInput,
                        onValueChange = { tempInput = it },
                        label = { Text("°C") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = conditionInput,
                        onValueChange = { conditionInput = it },
                        label = { Text("condition") },
                        singleLine = true,
                        modifier = Modifier.weight(2.5f),
                    )
                    Button(
                        onClick = {
                            val temp = tempInput.toDoubleOrNull() ?: 20.0
                            onActivate(temp, conditionInput.ifBlank { "clear sky" })
                        },
                    ) {
                        Text("Use")
                    }
                }
            }
        }
    }
}

@Composable
private fun OutfitCard(
    outfit: OutfitSuggestion,
    outfitNumber: Int,
    onWore: () -> Unit,
    onDismissed: () -> Unit,
    onRejectedCombo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.suggest_outfit_number, outfitNumber),
                style = MaterialTheme.typography.titleSmall,
            )

            Spacer(Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(outfit.items) { item ->
                    AsyncImage(
                        model = item.imagePath,
                        contentDescription = item.description,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(outfit.rationale, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(12.dp))

            Button(onClick = onWore, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.suggest_wore_it))
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onDismissed, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.suggest_not_today))
                }
                TextButton(onClick = onRejectedCombo, modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.suggest_never_combo),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
