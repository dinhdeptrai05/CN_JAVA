package vn.edu.donga.unischedule.ui.component;

import java.awt.Color;

public class SecondaryButton extends AppButton {
    public SecondaryButton(String text) {
        super(text, Color.WHITE, Color.decode("#F1F5F9"), Color.decode("#E2E8F0"), Color.decode("#64748B"));
        setBorderPainted(true);
    }
}
