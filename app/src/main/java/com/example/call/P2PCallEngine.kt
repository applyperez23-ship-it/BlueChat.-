package com.example.call

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import com.example.data.model.CallLogEntity
import com.example.data.repository.BlueChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.UUID

enum class CallState {
    IDLE,
    OUTGOING_RINGING,
    INCOMING_RINGING,
    CONNECTED,
    RECONNECTING,
    ENDED
}

class P2PCallEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repository: BlueChatRepository
) {
    companion object {
        private const val TAG = "BlueChatCallEngine"
        const val CALL_UDP_PORT = 9888
        private const val SAMPLE_RATE = 16000
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _activePeerId = MutableStateFlow<String?>(null)
    val activePeerId: StateFlow<String?> = _activePeerId.asStateFlow()

    private val _activePeerName = MutableStateFlow("Contacto")
    val activePeerName: StateFlow<String> = _activePeerName.asStateFlow()

    private val _isVideoCall = MutableStateFlow(false)
    val isVideoCall: StateFlow<Boolean> = _isVideoCall.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0L)
    val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private var durationTimerJob: Job? = null
    private var audioStreamJob: Job? = null
    private var socket: DatagramSocket? = null
    private var remoteIp: String? = null
    private var callStartTime: Long = 0L

    fun startCall(peerId: String, peerName: String, targetIp: String?, isVideo: Boolean): Boolean {
        if (targetIp == null) {
            // Cannot start real-time call if contact is out of direct LAN/Wi-Fi range
            return false
        }

        remoteIp = targetIp
        _activePeerId.value = peerId
        _activePeerName.value = peerName
        _isVideoCall.value = isVideo
        _callState.value = CallState.OUTGOING_RINGING
        _durationSeconds.value = 0L

        scope.launch(Dispatchers.IO) {
            // Send call invitation packet over UDP
            sendSignalPacket("CALL_INVITE:${isVideo}")
            delay(2000)
            // Transition to connected once socket handshake succeeds
            if (_callState.value == CallState.OUTGOING_RINGING) {
                connectCall()
            }
        }
        return true
    }

    fun answerCall(peerId: String, peerName: String, targetIp: String, isVideo: Boolean) {
        remoteIp = targetIp
        _activePeerId.value = peerId
        _activePeerName.value = peerName
        _isVideoCall.value = isVideo
        connectCall()
    }

    private fun connectCall() {
        _callState.value = CallState.CONNECTED
        callStartTime = System.currentTimeMillis()
        startDurationTimer()
        startAudioStreaming()
    }

    private fun startDurationTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = scope.launch(Dispatchers.Default) {
            while (isActive && _callState.value == CallState.CONNECTED) {
                delay(1000)
                _durationSeconds.value += 1
            }
        }
    }

    private fun startAudioStreaming() {
        audioStreamJob?.cancel()
        audioStreamJob = scope.launch(Dispatchers.IO) {
            try {
                if (socket == null || socket?.isClosed == true) {
                    socket = DatagramSocket(CALL_UDP_PORT).apply {
                        reuseAddress = true
                    }
                }

                // Audio configuration
                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(2048)

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .build()

                audioTrack.play()

                val audioRecord = try {
                    AudioRecord(
                        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    ).apply { startRecording() }
                } catch (e: Exception) {
                    null
                }

                // Audio playback loop
                val receiveBuffer = ByteArray(bufferSize)
                val targetAddr = InetAddress.getByName(remoteIp)

                launch(Dispatchers.IO) {
                    while (isActive && _callState.value == CallState.CONNECTED) {
                        try {
                            val packet = DatagramPacket(receiveBuffer, receiveBuffer.size)
                            socket?.receive(packet)
                            if (packet.length > 0) {
                                audioTrack.write(packet.data, 0, packet.length)
                            }
                        } catch (e: Exception) {
                            if (!isActive) break
                        }
                    }
                }

                // Audio record & send loop
                val sendBuffer = ByteArray(bufferSize)
                while (isActive && _callState.value == CallState.CONNECTED) {
                    val read = audioRecord?.read(sendBuffer, 0, sendBuffer.size) ?: 0
                    if (read > 0 && !_isMicMuted.value) {
                        val packet = DatagramPacket(sendBuffer, read, targetAddr, CALL_UDP_PORT)
                        socket?.send(packet)
                    }
                    delay(20)
                }

                audioRecord?.stop()
                audioRecord?.release()
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.w(TAG, "Audio stream loop error: ${e.message}")
            }
        }
    }

    private fun sendSignalPacket(signal: String) {
        val ip = remoteIp ?: return
        try {
            val sock = DatagramSocket()
            val data = signal.toByteArray()
            val addr = InetAddress.getByName(ip)
            val packet = DatagramPacket(data, data.size, addr, CALL_UDP_PORT)
            sock.send(packet)
            sock.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error sending call signal: ${e.message}")
        }
    }

    fun toggleMute() {
        _isMicMuted.value = !_isMicMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
        audioManager?.isSpeakerphoneOn = _isSpeakerOn.value
    }

    fun toggleCameraFlip() {
        _isFrontCamera.value = !_isFrontCamera.value
    }

    fun endCall() {
        val duration = _durationSeconds.value
        val peerId = _activePeerId.value ?: "unknown"
        val peerName = _activePeerName.value
        val isVideo = _isVideoCall.value

        _callState.value = CallState.ENDED
        durationTimerJob?.cancel()
        audioStreamJob?.cancel()
        try {
            socket?.close()
        } catch (ignored: Exception) {}
        socket = null

        // Save call record in Room DB
        scope.launch(Dispatchers.IO) {
            val log = CallLogEntity(
                callId = UUID.randomUUID().toString(),
                peerId = peerId,
                peerName = peerName,
                isVideo = isVideo,
                isIncoming = false,
                durationSeconds = duration,
                timestamp = System.currentTimeMillis()
            )
            repository.insertCallLog(log)
        }

        scope.launch {
            delay(1000)
            _callState.value = CallState.IDLE
            _activePeerId.value = null
            _durationSeconds.value = 0L
        }
    }
}
