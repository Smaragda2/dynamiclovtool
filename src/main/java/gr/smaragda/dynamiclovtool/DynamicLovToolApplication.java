package gr.smaragda.dynamiclovtool;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;

@SpringBootApplication
public class DynamicLovToolApplication {
	public static void main(String[] args) {
        // Set system properties for better Windows compatibility
        System.setProperty("java.awt.headless", "false");
        System.setProperty("spring.main.web-application-type", "SERVLET");

        SpringApplication app = new SpringApplication(DynamicLovToolApplication.class);
        app.setHeadless(false);

        // Add banner and logging
        app.setBannerMode(Banner.Mode.CONSOLE);
        app.setLogStartupInfo(true);

        ConfigurableApplicationContext ctx = app.run(args);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dynamic Lov Tool");
            frame.setSize(450, 160);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            // Layout
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Main info label
            JLabel infoLabel = new JLabel(
                    "<html>Η εφαρμογή τρέχει στο: <b>http://localhost:8089</b></html>",
                    SwingConstants.CENTER
            );
            infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Warning label
            JLabel warningLabel = new JLabel(
                    "<html><span style='color:#b30000;'>⚠ Κλείνοντας αυτό το παράθυρο, θα σταματήσει και η εφαρμογή!</span></html>",
                    SwingConstants.CENTER
            );
            warningLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            warningLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

            panel.add(infoLabel);
            panel.add(warningLabel);

            frame.setContentPane(panel);

            // Clean shutdown when window closes
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    System.out.println(">>> Shutting down Spring Boot...");
                    ctx.close();
                    System.exit(0);
                }
            });

            frame.setVisible(true);
        });
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        System.out.println(">>> Application is ready, trying to open browser...");
        try {
            if (!Desktop.isDesktopSupported()) {
                System.out.println(">>> Desktop is NOT supported");
                return;
            }
            Desktop.getDesktop().browse(new URI("http://localhost:" + 8089 + "/"));
            System.out.println(">>> Browser opened");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
