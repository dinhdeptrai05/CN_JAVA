package vn.edu.donga.unischedule.ui.panel;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.model.ChangeRequest;
import vn.edu.donga.unischedule.model.Classroom;
import vn.edu.donga.unischedule.model.RoomAvailability;
import vn.edu.donga.unischedule.model.TimeSlot;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.Priority;
import vn.edu.donga.unischedule.model.Enums.RequestStatus;
import vn.edu.donga.unischedule.model.Enums.RequestType;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.model.Enums.RoomType;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.RoundedPanel;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.ui.component.UiTasks;
import vn.edu.donga.unischedule.ui.dialog.MakeupClassDialog;
import vn.edu.donga.unischedule.ui.model.GenericTableModel;
import vn.edu.donga.unischedule.util.DateUtils;
import vn.edu.donga.unischedule.util.Dialogs;
import vn.edu.donga.unischedule.util.TableUtils;
import vn.edu.donga.unischedule.validation.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class RoomSearchPanel extends JPanel implements Refreshable {
    private final AppControllers controllers;
    private final User user;
    private final JTextField dateField = new JTextField(LocalDate.now().toString());
    private final JComboBox<TimeSlot> slotBox = new JComboBox<>();
    private final JComboBox<String> buildingBox = new JComboBox<>();
    private final JComboBox<String> typeBox = new JComboBox<>();
    private final JTextField capacityField = new JTextField("40");
    private final JTextField equipmentField = new JTextField();
    private final JComboBox<String> viewBox = new JComboBox<>(new String[]{"Dạng card", "Dạng bảng"});
    private final JComboBox<String> statusBox = new JComboBox<>(new String[]{"Tất cả trạng thái", "Trống", "Có lịch học", "Bảo trì", "Không sử dụng được"});
    private final JPanel resultsPanel = new JPanel(new CardLayout());
    private final JPanel cardsPanel = new vn.edu.donga.unischedule.ui.component.ResponsiveCardGrid();
    private final GenericTableModel<RoomAvailability> tableModel;
    private final JTable table;
    private List<RoomAvailability> latestRows = List.of();
    private Map<Long, String> equipmentByRoom = Map.of();
    private final JLabel resultStatus = new JLabel(" ");

    private record SearchResult(List<RoomAvailability> rooms, Map<Long, String> equipment) { }

    public RoomSearchPanel(AppControllers controllers, User user) {
        this.controllers = controllers;
        this.user = user;
        controllers.catalog().getTimeSlots().forEach(slotBox::addItem);
        tableModel = new GenericTableModel<>(
                new String[]{"Mã", "Tên phòng", "Tòa", "Sức chứa", "Trạng thái", "Sĩ số đăng ký", "Lớp", "Thiết bị"},
                result -> result.room().getCode(),
                result -> result.room().getName(),
                result -> result.room().getBuilding(),
                result -> result.room().getCapacity(),
                result -> result.status().label(),
                result -> studentCount(result),
                RoomAvailability::classes,
                result -> equipmentSummary(result.room()));
        table = new JTable(tableModel);
        TableUtils.style(table);
        setLayout(new BorderLayout(0, 14));
        setBackground(AppConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        buildFilters();
        buildUi();
        refresh();
    }

    private void buildFilters() {
        buildingBox.addItem("Tất cả tòa nhà");
        controllers.rooms().buildings().forEach(buildingBox::addItem);
        typeBox.addItem("Tất cả loại phòng");
        for (RoomType type : RoomType.values()) {
            typeBox.addItem(type.getDisplayName());
        }
        viewBox.addActionListener(event -> switchView());
    }

    private void buildUi() {
        JPanel top = new JPanel(new BorderLayout(12, 12));
        top.setOpaque(false);
        JPanel filters = new JPanel(new GridLayout(2, 5, 10, 10));
        filters.setOpaque(false);
        filters.add(labeled("Ngày sử dụng", dateField));
        filters.add(labeled("Ca học", slotBox));
        filters.add(labeled("Tòa nhà", buildingBox));
        filters.add(labeled("Loại phòng", typeBox));
        filters.add(labeled("Sức chứa tối thiểu", capacityField));
        equipmentField.putClientProperty("JTextField.placeholderText", "Thiết bị cần có");
        filters.add(labeled("Thiết bị", equipmentField));
        filters.add(labeled("Trạng thái", statusBox));
        filters.add(labeled("Hiển thị", viewBox));
        PrimaryButton search = new PrimaryButton("Tìm kiếm");
        search.addActionListener(event -> searchAsync());
        filters.add(search);
        top.add(filters, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        resultStatus.setForeground(AppConfig.MUTED);
        actions.add(resultStatus);
        JLabel countNote = new JLabel("Sĩ số theo lịch, không phải số người có mặt thực tế.");
        countNote.setForeground(AppConfig.MUTED);
        actions.add(countNote);
        SecondaryButton reset = new SecondaryButton("Đặt lại");
        reset.addActionListener(event -> resetFilters());
        actions.add(reset);
        if (user.getRole() == Role.LECTURER) {
            PrimaryButton requestRoom = new PrimaryButton("Đăng ký học bù");
            requestRoom.addActionListener(event -> createRequestForSelected());
            actions.add(requestRoom);
        }
        top.add(actions, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        resultsPanel.add(new JScrollPane(cardsPanel), "cards");
        resultsPanel.add(new JScrollPane(table), "table");
        add(resultsPanel, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim(), DateUtils.INPUT_FORMAT);
            int minCapacity = capacityField.getText().isBlank() ? 0 : Integer.parseInt(capacityField.getText().trim());
            showRows(searchRooms(date, (TimeSlot) slotBox.getSelectedItem(),
                    buildingBox.getSelectedIndex() == 0 ? null : (String) buildingBox.getSelectedItem(),
                    selectedRoomType(), minCapacity, equipmentField.getText(),
                    statusBox.getSelectedIndex() == 0 ? null : (String) statusBox.getSelectedItem()));
        } catch (NumberFormatException ex) {
            Dialogs.error(this, "Sức chứa tối thiểu phải là số nguyên.");
        } catch (java.time.format.DateTimeParseException ex) {
            Dialogs.error(this, "Ngày tra cứu phải nhập theo định dạng yyyy-MM-dd.");
        } catch (Exception ex) {
            Dialogs.error(this, UiTasks.message(ex));
        }
    }

    private void searchAsync() {
        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim(), DateUtils.INPUT_FORMAT);
            int minCapacity = capacityField.getText().isBlank() ? 0 : Integer.parseInt(capacityField.getText().trim());
            TimeSlot slot = (TimeSlot) slotBox.getSelectedItem();
            String building = buildingBox.getSelectedIndex() == 0 ? null : (String) buildingBox.getSelectedItem();
            RoomType type = selectedRoomType();
            String equipment = equipmentField.getText();
            String status = statusBox.getSelectedIndex() == 0 ? null : (String) statusBox.getSelectedItem();
            UiTasks.run(this, "Đang tra cứu phòng…", () -> searchRooms(
                    date, slot, building, type, minCapacity, equipment, status),
                    this::showRows, error -> Dialogs.error(this, UiTasks.message(error)));
        } catch (NumberFormatException ex) {
            Dialogs.error(this, "Sức chứa tối thiểu phải là số nguyên.");
        } catch (java.time.format.DateTimeParseException ex) {
            Dialogs.error(this, "Ngày tra cứu phải nhập theo định dạng yyyy-MM-dd.");
        }
    }

    private SearchResult searchRooms(LocalDate date, TimeSlot slot, String building, RoomType type,
                                     int minCapacity, String equipment, String status) {
        List<RoomAvailability> rows = controllers.rooms().searchRoomAvailability(date, slot, building, type,
                        minCapacity, equipment).stream()
                .filter(result -> status == null || result.status().label().equals(status)).toList();
        Map<Long, String> details = new HashMap<>();
        for (RoomAvailability result : rows)
            details.put(result.room().getId(), controllers.rooms().equipmentDetails(result.room()));
        return new SearchResult(rows, details);
    }

    private void showRows(SearchResult result) {
        latestRows = result.rooms();
        equipmentByRoom = result.equipment();
        tableModel.setRows(latestRows);
        rebuildCards();
        switchView();
        resultStatus.setText("Đã tìm thấy " + latestRows.size() + " phòng.");
    }

    private void switchView() {
        ((CardLayout) resultsPanel.getLayout()).show(resultsPanel, viewBox.getSelectedIndex() == 0 ? "cards" : "table");
    }

    private void rebuildCards() {
        cardsPanel.removeAll();
        cardsPanel.setBackground(AppConfig.BACKGROUND);
        for (RoomAvailability result : latestRows) {
            Classroom room = result.room();
            RoundedPanel card = new RoundedPanel(8, Color.WHITE);
            card.setLayout(new BorderLayout(0, 10));
            card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            JLabel title = new JLabel("<html><div style='width:180px'><b style='font-size:12px'>" + room.getCode() + " - " + room.getName()
                    + "</b><br><span style='color:#64748B'>" + room.getBuilding() + ", tầng " + room.getFloor()
                    + " | " + room.getCapacity() + " chỗ</span></div></html>");
            title.setIcon(vn.edu.donga.unischedule.ui.component.HeroIcons.of("building-office-2", 32, AppConfig.PRIMARY));
            title.setIconTextGap(10);
            JLabel body = new JLabel("<html><div style='width:210px'>Loại phòng: " + room.getRoomType().getDisplayName()
                    + "<br>Trạng thái: " + result.status().label()
                    + "<br>Sĩ số đăng ký: " + studentCount(result)
                    + (result.classes().isBlank() ? "" : "<br>Lớp: " + result.classes())
                    + "<br>Thiết bị: " + equipmentSummary(room) + "</div></html>");
            card.add(title, BorderLayout.NORTH);
            card.add(body, BorderLayout.CENTER);
            if (user.getRole() == Role.LECTURER && result.status() == RoomAvailability.Status.FREE) {
                SecondaryButton request = new SecondaryButton("Học bù tại phòng này");
                request.addActionListener(event -> createRequest(room));
                card.add(request, BorderLayout.SOUTH);
            }
            cardsPanel.add(card);
        }
        if (latestRows.isEmpty()) {
            cardsPanel.add(new vn.edu.donga.unischedule.ui.component.EmptyStatePanel("Không có phòng phù hợp", "Thử đổi ca học, trạng thái hoặc giảm sức chứa tối thiểu."));
        }
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private RoomType selectedRoomType() {
        String selected = (String) typeBox.getSelectedItem();
        if (selected == null || selected.startsWith("Tất cả")) {
            return null;
        }
        for (RoomType type : RoomType.values()) {
            if (type.getDisplayName().equals(selected)) {
                return type;
            }
        }
        return null;
    }

    private JPanel labeled(String title, javax.swing.JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 6)); panel.setOpaque(false);
        JLabel label = new JLabel(title); label.setLabelFor(field);
        label.setFont(label.getFont().deriveFont(11f)); label.setForeground(AppConfig.MUTED);
        panel.add(label, BorderLayout.NORTH); panel.add(field, BorderLayout.CENTER); return panel;
    }

    private String equipmentSummary(Classroom room) {
        return equipmentByRoom.getOrDefault(room.getId(), "Chưa khai báo");
    }

    private String studentCount(RoomAvailability result) {
        return result.registeredStudents() == null ? "Không rõ" : result.registeredStudents().toString();
    }

    private Classroom selectedRoom() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            RoomAvailability result = tableModel.getRowAt(table.convertRowIndexToModel(row));
            if (result.status() == RoomAvailability.Status.FREE) return result.room();
            throw new ValidationException("Phòng đã chọn không trống trong ngày và ca này.");
        }
        throw new ValidationException("Chọn một phòng trống trong bảng hoặc dùng nút trên thẻ phòng.");
    }

    private void createRequestForSelected() {
        try {
            createRequest(selectedRoom());
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void createRequest(Classroom room) {
        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim(), DateUtils.INPUT_FORMAT);
            boolean free = controllers.rooms().searchRoomAvailability(date, (TimeSlot) slotBox.getSelectedItem(),
                    null, null, 0, "").stream().anyMatch(result -> result.room().getId().equals(room.getId())
                    && result.status() == RoomAvailability.Status.FREE);
            if (!free) throw new ValidationException("Phòng không còn trống trong ngày và ca đã chọn.");
            if (MakeupClassDialog.showDialog(this, controllers, user, null, room, date,
                    (TimeSlot) slotBox.getSelectedItem()))
                Dialogs.success(this, "Đã gửi yêu cầu học bù tại phòng " + room.getCode() + " cho phòng đào tạo.");
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        } catch (Exception ex) {
            Dialogs.error(this, "Không thể tạo yêu cầu, vui lòng kiểm tra ngày và ca học.");
        }
    }

    private void resetFilters() {
        dateField.setText(LocalDate.now().toString());
        buildingBox.setSelectedIndex(0);
        typeBox.setSelectedIndex(0);
        statusBox.setSelectedIndex(0);
        capacityField.setText("40");
        equipmentField.setText("");
        searchAsync();
    }
}
