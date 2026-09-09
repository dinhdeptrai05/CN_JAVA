package vn.edu.donga.unischedule.ui.component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

public class EmptyStatePanel extends JPanel {
    public EmptyStatePanel(String title, String description) {
        setLayout(new BorderLayout(0, 8));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(28, 20, 28, 20));
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setForeground(Color.decode("#0F172A"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        JLabel descriptionLabel = new JLabel(description, JLabel.CENTER);
        descriptionLabel.setForeground(Color.decode("#64748B"));
        add(titleLabel, BorderLayout.CENTER);
        add(descriptionLabel, BorderLayout.SOUTH);
    }
}
