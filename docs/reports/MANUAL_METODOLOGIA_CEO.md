# 🦅 MANUAL DE METODOLOGÍA CEO: AUDITORÍA SEMÁNTICA ESTRUCTURAL

Como solicitaste, he construido un motor de auditoría basado en **Python**, pero esta vez **no usa búsquedas ciegas (Regex) tontas**. Regex no entiende si un código está dentro de un bucle `while`, si es parte de un constructor (donde sí es válido instanciar objetos), o si está en el Hot-Path (el ciclo principal a 60 FPS).

Para realizar la auditoría del 100% del proyecto de forma ininterrumpida y profunda, he diseñado un analizador que construye un **AST (Abstract Syntax Tree)** del código Java usando la librería `javalang`.

## ¿Cómo funciona el nuevo motor de auditoría?

1. **Análisis de Contexto (El "Por Qué")**:
   El script lee el archivo Java y lo transforma en un árbol de nodos (AST). Esto me permite saber exactamente _dónde_ está ocurriendo una acción.
2. **Definición de Hot-Paths**:
   El script identifica métodos críticos como `update()`, `tick()`, `run()`, `offer()`, `poll()` o bucles `while(running)`. Cualquier cosa que ocurra aquí sucede millones de veces.

3. **Criterios de Búsqueda y Reglas de Negocio**:
   Basado en tu arquitectura AAA (_Mechanical Sympathy_, _Zero-Allocation_, _Data-Oriented Design_), busco las siguientes violaciones arquitectónicas:
   - **Falso Zero-Garbage (Creación de Objetos en Hot-Path)**:
     Busca nodos `ClassCreator` (ej. `new String()`, `new Vector3f()`) o llamadas a `.split()`, `.getBytes()`, `.toString()` **únicamente** si están dentro de un método Hot-Path. (Crear objetos en el constructor es perfectamente legal y el script lo ignora).
   - **Dead-Locks por Spin-Wait sin Timeout**:
     Busca nodos de bucles (`while`, `for`) que contengan `Thread.onSpinWait()` o `LockSupport.parkNanos()`. Luego, verifica si dentro del mismo bucle existe alguna condición de ruptura (`break`, `return`, `throw`) ligada a un contador de "backoff". Si no hay límite, es un Dead-Lock potencial en caso de que el consumidor muera.

   - **Bloqueo I/O en Hilos Críticos**:
     Busca llamadas a `System.out.print`, `java.io.File`, o `java.net.Socket` dentro de los métodos del bucle principal o los Dispatchers del Bus. El I/O debe ser delegado a hilos secundarios.

   - **Divisiones FPU Innecesarias**:
     Busca operadores `/` dentro de cálculos vectoriales o bucles pesados. (Validando como tú hiciste: es mejor `* 0.01f` que `/ 100.0f`).

## Ejecución Continua

El script se ejecuta de forma ininterrumpida en segundo plano y recorre el 100% de los archivos `.java`. Extrae el archivo, la línea, el nombre del método contenedor y la razón arquitectónica de la deuda técnica.

Finalmente, vuelca todos los resultados en el reporte global. Esta es una auditoría real de nivel CEO.
