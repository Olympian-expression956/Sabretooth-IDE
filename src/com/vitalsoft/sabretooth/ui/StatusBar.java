package com.vitalsoft.sabretooth.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class StatusBar extends JPanel {
    private final JLabel leftLabel;
    private final JLabel centerLabel;
    private final JLabel rightLabel;
    private final JProgressBar progressBar;

    public StatusBar() {
        setLayout(new BorderLayout(8, 0));
        setBackground(Theme.BG_STATUSBAR);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));
        setPreferredSize(new Dimension(0, 24));

        leftLabel = new JLabel("  Ready");
        leftLabel.setForeground(Theme.FG_DIM);
        leftLabel.setFont(Theme.FONT_SMALL);

        centerLabel = new JLabel("");
        centerLabel.setForeground(Theme.FG_BRIGHT);
        centerLabel.setFont(Theme.FONT_SMALL);
        centerLabel.setHorizontalAlignment(SwingConstants.CENTER);

        rightLabel = new JLabel("  SabreTooth IDE  ");
        rightLabel.setForeground(Theme.FG_DIM);
        rightLabel.setFont(Theme.FONT_SMALL);
        rightLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setBackground(Theme.BG_STATUSBAR);
        progressBar.setForeground(Theme.FG_ACCENT);
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(120, 14));
        progressBar.setVisible(false);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(Theme.BG_STATUSBAR);
        right.add(progressBar);
        right.add(rightLabel);

        add(leftLabel, BorderLayout.WEST);
        add(centerLabel, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }

    public void setStatus(String text) {
        SwingUtilities.invokeLater(() -> leftLabel.setText("  " + text));
    }

    public void setStatusOk(String text) {
        SwingUtilities.invokeLater(() -> {
            leftLabel.setForeground(Theme.FG_SUCCESS);
            leftLabel.setText("  ✓ " + text);
        });
    }

    public void setStatusError(String text) {
        SwingUtilities.invokeLater(() -> {
            leftLabel.setForeground(Theme.FG_RED);
            leftLabel.setText("  ✗ " + text);
        });
    }

    public void setStatusNormal(String text) {
        SwingUtilities.invokeLater(() -> {
            leftLabel.setForeground(Theme.FG_DIM);
            leftLabel.setText("  " + text);
        });
    }

    public void setCaretInfo(int line, int col, String lang) {
        SwingUtilities.invokeLater(() ->
            rightLabel.setText("  Ln " + line + ", Col " + col + "  |  " + lang + "  "));
    }

    public void setProjectName(String name) {
        SwingUtilities.invokeLater(() -> centerLabel.setText(name));
    }

    public void showProgress(boolean show) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setVisible(show);
            progressBar.setIndeterminate(show);
        });
    }
}
