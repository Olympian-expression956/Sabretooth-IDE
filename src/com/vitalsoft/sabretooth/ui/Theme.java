package com.vitalsoft.sabretooth.ui;

import java.awt.*;

public class Theme {
    // Background colors
    public static final Color BG_DARK        = new Color(0x1E, 0x1F, 0x22);
    public static final Color BG_PANEL       = new Color(0x2B, 0x2D, 0x30);
    public static final Color BG_EDITOR      = new Color(0x1E, 0x1F, 0x22);
    public static final Color BG_TAB_ACTIVE  = new Color(0x1E, 0x1F, 0x22);
    public static final Color BG_TAB_INACTIVE= new Color(0x2B, 0x2D, 0x30);
    public static final Color BG_TOOLBAR     = new Color(0x3C, 0x3F, 0x41);
    public static final Color BG_STATUSBAR   = new Color(0x3C, 0x3F, 0x41);
    public static final Color BG_CONSOLE     = new Color(0x13, 0x14, 0x16);
    public static final Color BG_MENU        = new Color(0x3C, 0x3F, 0x41);
    public static final Color BG_TREE        = new Color(0x25, 0x27, 0x2A);
    public static final Color BG_SELECTED    = new Color(0x26, 0x4F, 0x78);
    public static final Color BG_HOVER       = new Color(0x35, 0x37, 0x3A);
    public static final Color BG_INPUT       = new Color(0x43, 0x46, 0x49);
    public static final Color BG_BUTTON      = new Color(0x4C, 0x51, 0x56);
    public static final Color BG_BUTTON_HOV  = new Color(0x5C, 0x61, 0x66);

    // Foreground / text
    public static final Color FG_DEFAULT     = new Color(0xBC, 0xBC, 0xBC);
    public static final Color FG_BRIGHT      = new Color(0xD4, 0xD4, 0xD4);
    public static final Color FG_DIM         = new Color(0x80, 0x80, 0x80);
    public static final Color FG_ACCENT      = new Color(0x56, 0x9C, 0xD6);
    public static final Color FG_GREEN       = new Color(0x6A, 0x99, 0x55);
    public static final Color FG_ORANGE      = new Color(0xCE, 0x91, 0x78);
    public static final Color FG_YELLOW      = new Color(0xDC, 0xDC, 0xAA);
    public static final Color FG_PURPLE      = new Color(0xC5, 0x86, 0xC0);
    public static final Color FG_CYAN        = new Color(0x4E, 0xC9, 0xB0);
    public static final Color FG_RED         = new Color(0xF4, 0x47, 0x47);
    public static final Color FG_STRING      = new Color(0xCE, 0x91, 0x78);
    public static final Color FG_KEYWORD     = new Color(0x56, 0x9C, 0xD6);
    public static final Color FG_COMMENT     = new Color(0x6A, 0x99, 0x55);
    public static final Color FG_NUMBER      = new Color(0xB5, 0xCE, 0xA8);
    public static final Color FG_TYPE        = new Color(0x4E, 0xC9, 0xB0);
    public static final Color FG_ANNOTATION  = new Color(0xBB, 0xBB, 0xFF);
    public static final Color FG_ERROR       = new Color(0xF4, 0x47, 0x47);
    public static final Color FG_SUCCESS     = new Color(0x6A, 0x99, 0x55);
    public static final Color FG_WARNING     = new Color(0xDC, 0xDC, 0xAA);

    // Borders
    public static final Color BORDER         = new Color(0x43, 0x46, 0x49);
    public static final Color BORDER_ACCENT  = new Color(0x56, 0x9C, 0xD6);

    // Line number gutter
    public static final Color GUTTER_BG     = new Color(0x23, 0x24, 0x27);
    public static final Color GUTTER_FG     = new Color(0x60, 0x60, 0x60);

    // Fonts
    public static Font FONT_CODE;
    public static Font FONT_UI;
    public static Font FONT_SMALL;

    static {
        // Try to use a nice monospace font
        String[] monoFonts = {"JetBrains Mono", "Consolas", "DejaVu Sans Mono", "Courier New", "Monospaced"};
        String monoFont = "Monospaced";
        for (String f : monoFonts) {
            Font test = new Font(f, Font.PLAIN, 13);
            if (test.getFamily().equalsIgnoreCase(f) || f.equals("Monospaced")) {
                monoFont = f;
                break;
            }
        }
        FONT_CODE  = new Font(monoFont, Font.PLAIN, 13);
        FONT_UI    = new Font("SansSerif", Font.PLAIN, 12);
        FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    }

