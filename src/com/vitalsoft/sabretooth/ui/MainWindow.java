package com.vitalsoft.sabretooth.ui;

import com.vitalsoft.sabretooth.build.BuildManager;
import com.vitalsoft.sabretooth.config.IdePreferences;
import com.vitalsoft.sabretooth.config.SabreConf;
import com.vitalsoft.sabretooth.project.Project;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;

public class MainWindow extends JFrame {
    private Project currentProject;
    private final BuildManager buildManager = new BuildManager();
    private final EditorTabPanel editorTabs;
    private final ProjectTreePanel projectTree;
    private final ConsolePanel console;
    private final StatusBar statusBar;
    private final MainToolbar toolbar;
    private FindReplaceDialog findReplaceDialog;
    private JSplitPane mainSplit;
    private JSplitPane rightSplit;

    public MainWindow() {
        super("SabreTooth IDE");
        Theme.applyGlobalDefaults();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        IdePreferences prefs = IdePreferences.get();
        setSize(prefs.getInt(IdePreferences.KEY_WINDOW_WIDTH, 1280),
                prefs.getInt(IdePreferences.KEY_WINDOW_HEIGHT, 800));

        // Try to center if no saved pos
        if (prefs.getInt(IdePreferences.KEY_WINDOW_X, -1) < 0) {
            setLocationRelativeTo(null);
        } else {
            setLocation(prefs.getInt(IdePreferences.KEY_WINDOW_X, 100),
                        prefs.getInt(IdePreferences.KEY_WINDOW_Y, 100));
        }

        setIconImage(createAppIcon());

        // Init components
        editorTabs  = new EditorTabPanel();
        projectTree = new ProjectTreePanel();
        console     = new ConsolePanel();
        statusBar   = new StatusBar();
        toolbar     = new MainToolbar();

        buildLayout();
        buildMenuBar();
        wireEvents();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { onExit(); }
        });
    }

    private void buildLayout() {
        getContentPane().setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());

        add(toolbar, BorderLayout.NORTH);
        add(statusBar, BorderLayout.SOUTH);

        // Right split: editor (top) + console (bottom)
        rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorTabs, console);
        rightSplit.setResizeWeight(0.75);
        rightSplit.setDividerSize(5);
        rightSplit.setBackground(Theme.BG_DARK);
        rightSplit.setBorder(BorderFactory.createEmptyBorder());
        IdePreferences prefs = IdePreferences.get();
        rightSplit.setDividerLocation(prefs.getInt(IdePreferences.KEY_SPLIT_POS_V, 550));

        // Main split: project tree (left) + right pane
        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectTree, rightSplit);
        mainSplit.setResizeWeight(0);
        mainSplit.setDividerSize(5);
        mainSplit.setBackground(Theme.BG_DARK);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());
        mainSplit.setDividerLocation(prefs.getInt(IdePreferences.KEY_SPLIT_POS_H, 250));

        add(mainSplit, BorderLayout.CENTER);

        console.setPreferredSize(new Dimension(0, 180));
    }

    private void buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(Theme.BG_MENU);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        setJMenuBar(menuBar);

        menuBar.add(buildFileMenu());
        menuBar.add(buildEditMenu());
        menuBar.add(buildViewMenu());
        menuBar.add(buildProjectMenu());
        menuBar.add(buildBuildMenu());
        menuBar.add(buildToolsMenu());
        menuBar.add(buildHelpMenu());
    }

    private JMenu buildFileMenu() {
        JMenu menu = menu("File");

        JMenuItem newProject = item("New Project...", KeyStroke.getKeyStroke(KeyEvent.VK_N,
            InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        newProject.addActionListener(e -> showNewProjectDialog());
        menu.add(newProject);

        JMenuItem newFile = item("New File...", KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newFile.addActionListener(e -> newFileInProject());
        menu.add(newFile);

        menu.addSeparator();

        JMenuItem openProject = item("Open Project...", KeyStroke.getKeyStroke(KeyEvent.VK_O,
            InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        openProject.addActionListener(e -> openProject());
        menu.add(openProject);

        JMenuItem openFile = item("Open File...", KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openFile.addActionListener(e -> openFile());
        menu.add(openFile);

        // Recent projects submenu
        JMenu recent = menu("Open Recent");
        recent.addMenuListener(new javax.swing.event.MenuListener() {
            @Override public void menuSelected(javax.swing.event.MenuEvent e) { buildRecentMenu(recent); }
            @Override public void menuDeselected(javax.swing.event.MenuEvent e) {}
            @Override public void menuCanceled(javax.swing.event.MenuEvent e) {}
        });
        menu.add(recent);

        menu.addSeparator();

        JMenuItem save = item("Save", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        save.addActionListener(e -> editorTabs.saveCurrentFile());
        menu.add(save);

        JMenuItem saveAs = item("Save As...", null);
        saveAs.addActionListener(e -> editorTabs.saveCurrentFileAs());
        menu.add(saveAs);

        JMenuItem saveAll = item("Save All", KeyStroke.getKeyStroke(KeyEvent.VK_S,
            InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        saveAll.addActionListener(e -> editorTabs.saveAllFiles());
        menu.add(saveAll);

        menu.addSeparator();

        JMenuItem closeTab = item("Close Tab", KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
        closeTab.addActionListener(e -> editorTabs.closeCurrentTab());
        menu.add(closeTab);

        menu.addSeparator();

        JMenuItem exit = item("Exit", KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        exit.addActionListener(e -> onExit());
        menu.add(exit);

        return menu;
    }

    private JMenu buildEditMenu() {
        JMenu menu = menu("Edit");

        JMenuItem undo = item("Undo", KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        undo.addActionListener(e -> {
            CodeEditorPane ed = editorTabs.getCurrentEditor();
            if (ed != null) {
                Action a = ed.getTextPane().getActionMap().get("undo");
                if (a != null) a.actionPerformed(new ActionEvent(ed.getTextPane(), 0, "undo"));
            }
        });
        menu.add(undo);

        JMenuItem redo = item("Redo", KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        redo.addActionListener(e -> {
            CodeEditorPane ed = editorTabs.getCurrentEditor();
            if (ed != null) {
                Action a = ed.getTextPane().getActionMap().get("redo");
                if (a != null) a.actionPerformed(new ActionEvent(ed.getTextPane(), 0, "redo"));
            }
        });
        menu.add(redo);

        menu.addSeparator();

        JMenuItem cut = item("Cut", KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        cut.addActionListener(e -> { CodeEditorPane ed = editorTabs.getCurrentEditor(); if (ed != null) ed.getTextPane().cut(); });
        menu.add(cut);

        JMenuItem copy = item("Copy", KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        copy.addActionListener(e -> { CodeEditorPane ed = editorTabs.getCurrentEditor(); if (ed != null) ed.getTextPane().copy(); });
        menu.add(copy);

        JMenuItem paste = item("Paste", KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        paste.addActionListener(e -> { CodeEditorPane ed = editorTabs.getCurrentEditor(); if (ed != null) ed.getTextPane().paste(); });
        menu.add(paste);

        JMenuItem selectAll = item("Select All", KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        selectAll.addActionListener(e -> { CodeEditorPane ed = editorTabs.getCurrentEditor(); if (ed != null) ed.getTextPane().selectAll(); });
        menu.add(selectAll);

        menu.addSeparator();

        JMenuItem find = item("Find / Replace...", KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        find.addActionListener(e -> showFindReplace());
        menu.add(find);

        return menu;
    }

    private JMenu buildViewMenu() {
        JMenu menu = menu("View");

        JMenuItem zoomIn = item("Increase Font Size", KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK));
        zoomIn.addActionListener(e -> changeFontSize(1));
        menu.add(zoomIn);

        JMenuItem zoomOut = item("Decrease Font Size", KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
        zoomOut.addActionListener(e -> changeFontSize(-1));
        menu.add(zoomOut);

        menu.addSeparator();

        JMenuItem toggleConsole = item("Toggle Console", null);
        toggleConsole.addActionListener(e -> {
            boolean visible = console.isVisible();
            console.setVisible(!visible);
        });
        menu.add(toggleConsole);

        JMenuItem toggleTree = item("Toggle Project Tree", null);
        toggleTree.addActionListener(e -> projectTree.setVisible(!projectTree.isVisible()));
        menu.add(toggleTree);

        return menu;
    }

    private JMenu buildProjectMenu() {
        JMenu menu = menu("Project");

        JMenuItem projectSettings = item("Project Settings...", null);
        projectSettings.addActionListener(e -> showProjectSettings());
        menu.add(projectSettings);

        JMenuItem refreshTree = item("Refresh File Tree", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        refreshTree.addActionListener(e -> {
            if (currentProject != null) projectTree.refresh(currentProject);
        });
        menu.add(refreshTree);

        menu.addSeparator();

        JMenuItem newJavaFile = item("New Java Class...", null);
        newJavaFile.addActionListener(e -> newJavaClass());
        menu.add(newJavaFile);

        JMenuItem newXmlFile = item("New XML Layout...", null);
        newXmlFile.addActionListener(e -> newXmlLayout());
        menu.add(newXmlFile);

        return menu;
    }

    private JMenu buildBuildMenu() {
        JMenu menu = menu("Build");

        JMenuItem buildDebug = item("Build Debug APK", KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0));
        buildDebug.addActionListener(e -> startBuild(BuildManager.BuildTask.ASSEMBLE_DEBUG));
        menu.add(buildDebug);

        JMenuItem buildRelease = item("Build Release APK", KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK));
        buildRelease.addActionListener(e -> startBuild(BuildManager.BuildTask.ASSEMBLE_RELEASE));
        menu.add(buildRelease);

        JMenuItem fullBuild = item("Full Build", null);
        fullBuild.addActionListener(e -> startBuild(BuildManager.BuildTask.BUILD));
        menu.add(fullBuild);

        menu.addSeparator();

        JMenuItem clean = item("Clean", KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
        clean.addActionListener(e -> startBuild(BuildManager.BuildTask.CLEAN));
        menu.add(clean);

        menu.addSeparator();

        JMenuItem cancel = item("Cancel Build", null);
        cancel.addActionListener(e -> cancelBuild());
        menu.add(cancel);

        return menu;
    }

    private JMenu buildToolsMenu() {
        JMenu menu = menu("Tools");

        JMenuItem prefs = item("Preferences...", null);
        prefs.addActionListener(e -> new IdePreferencesDialog(this).setVisible(true));
        menu.add(prefs);

        menu.addSeparator();

        JMenuItem terminal = item("Open Terminal Here", null);
        terminal.addActionListener(e -> openTerminal());
        menu.add(terminal);

        JMenuItem listGradle = item("Detect Installed Gradle...", null);
        listGradle.addActionListener(e -> showDetectedTools());
        menu.add(listGradle);

        return menu;
    }

    private JMenu buildHelpMenu() {
        JMenu menu = menu("Help");

        JMenuItem about = item("About SabreTooth IDE", null);
        about.addActionListener(e -> showAbout());
        menu.add(about);

        JMenuItem shortcuts = item("Keyboard Shortcuts", null);
        shortcuts.addActionListener(e -> showShortcuts());
        menu.add(shortcuts);

        return menu;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Wire toolbar buttons and caret listener
    // ──────────────────────────────────────────────────────────────────────────
    private void wireEvents() {
        toolbar.getBuildDebugBtn().addActionListener(e -> startBuild(BuildManager.BuildTask.ASSEMBLE_DEBUG));
        toolbar.getBuildReleaseBtn().addActionListener(e -> startBuild(BuildManager.BuildTask.ASSEMBLE_RELEASE));
        toolbar.getCleanBtn().addActionListener(e -> startBuild(BuildManager.BuildTask.CLEAN));
        toolbar.getStopBtn().addActionListener(e -> cancelBuild());
        toolbar.getSaveBtn().addActionListener(e -> editorTabs.saveCurrentFile());
        toolbar.getSaveAllBtn().addActionListener(e -> editorTabs.saveAllFiles());

        projectTree.setFileOpenCallback(f -> editorTabs.openFile(f));

        editorTabs.setOnTabChange(() -> {
            CodeEditorPane ed = editorTabs.getCurrentEditor();
            if (ed != null) {
                attachCaretListener(ed);
                updateTitle(ed.getFile() != null ? ed.getFile().getName() : "Untitled");
            }
        });
    }

    private void attachCaretListener(CodeEditorPane editor) {
        editor.getTextPane().addCaretListener(e -> {
            try {
                int pos = editor.getTextPane().getCaretPosition();
                Element root = editor.getTextPane().getDocument().getDefaultRootElement();
                int line = root.getElementIndex(pos) + 1;
                int col = pos - root.getElement(line - 1).getStartOffset() + 1;
                File f = editor.getFile();
                String lang = f != null ? SyntaxHighlighter.detectLanguage(f.getName()).name() : "TEXT";
                statusBar.setCaretInfo(line, col, lang);
            } catch (Exception ignored) {}
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Actions
    // ──────────────────────────────────────────────────────────────────────────
    private void showNewProjectDialog() {
        NewProjectDialog dlg = new NewProjectDialog(this);
        dlg.setVisible(true);
        if (!dlg.isConfirmed()) return;
        try {
            Project proj = Project.create(
                dlg.parentDir, dlg.projectName, dlg.packageName,
                dlg.minSdk, dlg.targetSdk, dlg.compileSdk,
                dlg.gradlePath, dlg.jdkPath, dlg.androidJar, dlg.projectType);
            loadProject(proj);
            IdePreferences.get().addRecentProject(proj.getRootDir().getAbsolutePath());
            IdePreferences.get().save();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error creating project: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openProject() {
        JFileChooser fc = new JFileChooser(IdePreferences.get().getString(IdePreferences.KEY_LAST_OPEN_DIR, System.getProperty("user.home")));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Open Android Project");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File dir = fc.getSelectedFile();
        IdePreferences.get().set(IdePreferences.KEY_LAST_OPEN_DIR, dir.getParent());
        try {
            Project proj = new Project(dir);
            loadProject(proj);
            IdePreferences.get().addRecentProject(dir.getAbsolutePath());
            IdePreferences.get().save();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error opening project: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openFile() {
        JFileChooser fc = new JFileChooser(currentProject != null
            ? currentProject.getRootDir().getAbsolutePath()
            : System.getProperty("user.home"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            editorTabs.openFile(fc.getSelectedFile());
        }
    }

    private void buildRecentMenu(JMenu menu) {
        menu.removeAll();
        List<String> recents = IdePreferences.get().getRecentProjects();
        if (recents.isEmpty()) {
            JMenuItem none = item("(No Recent Projects)", null);
            none.setEnabled(false);
            menu.add(none);
        } else {
            for (String path : recents) {
                JMenuItem mi = item(path, null);
                mi.addActionListener(ev -> {
                    try {
                        Project p = new Project(new File(path));
                        loadProject(p);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(this, "Cannot open: " + e.getMessage());
                    }
                });
                menu.add(mi);
            }
            menu.addSeparator();
            JMenuItem clear = item("Clear Recent Projects", null);
            clear.addActionListener(ev -> {
                IdePreferences.get().set(IdePreferences.KEY_RECENT_PROJECTS, "");
                IdePreferences.get().save();
            });
            menu.add(clear);
        }
    }

    private void loadProject(Project project) {
        this.currentProject = project;
        projectTree.loadProject(project);
        updateTitle(project.getName());
        statusBar.setProjectName(project.getName());
        statusBar.setStatus("Project loaded: " + project.getName());
        console.appendLine("[SabreTooth] Project opened: " + project.getRootDir().getAbsolutePath());
        // Open main activity if found
        tryOpenMainFile(project);
    }

    private void tryOpenMainFile(Project project) {
        SabreConf conf = project.getConf();
        String pkg = conf.get(SabreConf.KEY_PACKAGE_NAME);
        String activity = conf.get(SabreConf.KEY_MAIN_ACTIVITY);
        if (pkg.isEmpty() || activity.isEmpty()) return;

        String pkgPath = pkg.replace('.', File.separatorChar);
        String[] candidates = {
            "app/src/main/java/" + pkgPath + "/" + activity + ".java",
            "src/" + pkgPath + "/" + activity + ".java",
            "app/src/main/java/" + pkgPath + "/" + activity + ".kt",
        };
        for (String c : candidates) {
            File f = new File(project.getRootDir(), c);
            if (f.exists()) { editorTabs.openFile(f); return; }
        }
    }

    private void startBuild(BuildManager.BuildTask task) {
        if (currentProject == null) {
            JOptionPane.showMessageDialog(this, "No project open. Please open or create a project.",
                "No Project", JOptionPane.WARNING_MESSAGE);
            return;
        }
        editorTabs.saveAllFiles();
        console.clear();
        String taskName = task.name().replace("_", " ");
        console.printBuildStart(taskName + " - " + currentProject.getName());
        statusBar.setStatus("Building: " + taskName + "...");
        statusBar.showProgress(true);
        toolbar.setBuildRunning(true);

        buildManager.buildAsync(currentProject, task,
            line -> console.appendLine(line),
            result -> SwingUtilities.invokeLater(() -> {
                toolbar.setBuildRunning(false);
                statusBar.showProgress(false);
                console.printBuildEnd(result.success, result.apkPath);
                if (result.success) {
                    statusBar.setStatusOk("Build successful");
                    if (result.apkPath != null) {
                        int choice = JOptionPane.showConfirmDialog(this,
                            "Build successful!\nAPK: " + result.apkPath + "\n\nOpen containing folder?",
                            "Build Successful", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                        if (choice == JOptionPane.YES_OPTION) openFileInExplorer(result.apkPath);
                    }
                } else {
                    statusBar.setStatusError("Build failed");
                }
            }));
    }

    private void cancelBuild() {
        buildManager.cancel();
        toolbar.setBuildRunning(false);
        statusBar.showProgress(false);
        statusBar.setStatus("Build cancelled");
        console.appendLine("[SabreTooth] Build cancelled by user.");
    }

    private void openFileInExplorer(String path) {
        try {
            File f = new File(path);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(f.getParentFile());
            } else {
                // Fallback for Linux
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("linux")) {
                    new ProcessBuilder("xdg-open", f.getParent()).start();
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "APK location: " + path);
        }
    }

    private void showProjectSettings() {
        if (currentProject == null) {
            JOptionPane.showMessageDialog(this, "No project open.", "No Project", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ProjectSettingsDialog dlg = new ProjectSettingsDialog(this, currentProject);
        dlg.setVisible(true);
    }

    private void newFileInProject() {
        if (currentProject == null) { openFile(); return; }
        String name = JOptionPane.showInputDialog(this, "File name:", "New File", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        // Create in project src dir
        File srcDir = new File(currentProject.getRootDir(), "app/src/main/java");
        if (!srcDir.exists()) srcDir = currentProject.getRootDir();
        File newFile = new File(srcDir, name.trim());
        try {
            newFile.getParentFile().mkdirs();
            newFile.createNewFile();
            projectTree.refresh(currentProject);
            editorTabs.openFile(newFile);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void newJavaClass() {
        if (currentProject == null) { statusBar.setStatus("No project open"); return; }
        String name = JOptionPane.showInputDialog(this, "Class name (e.g. MyActivity):", "New Java Class", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        String pkg = currentProject.getConf().get(SabreConf.KEY_PACKAGE_NAME);
        String pkgPath = pkg.replace('.', File.separatorChar);
        File javaDir = new File(currentProject.getRootDir(), "app/src/main/java/" + pkgPath);
        if (!javaDir.exists()) javaDir = new File(currentProject.getRootDir(), "src/" + pkgPath);
        javaDir.mkdirs();
        File f = new File(javaDir, name.trim() + ".java");
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println("package " + pkg + ";");
            pw.println();
            pw.println("public class " + name.trim() + " {");
            pw.println();
            pw.println("    public " + name.trim() + "() {");
            pw.println("        // TODO");
            pw.println("    }");
            pw.println("}");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            return;
        }
        projectTree.refresh(currentProject);
        editorTabs.openFile(f);
    }

    private void newXmlLayout() {
        if (currentProject == null) { statusBar.setStatus("No project open"); return; }
        String name = JOptionPane.showInputDialog(this, "Layout name (e.g. activity_detail):", "New XML Layout", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        File resDir = new File(currentProject.getRootDir(), "app/src/main/res/layout");
        if (!resDir.exists()) resDir = new File(currentProject.getRootDir(), "res/layout");
        resDir.mkdirs();
        File f = new File(resDir, name.trim() + ".xml");
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            pw.println("<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"");
            pw.println("    android:layout_width=\"match_parent\"");
            pw.println("    android:layout_height=\"match_parent\"");
            pw.println("    android:orientation=\"vertical\">");
            pw.println();
            pw.println("</LinearLayout>");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            return;
        }
        projectTree.refresh(currentProject);
        editorTabs.openFile(f);
    }

    private void showFindReplace() {
        if (findReplaceDialog == null) {
            findReplaceDialog = new FindReplaceDialog(this);
            findReplaceDialog.setLocationRelativeTo(this);
        }
        findReplaceDialog.setEditor(editorTabs.getCurrentEditor());
        findReplaceDialog.setVisible(true);
    }

    private void changeFontSize(int delta) {
        IdePreferences prefs = IdePreferences.get();
        int size = prefs.getInt(IdePreferences.KEY_FONT_SIZE, 13) + delta;
        size = Math.max(8, Math.min(30, size));
        prefs.set(IdePreferences.KEY_FONT_SIZE, size);
        for (CodeEditorPane ed : editorTabs.getOpenEditors()) {
            ed.updateFontSize(size);
        }
    }

    private void openTerminal() {
        String dir = currentProject != null ? currentProject.getRootDir().getAbsolutePath() : System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("windows")) {
                new ProcessBuilder("cmd", "/c", "start", "cmd", "/k", "cd /d \"" + dir + "\"").start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", "-a", "Terminal", dir).start();
            } else {
                String[] terminals = {"gnome-terminal", "xterm", "konsole", "xfce4-terminal"};
                for (String t : terminals) {
                    try { new ProcessBuilder(t, "--working-directory=" + dir).start(); return; }
                    catch (IOException ignored) {}
                }
                new ProcessBuilder("xterm").start();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Cannot open terminal: " + e.getMessage());
        }
    }

    private void showDetectedTools() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Detected Gradle Installations ===\n");
        for (com.vitalsoft.sabretooth.util.ToolDetector.ToolInfo g : com.vitalsoft.sabretooth.util.ToolDetector.findGradleInstallations()) {
            sb.append("  ").append(g.label).append("\n");
        }
        sb.append("\n=== Detected JDK Installations ===\n");
        for (com.vitalsoft.sabretooth.util.ToolDetector.ToolInfo j : com.vitalsoft.sabretooth.util.ToolDetector.findJdkInstallations()) {
            sb.append("  ").append(j.label).append("\n");
        }
        sb.append("\n=== Android SDK Root ===\n");
        String sdk = com.vitalsoft.sabretooth.util.ToolDetector.detectAndroidSdkRoot();
        sb.append("  ").append(sdk.isEmpty() ? "(not found)" : sdk).append("\n");

        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        ta.setFont(Theme.FONT_CODE);
        ta.setBackground(Theme.BG_CONSOLE);
        ta.setForeground(Theme.FG_DEFAULT);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(600, 350));
        JOptionPane.showMessageDialog(this, sp, "Detected Tools", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAbout() {
        String msg = "<html><center>" +
            "<b><font size=+2>⚔ SabreTooth IDE</font></b><br><br>" +
            "A lightweight Android IDE for APK generation<br>" +
            "Version 1.0.0<br><br>" +
            "Package: com.vitalsoft.sabretooth<br>" +
            "Built with Java 21 + Swing<br><br>" +
            "<i>No layout editors. No bloat. Just build.</i>" +
            "</center></html>";
        JOptionPane.showMessageDialog(this, msg, "About SabreTooth IDE", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showShortcuts() {
        String msg = "<html><table cellpadding=4>" +
            "<tr><td><b>Ctrl+N</b></td><td>New File</td><td><b>Ctrl+Shift+N</b></td><td>New Project</td></tr>" +
            "<tr><td><b>Ctrl+O</b></td><td>Open File</td><td><b>Ctrl+Shift+O</b></td><td>Open Project</td></tr>" +
            "<tr><td><b>Ctrl+S</b></td><td>Save</td><td><b>Ctrl+Shift+S</b></td><td>Save All</td></tr>" +
            "<tr><td><b>Ctrl+W</b></td><td>Close Tab</td><td><b>Ctrl+F</b></td><td>Find / Replace</td></tr>" +
            "<tr><td><b>F10</b></td><td>Build Debug</td><td><b>Shift+F10</b></td><td>Build Release</td></tr>" +
            "<tr><td><b>F9</b></td><td>Clean</td><td><b>F5</b></td><td>Refresh Tree</td></tr>" +
            "<tr><td><b>Ctrl++</b></td><td>Font Bigger</td><td><b>Ctrl+-</b></td><td>Font Smaller</td></tr>" +
            "</table></html>";
        JOptionPane.showMessageDialog(this, msg, "Keyboard Shortcuts", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onExit() {
        // Save window state
        IdePreferences prefs = IdePreferences.get();
        prefs.set(IdePreferences.KEY_WINDOW_WIDTH, getWidth());
        prefs.set(IdePreferences.KEY_WINDOW_HEIGHT, getHeight());
        prefs.set(IdePreferences.KEY_WINDOW_X, getX());
        prefs.set(IdePreferences.KEY_WINDOW_Y, getY());
        prefs.set(IdePreferences.KEY_SPLIT_POS_H, mainSplit.getDividerLocation());
        prefs.set(IdePreferences.KEY_SPLIT_POS_V, rightSplit.getDividerLocation());
        prefs.save();

        // Check unsaved files
        boolean hasUnsaved = editorTabs.getOpenEditors().stream().anyMatch(CodeEditorPane::isModified);
        if (hasUnsaved) {
            int choice = JOptionPane.showConfirmDialog(this,
                "You have unsaved changes. Save all before exiting?",
                "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
            if (choice == JOptionPane.CANCEL_OPTION) return;
            if (choice == JOptionPane.YES_OPTION) editorTabs.saveAllFiles();
        }

        buildManager.cancel();
        dispose();
        System.exit(0);
    }

    private void updateTitle(String fileName) {
        String projectName = currentProject != null ? currentProject.getName() : "";
        if (fileName != null && !fileName.isEmpty() && !fileName.equals(projectName)) {
            setTitle("SabreTooth IDE — " + projectName + " — " + fileName);
        } else {
            setTitle("SabreTooth IDE" + (projectName.isEmpty() ? "" : " — " + projectName));
        }
    }

    private Image createAppIcon() {
        // Create a simple sword icon programmatically
        int size = 32;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Theme.BG_DARK);
        g.fillRoundRect(0, 0, size, size, 8, 8);
        g.setColor(Theme.FG_ACCENT);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // Sword shape
        g.drawLine(6, 26, 26, 6);
        g.drawLine(24, 8, 28, 4);
        g.drawLine(4, 20, 12, 20);
        g.drawLine(4, 20, 4, 28);
        g.dispose();
        return img;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Menu helpers
    // ──────────────────────────────────────────────────────────────────────────
    private JMenu menu(String text) {
        JMenu m = new JMenu(text);
        m.setBackground(Theme.BG_MENU);
        m.setForeground(Theme.FG_BRIGHT);
        m.setFont(Theme.FONT_UI);
        return m;
    }

    private JMenuItem item(String text, KeyStroke ks) {
        JMenuItem mi = new JMenuItem(text);
        mi.setBackground(Theme.BG_MENU);
        mi.setForeground(Theme.FG_BRIGHT);
        mi.setFont(Theme.FONT_UI);
        if (ks != null) {
            mi.setAccelerator(ks);
            // Register as global shortcut too
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, text);
            getRootPane().getActionMap().put(text, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    if (mi.isEnabled()) mi.doClick();
                }
            });
        }
        return mi;
    }
}
