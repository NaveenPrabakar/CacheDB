package example;

import cachedb.CacheDB;
import cachedb.SimpleDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Random;

/**
 * Session Management System Demo
 * 
 * Demonstrates CacheDB for managing user sessions in a web application:
 * - User login sessions
 * - Session data storage
 * - Session expiration
 * - Session invalidation (logout)
 * - Multi-device session tracking
 * 
 * This is ideal for:
 * - Web applications with high traffic
 * - Microservices that need fast session lookups
 * - Applications requiring session persistence
 */
public class SessionManagementDemo {

    private static CacheDB cache;
    private static Random random = new Random();

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║        Session Management System with CacheDB             ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        // Initialize CacheDB with short TTL for sessions (30 seconds)
        DataSource ds = new SimpleDataSource(
                "jdbc:mysql://localhost:3306/cachedb",
                "root",
                "defg5678@"
        );

        cache = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(30)  // Sessions expire after 30 seconds
                .build();

        System.out.println("✓ CacheDB initialized with 30 second session TTL\n");

        demonstrateUserLogin();
        demonstrateSessionData();
        demonstrateMultiDeviceSessions();
        demonstrateSessionRefresh();
        demonstrateSessionLogout();
        demonstrateSessionExpiration();

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║              Session Management Demo Complete!            ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
    }

    /**
     * Scenario 1: User Login and Session Creation
     */
    private static void demonstrateUserLogin() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 1: User Login and Session Creation");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        System.out.println("🔐 User logging in...");
        
        int userId = 1001;
        String sessionId = generateSessionId();
        long loginTime = System.currentTimeMillis();

        // Create session
        cache.set("sessions", Map.of("session_id", sessionId),
                Map.of("user_id", userId,
                       "username", "alice_user",
                       "login_time", loginTime,
                       "ip_address", "192.168.1.100",
                       "user_agent", "Mozilla/5.0",
                       "active", true));

        System.out.println("  ✓ Session created: " + sessionId);
        System.out.println("  ✓ User ID: " + userId);

        // Verify session exists (fast cache lookup)
        System.out.println("\n🔍 Verifying session (from cache)...");
        Map<String, Object> session = cache.get("sessions", Map.of("session_id", sessionId));
        System.out.println("  Session data: " + session);
        System.out.println("  Username: " + session.get("username"));
        System.out.println("  Login time: " + session.get("login_time"));

        Thread.sleep(1000);
        System.out.println("\n✓ Login scenario complete\n");
    }

    /**
     * Scenario 2: Storing Session Data
     */
    private static void demonstrateSessionData() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 2: Storing Session Data");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        String sessionId = generateSessionId();
        int userId = 1002;

        System.out.println("💾 Storing session with user preferences...");
        
        cache.set("sessions", Map.of("session_id", sessionId),
                Map.of("user_id", userId,
                       "username", "bob_user",
                       "theme", "dark",
                       "language", "en",
                       "timezone", "America/New_York",
                       "cart_items_count", 3,
                       "last_activity", System.currentTimeMillis()));

        // Update session data (e.g., user adds item to cart)
        System.out.println("\n🔄 Updating session data (user adds item to cart)...");
        cache.set("sessions", Map.of("session_id", sessionId),
                Map.of("user_id", userId,
                       "username", "bob_user",
                       "theme", "dark",
                       "language", "en",
                       "timezone", "America/New_York",
                       "cart_items_count", 4,  // Updated
                       "last_activity", System.currentTimeMillis()));

        Map<String, Object> updated = cache.get("sessions", Map.of("session_id", sessionId));
        System.out.println("  Updated cart count: " + updated.get("cart_items_count"));

        Thread.sleep(1000);
        System.out.println("\n✓ Session data storage complete\n");
    }

    /**
     * Scenario 3: Multi-Device Sessions
     */
    private static void demonstrateMultiDeviceSessions() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 3: Multi-Device Session Tracking");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        int userId = 1003;
        System.out.println("📱 User logging in from multiple devices...");

        // Desktop session
        String desktopSession = generateSessionId();
        cache.set("sessions", Map.of("session_id", desktopSession),
                Map.of("user_id", userId,
                       "device_type", "desktop",
                       "device_id", "DESKTOP-001",
                       "last_activity", System.currentTimeMillis()));

        // Mobile session
        String mobileSession = generateSessionId();
        cache.set("sessions", Map.of("session_id", mobileSession),
                Map.of("user_id", userId,
                       "device_type", "mobile",
                       "device_id", "MOBILE-001",
                       "last_activity", System.currentTimeMillis()));

        // Tablet session
        String tabletSession = generateSessionId();
        cache.set("sessions", Map.of("session_id", tabletSession),
                Map.of("user_id", userId,
                       "device_type", "tablet",
                       "device_id", "TABLET-001",
                       "last_activity", System.currentTimeMillis()));

        System.out.println("\n🔍 Retrieving all active sessions for user...");
        System.out.println("  Desktop session: " + desktopSession);
        System.out.println("  Mobile session: " + mobileSession);
        System.out.println("  Tablet session: " + tabletSession);

        // In real app, you'd query all sessions for a user_id
        Map<String, Object> desktop = cache.get("sessions", Map.of("session_id", desktopSession));
        System.out.println("  Desktop device: " + desktop.get("device_type"));

        Thread.sleep(1000);
        System.out.println("\n✓ Multi-device tracking complete\n");
    }

    /**
     * Scenario 4: Session Refresh (Keep-Alive)
     */
    private static void demonstrateSessionRefresh() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 4: Session Refresh (Keep-Alive)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        String sessionId = generateSessionId();
        int userId = 1004;

        System.out.println("⏰ Creating session and refreshing it...");
        
        cache.set("sessions", Map.of("session_id", sessionId),
                Map.of("user_id", userId,
                       "created_at", System.currentTimeMillis(),
                       "last_activity", System.currentTimeMillis()));

        long initialTime = System.currentTimeMillis();
        System.out.println("  Initial session time: " + initialTime);

        // Simulate user activity - refresh session
        Thread.sleep(2000);
        System.out.println("\n🔄 User activity detected - refreshing session...");
        
        Map<String, Object> current = cache.get("sessions", Map.of("session_id", sessionId));
        cache.set("sessions", Map.of("session_id", sessionId),
                Map.of("user_id", userId,
                       "created_at", current.get("created_at"),
                       "last_activity", System.currentTimeMillis()));  // Updated

        Map<String, Object> refreshed = cache.get("sessions", Map.of("session_id", sessionId));
        System.out.println("  Refreshed last_activity: " + refreshed.get("last_activity"));
        System.out.println("  ✓ Session TTL extended (new expiration time)");

        Thread.sleep(1000);
        System.out.println("\n✓ Session refresh complete\n");
    }

    /**
     * Scenario 5: Session Logout (Delete)
     */
    private static void demonstrateSessionLogout() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 5: Session Logout (Delete Operation)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        String sessionId = generateSessionId();
        int userId = 1005;

        System.out.println("🔐 User logging in...");
        cache.set("sessions", Map.of("session_id", sessionId),
                Map.of("user_id", userId,
                       "username", "charlie_user",
                       "login_time", System.currentTimeMillis()));

        Map<String, Object> session = cache.get("sessions", Map.of("session_id", sessionId));
        System.out.println("  Active session: " + session.get("username"));

        System.out.println("\n🚪 User logging out...");
        cache.delete("sessions", Map.of("session_id", sessionId));

        Map<String, Object> deleted = cache.get("sessions", Map.of("session_id", sessionId));
        System.out.println("  Session after logout: " + deleted + " (null = deleted)");
        System.out.println("  ✓ Session invalidated and will be removed from database");

        Thread.sleep(1000);
        System.out.println("\n✓ Logout scenario complete\n");
    }

    /**
     * Scenario 6: Session Expiration
     */
    private static void demonstrateSessionExpiration() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 6: Session Expiration (TTL-based)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        String sessionId = generateSessionId();
        int userId = 1006;

        System.out.println("⏳ Creating session (will expire after 30 seconds)...");
        cache.set("sessions", Map.of("session_id", sessionId),
                Map.of("user_id", userId,
                       "username", "david_user",
                       "created_at", System.currentTimeMillis()));

        Map<String, Object> active = cache.get("sessions", Map.of("session_id", sessionId));
        System.out.println("  Session active: " + (active != null ? "Yes" : "No"));

        System.out.println("\n⏱️  Waiting for session to expire (simulating 35 seconds)...");
        System.out.println("  (In real scenario, this would happen naturally after TTL)");
        
        // Note: In real scenario, you'd wait for actual TTL expiration
        // For demo purposes, we just show the concept
        System.out.println("  After TTL expiration:");
        System.out.println("  - Session removed from cache");
        System.out.println("  - Session data flushed to database (if dirty)");
        System.out.println("  - Subsequent lookups return null");

        Thread.sleep(1000);
        System.out.println("\n✓ Session expiration demonstration complete\n");
    }

    private static String generateSessionId() {
        return "sess_" + System.currentTimeMillis() + "_" + random.nextInt(10000);
    }
}

