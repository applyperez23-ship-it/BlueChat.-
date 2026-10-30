package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mesh.NetworkChannelsState
import com.example.ui.theme.BlueChatPrimary
import com.example.ui.theme.BlueChatSecondary
import com.example.ui.theme.BlueChatTertiary

@Composable
fun ActiveChannelsPill(
    channelsState: NetworkChannelsState,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_channels")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val anyRadioActive = channelsState.isBluetoothEnabled || channelsState.isWifiEnabled

    Column(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            modifier = Modifier
                .clickable { isExpanded = !isExpanded }
                .testTag("active_channels_pill")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                // Pulsing Status Dot
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(pulseScale)
                            .background(
                                color = (if (anyRadioActive) BlueChatTertiary else Color(0xFFFF5252)).copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (anyRadioActive) BlueChatTertiary else Color(0xFFFF5252),
                                shape = CircleShape
                            )
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Discreet Active Icons
                Icon(
                    imageVector = if (channelsState.isBluetoothEnabled) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                    contentDescription = null,
                    tint = if (channelsState.isBluetoothEnabled) BlueChatPrimary else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (channelsState.isWifiEnabled) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (channelsState.isWifiEnabled) BlueChatSecondary else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(14.dp)
                )

                if (channelsState.isRouterLanConnected) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        tint = BlueChatTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (channelsState.isHotspotActive) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.WifiTethering,
                        contentDescription = null,
                        tint = Color(0xFFFF9100),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                val label = when {
                    channelsState.connectedPeersCount == 1 -> "1 celular cerca"
                    channelsState.connectedPeersCount > 1 -> "${channelsState.connectedPeersCount} celulares cerca"
                    channelsState.isRouterLanConnected -> "Red local"
                    anyRadioActive -> "Buscando..."
                    else -> "Sin radios"
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(2.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Detalles de conexión",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Expandable Details Card
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(310.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Vías de conexión activas",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ChannelRow(
                        icon = Icons.Default.Bluetooth,
                        name = "Bluetooth",
                        status = if (channelsState.isBluetoothEnabled) "Activo • Pasa mensajes de celular en celular" else "Desactivado",
                        isActive = channelsState.isBluetoothEnabled,
                        activeColor = BlueChatPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    ChannelRow(
                        icon = Icons.Default.Wifi,
                        name = "Wi-Fi Direct / Local",
                        status = if (channelsState.isWifiEnabled) "Listo • Fotos y llamadas a velocidad máxima" else "Desactivado",
                        isActive = channelsState.isWifiEnabled,
                        activeColor = BlueChatSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    ChannelRow(
                        icon = Icons.Default.Router,
                        name = "Router Casero / Nauta",
                        status = if (channelsState.isRouterLanConnected) "Conectado • Alcance extendido en el edificio" else "Sin router conectado",
                        isActive = channelsState.isRouterLanConnected,
                        activeColor = BlueChatTertiary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    ChannelRow(
                        icon = Icons.Default.WifiTethering,
                        name = "Zona Wi-Fi (Hotspot)",
                        status = if (channelsState.isHotspotActive) "Punto de acceso activo para otros teléfonos" else "Inactiva",
                        isActive = channelsState.isHotspotActive,
                        activeColor = Color(0xFFFF9100)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(
    icon: ImageVector,
    name: String,
    status: String,
    isActive: Boolean,
    activeColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (isActive) activeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) activeColor else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
