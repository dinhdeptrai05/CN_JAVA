package vn.edu.donga.unischedule.ui.component;

import vn.edu.donga.unischedule.config.AppConfig;

import java.awt.Color;

public class PrimaryButton extends AppButton {
    public PrimaryButton(String text) {
        super(text, AppConfig.PRIMARY, Color.decode("#4E44E2"), Color.decode("#3E32D3"), Color.WHITE);
    }
}
