package com.example.mesh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BluetoothClassicServer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onPacketReceived: (rawJson: String) -> Unit
) {
    companion object {
        private const val TAG = "BlueChatClassicBT"
        val RFCOMM_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        private const val SERVICE_NAME = "BlueChatClassicRFCOMM"
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var serverSocket: BluetoothServerSocket? = null
    private var acceptJob: Job? = null
    private val activeSockets = ConcurrentHashMap<String, BluetoothSocket>()

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (bluetoothAdapter?.isEnabled != true) return
        stopListening()

        acceptJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, RFCOMM_UUID)
                Log.d(TAG, "RFCOMM server socket listening...")

                while (isActive) {
                    val socket: BluetoothSocket? = try {
                        serverSocket?.accept()
                    } catch (e: Exception) {
                        null
                    }

                    socket?.let { clientSocket ->
                        val remoteAddress = clientSocket.remoteDevice.address
                        activeSockets[remoteAddress] = clientSocket
                        handleConnectedSocket(clientSocket)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "RFCOMM accept loop error: ${e.message}")
            }
        }
    }

    private fun handleConnectedSocket(socket: BluetoothSocket) {
        scope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    if (line.isNotBlank()) {
                        onPacketReceived(line)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "RFCOMM socket disconnected: ${e.message}")
            } finally {
                try {
                    socket.close()
                } catch (ignored: Exception) {}
                activeSockets.remove(socket.remoteDevice.address)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun sendPacket(device: BluetoothDevice, json: String) {
        scope.launch(Dispatchers.IO) {
            var socket: BluetoothSocket? = activeSockets[device.address]
            try {
                if (socket == null || !socket.isConnected) {
                    socket = device.createRfcommSocketToServiceRecord(RFCOMM_UUID)
                    socket.connect()
                    activeSockets[device.address] = socket
                    handleConnectedSocket(socket)
                }

                val writer = PrintWriter(OutputStreamWriter(socket.outputStream), true)
                writer.println(json)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send RFCOMM packet to ${device.address}: ${e.message}")
            }
        }
    }

    fun broadcastPacket(json: String) {
        scope.launch(Dispatchers.IO) {
            activeSockets.values.forEach { socket ->
                try {
                    val writer = PrintWriter(OutputStreamWriter(socket.outputStream), true)
                    writer.println(json)
                } catch (e: Exception) {
                    Log.w(TAG, "Error broadcasting over RFCOMM: ${e.message}")
                }
            }
        }
    }

    fun stopListening() {
        acceptJob?.cancel()
        try {
            serverSocket?.close()
        } catch (ignored: Exception) {}
        activeSockets.values.forEach {
            try {
                it.close()
            } catch (ignored: Exception) {}
        }
        activeSockets.clear()
    }
}
