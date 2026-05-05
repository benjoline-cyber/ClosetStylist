package com.Ben.closetstylist.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.Ben.closetstylist.R

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        ApiKeySection(
            label = stringResource(R.string.settings_claude_key_label),
            placeholder = stringResource(R.string.settings_claude_key_hint),
            value = uiState.claudeKeyInput,
            isSaved = uiState.claudeKeySaved,
            keyTail = uiState.claudeKeyTail,
            editing = uiState.editingClaude,
            onStartEdit = viewModel::startEditClaude,
            onValueChange = viewModel::onClaudeKeyInput,
            onSave = viewModel::saveClaudeKey,
        )

        HorizontalDivider()

        ApiKeySection(
            label = stringResource(R.string.settings_weather_key_label),
            placeholder = stringResource(R.string.settings_weather_key_hint),
            value = uiState.weatherKeyInput,
            isSaved = uiState.weatherKeySaved,
            keyTail = uiState.weatherKeyTail,
            editing = uiState.editingWeather,
            onStartEdit = viewModel::startEditWeather,
            onValueChange = viewModel::onWeatherKeyInput,
            onSave = viewModel::saveWeatherKey,
        )

        HorizontalDivider()

        TestConnectionSection(
            state = uiState.testState,
            enabled = uiState.claudeKeySaved,
            onTest = viewModel::testConnection,
        )
    }
}

@Composable
private fun ApiKeySection(
    label: String,
    placeholder: String,
    value: String,
    isSaved: Boolean,
    keyTail: String,
    editing: Boolean,
    onStartEdit: () -> Unit,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    var keyVisible by remember(isSaved) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (isSaved) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.settings_key_saved),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (isSaved && !editing) {
            // Masked display row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = "••••••••$keyTail",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onStartEdit) {
                    Text(stringResource(R.string.settings_edit_key))
                }
            }
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder) },
                visualTransformation = if (keyVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { keyVisible = !keyVisible }) {
                        Text(
                            text = if (keyVisible) stringResource(R.string.settings_hide)
                                   else stringResource(R.string.settings_show),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (value.isNotBlank()) onSave() }),
            )

            Button(
                onClick = onSave,
                enabled = value.isNotBlank(),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.settings_save_key))
            }
        }
    }
}

@Composable
private fun TestConnectionSection(
    state: TestConnectionState,
    enabled: Boolean,
    onTest: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_test_connection_label),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.settings_test_connection_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = onTest,
            enabled = enabled && state !is TestConnectionState.Testing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state is TestConnectionState.Testing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(R.string.settings_test_connection_button))
            }
        }
        when (state) {
            is TestConnectionState.Success -> Text(
                text = stringResource(R.string.settings_test_connection_success),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            is TestConnectionState.Failure -> Text(
                text = state.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            else -> {}
        }
    }
}
