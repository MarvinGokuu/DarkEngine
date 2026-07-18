package sv.dark.rhi;

import imgui.ImDrawData;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImFontAtlas;
import imgui.type.ImInt;
import imgui.ImVec4;
import sv.dark.core.systems.DarkOpenGLLinker;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import sv.dark.core.DarkLogger;

public final class DarkOpenGLImGuiBackend implements DarkRHIRendererUI {
    
    private int g_ShaderHandle = 0;
    private int g_AttribLocationTex = 0, g_AttribLocationProjMtx = 0;
    private int g_VboHandle = 0, g_ElementsHandle = 0;
    private int g_FontTexture = 0;
    private final float[] orthoProjection = new float[16];

    @Override
    public void init() {
        createDeviceObjects();
        DarkLogger.info("IMGUI", "Renderer Nativo Inicializado (Project Panama + imgui-java).");
    }

    private void createDeviceObjects() {
        try (Arena arena = Arena.ofConfined()) {
            String vertexShader = 
                "#version 330 core\n" +
                "layout (location = 0) in vec2 Position;\n" +
                "layout (location = 1) in vec2 UV;\n" +
                "layout (location = 2) in vec4 Color;\n" +
                "uniform mat4 ProjMtx;\n" +
                "out vec2 Frag_UV;\n" +
                "out vec4 Frag_Color;\n" +
                "void main() {\n" +
                "    Frag_UV = UV;\n" +
                "    Frag_Color = Color;\n" +
                "    gl_Position = ProjMtx * vec4(Position.xy,0,1);\n" +
                "}\n";

            String fragmentShader = 
                "#version 330 core\n" +
                "in vec2 Frag_UV;\n" +
                "in vec4 Frag_Color;\n" +
                "uniform sampler2D Texture;\n" +
                "out vec4 Out_Color;\n" +
                "void main() {\n" +
                "    Out_Color = Frag_Color * texture(Texture, Frag_UV.st);\n" +
                "}\n";

            g_ShaderHandle = compileProgram(arena, vertexShader, fragmentShader);

            MemorySegment projName = arena.allocateFrom("ProjMtx");
            g_AttribLocationProjMtx = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(g_ShaderHandle, projName);
            MemorySegment texName = arena.allocateFrom("Texture");
            g_AttribLocationTex = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(g_ShaderHandle, texName);

            MemorySegment buffers = arena.allocate(ValueLayout.JAVA_INT, 2);
            DarkOpenGLLinker.glGenBuffers.invokeExact(2, buffers);
            g_VboHandle = buffers.getAtIndex(ValueLayout.JAVA_INT, 0);
            g_ElementsHandle = buffers.getAtIndex(ValueLayout.JAVA_INT, 1);

            createFontsTexture(arena);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void createFontsTexture(Arena arena) throws Throwable {
        ImGuiIO io = ImGui.getIO();
        ImFontAtlas fonts = io.getFonts();
        
        ImInt width = new ImInt();
        ImInt height = new ImInt();
        ByteBuffer pixels = fonts.getTexDataAsRGBA32(width, height);
        
        MemorySegment texPtr = arena.allocate(ValueLayout.JAVA_INT);
        DarkOpenGLLinker.glGenTextures.invokeExact(1, texPtr);
        g_FontTexture = texPtr.get(ValueLayout.JAVA_INT, 0);
        
        DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, g_FontTexture);
        DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkOpenGLLinker.GL_TEXTURE_MIN_FILTER, DarkOpenGLLinker.GL_LINEAR);
        DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkOpenGLLinker.GL_TEXTURE_MAG_FILTER, DarkOpenGLLinker.GL_LINEAR);
        DarkOpenGLLinker.glPixelStorei.invokeExact(DarkOpenGLLinker.GL_UNPACK_ROW_LENGTH, 0);
        
