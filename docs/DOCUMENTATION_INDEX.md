# DARK ENGINE - DOCUMENTATION INDEX & MAP
## Ultimate Reference & Reading Protocol

**Fecha:** 2026-06-09  
**Versión del Mapa:** 3.0.0  
**Estado:** ✅ Consolidado con Protocolo de Indexación Binaria y Reorganización Estructural

---

## 🛰️ PROTOCOLO DE INDEXACIÓN BINARIA (8-Bit Addresses)

Para garantizar un orden determinista y el 100% de cobertura de la documentación, cada documento en el proyecto tiene asignada una dirección binaria de 8 bits. Todo desarrollador debe leer, actualizar y documentar siguiendo esta topología estricta:

```
[Categoría de 4 bits] [Identificador de 4 bits]
```

### 📚 ORDEN DE LECTURA OFICIAL Y DIRECCIONAMIENTO

#### **Bloque 0000xxxx: Fundación e Inicio de Lectura**
*   `00000001` $\rightarrow$ **[README.md](../README.md)** (Introducción básica al repositorio).
*   `00000010` $\rightarrow$ **[docs/README_DOCS.md](README_DOCS.md)** (Resumen Ejecutivo y estado actual del motor).
*   `00000011` $\rightarrow$ **[docs/DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md)** (Este mapa maestro de lectura y actualización).
*   `00000100` $\rightarrow$ **[docs/glossary/TECHNICAL_GLOSSARY.md](glossary/TECHNICAL_GLOSSARY.md)** (Glosario técnico de hardware y concurrencia).

#### **Bloque 0001xxxx: Visión y Filosofía (docs/vision/)**
*   `00010001` $\rightarrow$ **[docs/vision/MASTER_VISION.md](vision/MASTER_VISION.md)** (Visión y metas del motor).
*   `00010010` $\rightarrow$ **[docs/vision/PORQUE_DARK_ENGINE.md](vision/PORQUE_DARK_ENGINE.md)** (Por qué desarrollamos DarkEngine y decisiones clave).

#### **Bloque 0010xxxx: Arquitectura e Ingeniería de Bajo Nivel (docs/architecture/)**
*   `00100001` $\rightarrow$ **[docs/architecture/ARQUITECTURA_DARK_ENGINE.md](architecture/ARQUITECTURA_DARK_ENGINE.md)** (Especificación del Core Kernel y flujos de datos).
*   `00100010` $\rightarrow$ **[docs/architecture/SYSTEM_DEPENDENCY_GRAPH.md](architecture/SYSTEM_DEPENDENCY_GRAPH.md)** (Grafo de capas de ejecución del ParallelSystemExecutor).
*   `00100011` $\rightarrow$ **[docs/architecture/COGNITIVE_ARCHITECTURE_SPECIFICATION.md](architecture/COGNITIVE_ARCHITECTURE_SPECIFICATION.md)** (Especificaciones de la arquitectura cognitiva).
*   `00100100` $\rightarrow$ **[docs/architecture/SECURITY_ARCHITECTURE.md](architecture/SECURITY_ARCHITECTURE.md)** (Seguridad, aislamiento y robustez del sistema).
*   `00100101` $\rightarrow$ **[docs/architecture/SIGNAL_DISPATCH_SPECIFICATION.md](architecture/SIGNAL_DISPATCH_SPECIFICATION.md)** (Especificación del sistema de despacho de señales de bajo nivel).
*   `00100110` $\rightarrow$ **[docs/architecture/BINARY_SIGNAL_INDEX.md](architecture/BINARY_SIGNAL_INDEX.md)** (Índice maestro de señales binarias y payloads).

#### **Bloque 0011xxxx: Certificación y Rendimiento (docs/certification/)**
*   `00110001` $\rightarrow$ **[docs/certification/aaa_certification_results.md](certification/aaa_certification_results.md)** (Resultados de suite de pruebas y cumplimiento AAA).
*   `00110010` $\rightarrow$ **[docs/certification/BINARY_DISPATCH_PERFORMANCE_ANALYSIS.md](certification/BINARY_DISPATCH_PERFORMANCE_ANALYSIS.md)** (Análisis detallado de latencia y throughput del bus circular).
*   `00110011` $\rightarrow$ **[docs/certification/boot_latency_comparison.md](certification/boot_latency_comparison.md)** (Comparativa de latencia de arranque).
*   `00110100` $\rightarrow$ **[docs/certification/PEAK_PERFORMANCE_REPORT.md](certification/PEAK_PERFORMANCE_REPORT.md)** (Reporte detallado de optimizaciones y benchmarks de rendimiento pico).

