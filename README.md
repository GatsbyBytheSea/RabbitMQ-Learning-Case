## Project Overview

This project is a distributed order processing example based on RabbitMQ, implemented using Java and Maven. It includes the following four core services:

- **OrderService**: Periodically generates random orders and sends them to the message queue.
- **InventoryService**: Processes order messages by performing stock checks and deductions.
- **PaymentService**: Handles orders processed by the inventory service, simulates payment logic, and updates order status.
- **DeliveryService**: Handles payment-completed order messages, simulates delivery, and updates the final order status.

Additionally, the project uses MySQL for data storage of orders and inventory. A Bash script initializes the required database and table structure. The `application.properties` file centralizes RabbitMQ and database connection configurations, simplifying deployment and maintenance across different environments.

---

## Features

- **Message-driven**: Services communicate asynchronously via RabbitMQ, ensuring loose coupling.
- **Multithreaded**: All services are launched from a single `Application` class, running in separate threads.
- **Database persistence**:
    - **Order database** (`order_db.orders`): Stores order information.
    - **Inventory database** (`inventory_db.stocks`): Stores product inventory, which is updated by `InventoryService`.
- **Extensibility**: New processes such as coupon services or loyalty point services can be added by creating new services and queue bindings.
- **Configuration decoupling**: Connection details for RabbitMQ and databases are managed in `application.properties`, separating business logic from environment-specific settings.

---

## Directory Structure

```
mq-demo
├── init_db.sh               # Bash script for initializing the database
├── pom.xml                  # Maven build file
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── example
    │   │           └── mqdemo
    │   │               ├── Application.java       # Starts all services (contains the main method)
    │   │               ├── AppConfig.java         # Loads application.properties
    │   │               ├── DbConnection.java      # Methods for database connection
    │   │               ├── DeliveryService.java   # Simulates delivery logic
    │   │               ├── InventoryService.java  # Simulates inventory checks and deductions
    │   │               ├── OrderService.java      # Periodically generates random orders and sends messages
    │   │               └── PaymentService.java    # Simulates payment logic
    │   └── resources
    │       └── application.properties            # RabbitMQ and database configuration
    └── test
        └── java
```

---

## Installation and Setup

### Install RabbitMQ

Install RabbitMQ locally or on a remote server. If using Docker, run:

```bash
docker run -d --hostname my-rabbit --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  rabbitmq:3-management
```

The default username and password are `guest/guest` (can be modified in `application.properties`).

### Install MySQL

Install MySQL locally or on a remote server. Configure database port, username, password, etc., and ensure the settings in `application.properties` are accurate.

### Initialize the Database

Navigate to the project root directory, ensure the script has execution permissions:

```bash
chmod +x init_db.sh
```

Run the script:

```bash
./init_db.sh
```

Enter the MySQL root password when prompted. The script will create `order_db` and `inventory_db`, set up the necessary table structure, and insert initial inventory data.

### Modify `application.properties`

Update RabbitMQ connection settings (`rabbitmq.host`, `rabbitmq.port`, `rabbitmq.username`, `rabbitmq.password`) based on your environment. Similarly, update database settings (`orderdb.url`, `orderdb.username`, `orderdb.password`, and `inventorydb.*`).

### Build the Project

Install Maven or use an IDE (e.g., IntelliJ IDEA) to open the project. From the project root directory, run:

```bash
mvn clean package
```

A runnable `.jar` file will be generated in the `target/` directory.

---

## Usage

### Method 1: Run in IDE

In an IDE like IntelliJ IDEA, locate `Application.java`, right-click and select "Run" or create a run configuration. If configured correctly, logs for the four services (`OrderService`, `InventoryService`, `PaymentService`, `DeliveryService`) will appear in the console.

### Method 2: Run from Command Line

In the project root directory, execute:

```bash
mvn clean package
java -jar target/mq-demo-1.0-SNAPSHOT.jar
```

After starting, `OrderService` will generate a random order every 2 seconds. `InventoryService`, `PaymentService`, and `DeliveryService` will process messages and update the database accordingly.

---

## Testing and Validation

- **Observe Console Logs**: Verify that logs indicate successful processing of orders at each stage.
- **RabbitMQ Management UI**: Visit [http://localhost:15672/](http://localhost:15672/) (adjust port as needed) and log in with the configured credentials. Check message queues (`inventory_queue`, `payment_queue`, `delivery_queue`) and consumer statuses.
- **Inspect Database Data**:
    - Check `order_db.orders` to confirm the order status progresses through `CREATED -> PAID -> DELIVERED`.
    - Verify inventory deductions in `inventory_db.stocks`.

---

## Common Issues

### Connection Failures

- Ensure RabbitMQ and MySQL connection settings in `application.properties` are correct.
- For Docker deployments, set `host` to the container's IP or hostname and confirm port mappings.

### Database Permissions

- If the MySQL account is not `root`, ensure it has sufficient privileges (e.g., create, insert, update) for `order_db` and `inventory_db`.

### Thread Termination

- By default, services run continuously. To gracefully stop, add listeners or invoke `stopService()` in `Application`, and release resources properly.

---

## Contribution and Expansion

- **Adding New Business Scenarios**: More services can be inserted into or run parallel to existing processes. For example:

  - Log Audit Service (subscribe to all order events and analyze the data)
- **Error Handling and Retry**: Add retry mechanisms in services such as PaymentService and InventoryService, or handle abnormal messages using RabbitMQ's dead letter queues.
- **Multilingual Microservices**: If certain modules need to be implemented in Python, Node.js, Go, etc., ensure compatibility with RabbitMQ's message format and routing keys to seamlessly integrate with Java modules.
- **Monitoring and Alerts**: Use tools like Prometheus and Grafana to monitor service metrics (message backlog, consumption rate, DB access latency, etc.) and set up timely alerts.

---

## Author Information

- **Author**: ZHU Xinlei
- **GitHub**: [GatsbyBytheSea](https://github.com/GatsbyBytheSea)

---

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.