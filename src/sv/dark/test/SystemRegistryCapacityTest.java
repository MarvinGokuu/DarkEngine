package sv.dark.test;

import sv.dark.kernel.SystemRegistry;
import java.lang.reflect.Field;
import java.util.List;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Validar que las colecciones del SystemRegistry se inicialicen
 * con la capacidad pre-dimensionada correcta para evitar reasignaciones en el hot-path.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-06-03
 */
public class SystemRegistryCapacityTest {

    public static void main(String[] args) {
        System.out.println("=======================================================");
        System.out.println("  AAA+ CERTIFICATION: SYSTEM REGISTRY PRE-SIZING");
        System.out.println("=======================================================");

        try {
            SystemRegistry registry = new SystemRegistry();

            // Extraer gameSystems
            Field gameSystemsField = SystemRegistry.class.getDeclaredField("gameSystems");
            gameSystemsField.setAccessible(true);
            List<?> gameSystems = (List<?>) gameSystemsField.get(registry);

            // Extraer renderSystems
            Field renderSystemsField = SystemRegistry.class.getDeclaredField("renderSystems");
            renderSystemsField.setAccessible(true);
            List<?> renderSystems = (List<?>) renderSystemsField.get(registry);

            // Inspeccionar la capacidad de la ArrayList a través del campo privado 'elementData'
            Field elementDataField = Class.forName("java.util.ArrayList").getDeclaredField("elementData");
            elementDataField.setAccessible(true);

            Object[] gameSystemsBackingArray = (Object[]) elementDataField.get(gameSystems);
            Object[] renderSystemsBackingArray = (Object[]) elementDataField.get(renderSystems);

            int gameCapacity = gameSystemsBackingArray.length;
            int renderCapacity = renderSystemsBackingArray.length;

            System.out.println("[TEST] gameSystems collection initial capacity: " + gameCapacity + " (Expected: 16)");
            System.out.println("[TEST] renderSystems collection initial capacity: " + renderCapacity + " (Expected: 8)");

            if (gameCapacity == 16 && renderCapacity == 8) {
                System.out.println("\n[PASSED] SYSTEM REGISTRY COLLECTIONS ARE CORRECTLY PRE-SIZED");
                System.exit(0);
            } else {
                System.err.println("\n[FAILED] SYSTEM REGISTRY COLLECTIONS ARE NOT PRE-SIZED CORRECTLY");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
