/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Cliente de Métricas AAA+ (Visual Command Center)
 * DEPENDENCIAS: VolcanMetricsServer (Backend Java)
 * MÉTRICAS: Latencia de Polling <16ms (60 FPS), Zero Allocations en Loop
 * 
 * Cliente determinista para consumir métricas del Volcan Engine.
 * PROHIBIDO: Variables genéricas (var x, let data).
 * MANDATO: Simetría Semántica Total con el Backend Java.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-10
 */

// ═══════════════════════════════════════════════════════════════════════════════
// CONFIGURACIÓN DE CONEXIÓN
// ═══════════════════════════════════════════════════════════════════════════════

const METRICS_SERVER_URL = 'http://localhost:8080/metrics';
const POLLING_INTERVAL_MS = 16; // ~60 FPS (16.67ms)

// ═══════════════════════════════════════════════════════════════════════════════
// ESTADO DEL CLIENTE (PRE-ALLOCATED)
// ═══════════════════════════════════════════════════════════════════════════════

let metric_frameCount = 0;
let metric_lastFetchTimestamp = 0;
let metric_connectionStatus = 'DISCONNECTED'; // DISCONNECTED | CONNECTED | ERROR

// ═══════════════════════════════════════════════════════════════════════════════
// MAPEO DE DOM (CACHE DE ELEMENTOS)
// ═══════════════════════════════════════════════════════════════════════════════
// PORQUÉ: Evitar llamadas repetidas a getElementById en el hot-path.
// TÉCNICA: Cache de referencias DOM en el init.

let dom_latencyValue = null;
let dom_cpuCoreValue = null;
let dom_execModeValue = null;
let dom_systemsValue = null;
let dom_execOrderValue = null;
let dom_parallelismValue = null;
let dom_frameCountValue = null;
let dom_adminQueueValue = null;
let dom_layersValue = null;
let dom_terminal = null;

// ═══════════════════════════════════════════════════════════════════════════════
// INICIALIZACIÓN
// ═══════════════════════════════════════════════════════════════════════════════

function initVolcanMetricsClient() {
    console.log('[VOLCAN METRICS CLIENT] Initializing...');

    // Cache DOM elements (Zero-Allocation Strategy)
    dom_latencyValue = document.getElementById('latency');
    dom_cpuCoreValue = document.getElementById('cpu-core');
    dom_execModeValue = document.getElementById('exec-mode');
    dom_systemsValue = document.getElementById('systems');
    dom_execOrderValue = document.getElementById('exec-order');
    dom_parallelismValue = document.getElementById('parallelism');
    dom_frameCountValue = document.getElementById('frame-count');
    dom_adminQueueValue = document.getElementById('admin-queue');
    dom_layersValue = document.getElementById('layers');
    dom_terminal = document.getElementById('terminal');

    // Verificar que todos los elementos existen
    if (!dom_latencyValue || !dom_terminal) {
        console.error('[VOLCAN METRICS CLIENT] CRITICAL: DOM elements not found. Aborting.');
        return;
    }

    console.log('[VOLCAN METRICS CLIENT] DOM cache ready.');

    // Iniciar polling loop
    startPollingLoop();
}

// ═══════════════════════════════════════════════════════════════════════════════
// POLLING LOOP (60 FPS)
// ═══════════════════════════════════════════════════════════════════════════════

function startPollingLoop() {
    console.log('[VOLCAN METRICS CLIENT] Starting polling loop (60 FPS)...');

    setInterval(() => {
        fetchMetricsFromServer();
    }, POLLING_INTERVAL_MS);
}

// ═══════════════════════════════════════════════════════════════════════════════
// FETCH DE MÉTRICAS (HTTP GET)
// ═══════════════════════════════════════════════════════════════════════════════

function fetchMetricsFromServer() {
    const fetch_startTime = performance.now();

    fetch(METRICS_SERVER_URL)
        .then(response_http => {
            if (!response_http.ok) {
                throw new Error(`HTTP ${response_http.status}`);
            }
            return response_http.json();
        })
        .then(payload_metrics => {
            // SIMETRÍA SEMÁNTICA: Las claves JSON deben coincidir con el Backend Java
            updateDOMWithMetrics(payload_metrics);

            metric_connectionStatus = 'CONNECTED';
            metric_lastFetchTimestamp = performance.now();

            const fetch_latency = metric_lastFetchTimestamp - fetch_startTime;
            if (fetch_latency > 16) {
                console.warn(`[VOLCAN METRICS CLIENT] Fetch latency: ${fetch_latency.toFixed(2)}ms (>16ms target)`);
            }
        })
        .catch(error_fetch => {
            metric_connectionStatus = 'ERROR';
            console.error('[VOLCAN METRICS CLIENT] Fetch failed:', error_fetch.message);

            // Mostrar estado de desconexión en el DOM
            if (dom_latencyValue) {
                dom_latencyValue.innerHTML = '<span style="color: var(--alert);">OFFLINE</span>';
            }
        });
}

