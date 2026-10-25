package com.example.mesh

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

class WifiLanBridge(
    private val context: Context,
    private val scope: CoroutineScope,
    private val myPeerId: String,
    private val onPacketReceived: (rawJson: String, remoteIp: String) -> Unit
) {
    companion object {
        private const val TAG = "BlueChatLanBridge"
        const val UDP_BROADCAST_PORT = 9876
        const val TCP_SERVER_PORT = 9877
    }

    private var udpSocket: DatagramSocket? = null
    private var tcpServer: ServerSocket? = null
    private var udpListenJob: Job? = null
    private var tcpListenJob: Job? = null

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun getLocalIpAddress(): String? {
        val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
        if (ipInt == 0) return null
        return String.format(
            "%d.%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )
    }

    fun start() {
        stop()

        try {
            multicastLock = wifiManager?.createMulticastLock("BlueChatMulticast")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "MulticastLock failed: ${e.message}")
        }

        startUdpListener()
        startTcpListener()
    }

    private fun startUdpListener() {
        udpListenJob = scope.launch(Dispatchers.IO) {
            try {
                udpSocket = DatagramSocket(UDP_BROADCAST_PORT).apply {
                    broadcast = true
                    reuseAddress = true
                }
                Log.d(TAG, "UDP listener started on port $UDP_BROADCAST_PORT")

                val buffer = ByteArray(4096)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        udpSocket?.receive(packet)
                        val text = String(packet.data, 0, packet.length, StandardCharsets.UTF_8)
                        val senderIp = packet.address.hostAddress ?: ""
                        if (text.isNotBlank()) {
                            onPacketReceived(text, senderIp)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "UDP socket listen error: ${e.message}")
            }
        }
    }

    private fun startTcpListener() {
        tcpListenJob = scope.launch(Dispatchers.IO) {
            try {
                tcpServer = ServerSocket(TCP_SERVER_PORT)
                Log.d(TAG, "TCP bridge server started on port $TCP_SERVER_PORT")

                while (isActive) {
                    val clientSocket = try {
                        tcpServer?.accept()
                    } catch (e: Exception) {
                        null
                    }

                    clientSocket?.let { socket ->
                        val remoteIp = socket.inetAddress.hostAddress ?: ""
                        handleTcpClient(socket, remoteIp)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "TCP Server error: ${e.message}")
            }
        }
    }

    private fun handleTcpClient(socket: Socket, remoteIp: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream, StandardCharsets.UTF_8))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    if (line.isNotBlank()) {
                        onPacketReceived(line, remoteIp)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "TCP client error: ${e.message}")
            } finally {
                try {
                    socket.close()
                } catch (ignored: Exception) {}
            }
        }
    }

    fun broadcastUdp(messageJson: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val socket = DatagramSocket()
                socket.broadcast = true
                val data = messageJson.toByteArray(StandardCharsets.UTF_8)
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(data, data.size, broadcastAddr, UDP_BROADCAST_PORT)
                socket.send(packet)
                socket.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error sending UDP broadcast: ${e.message}")
            }
        }
    }

    fun sendTcp(targetIp: String, messageJson: String, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch(Dispatchers.IO) {
            var success = false
            try {
                val socket = Socket(targetIp, TCP_SERVER_PORT)
                socket.soTimeout = 5000
                val writer = PrintWriter(OutputStreamWriter(socket.outputStream, StandardCharsets.UTF_8), true)
                writer.println(messageJson)
                socket.close()
                success = true
            } catch (e: Exception) {
                Log.w(TAG, "Failed TCP delivery to $targetIp: ${e.message}")
            }
            onComplete?.invoke(success)
        }
    }

    fun stop() {
        udpListenJob?.cancel()
        tcpListenJob?.cancel()
        try {
            udpSocket?.close()
        } catch (ignored: Exception) {}
        try {
            tcpServer?.close()
        } catch (ignored: Exception) {}
        try {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (ignored: Exception) {}
    }
}
