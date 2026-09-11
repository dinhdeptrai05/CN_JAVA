package vn.edu.donga.unischedule.ui.dialog;

import vn.edu.donga.unischedule.model.Classroom;
import vn.edu.donga.unischedule.model.Enums.RoomStatus;
import vn.edu.donga.unischedule.model.Enums.RoomType;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.util.Dialogs;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Optional;

public class RoomFormDialog extends JDialog {
    private final JTextField codeField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField buildingField = new JTextField();
    private final JTextField floorField = new JTextField();
    private final JTextField capacityField = new JTextField();
    private final JComboBox<RoomType> typeBox = new JComboBox<>(RoomType.values());
    private final JComboBox<RoomStatus> statusBox = new JComboBox<>(RoomStatus.values());
    private final JTextArea descriptionArea = new JTextArea(3, 24);
    private Classroom result;
    private final Classroom editing;

    public RoomFormDialog(JFrame owner, Classroom editing) {
        super(owner, editing == null ? "Thêm phòng học" : "Sửa phòng học", true);
        this.editing = editing;
        buildUi();
        fill(editing);
        setSize(520, 520);
        DialogTheme.apply(this);
        setLocationRelativeTo(owner);
    }

    public static Optional<Classroom> showDialog(Component parent, Classroom editing) {
        JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(parent);
        RoomFormDialog dialog = new RoomFormDialog(owner, editing);
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
        addRow(form, gbc, "Mã phòng", codeField);
        addRow(form, gbc, "Tên phòng", nameField);
        addRow(form, gbc, "Tòa nhà", buildingField);
        addRow(form, gbc, "Tầng", floorField);
        addRow(form, gbc, "Sức chứa", capacityField);
        addRow(form, gbc, "Loại phòng", typeBox);
        addRow(form, gbc, "Trạng thái", statusBox);
        addRow(form, gbc, "Mô tả", descriptionArea);
        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        SecondaryButton cancel = new SecondaryButton("Hủy");
        PrimaryButton save = new PrimaryButton("Lưu");
        cancel.addActionListener(event -> dispose());
        save.addActionListener(event -> save());
        actions.add(cancel);
        actions.add(save);
        root.add(actions, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);
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

    private void fill(Classroom room) {
        if (room == null) {
            buildingField.setText("Tòa A");
            floorField.setText("1");
            capacityField.setText("45");
            return;
        }
        codeField.setText(room.getCode());
        nameField.setText(room.getName());
        buildingField.setText(room.getBuilding());
        floorField.setText(String.valueOf(room.getFloor()));
        capacityField.setText(String.valueOf(room.getCapacity()));
        typeBox.setSelectedItem(room.getRoomType());
        statusBox.setSelectedItem(room.getRoomStatus());
        descriptionArea.setText(room.getDescription());
    }

    private void save() {
        try {
            result = vn.edu.donga.unischedule.controller.FormController.room(editing, codeField.getText(),
                    nameField.getText(), buildingField.getText(), floorField.getText(), capacityField.getText(),
                    (RoomType) typeBox.getSelectedItem(), (RoomStatus) statusBox.getSelectedItem(),
                    descriptionArea.getText().trim());
            dispose();
        } catch (NumberFormatException ex) {
            Dialogs.error(this, "Tầng và sức chứa phải là số nguyên.");
        }
    }
}
