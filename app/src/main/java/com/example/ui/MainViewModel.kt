package com.example.ui

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.VaultRepository
import com.example.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

enum class SortMode {
    NEWEST, OLDEST, TITLE_AZ, TITLE_ZA
}

sealed class ScreenState {
    object Home : ScreenState()
    data class AddEditLink(val linkId: String? = null) : ScreenState()
    object Actors : ScreenState()
    data class AddEditActor(val actorId: String? = null) : ScreenState()
    data class ActorScenes(val actorId: String) : ScreenState()
    object Studios : ScreenState()
    data class AddEditStudio(val studioId: String? = null) : ScreenState()
    data class StudioScenes(val studioId: String) : ScreenState()
    object Coomers : ScreenState()
    data class AddEditCoomer(val coomerId: String? = null) : ScreenState()
    data class CoomerDetail(val coomerId: String) : ScreenState()
    object Hanime : ScreenState()
    data class AddEditHanime(val hanimeId: String? = null) : ScreenState()
    data class HanimeDetail(val hanimeId: String) : ScreenState()
    data class PhotosetViewer(val title: String, val images: List<String>, val initialIndex: Int = 0) : ScreenState()
    object Settings : ScreenState()
}

data class ActiveVideoPlayback(
    val title: String,
    val qualities: List<StreamQuality>,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val headers: Map<String, String> = emptyMap()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: VaultRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = VaultRepository(db)

        viewModelScope.launch(Dispatchers.IO) {
            val currentLinks = repository.allLinks.first()
            if (currentLinks.isEmpty()) {
                com.example.data.TestSampleData.seed(repository)
            }
        }
    }

    // Navigation Stack / Current Screen
    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Home)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val screenStack = mutableListOf<ScreenState>(ScreenState.Home)

    fun navigateTo(screen: ScreenState) {
        screenStack.add(screen)
        _screenState.value = screen
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.size - 1)
            _screenState.value = screenStack.last()
            return true
        }
        return false
    }

    // Active Video Player Overlay State
    private val _activeVideo = MutableStateFlow<ActiveVideoPlayback?>(null)
    val activeVideo: StateFlow<ActiveVideoPlayback?> = _activeVideo.asStateFlow()

    // Video Resolution Loading & Error States
    private val _resolvingVideoStatus = MutableStateFlow<String?>(null)
    val resolvingVideoStatus: StateFlow<String?> = _resolvingVideoStatus.asStateFlow()

    private val _videoResolutionError = MutableStateFlow<String?>(null)
    val videoResolutionError: StateFlow<String?> = _videoResolutionError.asStateFlow()

    private var resolveVideoJob: kotlinx.coroutines.Job? = null

    fun playVideo(rawUrl: String, title: String = "Media Stream") {
        resolveVideoJob?.cancel()
        _videoResolutionError.value = null

        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            _videoResolutionError.value = "Cannot play empty stream URL."
            return
        }

        resolveVideoJob = viewModelScope.launch {
            _resolvingVideoStatus.value = if (trimmed.startsWith("magnet:", ignoreCase = true) || trimmed.matches(Regex("^[a-fA-F0-9]{40}$"))) {
                "Resolving torrent via Debrid cloud (Torbox / Real-Debrid)..."
            } else {
                "Resolving media stream..."
            }

            try {
                val settings = repository.getSettingsOnce()
                val resolved = VideoResolvers.resolve(
                    rawUrl = trimmed,
                    torboxApiKey = settings.torboxApiKey,
                    realDebridApiKey = settings.realDebridApiKey
                )

                if (resolved.qualities.isEmpty()) {
                    _videoResolutionError.value = "No playable media qualities found for this source."
                    _resolvingVideoStatus.value = null
                    return@launch
                }

                _activeVideo.value = ActiveVideoPlayback(
                    title = if (resolved.title.isNotBlank() && resolved.title != "Media Stream") resolved.title else title,
                    qualities = resolved.qualities,
                    subtitles = resolved.subtitles,
                    headers = resolved.headers
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _videoResolutionError.value = e.message ?: "Failed to resolve media stream"
            } finally {
                _resolvingVideoStatus.value = null
            }
        }
    }

    fun dismissVideoError() {
        _videoResolutionError.value = null
    }

    fun closeVideo() {
        _activeVideo.value = null
    }

    // Active Photoset Lightbox State
    private val _activeLightbox = MutableStateFlow<Pair<List<String>, Int>?>(null)
    val activeLightbox: StateFlow<Pair<List<String>, Int>?> = _activeLightbox.asStateFlow()

    fun openLightbox(images: List<String>, startIndex: Int = 0) {
        _activeLightbox.value = images to startIndex
    }

    fun closeLightbox() {
        _activeLightbox.value = null
    }

    // Search and Sort
    val searchQuery = MutableStateFlow("")
    val sortMode = MutableStateFlow(SortMode.NEWEST)

    // Data Flows
    val allLinks = repository.allLinks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allActors = repository.allActors.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allStudios = repository.allStudios.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allHanime = repository.allHanime.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCoomers = repository.allCoomers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = repository.settings.map { it ?: SettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsEntity())

    // Filtered scenes based on search and sort
    val filteredLinks = combine(allLinks, searchQuery, sortMode) { links, query, sort ->
        var list = if (query.isBlank()) {
            links
        } else {
            val q = query.trim().lowercase()
            links.filter { it.title.lowercase().contains(q) }
        }

        when (sort) {
            SortMode.NEWEST -> list.sortedByDescending { it.createdAt }
            SortMode.OLDEST -> list.sortedBy { it.createdAt }
            SortMode.TITLE_AZ -> list.sortedBy { it.title.lowercase() }
            SortMode.TITLE_ZA -> list.sortedByDescending { it.title.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // CRUD operations
    fun saveLink(link: LinkEntity) {
        viewModelScope.launch {
            repository.insertLink(link)
        }
    }

    fun deleteLink(id: String) {
        viewModelScope.launch {
            repository.deleteLinkById(id)
        }
    }

    fun saveActor(actor: ActorEntity) {
        viewModelScope.launch {
            repository.insertActor(actor)
        }
    }

    fun deleteActor(id: String) {
        viewModelScope.launch {
            repository.deleteActorById(id)
        }
    }

    fun saveStudio(studio: StudioEntity) {
        viewModelScope.launch {
            repository.insertStudio(studio)
        }
    }

    fun deleteStudio(id: String) {
        viewModelScope.launch {
            repository.deleteStudioById(id)
        }
    }

    fun saveHanime(hanime: HanimeEntity) {
        viewModelScope.launch {
            repository.insertHanime(hanime)
        }
    }

    fun deleteHanime(id: String) {
        viewModelScope.launch {
            repository.deleteHanimeById(id)
        }
    }

    fun saveCoomer(coomer: CoomerEntity) {
        viewModelScope.launch {
            repository.insertCoomer(coomer)
        }
    }

    fun deleteCoomer(id: String) {
        viewModelScope.launch {
            repository.deleteCoomerById(id)
        }
    }

    fun updateSettings(newSettings: SettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
        }
    }

    // Auto-match actors & studios by scene title (System Check)
    fun runSystemCheck(onComplete: (matched: Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val links = repository.allLinks.first()
            val actors = repository.allActors.first()
            val studios = repository.allStudios.first()
            var matchCount = 0

            for (link in links) {
                val foundActors = actors.filter {
                    it.name.isNotBlank() && link.title.contains(it.name, ignoreCase = true)
                }.map { it.id }

                val foundStudios = studios.filter {
                    it.name.isNotBlank() && link.title.contains(it.name, ignoreCase = true)
                }.map { it.id }

                val newActorIds = (link.actorIds + foundActors).distinct()
                val newStudioIds = (link.studioIds + foundStudios).distinct()

                if (newActorIds != link.actorIds || newStudioIds != link.studioIds) {
                    repository.updateLink(
                        link.copy(actorIds = newActorIds, studioIds = newStudioIds)
                    )
                    matchCount++
                }
            }
            onComplete(matchCount)
        }
    }

    // Export JSON string
    suspend fun exportDataJson(): String {
        val root = JSONObject()
        val linksArr = JSONArray()
        repository.allLinks.first().forEach { l ->
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("title", l.title)
            obj.put("coverImage", l.coverImage)
            obj.put("urlHD", l.urlHD ?: JSONObject.NULL)
            obj.put("url4K", l.url4K ?: JSONObject.NULL)
            obj.put("aspectRatio", l.aspectRatio)
            linksArr.put(obj)
        }
        root.put("links", linksArr)
        return root.toString(2)
    }

    // Supabase Cloud Sync
    fun syncWithSupabase(onStatus: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val currSettings = repository.getSettingsOnce()
            if (currSettings.supabaseUrl.isBlank() || currSettings.supabaseAnonKey.isBlank()) {
                onStatus("Supabase URL or Key not set")
                return@launch
            }

            try {
                onStatus("Syncing with Supabase...")
                val exportStr = exportDataJson()
                val compressed = compressGzip(exportStr)
                val base64Data = Base64.encodeToString(compressed, Base64.NO_WRAP)

                val bodyJson = JSONObject().apply {
                    put("id", currSettings.supabaseUserId.ifEmpty { UUID.randomUUID().toString() })
                    put("data", base64Data)
                    put("updated_at", System.currentTimeMillis())
                }

                val req = Request.Builder()
                    .url("${currSettings.supabaseUrl}/rest/v1/user_data")
                    .header("apikey", currSettings.supabaseAnonKey)
                    .header("Authorization", "Bearer ${currSettings.supabaseAnonKey}")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val resp = NetworkClient.okHttpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    repository.updateSettings(currSettings.copy(lastSyncTime = System.currentTimeMillis()))
                    onStatus("Synced successfully!")
                } else {
                    onStatus("Sync HTTP ${resp.code}")
                }
            } catch (e: Exception) {
                onStatus("Sync failed: ${e.message}")
            }
        }
    }

    fun loadTestSamples(onComplete: (String) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.data.TestSampleData.seed(repository)
                onComplete("5 Test scenes loaded successfully!")
            } catch (e: Exception) {
                onComplete("Failed to load test scenes: ${e.message}")
            }
        }
    }

    private fun compressGzip(str: String): ByteArray {
        val byteOut = ByteArrayOutputStream()
        GZIPOutputStream(byteOut).use { it.write(str.toByteArray(Charsets.UTF_8)) }
        return byteOut.toByteArray()
    }
}
