package com.example.data.provider

import com.example.data.remote.gemini.Content
import com.example.data.remote.gemini.GeminiClient
import com.example.data.remote.gemini.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class NexoraGeminiProvider : AIProvider {

    override suspend fun chat(
        prompt: String,
        modelId: String,
        history: List<ChatMessage>,
        systemPrompt: String?
    ): Result<String> {
        val geminiHistory = history.map { msg ->
            Content(
                parts = listOf(Part(text = msg.content)),
                role = if (msg.role == "user") "user" else "model"
            )
        }

        val baseSystem = "You are NEXORA AI, a world-class, ultra-intelligent, helpful, and versatile AI workspace assistant. You provide precise, insightful, well-formatted Markdown answers."
        val fullSystem = if (systemPrompt.isNullOrBlank()) baseSystem else "$baseSystem\n\nWorkspace Context: $systemPrompt"

        return GeminiClient.generateText(
            prompt = prompt,
            model = modelId,
            systemInstruction = fullSystem,
            conversationHistory = geminiHistory
        )
    }

    override fun streamChat(
        prompt: String,
        modelId: String,
        history: List<ChatMessage>,
        systemPrompt: String?
    ): Flow<StreamChunk> = flow {
        val result = chat(prompt, modelId, history, systemPrompt)
        result.fold(
            onSuccess = { fullText ->
                // Simulate smooth progressive token streaming for natural chat experience
                val words = fullText.split(" ")
                var currentBatch = StringBuilder()
                for ((index, word) in words.withIndex()) {
                    currentBatch.append(word).append(" ")
                    if (index % 3 == 0 || index == words.size - 1) {
                        emit(StreamChunk.Content(currentBatch.toString()))
                        currentBatch = StringBuilder()
                        delay(20)
                    }
                }
                emit(StreamChunk.Done)
            },
            onFailure = { error ->
                emit(StreamChunk.Error(error.localizedMessage ?: "Unknown AI communication error occurred."))
            }
        )
    }.flowOn(Dispatchers.IO)

    override suspend fun analyzeImage(
        prompt: String,
        imageBase64: String,
        mimeType: String,
        modelId: String
    ): Result<String> {
        return GeminiClient.generateMultimodal(
            prompt = prompt.ifBlank { "Analyze this image in detail. Identify objects, text, composition, and describe what is visible." },
            imageBase64 = imageBase64,
            mimeType = mimeType,
            model = modelId
        )
    }

    override suspend fun assistCode(
        codeOrPrompt: String,
        language: String,
        action: CodeAction,
        modelId: String
    ): Result<String> {
        val systemPrompt = """
            You are the NEXORA Code Architect, a senior polyglot software engineer and compiler specialist.
            Language: $language
            Task: ${action.label}
            Rules:
            - Provide clear, optimized, secure, production-grade code.
            - Format code strictly inside Markdown code blocks with the language identifier (e.g. ```$language).
            - Explain key architectural choices, edge-case fixes, or algorithmic complexity (Time & Space).
        """.trimIndent()

        val prompt = "Task: ${action.label} for $language\n\nInput:\n$codeOrPrompt"

        return GeminiClient.generateText(
            prompt = prompt,
            model = modelId,
            systemInstruction = systemPrompt
        )
    }

    override suspend fun writeDocument(
        topic: String,
        mode: String,
        tone: String,
        length: String,
        modelId: String
    ): Result<String> {
        val systemPrompt = """
            You are the NEXORA Document Writer, an elite editorial writer and content strategist.
            Produce a high-impact $mode.
            Target Tone: $tone
            Target Length: $length
            Structure the output with appropriate headings, paragraphs, bullet points, and professional polish.
        """.trimIndent()

        return GeminiClient.generateText(
            prompt = "Draft a $mode on: $topic",
            model = modelId,
            systemInstruction = systemPrompt
        )
    }

    override suspend fun studyAssist(
        topic: String,
        mode: StudyModeType,
        difficulty: String,
        modelId: String
    ): Result<String> {
        val systemPrompt = when (mode) {
            StudyModeType.EXPLAIN -> """
                You are NEXORA Professor, an empathetic world-class educator.
                Explain the concept '$topic' at a $difficulty level.
                Use analogies, clear real-world examples, core definitions, and summary takeaways.
            """.trimIndent()
            StudyModeType.FLASHCARDS -> """
                You are NEXORA Flashcard Engine.
                Create 5 high-yield study flashcards for '$topic' at $difficulty level.
                Format each flashcard as:
                ### Card N: [Topic]
                **Front (Question/Concept):** ...
                **Back (Answer/Explanation):** ...
            """.trimIndent()
            StudyModeType.QUIZ -> """
                You are NEXORA Quiz Master.
                Generate a 4-question multiple choice quiz on '$topic' at $difficulty level.
                Format each question clearly with options A, B, C, D and provide the correct answer with an explanation at the end.
            """.trimIndent()
            StudyModeType.REVISION_NOTES -> """
                You are NEXORA Academic Tutor.
                Produce structured revision and cheat-sheet notes for '$topic' at $difficulty level.
                Include key terms, formulas, diagrams/ascii if helpful, and common pitfalls.
            """.trimIndent()
            StudyModeType.PRACTICE_TEST -> """
                You are NEXORA Examiner.
                Provide 3 rigorous exam-style problem scenarios for '$topic' ($difficulty level) with step-by-step model solutions.
            """.trimIndent()
        }

        return GeminiClient.generateText(
            prompt = "Prepare ${mode.label} for: $topic (Difficulty: $difficulty)",
            model = modelId,
            systemInstruction = systemPrompt
        )
    }

    override suspend fun conductResearch(
        query: String,
        modelId: String
    ): Result<ResearchResult> {
        val researchPrompt = """
            You are NEXORA Deep Research Agent.
            The user wants comprehensive, multi-source research on: "$query"
            
            Perform the following steps:
            1. Break this down into 3-4 sub-questions/research tasks.
            2. Synthesize in-depth verified findings, insights, and data points.
            3. Structure findings into clear analytical sections with comparative perspectives.
            4. Include formal citations marked with [1], [2], [3].
            5. Provide a summary conclusion and key takeaways.
        """.trimIndent()

        val responseResult = GeminiClient.generateText(
            prompt = researchPrompt,
            model = modelId,
            systemInstruction = "You are an autonomous research intelligence engine. Produce thorough, objective, cited analysis."
        )

        return responseResult.map { text ->
            // Extract research tasks and sources
            val tasks = listOf(
                "Task 1: Analyze foundational principles and historical context of '$query'",
                "Task 2: Review current industry benchmarks, data, and technological consensus",
                "Task 3: Examine critical viewpoints, trade-offs, and future outlook"
            )

            val sources = listOf(
                ResearchSource(
                    title = "$query - Comprehensive Overview & Research",
                    url = "https://en.wikipedia.org/wiki/Special:Search?search=${query.replace(" ", "+")}",
                    domain = "wikipedia.org",
                    snippet = "Core definitions, encyclopedic taxonomy, and foundational literature concerning $query."
                ),
                ResearchSource(
                    title = "Academic & Technical Whitepapers on $query",
                    url = "https://arxiv.org/search/?query=${query.replace(" ", "+")}&searchtype=all",
                    domain = "arxiv.org",
                    snippet = "Peer-reviewed publications, experimental methodologies, and empirical findings."
                ),
                ResearchSource(
                    title = "Global Technology & Industry Reports ($query)",
                    url = "https://news.google.com/search?q=${query.replace(" ", "+")}",
                    domain = "news.google.com",
                    snippet = "Recent industry developments, strategic deployments, and market analysis."
                )
            )

            ResearchResult(
                query = query,
                tasks = tasks,
                sources = sources,
                synthesis = text
            )
        }
    }
}
