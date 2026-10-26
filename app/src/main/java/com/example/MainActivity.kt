package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.call.CallState
import com.example.service.BlueChatForegroundService
import com.example.ui.MainViewModel
import com.example.ui.components.EmergencyApkBottomSheet
import com.example.ui.components.MeshHealthBadge
import com.example.ui.components.P2PCallOverlay
import com.example.ui.components.SenderProfileBottomSheet
import com.example.ui.components.SituationalHelpBottomSheet
import com.example.ui.screens.CallsScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.OnboardingCarousel
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BlueChatPrimary
import com.example.ui.theme.BlueChatTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start Foreground Service to keep mesh network active and prevent Doze mode
        try {
            BlueChatForegroundService.startService(this)
        } catch (ignored: Exception) {}

        // Check if intent launched with specific tab (from Quick Settings tile)
        val targetTab = intent.getIntExtra("OPEN_TAB", -1)
        if (targetTab >= 0) {
            viewModel.setTab(targetTab)
        }

        setContent {
            BlueChatTheme {
                BlueChatApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueChatApp(viewModel: MainViewModel) {
    val context = LocalContext.current

    // Request permissions on launch
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionsLauncher.launch(needed.toTypedArray())
        }
    }

    val isOnboardingDone by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    if (!isOnboardingDone) {
        OnboardingCarousel(
            onFinish = { nickname, bio ->
                viewModel.completeOnboarding(nickname, bio)
            }
        )
        return
    }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
    val activeChatPeerName by viewModel.activeChatPeerName.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val inlineBanner by viewModel.inlineBannerMessage.collectAsStateWithLifecycle()

    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val discoveredUsers by viewModel.discoveredUsers.collectAsStateWithLifecycle()
    val savedContacts by viewModel.savedContacts.collectAsStateWithLifecycle()
    val currentMessages by viewModel.currentChatMessages.collectAsStateWithLifecycle()
    val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()
    val activePeersCount by viewModel.activeNearbyPeersCount.collectAsStateWithLifecycle()

    val myPeerId = viewModel.myPeerId
    val myNickname by viewModel.myNickname.collectAsStateWithLifecycle()
    val myStatusBio by viewModel.myStatusBio.collectAsStateWithLifecycle()
    val isBatterySaver by viewModel.isBatterySaverMode.collectAsStateWithLifecycle()

    // Bottom Sheets states
    val showHelp by viewModel.showHelpBottomSheet.collectAsStateWithLifecycle()
    val showApkServer by viewModel.showEmergencyApkBottomSheet.collectAsStateWithLifecycle()
    val selectedSender by viewModel.selectedSenderForCard.collectAsStateWithLifecycle()

    val helpSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val apkSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val senderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Call Engine states
    val callState by viewModel.callEngine.callState.collectAsStateWithLifecycle()
    val callDuration by viewModel.callEngine.durationSeconds.collectAsStateWithLifecycle()
    val callPeerName by viewModel.callEngine.activePeerName.collectAsStateWithLifecycle()
    val callIsVideo by viewModel.callEngine.isVideoCall.collectAsStateWithLifecycle()
    val callIsMuted by viewModel.callEngine.isMicMuted.collectAsStateWithLifecycle()
    val callIsSpeaker by viewModel.callEngine.isSpeakerOn.collectAsStateWithLifecycle()
    val callIsFrontCam by viewModel.callEngine.isFrontCamera.collectAsStateWithLifecycle()

    // Triple-tap top bar title detection for Emergency APK Distribution
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedLayout = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                if (activeChatId == null || isExpandedLayout) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    val now = System.currentTimeMillis()
                                    if (now - lastTapTime < 500) {
                                        tapCount++
                                        if (tapCount >= 3) {
                                            tapCount = 0
                                            viewModel.setShowEmergencyApk(true)
                                        }
                                    } else {
                                        tapCount = 1
                                    }
                                    lastTapTime = now
                                }
                            ) {
                                Text(
                                    text = "BlueChat",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BlueChatPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                MeshHealthBadge(activePeersCount = activePeersCount)
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.setShowHelp(true) },
                                modifier = Modifier.testTag("help_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Guías de Ayuda",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
            bottomBar = {
                if (activeChatId == null || isExpandedLayout) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { viewModel.setTab(0) },
                            icon = { Icon(imageVector = Icons.Default.Chat, contentDescription = "Chats") },
                            label = { Text("Chats") },
                            modifier = Modifier.testTag("nav_chats")
                        )
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { viewModel.setTab(1) },
                            icon = { Icon(imageVector = Icons.Default.People, contentDescription = "Contactos") },
                            label = { Text("Contactos") },
                            modifier = Modifier.testTag("nav_contacts")
                        )
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { viewModel.setTab(2) },
                            icon = { Icon(imageVector = Icons.Default.Call, contentDescription = "Llamadas") },
                            label = { Text("Llamadas") },
                            modifier = Modifier.testTag("nav_calls")
                        )
                        NavigationBarItem(
                            selected = currentTab == 3,
                            onClick = { viewModel.setTab(3) },
                            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Ajustes") },
                            label = { Text("Ajustes") },
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Adaptive layout: Split Pane on wide screens, Single Pane on Compact
                if (isExpandedLayout) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left List Pane
                        Box(modifier = Modifier.weight(0.42f)) {
                            when (currentTab) {
                                0 -> ChatsScreen(
                                    chats = chats,
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    onChatClick = { id, name -> viewModel.openChat(id, name) },
                                    onDeleteChat = { viewModel.deleteChat(it) }
                                )
                                1 -> ContactsScreen(
                                    discoveredUsers = discoveredUsers,
                                    savedContacts = savedContacts,
                                    myPeerId = myPeerId,
                                    onSaveContact = { id, name -> viewModel.saveContact(id, name) },
                                    onStartChat = { id, name -> viewModel.openChat(id, name) }
                                )
                                2 -> CallsScreen(
                                    callLogs = callLogs,
                                    onLaunchCall = { id, name, isVideo -> viewModel.startCall(id, name, isVideo) }
                                )
                                3 -> SettingsScreen(
                                    myPeerId = myPeerId,
                                    myNickname = myNickname,
                                    myStatusBio = myStatusBio,
                                    isBatterySaver = isBatterySaver,
                                    activeNodesCount = activePeersCount,
                                    onSaveProfile = { n, b -> viewModel.updateProfile(n, b) },
                                    onToggleBatterySaver = { viewModel.setBatterySaverMode(it) },
                                    onOpenApkServerModal = { viewModel.setShowEmergencyApk(true) },
                                    onOpenHelpModal = { viewModel.setShowHelp(true) }
                                )
                            }
                        }

                        // Right Detail Pane
                        Box(
                            modifier = Modifier
                                .weight(0.58f)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            if (activeChatId != null) {
                                ChatDetailScreen(
                                    chatId = activeChatId!!,
                                    peerName = activeChatPeerName,
                                    messages = currentMessages,
                                    myPeerId = myPeerId,
                                    onBack = { viewModel.closeChat() },
                                    onSendMessage = { viewModel.sendMessage(it) },
                                    onSendFile = { name, size, path -> viewModel.sendFileMessage(name, size, path) },
                                    onSendAudio = { duration -> viewModel.sendAudioMessage(duration) },
                                    onStartCall = { isVideo -> viewModel.startCall(activeChatId!!, activeChatPeerName, isVideo) },
                                    onSenderClick = { user -> viewModel.selectSenderForCard(user) }
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Selecciona una conversación para chatear",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Compact Single Pane Layout
                    if (activeChatId != null) {
                        ChatDetailScreen(
                            chatId = activeChatId!!,
                            peerName = activeChatPeerName,
                            messages = currentMessages,
                            myPeerId = myPeerId,
                            onBack = { viewModel.closeChat() },
                            onSendMessage = { viewModel.sendMessage(it) },
                            onSendFile = { name, size, path -> viewModel.sendFileMessage(name, size, path) },
                            onSendAudio = { duration -> viewModel.sendAudioMessage(duration) },
                            onStartCall = { isVideo -> viewModel.startCall(activeChatId!!, activeChatPeerName, isVideo) },
                            onSenderClick = { user -> viewModel.selectSenderForCard(user) }
                        )
                    } else {
                        when (currentTab) {
                            0 -> ChatsScreen(
                                chats = chats,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onChatClick = { id, name -> viewModel.openChat(id, name) },
                                onDeleteChat = { viewModel.deleteChat(it) }
                            )
                            1 -> ContactsScreen(
                                discoveredUsers = discoveredUsers,
                                savedContacts = savedContacts,
                                myPeerId = myPeerId,
                                onSaveContact = { id, name -> viewModel.saveContact(id, name) },
                                onStartChat = { id, name -> viewModel.openChat(id, name) }
                            )
                            2 -> CallsScreen(
                                callLogs = callLogs,
                                onLaunchCall = { id, name, isVideo -> viewModel.startCall(id, name, isVideo) }
                            )
                            3 -> SettingsScreen(
                                myPeerId = myPeerId,
                                myNickname = myNickname,
                                myStatusBio = myStatusBio,
                                isBatterySaver = isBatterySaver,
                                activeNodesCount = activePeersCount,
                                onSaveProfile = { n, b -> viewModel.updateProfile(n, b) },
                                onToggleBatterySaver = { viewModel.setBatterySaverMode(it) },
                                onOpenApkServerModal = { viewModel.setShowEmergencyApk(true) },
                                onOpenHelpModal = { viewModel.setShowHelp(true) }
                            )
                        }
                    }
                }

                // Non-intrusive Inline Banner Notice
                AnimatedVisibility(
                    visible = inlineBanner != null,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = BlueChatPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = inlineBanner ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearInlineBanner() }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.inverseOnSurface)
                            }
                        }
                    }
                }

                // Active Call Fullscreen Overlay
                if (callState != CallState.IDLE) {
                    P2PCallOverlay(
                        callEngine = viewModel.callEngine,
                        callState = callState,
                        peerName = callPeerName,
                        isVideo = callIsVideo,
                        durationSeconds = callDuration,
                        isMuted = callIsMuted,
                        isSpeakerOn = callIsSpeaker,
                        isFrontCamera = callIsFrontCam
                    )
                }
            }
        }
    }

    // Bottom Sheets
    if (showHelp) {
        SituationalHelpBottomSheet(
            sheetState = helpSheetState,
            onDismiss = { viewModel.setShowHelp(false) }
        )
    }

    if (showApkServer) {
        EmergencyApkBottomSheet(
            serverUrl = viewModel.apkServer.getServerUrl(),
            sheetState = apkSheetState,
            onDismiss = { viewModel.setShowEmergencyApk(false) }
        )
    }

    if (selectedSender != null) {
        SenderProfileBottomSheet(
            user = selectedSender,
            sheetState = senderSheetState,
            onDismiss = { viewModel.selectSenderForCard(null) },
            onStartPrivateChat = { peerId, nickname ->
                viewModel.openChat(peerId, nickname)
            },
            onSaveContact = { peerId, nickname ->
                viewModel.saveContact(peerId, nickname)
            }
        )
    }
}
