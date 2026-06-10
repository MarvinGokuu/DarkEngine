# Baseline Validation Protocol (A/B/C)

## Objetivo

El **Baseline Validation Protocol** permite medir con precisión el impacto de cada optimización en el DarkEngine y validar que no existen memory leaks.

## Concepto

En alta computación, no puedes saber cuánto estás ganando si no sabes cómo se ve el sistema "en reposo". Sin un Graceful Shutdown, estás operando a ciegas, confiando en que el motor se detuvo cuando en realidad podría haber dejado "hilos fantasma" consumiendo recursos.

## Protocolo A/B/C

### Estado A (Sin Motor)
**Baseline del sistema en reposo**

- Captura métricas del sistema sin el motor activo
- Representa el estado "limpio" del sistema
- Se usa como referencia para detectar leaks

```java
SystemSnapshot stateA = BaselineValidator.captureStateA();
```

### Estado B (Con Motor)
**Medición del impacto durante ejecución**

- Captura métricas con el motor en ejecución
- Permite medir el consumo real del motor
- Útil para validar optimizaciones

```java
SystemSnapshot stateB = BaselineValidator.captureStateB();
long heapImpact = stateB.heapUsedBytes - stateA.heapUsedBytes;
```

### Estado C (Post-Apagado)
**Validación de limpieza completa**

- Captura métricas después del Graceful Shutdown
- Ejecuta ciclo triple de GC para limpieza profunda
- **CRÍTICO**: Si C != A, hay un Memory Leak

```java
SystemSnapshot stateC = BaselineValidator.captureStateC();
boolean clean = BaselineValidator.validateCleanShutdown(stateA, stateC);
```

## Criterios de Validación

### ✅ Heap Memory
```
C <= A + 1MB (tolerancia para Code Cache residual)
```

### ✅ Non-Heap Memory
```
C <= A + 2MB (tolerancia para JIT Compiler)
```

### ✅ Thread Count
```
C == A (no threads fantasma)
```

## Uso Práctico

### Validación de Cambios "en Caliente"

#### Ejemplo 1: Con Padding vs Sin Padding
```java
// Estado A: Capturar baseline
SystemSnapshot stateA = BaselineValidator.captureStateA();

// Estado B1: Motor CON padding
runEngineWithPadding();
SystemSnapshot stateB1 = BaselineValidator.captureStateB();
shutdown();

// Estado C1: Validar limpieza
SystemSnapshot stateC1 = BaselineValidator.captureStateC();

// Estado B2: Motor SIN padding
runEngineWithoutPadding();
SystemSnapshot stateB2 = BaselineValidator.captureStateB();
shutdown();

// Estado C2: Validar limpieza
SystemSnapshot stateC2 = BaselineValidator.captureStateC();

// Comparar impacto
long paddingBenefit = stateB2.heapUsedBytes - stateB1.heapUsedBytes;
System.out.printf("Padding reduce False Sharing en: %.2f MB%n", paddingBenefit / 1_048_576.0);
```

#### Ejemplo 2: Con Filtro de ID vs Sin Filtro
```java
// Validar overhead de seguridad
SystemSnapshot stateA = BaselineValidator.captureStateA();

// Motor CON filtro de seguridad
runEngineWithSecurityFilter();
SystemSnapshot stateB1 = BaselineValidator.captureStateB();

// Motor SIN filtro de seguridad
runEngineWithoutSecurityFilter();
SystemSnapshot stateB2 = BaselineValidator.captureStateB();

// Comparar latencia
long securityOverhead = stateB1.heapUsedBytes - stateB2.heapUsedBytes;
System.out.printf("Filtro de seguridad añade: %.2f MB%n", securityOverhead / 1_048_576.0);
```

## Graceful Shutdown

### Secuencia de Apagado

El motor implementa un **Shutdown Hook** que se ejecuta automáticamente en:
- Ctrl+C
- System.exit()
- Cierre de terminal

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    System.out.println(">>> INICIANDO SECUENCIA DE APAGADO SEGURO...");
    gracefulShutdown();
    System.out.println(">>> MOTOR DARK FUERA DE SISTEMA. GRÁFICOS LIBRES.");
}));
```

### Orden de Cierre

1. **Detener Loop Soberano** → `running = false`
2. **Cerrar EventDispatcher** → Todos los buses de prioridad
3. **Cerrar AdminMetricsBus** → Bus de control
4. **Cerrar FrameArena** → Libera WorldStateFrame
5. **Cerrar StateVault** → Libera MemorySegments
6. **Cerrar SectorVault** → Libera memoria off-heap

### Thread-Safe Shutdown

Para prevenir SIGSEGV (Segmentation Fault), el shutdown sigue la secuencia:

```
Flags → Drain → Validation
```

1. **Cerrar Flags** (`closed = true`) → Bloquea nuevas operaciones
2. **Drain Period** (1ns wait) → Permite que operaciones en curso terminen
3. **Validación Final** → Verifica integridad de memoria

## Interpretación de Resultados

### ✅ Shutdown Limpio
```
Estado A: Heap=50MB, NonHeap=20MB, Threads=10
Estado C: Heap=51MB, NonHeap=21MB, Threads=10
Delta:    Heap=+1MB, NonHeap=+1MB, Threads=0

✅ BASELINE VALIDATION PASSED: Shutdown limpio al 100%
```

### ❌ Memory Leak Detectado
```
Estado A: Heap=50MB, NonHeap=20MB, Threads=10
Estado C: Heap=75MB, NonHeap=30MB, Threads=12
Delta:    Heap=+25MB, NonHeap=+10MB, Threads=+2

❌ HEAP LEAK DETECTED: Delta > 1MB
❌ THREAD LEAK DETECTED: Threads fantasma activos
```

## Troubleshooting

### Problema: Heap Delta > 1MB

**Causa**: Objetos no liberados en memoria heap

**Solución**:
1. Verificar que todos los buses se cierran correctamente
2. Revisar referencias circulares en objetos
3. Validar que no hay listeners activos

### Problema: Non-Heap Delta > 2MB

**Causa**: Memoria nativa (Project Panama) no liberada

**Solución**:
1. Verificar que todos los `Arena.close()` se ejecutan
2. Revisar que no hay `MemorySegment` activos
3. Validar orden de cierre (Arenas antes que Vaults)

### Problema: Thread Delta > 0

**Causa**: Threads fantasma activos

**Solución**:
1. Verificar que el loop soberano se detiene (`running = false`)
2. Revisar que no hay threads en `BLOCK` mode
3. Validar que `Thread.interrupt()` se propaga correctamente

## Ejecución del Test

```bash
# Compilar el proyecto
cd c:\Users\theca\Documents\GitHub\DarkEngine
build.bat

# Ejecutar test de Graceful Shutdown
java -cp bin sv.dark.test.GracefulShutdownTest
```

## Integración con VisualObserver

Para validación visual:

1. **Abrir Monitor de Recursos** (Windows)
2. **Ejecutar Estado A**: Capturar gráficos sin motor
3. **Ejecutar Estado B**: Iniciar motor y observar salto de RAM/CPU
4. **Ejecutar Estado C**: Apagar motor (Ctrl+C) y validar que gráficos vuelven a A

Si los gráficos no vuelven a A, hay un memory leak.

## Conclusión

El Baseline Validation Protocol te permite:

- ✅ Medir el impacto real de cada optimización
- ✅ Detectar memory leaks al 100%
- ✅ Validar que el shutdown es limpio
- ✅ Operar con confianza comercial

**Recuerda**: Si A != C, hay un problema. Si A == C, tu motor es profesional.
