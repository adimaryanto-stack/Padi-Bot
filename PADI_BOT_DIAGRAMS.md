# PadiBot System & Flow Diagrams
## Mermaid Diagrams untuk Architecture, User Journey, & State Management

---

## 📊 Diagram 1: User Journey - Complete Mission Flow

```mermaid
graph TD
    A["🏠 Open App<br/>Dashboard Screen"] --> B{Select Field<br/>or Create New?}
    B -->|Existing Field| C["📋 View Field<br/>List & Select"]
    B -->|New Field| D["➕ Create New Field<br/>Input Boundary"]
    C --> E["⚙️ Planting Settings<br/>Enter Machine Width<br/>& Headland Width"]
    D --> E
    E --> F["✨ Generate Route<br/>Calculate Boustrophedon<br/>Pattern"]
    F --> G["👁️ Route Preview<br/>Visualize Lanes<br/>& Coverage %"]
    G --> H{Approve Route?}
    H -->|No| E
    H -->|Yes| I["🎯 Mission Execution<br/>Ready to Start"]
    I --> J["▶️ Start Mission<br/>Send to Machine"]
    J --> K["📡 Real-Time Monitoring<br/>Track Progress<br/>& Telemetry"]
    K --> L{Mission State?}
    L -->|Pause| M["⏸️ Paused<br/>Machine Stops"]
    L -->|Resume| N["▶️ Resume<br/>Continue from<br/>Last Position"]
    L -->|Complete| O["✅ Mission Complete<br/>Show Summary"]
    L -->|Stop/Error| P["❌ Mission Stopped<br/>Show Error Log"]
    M --> M
    N --> K
    O --> Q["📊 Mission Report<br/>Coverage %, Time,<br/>Battery Used"]
    P --> Q
    Q --> R["💾 Save to History<br/>Store Data & Telemetry"]
    R --> S["🏠 Return to Dashboard"]
    S --> A
```

---

## 🏗️ Diagram 2: System Architecture

```mermaid
graph TB
    subgraph Mobile["📱 Mobile App (React Native + Expo)"]
        UI["UI Screens<br/>Dashboard, Settings,<br/>Mission Control"]
        Nav["🧭 Navigation<br/>Stack & Tab"]
        State["🗂️ Zustand Store<br/>Global State"]
        Services["🔧 Services<br/>API Client,<br/>Storage"]
    end
    
    subgraph Backend["🖥️ Backend (Node.js + Express)"]
        API["🔌 REST API<br/>Fields, Missions,<br/>Telemetry Endpoints"]
        Auth["🔐 Authentication<br/>JWT Sessions"]
        Db["🗄️ Database<br/>PostgreSQL + PostGIS"]
        Routes["🛣️ Route Planner<br/>Service"]
    end
    
    subgraph Hardware["⚙️ Hardware Layer"]
        ESP["🎛️ ESP32 Controller<br/>Motor Control,<br/>Sensor Reading"]
        Motors["⚡ Motors & Actuators<br/>Drive, Steering,<br/>Planting"]
        Sensors["📍 Sensors<br/>GNSS, IMU,<br/>Ultrasonics"]
    end
    
    subgraph External["☁️ External Services"]
        Cloud["☁️ Cloud Storage<br/>Mission Backups"]
        Notif["🔔 Notifications<br/>Push Alerts"]
    end
    
    Mobile -->|HTTP/REST| Backend
    Backend -->|Bluetooth/WiFi<br/>JSON-RPC| Hardware
    Backend -->|CRUD| Db
    Backend -->|Route<br/>Calculation| Routes
    Mobile -->|Local<br/>Storage| Services
    Backend -->|Upload<br/>Data| Cloud
    Backend -->|Send| Notif
    
    style Mobile fill:#e1f5ff
    style Backend fill:#f3e5f5
    style Hardware fill:#fff3e0
    style External fill:#f1f8e9
```

---

## 🔄 Diagram 3: Mission State Machine

