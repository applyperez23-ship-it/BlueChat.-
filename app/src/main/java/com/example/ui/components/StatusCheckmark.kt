package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.MessageEntity
import com.example.ui.theme.CheckmarkBlue
import com.example.ui.theme.CheckmarkGray

@Composable
fun StatusCheckmark(
    status: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (status) {
            MessageEntity.STATUS_CLOCK -> {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Guardado localmente",
                    modifier = Modifier.size(14.dp),
                    tint = CheckmarkGray
                )
            }
            MessageEntity.STATUS_SENT -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Enviado a la red",
                    modifier = Modifier.size(14.dp),
                    tint = CheckmarkGray
                )
            }
            MessageEntity.STATUS_DELIVERED -> {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Entregado al dispositivo",
                    modifier = Modifier.size(16.dp),
                    tint = CheckmarkGray
                )
            }
            MessageEntity.STATUS_READ -> {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Leído por el destinatario",
                    modifier = Modifier.size(16.dp),
                    tint = CheckmarkBlue
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Enviado",
                    modifier = Modifier.size(14.dp),
                    tint = CheckmarkGray
                )
            }
        }
    }
}
