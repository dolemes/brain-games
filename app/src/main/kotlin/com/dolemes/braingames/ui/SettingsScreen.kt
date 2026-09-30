package com.dolemes.braingames.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.dolemes.braingames.AppContainer
import com.dolemes.braingames.BuildConfig
import com.dolemes.braingames.R
import com.dolemes.braingames.core.PlayerProgress
import com.dolemes.braingames.core.Preferences
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(container: AppContainer, progress: PlayerProgress, onBack: () -> Unit) {
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    BackHandler(onBack = onBack)

    fun updatePreferences(change: (Preferences) -> Preferences) {
        scope.launch { container.progress.update { it.copy(preferences = change(it.preferences)) } }
    }

    SettingsScreen(
        preferences = progress.preferences,
        languageTag = AppCompatDelegate.getApplicationLocales().toLanguageTags(),
        showPrivacyOptions = container.consent.isPrivacyOptionsRequired,
        onUntimedChange = { value -> updatePreferences { it.copy(untimedMode = value) } },
        onTextScaleChange = { value -> updatePreferences { it.copy(textScale = value) } },
        onLanguageChange = { tag ->
            // Recria a tela no idioma novo e guarda a escolha (também no Android < 13).
            AppCompatDelegate.setApplicationLocales(
                if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
            )
        },
        onPrivacyOptions = { activity?.let { container.consent.showPrivacyOptions(it) } },
        onPrivacyPolicy = { uriHandler.openUri(BuildConfig.PRIVACY_POLICY_URL) },
        onBack = onBack,
    )
}

@Composable
fun SettingsScreen(
    preferences: Preferences,
    languageTag: String,
    showPrivacyOptions: Boolean,
    onUntimedChange: (Boolean) -> Unit,
    onTextScaleChange: (Float) -> Unit,
    onLanguageChange: (String) -> Unit,
    onPrivacyOptions: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var textScale by remember(preferences.textScale) { mutableFloatStateOf(preferences.textScale) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.game_back))
            }
        }

        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleLarge)
        LanguageOption(stringResource(R.string.settings_language_system), languageTag.isEmpty()) { onLanguageChange("") }
        LanguageOption(stringResource(R.string.settings_language_pt), languageTag.startsWith("pt")) { onLanguageChange("pt-BR") }
        LanguageOption(stringResource(R.string.settings_language_en), languageTag.startsWith("en")) { onLanguageChange("en") }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(value = preferences.untimedMode, onValueChange = onUntimedChange, role = Role.Switch)
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_untimed), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(R.string.settings_untimed_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = preferences.untimedMode, onCheckedChange = null)
        }

        Text(
            text = stringResource(R.string.settings_text_size),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 12.dp),
        )
        Slider(
            value = textScale,
            onValueChange = { textScale = it },
            onValueChangeFinished = { onTextScaleChange(textScale) },
            valueRange = 1f..1.6f,
            steps = 5,
        )

        if (showPrivacyOptions) {
            OutlinedButton(onClick = onPrivacyOptions, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.settings_privacy_options))
            }
        }
        OutlinedButton(onClick = onPrivacyPolicy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.settings_privacy_policy))
        }

        Text(
            text = stringResource(R.string.settings_disclaimer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
    }
}
