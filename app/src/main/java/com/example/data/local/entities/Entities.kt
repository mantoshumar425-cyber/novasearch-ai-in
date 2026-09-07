package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["updatedAt"]), Index(value = ["projectId"])]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelId: String = "gemini-3.5-flash",
    val projectId: String? = null,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false
)

@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversationId"]), Index(value = ["timestamp"])]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // "user", "model", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String? = null,
    val attachmentsJson: String? = null,
    val citationsJson: String? = null,
    val isError: Boolean = false
)

@Entity(
    tableName = "projects",
    indices = [Index(value = ["updatedAt"])]
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val systemInstruction: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val colorHex: String = "#8E7DF9"
)

@Entity(
    tableName = "project_files",
    indices = [Index(value = ["projectId"])]
)
data class ProjectFileEntity(
    @PrimaryKey val id: String,
    val projectId: String? = null,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val contentSnippet: String,
    val analysis: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "generated_images",
    indices = [Index(value = ["createdAt"])]
)
data class GeneratedImageEntity(
    @PrimaryKey val id: String,
    val prompt: String,
    val aspectRatio: String = "1:1",
    val imageData: String, // base64 or asset uri
    val modelName: String = "gemini-2.5-flash-image",
    val stylePreset: String = "Digital Art",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_memories",
    indices = [Index(value = ["category"])]
)
data class UserMemoryEntity(
    @PrimaryKey val id: String,
    val key: String,
    val content: String,
    val category: String = "Preference",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val defaultModel: String = "gemini-3.5-flash",
    val themeMode: String = "DARK", // "DARK", "LIGHT", "SYSTEM"
    val ttsVoiceSpeed: Float = 1.0f,
    val temperature: Float = 0.7f,
    val webSearchEnabled: Boolean = true,
    val longTermMemoryEnabled: Boolean = true
)
