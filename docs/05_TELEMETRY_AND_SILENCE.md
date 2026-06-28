# 🤫 05. TELEMETRY AND SILENCE

Un motor de alto rendimiento como DarkEngine tiene una regla inquebrantable de ingeniería: **Ningún byte de información se imprime de forma bloqueante a la consola en Producción.**

El clásico `System.out.println` o `printf` realiza una llamada de I/O bloqueante al kernel del sistema operativo. Esta llamada puede demorar milisegundos completos, destruyendo cualquier ganancia de microsegundos obtenida en la arquitectura paralela.

## La Regla del Silencio de Terminal

En producción, la terminal del DarkEngine debe permanecer limpia tras el arranque. 
Cualquier mensaje, latido, error, o advertencia se deriva al sistema de telemetría asíncrono para asegurar que la vía principal (Hot-Path) del motor jamás experimente Input Lag o caídas de cuadros.

**Directiva de Telemetría Zero-GC Implementada:** `DarkMetricsServer` opera mediante un Gateway NIO asíncrono puro (`AsynchronousServerSocketChannel`). El tráfico HTTP elude completamente las clases tradicionales de Java. Emplea búferes pre-asignados y un *scratchpad* `ThreadLocal<byte[]>` para traducir los dígitos de `Content-Length` directo a ASCII en la memoria nativa, garantizando cero (`0`) alojamientos en el Heap por cada *request* de red procesado.

## DarkLogger: Telemetría Off-Path

El `DarkLogger` es el recolector central de datos de telemetría y diagnósticos.
Su arquitectura de ruteo interno se divide en dos categorías, manejadas con un buffer en anillo (Ring Buffer) asíncrono:

### 1. `logs/darkengine_metrics.log`
Archiva latidos de subsistemas, eventos rutinarios (Info), rendimiento de cuadros (FPS/Frametime) y avisos de carga. Este archivo se sobreescribe para reflejar siempre la última sesión, manteniendo los discos libres de basura histórica inútil.

### 2. `logs/darkengine_errors.log`
Destinado exclusivamente para violaciones de integridad, excepciones atrapadas (`Throwable`), timeouts de bus o de apriete de hilo (`ThreadPinning`). 

## Zero-Blocking Backpressure (Test de Estrés)
La telemetría no debe penalizar nunca al Productor (EngineKernel), incluso si el Consumidor (I/O en Disco) es lento.
Para lograr Mechanical Sympathy total, el bus administrativo (DarkAtomicBus) emplea un modelo asíncrono de anillo donde **las caídas de paquetes son características, no bugs**. 
Un estrés comprobado inyectando 1,000,000 de métricas masivas demostró que el Productor completa la inserción en **<17 milisegundos (Zero-Blocking, Zero-Allocation)**. Cualquier señal rebasada ante cuellos de botella del SO o discos lentos simplemente se descarta silenciosamente para preservar la integridad de los FPS y la simulación AAA+.

## Resumen Final de Ejecución (Shutdown Summary)

Dado que la terminal es silenciada durante el ciclo de vida, ¿cómo sabe el usuario qué ocurrió?
Al momento del apagado (Graceful Shutdown), el `DarkLogger` inyecta una última función atada al `Runtime.getRuntime().addShutdownHook`. Este gancho recolecta si hubo errores durante la ejecución y, **solo al morir el motor**, emite en pantalla un cuadro resumen de diagnóstico ("ERROR SUMMARY") permitiendo al desarrollador saber inmediatamente si todo fue "Verde" o si ocurrió una catástrofe que requiera investigar el archivo de métricas.

## Limpieza Constante: `clean.bat`

El ecosistema debe ser impoluto para evitar conflictos. `clean.bat` destruye de manera quirúrgica la carpeta `bin/` y todo rastro de archivos de logs de la corrida pasada, depositando su huella en `logs/clean.log`. Esto asegura que nunca estés persiguiendo un fantasma de compilaciones viejas.
