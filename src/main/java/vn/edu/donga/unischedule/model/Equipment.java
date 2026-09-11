package vn.edu.donga.unischedule.model;

import vn.edu.donga.unischedule.model.Enums.ResourceStatus;

public class Equipment extends Resource {
    private String category;
    private int quantity;
    private String condition;
    private Classroom classroom;

    public Equipment(Long id, String code, String name, String category, int quantity, String condition,
                     Classroom classroom, ResourceStatus status) {
        super(id, code, name, status);
        this.category = category;
        this.quantity = quantity;
        this.condition = condition;
        this.classroom = classroom;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Classroom getClassroom() {
        return classroom;
    }

    public void setClassroom(Classroom classroom) {
        this.classroom = classroom;
    }
}
