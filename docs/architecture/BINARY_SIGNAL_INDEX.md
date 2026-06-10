# BINARY SIGNAL INDEX

**Subsistema**: Core / Signaling
**Tecnología**: 64-bit Packed Signals
**Estado**: V2.0 Standard Specification

---

## 1. 64-Bit Signal Architecture

The signaling system enforces strictly aligned 64-bit data structures to ensure atomic access on x86_64 hardware without locking mechanisms (Lock-Free).

### 1.1. Standard Signal (Command + Payload)
```
[ 63 .................... 32 ] [ 31 ..................... 0 ]
|       COMMAND ID (32b)     |        PAYLOAD (32b)         |
```
*   **Command ID**: 32-bit Operation Identifier.
*   **Payload**: 32-bit Immediate Value or Bitfield.

### 1.2. Vector Signal (Float32x2)
```
[ 63 .................... 32 ] [ 31 ..................... 0 ]
|       FLOAT X (32b)        |        FLOAT Y (32b)         |
```
*   **Use Case**: Physics integration, Velocity vectors.
*   **Precision**: IEEE 754 (Full Precision).

### 1.3. Spatial Signal (Packed Coordinate)
```
[ 63 ........ 48 ] [ 47 ........ 32 ] [ 31 ..................... 0 ]
|   X (16b)      |    Y (16b)       |          Z (32b)             |
```
*   **Use Case**: High-density orbital telemetry.
*   **Ranges**: X/Y (Int16), Z (Int32).

---

## 2. Command Set Architecture (CSA)

Reference mapping for `DarkSignalCommands`.

### 2.1. INPUT (0x1000 - 0x1FFF)
| OpCode | Mnemonic | Payload | Technical Description |
| :--- | :--- | :--- | :--- |
| `0x1001` | `INPUT_KEY_DOWN` | `KeyCode` | Key Press Event. |
| `0x1002` | `INPUT_KEY_UP` | `KeyCode` | Key Release Event. |
| `0x1003` | `INPUT_MOUSE_MOVE` | `Packed XY` | Mouse Delta Vector. |
| `0x1004` | `INPUT_MOUSE_CLICK` | `ButtonID` | Mouse Button Trigger. |
| `0x1005` | `INPUT_GAMEPAD_BUTTON` | `ButtonID` | Gamepad Input Trigger. |

### 2.2. NETWORK (0x2000 - 0x2FFF)
| OpCode | Mnemonic | Payload | Technical Description |
| :--- | :--- | :--- | :--- |
| `0x2001` | `NET_SYNC_ENTITY` | `EntityID` | State Synchronization Vector. |
| `0x2002` | `NET_PACKET_RECEIVED` | `PacketID` | Binary Packet Ingress. |
| `0x2003` | `NET_CONN_ESTABLISHED`| `ClientID` | Socket Connection Handshake. |
| `0x2004` | `NET_CONN_LOST` | `ClientID` | Socket Termination. |

### 2.3. SYSTEM (0x3000 - 0x3FFF)
| OpCode | Mnemonic | Payload | Technical Description |
| :--- | :--- | :--- | :--- |
| `0x3001` | `SYS_ENTITY_SPAWN` | `TypeID` | Entity Instantiation. |
| `0x3002` | `SYS_ENTITY_DESTROY` | `EntityID` | Entity Deallocation. |
| `0x3003` | `SYS_ENTITY_MOVE` | `EntityID` | Position Update Vector. |
| `0x3100` | `SYS_ENGINE_PAUSE` | `0` | Kernel Loop Suspend. |
| `0x3101` | `SYS_ENGINE_RESUME` | `0` | Kernel Loop Resume. |
| `0x3102` | `SYS_ENGINE_SHUTDOWN` | `ExitCode` | Immediate Termination. |

### 2.4. AUDIO (0x4000 - 0x4FFF)
| OpCode | Mnemonic | Payload | Technical Description |
| :--- | :--- | :--- | :--- |
| `0x4001` | `AUDIO_PLAY_SOUND` | `SoundID` | Audio Buffer Playback. |
| `0x4002` | `AUDIO_STOP_SOUND` | `SoundID` | Audio Buffer Halt. |
| `0x4003` | `AUDIO_SET_VOLUME` | `0-100` | Global Gain Control. |

### 2.5. PHYSICS (0x5000 - 0x5FFF)
| OpCode | Mnemonic | Payload | Technical Description |
| :--- | :--- | :--- | :--- |
| `0x5001` | `PHYS_APPLY_FORCE` | `EntityID` | Force Vector Application. |
| `0x5002` | `PHYS_COLLISION` | `EntityID` | AABB Overlap Event. |
| `0x5003` | `PHYS_SET_GRAVITY` | `Value` | Global Acceleration Constant. |

### 2.6. RENDER (0x6000 - 0x6FFF)
| OpCode | Mnemonic | Payload | Technical Description |
| :--- | :--- | :--- | :--- |
| `0x6001` | `RENDER_SET_SHADER` | `ShaderID` | Pipeline State Change. |
| `0x6002` | `RENDER_UPDATE_TEX` | `TextureID` | VRAM Buffer I/O. |
| `0x6003` | `RENDER_SET_CAMERA` | `CameraID` | View Matrix Update. |

