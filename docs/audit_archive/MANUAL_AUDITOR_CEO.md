# MANUAL DEL AUDITOR CEO (MECHANICAL SYMPATHY AAA+)

## 1. Misión y Mindset del Auditor
Como Auditor CEO, tu objetivo **NO** es buscar "código mal escrito" bajo estándares académicos clásicos. Tu misión es cazar **Deuda Técnica Arquitectónica** que impida que DarkEngine escale al nivel de Unreal Engine 5, StarCitizen o el motor RAGE de Rockstar.

Cada hallazgo debe responder a un **¿POR QUÉ?** profundo:
*   *Mal Código Clásico*: "Falta encapsulamiento aquí."
*   *Deuda Técnica CEO*: "Esta variable no tiene padding a 64-bytes. Si dos núcleos acceden simultáneamente, el bus de memoria saturará la caché L3 (False Sharing), destruyendo la latencia en procesadores AMD Threadripper o Intel Core i9."

## 2. Los 4 Pilares de la Auditoría Global

### 2.1. Dominio de Memoria (El Estándar StarCitizen / Rockstar)
*   **Zero-GC Obligatorio**: Cualquier `new` en el hot-path (Kernel loop, render loop, event dispatch) es considerado un **Bug Crítico**. Los AAA mapean la memoria una vez al inicio y reusan mediante Ring Buffers o Contenedores de Objetos.
*   **Project Panama vs JNI**: Las llamadas FFI deben ser seguras y estar pre-enlazadas (Downcall MethodHandles).
*   **I/O Asíncrono Estricto**: Ningún `System.out.print` o acceso a disco debe existir en el hilo lógico. Escribir a consola bloquea el thread y rompe la promesa de latencia sub-milisegundo.

### 2.2. Simpatía Mecánica (El Estándar L1/L2/L3)
*   **False Sharing**: Buscar variables compartidas altamente contendidas sin anotaciones `@Contended` o padding manual explícito (`long p1, p2...`).
*   **Predictibilidad de Saltos (Branch Prediction)**: Evitar branches (ifs) complejos en loops de 10 millones de iteraciones.
*   **Aritmética Bitwise**: `x % 1024` es inaceptable. Se exige `x & 1023`. Las GPUs y CPUs devoran operaciones a nivel de bit, pero el operador módulo castiga los ciclos de reloj.

### 2.3. Matemática y SIMD (El Estándar Vector API)
*   **Destrucción del FPU Overhead**: El uso de `double` y `float` para lógicas simples de colisión sin SIMD es deuda técnica. Debemos asegurar que el código invoca `jdk.incubator.vector` para procesar entidades de 512-bits (8 floats) por ciclo de reloj.

### 2.4. Compatibilidad Cruzada de Plataformas
*   **OS Abstraction**: Verificar que el manejo de afinidad de hilos (Thread Pinning) y temporizadores (TSC / WaitSpin) funcione tanto en el Kernel 6.x de Linux como en el planificador de Windows 11 sin corromper el ecosistema.
*   **APIs Gráficas**: Aislar todo el renderizado gráfico del Kernel lógico. El Kernel no debe saber si hay GLFW, Vulkan, o AWT al otro lado del puente de memoria.

## 3. Protocolo de Ejecución de la Auditoría
1. Analizar el código por bloques lógicos (ej. Core, Bus, Kernel).
2. Documentar las transgresiones en `HALLAZGOS_DE_AUDITORIA_GLOBAL.md`.
3. Por cada bloque completado, generar propuestas usando el `POST_AUDITORIA_SUGERENCIAS.md`.
4. Esperar luz verde antes de alterar el repositorio.
