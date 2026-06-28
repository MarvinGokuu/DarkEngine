# 🗺️ Mapa del Flujo de Audio Espacial 3D (Capa 3: Magia Sensorial)

El sonido en DarkEngine no es simplemente reproducir pistas estéreo. Cada fuente de sonido (Emisor) y la posición del jugador (Oyente) existen en un espacio 3D matemático. El motor calcula dinámicamente cómo las leyes de la física (Doppler, Distancia) alteran las ondas sonoras antes de llegar a los auriculares del jugador.

<div align="center">

```mermaid
graph TD
    subgraph ecs_state [Estado del Juego: Memoria SoA]
        A[(Coordenadas Oyente XYZ)]
        B[(Coordenadas Emisores XYZ)]
        C[(Velocidades Oyente/Emisor XYZ)]
    end

    subgraph audio_system [DarkAudioSystem: CPU Thread]
        A --> D{Motor Matemático Espacial}
        B --> D
        C -->|Para Efecto Doppler| D
        D -->|Atenuación por Distancia| E(Volumen Final y Paneo)
    end

    subgraph hardware [Capa de Silicio: Tarjeta de Sonido]
        E -->|Llamada FFI C++| F[OpenAL / FMOD Native API]
        F --> G((Mezcla de Sonido Binaural HW))
        G --> H((Auriculares del Jugador))
    end

    %% Estilos AAA
    classDef ecs fill:#27ae60,stroke:#2ecc71,stroke-width:2px,color:#fff;
    classDef audio fill:#2980b9,stroke:#3498db,stroke-width:2px,color:#fff;
    classDef hardware fill:#c0392b,stroke:#e74c3c,stroke-width:3px,color:#fff;

    class A,B,C ecs;
    class D,E audio;
    class F,G,H hardware;
```

</div>

## Leyenda Técnica:
*   **Efecto Doppler:** Si un emisor de sonido viaja hacia el jugador, el motor contrae matemáticamente la onda haciendo que suene más aguda. Si se aleja, suena más grave (como una ambulancia en la vida real).
*   **Paneo Espacial (Binaural):** Si el sonido ocurre a la derecha, la tarjeta de sonido procesa un micro-retraso para que llegue primero al audífono derecho simulando cómo funciona la audición humana.
*   **Atenuación Logarítmica:** El sonido pierde fuerza exponencialmente según el inverso del cuadrado de la distancia, procesado a través de las primitivas de ECS.
