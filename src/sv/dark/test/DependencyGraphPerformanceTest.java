package sv.dark.test;

import sv.dark.kernel.SystemDependencyGraph;
import sv.dark.core.systems.GameSystem;
import sv.dark.state.WorldStateFrame;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Validar que los mapas del SystemDependencyGraph se inicialicen
 * con la capacidad pre-dimensionada correcta para evitar re-hashing durante la construcción del DAG.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-06-03
 */
public class DependencyGraphPerformanceTest {

    public static void main(String[] args) {
        System.out.println("=======================================================");
        System.out.println("  AAA+ CERTIFICATION: DEPENDENCY GRAPH PRE-SIZING");
        System.out.println("=======================================================");

        try {
            SystemDependencyGraph graph = new SystemDependencyGraph();

            // Extraer los mapas mediante reflexión
            Field systemsByNameField = SystemDependencyGraph.class.getDeclaredField("systemsByName");
            systemsByNameField.setAccessible(true);
            Map<?, ?> systemsByName = (Map<?, ?>) systemsByNameField.get(graph);

            Field dependenciesField = SystemDependencyGraph.class.getDeclaredField("dependencies");
            dependenciesField.setAccessible(true);
            Map<?, ?> dependencies = (Map<?, ?>) dependenciesField.get(graph);

            // Agregar un sistema dummy para forzar la inicialización de las tablas de HashMap
            GameSystem dummySystem = new GameSystem() {
                @Override public void update(WorldStateFrame state, double dt) {}
                @Override public String getName() { return "DummySystem"; }
                @Override public String[] getDependencies() { return new String[0]; }
            };
            graph.addSystem(dummySystem);

            // Inspeccionar el tamaño de las tablas internas del HashMap
            Field tableField = Class.forName("java.util.HashMap").getDeclaredField("table");
            tableField.setAccessible(true);

            Object[] systemsTable = (Object[]) tableField.get(systemsByName);
            Object[] depsTable = (Object[]) tableField.get(dependencies);

            int systemsCapacity = systemsTable.length;
            int depsCapacity = depsTable.length;

            System.out.println("[TEST] systemsByName initial table capacity: " + systemsCapacity + " (Expected: 32)");
            System.out.println("[TEST] dependencies initial table capacity: " + depsCapacity + " (Expected: 32)");

            if (systemsCapacity == 32 && depsCapacity == 32) {
                System.out.println("\n[PASSED] SYSTEM DEPENDENCY GRAPH MAPS ARE CORRECTLY PRE-SIZED");
                System.exit(0);
            } else {
                System.err.println("\n[FAILED] SYSTEM DEPENDENCY GRAPH MAPS ARE NOT PRE-SIZED CORRECTLY");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