```mermaid
stateDiagram-v2
    [*] --> DRAFT
    
    DRAFT --> READY: Approve Mission
    DRAFT --> [*]: Cancel/Delete
    
    READY --> RUNNING: Start Mission
    READY --> DRAFT: Edit Settings
    READY --> [*]: Delete
    
    RUNNING --> PAUSED: Pause Button
    RUNNING --> STOPPED: Stop Button
    RUNNING --> ERROR: Connection Loss<br/>Sensor Failure<br/>Low Battery
    RUNNING --> COMPLETED: Reached Final WP
    
    PAUSED --> RUNNING: Resume Button
    PAUSED --> STOPPED: Stop Button
    PAUSED --> ERROR: Connection Loss
    
    COMPLETED --> [*]
    STOPPED --> [*]
    ERROR --> [*]: Acknowledge
    
    note right of DRAFT
        Mission created
        Not yet approved
    end note
    
    note right of READY
        Mission approved
        Waiting to start
    end note
    
    note right of RUNNING
        Machine actively
        planting in field
    end note
    
    note right of PAUSED
        Machine paused
        at current position
    end note
    
    note right of COMPLETED
        All waypoints
        completed successfully
    end note
    
    note right of ERROR
        Error condition
        requires human intervention
    end note
```

---

## 📡 Diagram 4: Real-Time Data Flow During Mission Execution

```mermaid
sequenceDiagram
    participant User as 👤 User
    participant App as 📱 Mobile App
    participant Backend as 🖥️ Backend
    participant Machine as ⚙️ Machine
    
    User->>App: Tap "Start Mission"
    activate App
    
    App->>Backend: POST /api/missions/:id/start
    activate Backend
    Backend->>Db: Update mission status = RUNNING
    Backend-->>App: {success: true, mission}
    deactivate Backend
    
    App->>Machine: uploadMission() via Bluetooth
    activate Machine
    Machine-->>App: Mission received & stored
    deactivate Machine
    
    App->>Machine: startMission() command
    activate Machine
    Machine->>Machine: Start moving towards WP[0]
    
    loop Every 1 second
        Machine->>Machine: Read sensors
        Machine->>Machine: Calculate position
        Machine->>App: Telemetry {position, battery, speed, progress}
        activate App
        App->>App: Update UI with telemetry
        App->>Backend: POST /api/missions/:id/telemetry
        activate Backend
        Backend->>Db: Store telemetry point
        Backend-->>App: {recorded: true}
        deactivate Backend
        App->>User: Display updated progress
        deactivate App
    end
    
    Note over Machine: Progress: 50% -> 100%
    Machine->>Machine: Reached final waypoint
    Machine->>App: COMPLETED signal
    
    activate App
    App->>Backend: POST /api/missions/:id/stop (with status=COMPLETED)
    activate Backend
    Backend->>Db: Update mission status = COMPLETED
    Backend-->>App: {success: true}
    deactivate Backend
    App->>User: Show completion summary
    deactivate App
    
    deactivate Machine
```

---

## 🗺️ Diagram 5: Route Generation Process

```mermaid
graph TD
    A["📍 Input:<br/>Field Boundary<br/>Machine Width<br/>Headland Width<br/>Orientation"] --> B["1️⃣ Validate Field<br/>Check polygon validity<br/>Calculate area<br/>Ensure min size"]
    
    B --> C{Valid?}
    C -->|No| D["❌ Error<br/>Invalid field"]
    C -->|Yes| E["2️⃣ Calculate Effective<br/>Working Area<br/>Offset boundary<br/>by headland width"]
    
    E --> F["3️⃣ Generate Parallel<br/>Lines<br/>Divide into lanes<br/>spaced by machine width<br/>at orientation angle"]
    
    F --> G["4️⃣ Clip Lines to<br/>Field Boundary<br/>Keep only points<br/>inside polygon"]
    
    G --> H["5️⃣ Generate Waypoints<br/>Create waypoint sequence<br/>Zig-zag order<br/>Add turn positions"]
    
    H --> I["6️⃣ Calculate Metrics<br/>Total distance<br/>Number of lanes<br/>Coverage %<br/>Uncovered areas"]
    
    I --> J["✅ Output:<br/>Waypoints Array<br/>Metrics<br/>Visualization Data"]
    
    D --> K["Show error to user<br/>Suggest adjustments"]
    
    style A fill:#c8e6c9
    style J fill:#c8e6c9
    style D fill:#ffcdd2
    style K fill:#ffcdd2
```

---

## 🔌 Diagram 6: Machine Connection Abstraction

