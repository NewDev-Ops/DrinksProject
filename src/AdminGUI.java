import javax.swing.*;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class AdminGUI extends JFrame {
  private JTextArea reportArea;
  private JButton refreshButton;

  private JComboBox<String> branchCombo, drinkCombo;
  private JTextField quantityField;
  private JButton updateStockButton;

  private OrderingService service;
  private Map<String, Integer> branchIds = new HashMap<>();
  private Map<String, Integer> drinkIds = new HashMap<>();

  public AdminGUI() {
    setTitle("Admin Panel");
    setSize(900, 600);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    reportArea = new JTextArea(25, 60);
    reportArea.setEditable(false);

    refreshButton = new JButton("Refresh Reports");
    branchCombo = new JComboBox<>();
    drinkCombo = new JComboBox<>();
    quantityField = new JTextField(5);
    updateStockButton = new JButton("Update Stock");

    JPanel panel = new JPanel();
    panel.setLayout(new FlowLayout());

    panel.add(refreshButton);
    panel.add(new JLabel("Branch:"));
    panel.add(branchCombo);
    panel.add(new JLabel("Drink:"));
    panel.add(drinkCombo);
    panel.add(new JLabel("Qty:"));
    panel.add(quantityField);
    panel.add(updateStockButton);
    panel.add(new JScrollPane(reportArea));

    getContentPane().add(panel);

    refreshButton.addActionListener(e -> {
      try {
        String report = service.generateReport();
        reportArea.setText(report);
      } catch (Exception ex) {
        reportArea.setText("Error fetching report: " + ex.getMessage());
      }
    });

    updateStockButton.addActionListener(e -> {
      try {
        int branchId = branchIds.get((String) branchCombo.getSelectedItem());
        int drinkId = drinkIds.get((String) drinkCombo.getSelectedItem());
        int quantity = Integer.parseInt(quantityField.getText());

        try (Connection conn = DatabaseConnection.getConnection()) {
          PreparedStatement stmt = conn.prepareStatement(
            "UPDATE stock SET quantity = quantity + ? WHERE branch_id = ? AND drink_id = ?"
          );
          stmt.setInt(1, quantity);
          stmt.setInt(2, branchId);
          stmt.setInt(3, drinkId);
          int rows = stmt.executeUpdate();
          if (rows > 0) {
            JOptionPane.showMessageDialog(this, "Stock updated successfully.");
          } else {
            JOptionPane.showMessageDialog(this, "Stock update failed or entry not found.");
          }
        }

      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
      }
    });

    try {
      Registry registry = LocateRegistry.getRegistry("localhost", 1099);
      service = (OrderingService) registry.lookup("OrderService");
    } catch (Exception e) {
      reportArea.setText("Error connecting to RMI: " + e.getMessage());
    }

    loadBranches();
    loadDrinks();

    setVisible(true);
  }

  private void loadBranches() {
    try (Connection conn = DatabaseConnection.getConnection()) {
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT branch_id, name FROM branches");
      while (rs.next()) {
        int id = rs.getInt("branch_id");
        String name = rs.getString("name");
        branchIds.put(name, id);
        branchCombo.addItem(name);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void loadDrinks() {
    try (Connection conn = DatabaseConnection.getConnection()) {
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT drink_id, name FROM drinks");
      while (rs.next()) {
        int id = rs.getInt("drink_id");
        String name = rs.getString("name");
        drinkIds.put(name, id);
        drinkCombo.addItem(name);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(AdminGUI::new);
  }
}
