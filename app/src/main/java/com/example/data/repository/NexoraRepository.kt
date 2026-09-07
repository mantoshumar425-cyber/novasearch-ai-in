package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entities.ConversationEntity
import com.example.data.local.entities.GeneratedImageEntity
import com.example.data.local.entities.MessageEntity
import com.example.data.local.entities.ProjectEntity
import com.example.data.local.entities.ProjectFileEntity
import com.example.data.local.entities.UserMemoryEntity
import com.example.data.local.entities.UserSettingsEntity
import com.example.data.provider.AIProvider
import com.example.data.provider.ChatMessage
import com.example.data.provider.CodeAction
import com.example.data.provider.ImageGenerationParams
import com.example.data.provider.ImageProvider
import com.example.data.provider.NexoraGeminiProvider
import com.example.data.provider.NexoraImageProvider
import com.example.data.provider.NexoraStorageProvider
import com.example.data.provider.ResearchResult
import com.example.data.provider.StorageProvider
import com.example.data.provider.StreamChunk
import com.example.data.provider.StudyModeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

class NexoraRepository(
    private val database: AppDatabase,
    val aiProvider: AIProvider = NexoraGeminiProvider(),
    val imageProvider: ImageProvider = NexoraImageProvider(),
    val storageProvider: StorageProvider = NexoraStorageProvider()
) {

    val activeConversations: Flow<List<ConversationEntity>> = database.conversationDao().getActiveConversations()
    val allProjects: Flow<List<ProjectEntity>> = database.projectDao().getAllProjects()
    val allFiles: Flow<List<ProjectFileEntity>> = database.projectFileDao().getAllFiles()
    val allImages: Flow<List<GeneratedImageEntity>> = database.generatedImageDao().getAllImages()
    val allMemories: Flow<List<UserMemoryEntity>> = database.memoryDao().getAllMemories()
    val settingsFlow: Flow<UserSettingsEntity?> = database.settingsDao().getSettingsFlow()

    fun getMessages(conversationId: String): Flow<List<MessageEntity>> {
        return database.messageDao().getMessages(conversationId)
    }

    suspend fun getSettings(): UserSettingsEntity {
        return database.settingsDao().getSettings() ?: UserSettingsEntity().also {
            database.settingsDao().saveSettings(it)
        }
    }

    suspend fun saveSettings(settings: UserSettingsEntity) {
        database.settingsDao().saveSettings(settings)
    }

    suspend fun createConversation(
        title: String = "New Conversation",
        modelId: String = "gemini-3.5-flash",
        projectId: String? = null
    ): String {
        val id = UUID.randomUUID().toString()
        val conversation = ConversationEntity(
            id = id,
            title = title,
            modelId = modelId,
            projectId = projectId
        )
        database.conversationDao().insertConversation(conversation)
        return id
    }

    suspend fun renameConversation(id: String, newTitle: String) {
        val conv = database.conversationDao().getConversationById(id) ?: return
        database.conversationDao().updateConversation(conv.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteConversation(id: String) {
        database.messageDao().deleteMessagesForConversation(id)
        database.conversationDao().deleteConversation(id)
    }

    suspend fun archiveConversation(id: String, archive: Boolean) {
        val conv = database.conversationDao().getConversationById(id) ?: return
        database.conversationDao().updateConversation(conv.copy(isArchived = archive, updatedAt = System.currentTimeMillis()))
    }

    suspend fun sendMessage(
        conversationId: String,
        userContent: String,
        imageBase64: String? = null
    ): Result<String> {
        val userMessageId = UUID.randomUUID().toString()
        val userMessage = MessageEntity(
            id = userMessageId,
            conversationId = conversationId,
            role = "user",
            content = userContent,
            attachmentsJson = imageBase64?.let { "image/jpeg" }
        )
        database.messageDao().insertMessage(userMessage)

        val conv = database.conversationDao().getConversationById(conversationId)
        val modelId = conv?.modelId ?: "gemini-3.5-flash"

        // Update conversation title if first message
        if (conv != null && conv.title == "New Conversation") {
            val autoTitle = userContent.take(30).trim()
            database.conversationDao().updateConversation(
                conv.copy(title = if (autoTitle.isNotBlank()) autoTitle else "Chat", updatedAt = System.currentTimeMillis())
            )
        }

        // Fetch project system prompt if linked
        val project = conv?.projectId?.let { database.projectDao().getProjectById(it) }
        val systemPrompt = project?.systemInstruction

        // Fetch recent messages for context
        val recentMessages = database.messageDao().getMessages(conversationId).firstOrNull() ?: emptyList()
        val chatHistory = recentMessages.takeLast(10).map {
            ChatMessage(role = it.role, content = it.content)
        }

        val aiResult = if (imageBase64 != null) {
            aiProvider.analyzeImage(userContent, imageBase64, "image/jpeg", modelId)
        } else {
            aiProvider.chat(userContent, modelId, chatHistory, systemPrompt)
        }

        val assistantMessageId = UUID.randomUUID().toString()
        val assistantMessage = aiResult.fold(
            onSuccess = { reply ->
                MessageEntity(
                    id = assistantMessageId,
                    conversationId = conversationId,
                    role = "model",
                    content = reply,
                    modelUsed = modelId,
                    isError = false
                )
            },
            onFailure = { error ->
                MessageEntity(
                    id = assistantMessageId,
                    conversationId = conversationId,
                    role = "model",
                    content = "NEXORA encountered an issue: ${error.localizedMessage ?: "Unknown error"}\n\nTip: Verify that your GEMINI_API_KEY is configured in AI Studio Secrets.",
                    modelUsed = modelId,
                    isError = true
                )
            }
        )

        database.messageDao().insertMessage(assistantMessage)
        conv?.let {
            database.conversationDao().updateConversation(it.copy(updatedAt = System.currentTimeMillis()))
        }

        return aiResult
    }

    suspend fun createProject(
        name: String,
        description: String,
        systemInstruction: String,
        colorHex: String = "#8E7DF9"
    ): String {
        val id = UUID.randomUUID().toString()
        val project = ProjectEntity(
            id = id,
            name = name,
            description = description,
            systemInstruction = systemInstruction,
            colorHex = colorHex
        )
        database.projectDao().insertProject(project)
        return id
    }

    suspend fun deleteProject(id: String) {
        database.projectDao().deleteProject(id)
    }

    suspend fun saveUploadedFile(
        name: String,
        mimeType: String,
        sizeBytes: Long,
        textContent: String,
        projectId: String? = null
    ): String {
        val id = UUID.randomUUID().toString()
        val file = ProjectFileEntity(
            id = id,
            projectId = projectId,
            name = name,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            contentSnippet = textContent.take(4000)
        )
        database.projectFileDao().insertFile(file)
        return id
    }

    suspend fun analyzeDocument(fileId: String, customPrompt: String? = null): Result<String> {
        val files = database.projectFileDao().getAllFiles().firstOrNull() ?: emptyList()
        val file = files.find { it.id == fileId } ?: return Result.failure(Exception("File not found"))

        val prompt = customPrompt ?: "Summarize the key takeaways, structural sections, and critical points from this document:\n\n${file.name}\n\n${file.contentSnippet}"
        val result = aiProvider.chat(prompt, "gemini-3.5-flash", emptyList(), "You are NEXORA Document Intelligence Agent.")

        result.onSuccess { analysis ->
            database.projectFileDao().insertFile(file.copy(analysis = analysis))
        }
        return result
    }

    suspend fun deleteFile(id: String) {
        database.projectFileDao().deleteFile(id)
    }

    suspend fun generateImage(
        prompt: String,
        aspectRatio: String = "1:1",
        stylePreset: String = "Digital Art"
    ): Result<GeneratedImageEntity> {
        val params = ImageGenerationParams(
            prompt = prompt,
            aspectRatio = aspectRatio,
            stylePreset = stylePreset
        )
        val result = imageProvider.generateImage(params)
        return result.map { generated ->
            val entity = GeneratedImageEntity(
                id = UUID.randomUUID().toString(),
                prompt = prompt,
                aspectRatio = aspectRatio,
                imageData = generated.base64Data,
                stylePreset = stylePreset
            )
            database.generatedImageDao().insertImage(entity)
            entity
        }
    }

    suspend fun deleteImage(id: String) {
        database.generatedImageDao().deleteImage(id)
    }

    suspend fun addMemory(key: String, content: String, category: String) {
        val memory = UserMemoryEntity(
            id = UUID.randomUUID().toString(),
            key = key,
            content = content,
            category = category
        )
        database.memoryDao().insertMemory(memory)
    }

    suspend fun deleteMemory(id: String) {
        database.memoryDao().deleteMemory(id)
    }

    suspend fun clearAllMemories() {
        database.memoryDao().clearAllMemories()
    }

    suspend fun clearAllData() {
        database.conversationDao().clearAllConversations()
        database.messageDao().clearAllMessages()
        database.projectDao().clearAllProjects()
        database.projectFileDao().clearAllFiles()
        database.generatedImageDao().clearAllImages()
        database.memoryDao().clearAllMemories()
    }

    suspend fun seedInitialDataIfEmpty() {
        val existing = database.conversationDao().getActiveConversations().firstOrNull()
        if (existing.isNullOrEmpty()) {
            val welcomeConvId = UUID.randomUUID().toString()
            database.conversationDao().insertConversation(
                ConversationEntity(
                    id = welcomeConvId,
                    title = "Welcome to NEXORA AI",
                    modelId = "gemini-3.5-flash",
                    isPinned = true
                )
            )

            database.messageDao().insertMessage(
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = welcomeConvId,
                    role = "model",
                    content = """
                        # Welcome to NEXORA AI Workspace ⚡
                        
                        I am **NEXORA**, your unified multimodal intelligence platform. Here is what we can accomplish together:
                        
                        - 💬 **Multimodal Conversational AI**: Ask questions, brainstorm, and inspect ideas.
                        - 🖼️ **Image Studio**: Generate digital artwork and analyze uploaded pictures.
                        - 💻 **AI Code Assistant**: Generate, debug, explain, and review syntax in 15+ languages.
                        - 🔍 **Deep Web Research**: Autonomous multi-source query decomposition with citations.
                        - 📄 **Document & PDF Intelligence**: Summarize, extract insights, and ask questions about files.
                        - ✍️ **Document Writer**: Draft high-impact articles, essays, emails, and reports with tone control.
                        - 🎓 **Study Mode**: Master topics with analogies, flashcards, and interactive quizzes.
                        - 📁 **Project Workspaces**: Organize conversations, files, and custom system instructions.
                        - 🎙️ **Voice & Audio**: Seamless speech recognition and natural text-to-speech.
                        
                        How can I assist your objectives today?
                    """.trimIndent()
                )
            )

            // Seed a sample project
            val projId = UUID.randomUUID().toString()
            database.projectDao().insertProject(
                ProjectEntity(
                    id = projId,
                    name = "Quantum Computing & AI",
                    description = "Research workspace exploring quantum algorithms, qubits, and neuromorphic architecture.",
                    systemInstruction = "Focus on quantum physics, linear algebra, and computational complexity.",
                    colorHex = "#00E5FF"
                )
            )

            // Seed a sample file in that project
            database.projectFileDao().insertFile(
                ProjectFileEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = projId,
                    name = "quantum_supremacy_notes.md",
                    mimeType = "text/markdown",
                    sizeBytes = 1420,
                    contentSnippet = "# Quantum Computing Overview\n\nQubits leverage superposition and entanglement to solve discrete optimization problems exponentially faster than classical Turing machines.",
                    analysis = "High-level summary of quantum computing advantages, focusing on superposition and entanglement for combinatorial speedups."
                )
            )

            // Seed an initial user memory
            database.memoryDao().insertMemory(
                UserMemoryEntity(
                    id = UUID.randomUUID().toString(),
                    key = "Preferred Tone",
                    content = "Concise, precise, with clear code snippets and analytical clarity",
                    category = "Tone & Style"
                )
            )
        }
    }
}
