# 🧠 01. ARCHITECTURE AND KERNEL

El **DarkEngine Kernel** es el orquestador maestro del ciclo de vida del motor. A diferencia de arquitecturas OOP pesadas, este Kernel opera como un micro-controlador incrustado: asume el control del hilo principal (Thread Pinning), inyecta sistemas en arrays primitivos (para Loop Unrolling), e inicializa subsistemas sin recolección de basura.

## Jerarquía de Ejecución

La cadena de mando está estrictamente jerarquizada para garantizar que ninguna instrucción de usuario interfiera con la telemetría y el pipeline de hardware.

1. **`DarkEngineMaster`**: El "Dios" del sistema. Posee el método `main`. Prepara el terreno (Vault, Dispatcher, Kernel) y arranca el hilo de alta prioridad. Inyecta el `DarkEngineWindow` como UI.
2. **`EngineKernel`**: El procesador central. Atado al núcleo físico (vía `ThreadPinning`). Posee el *Main Loop* de 60 FPS (o deslimitado). Manda señales al `DarkEventDispatcher` y despacha los ticks a los sistemas de usuario.
3. **`SystemRegistry`**: El administrador de memoria y ejecución de código de usuario. Todos los *GameSystems* y *RenderSystems* deben registrarse aquí. 
4. **`SystemStateManager`**: Capa superior para manejar transiciones (Ej: `High Performance Mode` en Windows llamando a `PowrProf.dll`).

## Secuencia de Arranque: Ultra Fast Boot

El motor se niega a arrancar de forma perezosa (Lazy Loading). El arranque, encapsulado en `UltraFastBootSequence`, es un asalto directo a la memoria.

1. **Pre-calentamiento del JIT (C2 Compiler)**: Envía eventos señuelo al `DarkAtomicBus` durante la fase de inicialización.
2. **Integridad Estructural**: Mide la latencia de respuesta del bus usando temporizadores de nanosegundos (`System.nanoTime()`). 
3. **Certificación**: Si el bus no es capaz de responder en < 150ns, el boot emite una advertencia o falla. Si lo logra en < 1ms total, recibe la certificación AAA+.

## SystemRegistry y Loop Unrolling

En lugar de recorrer `ArrayList<System>` utilizando iteradores que contaminan las cachés L1, el `SystemRegistry` consolida todos los sistemas en arreglos primitivos estáticos.
Esto le permite a la JVM realizar "Loop Unrolling", desenrollando los ciclos `for` a nivel de ensamblador, haciendo que llamar a 10 sistemas tome el mismo tiempo que llamar a 1 macro-sistema monolítico. Adicionalmente, los sistemas no guardan estado; operan sobre memorias planas **Structure of Arrays (SoA)** (como `DarkTransformSoA`) utilizando procesadores SIMD para máximo rendimiento.

## GPU-Driven Engine (El Asesino de la CPU)
El Kernel delega por completo las matemáticas espaciales masivas (ej. Frustum Culling) a la tarjeta gráfica. Utilizando enlaces nativos FFI (`DarkOpenGLLinker`), el motor transfiere la memoria cruda del `DarkTransformSoA` hacia *Shader Storage Buffer Objects* (SSBO) en la VRAM. Posteriormente, despacha miles de *Compute Shaders* paralelos que resuelven colisiones y descartes, dejando a la CPU 100% dedicada a la Inteligencia Artificial y reglas de juego.

## El Gobernador de Energía (TimeKeeper)

El `TimeKeeper` implementa un *Governor* mecánico:
- Si el motor termina su trabajo antes de su presupuesto de 16.6ms (para 60FPS), no utiliza `Thread.sleep` (lo cual entrega el hilo al SO y arruina la caché).
- En cambio, hace un *Spin-Wait* dinámico o utiliza un *ParkNanos* agresivo para mantenerse despierto y vigilante. 

## Graceful Shutdown (Zero-Deadlock)
Cuando el motor recibe la orden de detención (ej. el usuario cierra la ventana GLFW), el `SystemStateManager` y el `EngineKernel` cortan la ingesta de nuevos eventos y detienen el Bucle Principal. Posteriormente, el `DarkEventDispatcher` apaga los buses atómicos purificando instantáneamente la memoria sobrante (`clear()`) sin emplear esperas giratorias (*Spin-Waits*), evitando deadlocks que podrían colgar la Máquina Virtual. Finalmente, se cierran las *Arenas*, se libera la memoria *Off-Heap* en el `SectorMemoryVault`, y la tarjeta de audio OpenAL se desconecta del hardware nativamente, garantizando un apagado quirúrgico sin crasheos ni "Zombies".
