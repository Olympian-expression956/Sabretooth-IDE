package com.vitalsoft.sabretooth;

import com.vitalsoft.sabretooth.ui.MainWindow;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Set system look and feel with FlatLaf-style dark theme via UIManager
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // fallback
        }

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
