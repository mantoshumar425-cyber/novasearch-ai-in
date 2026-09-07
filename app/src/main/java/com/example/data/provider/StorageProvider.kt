package com.example.data.provider

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class UploadedFileMetadata(
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val textContent: String
)

interface StorageProvider {
    suspend fun readDocumentUri(context: Context, uri: Uri): Result<UploadedFileMetadata>
    suspend fun createSampleFile(fileName: String, content: String, mimeType: String): UploadedFileMetadata
}

class NexoraStorageProvider : StorageProvider {
    override suspend fun readDocumentUri(context: Context, uri: Uri): Result<UploadedFileMetadata> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "text/plain"

            var displayName = "document_${System.currentTimeMillis()}"
            var size = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }

            val stringBuilder = java.lang.StringBuilder()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    var lineCount = 0
                    while (line != null && lineCount < 1000) {
                        stringBuilder.append(line).append("\n")
                        line = reader.readLine()
                        lineCount++
                    }
                }
            }

            val text = stringBuilder.toString()
            Result.success(
                UploadedFileMetadata(
                    name = displayName,
                    mimeType = mimeType,
                    sizeBytes = if (size > 0) size else text.toByteArray().size.toLong(),
                    textContent = text
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createSampleFile(fileName: String, content: String, mimeType: String): UploadedFileMetadata {
        return UploadedFileMetadata(
            name = fileName,
            mimeType = mimeType,
            sizeBytes = content.toByteArray().size.toLong(),
            textContent = content
        )
    }
}
