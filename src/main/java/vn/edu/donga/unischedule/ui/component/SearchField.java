package vn.edu.donga.unischedule.ui.component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;

public class SearchField extends JPanel {
    private final JTextField textField = new JTextField();

    public SearchField(String placeholder) {
        setLayout(new BorderLayout(8, 0));
        setBackground(Color.decode("#F1F5F9"));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel icon = new JLabel(HeroIcons.of("magnifying-glass", 17, Color.decode("#94A3B8")));
        icon.setForeground(Color.decode("#64748B"));
        textField.setBorder(BorderFactory.createEmptyBorder());
        textField.setBackground(getBackground());
        textField.putClientProperty("JTextField.placeholderText", placeholder);
        add(icon, BorderLayout.WEST);
        add(textField, BorderLayout.CENTER);
    }

    public JTextField getTextField() {
        return textField;
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String value) {
        textField.setText(value);
    }
}
