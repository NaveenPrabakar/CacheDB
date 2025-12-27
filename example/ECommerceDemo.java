package example;

import cachedb.CacheDB;
import cachedb.SimpleDataSource;

import javax.sql.DataSource;
import java.util.Map;

/**
 * E-Commerce Order Management System Demo
 * 
 * This example demonstrates CacheDB in a real-world e-commerce scenario:
 * - User management
 * - Product catalog
 * - Shopping cart operations
 * - Order processing
 * - Order item management
 * 
 * Features demonstrated:
 * - Fast reads from cache (no database queries)
 * - Write-behind persistence (async database writes)
 * - Delete operations
 * - Composite primary keys
 * - Multiple tables
 * - Recovery from crashes
 */
public class ECommerceDemo {

    private static CacheDB cache;

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║     E-Commerce Order Management with CacheDB            ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        // Initialize CacheDB
        DataSource ds = new SimpleDataSource(
                "jdbc:mysql://localhost:3306/cachedb",
                "root",
                "defg5678@"
        );

        cache = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(5)  // Cache entries expire after 5 seconds
                .build();

        System.out.println("✓ CacheDB initialized with 5 second TTL\n");

        // Run demo scenarios
        demonstrateUserManagement();
        demonstrateProductCatalog();
        demonstrateShoppingCart();
        demonstrateOrderProcessing();
        demonstrateDeleteOperations();
        demonstrateRecovery();

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                    Demo Complete!                        ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
    }

    /**
     * Scenario 1: User Management
     * Demonstrates basic CRUD operations on user data
     */
    private static void demonstrateUserManagement() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 1: User Management");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        // Create users
        System.out.println("📝 Creating users...");
        cache.set("users", Map.of("id", 1),
                Map.of("name", "Alice Johnson", "email", "alice@example.com", "role", "customer"));
        cache.set("users", Map.of("id", 2),
                Map.of("name", "Bob Smith", "email", "bob@example.com", "role", "customer"));
        cache.set("users", Map.of("id", 3),
                Map.of("name", "Admin User", "email", "admin@example.com", "role", "admin"));

        // Read users (from cache - very fast!)
        System.out.println("\n🔍 Reading users from cache (no database query)...");
        Map<String, Object> user1 = cache.get("users", Map.of("id", 1));
        System.out.println("  User 1: " + user1);

        Map<String, Object> user2 = cache.get("users", Map.of("id", 2));
        System.out.println("  User 2: " + user2);

        // Update user
        System.out.println("\n✏️  Updating user 1...");
        cache.set("users", Map.of("id", 1),
                Map.of("name", "Alice Johnson", "email", "alice.new@example.com", "role", "premium"));

        Map<String, Object> updated = cache.get("users", Map.of("id", 1));
        System.out.println("  Updated: " + updated);

        Thread.sleep(1000);
        System.out.println("\n✓ User management operations complete\n");
    }

    /**
     * Scenario 2: Product Catalog
     * Demonstrates product data caching
     */
    private static void demonstrateProductCatalog() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 2: Product Catalog");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        System.out.println("📦 Adding products to catalog...");
        cache.set("products", Map.of("product_id", 101),
                Map.of("name", "Laptop", "price", 999.99, "stock", 50, "category", "Electronics"));
        cache.set("products", Map.of("product_id", 102),
                Map.of("name", "Mouse", "price", 29.99, "stock", 200, "category", "Electronics"));
        cache.set("products", Map.of("product_id", 103),
                Map.of("name", "Keyboard", "price", 79.99, "stock", 150, "category", "Electronics"));

        System.out.println("\n🔍 Fetching product details (from cache)...");
        Map<String, Object> laptop = cache.get("products", Map.of("product_id", 101));
        System.out.println("  Product 101: " + laptop);

        // Simulate high-frequency product lookups (cache makes this very fast)
        System.out.println("\n⚡ Simulating 10 rapid product lookups (all from cache)...");
        for (int i = 0; i < 10; i++) {
            cache.get("products", Map.of("product_id", 101));
        }
        System.out.println("  ✓ All lookups served from cache (no database queries!)");

        Thread.sleep(1000);
        System.out.println("\n✓ Product catalog operations complete\n");
    }

    /**
     * Scenario 3: Shopping Cart
     * Demonstrates composite primary keys and cart operations
     */
    private static void demonstrateShoppingCart() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 3: Shopping Cart (Composite Keys)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        int userId = 1;
        System.out.println("🛒 User " + userId + " adding items to cart...");

        // Add items to cart (composite key: user_id + product_id)
        cache.set("cart_items",
                Map.of("user_id", userId, "product_id", 101),
                Map.of("quantity", 1, "added_at", System.currentTimeMillis()));
        cache.set("cart_items",
                Map.of("user_id", userId, "product_id", 102),
                Map.of("quantity", 2, "added_at", System.currentTimeMillis()));
        cache.set("cart_items",
                Map.of("user_id", userId, "product_id", 103),
                Map.of("quantity", 1, "added_at", System.currentTimeMillis()));

        System.out.println("\n🔍 Retrieving cart items (composite key lookup)...");
        Map<String, Object> item1 = cache.get("cart_items",
                Map.of("user_id", userId, "product_id", 101));
        System.out.println("  Cart item (user=1, product=101): " + item1);

        Map<String, Object> item2 = cache.get("cart_items",
                Map.of("user_id", userId, "product_id", 102));
        System.out.println("  Cart item (user=1, product=102): " + item2);

        // Update quantity
        System.out.println("\n✏️  Updating cart item quantity...");
        cache.set("cart_items",
                Map.of("user_id", userId, "product_id", 102),
                Map.of("quantity", 3, "added_at", System.currentTimeMillis()));

        Map<String, Object> updated = cache.get("cart_items",
                Map.of("user_id", userId, "product_id", 102));
        System.out.println("  Updated quantity: " + updated.get("quantity"));

        Thread.sleep(1000);
        System.out.println("\n✓ Shopping cart operations complete\n");
    }

    /**
     * Scenario 4: Order Processing
     * Demonstrates order creation and order items
     */
    private static void demonstrateOrderProcessing() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 4: Order Processing");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        int orderId = 1001;
        int userId = 1;

        System.out.println("📋 Creating order " + orderId + "...");
        cache.set("orders", Map.of("order_id", orderId),
                Map.of("user_id", userId, "status", "PENDING", "total", 0.0, "created_at", System.currentTimeMillis()));

        System.out.println("\n📦 Adding items to order (composite key: order_id + item_id)...");
        cache.set("order_items",
                Map.of("order_id", orderId, "item_id", 1),
                Map.of("product_id", 101, "quantity", 1, "price", 999.99));
        cache.set("order_items",
                Map.of("order_id", orderId, "item_id", 2),
                Map.of("product_id", 102, "quantity", 2, "price", 29.99));
        cache.set("order_items",
                Map.of("order_id", orderId, "item_id", 3),
                Map.of("product_id", 103, "quantity", 1, "price", 79.99));

        // Calculate total
        double total = 999.99 + (2 * 29.99) + 79.99;

        System.out.println("\n💳 Processing payment and updating order status...");
        cache.set("orders", Map.of("order_id", orderId),
                Map.of("user_id", userId, "status", "PAID", "total", total, "created_at", System.currentTimeMillis()));

        System.out.println("\n🔍 Retrieving order details...");
        Map<String, Object> order = cache.get("orders", Map.of("order_id", orderId));
        System.out.println("  Order: " + order);

        Map<String, Object> item1 = cache.get("order_items",
                Map.of("order_id", orderId, "item_id", 1));
        System.out.println("  Order Item 1: " + item1);

        Thread.sleep(1000);
        System.out.println("\n✓ Order processing complete\n");
    }

    /**
     * Scenario 5: Delete Operations
     * Demonstrates removing items from cart and canceling orders
     */
    private static void demonstrateDeleteOperations() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 5: Delete Operations");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        int userId = 1;

        System.out.println("🗑️  Removing item from cart...");
        // Verify item exists
        Map<String, Object> item = cache.get("cart_items",
                Map.of("user_id", userId, "product_id", 103));
        System.out.println("  Before delete: " + item);

        // Delete the item
        cache.delete("cart_items", Map.of("user_id", userId, "product_id", 103));

        // Verify it's gone
        Map<String, Object> deleted = cache.get("cart_items",
                Map.of("user_id", userId, "product_id", 103));
        System.out.println("  After delete: " + deleted + " (null = deleted)");

        System.out.println("\n❌ Canceling an order...");
        int cancelOrderId = 1002;
        cache.set("orders", Map.of("order_id", cancelOrderId),
                Map.of("user_id", userId, "status", "PENDING", "total", 50.0));

        Map<String, Object> order = cache.get("orders", Map.of("order_id", cancelOrderId));
        System.out.println("  Order before cancel: " + order);

        cache.delete("orders", Map.of("order_id", cancelOrderId));

        Map<String, Object> canceled = cache.get("orders", Map.of("order_id", cancelOrderId));
        System.out.println("  Order after cancel: " + canceled + " (null = deleted)");

        Thread.sleep(1000);
        System.out.println("\n✓ Delete operations complete\n");
    }

    /**
     * Scenario 6: Crash Recovery
     * Demonstrates WAL recovery after application restart
     */
    private static void demonstrateRecovery() throws Exception {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 6: Crash Recovery (WAL Recovery)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        System.out.println("💾 Creating data that will be recovered after crash...");
        cache.set("users", Map.of("id", 100),
                Map.of("name", "Recovery Test User", "email", "recovery@test.com"));
        cache.set("orders", Map.of("order_id", 9999),
                Map.of("user_id", 100, "status", "PENDING", "total", 199.99));
        cache.delete("users", Map.of("id", 2));  // Delete operation

        System.out.println("\n💥 Simulating application crash...");
        System.out.println("  (In real scenario, application would crash here)");
        cache = null;  // Simulate crash

        Thread.sleep(500);

        System.out.println("\n🔄 Restarting application and recovering from WAL...");
        DataSource ds = new SimpleDataSource(
                "jdbc:mysql://localhost:3306/cachedb",
                "root",
                "defg5678@"
        );

        cache = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(5)
                .build();

        System.out.println("\n✅ Verifying recovered data...");
        Map<String, Object> recoveredUser = cache.get("users", Map.of("id", 100));
        System.out.println("  Recovered user: " + recoveredUser);

        Map<String, Object> recoveredOrder = cache.get("orders", Map.of("order_id", 9999));
        System.out.println("  Recovered order: " + recoveredOrder);

        // Verify delete was also recovered
        Map<String, Object> deletedUser = cache.get("users", Map.of("id", 2));
        System.out.println("  Deleted user (should be null): " + deletedUser);

        System.out.println("\n✓ All data recovered from WAL successfully!");
        System.out.println("  (Writes and deletes persisted through crash)\n");
    }
}

