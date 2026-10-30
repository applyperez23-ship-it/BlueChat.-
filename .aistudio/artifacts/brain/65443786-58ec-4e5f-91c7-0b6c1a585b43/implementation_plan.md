# BlueChat - Modernización UI/UX Material 3 Expressive & Enrutamiento Híbrido

Actualización integral de **BlueChat** enfocada en elevar la experiencia de usuario bajo los principios de **Material 3 Expressive (M3E)**, simplificando el lenguaje para usuarios no técnicos, renovando la identidad visual con un nuevo icono vectorial squircle, agregando detección y activación fluida de radios (Bluetooth y Wi-Fi), e implementando un enrutamiento híbrido automático (Routers LAN, Puntos de acceso Hotspot, Wi-Fi Direct y BLE Store-and-Forward).

---

### User Review & Critical Decisions

> [!IMPORTANT]
> Decisiones confirmadas a través del proceso interactivo de aclaración:
> - **Indicadores de Canales Activos**: Píldora interactiva y expandible en la barra superior (`TopAppBar`) que muestra de forma discreta y elegante los iconos de estado de cada radio (Bluetooth, Wi-Fi Local/Router, Wi-Fi Direct y Zona Wi-Fi).
> - **Aviso de Activación de Conexiones**: Banner superior integrado, limpio y amigable con botones de un solo toque ("Activar Bluetooth" / "Activar Wi-Fi") cuando los radios estén apagados, permitiendo encenderlos sin salir a los ajustes complejos del sistema.
> - **Llamadas sobre Bluetooth**: Si se inicia una llamada de voz/video y los dispositivos sólo tienen enlace Bluetooth, la app ofrece una guía con un solo toque para activar Zona Wi-Fi o handshake Wi-Fi Direct con emparejamiento automático.

---

## 1. Overview & Core Concept

- **Lenguaje Cotidiano y Cero Fricción**: Reemplazo total de términos técnicos por frases claras y humanas:
  - En lugar de "Buscando beacons BLE / Escaneo RSSI": *"Buscando celulares cercanos"*.
  - En lugar de "Enrutando por socket TCP/UDP LAN": *"Enviando por red local"*.
  - En lugar de "Store-and-forward packet buffered": *"Mensaje en camino"*.
  - En lugar de "Nodos mesh alcanzables": *"Personas conectadas"*.
- **Icono Vectorial de Aplicación (`ic_launcher_bluechat.xml`)**:
  - Diseño vectorial M3E con una burbuja de chat orgánica integrada con el glifo de Bluetooth luminoso en su núcleo, enmarcado sobre un squircle con bordes suavizados y colores corporativos (Azul Eléctrico `#0070F3`, Cian Neón `#00D2FF` y Azul Medianoche `#0A192F`).
- **Enrutamiento Híbrido Inteligente**:
  - Detección automática en segundo plano de: (a) Routers domésticos/oficina sin internet (ej. Nauta Hogar), (b) Hotspots/Puntos de acceso compartidos entre dispositivos, (c) Wi-Fi Direct de alta velocidad, y (d) Bluetooth Low Energy con almacenamiento en base de datos local (Buzón Ciego / Dead Drop) para retransmisión por saltos.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Apertura de la App y Detección de Conexión**:
   - Al abrir BlueChat, un monitor reactivo revisa si Bluetooth y Wi-Fi están activos.
   - Si alguno está apagado, aparece un banner sutil con esquinas redondeadas (20dp) en la parte superior: *"Para enviar mensajes sin internet necesitamos Bluetooth y Wi-Fi encendidos"* con botones directos `[Activar]`.
2. **Pantalla Principal (Chats)**:
   - Tarjetas flotantes con esquinas ultra-redondeadas de **28dp** (`RoundedCornerShape(28.dp)`), elevación tonal suave (`surfaceContainerHigh`), y micro-interacciones de pulsación suave.
   - Píldora superior expandible que revela el estado en tiempo real de los 4 canales de comunicación con texto claro:
     - 🔵 *Bluetooth*: "Buscando celulares alrededor"
     - 🟢 *Wi-Fi Local*: "Conectado al router casero (Velocidad máxima)"
     - 🟣 *Wi-Fi Direct*: "Transferencia directa lista"
     - 🟠 *Zona Wi-Fi*: "Punto de acceso activo"
3. **Conversación y Estados de Mensajes**:
   - Burbujas de chat de bordes amplios y suaves.
   - Subtítulos de estado sin jerga: *"Guardado en tu celular"*, *"Enviado a la red"*, *"Entregado al celular"*, *"Leído"*.
   - Botón directo de nota de voz rápida con visualización de ondas sonoras y botón de envío instantáneo.
