package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlueChatPrimary
import com.example.ui.theme.BlueChatSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SituationalHelpBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = BlueChatPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Guías de Supervivencia y Uso",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Text(
                text = "BlueChat funciona 100% sin internet. Consulta cómo sacarle el máximo partido en situaciones reales:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            HelpGuideCard(
                icon = Icons.Default.FlashOff,
                title = "¿Cómo comunicarse durante un apagón o sin cobertura?",
                description = "Mantén activado el Bluetooth y el servicio en segundo plano de BlueChat. Tus mensajes saltarán de teléfono en teléfono mediante la red Mesh hasta encontrar a su destinatario, sin importar que no haya señal de torres celulares."
            )

            Spacer(modifier = Modifier.height(12.dp))

            HelpGuideCard(
                icon = Icons.Default.Router,
                title = "¿Cómo conectar varios teléfonos usando un router casero?",
                description = "Si tienes un módem o router Wi-Fi en casa (como Nauta Hogar o un router viejo), conecta todos los teléfonos a ese Wi-Fi aunque no tenga internet. BlueChat activará el puente LAN de alta velocidad, multiplicando el alcance y la velocidad entre habitaciones."
            )

            Spacer(modifier = Modifier.height(12.dp))

            HelpGuideCard(
                icon = Icons.Default.Campaign,
                title = "¿Cómo usar el Canal General y sus comandos?",
                description = "• Escribe @todos para enviar un aviso prioritario que se propaga con alcance máximo (20 saltos).\n• Escribe /contactos al inicio del mensaje para que solo las personas que te tengan guardado en sus contactos puedan leerlo."
            )

            Spacer(modifier = Modifier.height(12.dp))

            HelpGuideCard(
                icon = Icons.Default.PermMedia,
                title = "¿Cómo enviar fotos o archivos pesados?",
                description = "Para no saturar la red Bluetooth, los archivos grandes esperan automáticamente a que ambos dispositivos estén en alcance directo Wi-Fi. Verás 'Esperando conexión directa Wi-Fi' y la transferencia se iniciará sola."
            )
        }
    }
}

@Composable
private fun HelpGuideCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BlueChatSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
