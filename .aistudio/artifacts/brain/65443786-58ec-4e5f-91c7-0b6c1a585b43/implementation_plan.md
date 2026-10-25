# BlueChat - Offline-First P2P Mesh Messaging Ecosystem

BlueChat is a resilient, 100% offline peer-to-peer messaging ecosystem designed to function without internet or cellular connectivity. It bridges nearby Android devices using zero-pairing Bluetooth Low Energy (BLE), Bluetooth Classic RFCOMM sockets, and local Wi-Fi router UDP/TCP bridging to create an ad-hoc local mesh for secure text, high-priority public announcements, contacts-only privacy filters, direct file transfers, real-time P2P voice/video calling, and an embedded offline APK distribution server.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed during the interactive interview phase:
> - **P2P Audio/Video Calling**: Implemented via direct LAN TCP/UDP socket streaming strictly between two physical devices with camera preview, duration timer, and audio controls.
> - **Embedded Offline Web Server**: Standalone APK download portal with dynamically generated QR code (`http://<local-ip>:8080`) and Android System Share Sheet trigger (`Intent.ACTION_SEND`).
> - **Mesh Engine Hardware Radios**: Real hardware radios only (BLE Scanning/Advertising, Bluetooth Classic RFCOMM, Wi-Fi Direct / LAN UDP broadcast) without simulated peers.

---

## 1. Overview & Core Concept

- **Zero Internet Requirement**: Completely eliminates external cloud dependencies, central authentication servers, or external DNS. Devices communicate directly through local radio waves and local routers.
- **Zero-Pairing Hassle**: The application discovers nearby nodes, completes authentication handshakes, negotiates RFCOMM or TCP channels, and exchanges packets autonomously under the hood without sending the user to Android Bluetooth Settings.
- **Multi-Hop Flood Mesh**: Messages destined for peers beyond single-hop radio reach are relayed by intermediate nodes with Time-To-Live (TTL) packet counters and automatic duplicate suppression, ensuring message delivery across crowds, homes, and neighborhoods during outages.
- **Speed-Optimized Split-Routing**: Lightweight text and mesh ACKs travel through resilient Bluetooth and BLE channels; heavy media files (images, videos, APKs) queue for high-bandwidth direct Wi-Fi links.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Onboarding & First Launch**:
   - A 3-step introductory carousel explaining: (1) 100% offline communication without internet, (2) automatic background mesh routing, and (3) zero-pairing convenience.
   - Profile quick-setup: User sets their display nickname and status bio (saved in local Room DB).
2. **Main Navigation (4 Tabs)**:
   - **Chats Tab**: Pinned *Canal General* (community frequency with `/contactos` filtering and `@todos` priority badge), followed by private conversations sorted by recent activity with unread badges, search filter, and message status indicators (Clock 🕒, Single Check ✔️, Double Check Gray ✔️✔️, Blue Check ✔️✔️).
   - **Contacts Tab**:
     - *Radar en Vivo*: Real-time radar visualization displaying actively discovered radio peers with signal strength (RSSI bar), hop distance, and instant `[Agregar]` button.
     - *Mis Contactos*: Saved contacts with custom local nicknames, routing status ("Cerca", "En la red", "Fuera de alcance"), and direct chat launcher.
   - **Calls Tab**: Call history with timestamps, call duration, and quick-call action for direct P2P voice/video sessions.
   - **Settings Tab**: User profile settings, active network telemetry, battery duty-cycle mode switch (Normal 7s vs Saver 17s), theme toggle, and situational help menu.
3. **Situational Help Bottom Sheet**:
   - Organized guides for real-world scenarios: Blackouts/no signal, heavy file transfers, General Channel commands, and connecting multiple homes via home Wi-Fi routers (Nauta Hogar / Local APs).
4. **Emergency Triple-Tap Action (Top Bar)**:
   - Rapidly tapping the title bar 3 times opens the emergency APK distribution sheet featuring the local HTTP server URL, QR code, and native system share intent.

### Visual Identity & Theme
- **Design System**: Material Design 3 (M3) with full `dynamicColorScheme` support for fluid Android wallpaper integration in both Light and Dark themes.
- **Expressive Motion**: Spring-based physics (`spring(stiffness = Spring.StiffnessMediumLow)`), animated card expansions, and pulsing radar sweep animations.
- **Typography & Icons**: Official Material 3 vector icons (`Icons.Default.*`); zero raw emojis in buttons or navigation. Clean hierarchy with distinctive bold headline tokens and readable body typography.
- **Non-Intrusive Polish**: Adheres to a strict zero-popup policy; all confirmations and user alerts utilize inline banners, snackbars, and M3 modal bottom sheets.

---

## 3. Key Product Decisions & Trade-Offs

- **Room Local Persistence Over Cloud Databases**:
  - *Chosen Approach*: Android Room with Kotlin Coroutines and reactive `Flow<List<T>>`.
  - *Why*: BlueChat is strictly offline; Room provides ACID reliability, reactive UI updates, and on-device Dead Drop queue storage.
- **Dual Bluetooth Architecture (BLE + Classic RFCOMM)**:
  - *Chosen Approach*: BLE for lightweight beaconing and fast discovery; seamless fallback to Bluetooth Classic RFCOMM sockets for continuous stream transfers and older hardware compatibility.
  - *Why*: BLE advertising packets are limited in payload size (~31-255 bytes); RFCOMM offers dependable bidirectional socket pipes.
- **Adaptive Battery Duty-Cycle**:
  - *Chosen Approach*: 7-second active scan/advertise intervals in Normal mode; 17-second intervals in Battery Saver mode (< 20% battery or background).
  - *Why*: Continuous Bluetooth scanning exhausts battery; duty cycling preserves multi-hour standby longevity while keeping the mesh alive.
