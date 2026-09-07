package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entities.ConversationEntity
import com.example.data.local.entities.GeneratedImageEntity
import com.example.data.local.entities.MessageEntity
import com.example.data.local.entities.ProjectEntity
import com.example.data.local.entities.ProjectFileEntity
import com.example.data.local.entities.UserMemoryEntity
import com.example.data.local.entities.UserSettingsEntity
import com.example.data.provider.CodeAction
import com.example.data.provider.ResearchResult
import com.example.data.provider.StudyModeType
import com.example.data.remote.gemini.GeminiClient
import com.example.data.repository.NexoraRepository
import com.example.speech.VoiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

enum class NexoraScreen(val title: String) {
    HOME("Workspace"),
    CHAT("AI Chat"),
    IMAGE_STUDIO("Image Studio"),
    RESEARCH("Deep Research"),
    CODE_ASSISTANT("Code Assistant"),
    WRITER("Document Writer"),
    STUDY("Study Mode"),
    PROJECTS("Projects"),
    FILES("Document AI"),
    MEMORY("Memory"),
    SETTINGS("Settings")
}

data class GlobalSearchResult(
    val title: String,
    val subtitle: String,
    val type: String, // "Conversation", "File", "Image"
    val id: String
)

class NexoraViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = NexoraRepository(database)
    val voiceManager = VoiceManager(application)

    private val _currentScreen = MutableStateFlow(NexoraScreen.HOME)
    val currentScreen: StateFlow<NexoraScreen> = _currentScreen.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = repository.activeConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMessages: StateFlow<List<MessageEntity>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId != null) repository.getMessages(convId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val files: StateFlow<List<ProjectFileEntity>> = repository.allFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val images: StateFlow<List<GeneratedImageEntity>> = repository.allImages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<UserMemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _settings = MutableStateFlow(UserSettingsEntity())
    val settings: StateFlow<UserSettingsEntity> = _settings.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _selectedModel = MutableStateFlow("gemini-3.5-flash")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    // Multimodal Chat State
    private val _pendingImageBase64 = MutableStateFlow<String?>(null)
    val pendingImageBase64: StateFlow<String?> = _pendingImageBase64.asStateFlow()

    // Coding Assistant State
    private val _codePrompt = MutableStateFlow("")
    val codePrompt: StateFlow<String> = _codePrompt.asStateFlow()
    private val _codeLanguage = MutableStateFlow("Kotlin")
    val codeLanguage: StateFlow<String> = _codeLanguage.asStateFlow()
    private val _codeAction = MutableStateFlow(CodeAction.GENERATE)
    val codeAction: StateFlow<CodeAction> = _codeAction.asStateFlow()
    private val _codeResult = MutableStateFlow("")
    val codeResult: StateFlow<String> = _codeResult.asStateFlow()

    // Document Writer State
    private val _writerTopic = MutableStateFlow("")
    val writerTopic: StateFlow<String> = _writerTopic.asStateFlow()
    private val _writerMode = MutableStateFlow("Article")
    val writerMode: StateFlow<String> = _writerMode.asStateFlow()
    private val _writerTone = MutableStateFlow("Professional")
    val writerTone: StateFlow<String> = _writerTone.asStateFlow()
    private val _writerLength = MutableStateFlow("Comprehensive")
    val writerLength: StateFlow<String> = _writerLength.asStateFlow()
    private val _writerResult = MutableStateFlow("")
    val writerResult: StateFlow<String> = _writerResult.asStateFlow()

    // Study Mode State
    private val _studyTopic = MutableStateFlow("")
    val studyTopic: StateFlow<String> = _studyTopic.asStateFlow()
    private val _studyMode = MutableStateFlow(StudyModeType.EXPLAIN)
    val studyMode: StateFlow<StudyModeType> = _studyMode.asStateFlow()
    private val _studyDifficulty = MutableStateFlow("Medium")
    val studyDifficulty: StateFlow<String> = _studyDifficulty.asStateFlow()
    private val _studyResult = MutableStateFlow("")
    val studyResult: StateFlow<String> = _studyResult.asStateFlow()

    // Deep Research State
    private val _researchQuery = MutableStateFlow("")
    val researchQuery: StateFlow<String> = _researchQuery.asStateFlow()
    private val _researchResult = MutableStateFlow<ResearchResult?>(null)
    val researchResult: StateFlow<ResearchResult?> = _researchResult.asStateFlow()

    // Image Studio State
    private val _imagePrompt = MutableStateFlow("")
    val imagePrompt: StateFlow<String> = _imagePrompt.asStateFlow()
    private val _imageAspectRatio = MutableStateFlow("1:1")
    val imageAspectRatio: StateFlow<String> = _imageAspectRatio.asStateFlow()
    private val _imageStylePreset = MutableStateFlow("Digital Art")
    val imageStylePreset: StateFlow<String> = _imageStylePreset.asStateFlow()
    private val _latestGeneratedImage = MutableStateFlow<GeneratedImageEntity?>(null)
    val latestGeneratedImage: StateFlow<GeneratedImageEntity?> = _latestGeneratedImage.asStateFlow()

    // Global Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val searchResults: StateFlow<List<GlobalSearchResult>> = combine(
        _searchQuery,
        conversations,
        files,
        images
    ) { query, convs, fls, imgs ->
        if (query.isBlank()) return@combine emptyList()
        val lower = query.lowercase()
        val list = mutableListOf<GlobalSearchResult>()
        convs.filter { it.title.lowercase().contains(lower) }.forEach {
            list.add(GlobalSearchResult(it.title, "Conversation", "Chat", it.id))
        }
        fls.filter { it.name.lowercase().contains(lower) || it.contentSnippet.lowercase().contains(lower) }.forEach {
            list.add(GlobalSearchResult(it.name, "${it.mimeType} • ${it.sizeBytes} bytes", "File", it.id))
        }
        imgs.filter { it.prompt.lowercase().contains(lower) }.forEach {
            list.add(GlobalSearchResult(it.prompt, "${it.stylePreset} • ${it.aspectRatio}", "Image", it.id))
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            val savedSettings = repository.getSettings()
            _settings.value = savedSettings
            _selectedModel.value = savedSettings.defaultModel

            // Ensure an active conversation exists
            val convList = repository.activeConversations.firstOrNull() ?: emptyList()
            if (convList.isNotEmpty() && _activeConversationId.value == null) {
                _activeConversationId.value = convList.first().id
            }
        }
    }

    fun navigateTo(screen: NexoraScreen) {
        _currentScreen.value = screen
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }

    fun selectConversation(id: String) {
        _activeConversationId.value = id
        _currentScreen.value = NexoraScreen.CHAT
    }

    fun createNewConversation(title: String = "New Conversation", projectId: String? = null) {
        viewModelScope.launch {
            val newId = repository.createConversation(
                title = title,
                modelId = _selectedModel.value,
                projectId = projectId
            )
            _activeConversationId.value = newId
            _currentScreen.value = NexoraScreen.CHAT
        }
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameConversation(id, newTitle)
            showSnackbar("Conversation renamed.")
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (_activeConversationId.value == id) {
                val remaining = repository.activeConversations.firstOrNull() ?: emptyList()
                _activeConversationId.value = remaining.firstOrNull()?.id
            }
            showSnackbar("Conversation deleted.")
        }
    }

    fun setPendingImageBase64(base64: String?) {
        _pendingImageBase64.value = base64
    }

    fun attachImageFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                    val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                    _pendingImageBase64.value = base64
                    withContext(Dispatchers.Main) {
                        showSnackbar("Image attached successfully.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showSnackbar("Failed to attach image: ${e.message}")
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val convId = _activeConversationId.value ?: return
        if (text.isBlank() && _pendingImageBase64.value == null) return

        val img = _pendingImageBase64.value
        _pendingImageBase64.value = null
        _isGenerating.value = true

        viewModelScope.launch {
            val result = repository.sendMessage(convId, text, img)
            _isGenerating.value = false
            result.onFailure { error ->
                showSnackbar("Error: ${error.localizedMessage}")
            }
        }
    }

    fun regenerateLastMessage() {
        val convId = _activeConversationId.value ?: return
        val msgs = activeMessages.value
        val lastUserMsg = msgs.findLast { it.role == "user" }
        if (lastUserMsg != null) {
            sendMessage(lastUserMsg.content)
        }
    }

    fun setModel(model: String) {
        _selectedModel.value = model
        viewModelScope.launch {
            val current = _settings.value
            val updated = current.copy(defaultModel = model)
            repository.saveSettings(updated)
            _settings.value = updated
        }
        showSnackbar("Model switched to $model")
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            val updated = _settings.value.copy(themeMode = mode)
            repository.saveSettings(updated)
            _settings.value = updated
            showSnackbar("Theme set to $mode")
        }
    }

    fun updateTtsSpeed(speed: Float) {
        viewModelScope.launch {
            val updated = _settings.value.copy(ttsVoiceSpeed = speed)
            repository.saveSettings(updated)
            _settings.value = updated
        }
    }

    fun speakText(text: String) {
        voiceManager.speak(text, _settings.value.ttsVoiceSpeed)
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    // Code Assistant Actions
    fun setCodePrompt(prompt: String) { _codePrompt.value = prompt }
    fun setCodeLanguage(lang: String) { _codeLanguage.value = lang }
    fun setCodeAction(action: CodeAction) { _codeAction.value = action }

    fun executeCodeAssist() {
        if (_codePrompt.value.isBlank()) return
        _isGenerating.value = true
        viewModelScope.launch {
            val result = repository.aiProvider.assistCode(
                codeOrPrompt = _codePrompt.value,
                language = _codeLanguage.value,
                action = _codeAction.value,
                modelId = "gemini-3.1-pro-preview"
            )
            _isGenerating.value = false
            result.fold(
                onSuccess = { _codeResult.value = it },
                onFailure = {
                    _codeResult.value = "Failed: ${it.localizedMessage}"
                    showSnackbar("Code execution failed: ${it.localizedMessage}")
                }
            )
        }
    }

    // Document Writer Actions
    fun setWriterTopic(topic: String) { _writerTopic.value = topic }
    fun setWriterMode(mode: String) { _writerMode.value = mode }
    fun setWriterTone(tone: String) { _writerTone.value = tone }
    fun setWriterLength(len: String) { _writerLength.value = len }

    fun executeDocumentWrite() {
        if (_writerTopic.value.isBlank()) return
        _isGenerating.value = true
        viewModelScope.launch {
            val result = repository.aiProvider.writeDocument(
                topic = _writerTopic.value,
                mode = _writerMode.value,
                tone = _writerTone.value,
                length = _writerLength.value
            )
            _isGenerating.value = false
            result.fold(
                onSuccess = { _writerResult.value = it },
                onFailure = {
                    _writerResult.value = "Failed: ${it.localizedMessage}"
                    showSnackbar("Document writer error: ${it.localizedMessage}")
                }
            )
        }
    }

    // Study Mode Actions
    fun setStudyTopic(topic: String) { _studyTopic.value = topic }
    fun setStudyMode(mode: StudyModeType) { _studyMode.value = mode }
    fun setStudyDifficulty(diff: String) { _studyDifficulty.value = diff }

    fun executeStudyAssist() {
        if (_studyTopic.value.isBlank()) return
        _isGenerating.value = true
        viewModelScope.launch {
            val result = repository.aiProvider.studyAssist(
                topic = _studyTopic.value,
                mode = _studyMode.value,
                difficulty = _studyDifficulty.value
            )
            _isGenerating.value = false
            result.fold(
                onSuccess = { _studyResult.value = it },
                onFailure = {
                    _studyResult.value = "Failed: ${it.localizedMessage}"
                    showSnackbar("Study engine error: ${it.localizedMessage}")
                }
            )
        }
    }

    // Research Actions
    fun setResearchQuery(query: String) { _researchQuery.value = query }

    fun executeResearch() {
        if (_researchQuery.value.isBlank()) return
        _isGenerating.value = true
        viewModelScope.launch {
            val result = repository.aiProvider.conductResearch(_researchQuery.value)
            _isGenerating.value = false
            result.fold(
                onSuccess = { _researchResult.value = it },
                onFailure = {
                    showSnackbar("Research error: ${it.localizedMessage}")
                }
            )
        }
    }

    // Image Studio Actions
    fun setImagePrompt(prompt: String) { _imagePrompt.value = prompt }
    fun setImageAspectRatio(ratio: String) { _imageAspectRatio.value = ratio }
    fun setImageStylePreset(preset: String) { _imageStylePreset.value = preset }

    fun generateImage() {
        if (_imagePrompt.value.isBlank()) return
        _isGenerating.value = true
        viewModelScope.launch {
            val result = repository.generateImage(
                prompt = _imagePrompt.value,
                aspectRatio = _imageAspectRatio.value,
                stylePreset = _imageStylePreset.value
            )
            _isGenerating.value = false
            result.fold(
                onSuccess = {
                    _latestGeneratedImage.value = it
                    showSnackbar("Image created successfully in Image Studio!")
                },
                onFailure = {
                    showSnackbar("Image generation error: ${it.localizedMessage}")
                }
            )
        }
    }

    fun deleteImage(id: String) {
        viewModelScope.launch {
            repository.deleteImage(id)
            if (_latestGeneratedImage.value?.id == id) {
                _latestGeneratedImage.value = null
            }
            showSnackbar("Image deleted from gallery.")
        }
    }

    // Project Actions
    fun createProject(name: String, description: String, systemPrompt: String, colorHex: String) {
        viewModelScope.launch {
            repository.createProject(name, description, systemPrompt, colorHex)
            showSnackbar("Project '$name' created.")
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
            showSnackbar("Project removed.")
        }
    }

    // File Actions
    fun uploadFileFromUri(uri: Uri, projectId: String? = null) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = repository.storageProvider.readDocumentUri(context, uri)
            result.fold(
                onSuccess = { meta ->
                    val fileId = repository.saveUploadedFile(
                        name = meta.name,
                        mimeType = meta.mimeType,
                        sizeBytes = meta.sizeBytes,
                        textContent = meta.textContent,
                        projectId = projectId
                    )
                    showSnackbar("File '${meta.name}' uploaded successfully.")
                },
                onFailure = {
                    showSnackbar("File upload failed: ${it.message}")
                }
            )
        }
    }

    fun uploadSampleFile(name: String, content: String, mimeType: String, projectId: String? = null) {
        viewModelScope.launch {
            val meta = repository.storageProvider.createSampleFile(name, content, mimeType)
            repository.saveUploadedFile(
                name = meta.name,
                mimeType = meta.mimeType,
                sizeBytes = meta.sizeBytes,
                textContent = meta.textContent,
                projectId = projectId
            )
            showSnackbar("Sample file '$name' created.")
        }
    }

    fun analyzeFile(fileId: String) {
        _isGenerating.value = true
        viewModelScope.launch {
            val result = repository.analyzeDocument(fileId)
            _isGenerating.value = false
            result.fold(
                onSuccess = { showSnackbar("Document analyzed successfully.") },
                onFailure = { showSnackbar("Document analysis failed: ${it.message}") }
            )
        }
    }

    fun deleteFile(id: String) {
        viewModelScope.launch {
            repository.deleteFile(id)
            showSnackbar("File deleted.")
        }
    }

    // Memory Actions
    fun addMemory(key: String, content: String, category: String) {
        viewModelScope.launch {
            repository.addMemory(key, content, category)
            showSnackbar("Memory saved to user profile.")
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            repository.deleteMemory(id)
            showSnackbar("Memory removed.")
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearAllMemories()
            showSnackbar("All user memories cleared.")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _activeConversationId.value = null
            repository.seedInitialDataIfEmpty()
            showSnackbar("All workspace data has been reset.")
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.cleanup()
    }
}
