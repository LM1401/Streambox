package com.example.streambox.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserPreferences(
    // Streaming Quality
    val defaultQuality: String = "1080p",
    val enableAutoQuality: Boolean = true,
    val maxBitrateMbps: Int = 10,

    // Torrent Sources
    val enabledSources: List<String> = listOf("YTS", "1337x", "EZTV"),
    val preferredSource: String = "YTS",
    val minSeeders: Int = 5,
    val maxFileSizeGB: Int = 10,

    // UI Preferences
    val theme: String = "dark", // "light", "dark", "system"
    val language: String = "en",
    val interfaceLanguage: String = "en", // UI Language
    val contentLanguage: String = "en", // Content/Movie Language
    val enableAnimations: Boolean = true,
    val showBackdrops: Boolean = true,

    // Playback
    val autoPlayNext: Boolean = true,
    val defaultPlaybackSpeed: Float = 1f,
    val enableSubtitles: Boolean = true,
    val subtitleLanguage: String = "en",
    val subtitleSize: Float = 16f, // SP units
    val subtitleStyle: String = "default", // "default", "bold", "outline"
    val subtitleBackground: Boolean = true,
    val enableAudioDescription: Boolean = false,
    val audioLanguage: String = "en",
    val enablePiP: Boolean = true,

    // Language & Region
    val region: String = "US", // For content availability
    val preferredLanguages: List<String> = listOf("en"), // Content languages preference
    val enableDubbedContent: Boolean = true,
    val enableOriginalAudio: Boolean = false,

    // Privacy & Security
    val enableAnalytics: Boolean = false,
    val enableCrashReporting: Boolean = true,
    val clearHistoryOnExit: Boolean = false,
    val requireAuth: Boolean = false,
    val enableIncognitoMode: Boolean = false,

    // Network
    val enableWifiOnly: Boolean = false,
    val maxConcurrentDownloads: Int = 3,
    val downloadLocation: String = "/storage/emulated/0/Download/StreamBox",
    val enableCaching: Boolean = true,
    val cacheSizeMB: Int = 500,
    val enableDataSaver: Boolean = false,

    // Accessibility
    val enableHighContrast: Boolean = false,
    val enableLargeText: Boolean = false,
    val enableReducedMotion: Boolean = false,
    val enableScreenReader: Boolean = false,

    // Advanced
    val enableDebugMode: Boolean = false,
    val logLevel: String = "INFO", // "DEBUG", "INFO", "WARN", "ERROR"
    val enableExperimentalFeatures: Boolean = false,
    val apiTimeoutSeconds: Int = 30,
    val enableHardwareAcceleration: Boolean = true,
    val enableBufferOptimization: Boolean = true
)

