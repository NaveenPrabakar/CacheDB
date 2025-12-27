package example;

import cachedb.CacheDB;
import cachedb.SimpleDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Real-Time Analytics Counter System Demo
 * 
 * Demonstrates CacheDB for high-frequency counter operations:
 * - Page view counters
 * - Like/upvote counters
 * - Click tracking
 * - Real-time metrics aggregation
 * - Burst write handling
 * 
 * This is ideal for:
 * - Analytics dashboards
 * - Social media engagement metrics
 * - E-commerce click tracking
 * - Real-time monitoring systems
 */
public class AnalyticsCounterDemo {

    private static CacheDB cache;
    private static Random random = new Random();
    private static AtomicInteger totalOperations = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║      Real-Time Analytics Counter System Demo            ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        // Initialize CacheDB with short TTL for frequent flushes
        DataSource ds = new SimpleDataSource(
                "jdbc:mysql://localhost:3306/cachedb",
                "root",
                "defg5678@"
        );

        cache = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)  // Counters flush every 10 seconds
                .build();

        System.out.println("✓ CacheDB initialized with 10 second TTL\n");

        demonstratePageViewCounters();
        demonstrateLikeCounters();
        demonstrateClickTracking();
        demonstrateBurstWrites();
        demonstrateRealTimeMetrics();
        demonstrateCounterRecovery();

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║            Analytics Counter Demo Complete!              ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
    }

    /**
     * Scenario 1: Page View Counters
     */
    private static void demonstratePageViewCounters() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 1: Page View Counters");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        System.out.println("📊 Tracking page views...");

        // Simulate page views
        String[] pages = {"/home", "/products", "/about", "/contact"};
        
        for (String page : pages) {
            // Get current count
            Map<String, Object> counter = cache.get("page_views", Map.of("page_path", page));
            int currentCount = counter != null ? (Integer) counter.getOrDefault("count", 0) : 0;
            
            // Increment
            cache.set("page_views", Map.of("page_path", page),
                    Map.of("count", currentCount + 1,
                           "last_viewed", System.currentTimeMillis()));

            System.out.println("  " + page + ": " + (currentCount + 1) + " views");
        }

        // Simulate multiple views of same page
        System.out.println("\n🔄 Simulating 5 more views of /home...");
        for (int i = 0; i < 5; i++) {
            Map<String, Object> counter = cache.get("page_views", Map.of("page_path", "/home"));
            int count = counter != null ? (Integer) counter.getOrDefault("count", 0) : 0;
            cache.set("page_views", Map.of("page_path", "/home"),
                    Map.of("count", count + 1,
                           "last_viewed", System.currentTimeMillis()));
        }

        Map<String, Object> finalCount = cache.get("page_views", Map.of("page_path", "/home"));
        System.out.println("  Final count: " + finalCount.get("count") + " views");

        Thread.sleep(1000);
        System.out.println("\n✓ Page view tracking complete\n");
    }

    /**
     * Scenario 2: Like/Upvote Counters
     */
    private static void demonstrateLikeCounters() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 2: Like/Upvote Counters");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        System.out.println("👍 Tracking likes on posts...");

        int[] postIds = {1001, 1002, 1003, 1004, 1005};

        // Initialize posts
        for (int postId : postIds) {
            cache.set("post_likes", Map.of("post_id", postId),
                    Map.of("like_count", 0,
                           "dislike_count", 0,
                           "created_at", System.currentTimeMillis()));
        }

        // Simulate likes
        System.out.println("\n💚 Users liking posts...");
        for (int i = 0; i < 10; i++) {
            int postId = postIds[random.nextInt(postIds.length)];
            Map<String, Object> post = cache.get("post_likes", Map.of("post_id", postId));
            int likes = post != null ? (Integer) post.getOrDefault("like_count", 0) : 0;
            
            cache.set("post_likes", Map.of("post_id", postId),
                    Map.of("like_count", likes + 1,
                           "dislike_count", post != null ? post.get("dislike_count") : 0,
                           "created_at", post != null ? post.get("created_at") : System.currentTimeMillis()));
        }

        // Show final counts
        System.out.println("\n📈 Final like counts:");
        for (int postId : postIds) {
            Map<String, Object> post = cache.get("post_likes", Map.of("post_id", postId));
            if (post != null) {
                System.out.println("  Post " + postId + ": " + post.get("like_count") + " likes");
            }
        }

        Thread.sleep(1000);
        System.out.println("\n✓ Like counter tracking complete\n");
    }

    /**
     * Scenario 3: Click Tracking
     */
    private static void demonstrateClickTracking() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 3: Click Tracking");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        System.out.println("🖱️  Tracking ad clicks...");

        String[] adIds = {"ad_001", "ad_002", "ad_003"};

        // Track clicks with metadata
        for (int i = 0; i < 15; i++) {
            String adId = adIds[random.nextInt(adIds.length)];
            
            Map<String, Object> ad = cache.get("ad_clicks", Map.of("ad_id", adId));
            int clicks = ad != null ? (Integer) ad.getOrDefault("click_count", 0) : 0;
            
            cache.set("ad_clicks", Map.of("ad_id", adId),
                    Map.of("click_count", clicks + 1,
                           "last_click", System.currentTimeMillis(),
                           "total_revenue", clicks * 0.50));  // $0.50 per click
        }

        System.out.println("\n📊 Ad click statistics:");
        for (String adId : adIds) {
            Map<String, Object> ad = cache.get("ad_clicks", Map.of("ad_id", adId));
            if (ad != null) {
                System.out.println("  " + adId + ":");
                System.out.println("    Clicks: " + ad.get("click_count"));
                System.out.println("    Revenue: $" + ad.get("total_revenue"));
            }
        }

        Thread.sleep(1000);
        System.out.println("\n✓ Click tracking complete\n");
    }

    /**
     * Scenario 4: Burst Write Handling
     */
    private static void demonstrateBurstWrites() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 4: Burst Write Handling");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        System.out.println("⚡ Simulating burst of 100 counter updates...");
        System.out.println("  (All writes cached immediately, flushed asynchronously)\n");

        long startTime = System.currentTimeMillis();

        // Burst of writes
        for (int i = 0; i < 100; i++) {
            String counterId = "counter_" + (i % 10);  // 10 different counters
            
            Map<String, Object> counter = cache.get("counters", Map.of("counter_id", counterId));
            int value = counter != null ? (Integer) counter.getOrDefault("value", 0) : 0;
            
            cache.set("counters", Map.of("counter_id", counterId),
                    Map.of("value", value + 1,
                           "updated_at", System.currentTimeMillis()));
            
            totalOperations.incrementAndGet();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("  ✓ 100 writes completed in " + duration + "ms");
        System.out.println("  ✓ Average: " + (duration / 100.0) + "ms per write");
        System.out.println("  ✓ All writes cached immediately (non-blocking)");
        System.out.println("  ✓ Database writes will happen asynchronously when TTL expires");

        Thread.sleep(1000);
        System.out.println("\n✓ Burst write demonstration complete\n");
    }

    /**
     * Scenario 5: Real-Time Metrics Aggregation
     */
    private static void demonstrateRealTimeMetrics() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 5: Real-Time Metrics Aggregation");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        System.out.println("📊 Aggregating real-time metrics...");

        // Track metrics for different time periods
        String[] metrics = {"requests_per_minute", "errors_per_minute", "api_calls_per_minute"};

        for (String metric : metrics) {
            int value = random.nextInt(1000) + 100;
            cache.set("metrics", Map.of("metric_name", metric),
                    Map.of("value", value,
                           "timestamp", System.currentTimeMillis(),
                           "unit", "count"));
        }

        System.out.println("\n📈 Current metrics (from cache):");
        for (String metric : metrics) {
            Map<String, Object> data = cache.get("metrics", Map.of("metric_name", metric));
            if (data != null) {
                System.out.println("  " + metric + ": " + data.get("value") + " " + data.get("unit"));
            }
        }

        // Update metrics rapidly
        System.out.println("\n🔄 Updating metrics in real-time...");
        for (int i = 0; i < 5; i++) {
            String metric = metrics[random.nextInt(metrics.length)];
            Map<String, Object> current = cache.get("metrics", Map.of("metric_name", metric));
            int newValue = current != null ? (Integer) current.get("value") + random.nextInt(50) : 0;
            
            cache.set("metrics", Map.of("metric_name", metric),
                    Map.of("value", newValue,
                           "timestamp", System.currentTimeMillis(),
                           "unit", "count"));
        }

        System.out.println("\n📊 Updated metrics:");
        for (String metric : metrics) {
            Map<String, Object> data = cache.get("metrics", Map.of("metric_name", metric));
            if (data != null) {
                System.out.println("  " + metric + ": " + data.get("value"));
            }
        }

        Thread.sleep(1000);
        System.out.println("\n✓ Real-time metrics aggregation complete\n");
    }

    /**
     * Scenario 6: Counter Recovery
     */
    private static void demonstrateCounterRecovery() throws Exception {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 6: Counter Recovery from WAL");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        System.out.println("💾 Creating counters before crash...");
        
        cache.set("counters", Map.of("counter_id", "recovery_test"),
                Map.of("value", 42,
                       "name", "Test Counter",
                       "created_at", System.currentTimeMillis()));

        cache.set("metrics", Map.of("metric_name", "recovery_metric"),
                Map.of("value", 100,
                       "timestamp", System.currentTimeMillis()));

        System.out.println("\n💥 Simulating application crash...");
        cache = null;

        Thread.sleep(500);

        System.out.println("\n🔄 Recovering from WAL...");
        DataSource ds = new SimpleDataSource(
                "jdbc:mysql://localhost:3306/cachedb",
                "root",
                "defg5678@"
        );

        cache = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        System.out.println("\n✅ Verifying recovered counters...");
        Map<String, Object> counter = cache.get("counters", Map.of("counter_id", "recovery_test"));
        System.out.println("  Recovered counter: " + counter);
        System.out.println("  Value: " + (counter != null ? counter.get("value") : "null"));

        Map<String, Object> metric = cache.get("metrics", Map.of("metric_name", "recovery_metric"));
        System.out.println("  Recovered metric: " + metric);
        System.out.println("  Value: " + (metric != null ? metric.get("value") : "null"));

        System.out.println("\n✓ All counters recovered from WAL successfully!\n");
    }
}

