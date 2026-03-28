package com.vitalsoft.sabretooth.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.*;
import java.util.regex.*;

/**
 * Java/XML/Groovy syntax highlighter for JTextPane.
 * Uses a DocumentListener approach with incremental re-highlighting.
 */
public class SyntaxHighlighter {

    public enum Language { JAVA, XML, GROOVY, KOTLIN, PROPERTIES, PLAIN }

    // Java keywords
    private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList(
        "abstract","assert","boolean","break","byte","case","catch","char","class",
        "const","continue","default","do","double","else","enum","extends","final",
        "finally","float","for","goto","if","implements","import","instanceof","int",
        "interface","long","native","new","package","private","protected","public",
        "return","short","static","strictfp","super","switch","synchronized","this",
        "throw","throws","transient","try","var","void","volatile","while","record",
        "sealed","permits","yield","null","true","false"
    ));

    private static final Set<String> JAVA_TYPES = new HashSet<>(Arrays.asList(
        "String","Integer","Long","Double","Float","Boolean","Byte","Short","Character",
        "Object","Class","Number","Math","System","Runtime","Thread","Runnable","Exception",
        "Throwable","Error","StringBuilder","StringBuffer","List","ArrayList","LinkedList",
        "Map","HashMap","LinkedHashMap","TreeMap","Set","HashSet","LinkedHashSet","TreeSet",
        "Collections","Arrays","Optional","Stream","Iterator","Iterable","Comparable",
        "Comparator","Override","Deprecated","SuppressWarnings","FunctionalInterface",
        "Annotation","Enum","Record","Interface"
    ));

    private final JTextPane textPane;
    private final SimpleAttributeSet defaultStyle;
    private final SimpleAttributeSet keywordStyle;
    private final SimpleAttributeSet typeStyle;
    private final SimpleAttributeSet commentStyle;
    private final SimpleAttributeSet stringStyle;
    private final SimpleAttributeSet numberStyle;
    private final SimpleAttributeSet annotationStyle;
    private final SimpleAttributeSet xmlTagStyle;
    private final SimpleAttributeSet xmlAttrStyle;
    private Language language = Language.JAVA;
    private boolean highlighting = false;

    public SyntaxHighlighter(JTextPane pane) {
        this.textPane = pane;

        defaultStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(defaultStyle, Theme.FG_DEFAULT);
        StyleConstants.setBackground(defaultStyle, Theme.BG_EDITOR);

        keywordStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(keywordStyle, Theme.FG_KEYWORD);
        StyleConstants.setBold(keywordStyle, true);

        typeStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(typeStyle, Theme.FG_TYPE);

        commentStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(commentStyle, Theme.FG_COMMENT);
        StyleConstants.setItalic(commentStyle, true);

        stringStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(stringStyle, Theme.FG_STRING);

        numberStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(numberStyle, Theme.FG_NUMBER);

        annotationStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(annotationStyle, Theme.FG_ANNOTATION);

        xmlTagStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(xmlTagStyle, Theme.FG_KEYWORD);

        xmlAttrStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(xmlAttrStyle, Theme.FG_ACCENT);
    }

    public void setLanguage(Language lang) {
        this.language = lang;
    }

    public static Language detectLanguage(String filename) {
        if (filename == null) return Language.PLAIN;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".java")) return Language.JAVA;
        if (lower.endsWith(".kt")) return Language.KOTLIN;
        if (lower.endsWith(".gradle") || lower.endsWith(".groovy")) return Language.GROOVY;
        if (lower.endsWith(".xml") || lower.endsWith(".axml")) return Language.XML;
        if (lower.endsWith(".properties") || lower.endsWith(".conf")) return Language.PROPERTIES;
        return Language.PLAIN;
    }

    public void highlight() {
        if (highlighting) return;
        highlighting = true;
        SwingUtilities.invokeLater(() -> {
            try {
                doHighlight();
            } finally {
                highlighting = false;
            }
        });
    }

    private void doHighlight() {
        StyledDocument doc = textPane.getStyledDocument();
        String text;
        try {
            text = doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            return;
        }

        // Reset all to default
        doc.setCharacterAttributes(0, doc.getLength(), defaultStyle, true);

        switch (language) {
            case JAVA, GROOVY, KOTLIN -> highlightJava(doc, text);
            case XML -> highlightXml(doc, text);
            case PROPERTIES -> highlightProperties(doc, text);
            default -> {}
        }
    }

    private void highlightJava(StyledDocument doc, String text) {
        int len = text.length();
        int i = 0;
        while (i < len) {
            char c = text.charAt(i);

            // Single-line comment
            if (c == '/' && i + 1 < len && text.charAt(i + 1) == '/') {
                int end = text.indexOf('\n', i);
                if (end < 0) end = len;
                doc.setCharacterAttributes(i, end - i, commentStyle, false);
                i = end;
                continue;
            }
            // Multi-line comment
            if (c == '/' && i + 1 < len && text.charAt(i + 1) == '*') {
                int end = text.indexOf("*/", i + 2);
                if (end < 0) end = len - 2;
                else end += 2;
                doc.setCharacterAttributes(i, end - i, commentStyle, false);
                i = end;
                continue;
            }
            // String literal
            if (c == '"') {
                int end = i + 1;
                while (end < len) {
                    char sc = text.charAt(end);
                    if (sc == '\\') { end += 2; continue; }
                    if (sc == '"') { end++; break; }
                    if (sc == '\n') break;
                    end++;
                }
                doc.setCharacterAttributes(i, end - i, stringStyle, false);
                i = end;
                continue;
            }
            // Char literal
            if (c == '\'') {
                int end = i + 1;
                while (end < len && end < i + 4) {
                    char sc = text.charAt(end);
                    if (sc == '\\') { end += 2; continue; }
                    if (sc == '\'') { end++; break; }
                    end++;
                }
                doc.setCharacterAttributes(i, end - i, stringStyle, false);
                i = end;
                continue;
            }
            // Annotation
            if (c == '@') {
                int end = i + 1;
                while (end < len && Character.isLetterOrDigit(text.charAt(end))) end++;
                doc.setCharacterAttributes(i, end - i, annotationStyle, false);
                i = end;
                continue;
            }
            // Number
            if (Character.isDigit(c) || (c == '-' && i + 1 < len && Character.isDigit(text.charAt(i + 1))
                    && (i == 0 || !Character.isLetterOrDigit(text.charAt(i - 1))))) {
                int end = i + 1;
                while (end < len && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '.' || text.charAt(end) == '_')) end++;
                doc.setCharacterAttributes(i, end - i, numberStyle, false);
                i = end;
                continue;
            }
            // Identifier / keyword
            if (Character.isLetter(c) || c == '_') {
                int end = i;
                while (end < len && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '_')) end++;
                String word = text.substring(i, end);
                if (JAVA_KEYWORDS.contains(word)) {
                    doc.setCharacterAttributes(i, end - i, keywordStyle, false);
                } else if (JAVA_TYPES.contains(word) || Character.isUpperCase(c)) {
                    doc.setCharacterAttributes(i, end - i, typeStyle, false);
                }
                i = end;
                continue;
            }
            i++;
        }
    }

    private void highlightXml(StyledDocument doc, String text) {
        // Highlight XML tags and attributes
        Pattern tagPattern = Pattern.compile("</?[a-zA-Z][a-zA-Z0-9:._-]*|/>|>");
        Pattern attrPattern = Pattern.compile("\\s([a-zA-Z][a-zA-Z0-9:._-]*)\\s*=");
        Pattern stringPattern = Pattern.compile("\"[^\"]*\"|'[^']*'");
        Pattern commentPattern = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

        // Comments first
        Matcher m = commentPattern.matcher(text);
        while (m.find()) doc.setCharacterAttributes(m.start(), m.end() - m.start(), commentStyle, false);

        // Tags
        m = tagPattern.matcher(text);
        while (m.find()) doc.setCharacterAttributes(m.start(), m.end() - m.start(), xmlTagStyle, false);

        // Attributes
        m = attrPattern.matcher(text);
        while (m.find()) doc.setCharacterAttributes(m.start(1), m.end(1) - m.start(1), xmlAttrStyle, false);

        // String values
        m = stringPattern.matcher(text);
        while (m.find()) doc.setCharacterAttributes(m.start(), m.end() - m.start(), stringStyle, false);
    }

    private void highlightProperties(StyledDocument doc, String text) {
        String[] lines = text.split("\n", -1);
        int pos = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#") || trimmed.startsWith("!")) {
                doc.setCharacterAttributes(pos, line.length(), commentStyle, false);
            } else {
                int eq = line.indexOf('=');
                int colon = line.indexOf(':');
                int sep = -1;
                if (eq >= 0 && colon >= 0) sep = Math.min(eq, colon);
                else if (eq >= 0) sep = eq;
                else if (colon >= 0) sep = colon;
                if (sep > 0) {
                    doc.setCharacterAttributes(pos, sep, keywordStyle, false);
                    if (sep + 1 < line.length()) {
                        doc.setCharacterAttributes(pos + sep + 1, line.length() - sep - 1, stringStyle, false);
                    }
                }
            }
            pos += line.length() + 1;
        }
    }
}
