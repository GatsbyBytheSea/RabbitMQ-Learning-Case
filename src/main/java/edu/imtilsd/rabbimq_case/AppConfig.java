package edu.imtilsd.rabbimq_case;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Properties props = new Properties();

    static {
        // 尝试从 resources 目录下加载 application.properties
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                System.err.println("application.properties not found on classpath!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取配置
     */
    public static String get(String key) {
        return props.getProperty(key);
    }

    /**
     * 获取 int 类型配置
     */
    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
