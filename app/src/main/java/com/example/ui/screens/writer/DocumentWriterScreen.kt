package com.example.ui.screens.writer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.chat.RenderFormattedContent
import com.example.ui.viewmodel.NexoraViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentWriterScreen(
    viewModel: NexoraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val topic by viewModel.writerTopic.collectAsState()
    val mode by viewModel.writerMode.collectAsState()
    val tone by viewModel.writerTone.collectAsState()
    val length by viewModel.writerLength.collectAsState()
    val result by viewModel.writerResult.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    val modes = listOf("Article", "Essay", "Email", "Report", "Executive Summary", "Social Post", "Speech")
    val tones = listOf("Professional", "Academic", "Casual", "Persuasive", "Creative")
    val lengths = listOf("Concise", "Balanced", "Comprehensive")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("document_writer_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFB300).copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "NEXORA Document Writer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "High-impact editorial drafting & content generation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Topic Input
        item {
            OutlinedTextField(
                value = topic,
                onValueChange = { viewModel.setWriterTopic(it) },
                label = { Text("Document Subject or Brief") },
                placeholder = { Text("What should NEXORA draft for you? Describe key requirements or outline...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("writer_topic_input"),
                shape = RoundedCornerShape(16.dp),
                minLines = 3,
                maxLines = 6
            )
        }

        // Format Mode Selector
        item {
            Text("Document Format", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modes.forEach { item ->
                    FilterChip(
                        selected = mode == item,
                        onClick = { viewModel.setWriterMode(item) },
                        label = { Text(item, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Tone & Length Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Editorial Tone", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        tones.forEach { t ->
                            FilterChip(
                                selected = tone == t,
                                onClick = { viewModel.setWriterTone(t) },
                                label = { Text(t, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Target Depth", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        lengths.forEach { l ->
                            FilterChip(
                                selected = length == l,
                                onClick = { viewModel.setWriterLength(l) },
                                label = { Text(l, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Draft Button
        item {
            Button(
                onClick = { viewModel.executeDocumentWrite() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("draft_document_button"),
                enabled = topic.isNotBlank() && !isGenerating,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB300)
                )
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Composing $mode...", color = Color.Black)
                } else {
                    Icon(Icons.Default.Create, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Draft $mode", color = Color.Black)
                }
            }
        }

        // Output Result
        if (result.isNotBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Drafted $mode ($tone)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Draft", result))
                                    viewModel.showSnackbar("Document copied to clipboard.")
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                                IconButton(onClick = { viewModel.speakText(result) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Read Aloud")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        RenderFormattedContent(
                            content = result,
                            textColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
