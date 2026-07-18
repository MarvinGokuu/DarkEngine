package sv.dark.rhi;

import imgui.ImDrawData;

/**
 * Interfaz genérica para renderizadores de Interfaz de Usuario (UI).
 * Permite inyectar backends específicos (OpenGL, Vulkan) sin acoplar
 * el módulo UI de alto nivel a llamadas gráficas crudas.
 */
public interface DarkRHIRendererUI {
    /**
     * Inicializa los recursos del backend de UI (shaders, texturas, buffers).
     */
    void init();

    /**
     * Renderiza un frame de UI basado en los datos de dibujo.
     * @param drawData Datos de vértices e índices generados por la UI.
     */
    void renderDrawData(ImDrawData drawData);

    /**
     * Libera todos los recursos gráficos del backend de UI.
     */
    void destroy();
}
