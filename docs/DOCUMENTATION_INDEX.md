# DARK ENGINE - DOCUMENTATION INDEX & MAP
## Ultimate Reference & Reading Protocol

**Fecha:** 2026-06-09  
**Versión del Mapa:** 2.3.0  
**Estado:** ✅ Consolidado con Protocolo de Indexación Binaria

---

## 🛰️ PROTOCOLO DE INDEXACIÓN BINARIA (8-Bit Addresses)

Para garantizar un orden determinista y el 100% de cobertura de la documentación, cada documento en el proyecto tiene asignada una dirección binaria de 8 bits. Todo desarrollador debe leer, actualizar y documentar siguiendo esta topología estricta:

```
[Categoría de 4 bits] [Identificador de 4 bits]
```

### 📚 ORDEN DE LECTURA OFICIAL

#### **Bloque 0000xxxx: Fundación e Inicio de Lectura**
*   `00000001` $\rightarrow$ **[README.md](file:///C:/Users/theca/Documents/GitHub/DarkEngine/README.md)** (Introducción básica al repositorio).
*   `00000010` $\rightarrow$ **[docs/README_DOCS.md](file:///C:/Users/theca/Documents/GitHub/DarkEngine/docs/README_DOCS.md)** (Resumen Ejecutivo y estado actual del motor).
*   `00000011` $\rightarrow$ **[docs/DOCUMENTATION_INDEX.md](file:///C:/Users/theca/Documents/GitHub/DarkEngine/docs/DOCUMENTATION_INDEX.md)** (Este mapa maestro de lectura y actualización).
*   `00000100` $\rightarrow$ **[docs/glossary/TECHNICAL_GLOSSARY.md](file:///C:/Users/theca/Documents/GitHub/DarkEngine/docs/glossary/TECHNICAL_GLOSSARY.md)** (Glosario técnico de hardware y concurrencia).

#### **Bloque 0001xxxx: Arquitectura e Ingeniería de Bajo Nivel**
*   `00010001` $\rightarrow$ **[docs/architecture/ARQUITECTURA_DARK_ENGINE.md](file:///C:/Users/theca/Documents/GitHub/DarkEngine/docs/architecture/ARQUITECTURA_DARK_ENGINE.md)** (Especificación del Core Kernel y flujos de datos).
*   `00010011` $\rightarrow$ **[docs/SYSTEM_DEPENDENCY_GRAPH.md](file:///C:/Users/theca/Documents/GitHub/DarkEngine/docs/SYSTEM_DEPENDENCY_GRAPH.md)** (Grafo de capas de ejecución del ParallelSystemExecutor).

#### **Bloque 0010xxxx: Estándares y Reportes de Rendimiento**
*   `00100001` $\rightarrow$ **[docs/standards/ESTANDAR_DOCUMENTACION.md](file:///C:/Users/theca/Documents/GitHub/DarkEngine/docs/standards/ESTANDAR_DOCUMENTACION.md)** (Normativa de redacción técnica libre de subjetividad).
*   `00100010` $\rightarrow$ **[docs/standards/AAA_CODING_STANDARDS.md](file:///C:/Users/theca/Documents/GitHub/DarkEngine/docs/standards/AAA_CODING_STANDARDS.md)** (Normativa de código: ZGC, FFM Panama, inlining y local-metrics).
*   `00100011` $\rightarrow$ **[docs/standards/aaa_certification_results.md](file:///C:/Users/theca/Documents/GitHub/DarkEngine/docs/standards/aaa_certification_results.md)** (Reporte detallado de benchmarks y paso de pruebas 12/12).

#### **Bloque 0011xxxx: Manuales de Operación y Soporte**
*   `00110001` $\rightarrow$ **[docs/manuals/walkthrough.md](file:///C:/Users/theca/Documents/GitHub/DarkEngine/docs/manuals/walkthrough.md)** (Historial e instrucciones de integración de características).
*   `00110010` $\rightarrow$ **[docs/manuals/GUIA_COMMITS.md](file:///C:/Users/theca/Documents/GitHub/DarkEngine/docs/manuals/GUIA_COMMITS.md)** (Normativa de commits atómicos y descriptivos).
*   `00110011` $\rightarrow$ **[docs/manuals/FLUJO_TRABAJO.md](file:///C:/Users/theca/Documents/GitHub/DarkEngine/docs/manuals/FLUJO_TRABAJO.md)** (Flujo diario de integración local y remota).
*   `00110100` $\rightarrow$ **[docs/TROUBLESHOOTING_GUIDE.md](file:///C:/Users/theca/Documents/GitHub/DarkEngine/docs/TROUBLESHOOTING_GUIDE.md)** (Registro de incidencias del motor y su respectiva resolución).

---

## 🛠️ PROTOCOLO DE ACTUALIZACIÓN (SemVer Docs)

1.  **Read**: Todo nuevo cambio en el kernel debe ser antecedido por la lectura del bloque `0010xxxx` (Estándares).
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