- **Local Embedded HTTP Server for APK Sharing**:
  - *Chosen Approach*: Lightweight multi-threaded local socket HTTP server serving an offline responsive HTML download page and streaming the installed APK directly via `context.applicationInfo.sourceDir`.
  - *Why*: Avoids external internet download portals and allows any nearby device (even without BlueChat installed) to download the APK over local Wi-Fi.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose M3 UI Layer                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │
│  │  Chats Tab   │  │ Contacts Tab │  │  Calls Tab   │  │Settings Tab│  │
│  │ (Canal Gen.) │  │ (Live Radar) │  │(P2P LAN Call)│  │ & Help BS  │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └─────┬──────┘  │
└─────────┼─────────────────┼─────────────────┼────────────────┼─────────┘
          │                 │                 │                │
┌─────────▼─────────────────▼─────────────────▼────────────────▼─────────┐
│                    BlueChat Main ViewModel (MVI / MVVM)                │
└─────────┬────────────────────────────────────────────────────┬─────────┘
          │                                                    │
┌─────────▼──────────────────────────┐   ┌─────────────────────▼─────────┐
│     Local Data Layer (Room)        │   │   Mesh & Connectivity Engine  │
│  ┌──────────────────────────────┐  │   │  ┌──────────────────────────┐ │
│  │ UserEntity & UserDao         │  │   │  │ BLE Scanner / Advertiser │ │
│  ├──────────────────────────────┤  │   │  ├──────────────────────────┤ │
│  │ ChatEntity & ChatDao         │  │   │  │ BT Classic RFCOMM Server │ │
│  ├──────────────────────────────┤  │   │  ├──────────────────────────┤ │
│  │ MessageEntity & MessageDao   │  │   │  │ Wi-Fi Router UDP Bridge  │ │
│  ├──────────────────────────────┤  │   │  ├──────────────────────────┤ │
│  │ DeadDropEntity & DeadDropDao │  │   │  │ Direct TCP Stream Socket │ │
│  └──────────────────────────────┘  │   │  └──────────────────────────┘ │
└────────────────────────────────────┘   └───────────────────────────────┘
                   │                                     │
┌──────────────────▼─────────────────────────────────────▼───────────────┐
│             Android System Foreground Service (Doze Prevention)        │
│    + Local Embedded HTTP Server (Port 8080 APK Distribution & QR)      │
└────────────────────────────────────────────────────────────────────────┘
```

### Core Entities & Persistence Schema

1. **UserEntity**:
   - `peerId: String` (Primary Key, unique hardware/crypto identifier)
   - `customNickname: String`
   - `statusBio: String`
   - `isSaved: Boolean` (saved contact vs transiently discovered)
   - `lastSeen: Long`
   - `routingHops: Int`
   - `avatarColorIndex: Int`
2. **ChatEntity**:
   - `chatId: String` (Primary Key, peerId or "general_channel")
   - `peerId: String`
   - `unreadCount: Int`
   - `lastMessageTimestamp: Long`
   - `lastMessageSnippet: String`
3. **MessageEntity**:
   - `messageId: String` (Primary Key, UUID)
   - `chatId: String`
   - `senderId: String`
   - `recipientId: String`
   - `textPayload: String`
   - `timestamp: Long`
   - `status: Int` (0 = SAVED/CLOCK, 1 = SENT/SINGLE_CHECK, 2 = DELIVERED/DOUBLE_GRAY, 3 = READ/DOUBLE_BLUE)
   - `isGeneralChannel: Boolean`
   - `isContactsOnly: Boolean` (`/contactos` flag)
   - `isPriority: Boolean` (`@todos` flag)
   - `mediaUri: String?`
   - `mediaPendingWifi: Boolean`
4. **DeadDropEntity**:
   - `packetId: String` (Primary Key)
   - `destinationId: String`
   - `ttl: Int`
   - `rawPayload: String`
   - `storedAt: Long`

### Mesh Packet Structure & Protocols

```kotlin
data class MeshPacket(
    val packetId: String,
    val senderId: String,
    val senderNickname: String,
    val recipientId: String, // "ALL" for broadcast, or target peerId
    val type: Int,           // 1=TEXT, 2=ACK_DELIVERY, 3=ACK_READ, 4=PING, 5=FILE_HEADER
    val payload: String,
    val ttl: Int = 7,        // Max 20 for @todos
    val isContactsOnly: Boolean = false,
    val isPriority: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
```

---

## 5. Implementation Strategy & Next Steps

Upon your approval to proceed, execution will follow these steps:
1. Update `metadata.json` and `strings.xml` with the official title "BlueChat".
2. Configure AndroidManifest.xml with all required Bluetooth, Wi-Fi, Camera, Audio, and Foreground Service permissions.
3. Build the Room persistence database (entities, DAOs, repository).
4. Implement the Multi-Layer Connectivity Engine:
   - BLE Advertiser and Scanner with 7s/17s duty cycle
   - Bluetooth Classic RFCOMM server/client socket listener
   - Wi-Fi Router UDP broadcast & LAN TCP socket communicator
   - Mesh packet router with duplicate filter, TTL decrement, and ACK handling
5. Implement the Embedded Offline HTTP APK Server & QR Code generator.
6. Build the P2P LAN Voice & Video call engine with active call UI.
7. Implement the Material 3 Expressive UI layer (4 tabs, WhatsApp-style chat bubbles, live radar screen, emergency bottom sheet, situational help guides).
8. Verify end-to-end compilation with `compile_applet`.
