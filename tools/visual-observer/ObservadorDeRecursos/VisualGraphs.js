/**
 * VISUAL OBSERVER - GRAPH RENDERER
 * Handles real-time drawing of canvas graphs for metrics.
 */

class MetricGraph {
    constructor(canvasId, color) {
        this.canvas = document.getElementById(canvasId);
        if (!this.canvas) return;

        this.ctx = this.canvas.getContext('2d');
        this.color = color;
        this.data = new Array(60).fill(0); // 60 frames history
        this.width = this.canvas.width = this.canvas.offsetWidth;
        this.height = this.canvas.height = 100; // Fixed height

        // Resize observer
        window.addEventListener('resize', () => {
            this.width = this.canvas.width = this.canvas.offsetWidth;
        });
    }

    push(value) {
        this.data.push(value);
        this.data.shift();
        this.draw();
    }

    draw() {
        if (!this.ctx) return;
        const w = this.width;
        const h = this.height;
        const ctx = this.ctx;

        ctx.clearRect(0, 0, w, h);

        ctx.beginPath();
        ctx.strokeStyle = this.color;
        ctx.lineWidth = 2;
        ctx.lineJoin = 'round';

        const step = w / (this.data.length - 1);

        // Find max for auto-scaling (min 100 for visibility)
        const max = Math.max(10, ...this.data) * 1.2;

        this.data.forEach((val, i) => {
            const x = i * step;
            const y = h - ((val / max) * h);
            if (i === 0) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
        });

        ctx.stroke();

        // Fill area
        ctx.lineTo(w, h);
        ctx.lineTo(0, h);
        ctx.fillStyle = this.color.replace('rgb', 'rgba').replace(')', ', 0.1)'); // Simple hack if rgb
        if (this.color.startsWith('#')) {
            ctx.fillStyle = this.hexToRgba(this.color, 0.1);
        }
        ctx.fill();
    }

    hexToRgba(hex, alpha) {
        const r = parseInt(hex.slice(1, 3), 16);
        const g = parseInt(hex.slice(3, 5), 16);
        const b = parseInt(hex.slice(5, 7), 16);
        return `rgba(${r}, ${g}, ${b}, ${alpha})`;
    }
}

// Initialize logic
const graphs = {};

window.addEventListener('DOMContentLoaded', () => {
    // CPU Graph (Neon Green)
    graphs.cpu = new MetricGraph('graph-cpu', '#00ff41');

    // Memory Graph (Neon Purple)
    graphs.mem = new MetricGraph('graph-mem', '#b400ff');

    // Disk Graph (Neon Blue)
    graphs.disk = new MetricGraph('graph-disk', '#00d4ff');

    // Network Graph (Neon Orange)
    graphs.net = new MetricGraph('graph-net', '#ff9900');

    // Simulate / Connect data
    // Ideally VolcanMetricsClient would call this. 
    // For now we hook into the global update if possible, or poll.
});

// Global hook for the MetricsClient to call
window.updateGraph = function (key, value) {
    if (graphs[key]) graphs[key].push(value);
};
