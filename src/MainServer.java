import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MainServer {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099); // Default RMI port
            OrderingService service = new OrderingServiceImp();
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("OrderService", service);
            System.out.println("RMI Server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
