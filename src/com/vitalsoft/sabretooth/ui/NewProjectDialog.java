package com.vitalsoft.sabretooth.ui;

import com.vitalsoft.sabretooth.config.IdePreferences;
import com.vitalsoft.sabretooth.util.ToolDetector;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class NewProjectDialog extends JDialog {
    private boolean confirmed = false;

    // Result fields
    public String projectName;
    public String packageName;
    public int minSdk;
    public int targetSdk;
    public int compileSdk;
    public String projectType;
    public String gradlePath;
    public String jdkPath;
    public String androidJar;
    public File parentDir;

    // UI components
    private JTextField nameField;
    private JTextField packageField;
    private JComboBox<String> minSdkCombo;
    private JComboBox<String> targetSdkCombo;
    private JComboBox<String> compileSdkCombo;
    private JComboBox<String> projectTypeCombo;
    private JComboBox<ToolDetector.ToolInfo> gradleCombo;
    private JComboBox<ToolDetector.ToolInfo> jdkCombo;
    private JComboBox<String> androidJarCombo;
    private JTextField locationField;

    public NewProjectDialog(Frame parent) {
        super(parent, "New Android Project", true);
        setPreferredSize(new Dimension(650, 700));
        setMinimumSize(new Dimension(600, 600));
        buildUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(Theme.BG_PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setContentPane(root);

        // Title
        JLabel title = new JLabel("Create New Android Project");
        title.setFont(Theme.FONT_UI.deriveFont(Font.BOLD, 16f));
        title.setForeground(Theme.FG_BRIGHT);
        root.add(title, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.BG_PANEL);
        root.add(form, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // === Project Section ===
        addSectionHeader(form, gbc, row++, "Project");

        nameField = addTextField(form, gbc, row++, "Application Name:", "MyApp");
        packageField = addTextField(form, gbc, row++, "Package Name:", "com.example.myapp");

        // Location
        addLabel(form, gbc, row, "Save Location:");
        JPanel locPanel = new JPanel(new BorderLayout(4, 0));
        locPanel.setBackground(Theme.BG_PANEL);
        locationField = styledTextField();
        locationField.setText(System.getProperty("user.home") + File.separator + "AndroidProjects");
        locPanel.add(locationField, BorderLayout.CENTER);
        JButton browseBtn = styledButton("Browse...");
        browseBtn.addActionListener(e -> browseLocation());
        locPanel.add(browseBtn, BorderLayout.EAST);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        form.add(locPanel, gbc);

        projectTypeCombo = new JComboBox<>(new String[]{"gradle", "eclipse"});
        projectTypeCombo.setBackground(Theme.BG_INPUT);
        projectTypeCombo.setForeground(Theme.FG_BRIGHT);
        projectTypeCombo.setFont(Theme.FONT_UI);
        addComboRow(form, gbc, row++, "Project Type:", projectTypeCombo);

        // === Android SDK Section ===
        addSectionHeader(form, gbc, row++, "Android Configuration");

        String[] sdkLevels = {"16","17","18","19","21","22","23","24","25","26","27","28","29","30","31","32","33","34","35"};
        minSdkCombo = new JComboBox<>(sdkLevels);
        minSdkCombo.setSelectedItem("21");
        styleCombo(minSdkCombo);
        addComboRow(form, gbc, row++, "Min SDK:", minSdkCombo);

        targetSdkCombo = new JComboBox<>(sdkLevels);
        targetSdkCombo.setSelectedItem("34");
        styleCombo(targetSdkCombo);
        addComboRow(form, gbc, row++, "Target SDK:", targetSdkCombo);

        compileSdkCombo = new JComboBox<>(sdkLevels);
        compileSdkCombo.setSelectedItem("34");
        styleCombo(compileSdkCombo);
        addComboRow(form, gbc, row++, "Compile SDK:", compileSdkCombo);

        // === Build Tools Section ===
        addSectionHeader(form, gbc, row++, "Build Tools");

        // Gradle
        List<ToolDetector.ToolInfo> gradles = ToolDetector.findGradleInstallations();
        gradleCombo = new JComboBox<>();
        gradleCombo.addItem(new ToolDetector.ToolInfo("", "system default"));
        for (ToolDetector.ToolInfo g : gradles) gradleCombo.addItem(g);
        styleCombo(gradleCombo);
        // Pre-select from preferences
        String prefGradle = IdePreferences.get().getString(IdePreferences.KEY_DEFAULT_GRADLE, "");
        if (!prefGradle.isEmpty()) {
            for (int i = 0; i < gradleCombo.getItemCount(); i++) {
                if (gradleCombo.getItemAt(i).path.equals(prefGradle)) {
                    gradleCombo.setSelectedIndex(i); break;
                }
            }
        }
        addComboRow(form, gbc, row++, "Gradle:", gradleCombo);

        // JDK
        List<ToolDetector.ToolInfo> jdks = ToolDetector.findJdkInstallations();
        jdkCombo = new JComboBox<>();
        jdkCombo.addItem(new ToolDetector.ToolInfo(System.getProperty("java.home", ""), "current JVM"));
        for (ToolDetector.ToolInfo j : jdks) jdkCombo.addItem(j);
        styleCombo(jdkCombo);
        addComboRow(form, gbc, row++, "JDK:", jdkCombo);

        // Android JAR
        String sdkRoot = IdePreferences.get().getString(IdePreferences.KEY_DEFAULT_ANDROID_SDK, "");
        List<String> jars = ToolDetector.findAndroidJars(sdkRoot);
        androidJarCombo = new JComboBox<>();
        androidJarCombo.addItem("");
        for (String j : jars) androidJarCombo.addItem(j);
        styleCombo(androidJarCombo);
        if (!jars.isEmpty()) androidJarCombo.setSelectedIndex(1);
        addComboRow(form, gbc, row++, "Android JAR:", androidJarCombo);

        // === Buttons ===
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBackground(Theme.BG_PANEL);
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));

        JButton cancel = styledButton("Cancel");
        cancel.addActionListener(e -> dispose());

        JButton create = styledButton("Create Project");
        create.setBackground(Theme.FG_ACCENT);
        create.setForeground(Color.WHITE);
        create.addActionListener(e -> onConfirm());

        buttons.add(cancel);
        buttons.add(create);
        root.add(buttons, BorderLayout.SOUTH);
    }

    private void addSectionHeader(JPanel form, GridBagConstraints gbc, int row, String text) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 1;
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_UI.deriveFont(Font.BOLD));
        lbl.setForeground(Theme.FG_ACCENT);
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        form.add(lbl, gbc);
        gbc.gridwidth = 1;
    }

    private JTextField addTextField(JPanel form, GridBagConstraints gbc, int row, String label, String def) {
        addLabel(form, gbc, row, label);
        JTextField tf = styledTextField();
        tf.setText(def);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1;
        form.add(tf, gbc);
        return tf;
    }

    private void addLabel(JPanel form, GridBagConstraints gbc, int row, String text) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel lbl = new JLabel(text);
        lbl.setForeground(Theme.FG_DIM);
        lbl.setFont(Theme.FONT_UI);
        lbl.setPreferredSize(new Dimension(140, 24));
        form.add(lbl, gbc);
    }

    private void addComboRow(JPanel form, GridBagConstraints gbc, int row, String label, JComboBox<?> combo) {
        addLabel(form, gbc, row, label);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1;
        form.add(combo, gbc);
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
        ((JComponent) combo.getRenderer()).setBackground(Theme.BG_INPUT);
    }

    private JButton styledButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(Theme.BG_BUTTON);
        btn.setForeground(Theme.FG_BRIGHT);
        btn.setFont(Theme.FONT_UI);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        return btn;
    }

    private void browseLocation() {
        JFileChooser fc = new JFileChooser(locationField.getText());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            locationField.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void onConfirm() {
        String name = nameField.getText().trim();
        String pkg = packageField.getText().trim();
        String loc = locationField.getText().trim();

        if (name.isEmpty()) { error("Application name is required."); return; }
        if (pkg.isEmpty() || !pkg.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+")) {
            error("Invalid package name. Must be like 'com.example.myapp'."); return;
        }
        if (loc.isEmpty()) { error("Save location is required."); return; }

        projectName = name;
        packageName = pkg;
        parentDir = new File(loc);
        minSdk = Integer.parseInt((String) minSdkCombo.getSelectedItem());
        targetSdk = Integer.parseInt((String) targetSdkCombo.getSelectedItem());
        compileSdk = Integer.parseInt((String) compileSdkCombo.getSelectedItem());
        projectType = (String) projectTypeCombo.getSelectedItem();

        ToolDetector.ToolInfo g = (ToolDetector.ToolInfo) gradleCombo.getSelectedItem();
        gradlePath = (g != null && !g.path.isEmpty()) ? g.path : "";

        ToolDetector.ToolInfo j = (ToolDetector.ToolInfo) jdkCombo.getSelectedItem();
        jdkPath = (j != null && !j.path.isEmpty()) ? j.path : System.getProperty("java.home", "");

        Object jar = androidJarCombo.getSelectedItem();
        androidJar = (jar != null) ? jar.toString() : "";

        confirmed = true;
        dispose();
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    public boolean isConfirmed() { return confirmed; }
}
