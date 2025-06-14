// === AdminGUI.java ===
import javax.swing.*;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class AdminGUI extends JFrame {
    private JTextArea reportArea;
    private JButton refreshButton;
    private OrderingService service;

    public AdminGUI() {
        setTitle("Reports Panel");
        setSize(1280, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        reportArea = new JTextArea(40, 80);
        reportArea.setEditable(false);
        refreshButton = new JButton("Refresh Reports");

        JPanel panel = new JPanel();
        panel.add(refreshButton);
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

        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            service = (OrderingService) registry.lookup("OrderService");
        } catch (Exception e) {
            reportArea.setText("Error connecting to RMI: " + e.getMessage());
        }

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AdminGUI::new);
    }
}