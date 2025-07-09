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
    updateStockButton = new JButton("Restock");

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

    // Fetch report from RMI
    refreshButton.addActionListener(e -> {
      try {
        String report = service.generateReport();
        reportArea.setText(report);
      } catch (Exception ex) {
        reportArea.setText("Error fetching report: " + ex.getMessage());
      }
    });

    // Handle restocking logic
    updateStockButton.addActionListener(e -> {
      try {
        String selectedBranch = (String) branchCombo.getSelectedItem();
        String selectedDrink = (String) drinkCombo.getSelectedItem();
        int quantity = Integer.parseInt(quantityField.getText());

        int branchId = branchIds.get(selectedBranch);
        int drinkId = drinkIds.get(selectedDrink);

        try (Connection conn = DatabaseConnection.getConnection()) {
          conn.setAutoCommit(false);

          // If restocking Nairobi directly (HQ)
          if (selectedBranch.equalsIgnoreCase("NAIROBI")) {
            PreparedStatement restockHQ = conn.prepareStatement(
              "UPDATE stock SET quantity = quantity + ? WHERE branch_id = ? AND drink_id = ?"
            );
            restockHQ.setInt(1, quantity);
            restockHQ.setInt(2, branchId);
            restockHQ.setInt(3, drinkId);
            int rows = restockHQ.executeUpdate();

            if (rows > 0) {
              conn.commit();
              JOptionPane.showMessageDialog(this, "Nairobi restocked successfully.");
            } else {
              JOptionPane.showMessageDialog(this, "Failed to restock Nairobi.");
            }
            return;
          }

          // Else: transfer from Nairobi to branch
          PreparedStatement checkHQ = conn.prepareStatement(
            "SELECT quantity FROM stock WHERE branch_id = (SELECT branch_id FROM branches WHERE name = 'NAIROBI') AND drink_id = ?"
          );
          checkHQ.setInt(1, drinkId);
          ResultSet rs = checkHQ.executeQuery();

          if (!rs.next() || rs.getInt("quantity") < quantity) {
            JOptionPane.showMessageDialog(this, "Not enough stock in HQ (Nairobi).");
            return;
          }

          // Deduct from HQ
          PreparedStatement deductHQ = conn.prepareStatement(
            "UPDATE stock SET quantity = quantity - ? WHERE branch_id = (SELECT branch_id FROM branches WHERE name = 'NAIROBI') AND drink_id = ?"
          );
          deductHQ.setInt(1, quantity);
          deductHQ.setInt(2, drinkId);
          deductHQ.executeUpdate();

          // Add to selected branch
          PreparedStatement addToBranch = conn.prepareStatement(
            "UPDATE stock SET quantity = quantity + ? WHERE branch_id = ? AND drink_id = ?"
          );
          addToBranch.setInt(1, quantity);
          addToBranch.setInt(2, branchId);
          addToBranch.setInt(3, drinkId);
          int rows = addToBranch.executeUpdate();

          conn.commit();

          if (rows > 0) {
            JOptionPane.showMessageDialog(this, "Stock transferred from Nairobi to " + selectedBranch + " successfully.");
          } else {
            JOptionPane.showMessageDialog(this, "Failed to update stock in branch.");
          }

        }

      } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
      }
    });

    // Connect to RMI server
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
