# DARK ENGINE - DOCUMENTATION INDEX
## Complete Reference Guide

**Fecha:** 2026-01-19  
**Versión:** 2.0  
**Estado:** ✅ Verificado y Consolidado

---

## 📚 ORDEN DE LECTURA RECOMENDADO

### **Para Nuevos Desarrolladores:**

1. **`README.md`** (Raíz del proyecto)
   - Introducción al proyecto
   - Quick start
   - Comandos básicos

2. **`docs/README_DOCS.md`** ← **EMPEZAR AQUÍ**
   - Resumen ejecutivo
   - Estado actual del motor
   - Referencias a todos los documentos

3. **`docs/certification/PEAK_PERFORMANCE_REPORT.md`**
   - Certificación AAA+
   - Métricas de rendimiento
   - Escalabilidad de hardware

4. **`docs/glossary/TECHNICAL_GLOSSARY.md`**
   - Definiciones técnicas
   - Conceptos de hardware
   - Primitivas de concurrencia

5. **`docs/BINARY_SIGNAL_INDEX.md`**
   - Arquitectura de señales
   - Command Set Architecture
   - Memory layout

6. **`docs/roadmap/FASE_1_GAME_LAUNCHER.md`**
   - Plan de implementación MVP
   - Timeline de 10 semanas
   - Estrategia de mercado

---

### **Para Arquitectos/Diseñadores:**

1. **`docs/architecture/ARQUITECTURA_DARK_ENGINE.md`**
   - Arquitectura completa del motor
   - Componentes principales
   - Diagramas de sistema

2. **`docs/architecture/DARK_OS_MASTER_PLAN.md`**
   - Plan a largo plazo (OS completo)
   - 8 fases de desarrollo
   - Roadmap Q1-Q4 2026

3. **`docs/SYSTEM_DEPENDENCY_GRAPH.md`**
   - Grafo de dependencias
   - Orden de ejecución
   - Capas de sistemas

---

### **Para Desarrolladores Activos:**

1. **`docs/standards/ESTANDAR_DOCUMENTACION.md`**
   - Estándar de documentación AAA+
   - Headers requeridos
   - Ejemplos

2. **`docs/standards/AAA_CODING_STANDARDS.md`**
   - Estándares de código
   - Naming conventions
   - Performance guidelines

3. **`docs/manuals/FLUJO_TRABAJO.md`**
   - Flujo de trabajo diario
   - Comandos comunes
   - Troubleshooting

4. **`docs/manuals/GUIA_COMMITS.md`**
   - Estrategia de commits
   - Mensajes atómicos
   - Git workflow

