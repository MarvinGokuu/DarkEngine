import java.lang.foreign.*;

public class TestArena {
    public static void main(String[] args) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ms = arena.allocateFrom(ValueLayout.JAVA_INT, 42);
            System.out.println("SUCCESS");
        } catch (Throwable t) {
            System.out.println("ERROR: " + t.getClass().getName());
        }
    }
}
