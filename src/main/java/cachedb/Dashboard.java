package cachedb;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Built-in dashboard for CacheDB monitoring
 */
public class Dashboard {

    private final CacheDB cache;
    private final CacheStore store;
    private final int port;
    private HttpServer server;
    private ScheduledExecutorService scheduler;
    private final DashboardStats stats;
    private final long startTime;

    public Dashboard(CacheDB cache, CacheStore store, int port) {
        this.cache = cache;
        this.store = store;
        this.port = port;
        this.stats = new DashboardStats();
        this.startTime = System.currentTimeMillis();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", new DashboardHandler());
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/api/operations", new OperationsHandler());
        server.createContext("/api/wal", new WALHandler());

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        // Start background stats collection
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::updateStats, 0, 1, TimeUnit.SECONDS);

        System.out.println("✓ Dashboard available at: http://localhost:" + port);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    public void recordRead() {
        stats.readOperations++;
        stats.totalOperations++;
        stats.cacheHits++; // Simplified - assumes cache hit
    }

    public void recordWrite() {
        stats.writeOperations++;
        stats.totalOperations++;
    }

    public void recordDelete() {
        stats.deleteOperations++;
        stats.totalOperations++;
    }

    public void recordMiss() {
        stats.cacheMisses++;
    }

    private void updateStats() {
        stats.updateTime = System.currentTimeMillis();
        stats.uptime = (stats.updateTime - startTime) / 1000;
    }

    private String getStatsJSON() {
        return String.format(
            "{\"uptime\":%d,\"totalOperations\":%d,\"readOperations\":%d,\"writeOperations\":%d,\"deleteOperations\":%d,\"cacheHits\":%d,\"cacheMisses\":%d,\"hitRate\":%.2f,\"updateTime\":%d}",
            stats.uptime,
            stats.totalOperations,
            stats.readOperations,
            stats.writeOperations,
            stats.deleteOperations,
            stats.cacheHits,
            stats.cacheMisses,
            stats.getHitRate(),
            stats.updateTime
        );
    }

    private String getOperationsJSON() {
        return String.format(
            "{\"reads\":%d,\"writes\":%d,\"deletes\":%d,\"total\":%d}",
            stats.readOperations,
            stats.writeOperations,
            stats.deleteOperations,
            stats.totalOperations
        );
    }