class AdvancedSettingsViewModel : ViewModel() {
    private val _preferences = MutableStateFlow(UserPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    fun updatePreferences(updates: UserPreferences.() -> UserPreferences) {
        _preferences.value = _preferences.value.updates()
        // Save to persistent storage
        viewModelScope.launch {
            // TODO: Save to DataStore
        }
    }

    fun resetToDefaults() {
        _preferences.value = UserPreferences()
        viewModelScope.launch {
            // TODO: Clear DataStore
        }
    }

    fun exportSettings(): String {
        // Convert preferences to JSON string
        val prefs = _preferences.value
        return """
        {
            "streaming": {
                "defaultQuality": "${prefs.defaultQuality}",
                "enableAutoQuality": ${prefs.enableAutoQuality},
                "maxBitrateMbps": ${prefs.maxBitrateMbps}
            },
            "sources": {
                "enabledSources": ${prefs.enabledSources.joinToString(prefix = "[", postfix = "]", transform = { "\"$it\"" })},
                "preferredSource": "${prefs.preferredSource}",
                "minSeeders": ${prefs.minSeeders},
                "maxFileSizeGB": ${prefs.maxFileSizeGB}
            }
        }
        """.trimIndent()
    }

    fun importSettings() {
        // Parse JSON and update preferences
        viewModelScope.launch {
            try {
                // TODO: Parse JSON and update preferences
                resetToDefaults()
            } catch (_: Exception) {
                // Handle import error
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    viewModel: AdvancedSettingsViewModel
) {
    val preferences by viewModel.preferences.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Advanced Settings",
                style = MaterialTheme.typography.headlineMedium
            )
            Row {
                TextButton(onClick = { viewModel.resetToDefaults() }) {
                    Text("Reset")
                }
                TextButton(onClick = { viewModel.exportSettings() }) {
                    Text("Export")
                }
                TextButton(onClick = { viewModel.importSettings() }) {
                    Text("Import")
                }
            }
        }

        // Streaming Quality Section
        SettingsSection(title = "Streaming Quality") {
            DropdownSetting(
                title = "Default Quality",
                options = listOf("480p", "720p", "1080p", "4K"),
                selected = preferences.defaultQuality,
                onSelected = { viewModel.updatePreferences { copy(defaultQuality = it) } }
            )

            SwitchSetting(
                title = "Auto Quality Adjustment",
                subtitle = "Automatically adjust quality based on connection speed",
                checked = preferences.enableAutoQuality,
                onCheckedChange = { viewModel.updatePreferences { copy(enableAutoQuality = it) } }
            )

            SliderSetting(
                title = "Max Bitrate",
                subtitle = "${preferences.maxBitrateMbps} Mbps",
                value = preferences.maxBitrateMbps.toFloat(),
                range = 1f..50f,
                onValueChange = { viewModel.updatePreferences { copy(maxBitrateMbps = it.toInt()) } }
            )
        }

        // Torrent Sources Section
        SettingsSection(title = "Torrent Sources") {
            Text(
                text = "Enabled Sources",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Column(Modifier.selectableGroup()) {
                listOf("YTS", "1337x", "EZTV", "RARBG").forEach { source ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = preferences.enabledSources.contains(source),
                                onClick = {
                                    val newSources = if (preferences.enabledSources.contains(source)) {
                                        preferences.enabledSources - source
                                    } else {
                                        preferences.enabledSources + source
                                    }
                                    viewModel.updatePreferences { copy(enabledSources = newSources) }
                                },
                                role = Role.Checkbox
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = preferences.enabledSources.contains(source),
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(source)
                    }
                }
            }

            DropdownSetting(
                title = "Preferred Source",
                options = preferences.enabledSources,
                selected = preferences.preferredSource,
                onSelected = { viewModel.updatePreferences { copy(preferredSource = it) } }
            )

            SliderSetting(
                title = "Minimum Seeders",
                subtitle = "${preferences.minSeeders} seeders",
                value = preferences.minSeeders.toFloat(),
                range = 0f..100f,
                onValueChange = { viewModel.updatePreferences { copy(minSeeders = it.toInt()) } }
            )

            SliderSetting(
                title = "Max File Size",
                subtitle = "${preferences.maxFileSizeGB} GB",
                value = preferences.maxFileSizeGB.toFloat(),
                range = 1f..50f,
                onValueChange = { viewModel.updatePreferences { copy(maxFileSizeGB = it.toInt()) } }
            )
        }

        // UI Preferences Section
        SettingsSection(title = "UI Preferences") {
            DropdownSetting(
                title = "Theme",
                options = listOf("light", "dark", "system"),
                selected = preferences.theme,
                onSelected = { viewModel.updatePreferences { copy(theme = it) } }
            )

            DropdownSetting(
                title = "Interface Language",
                options = listOf("en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh", "ar", "hi", "th", "vi"),
                selected = preferences.interfaceLanguage,
                onSelected = { viewModel.updatePreferences { copy(interfaceLanguage = it) } }
            )

            DropdownSetting(
                title = "Content Language",
                options = listOf("en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh", "ar", "hi", "th", "vi"),
                selected = preferences.contentLanguage,
                onSelected = { viewModel.updatePreferences { copy(contentLanguage = it) } }
            )

            SwitchSetting(
                title = "Enable Animations",
                subtitle = "Show animations and transitions",
                checked = preferences.enableAnimations,
                onCheckedChange = { viewModel.updatePreferences { copy(enableAnimations = it) } }
            )

            SwitchSetting(
                title = "Show Backdrops",
                subtitle = "Display video backdrops in the background",
                checked = preferences.showBackdrops,
                onCheckedChange = { viewModel.updatePreferences { copy(showBackdrops = it) } }
            )
        }

        // Language & Region Section
        SettingsSection(title = "Language & Region") {
            DropdownSetting(
                title = "Region",
                options = listOf("US", "GB", "CA", "AU", "DE", "FR", "ES", "IT", "JP", "KR", "BR", "MX", "IN"),
                selected = preferences.region,
                onSelected = { viewModel.updatePreferences { copy(region = it) } }
            )

            Text(
                text = "Preferred Content Languages",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Column(Modifier.selectableGroup()) {
                listOf("English", "Spanish", "French", "German", "Italian", "Portuguese", "Japanese", "Korean", "Chinese").forEach { lang ->
                    val langCode = when (lang) {
                        "English" -> "en"
                        "Spanish" -> "es"
                        "French" -> "fr"
                        "German" -> "de"
                        "Italian" -> "it"
                        "Portuguese" -> "pt"
                        "Japanese" -> "ja"
                        "Korean" -> "ko"
                        "Chinese" -> "zh"
                        else -> "en"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = preferences.preferredLanguages.contains(langCode),
                                onClick = {
                                    val newLangs = if (preferences.preferredLanguages.contains(langCode)) {
                                        preferences.preferredLanguages - langCode
                                    } else {
                                        preferences.preferredLanguages + langCode
                                    }
                                    viewModel.updatePreferences { copy(preferredLanguages = newLangs) }
                                },
                                role = Role.Checkbox
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = preferences.preferredLanguages.contains(langCode),
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(lang)
                    }
                }
            }

            SwitchSetting(
                title = "Enable Dubbed Content",
                subtitle = "Include dubbed versions in search results",
                checked = preferences.enableDubbedContent,
                onCheckedChange = { viewModel.updatePreferences { copy(enableDubbedContent = it) } }
            )

            SwitchSetting(
                title = "Prefer Original Audio",
                subtitle = "Always use original language audio when available",
                checked = preferences.enableOriginalAudio,
                onCheckedChange = { viewModel.updatePreferences { copy(enableOriginalAudio = it) } }
            )
        }

        // Playback Section
        SettingsSection(title = "Playback") {
            SwitchSetting(
                title = "Auto Play Next Episode",
                subtitle = "Automatically play the next episode in a series",
                checked = preferences.autoPlayNext,
                onCheckedChange = { viewModel.updatePreferences { copy(autoPlayNext = it) } }
            )

            DropdownSetting(
                title = "Default Playback Speed",
                options = listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"),
                selected = "${preferences.defaultPlaybackSpeed}x",
                onSelected = { viewModel.updatePreferences { copy(defaultPlaybackSpeed = it.removeSuffix("x").toFloat()) } }
            )

            SwitchSetting(
                title = "Enable Subtitles",
                subtitle = "Show subtitles by default",
                checked = preferences.enableSubtitles,
                onCheckedChange = { viewModel.updatePreferences { copy(enableSubtitles = it) } }
            )

            DropdownSetting(
                title = "Subtitle Language",
                options = listOf("en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh", "ar", "hi", "th", "vi", "off"),
                selected = preferences.subtitleLanguage,
                onSelected = { viewModel.updatePreferences { copy(subtitleLanguage = it) } }
            )

            SliderSetting(
                title = "Subtitle Size",
                subtitle = "${preferences.subtitleSize.toInt()}sp",
                value = preferences.subtitleSize,
                range = 12f..32f,
                onValueChange = { viewModel.updatePreferences { copy(subtitleSize = it) } }
            )

            DropdownSetting(
                title = "Subtitle Style",
                options = listOf("default", "bold", "outline", "shadow"),
                selected = preferences.subtitleStyle,
                onSelected = { viewModel.updatePreferences { copy(subtitleStyle = it) } }
            )

            SwitchSetting(
                title = "Subtitle Background",
                subtitle = "Show background behind subtitles for better readability",
                checked = preferences.subtitleBackground,
                onCheckedChange = { viewModel.updatePreferences { copy(subtitleBackground = it) } }
            )

            SwitchSetting(
                title = "Audio Description",
                subtitle = "Enable audio descriptions for visually impaired users",
                checked = preferences.enableAudioDescription,
                onCheckedChange = { viewModel.updatePreferences { copy(enableAudioDescription = it) } }
            )

            DropdownSetting(
                title = "Audio Language",
                options = listOf("en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh", "original"),
                selected = preferences.audioLanguage,
                onSelected = { viewModel.updatePreferences { copy(audioLanguage = it) } }
            )

            SwitchSetting(
                title = "Picture-in-Picture",
                subtitle = "Allow video to play in a small window",
                checked = preferences.enablePiP,
                onCheckedChange = { viewModel.updatePreferences { copy(enablePiP = it) } }
            )
        }

        // Network Section
        SettingsSection(title = "Network") {
            SwitchSetting(
                title = "WiFi Only Downloads",
                subtitle = "Only download when connected to WiFi",
                checked = preferences.enableWifiOnly,
                onCheckedChange = { viewModel.updatePreferences { copy(enableWifiOnly = it) } }
            )

            SwitchSetting(
                title = "Data Saver Mode",
                subtitle = "Reduce data usage by lowering quality and disabling some features",
                checked = preferences.enableDataSaver,
                onCheckedChange = { viewModel.updatePreferences { copy(enableDataSaver = it) } }
            )

            SliderSetting(
                title = "Max Concurrent Downloads",
                subtitle = "${preferences.maxConcurrentDownloads} downloads",
                value = preferences.maxConcurrentDownloads.toFloat(),
                range = 1f..10f,
                onValueChange = { viewModel.updatePreferences { copy(maxConcurrentDownloads = it.toInt()) } }
            )

            SwitchSetting(
                title = "Enable Caching",
                subtitle = "Cache metadata and thumbnails",
                checked = preferences.enableCaching,
                onCheckedChange = { viewModel.updatePreferences { copy(enableCaching = it) } }
            )

            SliderSetting(
                title = "Cache Size",
                subtitle = "${preferences.cacheSizeMB} MB",
                value = preferences.cacheSizeMB.toFloat(),
                range = 100f..2000f,
                onValueChange = { viewModel.updatePreferences { copy(cacheSizeMB = it.toInt()) } }
            )
        }

        // Accessibility Section
        SettingsSection(title = "Accessibility") {
            SwitchSetting(
                title = "High Contrast",
                subtitle = "Increase contrast for better visibility",
                checked = preferences.enableHighContrast,
                onCheckedChange = { viewModel.updatePreferences { copy(enableHighContrast = it) } }
            )

            SwitchSetting(
                title = "Large Text",
                subtitle = "Increase text size throughout the app",
                checked = preferences.enableLargeText,
                onCheckedChange = { viewModel.updatePreferences { copy(enableLargeText = it) } }
            )

            SwitchSetting(
                title = "Reduced Motion",
                subtitle = "Minimize animations and transitions",
                checked = preferences.enableReducedMotion,
                onCheckedChange = { viewModel.updatePreferences { copy(enableReducedMotion = it) } }
            )

            SwitchSetting(
                title = "Screen Reader Support",
                subtitle = "Optimize for screen readers like TalkBack",
                checked = preferences.enableScreenReader,
                onCheckedChange = { viewModel.updatePreferences { copy(enableScreenReader = it) } }
            )
        }

        // Advanced Section
        SettingsSection(title = "Advanced") {
            SwitchSetting(
                title = "Debug Mode",
                subtitle = "Enable debug logging and features",
                checked = preferences.enableDebugMode,
                onCheckedChange = { viewModel.updatePreferences { copy(enableDebugMode = it) } }
            )

            SwitchSetting(
                title = "Experimental Features",
                subtitle = "Enable beta features (may be unstable)",
                checked = preferences.enableExperimentalFeatures,
                onCheckedChange = { viewModel.updatePreferences { copy(enableExperimentalFeatures = it) } }
            )

            SwitchSetting(
                title = "Hardware Acceleration",
                subtitle = "Use GPU acceleration for video decoding",
                checked = preferences.enableHardwareAcceleration,
                onCheckedChange = { viewModel.updatePreferences { copy(enableHardwareAcceleration = it) } }
            )

            SwitchSetting(
                title = "Buffer Optimization",
                subtitle = "Optimize video buffering for smoother playback",
                checked = preferences.enableBufferOptimization,
                onCheckedChange = { viewModel.updatePreferences { copy(enableBufferOptimization = it) } }
            )

            SliderSetting(
                title = "API Timeout",
                subtitle = "${preferences.apiTimeoutSeconds} seconds",
                value = preferences.apiTimeoutSeconds.toFloat(),
                range = 5f..120f,
                onValueChange = { viewModel.updatePreferences { copy(apiTimeoutSeconds = it.toInt()) } }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            content()
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SliderSetting(
    title: String,
    subtitle: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSetting(
    title: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
