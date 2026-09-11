package vn.edu.donga.unischedule.ui.component;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

abstract class AppButton extends JButton {
    private final Color base;
    private final Color hover;
    private final Color pressed;

    AppButton(String text, Color base, Color hover, Color pressed, Color foreground) {
        super(text);
        this.base = base;
        this.hover = hover;
        this.pressed = pressed;
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setIcon(HeroIcons.of(HeroIcons.action(text), 16, foreground));
        setIconTextGap(8);
        setToolTipText(text);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(getFont().deriveFont(Font.PLAIN, 12f));
        setBackground(base);
        setForeground(foreground);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    setBackground(hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (isEnabled()) {
                    setBackground(base);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    setBackground(pressed);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isEnabled()) {
                    setBackground(hover);
                }
            }
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setBackground(enabled ? base : Color.decode("#CBD5E1"));
    }
}
