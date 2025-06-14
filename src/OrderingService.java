
import java.rmi.*;
import java.util.*;

public interface OrderingService extends Remote {
    String placeOrder(int customerId, int branchId, List<OrderItem> items ) throws Exception; //This is called by the client class to make an order from a specific branch
    String generateReport() throws RemoteException; // Using this function to create a report

}