```mermaid
graph TB
    App["📱 Mobile App"]
    
    Interface["MachineConnection<br/>Interface"]
    
    Simulator["SimulatorMachine<br/>Connection"]
    BT["BluetoothMachine<br/>Connection"]
    WiFi["WiFiMachine<br/>Connection"]
    GSM["GSMCloud<br/>Connection"]
    
    ESP["🎛️ ESP32<br/>Controller"]
    Motors["⚡ Motors"]
    Sensors["📍 Sensors"]
    Cloud["☁️ Cloud"]
    
    App -->|Uses| Interface
    
    Interface -.->|Implemented by| Simulator
    Interface -.->|Implemented by| BT
    Interface -.->|Implemented by| WiFi
    Interface -.->|Implemented by| GSM
    
    Simulator -->|Virtual| Motors
    Simulator -->|Simulated| Sensors
    
    BT -->|Short Range<br/>Low Latency| ESP
    WiFi -->|Local Network| ESP
    GSM -->|Remote| Cloud
    
    ESP -->|Control| Motors
    ESP -->|Read| Sensors
    Cloud -->|Store| Cloud
    
    note right of Interface
        Abstract interface
        isolates app from
        specific protocol
    end note
    
    note right of Simulator
        MVP: Development
        & testing without
        hardware
    end note
    
    style Interface fill:#bbdefb
    style Simulator fill:#c8e6c9
    style BT fill:#ffe0b2
    style WiFi fill:#ffe0b2
    style GSM fill:#ffe0b2
```

---

## 📊 Diagram 7: Telemetry Data Pipeline

```mermaid
graph LR
    A["⚙️ Machine<br/>GNSS/IMU/Sensors"] -->|Read sensors<br/>100ms interval| B["🎛️ ESP32<br/>Data Collection"]
    
    B -->|Process &<br/>Aggregate| C["📡 Machine<br/>Telemetry Packet<br/>{position, battery,<br/>speed, progress}"]
    
    C -->|Bluetooth/WiFi<br/>1 Hz| D["📱 Mobile App<br/>Receive &<br/>Parse"]
    
    D -->|Update UI| E["👁️ Display<br/>Real-time<br/>Progress"]
    
    D -->|Send to<br/>Backend| F["🖥️ Backend<br/>API"]
    
    F -->|Store<br/>Record| G["🗄️ Database<br/>telemetry_points<br/>table"]
    
    G -->|Query &<br/>Visualize| H["📊 Analytics<br/>Coverage %<br/>Performance<br/>Metrics"]
    
    G -->|Export| I["📥 Reports<br/>Mission Summary<br/>Historical Data"]
    
    style A fill:#fff3e0
    style D fill:#e1f5ff
    style G fill:#f3e5f5
    style H fill:#c8e6c9
```

---

## 🎯 Diagram 8: Feature Dependency Graph

```mermaid
graph TD
    F0["Field Mapping<br/>(Manual Boundary)"]
    F1["Route Generation<br/>(Boustrophedon)"]
    F2["Route Preview"]
    F3["Mission Control<br/>(Start/Pause/Resume)"]
    F4["Machine Connection<br/>(Simulator MVP)"]
    F5["Telemetry Monitoring"]
    F6["Manual Control Mode"]
    F7["Mission History"]
    F8["Emergency Stop"]
    
    F0 --> F1
    F0 --> F7
    F1 --> F2
    F2 --> F3
    F4 --> F3
    F4 --> F5
    F3 --> F5
    F3 --> F8
    F5 --> F7
    F6 --> F4
    
    style F0 fill:#c8e6c9
    style F1 fill:#c8e6c9
    style F4 fill:#c8e6c9
    style F8 fill:#ffcdd2
    
    Note1["✅ MVP Requirements<br/>Must complete"]
    Note2["⚠️ Critical<br/>Safety Feature"]
    
    linkStyle 0,1,2,3,4,5,6,7,8 stroke:#333
```

---

## 🚀 Diagram 9: Sprint Development Timeline

