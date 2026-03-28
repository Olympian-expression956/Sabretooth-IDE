package com.vitalsoft.sabretooth.ui;

import com.vitalsoft.sabretooth.config.SabreConf;
import com.vitalsoft.sabretooth.project.Project;
import com.vitalsoft.sabretooth.util.ToolDetector;
import com.vitalsoft.sabretooth.config.IdePreferences;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class ProjectSettingsDialog extends JDialog {
    private final Project project;
    private boolean saved = false;

    private JTextField nameField, packageField, versionCodeField, versionNameField;
    private JTextField mainActivityField, extraGradleField;
    private JComboBox<String> minSdkCombo, targetSdkCombo, compileSdkCombo, buildTypeCombo;
    private JComboBox<ToolDetector.ToolInfo> gradleCombo, jdkCombo;
    private JComboBox<String> androidJarCombo;
    private JTextField keystoreField, keystoreAliasField;
    private JTextField outputDirField;

    public ProjectSettingsDialog(Frame parent, Project project) {
        super(parent, "Project Settings: " + project.getName(), true);
        this.project = project;
        setPreferredSize(new Dimension(700, 750));
        setMinimumSize(new Dimension(600, 600));
        buildUI();
        loadValues();
        pack();
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(Theme.BG_PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 10, 16));
        setContentPane(root);

        JLabel title = new JLabel("Project Settings");
        title.setFont(Theme.FONT_UI.deriveFont(Font.BOLD, 16f));
        title.setForeground(Theme.FG_BRIGHT);
        root.add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Theme.BG_PANEL);
        tabs.setForeground(Theme.FG_BRIGHT);
        tabs.setFont(Theme.FONT_UI);
        root.add(tabs, BorderLayout.CENTER);

        tabs.addTab("Project", buildProjectTab());
        tabs.addTab("Android", buildAndroidTab());
        tabs.addTab("Build Tools", buildToolsTab());
        tabs.addTab("Signing", buildSigningTab());

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.setBackground(Theme.BG_PANEL);

        JButton cancel = styledBtn("Cancel");
        cancel.addActionListener(e -> dispose());

        JButton save = styledBtn("Save");
        save.setBackground(Theme.FG_ACCENT);
        save.setForeground(Color.WHITE);
        save.addActionListener(e -> onSave());

        btns.add(cancel);
        btns.add(save);
        root.add(btns, BorderLayout.SOUTH);
    }

    private JPanel buildProjectTab() {
        JPanel p = formPanel();
        int row = 0;
        nameField = addRow(p, row++, "Project Name:", "");
        packageField = addRow(p, row++, "Package Name:", "");
        versionCodeField = addRow(p, row++, "Version Code:", "");
        versionNameField = addRow(p, row++, "Version Name:", "");
        mainActivityField = addRow(p, row++, "Main Activity:", "");

        addSectionLbl(p, row++, "Project Type");
        return p;
    }

    private JPanel buildAndroidTab() {
        JPanel p = formPanel();
        int row = 0;
        String[] sdkLevels = {"16","17","18","19","21","22","23","24","25","26","27","28","29","30","31","32","33","34","35"};
        minSdkCombo = new JComboBox<>(sdkLevels);
        targetSdkCombo = new JComboBox<>(sdkLevels);
        compileSdkCombo = new JComboBox<>(sdkLevels);
        styleCombo(minSdkCombo); styleCombo(targetSdkCombo); styleCombo(compileSdkCombo);
        addComboRow(p, row++, "Min SDK:", minSdkCombo);
        addComboRow(p, row++, "Target SDK:", targetSdkCombo);
        addComboRow(p, row++, "Compile SDK:", compileSdkCombo);
        return p;
    }

    private JPanel buildToolsTab() {
        JPanel p = formPanel();
        int row = 0;

        buildTypeCombo = new JComboBox<>(new String[]{"debug", "release"});
        styleCombo(buildTypeCombo);
        addComboRow(p, row++, "Build Type:", buildTypeCombo);

        // Gradle
        List<ToolDetector.ToolInfo> gradles = ToolDetector.findGradleInstallations();
        gradleCombo = new JComboBox<>();
        gradleCombo.addItem(new ToolDetector.ToolInfo("", "system/gradlew"));
        for (ToolDetector.ToolInfo g : gradles) gradleCombo.addItem(g);
        styleCombo(gradleCombo);
        addComboRow(p, row++, "Gradle:", gradleCombo);

        // JDK
        List<ToolDetector.ToolInfo> jdks = ToolDetector.findJdkInstallations();
        jdkCombo = new JComboBox<>();
        jdkCombo.addItem(new ToolDetector.ToolInfo(System.getProperty("java.home", ""), "current JVM"));
        for (ToolDetector.ToolInfo j : jdks) jdkCombo.addItem(j);
        styleCombo(jdkCombo);
        addComboRow(p, row++, "JDK:", jdkCombo);

        // Android JAR
        String sdkRoot = IdePreferences.get().getString(IdePreferences.KEY_DEFAULT_ANDROID_SDK,
                ToolDetector.detectAndroidSdkRoot());
        List<String> jars = ToolDetector.findAndroidJars(sdkRoot);
        androidJarCombo = new JComboBox<>();
        androidJarCombo.addItem("");
        for (String j : jars) androidJarCombo.addItem(j);
        // Allow custom entry
        androidJarCombo.setEditable(true);
        styleCombo(androidJarCombo);
        addComboRow(p, row++, "Android JAR:", androidJarCombo);

        // Output dir
        outputDirField = addRow(p, row++, "Output Dir:", "");
        extraGradleField = addRow(p, row++, "Extra Gradle Args:", "");

        return p;
    }

    private JPanel buildSigningTab() {
        JPanel p = formPanel();
        int row = 0;

        addSectionLbl(p, row++, "Keystore Configuration (for release builds)");

        // Keystore path with browse
        addLabel(p, row, "Keystore Path:");
        JPanel ksPanel = new JPanel(new BorderLayout(4, 0));
        ksPanel.setBackground(Theme.BG_PANEL);
        keystoreField = styledTextField();
        JButton browseKs = styledBtn("Browse...");
        browseKs.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Keystore files", "jks", "keystore", "bks"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                keystoreField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        ksPanel.add(keystoreField, BorderLayout.CENTER);
        ksPanel.add(browseKs, BorderLayout.EAST);
        GridBagConstraints gbc = makeGbc();
        gbc.gridx = 1; gbc.gridy = row++;
        p.add(ksPanel, gbc);

        keystoreAliasField = addRow(p, row++, "Key Alias:", "");
        return p;
    }

    private void loadValues() {
        SabreConf conf = project.getConf();
        nameField.setText(conf.get(SabreConf.KEY_PROJECT_NAME));
        packageField.setText(conf.get(SabreConf.KEY_PACKAGE_NAME));
        versionCodeField.setText(conf.get(SabreConf.KEY_VERSION_CODE));
        versionNameField.setText(conf.get(SabreConf.KEY_VERSION_NAME));
        mainActivityField.setText(conf.get(SabreConf.KEY_MAIN_ACTIVITY));
        outputDirField.setText(conf.get(SabreConf.KEY_OUTPUT_DIR));
        extraGradleField.setText(conf.get(SabreConf.KEY_EXTRA_GRADLE_ARGS));
        keystoreField.setText(conf.get(SabreConf.KEY_KEYSTORE_PATH));
        keystoreAliasField.setText(conf.get(SabreConf.KEY_KEYSTORE_ALIAS));

        setComboValue(minSdkCombo, conf.get(SabreConf.KEY_MIN_SDK));
        setComboValue(targetSdkCombo, conf.get(SabreConf.KEY_TARGET_SDK));
        setComboValue(compileSdkCombo, conf.get(SabreConf.KEY_COMPILE_SDK));
        setComboValue(buildTypeCombo, conf.get(SabreConf.KEY_BUILD_TYPE));

        String confGradle = conf.get(SabreConf.KEY_GRADLE_PATH);
        for (int i = 0; i < gradleCombo.getItemCount(); i++) {
            if (gradleCombo.getItemAt(i).path.equals(confGradle)) {
                gradleCombo.setSelectedIndex(i); break;
            }
        }

        String confJdk = conf.get(SabreConf.KEY_JDK_PATH);
        for (int i = 0; i < jdkCombo.getItemCount(); i++) {
            if (jdkCombo.getItemAt(i).path.equals(confJdk)) {
                jdkCombo.setSelectedIndex(i); break;
            }
        }

        String confJar = conf.get(SabreConf.KEY_ANDROID_JAR);
        if (!confJar.isEmpty()) androidJarCombo.setSelectedItem(confJar);
    }

    private void onSave() {
        SabreConf conf = project.getConf();
        conf.set(SabreConf.KEY_PROJECT_NAME, nameField.getText().trim());
        conf.set(SabreConf.KEY_PACKAGE_NAME, packageField.getText().trim());
        conf.set(SabreConf.KEY_VERSION_CODE, versionCodeField.getText().trim());
        conf.set(SabreConf.KEY_VERSION_NAME, versionNameField.getText().trim());
        conf.set(SabreConf.KEY_MAIN_ACTIVITY, mainActivityField.getText().trim());
        conf.set(SabreConf.KEY_OUTPUT_DIR, outputDirField.getText().trim());
        conf.set(SabreConf.KEY_EXTRA_GRADLE_ARGS, extraGradleField.getText().trim());
        conf.set(SabreConf.KEY_KEYSTORE_PATH, keystoreField.getText().trim());
        conf.set(SabreConf.KEY_KEYSTORE_ALIAS, keystoreAliasField.getText().trim());
        conf.set(SabreConf.KEY_MIN_SDK, (String) minSdkCombo.getSelectedItem());
        conf.set(SabreConf.KEY_TARGET_SDK, (String) targetSdkCombo.getSelectedItem());
        conf.set(SabreConf.KEY_COMPILE_SDK, (String) compileSdkCombo.getSelectedItem());
        conf.set(SabreConf.KEY_BUILD_TYPE, (String) buildTypeCombo.getSelectedItem());

        ToolDetector.ToolInfo g = (ToolDetector.ToolInfo) gradleCombo.getSelectedItem();
        if (g != null) conf.set(SabreConf.KEY_GRADLE_PATH, g.path);
        ToolDetector.ToolInfo j = (ToolDetector.ToolInfo) jdkCombo.getSelectedItem();
        if (j != null) conf.set(SabreConf.KEY_JDK_PATH, j.path);
        Object jar = androidJarCombo.getSelectedItem();
        if (jar != null) conf.set(SabreConf.KEY_ANDROID_JAR, jar.toString());

        try {
            project.saveConf();
            saved = true;
            JOptionPane.showMessageDialog(this, "Settings saved to .sabreconf", "Saved", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Helpers ────────────────────────────────────────────
    private JPanel formPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return p;
    }

    private GridBagConstraints makeGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        return gbc;
    }

    private JTextField addRow(JPanel p, int row, String label, String def) {
        addLabel(p, row, label);
        JTextField tf = styledTextField();
        tf.setText(def);
        GridBagConstraints gbc = makeGbc();
        gbc.gridx = 1; gbc.gridy = row;
        p.add(tf, gbc);
        return tf;
    }

    private void addComboRow(JPanel p, int row, String label, JComboBox<?> combo) {
        addLabel(p, row, label);
        GridBagConstraints gbc = makeGbc();
        gbc.gridx = 1; gbc.gridy = row;
        p.add(combo, gbc);
    }

    private void addLabel(JPanel p, int row, String text) {
        GridBagConstraints gbc = makeGbc();
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel lbl = new JLabel(text);
        lbl.setForeground(Theme.FG_DIM);
        lbl.setFont(Theme.FONT_UI);
        lbl.setPreferredSize(new Dimension(140, 24));
        p.add(lbl, gbc);
    }

    private void addSectionLbl(JPanel p, int row, String text) {
        GridBagConstraints gbc = makeGbc();
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_UI.deriveFont(Font.BOLD));
        lbl.setForeground(Theme.FG_ACCENT);
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        p.add(lbl, gbc);
        gbc.gridwidth = 1;
    }

    private JTextField styledTextField() {
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

    private void styleCombo(JComboBox<?> combo) {
        combo.setBackground(Theme.BG_INPUT);
        combo.setForeground(Theme.FG_BRIGHT);
        combo.setFont(Theme.FONT_UI);
    }

    private JButton styledBtn(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(Theme.BG_BUTTON);
        btn.setForeground(Theme.FG_BRIGHT);
        btn.setFont(Theme.FONT_UI);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        return btn;
    }

    private void setComboValue(JComboBox<String> combo, String value) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).equals(value)) {
                combo.setSelectedIndex(i); return;
            }
        }
    }

}

