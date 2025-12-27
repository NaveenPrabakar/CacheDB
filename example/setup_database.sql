-- Database setup script for E-Commerce Demo
-- Run this script to create the necessary tables

USE cachedb;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255),
    role VARCHAR(50)
);

-- Products table
CREATE TABLE IF NOT EXISTS products (
    product_id INT PRIMARY KEY,
    name VARCHAR(255),
    price DECIMAL(10,2),
    stock INT,
    category VARCHAR(100)
);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    order_id INT PRIMARY KEY,
    user_id INT,
    status VARCHAR(50),
    total DECIMAL(10,2),
    created_at BIGINT
);

-- Shopping cart items (composite primary key)
CREATE TABLE IF NOT EXISTS cart_items (
    user_id INT,
    product_id INT,
    quantity INT,
    added_at BIGINT,
    PRIMARY KEY (user_id, product_id)
);

-- Order items (composite primary key)
CREATE TABLE IF NOT EXISTS order_items (
    order_id INT,
    item_id INT,
    product_id INT,
    quantity INT,
    price DECIMAL(10,2),
    PRIMARY KEY (order_id, item_id)
);

-- Optional: Insert some sample data
INSERT INTO products (product_id, name, price, stock, category) VALUES
    (101, 'Laptop', 999.99, 50, 'Electronics'),
    (102, 'Mouse', 29.99, 200, 'Electronics'),
    (103, 'Keyboard', 79.99, 150, 'Electronics')
ON DUPLICATE KEY UPDATE name=name;

