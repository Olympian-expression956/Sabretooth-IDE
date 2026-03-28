package com.vitalsoft.sabretooth.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ConsolePanel extends JPanel {
    private final JTextPane textPane;
    private final StyledDocument doc;
    private final SimpleAttributeSet defaultStyle;
    private final SimpleAttributeSet errorStyle;
    private final SimpleAttributeSet successStyle;
    private final SimpleAttributeSet warningStyle;
    private final SimpleAttributeSet headerStyle;
    private final SimpleAttributeSet dimStyle;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ConsolePanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG_CONSOLE);

        // Header bar
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG_TOOLBAR);
        header.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER));
        header.setPreferredSize(new Dimension(0, 26));

        JLabel title = new JLabel("  Build / Run");
        title.setFont(Theme.FONT_UI.deriveFont(Font.BOLD));
        title.setForeground(Theme.FG_BRIGHT);
        header.add(title, BorderLayout.WEST);

        JButton clearBtn = new JButton("Clear");
        clearBtn.setBackground(Theme.BG_BUTTON);
        clearBtn.setForeground(Theme.FG_BRIGHT);
        clearBtn.setFont(Theme.FONT_SMALL);
        clearBtn.setBorderPainted(false);
        clearBtn.setFocusPainted(false);
        clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearBtn.addActionListener(e -> clear());
        header.add(clearBtn, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBackground(Theme.BG_CONSOLE);
        textPane.setForeground(Theme.FG_DEFAULT);
        textPane.setFont(Theme.FONT_CODE);
        textPane.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        doc = textPane.getStyledDocument();

        // Styles
        defaultStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(defaultStyle, Theme.FG_DEFAULT);
        StyleConstants.setBackground(defaultStyle, Theme.BG_CONSOLE);
        StyleConstants.setFontFamily(defaultStyle, Theme.FONT_CODE.getFamily());
        StyleConstants.setFontSize(defaultStyle, Theme.FONT_CODE.getSize());

        errorStyle = new SimpleAttributeSet(defaultStyle);
        StyleConstants.setForeground(errorStyle, Theme.FG_RED);
        StyleConstants.setBold(errorStyle, true);

        successStyle = new SimpleAttributeSet(defaultStyle);
        StyleConstants.setForeground(successStyle, Theme.FG_SUCCESS);
        StyleConstants.setBold(successStyle, true);

        warningStyle = new SimpleAttributeSet(defaultStyle);
        StyleConstants.setForeground(warningStyle, Theme.FG_WARNING);

        headerStyle = new SimpleAttributeSet(defaultStyle);
        StyleConstants.setForeground(headerStyle, Theme.FG_ACCENT);
        StyleConstants.setBold(headerStyle, true);

        dimStyle = new SimpleAttributeSet(defaultStyle);
        StyleConstants.setForeground(dimStyle, Theme.FG_DIM);

        JScrollPane scroll = new JScrollPane(textPane);
        scroll.setBackground(Theme.BG_CONSOLE);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BG_CONSOLE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scroll, BorderLayout.CENTER);
    }

    public void appendLine(String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet style = classifyLine(text);
                String ts = "[" + LocalTime.now().format(TIME_FMT) + "] ";
                doc.insertString(doc.getLength(), ts, dimStyle);
                doc.insertString(doc.getLength(), text + "\n", style);
                // Auto-scroll to bottom
                textPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    public void appendRaw(String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.insertString(doc.getLength(), text + "\n", defaultStyle);
                textPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    private SimpleAttributeSet classifyLine(String text) {
        if (text == null) return defaultStyle;
        String lower = text.toLowerCase();
        if (lower.contains("error") || lower.contains("failed") || lower.contains("✗")
                || lower.startsWith("e:") || lower.contains("exception")) {
            return errorStyle;
        }
        if (lower.contains("warning") || lower.contains("warn")) return warningStyle;
        if (lower.contains("build successful") || lower.contains("✓") || lower.contains(":app:assemble")) {
            return successStyle;
        }
        if (text.startsWith("[SabreTooth]")) return headerStyle;
        if (text.startsWith("─")) return dimStyle;
        return defaultStyle;
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.remove(0, doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    public void printBuildStart(String task) {
        SwingUtilities.invokeLater(() -> {
            appendLine("[SabreTooth] Starting: " + task);
        });
    }

    public void printBuildEnd(boolean success, String apkPath) {
        SwingUtilities.invokeLater(() -> {
            if (success) {
                try {
                    doc.insertString(doc.getLength(), "\n✓ BUILD SUCCESSFUL\n\n", successStyle);
                    if (apkPath != null) {
                        doc.insertString(doc.getLength(), "APK: " + apkPath + "\n\n", headerStyle);
                    }
                } catch (BadLocationException ignored) {}
            } else {
                try {
                    doc.insertString(doc.getLength(), "\n✗ BUILD FAILED\n\n", errorStyle);
                } catch (BadLocationException ignored) {}
            }
            textPane.setCaretPosition(doc.getLength());
        });
    }
}