    public static void applyGlobalDefaults() {
        javax.swing.UIManager.put("Panel.background", BG_PANEL);
        javax.swing.UIManager.put("OptionPane.background", BG_PANEL);
        javax.swing.UIManager.put("OptionPane.messageForeground", FG_BRIGHT);
        javax.swing.UIManager.put("TextField.background", BG_INPUT);
        javax.swing.UIManager.put("TextField.foreground", FG_BRIGHT);
        javax.swing.UIManager.put("TextField.caretForeground", FG_BRIGHT);
        javax.swing.UIManager.put("TextField.selectionBackground", BG_SELECTED);
        javax.swing.UIManager.put("TextArea.background", BG_INPUT);
        javax.swing.UIManager.put("TextArea.foreground", FG_BRIGHT);
        javax.swing.UIManager.put("ComboBox.background", BG_INPUT);
        javax.swing.UIManager.put("ComboBox.foreground", FG_BRIGHT);
        javax.swing.UIManager.put("List.background", BG_INPUT);
        javax.swing.UIManager.put("List.foreground", FG_BRIGHT);
        javax.swing.UIManager.put("List.selectionBackground", BG_SELECTED);
        javax.swing.UIManager.put("List.selectionForeground", FG_BRIGHT);
        javax.swing.UIManager.put("Button.background", BG_BUTTON);
        javax.swing.UIManager.put("Button.foreground", FG_BRIGHT);
        javax.swing.UIManager.put("Label.foreground", FG_BRIGHT);
        javax.swing.UIManager.put("CheckBox.background", BG_PANEL);
        javax.swing.UIManager.put("CheckBox.foreground", FG_BRIGHT);
        javax.swing.UIManager.put("RadioButton.background", BG_PANEL);
        javax.swing.UIManager.put("RadioButton.foreground", FG_BRIGHT);
        javax.swing.UIManager.put("Spinner.background", BG_INPUT);
        javax.swing.UIManager.put("Spinner.foreground", FG_BRIGHT);
        javax.swing.UIManager.put("Tree.background", BG_TREE);
        javax.swing.UIManager.put("Tree.foreground", FG_BRIGHT);
        javax.swing.UIManager.put("Tree.selectionBackground", BG_SELECTED);
        javax.swing.UIManager.put("Tree.selectionForeground", FG_BRIGHT);
        javax.swing.UIManager.put("Tree.textBackground", BG_TREE);
        javax.swing.UIManager.put("Tree.textForeground", FG_BRIGHT);
        javax.swing.UIManager.put("ScrollPane.background", BG_PANEL);
        javax.swing.UIManager.put("ScrollBar.background", BG_PANEL);
        javax.swing.UIManager.put("ScrollBar.thumb", BG_TOOLBAR);
        javax.swing.UIManager.put("TabbedPane.background", BG_PANEL);
        javax.swing.UIManager.put("TabbedPane.foreground", FG_BRIGHT);
        javax.swing.UIManager.put("TabbedPane.selected", BG_TAB_ACTIVE);
        javax.swing.UIManager.put("SplitPane.background", BG_DARK);
        javax.swing.UIManager.put("MenuBar.background", BG_MENU);
        javax.swing.UIManager.put("MenuBar.foreground", FG_BRIGHT);
        javax.swing.UIManager.put("Menu.background", BG_MENU);
        javax.swing.UIManager.put("Menu.foreground", FG_BRIGHT);
        javax.swing.UIManager.put("MenuItem.background", BG_MENU);
        javax.swing.UIManager.put("MenuItem.foreground", FG_BRIGHT);
        javax.swing.UIManager.put("MenuItem.selectionBackground", BG_SELECTED);
        javax.swing.UIManager.put("MenuItem.selectionForeground", FG_BRIGHT);
        javax.swing.UIManager.put("PopupMenu.background", BG_MENU);
        javax.swing.UIManager.put("PopupMenu.border", javax.swing.BorderFactory.createLineBorder(BORDER));
        javax.swing.UIManager.put("Separator.foreground", BORDER);
        javax.swing.UIManager.put("ToolTip.background", BG_TOOLBAR);
        javax.swing.UIManager.put("ToolTip.foreground", FG_BRIGHT);
        javax.swing.UIManager.put("ToolTip.border", javax.swing.BorderFactory.createLineBorder(BORDER));
        javax.swing.UIManager.put("Table.background", BG_INPUT);
        javax.swing.UIManager.put("Table.foreground", FG_BRIGHT);
        javax.swing.UIManager.put("Table.selectionBackground", BG_SELECTED);
        javax.swing.UIManager.put("Table.gridColor", BORDER);
        javax.swing.UIManager.put("TableHeader.background", BG_TOOLBAR);
        javax.swing.UIManager.put("TableHeader.foreground", FG_BRIGHT);
    }
}
