package edu.imtilsd.rabbimq_case;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {

    /**
     * Retrieve order database connection
     */
    public static Connection getOrderDbConnection() throws SQLException {
        String url = AppConfig.get("orderdb.url");
        String user = AppConfig.get("orderdb.username");
        String pass = AppConfig.get("orderdb.password");
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * Retrieve inventory database connection
     */
    public static Connection getInventoryDbConnection() throws SQLException {
        String url = AppConfig.get("inventorydb.url");
        String user = AppConfig.get("inventorydb.username");
        String pass = AppConfig.get("inventorydb.password");
        return DriverManager.getConnection(url, user, pass);
    }
}

