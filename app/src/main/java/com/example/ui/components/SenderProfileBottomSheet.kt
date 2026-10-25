package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
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
import com.example.data.model.UserEntity
import com.example.ui.theme.BlueChatPrimary
import com.example.ui.theme.BlueChatSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenderProfileBottomSheet(
    user: UserEntity?,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onStartPrivateChat: (peerId: String, nickname: String) -> Unit,
    onSaveContact: (peerId: String, nickname: String) -> Unit
) {
    if (user == null) return

    var editableNickname by remember(user.peerId) { mutableStateOf(user.customNickname) }

    val avatarColors = listOf(
        Color(0xFF0070F3), Color(0xFF00D2FF), Color(0xFF00E676),
        Color(0xFFFF9100), Color(0xFFE040FB), Color(0xFFFF5252)
    )
    val avatarBg = avatarColors[Math.abs(user.peerId.hashCode()) % avatarColors.size]

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .background(avatarBg, CircleShape)
            ) {
                Text(
                    text = user.customNickname.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = user.customNickname,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Text(
                text = "ID: ${user.peerId.take(12)}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = editableNickname,
                onValueChange = { editableNickname = it },
                label = { Text("Apodo local") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sender_nickname_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Button(
                onClick = {
                    onStartPrivateChat(user.peerId, editableNickname)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_private_chat_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BlueChatPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Chat, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviar mensaje privado")
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    onSaveContact(user.peerId, editableNickname)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_contact_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (user.isSaved) Icons.Default.Check else Icons.Default.PersonAdd,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (user.isSaved) "Contacto guardado (Actualizar)" else "Guardar en contactos")
            }
        }
    }
}