4. **Llamadas y Asistente de Conexión Rápida**:
   - Al tocar llamar a un contacto conectado sólo por Bluetooth, la app muestra un cuadro de diálogo/tarjeta M3E: *"Para hablar en vivo necesitamos alta velocidad. ¿Deseas activar la Zona Wi-Fi o Wi-Fi Direct con 1 toque?"*, estableciendo la conexión sin configuraciones manuales.

---

## 3. Key Product Decisions & Trade-Offs

- **Nuevo Recurso Vectorial `ic_launcher_bluechat.xml` vs Rasterizado**:
  - *Enfoque*: Creación de un vector nativo XML de alta fidelidad con degradados radiales/lineales, compatible con todos los launchers modernos de Android 12+ (Adaptive Icons) y densidad independiente, garantizando nitidez perfecta.
- **Tarjetas Flotantes con `RoundedCornerShape(28.dp)`**:
  - *Enfoque*: Actualizar la estructura visual de las listas en `ChatsScreen`, `ContactsScreen`, `CallsScreen` y `SettingsScreen` adoptando contenedores suaves M3 (`surfaceContainer` y `surfaceContainerHigh`) para eliminar la sensación rígida tradicional y ofrecer una estética fluida y orgánica.
- **Banner de Activación No Invasivo**:
  - *Enfoque*: Usar `AnimatedVisibility` para mostrar el banner de solicitud de activación sólo cuando un radio esté apagado, permitiendo descartarlo y lanzando los intents del sistema estándar (`BluetoothAdapter.ACTION_REQUEST_ENABLE` y ajustes de Wi-Fi / Panel de Internet) para una experiencia sin fricción.

---

## 4. Technical Architecture & Data Strategy

### Component Hierarchy & Interaction Flow

```
┌────────────────────────────────────────────────────────────────────────┐
│                   TopAppBar (Material 3 Expressive)                    │
│  ┌──────────────────────┐  ┌────────────────────────────────────────┐  │
│  │ BlueChat Title       │  │ Expandable Radio Pill: [BT][WiFi][LAN] │  │
│  └──────────────────────┘  └────────────────────────────────────────┘  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│ Connection Prompt Banner (AnimatedVisibility if Bluetooth/Wi-Fi off)   │
│ "Enciende Bluetooth y Wi-Fi para encontrar celulares cercanos" [Activar]│
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│                       Floating Content Cards (28dp)                    │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Canal General (Tarjeta Flotante M3E con degradado sutil)         │  │
│  ├──────────────────────────────────────────────────────────────────┤  │
│  │ Conversaciones Privadas con Estados Claros ("Mensaje en camino") │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│                        Hybrid Mesh Routing Engine                      │
│   ┌───────────────┐ ┌────────────────┐ ┌───────────────┐ ┌──────────┐  │
│   │ Wi-Fi Router  │ │ Hotspot Detect │ │ Wi-Fi Direct  │ │ BLE Mesh │  │
│   │ UDP Broadcast │ │ Passive/Socket │ │ Fast Transfer │ │ DeadDrop │  │
│   └───────────────┘ └────────────────┘ └───────────────┘ └──────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### Plan de Archivos a Modificar / Crear

1. **Recurso Vectorial de Icono (`app/src/main/res/drawable/ic_launcher_bluechat.xml`)**:
   - Crear el vector drawable con la burbuja estilizada y el glifo Bluetooth en su interior sobre fondo squircle.
   - Actualizar `ic_launcher_foreground.xml` e iconos adaptativos para integrarlo.
2. **Capa de Conectividad (`MeshEngine.kt` y `WifiLanBridge.kt`)**:
   - Exponer un flujo de estado reactivo `StateFlow<NetworkChannelsState>` con el estado de Bluetooth, Wi-Fi Local, Hotspot y Wi-Fi Direct.
   - Reforzar la negociación automática de socket para llamadas y archivos.
3. **Capa de ViewModel (`MainViewModel.kt`)**:
   - Integrar detección en tiempo real del estado de Bluetooth y Wi-Fi en el dispositivo.
   - Exponer intents y callbacks para solicitar activación rápida de radios.
   - Simplificar los textos de estado expuestos a la interfaz.
4. **Capa UI y Componentes M3E**:
   - `ChatsScreen.kt`: Tarjetas flotantes redondeadas con `RoundedCornerShape(28.dp)`, contenedor M3 suave, y estados en lenguaje humano.
   - `MainActivity.kt`: Integración de la píldora expandible de canales en la barra superior y banner animado de activación automática.
   - `ChatDetailScreen.kt`: Actualizar estados de burbujas a lenguaje cotidiano y mejorar la integración del asistente para llamadas directas.
5. **Verificación y Compilación**:
   - Ejecutar `compile_applet` y pruebas unitarias con Robolectric para asegurar cero errores de compilación y compatibilidad total.
