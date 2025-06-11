import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class Client {
    public static void main(String[] args) {
        try {
            // Connect to RMI server
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            OrderingService service = (OrderingService) registry.lookup("OrderService");

            // Create an example order
            List<OrderItem> items = new ArrayList<>();
            items.add(new OrderItem(1, 2)); // drink_id = 1, quantity = 2
            items.add(new OrderItem(3, 1)); // drink_id = 3, quantity = 1

            // Place the order
            String result = service.placeOrder(1001, 1, items); // customer_id, branch_id
            System.out.println(result);

            // Get report
            String report = service.generateReport();
            System.out.println("\n" + report);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
