package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlueChatPrimary
import com.example.ui.theme.BlueChatSecondary
import com.example.ui.theme.BlueChatTertiary

@Composable
fun SettingsScreen(
    myPeerId: String,
    myNickname: String,
    myStatusBio: String,
    isBatterySaver: Boolean,
    activeNodesCount: Int,
    onSaveProfile: (nickname: String, bio: String) -> Unit,
    onToggleBatterySaver: (Boolean) -> Unit,
    onOpenApkServerModal: () -> Unit,
    onOpenHelpModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editNickname by remember(myNickname) { mutableStateOf(myNickname) }
    var editBio by remember(myStatusBio) { mutableStateOf(myStatusBio) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Profile Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .background(BlueChatPrimary, CircleShape)
                ) {
                    Text(
                        text = editNickname.take(1).uppercase().ifEmpty { "B" },
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hardware ID tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp), tint = BlueChatSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ID: $myPeerId",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = editNickname,
                    onValueChange = { editNickname = it },
                    label = { Text("Apodo en la red") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_nickname_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editBio,
                    onValueChange = { editBio = it },
                    label = { Text("Estado o información") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_bio_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onSaveProfile(editNickname, editBio) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_profile_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueChatPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Actualizar Perfil")
                }
            }
        }

        // Network Telemetry & Battery Saver Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Telemetría de Red Mesh",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Hub, contentDescription = null, tint = BlueChatTertiary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Nodos conectados")
                    }
                    Text(
                        text = "$activeNodesCount activos",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BlueChatTertiary)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.BatterySaver, contentDescription = null, tint = BlueChatSecondary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Ahorro de batería", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (isBatterySaver) "Ciclo de 17s (Consumo mínimo)" else "Ciclo de 7s (Máxima velocidad)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isBatterySaver,
                        onCheckedChange = onToggleBatterySaver,
                        colors = SwitchDefaults.colors(checkedThumbColor = BlueChatPrimary),
                        modifier = Modifier.testTag("battery_saver_switch")
                    )
                }
            }
        }

        // Tools & Sharing Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Distribución y Ayuda",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Button(
                    onClick = onOpenApkServerModal,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_apk_server_button")
                ) {
                    Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = BlueChatPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Distribuir APK por Wi-Fi / QR", color = MaterialTheme.colorScheme.onSurface)
                }

                Button(
                    onClick = onOpenHelpModal,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_help_guides_button")
                ) {
                    Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, tint = BlueChatSecondary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Guías de Supervivencia y Uso", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