#### **Bloque 0100xxxx: Estándares y Protocolos Técnicos (docs/standards/)**
*   `01000001` $\rightarrow$ **[docs/standards/ESTANDAR_DOCUMENTACION.md](standards/ESTANDAR_DOCUMENTACION.md)** (Normativa de redacción técnica objetiva).
*   `01000010` $\rightarrow$ **[docs/standards/AAA_CODING_STANDARDS.md](standards/AAA_CODING_STANDARDS.md)** (Normativa de desarrollo de bajo nivel para DarkEngine).
*   `01000011` $\rightarrow$ **[docs/standards/BASELINE_PROTOCOL.md](standards/BASELINE_PROTOCOL.md)** (Línea base y protocolo de preservación de estado).
*   `01000100` $\rightarrow$ **[docs/standards/DOCUMENTATION_BOOTSTRAP_PROTOCOL.md](standards/DOCUMENTATION_BOOTSTRAP_PROTOCOL.md)** (Protocolo de bootstrapping de documentación).
*   `01000101` $\rightarrow$ **[docs/standards/INITIAL_DEPLOYMENT_PROTOCOL.md](standards/INITIAL_DEPLOYMENT_PROTOCOL.md)** (Especificación de configuración y despliegue inicial).
*   `01000110` $\rightarrow$ **[docs/standards/AAA_CERTIFICATION.md](standards/AAA_CERTIFICATION.md)** (Requisitos de certificación de nivel AAA).
*   `01000111` $\rightarrow$ **[docs/standards/AAA_CERTIFICATION_QUICK_GUIDE.md](standards/AAA_CERTIFICATION_QUICK_GUIDE.md)** (Guía rápida de criterios AAA).
*   `01001000` $\rightarrow$ **[docs/standards/AAA_PLUS_PLUS_VALIDATION.md](standards/AAA_PLUS_PLUS_VALIDATION.md)** (Validación avanzada AAA++).
*   `01001001` $\rightarrow$ **[docs/standards/ACCELERATOR_CERTIFICATION.md](standards/ACCELERATOR_CERTIFICATION.md)** (Certificación de acelerador de cómputo).
*   `01001010` $\rightarrow$ **[docs/standards/DOCUMENTATION_REFACTORING_SPECIFICATION.md](standards/DOCUMENTATION_REFACTORING_SPECIFICATION.md)** (Especificación técnica de refactorización de docs).

#### **Bloque 0101xxxx: Manuales de Operación y Soporte (docs/manuals/)**
*   `01010001` $\rightarrow$ **[docs/manuals/walkthrough.md](manuals/walkthrough.md)** (Historial maestro de integraciones).
*   `01010010` $\rightarrow$ **[docs/manuals/GUIA_COMMITS.md](manuals/GUIA_COMMITS.md)** (Normativa de commits atómicos).
*   `01010011` $\rightarrow$ **[docs/manuals/FLUJO_TRABAJO.md](manuals/FLUJO_TRABAJO.md)** (Protocolo diario de trabajo).
*   `01010100` $\rightarrow$ **[docs/manuals/DEVELOPMENT_GUIDE.md](manuals/DEVELOPMENT_GUIDE.md)** (Guía general del programador).
*   `01010101` $\rightarrow$ **[docs/manuals/QUICK_START.md](manuals/QUICK_START.md)** (Guía de inicio rápido en 5 minutos).
*   `01010110` $\rightarrow$ **[docs/manuals/VARHANDLE_PANAMA_MASTERY.md](manuals/VARHANDLE_PANAMA_MASTERY.md)** (Manual de manipulación directa de memoria nativa).
*   `01010111` $\rightarrow$ **[docs/manuals/TROUBLESHOOTING_GUIDE.md](manuals/TROUBLESHOOTING_GUIDE.md)** (Guía de resolución de incidencias comunes).
*   `01011000` $\rightarrow$ **[docs/manuals/BUILD_WORKFLOWS.md](manuals/BUILD_WORKFLOWS.md)** (Workflows de compilación y empaquetado).
*   `01011001` $\rightarrow$ **[docs/manuals/kernel_shutdown_verification_guide.md](manuals/kernel_shutdown_verification_guide.md)** (Verificación de apagado limpio de descriptores de kernel).
*   `01011010` $\rightarrow$ **[docs/manuals/ACCELERATOR_PHYSICS.md](manuals/ACCELERATOR_PHYSICS.md)** (Manual de física acelerada de partículas).
*   `01011011` $\rightarrow$ **[docs/manuals/DOCUMENTACION_BUS.md](manuals/DOCUMENTACION_BUS.md)** (Manual del bus de comunicación interna).
*   `01011100` $\rightarrow$ **[docs/manuals/ESTRATEGIA_COMMITS.md](manuals/ESTRATEGIA_COMMITS.md)** (Estrategia de commits extendida).
*   `01011101` $\rightarrow$ **[docs/manuals/GUIA_UPDATE_SYNC.md](manuals/GUIA_UPDATE_SYNC.md)** (Guía de sincronización y actualización del repositorio).
*   `01011110` $\rightarrow$ **[docs/manuals/walkthrough_2026-01-24.md](manuals/walkthrough_2026-01-24.md)** (Historial de integración específico).