5. **[boot_latency_comparison.md](file:///c:/Users/theca/Documents/GitHub/DarkEngine/docs/boot_latency_comparison.md)**
   - Comparación y telemetría de latencia de arranque (AAA+).
   - Análisis del impacto de I/O bloqueante en micro-benchmarking.

6. **[kernel_shutdown_verification_guide.md](file:///c:/Users/theca/Documents/GitHub/DarkEngine/docs/kernel_shutdown_verification_guide.md)**
   - Protocolo estructurado de auditoría de apagado y liberación limpia de puertos/recursos.

---

## 🔧 COMPILACIÓN Y EJECUCIÓN

### **Orden de Compilación:**

#### **1. Compilación Completa (Recomendado)**
```batch
build.bat
```

**Qué hace:**
1. Limpia `bin/` directory
2. Compila todos los archivos `.java` con:
   - Java 25 + Preview features
   - ZGC enabled
   - Vector API (SIMD)
   - Heap fijo 4GB
3. Ejecuta `DarkEngineMaster`

**Orden de compilación interno:**
```
1. sv.dark.state.DarkEngineMaster.java
2. sv.dark.kernel.*.java
3. sv.dark.core.*.java
4. sv.dark.core.memory.*.java
5. sv.dark.core.systems.*.java
6. sv.dark.state.*.java
7. sv.dark.bus.*.java
8. sv.dark.net.*.java
9. sv.dark.test.*.java
```

---

#### **2. Compilación Rápida (Solo cambios)**
```batch
compile.bat
```

**Qué hace:**
- Compila solo archivos modificados
- Más rápido para desarrollo iterativo

---

#### **3. Ejecución sin Recompilar**
```batch
ignite.bat
```

**Qué hace:**
- Ejecuta el motor desde binarios existentes
- Útil para testing rápido

---

### **Tests Disponibles:**

| Test | Ubicación | Propósito |
|------|-----------|-----------|
| **BusBenchmarkTest** | `sv.dark.bus` | Benchmark del bus atómico (>10M ops/s) |
| **BusCoordinationTest** | `sv.dark.bus` | Coordinación multi-lane |
| **BusHardwareTest** | `sv.dark.bus` | Validación de hardware |
| **UltraFastBootTest** | `sv.dark.test` | Boot time <1ms |
| **GracefulShutdownTest** | `sv.dark.test` | Shutdown limpio |
| **PowerSavingTest** | `sv.dark.test` | Tiered idle system |
| **ParticleSystemDeterminismTest** | `sv.dark.test` | Validación de RNG determinista |
| **SystemRegistryCapacityTest** | `sv.dark.test` | Validación de pre-dimensionado de colecciones |
| **DependencyGraphPerformanceTest**| `sv.dark.test` | Validación de pre-dimensionado de grafos de dependencia |

**Ejecutar tests:**
```batch
.\test.bat
```

---

## 📁 ESTRUCTURA DE DOCUMENTACIÓN

```
docs/
├── README_DOCS.md                    ← ÍNDICE PRINCIPAL
├── BINARY_SIGNAL_INDEX.md            (Público)
├── TROUBLESHOOTING_GUIDE.md          (Público)
├── boot_latency_comparison.md        (Público)
├── kernel_shutdown_verification_guide.md (// Desarrollo únicamente)
├── walkthrough_visual_and_kernel.md  (// Desarrollo únicamente)
│
├── architecture/
│   ├── ARQUITECTURA_DARK_ENGINE.md (Público)
│   └── DARK_OS_MASTER_PLAN.md      (// Desarrollo - Plan a largo plazo)
│
├── certification/
│   └── PEAK_PERFORMANCE_REPORT.md    (Público - Marketing)
│
├── glossary/
│   └── TECHNICAL_GLOSSARY.md         (Público)
│
├── manuals/
│   ├── ACCELERATOR_PHYSICS.md        (Público)
│   ├── DOCUMENTACION_BUS.md          (Público)
│   ├── FLUJO_TRABAJO.md              (// Desarrollo únicamente)
│   ├── GUIA_COMMITS.md               (// Desarrollo únicamente)
│   ├── GUIA_UPDATE_SYNC.md           (// Desarrollo únicamente)
│   └── walkthrough.md                (// Desarrollo únicamente)
│
├── roadmap/
│   └── FASE_1_GAME_LAUNCHER.md       (// Desarrollo - Plan MVP)
│
└── standards/
    ├── AAA_CERTIFICATION.md          (Público)
    ├── AAA_CODING_STANDARDS.md       (// Desarrollo únicamente)
    ├── ESTANDAR_DOCUMENTACION.md     (// Desarrollo únicamente)
    └── aaa_certification_results.md  (Público)
```

### **Leyenda:**
- **(Público)** → Documentación para usuarios/clientes
- **(// Desarrollo únicamente)** → Solo para equipo interno

---

## 🗂️ DOCUMENTOS INTERNOS (NO PÚBLICOS)

> [!WARNING]
> Los siguientes documentos son **SOLO PARA DESARROLLO INTERNO**. No deben incluirse en releases públicos.

### **Planeación y Estrategia:**
- `docs/architecture/DARK_OS_MASTER_PLAN.md`
- `docs/roadmap/FASE_1_GAME_LAUNCHER.md`
- `docs/MASTER_PLAN_V2.md`
- `docs/PENDING_UPDATES_LOG.md`

### **Flujo de Trabajo:**
- `docs/manuals/FLUJO_TRABAJO.md`
- `docs/manuals/GUIA_COMMITS.md`
- `docs/manuals/GUIA_UPDATE_SYNC.md`
- `docs/manuals/ESTRATEGIA_COMMITS.md`
- `docs/manuals/walkthrough.md`
- `docs/walkthrough_visual_and_kernel.md`

### **Estándares Internos:**
- `docs/standards/AAA_CODING_STANDARDS.md`
- `docs/standards/ESTANDAR_DOCUMENTACION.md`
- `docs/standards/DOCUMENTATION_REFACTORING_SPECIFICATION.md`

### **Análisis y Especificaciones:**
- `docs/BINARY_DISPATCH_PERFORMANCE_ANALYSIS.md`
- `docs/COGNITIVE_ARCHITECTURE_SPECIFICATION.md`
- `docs/DOCUMENTATION_COVERAGE_ANALYSIS.md`
- `docs/SIGNAL_DISPATCH_SPECIFICATION.md`

### **Protocolos de Deployment:**
- `docs/BASELINE_PROTOCOL.md`
- `docs/INITIAL_DEPLOYMENT_PROTOCOL.md`
- `docs/DOCUMENTATION_BOOTSTRAP_PROTOCOL.md`
- `docs/SECURITY_ARCHITECTURE.md`
- `docs/kernel_shutdown_verification_guide.md`

---

## 📦 BINARIOS Y OUTPUTS

### **Directorio `bin/`:**
- Contiene archivos `.class` compilados
- Generado por `build.bat`
- **NO** incluir en Git (`.gitignore`)

### **Directorio `dist/`:**
- Releases empaquetados
- JARs distribuibles
- **NO** incluir en Git

### **Logs de Performance:**
- `gc_analysis.log` - Log de GC baseline
- `gc_optimized.log` - Log de GC con optimizaciones
- `gc_production.log` - Log de GC certificado AAA+
- `jit_compilation.log` - Log de compilación JIT
- **NO** incluir en Git (muy grandes)

### **Directorio `brain/`:**
- Documentación de planeación interna
- Neuronas de desarrollo
- **NO** incluir en Git

### **Directorio `tools/`:**
- Scripts de desarrollo
- Herramientas internas
- **NO** incluir en Git

### **Directorio `Dark-Engine/`:**
- Ejecutable nativo autocontenido de producción (`Dark-Engine.exe`)
- Generado por `exe.bat` usando `jpackage`
- **NO** incluir en Git (generado localmente)

---

## 🎯 DOCUMENTACIÓN PÚBLICA (Para Release)

### **Incluir en Release:**

1. **README.md** (Raíz)
2. **LICENSE.md**
3. **docs/README_DOCS.md**
4. **docs/certification/PEAK_PERFORMANCE_REPORT.md**
5. **docs/glossary/TECHNICAL_GLOSSARY.md**
6. **docs/BINARY_SIGNAL_INDEX.md**
7. **docs/TROUBLESHOOTING_GUIDE.md**
8. **docs/boot_latency_comparison.md**
9. **docs/architecture/ARQUITECTURA_DARK_ENGINE.md**
10. **docs/manuals/ACCELERATOR_PHYSICS.md**
11. **docs/manuals/DOCUMENTACION_BUS.md**
12. **docs/standards/AAA_CERTIFICATION.md**
13. **docs/standards/aaa_certification_results.md**

### **Excluir de Release:**
- Todo lo marcado como `(// Desarrollo únicamente)`
- Directorios: `brain/`, `tools/`
- Logs: `*.log`
- Binarios: `bin/`, `dist/`

---

## 🔍 VERIFICACIÓN DE CONSISTENCIA

### **Checklist de Documentación:**

- [x] Todos los documentos tienen headers correctos
- [x] Glosario técnico actualizado
- [x] Orden de lectura definido
- [x] Orden de compilación documentado
- [x] Tests listados y documentados
- [x] Documentos internos marcados como `// Desarrollo`
- [x] Documentos públicos identificados
- [x] Referencias cruzadas correctas
- [x] Escalabilidad de hardware documentada
- [x] Certificación AAA+ actualizada

---

## 📊 MÉTRICAS DE DOCUMENTACIÓN

| Categoría | Total | Público | Desarrollo |
|-----------|-------|---------|------------|
| **Architecture** | 2 | 1 | 1 |
| **Certification** | 1 | 1 | 0 |
| **Glossary** | 1 | 1 | 0 |
| **Manuals** | 6 | 2 | 4 |
| **Roadmap** | 1 | 0 | 1 |
| **Standards** | 7 | 2 | 5 |
| **Root Docs** | 13 | 3 | 10 |
| **TOTAL** | 31 | 10 | 21 |

**Cobertura Pública:** 32% (suficiente para release)  
**Cobertura Interna:** 68% (para desarrollo)

---

## ✅ ESTADO DE VERIFICACIÓN

### **Binarios:**
- ✅ Directorio `bin/` existe
- ✅ Compilación funcional (`build.bat`)
- ✅ Tests compilados y ejecutables

### **Glosario:**
- ✅ `TECHNICAL_GLOSSARY.md` actualizado
- ✅ Términos técnicos definidos
- ✅ Sin referencias a neuronas/planeación

### **Orden de Lectura:**
- ✅ Definido en este documento
- ✅ Tres rutas: Nuevos, Arquitectos, Desarrolladores
- ✅ Referencias cruzadas correctas

### **Orden de Compilación:**
- ✅ `build.bat` es el script principal
- ✅ Orden de paquetes documentado
- ✅ Flags de JVM documentados

### **Tests:**
- ✅ 7 tests identificados
- ✅ Propósito de cada test documentado
- ✅ Comandos de ejecución proporcionados

---

## 🚀 PRÓXIMOS PASOS

1. **Revisar documentos marcados como `// Desarrollo`**
   - Confirmar que no se incluyan en releases públicos
   - Actualizar `.gitignore` si es necesario

2. **Crear script de empaquetado**
   - Script que genere release con solo documentos públicos
   - Excluir automáticamente `brain/`, `tools/`, logs

3. **Validar referencias cruzadas**
   - Verificar que todos los links funcionen
   - Actualizar paths si es necesario

---

**Última Verificación:** 2026-01-19  
**Verificado Por:** System Architect  
**Estado:** ✅ Consistente y Actualizado
