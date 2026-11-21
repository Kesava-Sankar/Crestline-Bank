package com.crestlinebank;

import com.crestlinebank.ui.LoginFrame;
import com.crestlinebank.ui.SplashScreen;
import com.crestlinebank.ui.Ui;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep Swing's default look and feel if the system theme is unavailable.
        }

        Ui.installTheme();
        SwingUtilities.invokeLater(() -> {
            SplashScreen splash = new SplashScreen(() -> new LoginFrame().setVisible(true));
            splash.showSplash();
        });
    }
}
