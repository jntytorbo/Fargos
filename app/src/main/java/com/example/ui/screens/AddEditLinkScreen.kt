package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.LinkEntity
import com.example.network.MediaScrapers
import com.example.ui.MainViewModel
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditLinkScreen(
    viewModel: MainViewModel,
    linkId: String?,
    modifier: Modifier = Modifier
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val coroutineScope = rememberCoroutineScope()

    val links by viewModel.allLinks.collectAsStateWithLifecycle()
    val actors by viewModel.allActors.collectAsStateWithLifecycle()
    val studios by viewModel.allStudios.collectAsStateWithLifecycle()

    val existingLink = remember(linkId, links) {
        links.firstOrNull { it.id == linkId }
    }

    var title by remember { mutableStateOf(existingLink?.title ?: "") }
    var coverImage by remember { mutableStateOf(existingLink?.coverImage ?: "") }
    var coverOffset by remember { mutableFloatStateOf(existingLink?.coverOffset ?: 50f) }
    var aspectRatio by remember { mutableStateOf(existingLink?.aspectRatio ?: "16:9") }
    var urlHD by remember { mutableStateOf(existingLink?.urlHD ?: "") }
    var url4K by remember { mutableStateOf(existingLink?.url4K ?: "") }
    var magnetHD by remember { mutableStateOf(existingLink?.magnet ?: "") }
    var magnet4K by remember { mutableStateOf(existingLink?.magnet4K ?: "") }
    var galleryScraperUrl by remember { mutableStateOf(existingLink?.galleryScraperUrl ?: "") }
    var galleryUrls by remember { mutableStateOf(existingLink?.galleryUrls ?: emptyList()) }
    var selectedActorIds by remember { mutableStateOf(existingLink?.actorIds ?: emptyList()) }
    var selectedStudioIds by remember { mutableStateOf(existingLink?.studioIds ?: emptyList()) }

    var isScrapingGallery by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = palette.bg,
        topBar = {
            TopAppBar(
                title = { Text(if (existingLink != null) "Edit Scene" else "Add Scene", color = palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val newLink = LinkEntity(
                                    id = existingLink?.id ?: UUID.randomUUID().toString(),
                                    title = title.trim(),
                                    coverImage = coverImage.trim(),
                                    coverOffset = coverOffset,
                                    aspectRatio = aspectRatio,
                                    urlHD = urlHD.trim().ifEmpty { null },
                                    url4K = url4K.trim().ifEmpty { null },
                                    magnet = magnetHD.trim().ifEmpty { null },
                                    magnet4K = magnet4K.trim().ifEmpty { null },
                                    galleryScraperUrl = galleryScraperUrl.trim().ifEmpty { null },
                                    galleryUrls = galleryUrls,
                                    actorIds = selectedActorIds,
                                    studioIds = selectedStudioIds
                                )
                                viewModel.saveLink(newLink)
                                viewModel.navigateBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        enabled = title.isNotBlank(),
                        modifier = Modifier.testTag("save_scene_button")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Scene Title *") },
                modifier = Modifier.fillMaxWidth().testTag("scene_title_input")
            )

            // Cover Image URL & Live Preview
            OutlinedTextField(
                value = coverImage,
                onValueChange = { coverImage = it },
                label = { Text("Cover Image URL") },
                trailingIcon = {
                    if (coverImage.isNotEmpty()) {
                        IconButton(onClick = { coverImage = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("cover_image_input")
            )

            if (coverImage.isNotEmpty()) {
                Text("Cover Preview & Offset", color = palette.textSecondary, fontSize = 13.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surface)
                ) {
                    AsyncImage(
                        model = coverImage,
                        contentDescription = "Cover Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Offset slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Offset: ${coverOffset.toInt()}%", color = palette.textMuted, fontSize = 12.sp)
                    Slider(
                        value = coverOffset,
                        onValueChange = { coverOffset = it },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                }

                // Aspect Ratio Selector
                Text("Aspect Ratio", color = palette.textSecondary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("16:9", "3:2", "5:7", "Adaptive").forEach { ratio ->
                        FilterChip(
                            selected = aspectRatio == ratio,
                            onClick = { aspectRatio = ratio },
                            label = { Text(ratio) }
                        )
                    }
                }
            }

            HorizontalDivider(color = palette.border)

            // Video Stream URLs
            Text("Stream & Media Sources", color = palette.textPrimary, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = urlHD,
                onValueChange = { urlHD = it },
                label = { Text("HD Stream (1080p URL / HLS / Pornhub)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = url4K,
                onValueChange = { url4K = it },
                label = { Text("4K Stream (2160p URL / DASH)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = magnetHD,
                onValueChange = { magnetHD = it },
                label = { Text("Magnet / Torbox Link (HD)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = magnet4K,
                onValueChange = { magnet4K = it },
                label = { Text("Magnet / Torbox Link (4K)") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = palette.border)

            // Photoset / Gallery Scraper
            Text("Photoset Gallery Scraper", color = palette.textPrimary, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = galleryScraperUrl,
                    onValueChange = { galleryScraperUrl = it },
                    label = { Text("AdultPhotoSets / Gallery Page URL") },
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        if (galleryScraperUrl.isNotBlank()) {
                            isScrapingGallery = true
                            coroutineScope.launch {
                                val result = MediaScrapers.scrapeGallery(galleryScraperUrl)
                                if (result.images.isNotEmpty()) {
                                    galleryUrls = result.images
                                    if (coverImage.isEmpty() && result.coverImage != null) {
                                        coverImage = result.coverImage
                                    }
                                    if (title.isEmpty() && result.title.isNotEmpty()) {
                                        title = result.title
                                    }
                                }
                                isScrapingGallery = false
                            }
                        }
                    },
                    enabled = !isScrapingGallery && galleryScraperUrl.isNotBlank()
                ) {
                    if (isScrapingGallery) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Scrape")
                    }
                }
            }

            if (galleryUrls.isNotEmpty()) {
                Text("${galleryUrls.size} images extracted for gallery", color = Color(0xFF10B981), fontSize = 12.sp)
            }

            HorizontalDivider(color = palette.border)

            // Actor Tagging
            Text("Tag Actors", color = palette.textPrimary, fontWeight = FontWeight.Bold)
            if (actors.isEmpty()) {
                Text("No actors added yet. Go to Actors in menu to create actors.", color = palette.textMuted, fontSize = 13.sp)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    actors.forEach { actor ->
                        val isSelected = selectedActorIds.contains(actor.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedActorIds = if (isSelected) {
                                    selectedActorIds - actor.id
                                } else {
                                    selectedActorIds + actor.id
                                }
                            },
                            label = { Text(actor.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
