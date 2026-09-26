package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.SettingsEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette
import com.example.ui.theme.parseHexColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val coroutineScope = rememberCoroutineScope()

    val currentSettingsRaw by viewModel.settings.collectAsStateWithLifecycle()
    val currentSettings = currentSettingsRaw ?: SettingsEntity()

    var themeName by remember(currentSettings) { mutableStateOf(currentSettings.currentTheme) }
    var accentHex by remember(currentSettings) { mutableStateOf(currentSettings.accentColorHex) }
    var torboxKey by remember(currentSettings) { mutableStateOf(currentSettings.torboxApiKey) }
    var rdKey by remember(currentSettings) { mutableStateOf(currentSettings.realDebridApiKey) }
    var geminiKey by remember(currentSettings) { mutableStateOf(currentSettings.geminiApiKey) }
    var supabaseUrl by remember(currentSettings) { mutableStateOf(currentSettings.supabaseUrl) }
    var supabaseKey by remember(currentSettings) { mutableStateOf(currentSettings.supabaseAnonKey) }

    var syncStatus by remember { mutableStateOf("") }
    var systemCheckStatus by remember { mutableStateOf("") }
    var exportedJsonPreview by remember { mutableStateOf("") }

    Scaffold(
        containerColor = palette.bg,
        topBar = {
            TopAppBar(
                title = { Text("Settings & Sync", color = palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.updateSettings(
                                currentSettings.copy(
                                    currentTheme = themeName,
                                    accentColorHex = accentHex,
                                    torboxApiKey = torboxKey.trim(),
                                    realDebridApiKey = rdKey.trim(),
                                    geminiApiKey = geminiKey.trim(),
                                    supabaseUrl = supabaseUrl.trim(),
                                    supabaseAnonKey = supabaseKey.trim()
                                )
                            )
                            syncStatus = "Settings Saved!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        modifier = Modifier.testTag("save_settings_button")
                    ) {
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Theme Selector
            Text("Appearance & Themes (5 Palettes)", color = palette.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Dark", "Amoled", "Blue", "Grey", "Light").forEach { theme ->
                    FilterChip(
                        selected = themeName.equals(theme, ignoreCase = true),
                        onClick = { themeName = theme },
                        label = { Text(theme) }
                    )
                }
            }

            // Accent Color Selector
            Text("Accent Color", color = palette.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val accents = listOf(
                    "#8B5CF6", // Violet
                    "#06B6D4", // Cyan
                    "#F43F5E", // Rose
                    "#F59E0B", // Amber
                    "#10B981"  // Emerald
                )
                accents.forEach { hex ->
                    val color = parseHexColor(hex)
                    val isSelected = accentHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { accentHex = hex }
                    )
                }
            }

            HorizontalDivider(color = palette.border)

            // API Keys & Debrid
            Text("Debrid & AI Integrations", color = palette.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            OutlinedTextField(
                value = torboxKey,
                onValueChange = { torboxKey = it },
                label = { Text("Torbox API Key (Cloud Streaming)") },
                modifier = Modifier.fillMaxWidth().testTag("torbox_key_input")
            )

            OutlinedTextField(
                value = rdKey,
                onValueChange = { rdKey = it },
                label = { Text("Real-Debrid API Key") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = geminiKey,
                onValueChange = { geminiKey = it },
                label = { Text("Gemini AI API Key (Subtitle Translation)") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = palette.border)

            // Supabase Cloud Sync
            Text("Supabase Cloud Auto-Sync (GZIP)", color = palette.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            OutlinedTextField(
                value = supabaseUrl,
                onValueChange = { supabaseUrl = it },
                label = { Text("Supabase Project URL") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = supabaseKey,
                onValueChange = { supabaseKey = it },
                label = { Text("Supabase Anon Key") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.syncWithSupabase { status ->
                        syncStatus = status
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.fillMaxWidth().testTag("sync_supabase_button")
            ) {
                Icon(Icons.Default.CloudSync, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Now with Supabase")
            }

            if (syncStatus.isNotEmpty()) {
                Text(syncStatus, color = accent, fontSize = 13.sp)
            }

            HorizontalDivider(color = palette.border)

            // System Check (Auto-match actors & studios by title)
            Text("System Check", color = palette.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Automatically scan all scene titles and link matching actors & studios.", color = palette.textMuted, fontSize = 12.sp)
            OutlinedButton(
                onClick = {
                    viewModel.runSystemCheck { matched ->
                        systemCheckStatus = "System check complete! Linked $matched scenes with actors/studios."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CheckCircleOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Run System Check")
            }

            if (systemCheckStatus.isNotEmpty()) {
                Text(systemCheckStatus, color = Color(0xFF10B981), fontSize = 13.sp)
            }

            HorizontalDivider(color = palette.border)

            // Backup & Export
            Text("Data Backup & Testing", color = palette.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            
            Button(
                onClick = {
                    viewModel.loadTestSamples { status ->
                        systemCheckStatus = status
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.fillMaxWidth().testTag("load_test_scenes_button")
            ) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Load 5 Test Scenes (Pre-configured)")
            }

            FilledTonalButton(
                onClick = {
                    coroutineScope.launch {
                        exportedJsonPreview = viewModel.exportDataJson()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Backup JSON")
            }

            if (exportedJsonPreview.isNotEmpty()) {
                OutlinedTextField(
                    value = exportedJsonPreview,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Exported Data Preview") },
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
