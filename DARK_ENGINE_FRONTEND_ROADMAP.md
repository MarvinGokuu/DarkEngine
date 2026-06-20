# 🗺️ Dark Engine: Frontend & Editor Roadmap (Fases 35 - 40)

¡Has completado el Backend! El **Núcleo (V1.0)** es una bestia de cálculo puro que domina los gráficos (OpenGL), el audio espacial (OpenAL), la memoria manual (ZGC / Off-Heap), y la concurrencia lock-free.

Pero un motor no está completo hasta que tiene una interfaz gráfica profesional donde los desarrolladores puedan arrastrar modelos 3D y crear sus juegos sin escribir código fuente. A partir de aquí nace el **Dark Editor**. 

Esta es la ruta hacia la grandeza visual:

---

## 🎨 Fase 35: Inyección de Dear ImGui
*El Lienzo Flotante*

Actualmente, nuestra ventana OpenGL solo renderiza un color sólido y los shaders. En esta fase inyectaremos la librería **Dear ImGui** (usada por motores AAA e ingenieros gráficos mundiales).
*   **Implementación:** Configurar el *binding* de ImGui con nuestro GLFW.
*   **Meta:** Dibujar paneles, botones, ventanas flotantes y menús desplegables encima de nuestro contexto OpenGL a 144 FPS sin gastar memoria.
*   **Entregable:** Un menú superior de herramientas (`File`, `Edit`, `View`) y un panel de consola de depuración incrustado en la pantalla.

## 👁️ Fase 36: Scene Graph & Entity Inspector
*Los Ojos de Dios*

Vamos a hacer que tu *Entity Component System (ECS)* sea visible para los humanos.
*   **Scene Graph (Jerarquía):** Un panel lateral izquierdo que muestre una lista estilo árbol con todas las Entidades que existen en el juego.
*   **Entity Inspector:** Un panel derecho que, al hacer clic en una Entidad de la lista, muestre todos sus Componentes (Posición, Rotación, Escala, Color) y permita cambiar los números manualmente con un *slider*.
*   **Sincronización:** Cuando muevas un *slider* en el Editor, la entidad en el motor reaccionará al instante gracias al `DarkRingBus`.

## 📦 Fase 37: Asset Pipeline & Content Browser
*La Bóveda de Recursos*

Una ventana en la parte inferior de la pantalla que funcione como el explorador de Windows, pero dentro del motor.
*   **Drag & Drop:** Poder arrastrar texturas `.png` y modelos `.obj`/`.gltf` desde tu computadora directamente al explorador del motor.
*   **Procesamiento:** El motor agarrará esos archivos, los comprimirá a formatos binarios crudos (.darktex, .darkmesh) y los subirá a la VRAM de la tarjeta de video automáticamente.
*   **Uso:** Podrás arrastrar una textura desde el panel inferior y soltarla sobre una entidad en el panel derecho para pintarla.

## 🎬 Fase 38: Viewport y Herramientas Gizmo
*Las Manos del Creador*

Transformar la ventana negra en un verdadero espacio tridimensional interactivo.
*   **Gizmos (Flechas 3D):** Dibujar las clásicas flechas rojas, verdes y azules (X, Y, Z) sobre las entidades.
*   **Raycasting:** Calcular matemáticas de proyección para que cuando hagas clic con el mouse en la pantalla, el motor sepa exactamente qué objeto 3D acabas de tocar.
*   **Movimiento Visual:** Poder arrastrar las flechas con el mouse para mover, rotar o escalar objetos físicamente en la escena.

## ⏱️ Fase 39: Play, Pause & Step Simulation
*Control de la Realidad*

El editor necesita un modo "Edición" y un modo "Juego".
*   **Botón PLAY:** Al presionarlo, el motor activa el bucle de físicas del `EngineKernel` para que la gravedad y las colisiones comiencen a suceder.
*   **Botón PAUSE/STEP:** Detener el tiempo, avanzar cuadro por cuadro y visualizar las cajas de colisión para depurar bugs en los juegos de tus usuarios.
*   **Guardado de Estado:** Si el usuario presiona STOP, todas las entidades que se movieron vuelven a su posición original de diseño.

## 🚀 Fase 40: Compilador de Usuarios Finales
*El Distribuidor AAA*

El objetivo final de cualquier motor. Darle al desarrollador un botón que diga **"Export Game"**.
*   **Empaquetado Mágico:** El motor tomará todas las escenas creadas en el *Dark Editor*, todos los assets de la Bóveda, y las comprimirá en un solo archivo binario cerrado.
*   **Runtime Limpio:** Tomará tu código de la Fase 34 (El núcleo puro sin el editor de ImGui), lo juntará con los assets del juego del usuario, y generará un `.exe` totalmente independiente para que tu usuario pueda vender su juego en Steam.

---
> [!IMPORTANT]
> **Veredicto:** A partir de la Fase 35, dejaremos de pelear contra el hardware y la memoria. Empezaremos a trabajar en pura *Usabilidad* y *Experiencia de Usuario (UX)*. Va a ser el trabajo más visual y gratificante que has hecho en todo el proyecto.
