package edu.imtilsd.rabbimq_case;

import com.rabbitmq.client.*;
import org.json.JSONObject;

import java.sql.Connection;      // JDBC Connection
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeliveryService implements Runnable {

    private static final String EXCHANGE_NAME = "order_exchange";
    private static final String QUEUE_NAME = "delivery_queue";
    private static final String BINDING_KEY = "order.delivery";

    private volatile boolean running = true;

    @Override
    public void run() {
        // 1) 读取 RabbitMQ 配置信息
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(AppConfig.get("rabbitmq.host"));
        factory.setPort(AppConfig.getInt("rabbitmq.port", 5672));
        factory.setUsername(AppConfig.get("rabbitmq.username"));
        factory.setPassword(AppConfig.get("rabbitmq.password"));

        try {
            // 2) 建立 RabbitMQ 连接，不要用简写 Connection，这里显式写出来
            com.rabbitmq.client.Connection rmqConnection = factory.newConnection();
            Channel channel = rmqConnection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, BINDING_KEY);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String msg = new String(delivery.getBody());
                JSONObject orderJson = new JSONObject(msg);
                System.out.println("[DeliveryService] Received: " + orderJson);

                // 模拟配送逻辑
                processDelivery(orderJson);

                // 更新订单状态为 DELIVERED
                updateOrderStatus(orderJson.getInt("orderId"), "DELIVERED");
                System.out.println("[DeliveryService] Order delivered.");

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };

            // 3) 消费队列消息
            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});

            // 4) 保持运行
            while (running) {
                Thread.sleep(1000);
            }

            // 5) 停止时关闭资源
            channel.close();
            rmqConnection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 模拟配送逻辑
     */
    private void processDelivery(JSONObject orderJson) {
        System.out.println("[DeliveryService] Delivering order: " + orderJson.getInt("orderId"));
    }

    /**
     * 更新订单数据库中的订单状态
     */
    private void updateOrderStatus(int orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";

        // 这里用 JDBC 的 Connection
        try (Connection dbConnection = DbConnection.getOrderDbConnection();
             PreparedStatement stmt = dbConnection.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void stopService() {
        this.running = false;
    }
}

