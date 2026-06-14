# 🌌 DarkEngine — High-Performance Lock-Free Java Runtime

**Autoridad**: Chief Executive Officer / Kernel Architect  
**Tecnología Core**: Java 25 (Project Panama, Vector API, Loom)  
**Estado**: Production Ready (AAA+ Certified)  

> **🚀 "Un simple mortal les vino a bajar la industria"**  
> *DarkEngine no es un motor; es la demostración física de que el hardware barato puede entregar latencias extremas si aplicas Mechanical Sympathy absoluta.*

---

## 1. VISION Y DOMINIO DEL HARDWARE

El desarrollo de runtimes tradicionales está plagado de cuellos de botella (Garbage Collectors, OS Schedulers, Thread Locks). **DarkEngine somete al hardware:**

1.  **Cero Pausas GC**: Todo el estado reside en memoria nativa (Off-Heap) vía **Project Panama**. 
2.  **Cero Locks**: La concurrencia se logra con colas en anillo (Ring Buffers) usando **VarHandles** atómicos. Latencia de transferencia inter-núcleos: `~23ns`.
3.  **Ancho de Banda SIMD**: Matemáticas vectorizadas vía `jdk.incubator.vector` (AVX-512) para anchos de banda procesados superiores a `4.17 GB/s`.
4.  **Anti-False Sharing**: Aislamiento y padding de estructuras críticas a 64 bytes para empatizar perfectamente con las *L1 Cache Lines*.
5.  **GPU-Driven Compute**: Descarte masivo de geometría (Culling) 100% en VRAM vía Compute Shaders y OpenGL 4.3 FFI (Zero-Overhead).

Para entender la motivación y cómo planeamos revolucionar el mercado a $1/mes, lee:
👉 **[EL MANIFIESTO DARK ENGINE](docs/vision/DARK_ENGINE_MANIFESTO.md)**

---

## 2. ARQUITECTURA TÉCNICA (MECHANICAL SYMPATHY)

No existe un "sistema orquestador" que ahogue el rendimiento con overhead de gestión. Toda la base de código se rige por el determinismo asíncrono y paralelismo puro.

### 📚 El Santo Grial Técnico
Si quieres entender cómo implementamos todo esto usando Java 25, debes estudiar nuestras especificaciones de arquitectura Core. Aquí reside el conocimiento clasificado del motor:

1. **[MECHANICAL SYMPATHY CORE](docs/architecture/MECHANICAL_SYMPATHY_CORE.md)**: Cómo destruir el "False Sharing", alinear estructuras de memoria y operar atómicamente con VarHandles.
2. **[SYSTEM ARCHITECTURE](docs/architecture/SYSTEM_ARCHITECTURE.md)**: El flujo de ejecución del Kernel y el diseño modular libre de bloqueos.
3. **[CODE_DEBT_DETAILED_AUDIT](docs/reports/CODE_DEBT_DETAILED_AUDIT.md)**: La auditoría completa y detallada de deuda técnica de código, cuellos de botella de hardware, y plan de escalamiento a Java 26 / Valhalla.

---

## 3. COMPILACIÓN Y EJECUCIÓN (BOOTSTRAPPING)

### Prerrequisitos de Compilación
*   **JDK**: Oracle GraalVM 25 / OpenJDK 25 (módulos incubadora habilitados).
*   **OS**: Windows 11 / Linux Kernel 6.x.

### Operación Básica
Desde la terminal, utiliza los scripts provistos en la raíz del proyecto para comandar el runtime:

```bash
# 1. Compilación brutal y enlazado
.\build.bat

# 2. Verificación de todas las pruebas AAA+ y benchmarks
.\scripts\test.bat

# 3. Limpiar binarios y liberar memoria de procesos "zombies"
.\clean.bat

# 4. Lanzar el motor en segundo plano de manera autónoma
.\run.bat
```

> ⚠️ **Nota de Arquitecto**: Al modificar código, respeta los estándares AAA+. Nunca inyectes clases `new String()` ni reservas de Heap en el hot-path del motor. Todo se ejecuta `Off-Heap`.

---

**Licencia**: [GNU Lesser General Public License v3.0](LICENSE)  
**Versión**: v3.0 (Pure Mechanical Sympathy Edition)
