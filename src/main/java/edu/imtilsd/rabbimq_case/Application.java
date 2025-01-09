package edu.imtilsd.rabbimq_case;

public class Application {

    public static void main(String[] args) {
        // 正常情况下，你可以提示用户先运行 init_db.sh
        System.out.println("Please ensure you have run `./init_db.sh` to initialize the databases.");

        OrderService orderService = new OrderService();
        InventoryService inventoryService = new InventoryService();
        PaymentService paymentService = new PaymentService();
        DeliveryService deliveryService = new DeliveryService();

        Thread t1 = new Thread(orderService, "OrderService-Thread");
        Thread t2 = new Thread(inventoryService, "InventoryService-Thread");
        Thread t3 = new Thread(paymentService, "PaymentService-Thread");
        Thread t4 = new Thread(deliveryService, "DeliveryService-Thread");

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        System.out.println("All services started. Press Ctrl+C to stop.");
    }
}
