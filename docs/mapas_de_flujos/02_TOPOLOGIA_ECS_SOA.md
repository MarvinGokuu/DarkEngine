# 🗺️ Mapa de Topología ECS: Struct-of-Arrays (Capa 1: Cimientos)

El DarkEngine descarta el paradigma tradicional de Programación Orientada a Objetos (OOP) para las entidades del juego. Evitamos crear objetos `Entity` o `Transform` en el Heap de Java, ya que fragmentan la memoria caché del procesador (AoS - Array of Structs).

En su lugar, el motor almacena los componentes en arreglos gigantes y contiguos de primitivos matemáticos (`Struct-of-Arrays` - SoA) para saturar el bus de memoria y usar instrucciones SIMD (AVX-512).

<div align="center">

```mermaid
graph TD
    subgraph OOP_Mal[El Enfoque Lento: Array of Structs - AoS]
        direction LR
        O1[Entity 1<br/>XYZ] --> O2[Entity 2<br/>XYZ]
        O2 --> O3[Entity 3<br/>XYZ]
        note1>Caché Miss! La CPU<br/>tiene que saltar en RAM]
    end

    subgraph ECS_Bien[La Arquitectura DarkEngine: Struct of Arrays - SoA]
        direction TB
        X[Arreglo Off-Heap X<br/>X1, X2, X3, ..., X1000]
        Y[Arreglo Off-Heap Y<br/>Y1, Y2, Y3, ..., Y1000]
        Z[Arreglo Off-Heap Z<br/>Z1, Z2, Z3, ..., Z1000]
    end

    subgraph CPU_Cache[Canalización a Caché L1/L2]
        L1_X(Carga bloque X1-X16 en 64 bytes)
        L1_Y(Carga bloque Y1-Y16 en 64 bytes)
        SIMD{Registros AVX-512<br/>Procesa 16 entidades a la vez}
    end

    %% Conexiones
    X --> L1_X
    Y --> L1_Y
    L1_X --> SIMD
    L1_Y --> SIMD

    %% Estilos AAA
    classDef bad fill:#c0392b,stroke:#e74c3c,stroke-width:2px,color:#fff;
    classDef good fill:#27ae60,stroke:#2ecc71,stroke-width:3px,color:#fff;
    classDef cpu fill:#8e44ad,stroke:#9b59b6,stroke-width:2px,color:#fff;

    class OOP_Mal,O1,O2,O3 bad;
    class ECS_Bien,X,Y,Z good;
    class CPU_Cache,L1_X,L1_Y,SIMD cpu;
```

</div>

## Leyenda Técnica:
*   **AoS (Array of Structs):** El estilo tradicional (`List<Transform>`). Destruye el rendimiento porque la CPU carga basura a la caché al leer objetos fragmentados.
*   **SoA (Struct of Arrays):** El estilo de DarkEngine. Todos los valores "X" del universo están uno al lado del otro en memoria física.
*   **AVX-512 SIMD:** Al estar alineados, la CPU del servidor puede tomar 16 posiciones "X" a la vez y sumarles velocidad de un solo golpe de reloj, logrando físicas a nivel microscópico sin cuellos de botella.
