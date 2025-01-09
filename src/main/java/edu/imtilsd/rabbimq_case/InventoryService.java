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
            // RabbitMQ 连接
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // 声明交换机 & 队列
            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, BINDING_KEY);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String msg = new String(delivery.getBody());
                JSONObject orderJson = new JSONObject(msg);
                System.out.println("[InventoryService] Received order: " + orderJson);

                // 模拟库存检查和扣减
                boolean success = checkAndDeductInventory(orderJson);
                if (success) {
                    // 转发消息给支付服务
                    channel.basicPublish(EXCHANGE_NAME, NEXT_ROUTING_KEY, null, msg.getBytes());
                    System.out.println("[InventoryService] Inventory OK. Forwarded to PaymentService.");
                } else {
                    System.out.println("[InventoryService] Inventory NOT enough. Order failed.");
                    // 也可更新订单数据库状态为 FAILED，具体逻辑可自行拓展
                }

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };

            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});

            // 保持当前线程存活
            while (running) {
                Thread.sleep(1000);
            }

            // 停止时关闭资源
            channel.close();
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查并扣减库存
     */
    private boolean checkAndDeductInventory(JSONObject orderJson) {
        String productId = orderJson.getString("productId");
        int requiredQty = orderJson.getInt("quantity");
        String querySql = "SELECT stock_quantity FROM stocks WHERE product_id = ?";
        String updateSql = "UPDATE stocks SET stock_quantity = stock_quantity - ? WHERE product_id = ?";

        try (java.sql.Connection conn = DbConnection.getInventoryDbConnection()) {
            // 查询库存
            int currentStock = 0;
            try (PreparedStatement stmt = conn.prepareStatement(querySql)) {
                stmt.setString(1, productId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    currentStock = rs.getInt("stock_quantity");
                }
            }

            // 如果库存足够，则扣减
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
