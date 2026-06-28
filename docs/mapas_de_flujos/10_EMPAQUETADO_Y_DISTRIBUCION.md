# 🗺️ Mapa de Ciclo de Vida y Empaquetado (Capa 4: Traje Espacial)

Este mapa revela el proceso industrial (Build Pipeline) que ocurre al ejecutar `build_release.bat`. El código fuente escrito en lenguajes mixtos (Java, GLSL, C++) debe ser destilado, ofuscado y fusionado en un único binario ejecutable (`.exe`) sin dependencias externas.

<div align="center">

```mermaid
graph TD
    subgraph codigo_fuente [Código Fuente Multilenguaje]
        A(Java Code)
        B(GLSL Shaders: .vert, .frag, .comp)
        C(C++ Libraries: .dll, .so)
    end

    subgraph compilacion_cruda [1. Compilación Extrema (javac)]
        A -->|javac -g:none| D{Despojado de Símbolos Debug}
        note1>Se borra todo rastro de nombres<br/>de variables para ofuscación y velocidad]
        B --> E{Copiado a Carpeta de Salida}
    end

    subgraph empaquetado_jar [2. Ensamblaje Interno]
        D --> F[DarkEngine.jar]
        E -->|Inyección de Shaders| F
        C -->|Inyección Binaria| F
    end

    subgraph jpackage [3. Empaquetado Nativo Final]
        F --> G{JPackage / JVM Shrinking}
        G -->|Elimina clases de Java no usadas| H((Dark-Engine.exe Final))
    end

    %% Estilos AAA
    classDef source fill:#2c3e50,stroke:#34495e,stroke-width:2px,color:#fff;
    classDef javac fill:#c0392b,stroke:#e74c3c,stroke-width:2px,color:#fff;
    classDef jar fill:#f39c12,stroke:#d35400,stroke-width:2px,color:#fff;
    classDef native fill:#27ae60,stroke:#2ecc71,stroke-width:3px,color:#fff;

    class A,B,C source;
    class D,E javac;
    class F jar;
    class G,H native;
```

</div>

## Leyenda Técnica:
*   **-g:none (Zero Debug):** Parámetro del compilador que elimina intencionalmente el número de líneas y nombres locales. El código corre más rápido al no mantener metadatos en la RAM.
*   **JPackage:** Herramienta oficial del JDK que incrusta una Máquina Virtual microscópica dentro de tu `.exe` para que el jugador final NO tenga que instalar Java en su computadora.
*   **Inyección de Shaders:** El proceso donde nuestros Compute Shaders (`radix_sort.comp`, `light_culling.comp`) son embebidos dentro de la arquitectura del programa para protegerlos de piratería básica.
