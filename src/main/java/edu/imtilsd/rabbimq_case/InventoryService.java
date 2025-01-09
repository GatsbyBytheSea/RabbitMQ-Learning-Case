package edu.imtilsd.rabbimq_case;

import com.rabbitmq.client.*;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InventoryService implements Runnable {

    private static final String EXCHANGE_NAME = "order_exchange";
    private static final String QUEUE_NAME = "inventory_queue";
    private static final String BINDING_KEY = "order.inventory";
    private static final String NEXT_ROUTING_KEY = "order.payment";

    private volatile boolean running = true;

    @Override
    public void run() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(AppConfig.get("rabbitmq.host"));
        factory.setPort(AppConfig.getInt("rabbitmq.port", 5672));
        factory.setUsername(AppConfig.get("rabbitmq.username"));
        factory.setPassword(AppConfig.get("rabbitmq.password"));


        try {
            // Connect to RabbitMQ
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // Declare exchange and queue
            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, BINDING_KEY);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String msg = new String(delivery.getBody());
                JSONObject orderJson = new JSONObject(msg);
                System.out.println("[InventoryService] Received order: " + orderJson);

                // simulate inventory check and deduction
                boolean success = checkAndDeductInventory(orderJson);
                if (success) {
                    // send to PaymentService
                    channel.basicPublish(EXCHANGE_NAME, NEXT_ROUTING_KEY, null, msg.getBytes());
                    System.out.println("[InventoryService] Inventory OK. Forwarded to PaymentService.");
                } else {
                    System.out.println("[InventoryService] Inventory NOT enough. Order failed.");
                }

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };

            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});

            // keep running
            while (running) {
                Thread.sleep(1000);
            }

            // close resources
            channel.close();
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Check and deduct inventory
     */
    private boolean checkAndDeductInventory(JSONObject orderJson) {
        String productId = orderJson.getString("productId");
        int requiredQty = orderJson.getInt("quantity");
        String querySql = "SELECT stock_quantity FROM stocks WHERE product_id = ?";
        String updateSql = "UPDATE stocks SET stock_quantity = stock_quantity - ? WHERE product_id = ?";

        try (java.sql.Connection conn = DbConnection.getInventoryDbConnection()) {
            // get current stock
            int currentStock = 0;
            try (PreparedStatement stmt = conn.prepareStatement(querySql)) {
                stmt.setString(1, productId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    currentStock = rs.getInt("stock_quantity");
                }
            }

            // check and deduct
            if (currentStock >= requiredQty) {
                try (PreparedStatement stmt2 = conn.prepareStatement(updateSql)) {
                    stmt2.setInt(1, requiredQty);
                    stmt2.setString(2, productId);
                    stmt2.executeUpdate();
                }
                return true;
            } else {
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void stopService() {
        this.running = false;
    }
}
