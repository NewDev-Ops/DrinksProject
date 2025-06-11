import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

public class MainServer {
    public static void main(String[] args){
        try{
            LocateRegistry.createRegistry(1099);
            OrderingService service = new OrderingServiceImp() {
                public String placeOrder(int customerId, int branchId, List<OrderItem> items) {
                    return "";
                }

                public String generateReport() throws RemoteException {
                    return "";
                }
            };
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("OrderService", service);
            System.out.println("Server ready");
        } catch (Exception error) {
            error.printStackTrace();
        }
    }
}
