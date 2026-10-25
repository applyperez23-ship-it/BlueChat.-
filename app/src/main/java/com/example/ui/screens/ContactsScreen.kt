package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.theme.BlueChatPrimary
import com.example.ui.theme.BlueChatSecondary
import com.example.ui.theme.BlueChatTertiary

@Composable
fun ContactsScreen(
    discoveredUsers: List<UserEntity>,
    savedContacts: List<UserEntity>,
    myPeerId: String,
    onSaveContact: (peerId: String, nickname: String) -> Unit,
    onStartChat: (peerId: String, nickname: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Radar en Vivo, 1: Mis Contactos

    val nearbyPeers = remember(discoveredUsers) {
        discoveredUsers.filter { it.peerId != myPeerId }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = Color.Transparent,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Radar en Vivo (${nearbyPeers.size})")
                    }
                },
                modifier = Modifier.testTag("tab_radar")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mis Contactos (${savedContacts.size})")
                    }
                },
                modifier = Modifier.testTag("tab_saved_contacts")
            )
        }

        if (selectedSubTab == 0) {
            // Sub-tab 1: Radar en Vivo
            RadarEnVivoView(
                nearbyPeers = nearbyPeers,
                onSaveContact = onSaveContact,
                onStartChat = onStartChat
            )
        } else {
            // Sub-tab 2: Mis Contactos
            MisContactosView(
                savedContacts = savedContacts,
                onStartChat = onStartChat
            )
        }
    }
}

@Composable
private fun RadarEnVivoView(
    nearbyPeers: List<UserEntity>,
    onSaveContact: (peerId: String, nickname: String) -> Unit,
    onStartChat: (peerId: String, nickname: String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Visual Radar Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2

                    // Concentric rings
                    drawCircle(Color(0xFF0070F3).copy(alpha = 0.2f), radius = radius, center = center, style = Stroke(1.5f))
                    drawCircle(Color(0xFF0070F3).copy(alpha = 0.35f), radius = radius * 0.66f, center = center, style = Stroke(1.5f))
                    drawCircle(Color(0xFF0070F3).copy(alpha = 0.5f), radius = radius * 0.33f, center = center, style = Stroke(1.5f))

                    // Radar sweep line
                    val sweepRadians = Math.toRadians(sweepAngle.toDouble())
                    val endX = center.x + (radius * Math.cos(sweepRadians)).toFloat()
                    val endY = center.y + (radius * Math.sin(sweepRadians)).toFloat()
                    drawLine(
                        brush = Brush.linearGradient(listOf(Color(0xFF00D2FF), Color.Transparent)),
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )
                }

                // Center node
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(BlueChatPrimary, CircleShape)
                )
            }
        }

        if (nearbyPeers.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Escaneando señales Bluetooth y Wi-Fi...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = BlueChatSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Cuando otro dispositivo con BlueChat esté al alcance, aparecerá aquí automáticamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(nearbyPeers, key = { it.peerId }) { peer ->
                RadarPeerCard(
                    peer = peer,
                    onSave = { onSaveContact(peer.peerId, peer.customNickname) },
                    onChat = { onStartChat(peer.peerId, peer.customNickname) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun RadarPeerCard(
    peer: UserEntity,
    onSave: () -> Unit,
    onChat: () -> Unit
) {
    val avatarColors = listOf(
        Color(0xFF0070F3), Color(0xFF00D2FF), Color(0xFF00E676),
        Color(0xFFFF9100), Color(0xFFE040FB), Color(0xFFFF5252)
    )
    val avatarBg = avatarColors[Math.abs(peer.peerId.hashCode()) % avatarColors.size]

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("radar_peer_${peer.peerId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(avatarBg, CircleShape)
            ) {
                Text(
                    text = peer.customNickname.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.customNickname,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.SignalCellularAlt, contentDescription = null, tint = BlueChatTertiary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (peer.routingHops <= 1) "Alcance Directo (${peer.rssi} dBm)" else "${peer.routingHops} saltos Mesh",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!peer.isSaved) {
                    OutlinedButton(
                        onClick = onSave,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_contact_button_${peer.peerId}")
                    ) {
                        Text("Agregar", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onChat,
                    colors = ButtonDefaults.buttonColors(containerColor = BlueChatPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("chat_contact_button_${peer.peerId}")
                ) {
                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MisContactosView(
    savedContacts: List<UserEntity>,
    onStartChat: (peerId: String, nickname: String) -> Unit
) {
    if (savedContacts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Aún no tienes contactos guardados",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Usa el Radar en Vivo para descubrir y guardar personas cercanas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(savedContacts, key = { it.peerId }) { contact ->
                val now = System.currentTimeMillis()
                val statusText = when {
                    now - contact.lastSeen < 60_000 -> "Cerca"
                    now - contact.lastSeen < 300_000 -> "En la red"
                    else -> "Fuera de alcance"
                }

                val statusColor = when (statusText) {
                    "Cerca" -> BlueChatTertiary
                    "En la red" -> BlueChatSecondary
                    else -> MaterialTheme.colorScheme.outlineVariant
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStartChat(contact.peerId, contact.customNickname) }
                        .testTag("saved_contact_card_${contact.peerId}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(46.dp)
                                .background(BlueChatPrimary.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Text(
                                text = contact.customNickname.take(1).uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.customNickname,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(statusColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor
                                )
                            }
                        }

                        IconButton(
                            onClick = { onStartChat(contact.peerId, contact.customNickname) },
                            modifier = Modifier.testTag("open_chat_for_${contact.peerId}")
                        ) {
                            Icon(imageVector = Icons.Default.Chat, contentDescription = "Abrir chat", tint = BlueChatPrimary)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
