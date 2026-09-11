package vn.edu.donga.unischedule.ui.dialog;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.ui.component.HeroIcons;
import javax.swing.*;
import java.awt.*;

final class DialogTheme {
    private DialogTheme() { }
    static void apply(JDialog dialog) {
        Container content = dialog.getContentPane();
        style(content);
        vn.edu.donga.unischedule.ui.renderer.SemanticLabels.install(content);
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Color.WHITE);
        JLabel title = new JLabel(dialog.getTitle());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setIcon(HeroIcons.of("document-text", 24, AppConfig.PRIMARY));
        title.setIconTextGap(12);
        title.setBorder(BorderFactory.createEmptyBorder(22, 24, 8, 24));
        root.add(title, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(content);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        root.add(scroll, BorderLayout.CENTER);
        dialog.setContentPane(root);
        Dimension preferred = root.getPreferredSize();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setSize(Math.min(screen.width - 80, Math.max(dialog.getWidth(), preferred.width + 24)),
                Math.min(screen.height - 80, Math.max(dialog.getHeight(), preferred.height + 56)));
        dialog.setMinimumSize(new Dimension(460, 360));
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
    private static void style(Container parent) {
        if (parent instanceof JPanel) parent.setBackground(Color.WHITE);
        for (Component child : parent.getComponents()) {
            if (child instanceof JTextArea area) {
                area.setLineWrap(true); area.setWrapStyleWord(true);
                area.setMinimumSize(new Dimension(150, 72));
                area.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.decode("#E2E8F0")), BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            }
            if (child instanceof JScrollPane scroll && scroll.getViewport().getView() instanceof JTextArea)
                scroll.setMinimumSize(new Dimension(150, 80));
            if (child instanceof Container nested) style(nested);
        }
    }
}
