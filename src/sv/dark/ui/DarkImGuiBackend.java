package sv.dark.ui;

import java.lang.foreign.MemorySegment;
import sv.dark.core.DarkUIContext;
import imgui.ImGui;
import imgui.ImGuiIO;

public final class DarkImGuiBackend extends DarkUIContext {

    @Override
    public void init() {
        ImGui.createContext();
        DarkImGuiInput.init();
        DarkImGuiRenderer.init();
    }

    @Override
    public void newFrame(MemorySegment windowPtr) {
        DarkImGuiInput.newFrame(windowPtr);
        ImGui.newFrame();
    }

    @Override
    public void render() {
        ImGui.render();
        DarkImGuiRenderer.renderDrawData(ImGui.getDrawData());
    }

    @Override
    public int getAFKTier() {
        return DarkImGuiInput.getAFKTier();
    }

    @Override
    public void cleanup() {
        DarkImGuiRenderer.destroy();
        ImGui.destroyContext();
    }
}
