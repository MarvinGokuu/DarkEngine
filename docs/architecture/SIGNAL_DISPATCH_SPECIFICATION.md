# SIGNAL_DISPATCH_SPECIFICATION

**Subsystem**: Core / Signaling
**Technology**: 64-bit Packed Signals
**Status**: V2.0 Standard
**Authority**: System Architect

---

## 1. System Specification

### 1.1. Packing Principles
The system exclusively uses 64-bit registers (`long`) for complex data transmission, leveraging General Purpose Register (GPR) uniformity.

*   **Zero-Allocation**: Total absence of object instantiation in the Hot-Path.
*   **ALU Efficiency**: Use of bitwise operations (`Shift`, `Mask`) executable in < 1 clock cycle.
*   **Hardware Atomicity**: Guaranteed atomic 64-bit read/write on x64 architecture.

### 1.2. Packing Diagram (Memory Layout)

```
[ COMMAND ID (16b) ] [ PAYLOAD TYPE (16b) ] [ DATA (32b) ]
|------------------|----------------------|--------------|
       Header             Meta-Data            Value
```

---

## 2. Signal Types (Data Schema)

### 2.1. Vector 2D (Float32x2)
Packing of two 32-bit floating-point values into a single 64-bit register.

**Structure**:
```
[ Float32 Y (High) ] [ Float32 X (Low) ]
```

**Hardware Benefit**:
*   **SIMD-Ready**: Direct load to vector registers (XMM/YMM).
*   **Precision**: Full IEEE 754 without loss.

### 2.2. Spatial Coordinates (16/16/32)
Compression for spatial telemetry with high dynamic range.

**Structure**:
```
[ X: Int16 ] [ Y: Int16 ] [ Z: Int32 ]
```

### 2.3. Atomic Signals (Flags)
Bit vector for massive boolean states.
*   **Capacity**: 63 flags + 1 sign bit.
*   **Usage**: Atomic synchronization of subsystem states.

---

## 3. System Commands (OpCodes)

### 3.1. Virtual Memory Ranges

| Range | Subsystem | Technical Description |
| :--- | :--- | :--- |
| `0x1000` | **INPUT** | HID Events (Keyboard/Mouse) |
| `0x2000` | **NET** | Packet Synchronization |
| `0x3000` | **KERNEL** | Process Control |
| `0x7000` | **SPATIAL** | Telemetry & Orbitals |
| `0x8000` | **MEM** | Off-Heap Management |

### 3.2. Inline Arithmetic Operations
Operations executed directly on CPU registers without unpacking to main memory.
*   `computeOrbitalDifferential`: Vector differential in registers.
*   `scaleFlow`: Direct scalar multiplication.

---

## 4. Performance Metrics (Targets)

| Operation | Latency | Throughput | Technical Note |
| :--- | :--- | :--- | :--- |
| **Pack (2xFloat)** | ~2 ns | > 500M op/s | Bitwise Shift/Or |
| **Unpack (Float)** | ~1 ns | > 1G op/s | Bitwise Mask |
| **Dispatch** | < 150 ns | 12M msg/s | Includes Bus latency |

---

## 5. Edge Computing Integration

### 5.1. Zero-Copy Injection
Mechanism to inject external data buffers directly into the event bus without intermediate copying.

**Data Flow**:
1.  Map external `MemorySegment`.
2.  Validate Page Alignment (4KB Page).
3.  Bulk Transfer (`VectorCopy`) to Ring Buffer.

---

**Version**: 2.0
**Status**: ACTIVE
**Authority**: System Architect
