package com.vitalsoft.sabretooth.ui;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;

public class CodeEditorPane extends JPanel {
    private final JTextPane textPane;
    private final LineNumberPanel lineNumbers;
    private final SyntaxHighlighter highlighter;
    private File file;
    private boolean modified = false;
    private String originalContent = "";
    private final java.util.List<Runnable> modifiedListeners = new java.util.ArrayList<>();

    public CodeEditorPane() {
        setLayout(new BorderLayout());

        textPane = new JTextPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getUI().getPreferredSize(this).width <= getParent().getSize().width;
            }
        };

        // Editor styling
        textPane.setBackground(Theme.BG_EDITOR);
        textPane.setForeground(Theme.FG_DEFAULT);
        textPane.setCaretColor(Color.WHITE);
        textPane.setSelectionColor(Theme.BG_SELECTED);
        textPane.setSelectedTextColor(Theme.FG_BRIGHT);
        textPane.setFont(Theme.FONT_CODE);
        textPane.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        // Set document with default char attrs
        StyledDocument doc = textPane.getStyledDocument();
        SimpleAttributeSet defAttrs = new SimpleAttributeSet();
        StyleConstants.setForeground(defAttrs, Theme.FG_DEFAULT);
        StyleConstants.setBackground(defAttrs, Theme.BG_EDITOR);
        StyleConstants.setFontFamily(defAttrs, Theme.FONT_CODE.getFamily());
        StyleConstants.setFontSize(defAttrs, Theme.FONT_CODE.getSize());
        doc.setParagraphAttributes(0, 0, defAttrs, false);

        highlighter = new SyntaxHighlighter(textPane);

        // Line number panel
        lineNumbers = new LineNumberPanel(textPane);

        JScrollPane scroll = new JScrollPane(textPane);
        scroll.setRowHeaderView(lineNumbers);
        scroll.setBackground(Theme.BG_EDITOR);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BG_EDITOR);
        scroll.getHorizontalScrollBar().setBackground(Theme.BG_PANEL);
        scroll.getVerticalScrollBar().setBackground(Theme.BG_PANEL);

        add(scroll, BorderLayout.CENTER);

        // Tab key -> spaces
        textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    e.consume();
                    try {
                        int pos = textPane.getCaretPosition();
                        textPane.getDocument().insertString(pos, "    ", null);
                    } catch (BadLocationException ignored) {}
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleEnterKey(e);
                }
            }
        });

        // Document listener for highlighting + modified tracking
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onDocumentChange(); }
            @Override public void removeUpdate(DocumentEvent e) { onDocumentChange(); }
            @Override public void changedUpdate(DocumentEvent e) {}
        });
    }

    private void handleEnterKey(KeyEvent e) {
        // Auto-indent: match leading whitespace of current line
        try {
            int pos = textPane.getCaretPosition();
            int lineStart = Utilities.getRowStart(textPane, pos);
            String lineText = textPane.getDocument().getText(lineStart, pos - lineStart);
            StringBuilder indent = new StringBuilder("\n");
            for (char c : lineText.toCharArray()) {
                if (c == ' ' || c == '\t') indent.append(c);
                else break;
            }
            // If line ends with '{', add extra indent
            String trimmed = lineText.stripTrailing();
            if (trimmed.endsWith("{")) indent.append("    ");
            e.consume();
            textPane.getDocument().insertString(pos, indent.toString(), null);
        } catch (BadLocationException ignored) {}
    }

    private void onDocumentChange() {
        boolean wasModified = modified;
        try {
            String current = textPane.getDocument().getText(0, textPane.getDocument().getLength());
            modified = !current.equals(originalContent);
        } catch (BadLocationException ignored) {}
        if (wasModified != modified) {
            for (Runnable r : modifiedListeners) r.run();
        }
        // Re-highlight (debounced via SwingUtilities)
        highlighter.highlight();
    }

    public void loadFile(File f) throws IOException {
        this.file = f;
        String content = Files.readString(f.toPath());
        originalContent = content;
        modified = false;
        highlighter.setLanguage(SyntaxHighlighter.detectLanguage(f.getName()));
        // Set text - we re-sync originalContent after
        textPane.setText(content);
        originalContent = content;
        modified = false;
        textPane.setCaretPosition(0);
        highlighter.highlight();
    }

    public void saveFile() throws IOException {
        if (file == null) return;
        String content = getText();
        Files.writeString(file.toPath(), content);
        originalContent = content;
        modified = false;
        for (Runnable r : modifiedListeners) r.run();
    }

    public void saveFileAs(File f) throws IOException {
        this.file = f;
        saveFile();
    }

    public String getText() {
        try {
            return textPane.getDocument().getText(0, textPane.getDocument().getLength());
        } catch (BadLocationException e) {
            return "";
        }
    }

    public void setText(String text) {
        textPane.setText(text);
        originalContent = text;
        modified = false;
        textPane.setCaretPosition(0);
        highlighter.highlight();
    }

    public void setLanguage(SyntaxHighlighter.Language lang) {
        highlighter.setLanguage(lang);
        highlighter.highlight();
    }

    public boolean isModified() { return modified; }
    public File getFile() { return file; }
    public JTextPane getTextPane() { return textPane; }

    public void addModifiedListener(Runnable r) { modifiedListeners.add(r); }

    public void updateFontSize(int size) {
        Font f = Theme.FONT_CODE.deriveFont((float) size);
        textPane.setFont(f);
        lineNumbers.setFont(f.deriveFont((float)(size - 1)));
        highlighter.highlight();
    }

    // Find & replace
    public int findNext(String query, boolean caseSensitive, boolean fromStart) {
        try {
            String text = textPane.getDocument().getText(0, textPane.getDocument().getLength());
            String searchText = caseSensitive ? text : text.toLowerCase();
            String searchQuery = caseSensitive ? query : query.toLowerCase();
            int start = fromStart ? 0 : textPane.getCaretPosition();
            int idx = searchText.indexOf(searchQuery, start);
            if (idx < 0 && start > 0) idx = searchText.indexOf(searchQuery, 0); // wrap
            if (idx >= 0) {
                textPane.setSelectionStart(idx);
                textPane.setSelectionEnd(idx + query.length());
                textPane.requestFocus();
                return idx;
            }
        } catch (BadLocationException ignored) {}
        return -1;
    }

    /**
     * Line numbers panel displayed in the scroll pane row header.
     */
    private static class LineNumberPanel extends JComponent {
        private final JTextPane editor;
        private static final int PADDING = 8;

        LineNumberPanel(JTextPane editor) {
            this.editor = editor;
            setBackground(Theme.GUTTER_BG);
            setForeground(Theme.GUTTER_FG);
            setFont(Theme.FONT_CODE.deriveFont(Theme.FONT_CODE.getSize() - 1f));
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));

            editor.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { repaint(); }
                @Override public void removeUpdate(DocumentEvent e) { repaint(); }
                @Override public void changedUpdate(DocumentEvent e) { repaint(); }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            int lines = countLines();
            int digits = Math.max(3, String.valueOf(lines).length());
            FontMetrics fm = getFontMetrics(getFont());
            int w = fm.charWidth('0') * digits + PADDING * 2;
            return new Dimension(w, editor.getHeight());
        }

        private int countLines() {
            try {
                int len = editor.getDocument().getLength();
                String text = editor.getDocument().getText(0, len);
                int count = 1;
                for (char c : text.toCharArray()) if (c == '\n') count++;
                return count;
            } catch (BadLocationException e) { return 1; }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.GUTTER_BG);
            g2.fillRect(0, 0, getWidth(), getHeight());

            FontMetrics fm = g2.getFontMetrics(getFont());
            g2.setFont(getFont());
            g2.setColor(Theme.GUTTER_FG);

            Rectangle clip = g.getClipBounds();
            int startY = clip != null ? clip.y : 0;
            int endY = clip != null ? clip.y + clip.height : getHeight();

            try {
                int docLen = editor.getDocument().getLength();
                String text = editor.getDocument().getText(0, docLen);
                int lineNum = 1;
                int offset = 0;
                while (offset <= docLen) {
                    Rectangle r = editor.modelToView2D(offset).getBounds();
                    int lineY = r.y + r.height;
                    if (lineY >= startY && r.y <= endY) {
                        String num = String.valueOf(lineNum);
                        int x = getWidth() - fm.stringWidth(num) - PADDING;
                        int y = r.y + fm.getAscent() + 2;
                        g2.drawString(num, x, y);
                    }
                    if (lineY > endY && r.y > endY) break;
                    // Find next line
                    int nextLine = text.indexOf('\n', offset);
                    if (nextLine < 0 || nextLine >= docLen) break;
                    offset = nextLine + 1;
                    lineNum++;
                }
            } catch (BadLocationException ignored) {}
        }
    }
}
