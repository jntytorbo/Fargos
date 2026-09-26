package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.SortMode
import com.example.ui.components.LinkCard
import com.example.ui.components.PaginationBar
import com.example.ui.components.StaggeredItemAnimation
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette
import compose.icons.FeatherIcons
import compose.icons.feathericons.Film
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Search
import compose.icons.feathericons.Sliders
import compose.icons.feathericons.X

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current

    val links by viewModel.filteredLinks.collectAsStateWithLifecycle()
    val actors by viewModel.allActors.collectAsStateWithLifecycle()
    val studios by viewModel.allStudios.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentSort by viewModel.sortMode.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    val pageSize = 12

    val totalPages = (links.size + pageSize - 1) / pageSize
    val displayedLinks = links.drop((currentPage - 1) * pageSize).take(pageSize)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = palette.bg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(ScreenState.AddEditLink()) },
                containerColor = accent,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_scene_fab")
            ) {
                Icon(FeatherIcons.Plus, contentDescription = "Add Scene", modifier = Modifier.size(24.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search and Sort Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        viewModel.searchQuery.value = it
                        currentPage = 1
                    },
                    placeholder = { Text("Search scenes, actors...", fontSize = 14.sp, color = palette.textMuted) },
                    leadingIcon = { Icon(FeatherIcons.Search, contentDescription = "Search", tint = palette.textMuted, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(FeatherIcons.X, contentDescription = "Clear", tint = palette.textMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.surface,
                        unfocusedContainerColor = palette.surface,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = palette.border,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_scenes_input")
                )

                // Sort Dropdown
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.surface)
                    ) {
                        Icon(FeatherIcons.Sliders, contentDescription = "Sort", tint = palette.textPrimary, modifier = Modifier.size(18.dp))
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Newest Added") },
                            onClick = {
                                viewModel.sortMode.value = SortMode.NEWEST
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest Added") },
                            onClick = {
                                viewModel.sortMode.value = SortMode.OLDEST
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Title (A-Z)") },
                            onClick = {
                                viewModel.sortMode.value = SortMode.TITLE_AZ
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            // Scenes List or Empty State
            if (displayedLinks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = FeatherIcons.Film,
                            contentDescription = null,
                            tint = palette.textMuted,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No scenes match '$searchQuery'" else "Vault Standby",
                            color = palette.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try clearing your search query" else "Tap '+' to add your first scene, or load the test sample scenes.",
                            color = palette.textMuted,
                            fontSize = 14.sp
                        )
                        if (searchQuery.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.loadTestSamples() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Load 5 Test Scenes", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    itemsIndexed(displayedLinks, key = { _, it -> it.id }) { index, link ->
                        StaggeredItemAnimation(
                            index = index,
                            modifier = Modifier.animateItem()
                        ) {
                            LinkCard(
                                link = link,
                                actors = actors,
                                studios = studios,
                                onPlay = { url -> viewModel.playVideo(url, link.title) },
                                onOpenGallery = {
                                    viewModel.navigateTo(ScreenState.PhotosetViewer(link.title, link.galleryUrls))
                                },
                                onEdit = {
                                    viewModel.navigateTo(ScreenState.AddEditLink(link.id))
                                },
                                onDelete = {
                                    viewModel.deleteLink(link.id)
                                }
                            )
                        }
                    }

                    if (totalPages > 1) {
                        item {
                            PaginationBar(
                                currentPage = currentPage,
                                totalPages = totalPages,
                                onPageSelected = { currentPage = it }
                            )
                        }
                    }
                }
            }
        }
    }
}
