package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.call.CallState
import com.example.call.P2PCallEngine
import com.example.data.db.AppDatabase
import com.example.data.model.CallLogEntity
import com.example.data.model.ChatEntity
import com.example.data.model.MessageEntity
import com.example.data.model.UserEntity
import com.example.data.repository.BlueChatRepository
import com.example.mesh.MeshEngine
import com.example.mesh.NetworkChannelsState
import com.example.server.OfflineApkServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("bluechat_prefs", Context.MODE_PRIVATE)

    val myPeerId: String = prefs.getString("my_peer_id", null) ?: run {
        val newId = "BC_" + UUID.randomUUID().toString().take(8).uppercase()
        prefs.edit().putString("my_peer_id", newId).apply()
        newId
    }

    private val _myNickname = MutableStateFlow(prefs.getString("my_nickname", "Usuario BlueChat") ?: "Usuario BlueChat")
    val myNickname: StateFlow<String> = _myNickname.asStateFlow()

    private val _myStatusBio = MutableStateFlow(prefs.getString("my_status_bio", "Conectado a la red Mesh") ?: "Conectado a la red Mesh")
    val myStatusBio: StateFlow<String> = _myStatusBio.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_done", false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _isBatterySaverMode = MutableStateFlow(prefs.getBoolean("battery_saver", false))
    val isBatterySaverMode: StateFlow<Boolean> = _isBatterySaverMode.asStateFlow()

    // Navigation state
    private val _currentTab = MutableStateFlow(0) // 0: Chats, 1: Contacts, 2: Calls, 3: Settings
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _activeChatPeerName = MutableStateFlow("Canal General")
    val activeChatPeerName: StateFlow<String> = _activeChatPeerName.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Modals / Bottom Sheets
    private val _showHelpBottomSheet = MutableStateFlow(false)
    val showHelpBottomSheet: StateFlow<Boolean> = _showHelpBottomSheet.asStateFlow()

    private val _showEmergencyApkBottomSheet = MutableStateFlow(false)
    val showEmergencyApkBottomSheet: StateFlow<Boolean> = _showEmergencyApkBottomSheet.asStateFlow()

    private val _selectedSenderForCard = MutableStateFlow<UserEntity?>(null)
    val selectedSenderForCard: StateFlow<UserEntity?> = _selectedSenderForCard.asStateFlow()

    // Transient inline feedback message
    private val _inlineBannerMessage = MutableStateFlow<String?>(null)
    val inlineBannerMessage: StateFlow<String?> = _inlineBannerMessage.asStateFlow()

    // Database & Repository
    val repository = BlueChatRepository(AppDatabase.getDatabase(application))

    // Mesh Engine
    val meshEngine = MeshEngine(
        context = application,
        scope = viewModelScope,
        repository = repository,
        myPeerId = myPeerId,
        myNickname = _myNickname.value
    )

    // Call Engine
    val callEngine = P2PCallEngine(
        context = application,
        scope = viewModelScope,
        repository = repository
    )

    // Embedded Offline APK Server
    val apkServer = OfflineApkServer(application, viewModelScope)

    // Reactive streams
    val chats: StateFlow<List<ChatEntity>> = repository.allChats.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val discoveredUsers: StateFlow<List<UserEntity>> = repository.allDiscoveredUsers.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val savedContacts: StateFlow<List<UserEntity>> = repository.savedContacts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val generalMessages: StateFlow<List<MessageEntity>> = repository.generalChannelMessages.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val callLogs: StateFlow<List<CallLogEntity>> = repository.allCallLogs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val activeNearbyPeersCount: StateFlow<Int> = meshEngine.activeNearbyPeersCount
    val channelsState: StateFlow<NetworkChannelsState> = meshEngine.channelsState

    fun refreshChannels() {
        meshEngine.refreshChannelsState()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentChatMessages: StateFlow<List<MessageEntity>> = _activeChatId.flatMapLatest { chatId ->
        if (chatId == null) {
            flowOf(emptyList())
        } else if (chatId == "general_channel") {
            repository.generalChannelMessages
        } else {
            repository.getMessagesForChat(chatId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        meshEngine.start()
        apkServer.start()
        ensureGeneralChannelExists()
    }

    private fun ensureGeneralChannelExists() {
        viewModelScope.launch(Dispatchers.IO) {
            val general = repository.getUserByPeerId("general_channel")
            if (general == null) {
                repository.insertOrUpdateChat(
                    ChatEntity(
                        chatId = "general_channel",
                        peerId = "general_channel",
                        displayName = "Canal General",
                        unreadCount = 0,
                        lastMessageTimestamp = System.currentTimeMillis(),
                        lastMessageSnippet = "Canal de frecuencia abierta para la comunidad",
                        lastMessageStatus = MessageEntity.STATUS_DELIVERED
                    )
                )
            }
        }
    }

    fun completeOnboarding(nickname: String, bio: String) {
        val cleanName = nickname.trim().ifEmpty { "Usuario BlueChat" }
        _myNickname.value = cleanName
        _myStatusBio.value = bio.trim().ifEmpty { "Conectado a la red Mesh" }
        _isOnboardingCompleted.value = true

        prefs.edit()
            .putString("my_nickname", cleanName)
            .putString("my_status_bio", _myStatusBio.value)
            .putBoolean("onboarding_done", true)
            .apply()

        meshEngine.updateNickname(cleanName)
    }

    fun updateProfile(nickname: String, bio: String) {
        val cleanName = nickname.trim().ifEmpty { "Usuario BlueChat" }
        _myNickname.value = cleanName
        _myStatusBio.value = bio.trim()

        prefs.edit()
            .putString("my_nickname", cleanName)
            .putString("my_status_bio", bio.trim())
            .apply()

        meshEngine.updateNickname(cleanName)
    }

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun openChat(chatId: String, peerName: String) {
        _activeChatId.value = chatId
        _activeChatPeerName.value = peerName
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetChatUnread(chatId)
            repository.markIncomingRead(chatId, myPeerId)
        }
    }

    fun closeChat() {
        _activeChatId.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun sendMessage(text: String, isGeneral: Boolean = false) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val isContactsOnly = trimmed.startsWith("/contactos", ignoreCase = true)
        val cleanText = if (isContactsOnly) {
            trimmed.removePrefix("/contactos").removePrefix("/Contactos").trim()
        } else {
            trimmed
        }

        val isPriority = trimmed.contains("@todos", ignoreCase = true)
        val targetChatId = _activeChatId.value ?: if (isGeneral) "general_channel" else return

        meshEngine.sendChatMessage(
            recipientId = if (targetChatId == "general_channel") "ALL" else targetChatId,
            recipientName = _activeChatPeerName.value,
            text = cleanText,
            isGeneral = (targetChatId == "general_channel"),
            isContactsOnly = isContactsOnly,
            isPriority = isPriority
        )
    }

    fun sendFileMessage(fileName: String, fileSize: Long, filePath: String) {
        val targetChatId = _activeChatId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val messageId = UUID.randomUUID().toString()
            val msg = MessageEntity(
                messageId = messageId,
                chatId = targetChatId,
                senderId = myPeerId,
                senderName = _myNickname.value,
                recipientId = targetChatId,
                textPayload = "Archivo adjunto: $fileName",
                timestamp = System.currentTimeMillis(),
                status = MessageEntity.STATUS_CLOCK,
                fileName = fileName,
                fileSize = fileSize,
                filePath = filePath,
                waitingWifi = true // Waiting direct Wi-Fi range
            )
            repository.insertMessage(msg)
            _inlineBannerMessage.value = "Esperando conexión directa Wi-Fi para enviar archivo"
        }
    }

    fun sendAudioMessage(durationSeconds: Long) {
        val targetChatId = _activeChatId.value ?: return
        val duration = durationSeconds.coerceAtLeast(1L)
        val minutes = duration / 60
        val seconds = duration % 60
        val timeLabel = String.format("%02d:%02d", minutes, seconds)
        val isGeneral = (targetChatId == "general_channel")

        val messageId = UUID.randomUUID().toString()
        val text = "Nota de voz ($timeLabel)"

        viewModelScope.launch(Dispatchers.IO) {
            val msg = MessageEntity(
                messageId = messageId,
                chatId = targetChatId,
                senderId = myPeerId,
                senderName = _myNickname.value,
                recipientId = if (isGeneral) "ALL" else targetChatId,
                textPayload = text,
                timestamp = System.currentTimeMillis(),
                status = MessageEntity.STATUS_SENT,
                fileName = "audio_${System.currentTimeMillis()}.m4a",
                fileSize = duration * 16000L,
                filePath = null,
                waitingWifi = false
            )
            repository.insertMessage(msg)

            val chat = ChatEntity(
                chatId = targetChatId,
                peerId = targetChatId,
                displayName = if (isGeneral) "Canal General" else _activeChatPeerName.value,
                unreadCount = 0,
                lastMessageTimestamp = System.currentTimeMillis(),
                lastMessageSnippet = "Nota de voz ($timeLabel)",
                lastMessageStatus = MessageEntity.STATUS_SENT
            )
            repository.insertOrUpdateChat(chat)

            meshEngine.sendChatMessage(
                recipientId = if (isGeneral) "ALL" else targetChatId,
                recipientName = _activeChatPeerName.value,
                text = text,
                isGeneral = isGeneral,
                isContactsOnly = false,
                isPriority = false
            )
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteChat(chatId)
            if (_activeChatId.value == chatId) {
                _activeChatId.value = null
            }
        }
    }

    fun saveContact(peerId: String, nickname: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setContactSavedStatus(peerId, true)
            repository.updateNickname(peerId, nickname)
        }
    }

    private val _callHandshakeInfo = MutableStateFlow<CallHandshakeInfo?>(null)
    val callHandshakeInfo: StateFlow<CallHandshakeInfo?> = _callHandshakeInfo.asStateFlow()

    fun dismissCallHandshake() {
        _callHandshakeInfo.value = null
    }

    fun startCall(peerId: String, peerName: String, isVideo: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.getUserByPeerId(peerId)
            val ip = user?.ipAddress
            if (ip.isNullOrBlank()) {
                // Peer is reached via Bluetooth BLE / mesh hops, but high-speed direct IP (Hotspot or Wi-Fi) is needed for real-time UDP call
                _callHandshakeInfo.value = CallHandshakeInfo(peerId, peerName, isVideo)
            } else {
                val started = callEngine.startCall(peerId, peerName, ip, isVideo)
                if (!started) {
                    _callHandshakeInfo.value = CallHandshakeInfo(peerId, peerName, isVideo)
                }
            }
        }
    }

    fun setBatterySaverMode(enabled: Boolean) {
        _isBatterySaverMode.value = enabled
        prefs.edit().putBoolean("battery_saver", enabled).apply()
        meshEngine.setBatterySaverMode(enabled)
    }

    fun setShowHelp(show: Boolean) {
        _showHelpBottomSheet.value = show
    }

    fun setShowEmergencyApk(show: Boolean) {
        _showEmergencyApkBottomSheet.value = show
    }

    fun selectSenderForCard(user: UserEntity?) {
        _selectedSenderForCard.value = user
    }

    fun clearInlineBanner() {
        _inlineBannerMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        meshEngine.stop()
        apkServer.stop()
    }
}

data class CallHandshakeInfo(
    val peerId: String,
    val peerName: String,
    val isVideo: Boolean
)

