package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DownloadLog
import com.example.model.DownloadQueueItem
import com.example.model.MediaFormat
import com.example.model.ResolvedVideoInfo
import com.example.repository.DownloadRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class VidSaverViewModel(
    application: Application,
    private val repository: DownloadRepository
) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("vidsaver_preferences", Context.MODE_PRIVATE)

    // UI Input states
    val inputUrl = MutableStateFlow("")
    val isResolving = MutableStateFlow(false)
    val resolvedVideoInfo = MutableStateFlow<ResolvedVideoInfo?>(null)
    val errorMessage = MutableStateFlow<String?>(null)

    // Settings States
    val settingsApiUrl = MutableStateFlow("")
    val settingsAutoPaste = MutableStateFlow(true)

    // Active download lists from Repository State Flow
    val activeQueue: StateFlow<List<DownloadQueueItem>> = repository.activeQueue

    // Search and filter logs state
    val historySearchQuery = MutableStateFlow("")
    val historyPlatformFilter = MutableStateFlow<String?>(null)

    // Historical downloads from SQLite
    val allDownloads: StateFlow<List<DownloadLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredDownloads: StateFlow<List<DownloadLog>> = combine(
        allDownloads,
        historySearchQuery,
        historyPlatformFilter
    ) { logs, query, platform ->
        logs.filter { log ->
            val matchesQuery = query.isBlank() || log.title.contains(query, ignoreCase = true)
            val matchesPlatform = platform == null || log.platform.equals(platform, ignoreCase = true)
            matchesQuery && matchesPlatform
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Conversion and processing feedback logs
    val isConverting = MutableStateFlow(false)
    val conversionMessage = MutableStateFlow<String?>(null)

    init {
        loadSettings()
    }

    private fun loadSettings() {
        settingsApiUrl.value = sharedPrefs.getString("custom_api_url", "") ?: ""
        settingsAutoPaste.value = sharedPrefs.getBoolean("auto_paste_on_start", true)
    }

    fun updateSettings(apiUrl: String, autoPaste: Boolean) {
        viewModelScope.launch {
            sharedPrefs.edit()
                .putString("custom_api_url", apiUrl.trim())
                .putBoolean("auto_paste_on_start", autoPaste)
                .apply()
            settingsApiUrl.value = apiUrl.trim()
            settingsAutoPaste.value = autoPaste
        }
    }

    fun onUrlInputChanged(newUrl: String) {
        inputUrl.value = newUrl
        errorMessage.value = null
    }

    /**
     * Resolves the current URL string to construct media download formats.
     */
    fun resolveUrl() {
        val url = inputUrl.value.trim()
        if (url.isEmpty()) {
            errorMessage.value = "Please enter or paste a valid video link"
            return
        }

        viewModelScope.launch {
            isResolving.value = true
            errorMessage.value = null
            resolvedVideoInfo.value = null

            val result = repository.resolveVideoUrl(url, settingsApiUrl.value.ifBlank { null })
            isResolving.value = false

            result.fold(
                onSuccess = { info ->
                    resolvedVideoInfo.value = info
                },
                onFailure = { err ->
                    errorMessage.value = "Failed to resolve link: ${err.localizedMessage ?: "Invalid URL or connection issue"}"
                }
            )
        }
    }

    fun clearOutputPreview() {
        resolvedVideoInfo.value = null
        inputUrl.value = ""
        errorMessage.value = null
    }

    /**
     * Spawns an dynamic asynchronous download worker.
     */
    fun startFormatDownload(format: MediaFormat) {
        val info = resolvedVideoInfo.value ?: return
        repository.addToQueue(info, format)
        clearOutputPreview() // Clear so they see the batch download transition smoothly
    }

    fun pauseQueueItem(itemId: String) {
        repository.pauseDownload(itemId)
    }

    fun resumeQueueItem(itemId: String) {
        repository.resumeDownload(itemId)
    }

    fun cancelQueueItem(itemId: String) {
        repository.cancelDownload(itemId)
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteHistoryLog(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    /**
     * Audio format transcoders.
     */
    fun convertVideoToAudio(log: DownloadLog, targetFormat: String) {
        viewModelScope.launch {
            isConverting.value = true
            conversionMessage.value = "Transcoding \"${log.title}\" to $targetFormat Audio Stream..."
            
            val result = repository.convertFormat(log, targetFormat)
            isConverting.value = false
            
            result.fold(
                onSuccess = { updated ->
                    conversionMessage.value = "Transcoded and saved: ${updated.title}"
                },
                onFailure = { err ->
                    conversionMessage.value = "Audio conversion failed: ${err.localizedMessage}"
                }
            )
            
            delay(3000)
            conversionMessage.value = null
        }
    }

    fun onClipboardPasted(url: String) {
        inputUrl.value = url.trim()
        resolveUrl()
    }
}

class VidSaverViewModelFactory(
    private val application: Application,
    private val repository: DownloadRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VidSaverViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VidSaverViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