        MemorySegment pixelsSeg = MemorySegment.ofBuffer(pixels);
        DarkOpenGLLinker.glTexImage2D.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, 0, DarkOpenGLLinker.GL_RGBA, width.get(), height.get(), 0, DarkOpenGLLinker.GL_RGBA, DarkOpenGLLinker.GL_UNSIGNED_BYTE, pixelsSeg);
        
        fonts.setTexID(g_FontTexture);
    }

    @Override
    public void renderDrawData(ImDrawData drawData) {
        if (drawData == null || drawData.getCmdListsCount() == 0) return;
        
        try {
            int fbWidth = (int) (drawData.getDisplaySizeX() * drawData.getFramebufferScaleX());
            int fbHeight = (int) (drawData.getDisplaySizeY() * drawData.getFramebufferScaleY());
            if (fbWidth <= 0 || fbHeight <= 0) return;

            // Setup Render State
            DarkOpenGLLinker.glEnable.invokeExact(DarkOpenGLLinker.GL_BLEND);
            DarkOpenGLLinker.glBlendEquation.invokeExact(DarkOpenGLLinker.GL_FUNC_ADD);
            DarkOpenGLLinker.glBlendFuncSeparate.invokeExact(DarkOpenGLLinker.GL_SRC_ALPHA, DarkOpenGLLinker.GL_ONE_MINUS_SRC_ALPHA, 1 /*GL_ONE*/, DarkOpenGLLinker.GL_ONE_MINUS_SRC_ALPHA);
            DarkOpenGLLinker.glDisable.invokeExact(DarkOpenGLLinker.GL_CULL_FACE);
            DarkOpenGLLinker.glDisable.invokeExact(DarkOpenGLLinker.GL_DEPTH_TEST);
            DarkOpenGLLinker.glEnable.invokeExact(DarkOpenGLLinker.GL_SCISSOR_TEST);
            
            DarkOpenGLLinker.glViewport.invokeExact(0, 0, fbWidth, fbHeight);
            
            float L = drawData.getDisplayPosX();
            float R = drawData.getDisplayPosX() + drawData.getDisplaySizeX();
            float T = drawData.getDisplayPosY();
            float B = drawData.getDisplayPosY() + drawData.getDisplaySizeY();
            
            orthoProjection[0] = 2.0f/(R-L);   orthoProjection[1] = 0.0f;         orthoProjection[2] = 0.0f;   orthoProjection[3] = 0.0f;
            orthoProjection[4] = 0.0f;         orthoProjection[5] = 2.0f/(T-B);   orthoProjection[6] = 0.0f;   orthoProjection[7] = 0.0f;
            orthoProjection[8] = 0.0f;         orthoProjection[9] = 0.0f;         orthoProjection[10] = -1.0f;  orthoProjection[11] = 0.0f;
            orthoProjection[12] = (R+L)/(L-R); orthoProjection[13] = (T+B)/(B-T); orthoProjection[14] = 0.0f;  orthoProjection[15] = 1.0f;
            
            sv.dark.scene.DarkRenderScratchpad.writeMatrix(orthoProjection);
            DarkOpenGLLinker.glUseProgram.invokeExact(g_ShaderHandle);
            DarkOpenGLLinker.glUniform1i.invokeExact(g_AttribLocationTex, 0);
            DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(g_AttribLocationProjMtx, 1, false, sv.dark.scene.DarkRenderScratchpad.MATRIX_64B);
            
            MemorySegment vaoPtr = sv.dark.scene.DarkRenderScratchpad.INT_4B;
            DarkOpenGLLinker.glGenVertexArrays.invokeExact(1, vaoPtr);
            int vao = vaoPtr.get(ValueLayout.JAVA_INT, 0);
            DarkOpenGLLinker.glBindVertexArray.invokeExact(vao);
            
            DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_ARRAY_BUFFER, g_VboHandle);
            DarkOpenGLLinker.glEnableVertexAttribArray.invokeExact(0);
            DarkOpenGLLinker.glEnableVertexAttribArray.invokeExact(1);
            DarkOpenGLLinker.glEnableVertexAttribArray.invokeExact(2);
            DarkOpenGLLinker.glVertexAttribPointer.invokeExact(0, 2, DarkOpenGLLinker.GL_FLOAT, false, 20, 0L);
            DarkOpenGLLinker.glVertexAttribPointer.invokeExact(1, 2, DarkOpenGLLinker.GL_FLOAT, false, 20, 8L);
            DarkOpenGLLinker.glVertexAttribPointer.invokeExact(2, 4, DarkOpenGLLinker.GL_UNSIGNED_BYTE, true, 20, 16L);
            
            DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_ELEMENT_ARRAY_BUFFER, g_ElementsHandle);
            
            for (int n = 0; n < drawData.getCmdListsCount(); n++) {
                ByteBuffer vtxBuffer = drawData.getCmdListVtxBufferData(n);
                ByteBuffer idxBuffer = drawData.getCmdListIdxBufferData(n);
                
                DarkOpenGLLinker.glBufferData.invokeExact(DarkOpenGLLinker.GL_ARRAY_BUFFER, (long) vtxBuffer.remaining(), MemorySegment.ofBuffer(vtxBuffer), 0x88E0 /* GL_STREAM_DRAW */);
                DarkOpenGLLinker.glBufferData.invokeExact(DarkOpenGLLinker.GL_ELEMENT_ARRAY_BUFFER, (long) idxBuffer.remaining(), MemorySegment.ofBuffer(idxBuffer), 0x88E0 /* GL_STREAM_DRAW */);
                
                for (int cmd_i = 0; cmd_i < drawData.getCmdListCmdBufferSize(n); cmd_i++) {
                    int texId = drawData.getCmdListCmdBufferTextureId(n, cmd_i);
                    DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, texId);
                    
                    ImVec4 clipRect = drawData.getCmdListCmdBufferClipRect(n, cmd_i);
                    float clipX = clipRect.x - drawData.getDisplayPosX();
                    float clipY = clipRect.y - drawData.getDisplayPosY();
                    float clipZ = clipRect.z - drawData.getDisplayPosX();
                    float clipW = clipRect.w - drawData.getDisplayPosY();
                    
                    if (clipX < fbWidth && clipY < fbHeight && clipZ >= 0.0f && clipW >= 0.0f) {
                        DarkOpenGLLinker.glScissor.invokeExact((int)(clipX * drawData.getFramebufferScaleX()), 
                                                               (int)((drawData.getDisplaySizeY() - clipW) * drawData.getFramebufferScaleY()), 
                                                               (int)((clipZ - clipX) * drawData.getFramebufferScaleX()), 
                                                               (int)((clipW - clipY) * drawData.getFramebufferScaleY()));
                        int idxOffset = drawData.getCmdListCmdBufferIdxOffset(n, cmd_i);
                        int elemCount = drawData.getCmdListCmdBufferElemCount(n, cmd_i);
                        DarkOpenGLLinker.glDrawElements.invokeExact(DarkOpenGLLinker.GL_TRIANGLES, elemCount, DarkOpenGLLinker.GL_UNSIGNED_SHORT, (long)(idxOffset * 2));
                    }
                }
            }
            DarkOpenGLLinker.glDeleteVertexArrays.invokeExact(1, vaoPtr);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static int compileProgram(Arena arena, String vtx, String frag) throws Throwable {
        int vertexShader = (int) DarkOpenGLLinker.glCreateShader.invokeExact(0x8B31);
        MemorySegment vtxPtr = arena.allocateFrom(vtx);
        MemorySegment vtxArrayPtr = arena.allocateFrom(ValueLayout.ADDRESS, vtxPtr);
        DarkOpenGLLinker.glShaderSource.invokeExact(vertexShader, 1, vtxArrayPtr, MemorySegment.NULL);
        DarkOpenGLLinker.glCompileShader.invokeExact(vertexShader);

        int fragmentShader = (int) DarkOpenGLLinker.glCreateShader.invokeExact(0x8B30);
        MemorySegment fragPtr = arena.allocateFrom(frag);
        MemorySegment fragArrayPtr = arena.allocateFrom(ValueLayout.ADDRESS, fragPtr);
        DarkOpenGLLinker.glShaderSource.invokeExact(fragmentShader, 1, fragArrayPtr, MemorySegment.NULL);
        DarkOpenGLLinker.glCompileShader.invokeExact(fragmentShader);

        int program = (int) DarkOpenGLLinker.glCreateProgram.invokeExact();
        DarkOpenGLLinker.glAttachShader.invokeExact(program, vertexShader);
        DarkOpenGLLinker.glAttachShader.invokeExact(program, fragmentShader);
        DarkOpenGLLinker.glLinkProgram.invokeExact(program);
        return program;
    }

    @Override
    public void destroy() {
        try {
            if (g_ShaderHandle != 0) {
                DarkOpenGLLinker.glDeleteProgram.invokeExact(g_ShaderHandle);
                g_ShaderHandle = 0;
            }
        } catch (Throwable e) {
            DarkLogger.fatal("IMGUI", "Error al destruir shader de ImGui", e);
        }
    }
}
