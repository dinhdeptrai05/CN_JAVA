package vn.edu.donga.unischedule.ui.component;

import vn.edu.donga.unischedule.config.AppConfig;

import java.awt.Color;

public class DangerButton extends AppButton {
    public DangerButton(String text) {
        super(text, AppConfig.DANGER, Color.decode("#B91C1C"), Color.decode("#991B1B"), Color.WHITE);
    }
}