### 2.7. SPATIAL (0x7000 - 0x7FFF)
| OpCode | Mnemonic | Payload | Technical Description |
| :--- | :--- | :--- | :--- |
| `0x7001` | `SPATIAL_ORBITAL_UPD` | `OrbitID` | Orbital Vector Recalculation. |
| `0x7002` | `SPATIAL_TELEM_RX` | `TelemID` | Satellite Uplink Data. |
| `0x7003` | `SPATIAL_SAT_SYNC` | `SatID` | Satellite Handshake. |
| `0x7010` | `SPATIAL_COMPUTE_DIFF`| `PairID` | Delta Vector Computation. |
| `0x7011` | `SPATIAL_SCALE_FLOW` | `Percent` | Data Flow Scalar. |
| `0x7012` | `SPATIAL_ALIGN_PAGE` | `Type` | Memory Page Alignment (4KB). |
| `0x7020` | `SPATIAL_EDGE_INJECT` | `BufID` | Edge Node Injection. |
| `0x7021` | `SPATIAL_ZERO_COPY` | `0/1` | Zero-Copy Mode Toggle. |

### 2.8. MEMORY (0x8000 - 0x8FFF)
| OpCode | Mnemonic | Payload | Technical Description |
| :--- | :--- | :--- | :--- |
| `0x8001` | `MEM_ALLOC_OFFHEAP` | `Size` | Native Memory Segmentation. |
| `0x8002` | `MEM_FREE_OFFHEAP` | `PtrID` | Segment Deallocation. |
| `0x8003` | `MEM_MAP_SEGMENT` | `SegID` | Memory Mapped File I/O. |
| `0x8010` | `MEM_ALIGN_PAGE_4KB` | `PtrID` | 4KB Page Alignment. |
| `0x8011` | `MEM_ALIGN_PAGE_2MB` | `PtrID` | 2MB Huge Page Alignment. |
| `0x8020` | `MEM_PREFETCH_EN` | `BufID` | Hardware Prefetch Enable. |
| `0x8021` | `MEM_PREFETCH_DIS` | `BufID` | Hardware Prefetch Disable. |

### 2.9. ACCELERATOR (0x9000 - 0x9FFF)
| OpCode | Mnemonic | Payload | Technical Description |
| :--- | :--- | :--- | :--- |
| `0x9001` | `ACCEL_INIT_SIMD` | `LaneWidth` | Initialize Vector Processing. |
| `0x9002` | `ACCEL_VECTOR_REDUCE`| `MemPtr` | SIMD Reduction Operation. |
| `0x9003` | `ACCEL_VECTOR_TRANS` | `MemPtr` | SIMD Transformation. |
| `0x9004` | `ACCEL_DUMP_STATS` | `0` | Throughput Reporting. |

---

## 3. State Memory Layout

Direct offset addressing within the `MemorySegment` block.

### 3.1. Actor State Block (0-99)
High-frequency access data (Hot Data).
*   **Offset 0**: `PLAYER_X`
*   **Offset 4**: `PLAYER_Y`
*   **Offset 8**: `PLAYER_DIR`
*   **Offset 12**: `PLAYER_SCORE`

### 3.2. Kernel Control Block (100-199)
System critical control flags.
*   **Offset 400**: `SYS_TICK` (Master Clock)
*   **Offset 404**: `SYS_ENGINE_FLAGS` (State register)
*   **Offset 408**: `SYS_TARGET_FPS` (Timing constraint)
*   **Offset 412**: `SYS_DELTA_TIME` (Scalar)
*   **Offset 416**: `ENTITY_COUNT` (Population metric)

### 3.3. Hardware Telemetry Block (200-299)
Real-time system metrics.
*   **Offset 800**: `METRIC_CPU_LOAD` (normalized 0-10000)
*   **Offset 804**: `METRIC_RAM_FREE` (MB)
*   **Offset 808**: `METRIC_RAM_TOTAL` (MB)

### 3.4. Input Pipeline Block (300-399)
HID state registers.
*   **Offset 1200**: `INPUT_MOUSE_X`
*   **Offset 1204**: `INPUT_MOUSE_Y`
*   **Offset 1208**: `INPUT_LAST_SIGNAL` (Last valid opcode)

---

## 4. Kernel State Machine

Atomic state transitions managed by `KernelControlRegister`.

*   **0**: `STATE_OFFLINE`
*   **1**: `STATE_BOOTING`
*   **2**: `STATE_STARTING` (JIT Warm-up phase)
*   **3**: `STATE_RUNNING` (Active Loop)
*   **4**: `STATE_PAUSED`
*   **5**: `STATE_HALTING`
*   **6**: `STATE_TERMINATED`
*   **99**: `STATE_PANIC` (Unrecoverable Error)

---

**Autoridad**: System Architect
**Versión**: 2.0
