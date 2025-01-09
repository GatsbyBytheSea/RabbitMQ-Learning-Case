package edu.imtilsd.rabbimq_case;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {

    static {
        try {
            // 加载 MySQL 驱动，如果使用了 Maven 依赖可忽略
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取订单数据库连接
     */
    public static Connection getOrderDbConnection() throws SQLException {
        String url = AppConfig.get("orderdb.url");
        String user = AppConfig.get("orderdb.username");
        String pass = AppConfig.get("orderdb.password");
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * 获取库存数据库连接
     */
    public static Connection getInventoryDbConnection() throws SQLException {
        String url = AppConfig.get("inventorydb.url");
        String user = AppConfig.get("inventorydb.username");
        String pass = AppConfig.get("inventorydb.password");
        return DriverManager.getConnection(url, user, pass);
    }
}

