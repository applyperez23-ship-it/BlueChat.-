package com.example.mesh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.UUID

class BleManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val myPeerId: String,
    private var myNickname: String,
    private val onPeerDiscovered: (peerId: String, nickname: String, rssi: Int) -> Unit
) {
    companion object {
        private const val TAG = "BlueChatBle"
        val SERVICE_UUID: UUID = UUID.fromString("0000BCE0-0000-1000-8000-00805F9B34FB")
        val PARCEL_UUID = ParcelUuid(SERVICE_UUID)
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null

    private var isBatterySaver: Boolean = false
    private var dutyCycleJob: Job? = null
    private var isScanningActive = false
    private var isAdvertisingActive = false

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertisingActive = true
            Log.d(TAG, "BLE advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertisingActive = false
            Log.w(TAG, "BLE advertising failed with code: $errorCode")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val scanRecord = result.scanRecord ?: return
            val serviceData = scanRecord.getServiceData(PARCEL_UUID)
            if (serviceData != null && serviceData.isNotEmpty()) {
                val dataStr = String(serviceData, StandardCharsets.UTF_8)
                val parts = dataStr.split(":", limit = 2)
                if (parts.isNotEmpty()) {
                    val peerId = parts[0]
                    val nickname = if (parts.size > 1) parts[1] else "Usuario BlueChat"
                    if (peerId != myPeerId) {
                        onPeerDiscovered(peerId, nickname, result.rssi)
                    }
                }
            } else {
                // If service data is not available, check device name
                val name = result.device.name
                if (name != null && name.startsWith("BC_")) {
                    val peerId = name.removePrefix("BC_")
                    if (peerId != myPeerId) {
                        onPeerDiscovered(peerId, "Peer_${peerId.take(4)}", result.rssi)
                    }
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE scan failed with code: $errorCode")
        }
    }

    fun updateNickname(newNickname: String) {
        myNickname = newNickname
        if (isAdvertisingActive) {
            stopAdvertising()
            startAdvertising()
        }
    }

    fun setBatterySaverMode(enabled: Boolean) {
        isBatterySaver = enabled
        restartDutyCycle()
    }

    fun start() {
        advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        scanner = bluetoothAdapter?.bluetoothLeScanner
        restartDutyCycle()
    }

    fun stop() {
        dutyCycleJob?.cancel()
        stopAdvertising()
        stopScanning()
    }

    private fun restartDutyCycle() {
        dutyCycleJob?.cancel()
        dutyCycleJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                // Determine cycle interval
                val cycleSeconds = if (isBatterySaver) 17L else 7L

                startAdvertising()
                startScanning()

                delay(cycleSeconds * 1000)

                // Brief pause for battery breathing in power-saver mode
                if (isBatterySaver) {
                    stopScanning()
                    delay(3000)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        if (bluetoothAdapter?.isEnabled != true) return
        if (advertiser == null) {
            advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        }
        val adv = advertiser ?: return

        try {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(
                    if (isBatterySaver) AdvertiseSettings.ADVERTISE_MODE_LOW_POWER
                    else AdvertiseSettings.ADVERTISE_MODE_BALANCED
                )
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .setTimeout(0)
                .build()

            // Service data: peerId:nickname truncated to fit 24 bytes
            val cleanId = myPeerId.take(8)
            val cleanName = myNickname.take(12)
            val payload = "$cleanId:$cleanName".toByteArray(StandardCharsets.UTF_8)

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(PARCEL_UUID)
                .addServiceData(PARCEL_UUID, payload)
                .build()

            adv.startAdvertising(settings, data, advertiseCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Error starting advertising: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            isAdvertisingActive = false
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping advertising: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanning() {
        if (bluetoothAdapter?.isEnabled != true) return
        if (scanner == null) {
            scanner = bluetoothAdapter?.bluetoothLeScanner
        }
        val sc = scanner ?: return
        if (isScanningActive) return

        try {
            val filter = ScanFilter.Builder()
                .setServiceUuid(PARCEL_UUID)
                .build()

            val settings = ScanSettings.Builder()
                .setScanMode(
                    if (isBatterySaver) ScanSettings.SCAN_MODE_LOW_POWER
                    else ScanSettings.SCAN_MODE_BALANCED
                )
                .build()

            sc.startScan(listOf(filter), settings, scanCallback)
            isScanningActive = true
        } catch (e: Exception) {
            Log.w(TAG, "Error starting scanning: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        try {
            if (isScanningActive) {
                scanner?.stopScan(scanCallback)
                isScanningActive = false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping scanning: ${e.message}")
        }
    }
}
