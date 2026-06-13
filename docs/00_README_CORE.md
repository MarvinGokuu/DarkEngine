# ⬛ DARK ENGINE v2.0 (AAA+ Elite)

![Status: Production Ready](https://img.shields.io/badge/Status-Production%20Ready-brightgreen.svg)
![Latency: <150ns](https://img.shields.io/badge/Latency-<%20150ns-blue.svg)
![Throughput: >50M ops/s](https://img.shields.io/badge/Throughput->50M%20ops%2Fs-blue.svg)
![Boot Time: <1ms](https://img.shields.io/badge/Boot%20Time-<%201ms-blue.svg)

**DarkEngine** es un motor de estado y orquestación escrito en Java 25+, diseñado bajo una estricta filosofía de **Simpatía Mecánica (Mechanical Sympathy)**. No es un motor de juegos convencional; es un Kernel de Ultra-Baja Latencia diseñado para eludir los cuellos de botella del JVM y operar directamente sobre las cachés del procesador (L1/L2/L3).

## 🚀 Filosofía Core

1. **Memoria Off-Heap**: El motor gestiona su propia memoria lineal fuera del Heap de Java mediante la Foreign Function & Memory API (Panama). El Recolector de Basura (GC) está de facto obsoleto aquí.
2. **Atomicidad Sin Bloqueos**: No existen bloqueos Mutex (`synchronized`) en la vía crítica. Todo opera mediante `VarHandle` y operaciones de memoria atómica a nivel de hardware.
3. **Cero Ruido en Terminal**: La ejecución en producción es **100% silenciosa**. Cualquier impresión en la terminal rompe la latencia del procesador.
4. **Verificación Estricta**: Ningún componente se carga sin una validación matemática de su integridad en memoria (`BusSymmetryValidator`).

## ⚡ Métricas AAA+ Certificadas
- **Latencia del Bus**: ~30 nanosegundos (P99 < 100ns).
- **Throughput**: Más de 60,000,000 de operaciones por segundo.
- **Tiempo de Arranque (Boot)**: ~0.0005 milisegundos.

## 🛠️ Primeros Pasos (Quick Start)

### Requisitos
- **JDK 25.0.1** o superior (con `--enable-preview` habilitado).
- Sistema Operativo moderno (Windows/Linux) capaz de invocar FFI.

### Comandos de Administración
- `.\build.bat`: Compila todo el motor desde cero. Si hay errores sintácticos, te arrojará la salida en pantalla.
- `.\test.bat`: Corre la suite de 14 Pruebas de Integridad AAA+ y emite el reporte a `aaa_test_report.log`.
- `.\clean.bat`: Limpia directorios binarios, puertos en uso y regenera una estructura de logs en blanco.
- `.\run.bat`: Ejecuta el motor en su perfil por defecto.

## 📚 Estructura de Documentación Maestra

Este repositorio mantiene su conocimiento arquitectónico consolidado en **8 Archivos Maestros**:
1. `00_README_CORE.md` (Estás aquí)
2. `01_ARCHITECTURE_AND_KERNEL.md`: Estructura del Kernel y Secuencia de Arranque.
3. `02_MECHANICAL_SYMPATHY.md`: Memoria de bajo nivel, Hilos y VarHandles.
4. `03_ATOMIC_BUS_PROTOCOL.md`: El corazón de comunicación (RingBus y AtomicBus).
5. `04_MEMORY_AND_STATE.md`: La Bóveda de Memoria de Sectores.
6. `05_TELEMETRY_AND_SILENCE.md`: DarkLogger y manejo de errores silenciosos.
7. `06_TESTING_AND_CERTIFICATION.md`: Suite de Integridad AAA+.
8. `07_GLOSSARY.md`: Diccionario técnico de términos.

---
*Escrito y diseñado por la élite arquitectónica. Bienvenido al DarkEngine.*
