package com.example.server

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

class OfflineApkServer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "BlueChatApkServer"
        const val SERVER_PORT = 8080
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    var isRunning = false
        private set

    fun getLocalIpAddress(): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
        if (ipInt == 0) return "192.168.43.1" // Standard Android Hotspot gateway
        return String.format(
            "%d.%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )
    }

    fun getServerUrl(): String {
        return "http://${getLocalIpAddress()}:$SERVER_PORT"
    }

    fun start() {
        if (isRunning) return
        stop()

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(SERVER_PORT)
                isRunning = true
                Log.d(TAG, "Offline APK Server started on port $SERVER_PORT")

                while (isActive) {
                    val clientSocket = try {
                        serverSocket?.accept()
                    } catch (e: Exception) {
                        null
                    }

                    clientSocket?.let { socket ->
                        handleClient(socket)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Offline APK Server socket error: ${e.message}")
            } finally {
                isRunning = false
            }
        }
    }

    private fun handleClient(socket: Socket) {
        scope.launch(Dispatchers.IO) {
            try {
                val input = socket.getInputStream()
                val output = socket.getOutputStream()
                val reader = input.bufferedReader()
                val requestLine = reader.readLine() ?: ""
                val tokens = requestLine.split(" ")
                val method = if (tokens.isNotEmpty()) tokens[0] else "GET"
                val path = if (tokens.size > 1) tokens[1] else "/"

                if (method.equals("GET", ignoreCase = true)) {
                    when {
                        path == "/download" || path.endsWith(".apk") -> {
                            serveApkFile(output)
                        }
                        else -> {
                            serveHtmlPortal(output)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Client handle error: ${e.message}")
            } finally {
                try {
                    socket.close()
                } catch (ignored: Exception) {}
            }
        }
    }

    private fun serveHtmlPortal(output: OutputStream) {
        val html = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>BlueChat • Instalador Offline</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        background: #0A192F;
                        color: #E2E8F0;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        padding: 20px;
                    }
                    .card {
                        background: #112240;
                        border: 1px solid #233554;
                        border-radius: 28px;
                        max-width: 440px;
                        width: 100%;
                        padding: 32px 24px;
                        text-align: center;
                        box-shadow: 0 20px 40px rgba(0,0,0,0.5);
                    }
                    .badge {
                        display: inline-block;
                        background: rgba(0, 210, 255, 0.15);
                        color: #00D2FF;
                        font-size: 13px;
                        font-weight: 700;
                        padding: 6px 14px;
                        border-radius: 20px;
                        margin-bottom: 20px;
                        letter-spacing: 0.5px;
                        text-transform: uppercase;
                    }
                    h1 {
                        font-size: 28px;
                        font-weight: 800;
                        color: #FFFFFF;
                        margin-bottom: 12px;
                    }
                    p {
                        font-size: 15px;
                        line-height: 1.6;
                        color: #94A3B8;
                        margin-bottom: 28px;
                    }
                    .btn {
                        display: block;
                        width: 100%;
                        padding: 16px 20px;
                        background: #0070F3;
                        color: #FFFFFF;
                        text-decoration: none;
                        font-size: 17px;
                        font-weight: 700;
                        border-radius: 16px;
                        box-shadow: 0 8px 24px rgba(0, 112, 243, 0.4);
                        transition: background 0.2s, transform 0.1s;
                    }
                    .btn:active {
                        transform: scale(0.98);
                    }
                    .steps {
                        margin-top: 32px;
                        text-align: left;
                        background: #0D1B2A;
                        padding: 16px;
                        border-radius: 16px;
                        font-size: 13px;
                        color: #A0AEC0;
                    }
                    .steps ol {
                        padding-left: 20px;
                        line-height: 1.8;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <span class="badge">Red Mesh Local • Sin Internet</span>
                    <h1>BlueChat APK</h1>
                    <p>Mensajería P2P cifrada, llamadas de voz y transferencia de archivos directa entre teléfonos sin cobertura ni saldo.</p>
                    <a href="/download" class="btn">Descargar BlueChat (APK)</a>
                    <div class="steps">
                        <strong>Instrucciones de instalación:</strong>
                        <ol>
                            <li>Toca el botón azul para descargar el archivo APK.</li>
                            <li>Abre la descarga en tu navegador o explorador de archivos.</li>
                            <li>Permite la instalación desde esta fuente si tu teléfono lo solicita.</li>
                        </ol>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val bytes = html.toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"

        output.write(header.toByteArray(StandardCharsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun serveApkFile(output: OutputStream) {
        val apkPath = context.applicationInfo.sourceDir
        val apkFile = File(apkPath)

        if (apkFile.exists() && apkFile.canRead()) {
            val length = apkFile.length()
            val header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/vnd.android.package-archive\r\n" +
                    "Content-Disposition: attachment; filename=\"BlueChat.apk\"\r\n" +
                    "Content-Length: $length\r\n" +
                    "Connection: close\r\n\r\n"

            output.write(header.toByteArray(StandardCharsets.UTF_8))

            val fis = FileInputStream(apkFile)
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
            }
            fis.close()
            output.flush()
        } else {
            val notFound = "HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\nAPK File Not Found"
            output.write(notFound.toByteArray(StandardCharsets.UTF_8))
            output.flush()
        }
    }

    fun stop() {
        serverJob?.cancel()
        try {
            serverSocket?.close()
        } catch (ignored: Exception) {}
        isRunning = false
    }
}
