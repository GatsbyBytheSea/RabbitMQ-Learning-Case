package edu.imtilsd.rabbimq_case;

import com.rabbitmq.client.*;
import org.json.JSONObject;

// 不再导入 java.sql.Connection
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PaymentService implements Runnable {

    private static final String EXCHANGE_NAME = "order_exchange";
    private static final String QUEUE_NAME = "payment_queue";
    private static final String BINDING_KEY = "order.payment";
    private static final String NEXT_ROUTING_KEY = "order.delivery";

    private volatile boolean running = true;

    @Override
    public void run() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(AppConfig.get("rabbitmq.host"));
        factory.setPort(AppConfig.getInt("rabbitmq.port", 5672));
        factory.setUsername(AppConfig.get("rabbitmq.username"));
        factory.setPassword(AppConfig.get("rabbitmq.password"));

        try {
            // 使用 com.rabbitmq.client.Connection 显式命名
            com.rabbitmq.client.Connection rmqConnection = factory.newConnection();
            Channel channel = rmqConnection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, BINDING_KEY);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String msg = new String(delivery.getBody());
                JSONObject orderJson = new JSONObject(msg);
                System.out.println("[PaymentService] Received: " + orderJson);

                // 模拟支付逻辑
                boolean paySuccess = processPayment(orderJson);
                if (paySuccess) {
                    // 更新订单状态为 PAID
                    updateOrderStatus(orderJson.getInt("orderId"), "PAID");

                    // 转发给配送服务
                    channel.basicPublish(EXCHANGE_NAME, NEXT_ROUTING_KEY, null, msg.getBytes());
                    System.out.println("[PaymentService] Payment OK. Forwarded to DeliveryService.");
                } else {
                    System.out.println("[PaymentService] Payment FAILED. Update order status if needed.");
                }

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };

            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});

            while (running) {
                Thread.sleep(1000);
            }

            channel.close();
            rmqConnection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean processPayment(JSONObject orderJson) {
        return true;
    }

    private void updateOrderStatus(int orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";

        // 在这里使用 java.sql.Connection 的全限定名
        try (java.sql.Connection conn = DbConnection.getOrderDbConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

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
