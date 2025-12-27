# E-Commerce Demo with CacheDB

This example demonstrates CacheDB in a real-world e-commerce order management system.

## Overview

The `ECommerceDemo` program showcases CacheDB's capabilities through a complete e-commerce scenario including:

- **User Management**: Create, read, update user profiles
- **Product Catalog**: Fast product lookups from cache
- **Shopping Cart**: Cart operations with composite primary keys
- **Order Processing**: Order creation and order item management
- **Delete Operations**: Removing items and canceling orders
- **Crash Recovery**: Demonstrating WAL-based recovery

## Features Demonstrated

### 1. Write-Behind Caching
- All writes are immediately cached (fast response)
- Database writes happen asynchronously when TTL expires
- No blocking on database operations

### 2. High-Performance Reads
- Product lookups served from memory (no database queries)
- User profile reads are instant
- Cart operations are lightning fast

### 3. Composite Primary Keys
- Shopping cart items: `(user_id, product_id)`
- Order items: `(order_id, item_id)`
- Demonstrates complex key lookups

### 4. Delete Operations
- Remove items from cart
- Cancel orders
- All deletes are logged to WAL and persisted

### 5. Crash Recovery
- Simulates application crash
- Recovers all writes and deletes from WAL
- Demonstrates durability guarantees

## Running the Demo

### Prerequisites

1. **MySQL Database**: Ensure MySQL is running with a database named `cachedb`
2. **Database Tables**: The following tables should exist (CacheDB will auto-discover schemas):

```sql
CREATE TABLE users (
    id INT PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255),
    role VARCHAR(50)
);

CREATE TABLE products (
    product_id INT PRIMARY KEY,
    name VARCHAR(255),
    price DECIMAL(10,2),
    stock INT,
    category VARCHAR(100)
);

CREATE TABLE orders (
    order_id INT PRIMARY KEY,
    user_id INT,
    status VARCHAR(50),
    total DECIMAL(10,2),
    created_at BIGINT
);

CREATE TABLE cart_items (
    user_id INT,
    product_id INT,
    quantity INT,
    added_at BIGINT,
    PRIMARY KEY (user_id, product_id)
);

CREATE TABLE order_items (
    order_id INT,
    item_id INT,
    product_id INT,
    quantity INT,
    price DECIMAL(10,2),
    PRIMARY KEY (order_id, item_id)
);
```

### Compilation

```bash
# From the project root, compile the main project first
mvn compile

# Then compile the example (it will be in target/classes/example/)
javac -cp "target/classes;%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" example\ECommerceDemo.java -d target\classes
```

**On Linux/Mac:**
```bash
mvn compile
javac -cp "target/classes:$(find ~/.m2/repository -name 'mysql-connector-j*.jar' | head -1)" example/ECommerceDemo.java -d target/classes
```

### Execution

**Option 1: Using Maven dependencies**
```bash
# Windows
java -cp "target/classes;%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" example.ECommerceDemo

# Linux/Mac
java -cp "target/classes:$(find ~/.m2/repository -name 'mysql-connector-j*.jar' | head -1)" example.ECommerceDemo
```

**Option 2: Using the fat JAR (recommended)**
```bash
# Build the fat JAR with all dependencies
mvn package

# Run the example (you'll need to manually add the example class)
# Or copy ECommerceDemo.class to the JAR
```

**Option 3: Use the provided scripts**
```bash
# Windows
cd example
run.bat

# Linux/Mac
cd example
chmod +x run.sh
./run.sh
```

## What You'll See

The demo runs through 6 scenarios:

1. **User Management**: Creates users, reads from cache, updates profiles
2. **Product Catalog**: Adds products, demonstrates fast cache lookups
3. **Shopping Cart**: Shows composite key operations for cart items
4. **Order Processing**: Creates orders with multiple items
5. **Delete Operations**: Removes items and cancels orders
6. **Crash Recovery**: Simulates crash and recovers from WAL

## Key Benefits Demonstrated

### Performance
- **Instant Reads**: All reads served from memory (microsecond latency)
- **Non-Blocking Writes**: Application continues immediately after writes
- **Batch Database Writes**: Multiple operations batched when TTL expires

### Durability
- **WAL Logging**: Every write and delete logged to disk
- **Crash Recovery**: All operations recovered after restart
- **No Data Loss**: Writes survive application crashes

### Flexibility
- **Multiple Tables**: Handles different table schemas automatically
- **Composite Keys**: Supports complex primary key structures
- **Schema Discovery**: Automatically detects table schemas

## Real-World Use Cases

This pattern is ideal for:

- **E-Commerce Platforms**: Fast product catalog, shopping carts, order management
- **Session Management**: User sessions, shopping carts, temporary data
- **High-Traffic APIs**: Frequently accessed data with occasional persistence
- **Microservices**: Fast local cache with eventual database consistency
- **Analytics Dashboards**: Cached metrics and reports

## Customization

You can modify the demo to:

- Change TTL values (how long data stays in cache)
- Add more tables and operations
- Simulate different crash scenarios
- Test with different data volumes
- Measure performance improvements

## Next Steps

After running this demo, you can:

1. Integrate CacheDB into your own application
2. Adjust TTL based on your use case
3. Monitor WAL growth and checkpoint frequency
4. Scale to handle higher traffic volumes

