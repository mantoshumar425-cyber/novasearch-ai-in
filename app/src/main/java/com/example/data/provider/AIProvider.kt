package com.example.data.provider

import kotlinx.coroutines.flow.Flow

data class ChatMessage(
    val role: String, // "user" or "model"
    val content: String,
    val imageBase64: String? = null
)

sealed class StreamChunk {
    data class Content(val text: String) : StreamChunk()
    data class Error(val message: String) : StreamChunk()
    object Done : StreamChunk()
}

enum class CodeAction(val label: String) {
    GENERATE("Generate Code"),
    EXPLAIN("Explain Architecture"),
    DEBUG("Debug & Fix Issues"),
    REFACTOR("Refactor & Clean"),
    OPTIMIZE("Optimize Performance"),
    UNIT_TESTS("Generate Unit Tests"),
    REVIEW("Security & Code Review")
}

enum class StudyModeType(val label: String) {
    EXPLAIN("Concept Explainer"),
    FLASHCARDS("Generate Flashcards"),
    QUIZ("Interactive Quiz"),
    REVISION_NOTES("Revision Summary"),
    PRACTICE_TEST("Practice Exam Questions")
}

data class ResearchResult(
    val query: String,
    val tasks: List<String>,
    val sources: List<ResearchSource>,
    val synthesis: String
)

data class ResearchSource(
    val title: String,
    val url: String,
    val domain: String,
    val snippet: String
)

interface AIProvider {
    suspend fun chat(
        prompt: String,
        modelId: String = "gemini-3.5-flash",
        history: List<ChatMessage> = emptyList(),
        systemPrompt: String? = null
    ): Result<String>

    fun streamChat(
        prompt: String,
        modelId: String = "gemini-3.5-flash",
        history: List<ChatMessage> = emptyList(),
        systemPrompt: String? = null
    ): Flow<StreamChunk>

    suspend fun analyzeImage(
        prompt: String,
        imageBase64: String,
        mimeType: String = "image/jpeg",
        modelId: String = "gemini-3.5-flash"
    ): Result<String>

    suspend fun assistCode(
        codeOrPrompt: String,
        language: String,
        action: CodeAction,
        modelId: String = "gemini-3.1-pro-preview"
    ): Result<String>

    suspend fun writeDocument(
        topic: String,
        mode: String,
        tone: String,
        length: String,
        modelId: String = "gemini-3.5-flash"
    ): Result<String>

    suspend fun studyAssist(
        topic: String,
        mode: StudyModeType,
        difficulty: String,
        modelId: String = "gemini-3.5-flash"
    ): Result<String>

    suspend fun conductResearch(
        query: String,
        modelId: String = "gemini-3.5-flash"
    ): Result<ResearchResult>
}