    private String getWALJSON() {
        Path walPath = Paths.get("logs", "wal.log");
        long walSize = 0;
        boolean exists = false;
        try {
            if (Files.exists(walPath)) {
                exists = true;
                walSize = Files.size(walPath);
            }
        } catch (Exception e) {
            // Ignore
        }
        return String.format(
            "{\"exists\":%s,\"size\":%d,\"sizeFormatted\":\"%s\"}",
            exists,
            walSize,
            formatBytes(walSize)
        );
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    private void sendResponse(HttpExchange exchange, int status, String response, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    static class DashboardStats {
        long uptime = 0;
        long totalOperations = 0;
        long readOperations = 0;
        long writeOperations = 0;
        long deleteOperations = 0;
        long cacheHits = 0;
        long cacheMisses = 0;
        long updateTime = System.currentTimeMillis();

        double getHitRate() {
            long total = cacheHits + cacheMisses;
            if (total == 0) return 0.0;
            return (cacheHits * 100.0) / total;
        }
    }

    class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String html = getDashboardHTML();
                sendResponse(exchange, 200, html, "text/html");
            } else {
                sendResponse(exchange, 405, "Method not allowed", "text/plain");
            }
        }
    }

    class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String json = getStatsJSON();
                sendResponse(exchange, 200, json, "application/json");
            } else {
                sendResponse(exchange, 405, "Method not allowed", "text/plain");
            }
        }
    }

    class OperationsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String json = getOperationsJSON();
                sendResponse(exchange, 200, json, "application/json");
            } else {
                sendResponse(exchange, 405, "Method not allowed", "text/plain");
            }
        }
    }

    class WALHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String json = getWALJSON();
                sendResponse(exchange, 200, json, "application/json");
            } else {
                sendResponse(exchange, 405, "Method not allowed", "text/plain");
            }
        }
    }

    private String getDashboardHTML() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CacheDB Dashboard</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #333;
            min-height: 100vh;
            padding: 20px;
        }
        
        .container {
            max-width: 1400px;
            margin: 0 auto;
        }
        
        .header {
            background: white;
            border-radius: 12px;
            padding: 30px;
            margin-bottom: 20px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
        
        .header h1 {
            color: #667eea;
            font-size: 32px;
            margin-bottom: 10px;
        }
        
        .header .subtitle {
            color: #666;
            font-size: 16px;
        }
        
        .status-indicator {
            display: inline-block;
            width: 12px;
            height: 12px;
            border-radius: 50%;
            background: #10b981;
            margin-right: 8px;
            animation: pulse 2s infinite;
        }
        
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.5; }
        }
        
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 20px;
        }
        
        .stat-card {
            background: white;
            border-radius: 12px;
            padding: 25px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            transition: transform 0.2s;
        }
        
        .stat-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 12px rgba(0,0,0,0.15);
        }
        
        .stat-card h3 {
            color: #666;
            font-size: 14px;
            text-transform: uppercase;
            letter-spacing: 1px;
            margin-bottom: 10px;
        }
        
        .stat-card .value {
            font-size: 36px;
            font-weight: bold;
            color: #667eea;
            margin-bottom: 5px;
        }
        
        .stat-card .sub-value {
            color: #999;
            font-size: 14px;
        }
        
        .section {
            background: white;
            border-radius: 12px;
            padding: 25px;
            margin-bottom: 20px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
        
        .section h2 {
            color: #333;
            font-size: 20px;
            margin-bottom: 20px;
            padding-bottom: 10px;
            border-bottom: 2px solid #f0f0f0;
        }
        
        .metric-row {
            display: flex;
            justify-content: space-between;
            padding: 12px 0;
            border-bottom: 1px solid #f0f0f0;
        }
        
        .metric-row:last-child {
            border-bottom: none;
        }
        
        .metric-label {
            color: #666;
            font-weight: 500;
        }
        
        .metric-value {
            color: #333;
            font-weight: bold;
        }
        
        .progress-bar {
            width: 100%;
            height: 8px;
            background: #f0f0f0;
            border-radius: 4px;
            overflow: hidden;
            margin-top: 8px;
        }
        
        .progress-fill {
            height: 100%;
            background: linear-gradient(90deg, #667eea, #764ba2);
            transition: width 0.3s ease;
        }
        
        .operations-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 15px;
        }
        
        .operation-card {
            text-align: center;
            padding: 20px;
            background: #f8f9fa;
            border-radius: 8px;
        }
        
        .operation-card .count {
            font-size: 32px;
            font-weight: bold;
            color: #667eea;
            margin: 10px 0;
        }
        
        .operation-card .label {
            color: #666;
            font-size: 14px;
            text-transform: uppercase;
        }
        
        .footer {
            text-align: center;
            color: white;
            margin-top: 20px;
            opacity: 0.8;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1><span class="status-indicator"></span>CacheDB Dashboard</h1>
            <div class="subtitle">Real-time cache monitoring and statistics</div>
        </div>
        
        <div class="stats-grid">
            <div class="stat-card">
                <h3>Uptime</h3>
                <div class="value" id="uptime">0s</div>
                <div class="sub-value">System running time</div>
            </div>
            
            <div class="stat-card">
                <h3>Total Operations</h3>
                <div class="value" id="totalOps">0</div>
                <div class="sub-value">All cache operations</div>
            </div>
            
            <div class="stat-card">
                <h3>Cache Hit Rate</h3>
                <div class="value" id="hitRate">0%</div>
                <div class="sub-value" id="hitsMisses">0 hits / 0 misses</div>
                <div class="progress-bar">
                    <div class="progress-fill" id="hitRateBar" style="width: 0%"></div>
                </div>
            </div>
            
            <div class="stat-card">
                <h3>Read Operations</h3>
                <div class="value" id="readOps">0</div>
                <div class="sub-value">Cache reads</div>
            </div>
            
            <div class="stat-card">
                <h3>Write Operations</h3>
                <div class="value" id="writeOps">0</div>
                <div class="sub-value">Cache writes</div>
            </div>
            
            <div class="stat-card">
                <h3>WAL Size</h3>
                <div class="value" id="walSize">0 B</div>
                <div class="sub-value" id="walStatus">Not available</div>
            </div>
        </div>
        
        <div class="section">
            <h2>Operation Breakdown</h2>
            <div class="operations-grid">
                <div class="operation-card">
                    <div class="label">Reads</div>
                    <div class="count" id="reads">0</div>
                </div>
                <div class="operation-card">
                    <div class="label">Writes</div>
                    <div class="count" id="writes">0</div>
                </div>
                <div class="operation-card">
                    <div class="label">Deletes</div>
                    <div class="count" id="deletes">0</div>
                </div>
            </div>
        </div>
        
        <div class="section">
            <h2>Performance Metrics</h2>
            <div class="metric-row">
                <span class="metric-label">Cache Hits</span>
                <span class="metric-value" id="cacheHits">0</span>
            </div>
            <div class="metric-row">
                <span class="metric-label">Cache Misses</span>
                <span class="metric-value" id="cacheMisses">0</span>
            </div>
            <div class="metric-row">
                <span class="metric-label">Hit Rate</span>
                <span class="metric-value" id="hitRateDetail">0.00%</span>
            </div>
        </div>
        
        <div class="footer">
            <p>CacheDB Dashboard - Auto-refreshing every second</p>
        </div>
    </div>
    
    <script>
        function formatUptime(seconds) {
            if (seconds < 60) return seconds + 's';
            if (seconds < 3600) return Math.floor(seconds / 60) + 'm ' + (seconds % 60) + 's';
            const hours = Math.floor(seconds / 3600);
            const mins = Math.floor((seconds % 3600) / 60);
            const secs = seconds % 60;
            return hours + 'h ' + mins + 'm ' + secs + 's';
        }
        
        function updateDashboard() {
            fetch('/api/stats')
                .then(r => r.json())
                .then(data => {
                    document.getElementById('uptime').textContent = formatUptime(data.uptime);
                    document.getElementById('totalOps').textContent = data.totalOperations.toLocaleString();
                    document.getElementById('readOps').textContent = data.readOperations.toLocaleString();
                    document.getElementById('writeOps').textContent = data.writeOperations.toLocaleString();
                    document.getElementById('hitRate').textContent = data.hitRate.toFixed(1) + '%';
                    document.getElementById('hitsMisses').textContent = 
                        data.cacheHits + ' hits / ' + data.cacheMisses + ' misses';
                    document.getElementById('hitRateBar').style.width = data.hitRate + '%';
                    document.getElementById('cacheHits').textContent = data.cacheHits.toLocaleString();
                    document.getElementById('cacheMisses').textContent = data.cacheMisses.toLocaleString();
                    document.getElementById('hitRateDetail').textContent = data.hitRate.toFixed(2) + '%';
                })
                .catch(e => console.error('Stats error:', e));
            
            fetch('/api/operations')
                .then(r => r.json())
                .then(data => {
                    document.getElementById('reads').textContent = data.reads.toLocaleString();
                    document.getElementById('writes').textContent = data.writes.toLocaleString();
                    document.getElementById('deletes').textContent = data.deletes.toLocaleString();
                })
                .catch(e => console.error('Operations error:', e));
            
            fetch('/api/wal')
                .then(r => r.json())
                .then(data => {
                    document.getElementById('walSize').textContent = data.sizeFormatted;
                    document.getElementById('walStatus').textContent = data.exists ? 'Active' : 'Not available';
                })
                .catch(e => console.error('WAL error:', e));
        }
        
        updateDashboard();
        setInterval(updateDashboard, 1000);
    </script>
</body>
</html>
""";
    }
}

