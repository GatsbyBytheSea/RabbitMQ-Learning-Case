package edu.imtilsd.rabbimq_case;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

public class OrderService implements Runnable {

    private static final String EXCHANGE_NAME = "order_exchange";
    private static final String ROUTING_KEY = "order.inventory";

    private volatile boolean running = true;
    private final Random random = new Random();

    @Override
    public void run() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(AppConfig.get("rabbitmq.host"));
        factory.setPort(AppConfig.getInt("rabbitmq.port", 5672));
        factory.setUsername(AppConfig.get("rabbitmq.username"));
        factory.setPassword(AppConfig.get("rabbitmq.password"));


        try (Connection rabbitConnection = factory.newConnection();
             Channel channel = rabbitConnection.createChannel()) {

            // 声明交换机
            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);

            // 不断发送随机订单
            while (running) {
                // 1. 随机构造订单
                JSONObject order = createRandomOrder();

                // 2. 插入到订单数据库(简易示例)
                insertOrderIntoDb(order);

                // 3. 发布到 RabbitMQ
                channel.basicPublish(
                        EXCHANGE_NAME,
                        ROUTING_KEY,
                        null,
                        order.toString().getBytes()
                );
                System.out.println("[OrderService] Sent order: " + order);

                // 每隔 2 秒发送一个订单
                Thread.sleep(2000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 随机构造订单
     */
    private JSONObject createRandomOrder() {
        JSONObject orderJson = new JSONObject();
        int orderId = random.nextInt(100000);
        int userId = random.nextInt(1000);
        double price = random.nextDouble() * 1000; // 随机生成价格
        int quantity = 1 + random.nextInt(5);      // 1~5 件

        // 随机生成商品 ID
        String productId = "P" + (100 + random.nextInt(10)); // P100 ~ P109

        orderJson.put("orderId", orderId);
        orderJson.put("userId", userId);
        orderJson.put("productId", productId);
        orderJson.put("quantity", quantity);
        orderJson.put("totalPrice", price);
        orderJson.put("status", "CREATED"); // 初始状态
        return orderJson;
    }

    /**
     * 将订单插入到订单数据库
     */
    private void insertOrderIntoDb(JSONObject order) {
        String sql = "INSERT INTO orders(order_id, user_id, product_id, quantity, total_price, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (java.sql.Connection conn = DbConnection.getOrderDbConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, order.getInt("orderId"));
            stmt.setInt(2, order.getInt("userId"));
            stmt.setString(3, order.getString("productId"));
            stmt.setInt(4, order.getInt("quantity"));
            stmt.setDouble(5, order.getDouble("totalPrice"));
            stmt.setString(6, order.getString("status"));
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void stopService() {
        this.running = false;
    }
}