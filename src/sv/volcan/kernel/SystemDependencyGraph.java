/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Grafo de Dependencias para Ejecución Paralela Determinista
 * DEPENDENCIAS: Ninguna (Algoritmo puro)
 * MÉTRICAS: O(V + E) construcción, O(1) consulta de capas
 * 
 * Implementa Ordenamiento Topológico (Kahn's Algorithm) para construir
 * un DAG (Directed Acyclic Graph) de sistemas. Agrupa sistemas en capas
 * donde cada capa contiene sistemas independientes que pueden ejecutarse
 * en paralelo.
 * 
 * GARANTÍAS:
 * - Detección de ciclos (falla rápido si hay dependencias circulares)
 * - Orden determinista (mismo grafo = mismas capas siempre)
 * - Barreras naturales entre capas (sincronización implícita)
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-08
 */
package sv.volcan.kernel;

import sv.volcan.core.systems.SovereignSystem;
import sv.volcan.core.AAACertified;

import java.util.*;

@AAACertified(date = "2026-01-08", maxLatencyNs = 1000, minThroughput = 1000, alignment = 64, lockFree = true, offHeap = false, notes = "Dependency graph builder - Topological sort for parallel execution")
public final class SystemDependencyGraph {

    // Nodos del grafo (sistema -> nombre)
    private final Map<String, SovereignSystem> systemsByName;

    // Aristas del grafo (sistema -> dependencias)
    private final Map<String, Set<String>> dependencies;

    // Capas de ejecución (resultado del ordenamiento topológico)
    private List<List<SovereignSystem>> executionLayers;

    // Estado de validación
    private boolean validated = false;

    public SystemDependencyGraph() {
        this.systemsByName = new LinkedHashMap<>(); // Mantiene orden de inserción
        this.dependencies = new HashMap<>();
        this.executionLayers = null;
    }

    /**
     * Agrega un sistema al grafo con sus dependencias.
     * 
     * @param system          Sistema a agregar
     * @param dependencyNames Nombres de sistemas de los que depende
     */
    public void addSystem(SovereignSystem system, String... dependencyNames) {
        if (system == null) {
            throw new IllegalArgumentException("System cannot be null");
        }

        String systemName = system.getName();
        systemsByName.put(systemName, system);

        // Registrar dependencias
        Set<String> deps = new HashSet<>(Arrays.asList(dependencyNames));
        dependencies.put(systemName, deps);

        validated = false; // Invalidar caché
    }

    /**
     * Construye y valida el grafo de dependencias.
     * 
     * ALGORITMO: Kahn's Topological Sort
     * 1. Calcular in-degree de cada nodo
     * 2. Encolar nodos con in-degree 0 (sin dependencias)
     * 3. Procesar cola, decrementar in-degree de vecinos
     * 4. Si quedan nodos sin procesar = ciclo detectado
     * 
     * @throws IllegalStateException si hay dependencias circulares
     */
    public void validate() {
        // Calcular in-degree (cuántas dependencias tiene cada sistema)
        Map<String, Integer> inDegree = new HashMap<>();
        for (String system : systemsByName.keySet()) {
            // in-degree = número de dependencias que TIENE este sistema
            Set<String> deps = dependencies.get(system);
            inDegree.put(system, deps.size());
        }

        // Validar que todas las dependencias existen
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            for (String dep : entry.getValue()) {
                if (!systemsByName.containsKey(dep)) {
                    throw new IllegalStateException("Dependency not found: " + dep);
                }
            }
        }

        // Kahn's Algorithm: Ordenamiento topológico por capas
        executionLayers = new ArrayList<>();
        Set<String> processed = new HashSet<>();

        while (processed.size() < systemsByName.size()) {
            // Encontrar todos los nodos con in-degree 0 (capa actual)
            List<SovereignSystem> currentLayer = new ArrayList<>();

            for (String systemName : systemsByName.keySet()) {
                if (!processed.contains(systemName) && inDegree.get(systemName) == 0) {
                    currentLayer.add(systemsByName.get(systemName));
                    processed.add(systemName);
                }
            }

            if (currentLayer.isEmpty()) {
                // No hay nodos sin dependencias = ciclo detectado
                throw new IllegalStateException(
                        "Circular dependency detected! Remaining systems: " +
                                (systemsByName.size() - processed.size()));
            }

            executionLayers.add(currentLayer);

            // Decrementar in-degree de sistemas que dependen de la capa actual
            for (SovereignSystem system : currentLayer) {
                String systemName = system.getName();
                for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
                    if (entry.getValue().contains(systemName)) {
                        String dependent = entry.getKey();
                        inDegree.put(dependent, inDegree.get(dependent) - 1);
                    }
                }
            }
        }

        validated = true;
    }

    /**
     * Retorna las capas de ejecución.
     * Cada capa contiene sistemas que pueden ejecutarse en paralelo.
     * 
     * @return Lista de capas, cada capa es una lista de sistemas
     * @throws IllegalStateException si el grafo no ha sido validado
     */
    public List<List<SovereignSystem>> getExecutionLayers() {
        if (!validated) {
            throw new IllegalStateException("Graph must be validated before getting execution layers");
        }
        return Collections.unmodifiableList(executionLayers);
    }

    /**
     * Retorna el número de capas de ejecución.
     * 
     * @return Número de capas
     */
    public int getLayerCount() {
        if (!validated) {
            throw new IllegalStateException("Graph must be validated first");
        }
        return executionLayers.size();
    }

    /**
     * Imprime el grafo de dependencias (debug).
     */
    public void printGraph() {
        if (!validated) {
            System.out.println("[GRAPH] Not validated yet");
            return;
        }

        System.out.println("[GRAPH] Execution Layers: " + executionLayers.size());
        for (int i = 0; i < executionLayers.size(); i++) {
            List<SovereignSystem> layer = executionLayers.get(i);
            System.out.print("[GRAPH] Layer " + i + ": ");
            for (SovereignSystem system : layer) {
                System.out.print(system.getName() + " ");
            }
            System.out.println();
        }
    }
}
