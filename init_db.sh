#!/usr/bin/env bash

# ------------------------------------------------------------------
#  init_db.sh
#  This script creates and initializes databases and tables.
# ------------------------------------------------------------------

set -e
trap 'echo "An error occurred. Exiting..."' ERR

# test if MySQL service is running
if ! systemctl is-active --quiet mysql; then
  echo "MySQL service is not running. Please start the service and try again."
  exit 1
fi

echo "Creating and initializing databases..."

mysql -u root -p <<EOF

-- creat order_db
CREATE DATABASE IF NOT EXISTS order_db;
USE order_db;

-- creat orders sheet
CREATE TABLE IF NOT EXISTS orders (
  order_id INT PRIMARY KEY,
  user_id INT,
  product_id VARCHAR(50),
  quantity INT,
  total_price DECIMAL(10, 2),
  status VARCHAR(20),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- creat inventory_db
CREATE DATABASE IF NOT EXISTS inventory_db;
USE inventory_db;

-- creat stocks sheet
CREATE TABLE IF NOT EXISTS stocks (
  product_id VARCHAR(50) PRIMARY KEY,
  stock_quantity INT
);

-- insert example data
INSERT INTO stocks (product_id, stock_quantity) VALUES
('P100', 50),
('P101', 50),
('P102', 50),
('P103', 50),
('P104', 50),
('P105', 50),
('P106', 50),
('P107', 50),
('P108', 50),
('P109', 50)
ON DUPLICATE KEY UPDATE stock_quantity = VALUES(stock_quantity);

EOF

echo "Databases and tables have been initialized successfully!"
