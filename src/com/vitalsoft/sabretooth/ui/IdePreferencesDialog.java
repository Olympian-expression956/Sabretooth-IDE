package com.vitalsoft.sabretooth.ui;

import com.vitalsoft.sabretooth.config.IdePreferences;
import com.vitalsoft.sabretooth.util.ToolDetector;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class IdePreferencesDialog extends JDialog {
    private JSpinner fontSizeSpinner;
    private JSpinner tabSizeSpinner;
    private JCheckBox showLineNums;
    private JCheckBox wordWrap;
    private JTextField androidSdkField;
    private JTextField gradleUserHomeField;
    private JComboBox<ToolDetector.ToolInfo> defaultGradleCombo;
    private JComboBox<ToolDetector.ToolInfo> defaultJdkCombo;

    public IdePreferencesDialog(Frame parent) {
        super(parent, "IDE Preferences", true);
        setPreferredSize(new Dimension(600, 520));
        buildUI();
        loadValues();
        pack();
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(Theme.BG_PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 10, 16));
        setContentPane(root);

        JLabel title = new JLabel("IDE Preferences");
        title.setFont(Theme.FONT_UI.deriveFont(Font.BOLD, 15f));
        title.setForeground(Theme.FG_BRIGHT);
        root.add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Theme.BG_PANEL);
        tabs.setForeground(Theme.FG_BRIGHT);
        root.add(tabs, BorderLayout.CENTER);

        tabs.addTab("Editor", buildEditorTab());
        tabs.addTab("Tools & SDK", buildToolsTab());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.setBackground(Theme.BG_PANEL);

        JButton cancel = btn("Cancel");
        cancel.addActionListener(e -> dispose());

        JButton save = btn("Save");
        save.setBackground(Theme.FG_ACCENT);
        save.setForeground(Color.WHITE);
        save.addActionListener(e -> onSave());

        btns.add(cancel);
        btns.add(save);
        root.add(btns, BorderLayout.SOUTH);
    }

    private JPanel buildEditorTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = gbc();

        int row = 0;

        addLbl(p, gbc, row, "Font Size:");
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(13, 8, 30, 1));
        styleSpinner(fontSizeSpinner);
        gbc.gridx = 1; gbc.gridy = row++;
        p.add(fontSizeSpinner, gbc);

        addLbl(p, gbc, row, "Tab Size:");
        tabSizeSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 8, 1));
        styleSpinner(tabSizeSpinner);
        gbc.gridx = 1; gbc.gridy = row++;
        p.add(tabSizeSpinner, gbc);

        showLineNums = new JCheckBox("Show Line Numbers");
        showLineNums.setBackground(Theme.BG_PANEL);
        showLineNums.setForeground(Theme.FG_BRIGHT);
        showLineNums.setFont(Theme.FONT_UI);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        p.add(showLineNums, gbc);
        gbc.gridwidth = 1;

        wordWrap = new JCheckBox("Word Wrap");
        wordWrap.setBackground(Theme.BG_PANEL);
        wordWrap.setForeground(Theme.FG_BRIGHT);
        wordWrap.setFont(Theme.FONT_UI);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        p.add(wordWrap, gbc);
        gbc.gridwidth = 1;

        return p;
    }

    private JPanel buildToolsTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = gbc();

        int row = 0;

        // Android SDK
        addLbl(p, gbc, row, "Android SDK Root:");
        JPanel sdkPanel = new JPanel(new BorderLayout(4, 0));
        sdkPanel.setBackground(Theme.BG_PANEL);
        androidSdkField = styledTf();
        JButton sdkBrowse = btn("Browse...");
        sdkBrowse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(androidSdkField.getText());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                androidSdkField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        sdkPanel.add(androidSdkField, BorderLayout.CENTER);
        sdkPanel.add(sdkBrowse, BorderLayout.EAST);
        gbc.gridx = 1; gbc.gridy = row++;
        p.add(sdkPanel, gbc);

        // Gradle user home
        addLbl(p, gbc, row, "Gradle User Home:");
        JPanel guhPanel = new JPanel(new BorderLayout(4, 0));
        guhPanel.setBackground(Theme.BG_PANEL);
        gradleUserHomeField = styledTf();
        gradleUserHomeField.setText(System.getProperty("user.home") + File.separator + ".gradle");
        JButton guhBrowse = btn("Browse...");
        guhBrowse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(gradleUserHomeField.getText());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                gradleUserHomeField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        guhPanel.add(gradleUserHomeField, BorderLayout.CENTER);
        guhPanel.add(guhBrowse, BorderLayout.EAST);
        gbc.gridx = 1; gbc.gridy = row++;
        p.add(guhPanel, gbc);

        // Default Gradle
        addLbl(p, gbc, row, "Default Gradle:");
        defaultGradleCombo = new JComboBox<>();
        defaultGradleCombo.addItem(new ToolDetector.ToolInfo("", "auto-detect"));
        for (ToolDetector.ToolInfo g : ToolDetector.findGradleInstallations()) defaultGradleCombo.addItem(g);
        styleCombo(defaultGradleCombo);
        gbc.gridx = 1; gbc.gridy = row++;
        p.add(defaultGradleCombo, gbc);

        // Default JDK
        addLbl(p, gbc, row, "Default JDK:");
        defaultJdkCombo = new JComboBox<>();
        defaultJdkCombo.addItem(new ToolDetector.ToolInfo(System.getProperty("java.home",""), "current JVM"));
        for (ToolDetector.ToolInfo j : ToolDetector.findJdkInstallations()) defaultJdkCombo.addItem(j);
        styleCombo(defaultJdkCombo);
        gbc.gridx = 1; gbc.gridy = row++;
        p.add(defaultJdkCombo, gbc);

        return p;
    }

    private void loadValues() {
        IdePreferences prefs = IdePreferences.get();
        fontSizeSpinner.setValue(prefs.getInt(IdePreferences.KEY_FONT_SIZE, 13));
        tabSizeSpinner.setValue(prefs.getInt(IdePreferences.KEY_TAB_SIZE, 4));
        showLineNums.setSelected(prefs.getBoolean(IdePreferences.KEY_SHOW_LINE_NUMS, true));
        wordWrap.setSelected(prefs.getBoolean(IdePreferences.KEY_WORD_WRAP, false));
        androidSdkField.setText(prefs.getString(IdePreferences.KEY_DEFAULT_ANDROID_SDK, ""));
        gradleUserHomeField.setText(prefs.getString(IdePreferences.KEY_GRADLE_USER_HOME,
                System.getProperty("user.home") + File.separator + ".gradle"));

        String dg = prefs.getString(IdePreferences.KEY_DEFAULT_GRADLE, "");
        for (int i = 0; i < defaultGradleCombo.getItemCount(); i++) {
            if (defaultGradleCombo.getItemAt(i).path.equals(dg)) {
                defaultGradleCombo.setSelectedIndex(i); break;
            }
        }

        String dj = prefs.getString(IdePreferences.KEY_DEFAULT_JDK, "");
        for (int i = 0; i < defaultJdkCombo.getItemCount(); i++) {
            if (defaultJdkCombo.getItemAt(i).path.equals(dj)) {
                defaultJdkCombo.setSelectedIndex(i); break;
            }
        }
    }

    private void onSave() {
        IdePreferences prefs = IdePreferences.get();
        prefs.set(IdePreferences.KEY_FONT_SIZE, (int) fontSizeSpinner.getValue());
        prefs.set(IdePreferences.KEY_TAB_SIZE, (int) tabSizeSpinner.getValue());
        prefs.set(IdePreferences.KEY_SHOW_LINE_NUMS, showLineNums.isSelected());
        prefs.set(IdePreferences.KEY_WORD_WRAP, wordWrap.isSelected());
        prefs.set(IdePreferences.KEY_DEFAULT_ANDROID_SDK, androidSdkField.getText().trim());
        prefs.set(IdePreferences.KEY_GRADLE_USER_HOME, gradleUserHomeField.getText().trim());

        ToolDetector.ToolInfo g = (ToolDetector.ToolInfo) defaultGradleCombo.getSelectedItem();
        if (g != null) prefs.set(IdePreferences.KEY_DEFAULT_GRADLE, g.path);
        ToolDetector.ToolInfo j = (ToolDetector.ToolInfo) defaultJdkCombo.getSelectedItem();
        if (j != null) prefs.set(IdePreferences.KEY_DEFAULT_JDK, j.path);

        prefs.save();
        JOptionPane.showMessageDialog(this, "Preferences saved.", "Saved", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    // helpers
    private GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        return g;
    }

    private void addLbl(JPanel p, GridBagConstraints gbc, int row, String text) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel lbl = new JLabel(text);
        lbl.setForeground(Theme.FG_DIM);
        lbl.setFont(Theme.FONT_UI);
        lbl.setPreferredSize(new Dimension(150, 24));
        p.add(lbl, gbc);
        gbc.weightx = 1;
    }

    private JTextField styledTf() {
        JTextField tf = new JTextField();
        tf.setBackground(Theme.BG_INPUT);
        tf.setForeground(Theme.FG_BRIGHT);
        tf.setCaretColor(Theme.FG_BRIGHT);
        tf.setFont(Theme.FONT_UI);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        return tf;
    }

    private void styleSpinner(JSpinner s) {
        s.setBackground(Theme.BG_INPUT);
        s.setForeground(Theme.FG_BRIGHT);
        s.setFont(Theme.FONT_UI);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setBackground(Theme.BG_INPUT);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setForeground(Theme.FG_BRIGHT);
    }

    private void styleCombo(JComboBox<?> c) {
        c.setBackground(Theme.BG_INPUT);
        c.setForeground(Theme.FG_BRIGHT);
        c.setFont(Theme.FONT_UI);
    }

    private JButton btn(String text) {
        JButton b = new JButton(text);
        b.setBackground(Theme.BG_BUTTON);
        b.setForeground(Theme.FG_BRIGHT);
        b.setFont(Theme.FONT_UI);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        return b;
    }
}
