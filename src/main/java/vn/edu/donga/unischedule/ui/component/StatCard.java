package vn.edu.donga.unischedule.ui.component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

public class StatCard extends RoundedPanel {
    private final JLabel valueLabel = new JLabel();
    private final JLabel descLabel = new JLabel();

    public StatCard(String icon, String title, String value, String description, Color accent) {
        super(8, Color.WHITE);
        setLayout(new BorderLayout(12, 8));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 18));
        String symbol = switch (icon) {
            case "PH", "TR", "SD", "BT" -> "building-office-2";
            case "XD" -> "exclamation-triangle";
            case "YC" -> "clock";
            case "TB" -> "bell";
            case "ND" -> "users";
            default -> "calendar-days";
        };
        JLabel iconLabel = new JLabel(HeroIcons.of(symbol, 22, accent));
        iconLabel.setOpaque(false);
        iconLabel.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
        iconLabel.setForeground(accent);
        iconLabel.setHorizontalAlignment(JLabel.CENTER);
        iconLabel.setFont(iconLabel.getFont().deriveFont(Font.BOLD, 18f));
        setPreferredSize(new java.awt.Dimension(210, 148));

        javax.swing.JPanel body = new javax.swing.JPanel();
        body.setOpaque(false);
        body.setLayout(new BorderLayout(2, 4));
        JLabel titleLabel = new JLabel("<html>" + title.toUpperCase() + "</html>");
        titleLabel.setForeground(Color.decode("#64748B"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));
        valueLabel.setText(value);
        valueLabel.setForeground(Color.decode("#0F172A"));
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 30f));
        descLabel.setText(description);
        descLabel.setForeground(Color.decode("#64748B"));
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 12f));
        javax.swing.JPanel heading = new javax.swing.JPanel(new BorderLayout(8, 0));
        heading.setOpaque(false);
        heading.add(titleLabel, BorderLayout.CENTER);
        heading.add(iconLabel, BorderLayout.EAST);
        body.add(heading, BorderLayout.NORTH);
        body.add(valueLabel, BorderLayout.CENTER);
        body.add(descLabel, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setDescription(String description) {
        descLabel.setText(description);
    }
}
