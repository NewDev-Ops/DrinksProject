import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.*;
import java.util.*;
import java.util.List;

public class CustomerGUI extends JFrame {

    private JComboBox<String> drinkCombo;
    private JTextField quantityField;
    private JButton orderButton;
    private JTextArea outputArea;

    private Map<String, Integer> drinkIds = new HashMap<>();
    private Map<String, Double> drinkPrices = new HashMap<>();

    public CustomerGUI() {
        setTitle("Drink Ordering System");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        loadDrinksFromDatabase(); // ⬅️ Load drinks dynamically

        drinkCombo = new JComboBox<>(drinkIds.keySet().toArray(new String[0]));
        quantityField = new JTextField(5);
        orderButton = new JButton("Place Order");
        outputArea = new JTextArea(10, 30);
        outputArea.setEditable(false);

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
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/drinks", "root", "")) {
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

    private void placeOrder() {
        try {
            String selectedDrink = (String) drinkCombo.getSelectedItem();
            int quantity = Integer.parseInt(quantityField.getText());

            int drinkId = drinkIds.get(selectedDrink);
            OrderItem item = new OrderItem(drinkId, quantity);
            List<OrderItem> items = new ArrayList<>();
            items.add(item);

            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            OrderingService service = (OrderingService) registry.lookup("OrderService");

            int customerId = 1; // Example
            int branchId = 2;   // Example
            String response = service.placeOrder(customerId, branchId, items);
            outputArea.setText(response);

        } catch (Exception e) {
            e.printStackTrace();
            outputArea.setText("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CustomerGUI::new);
    }
}
