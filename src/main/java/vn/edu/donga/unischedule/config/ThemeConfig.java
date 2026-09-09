package vn.edu.donga.unischedule.config;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import java.awt.Font;

public final class ThemeConfig {
    private ThemeConfig() {
    }

    public static void install() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignoredAgain) {
                // Swing keeps its default look and feel if both setup paths fail.
            }
        }
        try {
            for (String weight : new String[]{"Regular", "Medium", "SemiBold", "Bold"}) {
                try (var stream = ThemeConfig.class.getResourceAsStream("/fonts/Poppins-" + weight + ".ttf")) {
                    if (stream != null) java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                            .registerFont(Font.createFont(Font.TRUETYPE_FONT, stream));
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot load bundled Poppins fonts", ex);
        }
        Font base = javax.swing.text.StyleContext.getDefaultStyleContext().getFont("Poppins", Font.PLAIN, 13);
        UIManager.put("defaultFont", base);
        UIManager.put("Button.arc", 24);
        UIManager.put("Component.arc", 16);
        UIManager.put("TextComponent.arc", 16);
        UIManager.put("Component.minimumHeight", 36);
        UIManager.put("Button.minimumHeight", 36);
        UIManager.put("ToggleButton.arc", 32);
        UIManager.put("ToggleButton.selectedBackground", java.awt.Color.decode("#EEF2FF"));
        UIManager.put("ToggleButton.selectedForeground", AppConfig.PRIMARY);
        UIManager.put("Button.margin", new java.awt.Insets(8, 14, 8, 14));
        UIManager.put("Component.focusColor", AppConfig.PRIMARY);
        UIManager.put("Component.borderColor", java.awt.Color.decode("#E2E8F0"));
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Panel.background", AppConfig.BACKGROUND);
        UIManager.put("Label.foreground", AppConfig.TEXT);
        UIManager.put("TabbedPane.underlineColor", AppConfig.PRIMARY);
        UIManager.put("TabbedPane.selectedBackground", java.awt.Color.WHITE);
        UIManager.put("Table.selectionBackground", java.awt.Color.decode("#EEF2FF"));
        UIManager.put("Table.selectionForeground", AppConfig.TEXT);
        UIManager.put("Table.rowHeight", 48);
        UIManager.put("TextComponent.margin", new java.awt.Insets(8, 12, 8, 12));
        UIManager.put("ScrollBar.width", 8);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
    }
}
