package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.ActorEntity
import com.example.data.local.entity.LinkEntity
import com.example.data.local.entity.StudioEntity
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette
import com.example.ui.theme.rememberDominantColor
import compose.icons.FeatherIcons
import compose.icons.feathericons.Download
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Image
import compose.icons.feathericons.Play
import compose.icons.feathericons.Trash2

@Composable
fun LinkCard(
    link: LinkEntity,
    actors: List<ActorEntity> = emptyList(),
    studios: List<StudioEntity> = emptyList(),
    onPlay: (url: String) -> Unit,
    onOpenGallery: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val dominantColor = rememberDominantColor(link.coverImage, defaultColor = palette.cardBg)

    // Calculate aspect ratio float
    val aspectMultiplier = when (link.aspectRatio) {
        "3:2" -> 3f / 2f
        "5:7" -> 5f / 7f
        "Adaptive" -> 1f
        else -> 16f / 9f
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
            .testTag("scene_card_${link.id}"),
        colors = CardDefaults.cardColors(containerColor = palette.cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            palette.cardBg,
                            palette.cardBg.copy(alpha = 0.85f),
                            dominantColor.copy(alpha = 0.18f)
                        )
                    )
                )
        ) {
            // Cover Image Container with Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectMultiplier)
                    .background(palette.skeletonBg)
            ) {
                if (link.coverImage.isNotEmpty()) {
                    AsyncImage(
                        model = link.coverImage,
                        contentDescription = link.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                val playUrl = link.url4K ?: link.urlHD ?: link.magnet4K ?: link.magnet
                                if (playUrl != null) onPlay(playUrl)
                            }
                    )
                }

                // Smooth dark gradient overlay at bottom of cover
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                // Top Left: Studio Badge
                val assignedStudio = studios.firstOrNull { link.studioIds.contains(it.id) }
                if (assignedStudio != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = assignedStudio.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Top Right: Quality Badges (4K, 1080p, Gallery count)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (link.url4K != null || link.magnet4K != null) {
                        BadgePill(text = "4K", bg = Color(0xFFEAB308), textColor = Color.Black)
                    }
                    if (link.urlHD != null || link.magnet != null) {
                        BadgePill(text = "1080p", bg = Color(0xFF06B6D4), textColor = Color.Black)
                    }
                    if (link.galleryUrls.isNotEmpty()) {
                        BadgePill(
                            text = "${link.galleryUrls.size}P",
                            bg = Color(0xFF10B981),
                            textColor = Color.White
                        )
                    }
                }

                // Center Play Button Overlay (Feather Play)
                IconButton(
                    onClick = {
                        val playUrl = link.url4K ?: link.urlHD ?: link.magnet4K ?: link.magnet
                        if (playUrl != null) onPlay(playUrl)
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.9f))
                        .testTag("play_button_${link.id}")
                ) {
                    Icon(
                        imageVector = FeatherIcons.Play,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp).offset(x = 1.5.dp)
                    )
                }
            }

            // Info Content
            Column(modifier = Modifier.padding(14.dp)) {
                // Title
                Text(
                    text = link.title,
                    color = palette.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Actors list
                val sceneActors = actors.filter { link.actorIds.contains(it.id) }
                if (sceneActors.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        sceneActors.take(3).forEach { actor ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (actor.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = actor.imageUrl,
                                            contentDescription = actor.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                    }
                                    Text(
                                        text = actor.name,
                                        color = accent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Action Bar (Gallery, HD, 4K, Edit, Delete)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (link.galleryUrls.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = onOpenGallery,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(FeatherIcons.Image, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gallery", fontSize = 11.sp)
                            }
                        }

                        val hdSource = link.urlHD ?: link.magnet
                        val fourKSource = link.url4K ?: link.magnet4K

                        if (hdSource != null) {
                            OutlinedButton(
                                onClick = { onPlay(hdSource) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                if (link.urlHD == null && link.magnet != null) {
                                    Icon(FeatherIcons.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text("HD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (fourKSource != null) {
                            OutlinedButton(
                                onClick = { onPlay(fourKSource) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                if (link.url4K == null && link.magnet4K != null) {
                                    Icon(FeatherIcons.Download, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFEAB308))
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text("4K", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEAB308))
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(34.dp).testTag("edit_scene_${link.id}")) {
                            Icon(FeatherIcons.Edit2, contentDescription = "Edit", tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp).testTag("delete_scene_${link.id}")) {
                            Icon(FeatherIcons.Trash2, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BadgePill(text: String, bg: Color, textColor: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
