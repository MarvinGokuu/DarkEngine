# 🗺️ Mapa de Ciclo de Vida y Empaquetado (Capa 4: Traje Espacial)

Este mapa revela el proceso industrial (Build Pipeline) que ocurre al ejecutar `build_release.bat` y cómo interactúa con nuestro sistema de Integración Continua (GitHub Actions). El código fuente debe ser destilado, ofuscado y fusionado en un único binario ejecutable (`.exe`) sin dependencias externas, asegurando que no se empaquete código de prueba ni el historial del repositorio.

<div align="center">

```mermaid
graph TD
    subgraph codigo_fuente [Código Fuente Multilenguaje]
        A0(Filtro de Exclusión de Tests/JMH) --> A
        A(Java Code)
        B(GLSL Shaders: .vert, .frag, .comp)
        C(C++ Libraries: .dll, .so)
    end

    subgraph compilacion_cruda [1. Compilación Extrema (javac)]
        A -->|javac -g:none| D{Despojado de Símbolos Debug}
        note1>Se borra todo rastro de nombres<br/>de variables para ofuscación y velocidad]
        B --> E{Copiado a Carpeta de Salida}
    end

    subgraph empaquetado_jar [2. Ensamblaje Interno (Fat-JAR)]
        D --> F[DarkEngine-v1.0.jar]
        E -->|Inyección de Shaders| F
        C -->|Inyección de Binarios ImGui| F
    end

    subgraph jpackage [3. Empaquetado Nativo Aislado]
        F --> I[Mover a carpeta 'release_input']
        I --> G{JPackage / JVM Shrinking}
        G -->|Bundling puro sin historial Git| H((Dark-Engine.exe Final))
        C -->|Inyección DLL Externa| H
    end

    %% Estilos AAA
    classDef source fill:#2c3e50,stroke:#34495e,stroke-width:2px,color:#fff;
    classDef javac fill:#c0392b,stroke:#e74c3c,stroke-width:2px,color:#fff;
    classDef jar fill:#f39c12,stroke:#d35400,stroke-width:2px,color:#fff;
    classDef native fill:#27ae60,stroke:#2ecc71,stroke-width:3px,color:#fff;

    class A,B,C source;
    class D,E javac;
    class F,I jar;
    class G,H native;
```

</div>

## Leyenda Técnica de CI/CD:
*   **Filtro de Exclusión de Tests:** Al descubrir los archivos con `dir /s /B`, aplicamos filtros `findstr /v "\test\"` y `\benchmark\` para garantizar que el ejecutable final no incluya código de pruebas, previniendo errores de dependencias (ej. JMH) en producción.
*   **-g:none (Zero Debug):** Parámetro del compilador que elimina intencionalmente el número de líneas y nombres locales para máxima velocidad y ofuscación.
*   **Aislamiento de Input (JPackage):** En lugar de empaquetar el directorio raíz completo (`--input .`), se mueve el JAR a una carpeta aislada (`release_input`). Esto evita que la herramienta intente empaquetar la carpeta oculta `.git` (que pesa gigabytes), lo cual causaba timeouts de 20 minutos en GitHub Actions.
*   **Flujos Paralelos (GitHub Actions):** El proyecto cuenta con un `test-and-merge` (que verifica `build.bat` en cada push) y un `release` (que dispara este flujo de empaquetado final tras llegar a la rama `master`).