```mermaid
gantt
    title PadiBot MVP Development Timeline (6 weeks)
    
    section Sprint 1
    Project Setup :s1_1, 0d, 2d
    Route Planner :s1_2, after s1_1, 3d
    Machine Protocol :s1_3, after s1_2, 2d
    Backend Skeleton :s1_4, after s1_3, 2d
    
    section Sprint 2
    Navigation Setup :s2_1, 9d, 2d
    Dashboard Screen :s2_2, after s2_1, 2d
    Field Screens :s2_3, after s2_2, 2d
    Settings Form :s2_4, after s2_3, 2d
    History Screen :s2_5, after s2_4, 2d
    
    section Sprint 3
    Route Preview :s3_1, 19d, 2d
    Mission Execution :s3_2, after s3_1, 4d
    Simulator :s3_3, after s3_2, 4d
    Telemetry Logging :s3_4, after s3_3, 2d
    Backend Endpoints :s3_5, after s3_4, 3d
    
    section Sprint 4
    API Client Setup :s4_1, 32d, 1d
    Frontend-Backend Integration :s4_2, after s4_1, 3d
    Manual Control :s4_3, after s4_2, 2d
    Settings & i18n :s4_4, after s4_3, 1d
    UI Polish :s4_5, after s4_4, 2d
    
    section Sprint 5
    Route Tests :s5_1, 41d, 2d
    State Machine Tests :s5_2, after s5_1, 2d
    API Tests :s5_3, after s5_2, 2d
    E2E Testing :s5_4, after s5_3, 2d
    Documentation :s5_5, after s5_4, 2d
    Release :s5_6, after s5_5, 1d
```

---

## 🎨 Diagram 10: Screen Navigation Flow

```mermaid
graph TB
    Splash["🎬 Splash Screen"]
    
    Dashboard["🏠 Dashboard<br/>Machine Status<br/>Current Field<br/>Quick Actions"]
    
    FieldList["📋 Field List<br/>All Fields<br/>Edit/Delete"]
    FieldDetail["🔍 Field Detail<br/>Boundary Map<br/>Missions"]
    FieldCreate["➕ Create Field<br/>Polygon Input"]
    
    Settings["⚙️ Planting Settings<br/>Machine Width<br/>Headland Width<br/>Orientation"]
    
    RoutePreview["👁️ Route Preview<br/>Lane Visualization<br/>Coverage %"]
    
    Mission["🎯 Mission Execution<br/>Real-Time Progress<br/>Telemetry<br/>Controls"]
    
    History["📊 Mission History<br/>Past Missions<br/>Reports"]
    
    Settings2["⚙️ App Settings<br/>Debug Mode<br/>Clear Cache"]
    
    ManualControl["🕹️ Manual Control<br/>D-Pad Controls<br/>Speed Slider"]
    
    Splash --> Dashboard
    
    Dashboard -->|Tab| FieldList
    Dashboard -->|Tab| History
    Dashboard -->|Tab| Settings2
    Dashboard -->|New Mission| Settings
    Dashboard -->|Manual| ManualControl
    
    FieldList --> FieldDetail
    FieldList -->|Add| FieldCreate
    FieldCreate --> Dashboard
    
    FieldDetail -->|Edit| FieldCreate
    FieldDetail -->|New Mission| Settings
    
    Settings -->|Generate| RoutePreview
    Settings -->|Back| Dashboard
    
    RoutePreview -->|Approve| Mission
    RoutePreview -->|Edit| Settings
    
    Mission -->|Complete| History
    Mission -->|Stop| Dashboard
    
    History -->|Back| Dashboard
    
    ManualControl -->|Back| Dashboard
    
    Settings2 -->|Back| Dashboard
    
    style Splash fill:#bbdefb
    style Dashboard fill:#c8e6c9
    style Mission fill:#ffe0b2
    style ManualControl fill:#ffcdd2
```

---

## 🔐 Diagram 11: Authentication & Authorization Flow

