package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.server.QrCodeGenerator
import com.example.ui.theme.BlueChatPrimary
import com.example.ui.theme.BlueChatSecondary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyApkBottomSheet(
    serverUrl: String,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrBitmap: Bitmap = remember(serverUrl) {
        QrCodeGenerator.generateQrBitmap(serverUrl, size = 320)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WifiTethering,
                    contentDescription = null,
                    tint = BlueChatPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Distribuir APK Offline",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Required Step Card: Activate Hotspot
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint = BlueChatPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Paso 1: Activa tu Zona Wi-Fi (Hotspot)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = BlueChatPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Para que otros teléfonos descarguen el APK sin internet, debes activar tu Punto de Acceso Wi-Fi y pedirles que se conecten a él.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            openHotspotSettings(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueChatPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_hotspot_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Activar Zona Wi-Fi / Hotspot")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Text(
                text = "Paso 2: Haz que la otra persona se conecte a tu Wi-Fi y escanee este código QR:",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // QR Code Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Código QR para descargar BlueChat",
                    modifier = Modifier.size(196.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "O que visite esta dirección en su navegador:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = serverUrl,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = BlueChatPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    shareApkViaSystem(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_apk_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartir archivo APK por Bluetooth / Sistema", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

private fun openHotspotSettings(context: Context) {
    try {
        val intent = Intent().apply {
            action = "android.settings.TETHER_SETTINGS"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        } catch (ignored: Exception) {}
    }
}

private fun shareApkViaSystem(context: Context) {
    try {
        val apkPath = context.applicationInfo.sourceDir
        val apkFile = File(apkPath)
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
        } catch (e: Exception) {
            android.net.Uri.fromFile(apkFile)
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Enviar instalador BlueChat"))
    } catch (e: Exception) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Descarga BlueChat para comunicarte sin internet ni datos.")
        }
        context.startActivity(Intent.createChooser(intent, "Compartir BlueChat"))
    }
}
