package sv.dark.ui;

import java.lang.foreign.MemorySegment;
import sv.dark.core.DarkUIContext;
import sv.dark.core.DarkRHIContext;
import sv.dark.rhi.DarkRHIRendererUI;
import imgui.ImGui;
import imgui.ImGuiIO;

public final class DarkImGuiBackend extends DarkUIContext {

    private DarkRHIRendererUI renderer;

    @Override
    public void init() {
        ImGui.createContext();
        DarkImGuiInput.init();
        renderer = DarkRHIContext.get().getUIRenderer();
        if (renderer != null) {
            renderer.init();
        }
    }

    @Override
    public void newFrame(MemorySegment windowPtr) {
        DarkImGuiInput.newFrame(windowPtr);
        ImGui.newFrame();
    }

    @Override
    public void render() {
        ImGui.render();
        if (renderer != null) {
            renderer.renderDrawData(ImGui.getDrawData());
        }
    }

    @Override
    public int getAFKTier() {
        return DarkImGuiInput.getAFKTier();
    }

    @Override
    public void cleanup() {
        if (renderer != null) {
            renderer.destroy();
        }
        ImGui.destroyContext();
    }
}
