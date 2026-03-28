package com.vitalsoft.sabretooth.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class EditorTabPanel extends JPanel {
    private final JTabbedPane tabs;
    private final Map<String, CodeEditorPane> openFiles = new LinkedHashMap<>();
    private Runnable onTabChange;

    public EditorTabPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG_DARK);

        tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setBackground(Theme.BG_PANEL);
        tabs.setForeground(Theme.FG_BRIGHT);
        tabs.setFont(Theme.FONT_UI);
        tabs.setBorder(BorderFactory.createEmptyBorder());

        // Custom tab UI
        try {
            tabs.setUI(new DarkTabbedPaneUI());
        } catch (Exception ignored) {}

        tabs.addChangeListener(e -> {
            if (onTabChange != null) onTabChange.run();
        });

        // Ctrl+W = close current tab
        tabs.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK), "closeTab");
        tabs.getActionMap().put("closeTab", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { closeCurrentTab(); }
        });

        // Middle-click to close
        tabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    int idx = tabs.indexAtLocation(e.getX(), e.getY());
                    if (idx >= 0) closeTabAt(idx);
                }
            }
        });

        add(tabs, BorderLayout.CENTER);

        // Empty state
        showWelcome();
    }

    private void showWelcome() {
        JPanel welcome = new JPanel(new GridBagLayout());
        welcome.setBackground(Theme.BG_EDITOR);
        welcome.setName("__welcome__");

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(Theme.BG_EDITOR);

        JLabel logo = new JLabel("⚔ SabreTooth IDE");
        logo.setFont(Theme.FONT_UI.deriveFont(Font.BOLD, 28f));
        logo.setForeground(Theme.FG_ACCENT);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Lightweight Android Development");
        sub.setFont(Theme.FONT_UI.deriveFont(15f));
        sub.setForeground(Theme.FG_DIM);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("Open a project or create a new one to get started.");
        hint.setFont(Theme.FONT_UI);
        hint.setForeground(Theme.FG_DIM);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(logo);
        inner.add(Box.createVerticalStrut(8));
        inner.add(sub);
        inner.add(Box.createVerticalStrut(24));
        inner.add(hint);

        welcome.add(inner);
        tabs.addTab("Welcome", welcome);
        tabs.setTabComponentAt(0, makeTabHeader("Welcome", null));
    }

    public void openFile(File file) {
        String key = file.getAbsolutePath();

        // If already open, switch to it
        if (openFiles.containsKey(key)) {
            CodeEditorPane existing = openFiles.get(key);
            for (int i = 0; i < tabs.getTabCount(); i++) {
                if (tabs.getComponentAt(i) == existing) {
                    tabs.setSelectedIndex(i);
                    existing.getTextPane().requestFocus();
                    return;
                }
            }
        }

        // Remove welcome tab if it's the only one
        if (tabs.getTabCount() == 1) {
            Component c = tabs.getComponentAt(0);
            if (c instanceof JPanel jp && "__welcome__".equals(jp.getName())) {
                tabs.removeTabAt(0);
            }
        }

        CodeEditorPane editor = new CodeEditorPane();
        try {
            editor.loadFile(file);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Cannot open file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        openFiles.put(key, editor);
        int idx = tabs.getTabCount();
        tabs.addTab(file.getName(), editor);
        tabs.setTabComponentAt(idx, makeTabHeader(file.getName(), file));
        tabs.setSelectedIndex(idx);
        editor.getTextPane().requestFocus();

        // Update tab title when modified
        editor.addModifiedListener(() -> {
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < tabs.getTabCount(); i++) {
                    if (tabs.getComponentAt(i) == editor) {
                        String tabName = file.getName() + (editor.isModified() ? " *" : "");
                        tabs.setTabComponentAt(i, makeTabHeader(tabName, file));
                        break;
                    }
                }
            });
        });
    }

    public void openUntitled(String name, String content, SyntaxHighlighter.Language lang) {
        CodeEditorPane editor = new CodeEditorPane();
        editor.setText(content);
        editor.setLanguage(lang);

        if (tabs.getTabCount() == 1) {
            Component c = tabs.getComponentAt(0);
            if (c instanceof JPanel jp && "__welcome__".equals(jp.getName())) {
                tabs.removeTabAt(0);
            }
        }

        int idx = tabs.getTabCount();
        tabs.addTab(name, editor);
        tabs.setTabComponentAt(idx, makeTabHeader(name, null));
        tabs.setSelectedIndex(idx);
    }

    private JPanel makeTabHeader(String title, File file) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        header.setOpaque(false);

        JLabel lbl = new JLabel(title);
        lbl.setFont(Theme.FONT_UI);
        lbl.setForeground(Theme.FG_BRIGHT);

        JButton closeBtn = new JButton("×");
        closeBtn.setFont(Theme.FONT_UI.deriveFont(14f));
        closeBtn.setForeground(Theme.FG_DIM);
        closeBtn.setBackground(null);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setPreferredSize(new Dimension(18, 18));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { closeBtn.setForeground(Theme.FG_RED); }
            @Override public void mouseExited(MouseEvent e) { closeBtn.setForeground(Theme.FG_DIM); }
        });
        closeBtn.addActionListener(e -> {
            if (file != null) {
                String key = file.getAbsolutePath();
                CodeEditorPane editor = openFiles.get(key);
                if (editor != null && editor.isModified()) {
                    int choice = JOptionPane.showConfirmDialog(this,
                        "Save changes to " + file.getName() + "?", "Unsaved Changes",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                    if (choice == JOptionPane.CANCEL_OPTION) return;
                    if (choice == JOptionPane.YES_OPTION) {
                        try { editor.saveFile(); } catch (IOException ex) {
                            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
                        }
                    }
                }
                openFiles.remove(key);
            }
            // Find tab and close
            for (int i = 0; i < tabs.getTabCount(); i++) {
                Component tc = tabs.getTabComponentAt(i);
                if (tc == header) { closeTabAt(i); return; }
            }
        });

        header.add(lbl);
        header.add(closeBtn);
        return header;
    }

    public void closeCurrentTab() {
        int idx = tabs.getSelectedIndex();
        if (idx >= 0) closeTabAt(idx);
    }

    private void closeTabAt(int idx) {
        Component comp = tabs.getComponentAt(idx);
        if (comp instanceof CodeEditorPane editor) {
            if (editor.isModified()) {
                File f = editor.getFile();
                String name = f != null ? f.getName() : "Untitled";
                int choice = JOptionPane.showConfirmDialog(this,
                    "Save changes to " + name + "?", "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION);
                if (choice == JOptionPane.CANCEL_OPTION) return;
                if (choice == JOptionPane.YES_OPTION) {
                    try { editor.saveFile(); } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
                    }
                }
            }
            if (editor.getFile() != null) {
                openFiles.remove(editor.getFile().getAbsolutePath());
            }
        }
        tabs.removeTabAt(idx);
        if (tabs.getTabCount() == 0) showWelcome();
    }

    public CodeEditorPane getCurrentEditor() {
        Component c = tabs.getSelectedComponent();
        return (c instanceof CodeEditorPane e) ? e : null;
    }

    public void saveCurrentFile() {
        CodeEditorPane e = getCurrentEditor();
        if (e == null) return;
        try {
            if (e.getFile() != null) {
                e.saveFile();
            } else {
                saveCurrentFileAs();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void saveCurrentFileAs() {
        CodeEditorPane e = getCurrentEditor();
        if (e == null) return;
        JFileChooser fc = new JFileChooser(e.getFile() != null ? e.getFile().getParent() : ".");
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                e.saveFileAs(fc.getSelectedFile());
                openFiles.put(fc.getSelectedFile().getAbsolutePath(), e);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
            }
        }
    }

    public void saveAllFiles() {
        for (CodeEditorPane editor : openFiles.values()) {
            if (editor.isModified() && editor.getFile() != null) {
                try { editor.saveFile(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    public void setOnTabChange(Runnable r) { this.onTabChange = r; }

    public Collection<CodeEditorPane> getOpenEditors() { return openFiles.values(); }

    // Simple dark TabbedPane UI
    private static class DarkTabbedPaneUI extends javax.swing.plaf.basic.BasicTabbedPaneUI {
        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
            g.setColor(isSelected ? Theme.BG_TAB_ACTIVE : Theme.BG_TAB_INACTIVE);
            g.fillRect(x, y, w, h);
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int w, int h, boolean isSelected) {
            g.setColor(isSelected ? Theme.BORDER_ACCENT : Theme.BORDER);
            if (isSelected) {
                g.fillRect(x, y + h - 2, w, 2);
            } else {
                g.drawRect(x, y, w - 1, h - 1);
            }
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            g.setColor(Theme.BORDER);
            Rectangle r = tabPane.getBounds();
            int th = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
            g.drawLine(0, th, r.width, th);
        }

        @Override
        protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) { return 0; }
    }
}
