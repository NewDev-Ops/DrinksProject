import javax.swing.*;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.*;
import java.util.*;
import java.util.List;

public class CustomerGUI extends JFrame {

  private JComboBox<String> drinkCombo, branchCombo;
  private JTextField quantityField;
  private JButton orderButton;
  private JTextArea outputArea;

  private Map<String, Integer> drinkIds = new HashMap<>();
  private Map<String, Double> drinkPrices = new HashMap<>();
  private Map<String, Integer> branchIds = new HashMap<>();

  public CustomerGUI() {
    setTitle("Drink Ordering System");
    setSize(500, 400);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setLayout(new FlowLayout());

    loadDrinksFromDatabase();
    loadBranchesFromDatabase();

    drinkCombo = new JComboBox<>(drinkIds.keySet().toArray(new String[0]));
    branchCombo = new JComboBox<>(branchIds.keySet().toArray(new String[0]));

    quantityField = new JTextField(5);
    orderButton = new JButton("Place Order");
    outputArea = new JTextArea(10, 40);
    outputArea.setEditable(false);

    add(new JLabel("Select Branch:"));
    add(branchCombo);
    add(new JLabel("Select Drink:"));
    add(drinkCombo);
    add(new JLabel("Quantity:"));
    add(quantityField);
    add(orderButton);
    add(new JScrollPane(outputArea));

    orderButton.addActionListener(e -> placeOrder());

    setVisible(true);
  }

  private void loadDrinksFromDatabase() {
    try (Connection conn = DriverManager.getConnection("jdbc:mysql://10.116.116.133:3306/drinks", "root", "")) {
      String sql = "SELECT drink_id, name, prices FROM drinks";
      PreparedStatement stmt = conn.prepareStatement(sql);
      ResultSet rs = stmt.executeQuery();
      while (rs.next()) {
        int id = rs.getInt("drink_id");
        String name = rs.getString("name");
        double price = rs.getDouble("prices");

        drinkIds.put(name, id);
        drinkPrices.put(name, price);
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, "Failed to load drinks from DB.\n" + e.getMessage());
    }
  }

  private void loadBranchesFromDatabase() {
    try (Connection conn = DriverManager.getConnection("jdbc:mysql://10.116.116.133:3306/drinks", "root", "")) {
      String sql = "SELECT branch_id, name FROM branches";
      PreparedStatement stmt = conn.prepareStatement(sql);
      ResultSet rs = stmt.executeQuery();
      while (rs.next()) {
        int id = rs.getInt("branch_id");
        String name = rs.getString("name");
        branchIds.put(name, id);
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, "Failed to load branches from DB.\n" + e.getMessage());
    }
  }

  private void placeOrder() {
    try {
      String selectedDrink = (String) drinkCombo.getSelectedItem();
      String selectedBranch = (String) branchCombo.getSelectedItem();
      int quantity = Integer.parseInt(quantityField.getText());

      int drinkId = drinkIds.get(selectedDrink);
      int branchId = branchIds.get(selectedBranch);

      // Check stock before placing the order
      int currentStock = getCurrentStock(drinkId, branchId);

      if (currentStock < quantity) {
        JOptionPane.showMessageDialog(this,
          "Not enough stock available.\nCurrent stock: " + currentStock,
          "Stock Alert", JOptionPane.WARNING_MESSAGE);
        return;
      }

      // Restock alert if stock after order would be too low
      if (currentStock - quantity <= 5) {
        int confirm = JOptionPane.showConfirmDialog(this,
          "Warning: Stock will be low after this order (" + (currentStock - quantity) + " left).\nDo you still want to proceed?",
          "Restock Alert", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
          return;
        }
      }

      // Proceed to place order via RMI
      OrderItem item = new OrderItem(drinkId, quantity);
      List<OrderItem> items = new ArrayList<>();
      items.add(item);

      Registry registry = LocateRegistry.getRegistry("10.116.116.133", 1099);
      OrderingService service = (OrderingService) registry.lookup("OrderService");

      int customerId = 1; // Set dynamically or hardcode for now
      String response = service.placeOrder(customerId, branchId, items);
      outputArea.setText(response);

    } catch (NumberFormatException nfe) {
      JOptionPane.showMessageDialog(this, "Please enter a valid quantity.");
    } catch (Exception e) {
      e.printStackTrace();
      outputArea.setText("Error: " + e.getMessage());
    }
  }

  private int getCurrentStock(int drinkId, int branchId) {
    int stock = 0;
    try (Connection conn = DriverManager.getConnection("jdbc:mysql://10.116.116.133:3306/drinks", "root", "")) {
      String sql = "SELECT quantity FROM stock WHERE drink_id = ? AND branch_id = ?";
      PreparedStatement stmt = conn.prepareStatement(sql);
      stmt.setInt(1, drinkId);
      stmt.setInt(2, branchId);
      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        stock = rs.getInt("quantity");
      }
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(this, "Failed to check stock.\n" + e.getMessage());
    }
    return stock;
  }


  public static void main(String[] args) {
    SwingUtilities.invokeLater(CustomerGUI::new);
  }
}
