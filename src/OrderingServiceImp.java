
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.sql.*;

public class OrderingServiceImp extends UnicastRemoteObject implements OrderingService {

    protected OrderingServiceImp() throws RemoteException {
        super();
    }
    
    public String placeOrder(int customerId, int branchId, List<OrderItem> items) throws RemoteException {
        try (Connection conn =  DatabaseConnection.getConnection()){
            conn.setAutoCommit(false);

            String insertOrder = "INSERT INTO Orders (order_id, customer_id, branch_id, order_date) VALUES (?, ?, ?, CURDATE())";
            String insertdetails = "INSERT INTO Order_Details (order_detail_id, order_id, drink_id, quantity, subtotal) VALUES (?, ?, ?, ?, ?)";
            String updatestock = "UPDATE Stock SET quantity = quantity - ? WHERE branch_id = ? AND drink_id = ?";

            int OrderId = (int)(Math.random()* 10000);//Generates a random order ID between 0–9999
            PreparedStatement placing = conn.prepareStatement(insertOrder);


            placing.setInt(1, OrderId);
            placing.setInt(2, customerId);
            placing.setInt(3, branchId);
            placing.executeUpdate();


            int detailid = (int)(Math.random()* 10000);
            for(OrderItem item : items){
                double price = getPrice(conn, item.drinkId);
                double subtotal = price * item.quantity;

                PreparedStatement place_detail = conn.prepareStatement(insertdetails);
                place_detail.setInt(1, detailid);
                place_detail.setInt(2, OrderId);
                place_detail.setInt(3, item.drinkId);
                place_detail.setInt(4, item.quantity);
                place_detail.setDouble(5, subtotal);
                place_detail.executeUpdate();

                PreparedStatement place_stock = conn.prepareStatement(updatestock);
                place_stock.setInt(1, item.quantity);
                place_stock.setInt(2, branchId);
                place_stock.setInt(3, item.drinkId);
                place_stock.executeUpdate();
            }

            conn.commit();
            return "Your order for Order Placed";
           } catch (Exception error) {
            error.printStackTrace();
            return "Error" + error.getMessage();

        }
    }

    private double getPrice(Connection conn, int drinkId) throws SQLException {
        String sql = "SELECT prices FROM drinks WHERE drink_id = ?";
        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setInt(1, drinkId);
        ResultSet rs = statement.executeQuery();

        if (rs.next()) return rs.getDouble("prices"); {
            return 0;
        }

    }

  public String generateReport() throws RemoteException {
    StringBuilder sb = new StringBuilder();
    try (Connection conn = DatabaseConnection.getConnection()) {


      sb.append("=== Sales by Branch ===\n");
      Statement stmt1 = conn.createStatement();
      ResultSet rs1 = stmt1.executeQuery(
        "SELECT b.name AS branch, SUM(od.subtotal) AS total_sales " +
          "FROM Order_Details od " +
          "JOIN Orders o ON od.order_id = o.order_id " +
          "JOIN Branches b ON o.branch_id = b.branch_id " +
          "GROUP BY b.name"
      );
      while (rs1.next()) {
        sb.append(rs1.getString("branch")).append(": Kes ")
          .append(rs1.getDouble("total_sales")).append("\n");
      }


      sb.append("\n=== Remaining Stock ===\n");
      Statement stmt2 = conn.createStatement();
      ResultSet rs2 = stmt2.executeQuery(
        "SELECT b.name AS branch, d.name AS drink, s.quantity " +
          "FROM Stock s " +
          "JOIN Branches b ON s.branch_id = b.branch_id " +
          "JOIN Drinks d ON s.drink_id = d.drink_id " +
          "ORDER BY b.name"
      );
      while (rs2.next()) {
        sb.append(rs2.getString("branch")).append(" - ")
          .append(rs2.getString("drink")).append(": ")
          .append(rs2.getInt("quantity")).append("\n");
      }

      sb.append("\n=== Customer Purchases ===\n");
      Statement stmt3 = conn.createStatement();
      ResultSet rs3 = stmt3.executeQuery(
        "SELECT c.name AS customer, b.name AS branch, d.name AS drink, od.quantity " +
          "FROM Order_Details od " +
          "JOIN Orders o ON od.order_id = o.order_id " +
          "JOIN Customers c ON o.customer_id = c.customer_id " +
          "JOIN Branches b ON o.branch_id = b.branch_id " +
          "JOIN Drinks d ON od.drink_id = d.drink_id " +
          "ORDER BY customer"
      );
      while (rs3.next()) {
        sb.append(rs3.getString("customer")).append(" bought ")
          .append(rs3.getInt("quantity")).append(" of ")
          .append(rs3.getString("drink")).append(" from ")
          .append(rs3.getString("branch")).append("\n");
      }

    } catch (Exception e) {
      e.printStackTrace();
      return "Error generating report: " + e.getMessage();
    }
    return sb.toString();
  }

}
