# TECHNICAL DOCUMENTATION STANDARD: HPC RUNTIME

**Subsistema**: Documentation Engineering
**Tecnología**: Markdown / SemVer
**Estado**: Active Specification
**Autoridad**: System Architect

---

## 1. Protocolos de Comunicación Técnica

### 1.1. Axiomas de Redacción
La documentación técnica se rige por la fidelidad a la implementación en hardware.

1.  **Cero Subjetividad**: Se prohíben adjetivos cualitativos ("rápido", "robusto", "mejor"). Se requieren métricas cuantificables.
2.  **Abstracción Cero**: Se describe la operación a nivel de sistema, no la metáfora de alto nivel.
3.  **Identidad Pasiva**: La autoría recae en el rol ("System Architect"), no en el individuo.

### 1.2. Transformación Léxica (Mapeo de Abstracciones)
Sustitución mandatoria de términos conceptuales por implementaciones de hardware/runtime:

*   `Almacén` / `Vault` $\rightarrow$ **MemorySegment / Off-heap Region**
*   `Acelerador` $\rightarrow$ **Vector API / SIMD Instructions**
*   `Velocidad` / `Potencia` $\rightarrow$ **Throughput (GB/s) / Latencia (ns)**
*   `Hilos` $\rightarrow$ **Hardware Contexts / Execution Lanes**
*   `Motor` $\rightarrow$ **Runtime / Kernel**

---

## 2. Esquema de Metadatos (Documentos Markdown)

Todo artefacto documental debe iniciar con el siguiente bloque de definición de contexto:

```markdown
# [COMPONENT_ID]

**Subsistema**: [Kernel | Memory | Net | I/O]
**Tecnología**: [Java 25 | FFM | Vector | ZGC]
**Estado**: [Stable | Incubator | Deprecated]
**Autoridad**: System Architect

---
```

---

## 3. Estándar de Métricas de Rendimiento

La validación del rendimiento requiere tablas comparativas con deltas explícitos y unidades del Sistema Internacional.

### Formato de Tabla
```markdown
| Métrica | Target | Medido | Delta | Unidad |
| :--- | :--- | :--- | :--- | :--- |
| Latencia (Hot-Path) | < 150 | 142 | -5.3% | ns |
| Memory Bandwidth | > 4.0 | 4.17 | +4.2% | GB/s |
| L1 Cache Misses | < 1.0 | 0.02 | - | % |
```

---

## 4. Estándar de Documentación de Código (Java)

**Depreciación**: Se prohíbe el uso del bloque Javadoc legacy (`/** AUTORIDAD: Marvin-Dev ... */`).

**Nuevo Estándar**: Uso mandatorio de la anotación `@AAACertified` para componentes críticos.

```java
import sv.dark.core.AAACertified;

/**
 * TECHNICAL SPECIFICATION
 *
 * CONTEXT:
 * - [Descripción técnica del rol del componente]
 *
 * MEMORY SEMANTICS:
 * - [Descripción de acceso a memoria, alineación, etc.]
 */
@AAACertified(
    date = "YYYY-MM-DD",
    maxLatencyNs = [valor],
    minThroughput = [valor],
    alignment = 64,
    lockFree = true,
    offHeap = false,
    notes = "[Resumen técnico conciso]"
)
public final class ComponentName { ... }
```

---

## 5. Control de Versiones

Adopción de SemVer para documentación técnica:
*   **MAJOR**: Cambio en modelo de memoria o garantías de concurrencia.
*   **MINOR**: Nuevas métricas o refactorización de implementación.
*   **PATCH**: Correcciones ortográficas o de formato.

---

**Estado**: VIGENTE
**Autoridad**: System Architect
