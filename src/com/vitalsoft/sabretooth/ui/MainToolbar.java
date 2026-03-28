package com.vitalsoft.sabretooth.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainToolbar extends JToolBar {
    private final JButton buildDebugBtn;
    private final JButton buildReleaseBtn;
    private final JButton cleanBtn;
    private final JButton stopBtn;
    private final JButton saveBtn;
    private final JButton saveAllBtn;

    public MainToolbar() {
        setFloatable(false);
        setBackground(Theme.BG_TOOLBAR);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        setPreferredSize(new Dimension(0, 38));

        add(Box.createHorizontalStrut(6));

        saveBtn    = addToolBtn("💾", "Save (Ctrl+S)", Theme.FG_BRIGHT);
        saveAllBtn = addToolBtn("📁", "Save All (Ctrl+Shift+S)", Theme.FG_BRIGHT);

        addSeparator(new Dimension(10, 30));

        buildDebugBtn   = addToolBtn("▶ Debug", "Build Debug APK", Theme.FG_GREEN);
        buildReleaseBtn = addToolBtn("▶ Release", "Build Release APK", Theme.FG_ORANGE);
        cleanBtn        = addToolBtn("🧹 Clean", "Clean Build", Theme.FG_DIM);
        stopBtn         = addToolBtn("■ Stop", "Cancel Build", Theme.FG_RED);
        stopBtn.setEnabled(false);

        add(Box.createHorizontalGlue());

        // Project label area
        JLabel spacer = new JLabel("  ⚔ SabreTooth IDE  ");
        spacer.setForeground(Theme.FG_DIM);
        spacer.setFont(Theme.FONT_SMALL);
        add(spacer);
    }

    private JButton addToolBtn(String text, String tooltip, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_UI);
        btn.setForeground(fg);
        btn.setBackground(Theme.BG_TOOLBAR);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setFocusPainted(false);
        btn.setToolTipText(tooltip);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(Theme.BG_HOVER);
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(Theme.BG_TOOLBAR);
            }
        });
        add(btn);
        add(Box.createHorizontalStrut(2));
        return btn;
    }

    public JButton getBuildDebugBtn()   { return buildDebugBtn; }
    public JButton getBuildReleaseBtn() { return buildReleaseBtn; }
    public JButton getCleanBtn()        { return cleanBtn; }
    public JButton getStopBtn()         { return stopBtn; }
    public JButton getSaveBtn()         { return saveBtn; }
    public JButton getSaveAllBtn()      { return saveAllBtn; }

    public void setBuildRunning(boolean running) {
        SwingUtilities.invokeLater(() -> {
            buildDebugBtn.setEnabled(!running);
            buildReleaseBtn.setEnabled(!running);
            cleanBtn.setEnabled(!running);
            stopBtn.setEnabled(running);
        });
    }
}
