# 🗺️ Mapa del Flujo de Telemetría NIO Zero-GC (Capa 4: Traje Espacial)

El sistema de telemetría y diagnóstico web no usa la red tradicional de Java (`java.io` ni `ServerSocket` bloqueantes) porque causarían congelamientos en el servidor del juego.

Toda la transferencia de red se hace a través de NIO (New I/O) y canales asíncronos (`AsynchronousServerSocketChannel`). Esto nos permite servir miles de peticiones HTTP en segundo plano a las herramientas de monitoreo (como Grafana o Prometheus) sin gastar un solo ciclo del reloj del motor físico.

<div align="center">

```mermaid
graph TD
    subgraph game_loop [Motor Físico: 60 FPS]
        A(DarkEngineMaster) -->|Actualiza Punteros| B[(SectorMemoryVault)]
    end

    subgraph nio_gateway [DarkMetricsServer: Hilo Secundario Asíncrono - Puerto 13000]
        C((Cliente Web / Grafana)) -.->|Petición HTTP| D{AsynchronousSocketChannel}
        D -->|Lectura Non-Blocking| E[ByteBuffer Directo Off-Heap]
        E -->|ThreadLocal<byte[]>| F{Parseo de Rutas Zero-GC}
    end

    subgraph response_gen [Generación de Respuesta Estática]
        F -->|Busca Datos| B
        B -->|Lee sin candados| F
        F -->|Header Pre-Compilado| G(ByteBuffer Respuesta)
        G -->|Escritura Non-Blocking| D
    end

    %% Flujos que están prohibidos (rojos)
    H((String Concat)) -.->|PROHIBIDO| response_gen
    I((new Object GC)) -.->|PROHIBIDO| response_gen

    %% Estilos AAA
    classDef engine fill:#1e1e1e,stroke:#f39c12,stroke-width:2px,color:#fff;
    classDef async fill:#2980b9,stroke:#3498db,stroke-width:2px,color:#fff;
    classDef mem fill:#27ae60,stroke:#2ecc71,stroke-width:2px,color:#fff;
    classDef bad fill:#c0392b,stroke:#e74c3c,stroke-width:2px,color:#fff,stroke-dasharray: 5 5;

    class A,B engine;
    class C,D,F async;
    class E,G mem;
    class H,I bad;
```

</div>

## Leyenda Técnica:
*   **AsynchronousSocketChannel:** Escucha y despacha paquetes de red utilizando callbacks del sistema operativo subyacente (epoll en Linux / IOCP en Windows). El juego nunca espera a la red.
*   **Puerto 13000:** Migrado del 8080 para evadir colisiones con web servers (Tomcat/Node) y blindar la conexión.
*   **ThreadLocal Scratchpad:** En lugar de crear bytes temporales, se usa un espacio estático por hilo para decodificar los enteros del Content-Length sin alocar nada.
*   **Header Pre-Compilado:** En lugar de concatenar *Strings* (`"HTTP/1.1 " + status...`) que generarían basura para el GC en cada respuesta, el servidor inyecta arreglos de bytes constantes pre-calculados en memoria.
*   **SectorMemoryVault:** El servidor web lee los datos del motor desde esta bóveda central, garantizando seguridad entre hilos sin usar semáforos bloqueantes.
*   **DarkLogger Zero-GC (Global Audit):** Extensión de la política de cero asignaciones a nivel local; el registro asíncrono utiliza un `ThreadLocal<StringBuilder>` erradicando por completo el `String.format()` tradicional del ecosistema Java.
