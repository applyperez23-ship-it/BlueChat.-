package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MessageEntity
import com.example.data.model.UserEntity
import com.example.ui.components.StatusCheckmark
import com.example.ui.theme.BlueChatPrimary
import com.example.ui.theme.BlueChatSecondary
import com.example.ui.theme.IncomingBubbleDark
import com.example.ui.theme.IncomingBubbleLight
import com.example.ui.theme.OutgoingBubbleDark
import com.example.ui.theme.OutgoingBubbleLight
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    peerName: String,
    messages: List<MessageEntity>,
    myPeerId: String,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendFile: (fileName: String, fileSize: Long, filePath: String) -> Unit,
    onSendAudio: (durationSeconds: Long) -> Unit,
    onStartCall: (isVideo: Boolean) -> Unit,
    onSenderClick: (UserEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val isGeneral = chatId == "general_channel"
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Quick Voice Note Recording State
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingSeconds = 0L
            while (isRecordingAudio) {
                delay(1000)
                recordingSeconds += 1
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val avatarColors = listOf(
        Color(0xFF0070F3), Color(0xFF00D2FF), Color(0xFF00E676),
        Color(0xFFFF9100), Color(0xFFE040FB), Color(0xFFFF5252)
    )
    val avatarBg = avatarColors[Math.abs(chatId.hashCode()) % avatarColors.size]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // WhatsApp-style Clean Header
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (isGeneral) BlueChatPrimary else avatarBg, CircleShape)
                    ) {
                        if (isGeneral) {
                            Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text(
                                text = peerName.take(1).uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = peerName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isGeneral) "Canal público para todos los nodos" else "Cifrado de extremo a extremo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            },
            actions = {
                if (!isGeneral) {
                    IconButton(
                        onClick = { onStartCall(false) },
                        modifier = Modifier.testTag("start_voice_call_button")
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Llamada de voz")
                    }
                    IconButton(
                        onClick = { onStartCall(true) },
                        modifier = Modifier.testTag("start_video_call_button")
                    ) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = "Videollamada")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(messages, key = { it.messageId }) { msg ->
                MessageBubbleItem(
                    message = msg,
                    isOutgoing = (msg.senderId == myPeerId),
                    isGeneralChannel = isGeneral,
                    onSenderClick = {
                        onSenderClick(
                            UserEntity(
                                peerId = msg.senderId,
                                customNickname = msg.senderName,
                                statusBio = "Conectado por BlueChat"
                            )
                        )
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Quick command chips for General Channel
        if (isGeneral) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        if (!inputText.contains("@todos")) {
                            inputText = "@todos $inputText".trim()
                        }
                    },
                    label = { Text("@todos (Prioritario)") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.PriorityHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
                AssistChip(
                    onClick = {
                        if (!inputText.startsWith("/contactos")) {
                            inputText = "/contactos $inputText".trim()
                        }
                    },
                    label = { Text("/contactos (Privado)") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        // Input Field pinned rigidly above keyboard
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isRecordingAudio) {
                // Quick Audio Recording Bar
                val infiniteTransition = rememberInfiniteTransition(label = "audio_rec_pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                val recMin = recordingSeconds / 60
                val recSec = recordingSeconds % 60
                val timerString = String.format("%02d:%02d", recMin, recSec)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            isRecordingAudio = false
                        },
                        modifier = Modifier.testTag("cancel_audio_recording_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Descartar audio",
                            tint = Color(0xFFFF5252)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .scale(pulseScale)
                                .background(Color(0xFFFF5252), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Grabando audio rápido... $timerString",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFF5252)
                        )
                    }

                    FilledIconButton(
                        onClick = {
                            val duration = recordingSeconds
                            isRecordingAudio = false
                            onSendAudio(duration)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = BlueChatPrimary),
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("send_audio_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar nota de voz",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attach file button
                    IconButton(
                        onClick = {
                            onSendFile("Foto_BlueChat_${System.currentTimeMillis() % 1000}.jpg", 2_450_000L, "/storage/emulated/0/Download/photo.jpg")
                        },
                        modifier = Modifier.testTag("attach_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Adjuntar archivo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Escribe un mensaje...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .testTag("message_input_field")
                    )

                    if (inputText.isNotBlank()) {
                        FilledIconButton(
                            onClick = {
                                onSendMessage(inputText)
                                inputText = ""
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("send_message_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = BlueChatPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar mensaje",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        // Quick Voice Note / Audio Button
                        FilledIconButton(
                            onClick = {
                                isRecordingAudio = true
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("quick_audio_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = BlueChatPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Grabar audio rápido",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubbleItem(
    message: MessageEntity,
    isOutgoing: Boolean,
    isGeneralChannel: Boolean,
    onSenderClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val bubbleColor = if (isOutgoing) {
        if (isDark) OutgoingBubbleDark else OutgoingBubbleLight
    } else {
        if (isDark) IncomingBubbleDark else IncomingBubbleLight
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) {
        timeFormat.format(Date(message.timestamp))
    }

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val isAudioMessage = message.textPayload.startsWith("Nota de voz") || message.fileName?.endsWith(".m4a") == true

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        // High-Priority Banner for @todos
        if (message.isPriority) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp, start = 8.dp, end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PriorityHigh,
                    contentDescription = null,
                    tint = Color(0xFFFF9100),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Aviso prioritario",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFFF9100))
                )
            }
        }

        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = 1.dp,
            modifier = Modifier
                .widthIn(min = 100.dp, max = 320.dp)
                .clickable(enabled = (!isOutgoing && isGeneralChannel)) {
                    onSenderClick()
                }
                .testTag("message_bubble_${message.messageId}")
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                // Sender nickname in General Channel
                if (isGeneralChannel && !isOutgoing) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BlueChatSecondary
                        ),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Waiting Wi-Fi Direct file transfer banner
                if (message.waitingWifi) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0x330070F3), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = BlueChatPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Esperando conexión directa Wi-Fi para enviar archivo",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = BlueChatPrimary)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Audio message layout or standard text
                if (isAudioMessage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .background(BlueChatPrimary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Reproducir nota de voz",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Waveform visualization bars
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val barHeights = listOf(8, 16, 12, 22, 18, 14, 20, 10, 16, 12, 18, 6)
                            barHeights.forEach { h ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(h.dp)
                                        .background(BlueChatPrimary.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = message.textPayload.substringAfter("(").substringBefore(")").ifEmpty { "0:05" },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Standard Message Text
                    Text(
                        text = message.textPayload,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Bottom row: Timestamp + Status Checkmarks
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        StatusCheckmark(status = message.status)
                    }
                }
            }
        }
    }
}