#### **Bloque 0110xxxx: Reportes de Cobertura, Calidad e Integridad (docs/reports/)**
*   `01100001` $\rightarrow$ **[docs/reports/PROJECT_HEALTH_REPORT.md](reports/PROJECT_HEALTH_REPORT.md)** (Diagnóstico del estado del proyecto y dependencias).
*   `01100010` $\rightarrow$ **[docs/reports/DOCUMENTATION_COVERAGE_ANALYSIS.md](reports/DOCUMENTATION_COVERAGE_ANALYSIS.md)** (Cobertura de documentación y verificación binaria).
*   `01100011` $\rightarrow$ **[docs/reports/LINKS_VERIFICATION_REPORT.md](reports/LINKS_VERIFICATION_REPORT.md)** (Reporte y estado de consistencia de enlaces).

---

## 🛠️ PROTOCOLO DE ACTUALIZACIÓN (SemVer Docs)

1.  **Read**: Todo nuevo cambio en el kernel debe ser antecedido por la lectura de los Estándares (`0100xxxx`).
2.  **Unify**: Al agregar un nuevo archivo `.java` o método crítico en el hot-path, se debe documentar en:
    *   `TECHNICAL_GLOSSARY.md` (si introduce conceptos o métricas nuevas).
    *   `aaa_certification_results.md` (si agrega pruebas a la suite o altera el throughput del bus).
3.  **Depurate**: Se prohíbe la duplicidad de manuales. Cualquier walkthrough técnico temporal debe ser depurado o integrado en el walkthrough maestro de manuales (`walkthrough.md`) tras su fusión.

---

## 📦 COMPILACIÓN Y SUITE DE PRUEBAS (12/12)

### **Comando de Compilación Directa (Java 25)**
```powershell
javac -d bin --enable-preview --source 25 --add-modules jdk.incubator.vector -Xlint:-incubating -cp src src\sv\dark\state\DarkEngineMaster.java src\sv\dark\kernel\*.java src\sv\dark\core\*.java src\sv\dark\core\memory\*.java src\sv\dark\core\systems\*.java src\sv\dark\state\*.java src\sv\dark\bus\*.java src\sv\dark\net\*.java src\sv\dark\test\*.java src\sv\dark\ui\*.java
```

### **Tabla de Pruebas Automatizadas (`.\test.bat`)**

| Paso | Clase de Prueba | Propósito y Garantías |
| :--- | :--- | :--- |
| **[1/12]** | `sv.dark.bus.BusBenchmarkTest` | Latencia típica de offer/poll <150ns en el bus circular. |
| **[2/12]** | `sv.dark.bus.BusCoordinationTest` | Multi-hilo no bloqueante bajo Dispatcher multi-lane. |
| **[3/12]** | `sv.dark.bus.BusHardwareTest` | Validación estructural de padding de variables anti false-sharing. |
| **[4/12]** | `sv.dark.test.UltraFastBootTest` | Latencia de arranque del motor garantizada <1.0ms. |
| **[5/12]** | `sv.dark.test.GracefulShutdownTest` | Liberación de descriptores de memoria nativa y sockets (sin leaks). |
| **[6/12]** | `sv.dark.test.PowerSavingTest` | Escalado de reposo de CPU de 3 niveles en modo de inactividad (idle). |
| **[7/12]** | `sv.dark.test.ParticleSystemDeterminismTest` | Reproducibilidad de simulación con RNG determinista local. |
| **[8/12]** | `sv.dark.test.SystemRegistryCapacityTest` | Pre-dimensionamiento de colecciones del registro de sistemas. |
| **[9/12]** | `sv.dark.test.DependencyGraphPerformanceTest` | Grafo de dependencias de hilos sin reallocations en ejecución. |
| **[10/12]** | `sv.dark.bus.BusBenchmarkTest` (final) | Consistencia de throughput del bus atómico (>185M ops/s). |
| **[11/12]** | `sv.dark.test.MetricsAggregationTest` | Aislamiento de métricas de sistemas paralelos sin false-sharing (174M ops/s). |
| **[12/12]** | `sv.dark.test.SystemStateManagerTest` | Captura, boosting a Alto Rendimiento y restauración 100% limpia de Windows. |

---

**Última Verificación:** 2026-06-09  
**Verificado Por:** System Architect  
**Estado:** ✅ Consistente y Actualizado
