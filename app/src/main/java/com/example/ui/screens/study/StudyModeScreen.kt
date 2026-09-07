package com.example.ui.screens.study

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
import androidx.compose.material.icons.filled.School
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
import com.example.data.provider.StudyModeType
import com.example.ui.screens.chat.RenderFormattedContent
import com.example.ui.viewmodel.NexoraViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyModeScreen(
    viewModel: NexoraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val topic by viewModel.studyTopic.collectAsState()
    val mode by viewModel.studyMode.collectAsState()
    val difficulty by viewModel.studyDifficulty.collectAsState()
    val result by viewModel.studyResult.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    val difficulties = listOf("Beginner", "Medium", "Advanced", "PhD / Expert")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("study_mode_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFAB47BC).copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = Color(0xFFAB47BC),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "NEXORA Study & Academics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Interactive concept mastery, quizzes & flashcards",
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
                onValueChange = { viewModel.setStudyTopic(it) },
                label = { Text("Subject or Academic Concept") },
                placeholder = { Text("e.g. Mitochondria & ATP Synthesis, General Relativity, Bayesian Statistics...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("study_topic_input"),
                shape = RoundedCornerShape(16.dp),
                maxLines = 3
            )
        }

        // Mode Chips
        item {
            Text("Study Engine Mode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudyModeType.values().forEach { sm ->
                    FilterChip(
                        selected = mode == sm,
                        onClick = { viewModel.setStudyMode(sm) },
                        label = { Text(sm.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFAB47BC),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Difficulty Selector
        item {
            Text("Target Mastery Level", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                difficulties.forEach { diff ->
                    FilterChip(
                        selected = difficulty == diff,
                        onClick = { viewModel.setStudyDifficulty(diff) },
                        label = { Text(diff, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Generate Action Button
        item {
            Button(
                onClick = { viewModel.executeStudyAssist() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("generate_study_button"),
                enabled = topic.isNotBlank() && !isGenerating,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFAB47BC)
                )
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating Study Material...", color = Color.White)
                } else {
                    Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Launch ${mode.label}", color = Color.White)
                }
            }
        }

        // Result Output Card
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
                                text = "${mode.label} ($difficulty)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Study Material", result))
                                    viewModel.showSnackbar("Study notes copied.")
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