```mermaid
sequenceDiagram
    participant User as 👤 User
    participant App as 📱 App
    participant Auth as 🔐 Auth Service
    participant API as 🔌 API
    participant DB as 🗄️ Database
    
    User->>App: Open app / Login
    activate App
    
    App->>Auth: POST /api/auth/login {email, password}
    activate Auth
    
    Auth->>Auth: Validate credentials
    Auth->>DB: Query user by email
    activate DB
    DB-->>Auth: User record
    deactivate DB
    
    Auth->>Auth: Hash check password
    
    alt Password Valid
        Auth->>Auth: Generate JWT token
        Auth-->>App: {token, user, permissions}
        App->>App: Store token in localStorage
        App->>App: Navigate to Dashboard
    else Password Invalid
        Auth-->>App: {error: "Invalid credentials"}
        App->>User: Show error message
    end
    
    deactivate Auth
    
    Note over App: User is now Authenticated
    
    User->>App: Make API request (e.g., fetch fields)
    App->>API: GET /api/fields with Authorization header
    activate API
    
    API->>API: Verify JWT token
    API->>API: Extract user ID from token
    API->>DB: Query fields for user
    activate DB
    DB-->>API: Fields data
    deactivate DB
    
    API-->>App: {fields: [...]}
    deactivate API
    
    App->>User: Display fields
    
    deactivate App
```

---

## 📝 Diagram 12: Error Handling & Recovery Flow

```mermaid
graph TD
    A["🔴 Error Occurs<br/>Connection Loss?<br/>Sensor Failure?<br/>Invalid State?"] --> B{Error Type?}
    
    B -->|Connection Loss| C["📡 Connection Lost<br/>Last known position<br/>stored"]
    B -->|Low Battery| D["🔋 Low Battery<br/><20%"]
    B -->|GPS Loss| E["📍 GPS Unavailable<br/>Use last fix +<br/>dead reckoning"]
    B -->|Invalid State| F["⚠️ State Error<br/>Invalid transition"]
    
    C --> G["⏸️ Auto-Pause Mission<br/>Stop machine<br/>Wait for reconnection"]
    D --> H["⚠️ Alert User<br/>Suggest return<br/>to start point"]
    E --> I["⚠️ Degraded Mode<br/>Reduce accuracy<br/>requirement"]
    F --> J["❌ Reject Action<br/>Show valid options"]
    
    G --> K{Recovery?}
    H --> K
    I --> K
    J --> K
    
    K -->|User Resumes| L["▶️ Resume Mission<br/>From last waypoint"]
    K -->|User Stops| M["🛑 Stop Mission<br/>Save state<br/>Log error"]
    K -->|Auto-Timeout<br/>5 min| N["⏱️ Auto Stop<br/>Save telemetry<br/>Mark as ERROR"]
    
    L --> O["✅ Continue Operation"]
    M --> P["💾 Save to History"]
    N --> P
    O --> Q["📊 Mission Complete"]
    P --> Q
    
    style A fill:#ffcdd2
    style G fill:#fff3e0
    style H fill:#fff3e0
    style L fill:#c8e6c9
    style Q fill:#c8e6c9
```

---

## 🧪 Diagram 13: MVP Testing Strategy

```mermaid
graph TD
    A["🧪 Testing Pyramid"]
    
    A --> L1["🏠 Unit Tests<br/>Route Planner<br/>State Machine<br/>Calculations<br/>Coverage: 80%+"]
    
    A --> L2["🔗 Integration Tests<br/>API Endpoints<br/>Database CRUD<br/>Machine Protocol<br/>Message Format"]
    
    A --> L3["🎯 E2E Tests<br/>Complete User Journey<br/>Field → Route → Mission<br/>Manual Testing<br/>Simulator Scenarios"]
    
    L1 --> Tools1["Tools:<br/>Jest<br/>React Native<br/>Testing Library"]
    
    L2 --> Tools2["Tools:<br/>Supertest<br/>PostgreSQL<br/>Test DB"]
    
    L3 --> Tools3["Tools:<br/>Manual Testing<br/>Device Testing<br/>Simulator<br/>Checklist"]
    
    Tools1 --> Exec["⚡ Execution"]
    Tools2 --> Exec
    Tools3 --> Exec
    
    Exec --> Report["📊 Test Report<br/>Coverage %<br/>Pass/Fail<br/>Performance"]
    
    Report --> Decision{All Passing?}
    Decision -->|Yes| Release["🚀 Release v0.1.0"]
    Decision -->|No| Debug["🐛 Debug & Fix"]
    Debug --> Exec
    
    style L1 fill:#e1f5ff
    style L2 fill:#f3e5f5
    style L3 fill:#ffe0b2
    style Release fill:#c8e6c9
```

---

Generated for **PadiBot v1.1 PRD**  
Created: August 27, 2026  
Format: Mermaid Diagrams (mdx compatible)
