package vn.edu.donga.unischedule.ui.dialog;

import vn.edu.donga.unischedule.model.Classroom;
import vn.edu.donga.unischedule.model.Equipment;
import vn.edu.donga.unischedule.model.Enums.ResourceStatus;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.util.Dialogs;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Optional;

public class EquipmentDialog extends JDialog {
    private final JTextField codeField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField categoryField = new JTextField();
    private final JTextField quantityField = new JTextField("1");
    private final JTextField conditionField = new JTextField("Tốt");
    private final JComboBox<ResourceStatus> statusBox = new JComboBox<>(ResourceStatus.values());
    private final Classroom room;
    private final Equipment editing;
    private Equipment result;

    public EquipmentDialog(JFrame owner, Classroom room, Equipment editing) {
        super(owner, editing == null ? "Thêm thiết bị" : "Sửa thiết bị", true);
        this.room = room;
        this.editing = editing;
        buildUi();
        fill(editing);
        setSize(480, 390);
        DialogTheme.apply(this);
        setLocationRelativeTo(owner);
    }

    public static Optional<Equipment> showDialog(Component parent, Classroom room, Equipment editing) {
        JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(parent);
        EquipmentDialog dialog = new EquipmentDialog(owner, room, editing);
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.result);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        setContentPane(root);
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 0, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridy = 0;
        addRow(form, gbc, "Mã thiết bị", codeField);
        addRow(form, gbc, "Tên thiết bị", nameField);
        addRow(form, gbc, "Nhóm thiết bị", categoryField);
        addRow(form, gbc, "Số lượng", quantityField);
        addRow(form, gbc, "Tình trạng", conditionField);
        addRow(form, gbc, "Trạng thái", statusBox);
        root.add(form, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        SecondaryButton cancel = new SecondaryButton("Hủy");
        PrimaryButton save = new PrimaryButton("Lưu");
        cancel.addActionListener(event -> dispose());
        save.addActionListener(event -> save());
        actions.add(cancel);
        actions.add(save);
        root.add(actions, BorderLayout.SOUTH);
    }

    private void addRow(JPanel form, GridBagConstraints gbc, String label, Component field) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(field, gbc);
        gbc.gridy++;
    }

    private void fill(Equipment equipment) {
        if (equipment == null) {
            return;
        }
        codeField.setText(equipment.getCode());
        nameField.setText(equipment.getName());
        categoryField.setText(equipment.getCategory());
        quantityField.setText(String.valueOf(equipment.getQuantity()));
        conditionField.setText(equipment.getCondition());
        statusBox.setSelectedItem(equipment.getStatus());
    }

    private void save() {
        try {
            int quantity = Integer.parseInt(quantityField.getText().trim());
            result = new Equipment(editing == null ? null : editing.getId(), codeField.getText().trim(),
                    nameField.getText().trim(), categoryField.getText().trim(), quantity,
                    conditionField.getText().trim(), room, (ResourceStatus) statusBox.getSelectedItem());
            dispose();
        } catch (NumberFormatException ex) {
            Dialogs.error(this, "Số lượng phải là số nguyên dương.");
        }
    }
}
