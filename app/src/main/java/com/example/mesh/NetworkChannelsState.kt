package com.example.mesh

data class NetworkChannelsState(
    val isBluetoothEnabled: Boolean = false,
    val isWifiEnabled: Boolean = false,
    val isRouterLanConnected: Boolean = false,
    val isHotspotActive: Boolean = false,
    val isWifiDirectReady: Boolean = true,
    val connectedPeersCount: Int = 0,
    val localIp: String? = null
) {
    val statusText: String
        get() = when {
            isRouterLanConnected -> "Conectado por red local • Alta velocidad"
            isHotspotActive -> "Zona Wi-Fi activa • Celulares pueden conectarse"
            isBluetoothEnabled && isWifiEnabled && connectedPeersCount > 0 -> "$connectedPeersCount celulares conectados cerca"
            isBluetoothEnabled && isWifiEnabled -> "Buscando celulares cercanos..."
            isBluetoothEnabled -> "Buscando por Bluetooth • Enciende Wi-Fi para alta velocidad"
            isWifiEnabled -> "Wi-Fi encendido • Enciende Bluetooth para descubrir celulares"
            else -> "Activa Bluetooth y Wi-Fi para comunicarte"
        }
}
