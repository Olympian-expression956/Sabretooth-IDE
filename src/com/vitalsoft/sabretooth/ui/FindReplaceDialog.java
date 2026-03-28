package com.vitalsoft.sabretooth.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

public class FindReplaceDialog extends JDialog {
    private final JTextField findField;
    private final JTextField replaceField;
    private final JCheckBox caseSensitive;
    private CodeEditorPane currentEditor;

    public FindReplaceDialog(Frame parent) {
        super(parent, "Find / Replace", false);
        setAlwaysOnTop(true);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BG_PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Find row
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel findLbl = new JLabel("Find:");
        findLbl.setForeground(Theme.FG_DIM);
        findLbl.setFont(Theme.FONT_UI);
        root.add(findLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        findField = styledTf();
        root.add(findField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        JButton findNext = btn("Find Next");
        findNext.addActionListener(e -> doFind(false));
        root.add(findNext, gbc);

        gbc.gridx = 3;
        JButton findPrev = btn("Find Prev");
        findPrev.addActionListener(e -> doFindPrev());
        root.add(findPrev, gbc);

        // Replace row
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel replaceLbl = new JLabel("Replace:");
        replaceLbl.setForeground(Theme.FG_DIM);
        replaceLbl.setFont(Theme.FONT_UI);
        root.add(replaceLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        replaceField = styledTf();
        root.add(replaceField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        JButton replace = btn("Replace");
        replace.addActionListener(e -> doReplace());
        root.add(replace, gbc);

        gbc.gridx = 3;
        JButton replaceAll = btn("Replace All");
        replaceAll.addActionListener(e -> doReplaceAll());
        root.add(replaceAll, gbc);

        // Options row
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 3;
        caseSensitive = new JCheckBox("Case Sensitive");
        caseSensitive.setBackground(Theme.BG_PANEL);
        caseSensitive.setForeground(Theme.FG_BRIGHT);
        caseSensitive.setFont(Theme.FONT_UI);
        root.add(caseSensitive, gbc);

        // Close button
        gbc.gridx = 3; gbc.gridy = 3; gbc.gridwidth = 1;
        JButton close = btn("Close");
        close.addActionListener(e -> setVisible(false));
        root.add(close, gbc);

        // Enter key triggers find
        findField.addActionListener(e -> doFind(false));

        pack();
        setResizable(false);
    }

    private void doFind(boolean fromStart) {
        if (currentEditor == null) return;
        String query = findField.getText();
        if (query.isEmpty()) return;
        int idx = currentEditor.findNext(query, caseSensitive.isSelected(), fromStart);
        if (idx < 0) {
            findField.setBackground(new Color(0x6B, 0x2B, 0x2B));
        } else {
            findField.setBackground(Theme.BG_INPUT);
        }
    }

    private void doFindPrev() {
        if (currentEditor == null) return;
        // Move caret back then search
        JTextPane tp = currentEditor.getTextPane();
        int pos = tp.getSelectionStart();
        if (pos > 0) tp.setCaretPosition(pos - 1);
        doFind(false);
    }

    private void doReplace() {
        if (currentEditor == null) return;
        JTextPane tp = currentEditor.getTextPane();
        String selected = tp.getSelectedText();
        String query = findField.getText();
        if (selected != null && !selected.isEmpty()) {
            boolean matches = caseSensitive.isSelected()
                ? selected.equals(query)
                : selected.equalsIgnoreCase(query);
            if (matches) {
                tp.replaceSelection(replaceField.getText());
            }
        }
        doFind(false);
    }

    private void doReplaceAll() {
        if (currentEditor == null) return;
        String text = currentEditor.getText();
        String query = findField.getText();
        String replacement = replaceField.getText();
        if (query.isEmpty()) return;
        String newText = caseSensitive.isSelected()
            ? text.replace(query, replacement)
            : text.replaceAll("(?i)" + java.util.regex.Pattern.quote(query),
                              java.util.regex.Matcher.quoteReplacement(replacement));
        int caret = currentEditor.getTextPane().getCaretPosition();
        currentEditor.setText(newText);
        try { currentEditor.getTextPane().setCaretPosition(Math.min(caret, newText.length())); }
        catch (Exception ignored) {}
    }

    public void setEditor(CodeEditorPane editor) {
        this.currentEditor = editor;
        // Pre-fill with selected text if any
        if (editor != null) {
            String sel = editor.getTextPane().getSelectedText();
            if (sel != null && !sel.isEmpty() && sel.length() < 200) {
                findField.setText(sel);
            }
        }
        findField.selectAll();
        findField.requestFocus();
    }

    private JTextField styledTf() {
        JTextField tf = new JTextField(24);
        tf.setBackground(Theme.BG_INPUT);
        tf.setForeground(Theme.FG_BRIGHT);
        tf.setCaretColor(Theme.FG_BRIGHT);
        tf.setFont(Theme.FONT_UI);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        return tf;
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
