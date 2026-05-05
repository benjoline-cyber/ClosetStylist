package com.Ben.closetstylist.ui.suggest

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.Ben.closetstylist.BuildConfig
import com.Ben.closetstylist.R
import com.Ben.closetstylist.domain.StylistPersona
import com.Ben.closetstylist.domain.WeatherInfo
import kotlinx.coroutines.launch

@Composable
fun SuggestScreen(viewModel: SuggestViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val debugOverride by viewModel.debugWeatherOverride.collectAsState()
    val selectedPersona by viewModel.selectedPersona.collectAsState()
    val weatherSummary by viewModel.weatherSummary.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
        // Skip location entirely when the debug override is active.
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
                WeatherStrip(summary = weatherSummary, onClick = { requestAndGenerate() })
                Spacer(Modifier.height(8.dp))
            }

            PersonaPicker(
                selected = selectedPersona,
                onSelect = viewModel::selectPersona,
            )

            Spacer(Modifier.height(12.dp))

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
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.suggest_generating),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                uiState.error != null -> {
                    val errorMessage = uiState.error ?: ""
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            OutlinedButton(onClick = { requestAndGenerate() }) {
                                Text(stringResource(R.string.suggest_retry))
                            }
                        }
                    }
                }

                uiState.outfits.isNotEmpty() -> {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        uiState.outfits.forEachIndexed { index, outfit ->
                            AnimatedVisibility(
                                visible = outfit.cardKey !in uiState.collapsedKeys,
                                exit = shrinkVertically() + fadeOut(),
                                enter = fadeIn(),
                            ) {
                                OutfitCard(
                                    outfit = outfit,
                                    outfitNumber = index + 1,
                                    onWore = { viewModel.onWore(outfit) },
                                    onDismissed = { viewModel.onDismissed(outfit) },
                                    onRejectedCombo = { viewModel.onRejectedCombo(outfit) },
                                )
                            }
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
            Text(summary, style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
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
                fontSize = 11.sp,
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