// ═══════════════════════════════════════════════════════════════════════════════
// ACTUALIZACIÓN DEL DOM (DETERMINISTA)
// ═══════════════════════════════════════════════════════════════════════════════
// PORQUÉ: Mapeo 1:1 entre el JSON del Backend y el DOM del Frontend.
// TÉCNICA: Nombres de variables idénticos a los del Java (SovereignAdmin).

function updateDOMWithMetrics(payload_metrics) {
    // Frame Latency (ns -> μs)
    const metric_frameLatencyNs = payload_metrics.frameLatency || 0;
    const metric_frameLatencyUs = (metric_frameLatencyNs / 1000).toFixed(2);
    dom_latencyValue.innerHTML = `${metric_frameLatencyUs}<span class="metric-unit">μs</span>`;

    // CPU Core
    const metric_cpuCore = payload_metrics.cpuCore || 1;
    dom_cpuCoreValue.textContent = `Core ${metric_cpuCore}`;

    // Execution Mode
    const metric_executionMode = payload_metrics.executionMode || 'Sequential';
    dom_execModeValue.textContent = metric_executionMode;

    // Systems Count
    const metric_systemsCount = payload_metrics.systems || 0;
    dom_systemsValue.innerHTML = `${metric_systemsCount}<span class="metric-unit">systems</span>`;

    // Execution Order
    const metric_executionOrder = payload_metrics.executionOrder || 'Linear';
    dom_execOrderValue.textContent = metric_executionOrder;

    // Parallelism
    const metric_parallelism = payload_metrics.parallelism || 'OFF';
    dom_parallelismValue.textContent = metric_parallelism;

    // Frame Count
    const metric_frameCountBackend = payload_metrics.frameCount || 0;
    dom_frameCountValue.innerHTML = `${metric_frameCountBackend}<span class="metric-unit">frames</span>`;

    // Admin Queue (Simulado por ahora, el backend no lo envía aún)
    // TODO: Agregar adminQueueSize al JSON del Backend
    dom_adminQueueValue.innerHTML = `0<span class="metric-unit">/1024</span>`;

    // Dependency Layers (Derivado del modo de ejecución)
    const metric_dependencyLayers = metric_executionMode.includes('Parallel') ?
        (metric_executionMode.match(/\d+/) ? parseInt(metric_executionMode.match(/\d+/)[0]) : 2) : 0;
    dom_layersValue.innerHTML = `${metric_dependencyLayers}<span class="metric-unit">layers</span>`;

    // Incrementar contador local de frames
    metric_frameCount++;

    // Log en terminal cada 60 frames (~1 segundo)
    if (metric_frameCount % 60 === 0) {
        appendLogToTerminal(`Metrics received: ${metric_frameLatencyUs}μs latency`);
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TERMINAL LOG (VISUAL FEEDBACK)
// ═══════════════════════════════════════════════════════════════════════════════

function appendLogToTerminal(log_message) {
    if (!dom_terminal) return;

    const log_timestamp = new Date().toTimeString().split(' ')[0];

    const log_lineElement = document.createElement('div');
    log_lineElement.className = 'log-line';
    log_lineElement.innerHTML = `<span class="timestamp">[${log_timestamp}]</span> ${log_message}`;

    dom_terminal.appendChild(log_lineElement);

    // Mantener solo últimas 10 líneas (Evitar Memory Leak)
    while (dom_terminal.children.length > 10) {
        dom_terminal.removeChild(dom_terminal.firstChild);
    }

    // Auto-scroll
    dom_terminal.scrollTop = dom_terminal.scrollHeight;
}

// ═══════════════════════════════════════════════════════════════════════════════
// AUTO-INIT (DOMContentLoaded)
// ═══════════════════════════════════════════════════════════════════════════════

if (typeof document !== 'undefined') {
    document.addEventListener('DOMContentLoaded', () => {
        initVolcanMetricsClient();
    });
}
