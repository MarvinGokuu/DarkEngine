# Code Injector (Herramienta de Auditoría)

Este script (`DocInjector.java`) es una herramienta de automatización creada para el mantenimiento del código fuente del motor.

## ¿Qué hace exactamente?
1. Escanea todos los archivos `.java` dentro de la carpeta `src/sv/dark/`.
2. Inyecta automáticamente los comentarios de cabecera (`RESPONSIBILITY`, `WHY`, `TECHNIQUE`, `GUARANTEES`).
3. Agrega la anotación `@AAACertified` a las clases que no la tengan, marcándolas como auditadas para operaciones Zero-GC y Lock-Free.
4. Asegura que el `Reading Order` esté presente al inicio del archivo.

## ¿Por qué está fuera de la carpeta `src/`?
Porque **no es parte del motor Dark-Engine**. 
El código que está dentro de `src/` es estrictamente el código que se compila y se ejecuta en producción para el jugador. Si este archivo estuviera dentro de `src/`, el empaquetador lo compilaría junto al juego y estaríamos enviándole a los jugadores código de "herramientas de desarrollo", lo cual viola los estándares de peso y optimización AAA+. 

Al estar en la carpeta `tools/`, vive exclusivamente para los desarrolladores.

## ¿Es útil todavía o se puede eliminar?
Actualmente todos los archivos principales ya fueron "inyectados" y certificados. Podría eliminarse si el motor no va a crecer más. Sin embargo, se ha conservado en esta carpeta porque, si en el futuro creas 50 clases nuevas, puedes correr este script y certificarlas/documentarlas todas en 1 segundo en lugar de escribir los comentarios a mano una por una.
