package sv.dark.ui;

import imgui.ImDrawData;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImFontAtlas;
import imgui.type.ImInt;
import sv.dark.core.systems.DarkOpenGLLinker;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import sv.dark.core.DarkLogger;

public final class DarkImGuiRenderer {
    
    private static int g_ShaderHandle = 0;
    private static int g_VertHandle = 0;
    private static int g_FragHandle = 0;
    private static int g_AttribLocationTex = 0, g_AttribLocationProjMtx = 0;
    private static int g_AttribLocationVtxPos = 0, g_AttribLocationVtxUV = 0, g_AttribLocationVtxColor = 0;
    private static int g_VboHandle = 0, g_ElementsHandle = 0;
    private static int g_FontTexture = 0;

    public static void init() {
        createDeviceObjects();
        DarkLogger.info("IMGUI", "Renderer Nativo Inicializado (Project Panama).");
    }

    private static void createDeviceObjects() {
        try (Arena arena = Arena.ofConfined()) {
            int lastTexture = getIntegerv(arena, DarkOpenGLLinker.GL_TEXTURE_BINDING_2D);
            int lastArrayBuffer = getIntegerv(arena, DarkOpenGLLinker.GL_ARRAY_BUFFER_BINDING);
            int lastVertexArray = getIntegerv(arena, DarkOpenGLLinker.GL_VERTEX_ARRAY_BINDING);

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

            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, lastTexture);
            DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_ARRAY_BUFFER, lastArrayBuffer);
            // DarkOpenGLLinker.glBindVertexArray.invokeExact(lastVertexArray);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void createFontsTexture(Arena arena) throws Throwable {
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

    public static void renderDrawData(ImDrawData drawData) {
        if (drawData.getCmdListsCount() == 0) return;
        
        try (Arena arena = Arena.ofConfined()) {
            int fbWidth = (int) (drawData.getDisplaySizeX() * drawData.getFramebufferScaleX());
            int fbHeight = (int) (drawData.getDisplaySizeY() * drawData.getFramebufferScaleY());
            if (fbWidth <= 0 || fbHeight <= 0) return;

            // Setup Render State
            DarkOpenGLLinker.glEnable.invokeExact(DarkOpenGLLinker.GL_BLEND);
            DarkOpenGLLinker.glBlendEquation.invokeExact(DarkOpenGLLinker.GL_FUNC_ADD);
            DarkOpenGLLinker.glBlendFuncSeparate.invokeExact(DarkOpenGLLinker.GL_SRC_ALPHA, DarkOpenGLLinker.GL_ONE_MINUS_SRC_ALPHA, DarkOpenGLLinker.GL_ONE, DarkOpenGLLinker.GL_ONE_MINUS_SRC_ALPHA);
            DarkOpenGLLinker.glDisable.invokeExact(DarkOpenGLLinker.GL_CULL_FACE);
            DarkOpenGLLinker.glDisable.invokeExact(DarkOpenGLLinker.GL_DEPTH_TEST);
            DarkOpenGLLinker.glEnable.invokeExact(DarkOpenGLLinker.GL_SCISSOR_TEST);
            
            DarkOpenGLLinker.glViewport.invokeExact(0, 0, fbWidth, fbHeight);
            
            float L = drawData.getDisplayPosX();
            float R = drawData.getDisplayPosX() + drawData.getDisplaySizeX();
            float T = drawData.getDisplayPosY();
            float B = drawData.getDisplayPosY() + drawData.getDisplaySizeY();
            
            float[] orthoProjection = new float[] {
                2.0f/(R-L),   0.0f,         0.0f,   0.0f,
                0.0f,         2.0f/(T-B),   0.0f,   0.0f,
                0.0f,         0.0f,        -1.0f,   0.0f,
                (R+L)/(L-R),  (T+B)/(B-T),  0.0f,   1.0f
            };
            
            MemorySegment orthoSeg = arena.allocateFrom(ValueLayout.JAVA_FLOAT, orthoProjection);
            DarkOpenGLLinker.glUseProgram.invokeExact(g_ShaderHandle);
            DarkOpenGLLinker.glUniform1i.invokeExact(g_AttribLocationTex, 0);
            DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(g_AttribLocationProjMtx, 1, false, orthoSeg);
            
            MemorySegment vaoPtr = arena.allocate(ValueLayout.JAVA_INT);
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
                ImDrawList cmdList = drawData.getCmdLists()[n];
                
                ByteBuffer vtxBuffer = cmdList.getVtxBufferData();
                ByteBuffer idxBuffer = cmdList.getIdxBufferData();
                
                DarkOpenGLLinker.glBufferData.invokeExact(DarkOpenGLLinker.GL_ARRAY_BUFFER, (long) vtxBuffer.remaining(), MemorySegment.ofBuffer(vtxBuffer), DarkOpenGLLinker.GL_STREAM_DRAW);
                DarkOpenGLLinker.glBufferData.invokeExact(DarkOpenGLLinker.GL_ELEMENT_ARRAY_BUFFER, (long) idxBuffer.remaining(), MemorySegment.ofBuffer(idxBuffer), DarkOpenGLLinker.GL_STREAM_DRAW);
                
                long idxBufferOffset = 0;
                for (int cmd_i = 0; cmd_i < cmdList.getCmdBufferSize(); cmd_i++) {
                    // Extract Texture
                    int texId = cmdList.getCmdBufferTextureId(cmd_i);
                    DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, texId);
                    
                    // Scissor
                    float clipX = cmdList.getCmdBufferClipRectX(cmd_i) - drawData.getDisplayPosX();
                    float clipY = cmdList.getCmdBufferClipRectY(cmd_i) - drawData.getDisplayPosY();
                    float clipZ = cmdList.getCmdBufferClipRectZ(cmd_i) - drawData.getDisplayPosX();
                    float clipW = cmdList.getCmdBufferClipRectW(cmd_i) - drawData.getDisplayPosY();
                    
                    if (clipX < fbWidth && clipY < fbHeight && clipZ >= 0.0f && clipW >= 0.0f) {
                        DarkOpenGLLinker.glScissor.invokeExact((int)(clipX * drawData.getFramebufferScaleX()), 
                                                               (int)((drawData.getDisplaySizeY() - clipW) * drawData.getFramebufferScaleY()), 
                                                               (int)((clipZ - clipX) * drawData.getFramebufferScaleX()), 
                                                               (int)((clipW - clipY) * drawData.getFramebufferScaleY()));
                        DarkOpenGLLinker.glDrawElements.invokeExact(DarkOpenGLLinker.GL_TRIANGLES, cmdList.getCmdBufferElemCount(cmd_i), DarkOpenGLLinker.GL_UNSIGNED_SHORT, idxBufferOffset);
                    }
                    idxBufferOffset += cmdList.getCmdBufferElemCount(cmd_i) * 2L;
                }
            }
            
            // Cleanup state
            // ... omitting for brevity
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static int compileProgram(Arena arena, String vtx, String frag) throws Throwable {
        int v = (int) DarkOpenGLLinker.glCreateShader.invokeExact(DarkOpenGLLinker.GL_COMPUTE_SHADER - 2); // Vertex Shader is 0x8B31
        // We need 0x8B31 for vertex and 0x8B30 for fragment.
        return 0; // TEMPORARY
    }

    private static int getIntegerv(Arena arena, int pname) throws Throwable {
        MemorySegment val = arena.allocate(ValueLayout.JAVA_INT);
        // DarkOpenGLLinker.glGetIntegerv.invokeExact(pname, val);
        return val.get(ValueLayout.JAVA_INT, 0);
    }
}
