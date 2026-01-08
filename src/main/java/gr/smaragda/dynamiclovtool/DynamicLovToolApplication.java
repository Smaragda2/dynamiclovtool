package gr.smaragda.dynamiclovtool;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@SpringBootApplication
public class DynamicLovToolApplication {
    @Value("${app.version:0.0.2}")
    private String appVersion;

    @Value("${app.name:Dynamic Lov Tool (Dev)}")
    private String appName;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(new Class[]{DynamicLovToolApplication.class});
        app.setHeadless(false);
        ConfigurableApplicationContext ctx = app.run(args);

        DynamicLovToolApplication appInstance = ctx.getBean(DynamicLovToolApplication.class);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(String.format(String.format("%s v%s", appInstance.appName, appInstance.appVersion)));
            frame.setSize(450, 160);
            frame.setLocationRelativeTo((Component)null);
            frame.setDefaultCloseOperation(3);
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, 1));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            JLabel infoLabel = new JLabel("<html>Η εφαρμογή τρέχει στο: <b>http://localhost:8089</b></html>", 0);
            infoLabel.setAlignmentX(0.5F);
            JLabel warningLabel = new JLabel("<html><span style='color:#b30000;'>⚠ Κλείνοντας αυτό το παράθυρο, θα σταματήσει και η εφαρμογή!</span></html>", 0);
            warningLabel.setAlignmentX(0.5F);
            warningLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            panel.add(infoLabel);
            panel.add(warningLabel);
            frame.setContentPane(panel);
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    System.out.println(">>> Shutting down Spring Boot...");
                    ctx.close();
                    System.exit(0);
                }
            });
            frame.setVisible(true);
        });
    }

    @EventListener({ApplicationReadyEvent.class})
    public void openBrowser() {
        System.out.println(">>> Application is ready, trying to open browser...");

        try {
            if (!Desktop.isDesktopSupported()) {
                System.out.println(">>> Desktop is NOT supported");
                return;
            }

            Desktop.getDesktop().browse(new URI("http://localhost:8089/"));
            System.out.println(">>> Browser opened");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
