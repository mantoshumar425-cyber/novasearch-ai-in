package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.coding.CodingAssistantScreen
import com.example.ui.screens.files.FilesScreen
import com.example.ui.screens.imagestudio.ImageStudioScreen
import com.example.ui.screens.landing.LandingScreen
import com.example.ui.screens.memory.MemoryScreen
import com.example.ui.screens.projects.ProjectsScreen
import com.example.ui.screens.research.ResearchScreen
import com.example.ui.screens.search.GlobalSearchDialog
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.study.StudyModeScreen
import com.example.ui.screens.writer.DocumentWriterScreen
import com.example.ui.theme.NexoraTheme
import com.example.ui.viewmodel.NexoraScreen
import com.example.ui.viewmodel.NexoraViewModel
import kotlinx.coroutines.launch

data class DrawerNavItem(
    val screen: NexoraScreen,
    val icon: ImageVector,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexoraApp(
    viewModel: NexoraViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (settings.themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemDark
    }

    NexoraTheme(darkTheme = isDarkTheme) {
        val currentScreen by viewModel.currentScreen.collectAsState()
        val snackbarMessage by viewModel.snackbarMessage.collectAsState()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        var showSearchDialog by remember { mutableStateOf(false) }

        LaunchedEffect(snackbarMessage) {
            snackbarMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.dismissSnackbar()
            }
        }

        val navDrawerItems = listOf(
            DrawerNavItem(NexoraScreen.HOME, Icons.Default.Home, "Workspace Hub"),
            DrawerNavItem(NexoraScreen.CHAT, Icons.AutoMirrored.Filled.Chat, "AI Chat"),
            DrawerNavItem(NexoraScreen.IMAGE_STUDIO, Icons.Default.Image, "Image Studio"),
            DrawerNavItem(NexoraScreen.RESEARCH, Icons.Default.Search, "Deep Research"),
            DrawerNavItem(NexoraScreen.CODE_ASSISTANT, Icons.Default.Code, "Code Architect"),
            DrawerNavItem(NexoraScreen.WRITER, Icons.Default.Description, "Document Writer"),
            DrawerNavItem(NexoraScreen.STUDY, Icons.Default.School, "Study Mode"),
            DrawerNavItem(NexoraScreen.PROJECTS, Icons.Default.Folder, "Projects"),
            DrawerNavItem(NexoraScreen.FILES, Icons.Default.Psychology, "Document AI"),
            DrawerNavItem(NexoraScreen.MEMORY, Icons.Default.Security, "Persistent Memory"),
            DrawerNavItem(NexoraScreen.SETTINGS, Icons.Default.Settings, "Settings & API")
        )

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Brand Logo in Drawer
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "NEXORA AI",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "All-In-One AI Platform",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Navigation Items
                        navDrawerItems.forEach { item ->
                            NavigationDrawerItem(
                                icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(20.dp)) },
                                label = { Text(item.label, fontSize = 13.sp) },
                                selected = currentScreen == item.screen,
                                onClick = {
                                    viewModel.navigateTo(item.screen)
                                    scope.launch { drawerState.close() }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "v1.0.0 • Production Build",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        ) {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                    ) {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 2.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "N",
                                                color = androidx.compose.ui.graphics.Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = androidx.compose.ui.text.buildAnnotatedString {
                                            append("NEXORA ")
                                            pushStyle(androidx.compose.ui.text.SpanStyle(color = com.example.ui.theme.VibrantIndigoLight))
                                            append("AI")
                                            pop()
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                                }
                            },
                            actions = {
                                // Active Model Version Badge (as in Vibrant Palette Design HTML)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = "ULTRA V2",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                // Live pulse indicator dot button
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable { viewModel.navigateTo(NexoraScreen.SETTINGS) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(com.example.ui.theme.NexoraSuccess, CircleShape)
                                        )
                                    }
                                }

                                IconButton(onClick = { showSearchDialog = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search Workspace")
                                }
                                if (currentScreen != NexoraScreen.CHAT) {
                                    IconButton(onClick = { viewModel.createNewConversation() }) {
                                        Icon(Icons.Default.Add, contentDescription = "New Chat", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                },
                bottomBar = {
                    // Vibrant Palette Bottom Navigation Bar
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                    ) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.background,
                            tonalElevation = 0.dp
                        ) {
                            listOf(
                                Triple(NexoraScreen.HOME, Icons.Default.Home, "Home"),
                                Triple(NexoraScreen.CHAT, Icons.AutoMirrored.Filled.Chat, "Chat"),
                                Triple(NexoraScreen.IMAGE_STUDIO, Icons.Default.Image, "Studio"),
                                Triple(NexoraScreen.RESEARCH, Icons.Default.Search, "Research"),
                                Triple(NexoraScreen.CODE_ASSISTANT, Icons.Default.Code, "Code")
                            ).forEach { (screen, icon, label) ->
                                val selected = currentScreen == screen
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.navigateTo(screen) },
                                    icon = {
                                        Icon(
                                            icon,
                                            contentDescription = label,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (selected) com.example.ui.theme.VibrantIndigoLight else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    },
                                    label = {
                                        Text(
                                            label,
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) com.example.ui.theme.VibrantIndigoLight else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = com.example.ui.theme.VibrantIndigoLight,
                                        selectedTextColor = com.example.ui.theme.VibrantIndigoLight,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        NexoraScreen.HOME -> LandingScreen(viewModel)
                        NexoraScreen.CHAT -> ChatScreen(viewModel)
                        NexoraScreen.IMAGE_STUDIO -> ImageStudioScreen(viewModel)
                        NexoraScreen.RESEARCH -> ResearchScreen(viewModel)
                        NexoraScreen.CODE_ASSISTANT -> CodingAssistantScreen(viewModel)
                        NexoraScreen.WRITER -> DocumentWriterScreen(viewModel)
                        NexoraScreen.STUDY -> StudyModeScreen(viewModel)
                        NexoraScreen.PROJECTS -> ProjectsScreen(viewModel)
                        NexoraScreen.FILES -> FilesScreen(viewModel)
                        NexoraScreen.MEMORY -> MemoryScreen(viewModel)
                        NexoraScreen.SETTINGS -> SettingsScreen(viewModel)
                    }
                }
            }
        }

        if (showSearchDialog) {
            GlobalSearchDialog(
                viewModel = viewModel,
                onDismiss = { showSearchDialog = false }
            )
        }
    }
}
