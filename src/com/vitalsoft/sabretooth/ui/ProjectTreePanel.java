package com.vitalsoft.sabretooth.ui;

import com.vitalsoft.sabretooth.project.Project;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.io.File;
import java.util.*;

public class ProjectTreePanel extends JPanel {
    private JTree tree;
    private DefaultTreeModel treeModel;
    private java.util.function.Consumer<File> fileOpenCallback;

    public ProjectTreePanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG_TREE);
        setPreferredSize(new Dimension(250, 600));

        JLabel header = new JLabel(" Project");
        header.setBackground(Theme.BG_TOOLBAR);
        header.setForeground(Theme.FG_BRIGHT);
        header.setFont(Theme.FONT_UI.deriveFont(Font.BOLD));
        header.setOpaque(true);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.BORDER));
        header.setPreferredSize(new Dimension(250, 26));
        add(header, BorderLayout.NORTH);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("No Project Open");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setBackground(Theme.BG_TREE);
        tree.setForeground(Theme.FG_BRIGHT);
        tree.setFont(Theme.FONT_UI);
        tree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        tree.setShowsRootHandles(true);
        tree.setRootVisible(true);
        tree.setCellRenderer(new FileTreeCellRenderer());
        tree.setRowHeight(22);

        // Double-click to open file
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path == null) return;
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    Object uo = node.getUserObject();
                    if (uo instanceof File f && f.isFile() && fileOpenCallback != null) {
                        fileOpenCallback.accept(f);
                    }
                }
            }
        });

        // Context menu
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }
        });

        JScrollPane scroll = new JScrollPane(tree);
        scroll.setBackground(Theme.BG_TREE);
        scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));
        scroll.getViewport().setBackground(Theme.BG_TREE);
        add(scroll, BorderLayout.CENTER);
    }

    private void showContextMenu(java.awt.event.MouseEvent e) {
        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
        if (path == null) return;
        tree.setSelectionPath(path);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object uo = node.getUserObject();
        if (!(uo instanceof File file)) return;

        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Theme.BG_MENU);
        menu.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        if (file.isFile()) {
            JMenuItem open = styledItem("Open");
            open.addActionListener(ev -> { if (fileOpenCallback != null) fileOpenCallback.accept(file); });
            menu.add(open);
            menu.addSeparator();
        }

        if (file.isDirectory()) {
            JMenuItem newFile = styledItem("New File...");
            newFile.addActionListener(ev -> createNewFile(file, node));
            menu.add(newFile);
            JMenuItem newDir = styledItem("New Directory...");
            newDir.addActionListener(ev -> createNewDirectory(file, node));
            menu.add(newDir);
            menu.addSeparator();
        }

        JMenuItem rename = styledItem("Rename...");
        rename.addActionListener(ev -> renameFile(file, node));
        menu.add(rename);

        JMenuItem delete = styledItem("Delete");
        delete.addActionListener(ev -> deleteFile(file, node));
        delete.setForeground(Theme.FG_RED);
        menu.add(delete);

        menu.show(tree, e.getX(), e.getY());
    }

    private JMenuItem styledItem(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setBackground(Theme.BG_MENU);
        item.setForeground(Theme.FG_BRIGHT);
        item.setFont(Theme.FONT_UI);
        return item;
    }

    private void createNewFile(File dir, DefaultMutableTreeNode parentNode) {
        String name = JOptionPane.showInputDialog(this, "File name:", "New File", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        File newFile = new File(dir, name.trim());
        try {
            newFile.createNewFile();
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newFile);
            insertSorted(parentNode, newNode);
            treeModel.reload(parentNode);
            if (fileOpenCallback != null) fileOpenCallback.accept(newFile);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createNewDirectory(File dir, DefaultMutableTreeNode parentNode) {
        String name = JOptionPane.showInputDialog(this, "Directory name:", "New Directory", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        File newDir = new File(dir, name.trim());
        newDir.mkdir();
        DefaultMutableTreeNode newNode = buildTree(newDir);
        insertSorted(parentNode, newNode);
        treeModel.reload(parentNode);
    }

    private void renameFile(File file, DefaultMutableTreeNode node) {
        String newName = (String) JOptionPane.showInputDialog(this, "Rename to:", "Rename",
            JOptionPane.PLAIN_MESSAGE, null, null, file.getName());
        if (newName == null || newName.trim().isEmpty()) return;
        File renamed = new File(file.getParent(), newName.trim());
        if (file.renameTo(renamed)) {
            node.setUserObject(renamed);
            treeModel.nodeChanged(node);
        }
    }

    private void deleteFile(File file, DefaultMutableTreeNode node) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete " + file.getName() + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        deleteRecursive(file);
        treeModel.removeNodeFromParent(node);
    }

    private void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteRecursive(c);
        }
        f.delete();
    }

    private void insertSorted(DefaultMutableTreeNode parent, DefaultMutableTreeNode child) {
        int count = parent.getChildCount();
        for (int i = 0; i < count; i++) {
            DefaultMutableTreeNode existing = (DefaultMutableTreeNode) parent.getChildAt(i);
            Object uo = existing.getUserObject();
            if (uo instanceof File ef) {
                Object co = child.getUserObject();
                if (co instanceof File cf) {
                    // dirs first, then files
                    if (cf.isDirectory() && !ef.isDirectory()) {
                        parent.insert(child, i); return;
                    }
                    if (cf.isDirectory() == ef.isDirectory() && cf.getName().compareToIgnoreCase(ef.getName()) < 0) {
                        parent.insert(child, i); return;
                    }
                }
            }
        }
        parent.add(child);
    }

    public void loadProject(Project project) {
        DefaultMutableTreeNode root = buildTree(project.getRootDir());
        treeModel.setRoot(root);
        tree.expandPath(new TreePath(root.getPath()));
        // Expand first level
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
    }

    public void refresh(Project project) {
        if (project != null) loadProject(project);
    }

    private DefaultMutableTreeNode buildTree(File dir) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(dir);
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                Arrays.sort(children, (a, b) -> {
                    if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });
                for (File child : children) {
                    if (shouldHide(child)) continue;
                    node.add(buildTree(child));
                }
            }
        }
        return node;
    }

    private boolean shouldHide(File f) {
        String name = f.getName();
        return name.startsWith(".git") || name.equals(".gradle") || name.equals("build")
            || name.equals(".idea") || name.equals("__pycache__") || name.equals(".DS_Store");
    }

    public void setFileOpenCallback(java.util.function.Consumer<File> cb) { this.fileOpenCallback = cb; }

    // Custom cell renderer for file tree
    private static class FileTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                       boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            setBackground(sel ? Theme.BG_SELECTED : Theme.BG_TREE);
            setForeground(sel ? Theme.FG_BRIGHT : Theme.FG_DEFAULT);
            setBackgroundNonSelectionColor(Theme.BG_TREE);
            setBackgroundSelectionColor(Theme.BG_SELECTED);
            setTextNonSelectionColor(Theme.FG_DEFAULT);
            setTextSelectionColor(Theme.FG_BRIGHT);
            setBorderSelectionColor(Theme.BG_SELECTED);
            setFont(Theme.FONT_UI);

            if (value instanceof DefaultMutableTreeNode dmtn) {
                Object uo = dmtn.getUserObject();
                if (uo instanceof File f) {
                    setText(f.getName());
                    if (f.isDirectory()) {
                        setIcon(getDirIcon());
                    } else {
                        setIcon(getFileIcon(f.getName()));
                    }
                } else {
                    setText(uo.toString());
                }
            }
            return this;
        }

        private Icon getDirIcon() {
            return UIManager.getIcon("FileView.directoryIcon");
        }

        private Icon getFileIcon(String name) {
            String lower = name.toLowerCase();
            if (lower.endsWith(".java") || lower.endsWith(".kt")) {
                return createColorIcon(Theme.FG_ACCENT, 14);
            } else if (lower.endsWith(".xml")) {
                return createColorIcon(Theme.FG_ORANGE, 14);
            } else if (lower.endsWith(".gradle") || lower.endsWith(".groovy")) {
                return createColorIcon(Theme.FG_GREEN, 14);
            } else if (lower.endsWith(".sabreconf") || lower.endsWith(".properties")) {
                return createColorIcon(Theme.FG_YELLOW, 14);
            }
            return UIManager.getIcon("FileView.fileIcon");
        }

        private Icon createColorIcon(Color color, int size) {
            return new Icon() {
                @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                    g.setColor(color);
                    g.fillRoundRect(x + 1, y + 1, size - 3, size - 3, 3, 3);
                }
                @Override public int getIconWidth() { return size; }
                @Override public int getIconHeight() { return size; }
            };
        }
    }
}
