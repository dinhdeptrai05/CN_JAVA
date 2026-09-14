package vn.edu.donga.unischedule.model;

import vn.edu.donga.unischedule.model.Enums.RoomType;

public class Course {
    private Long id;
    private String code;
    private String name;
    private int credits;
    private Department department;
    private RoomType requiredRoomType;

    public Course(Long id, String code, String name, int credits, Department department, RoomType requiredRoomType) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.credits = credits;
        this.department = department;
        this.requiredRoomType = requiredRoomType;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getCredits() {
        return credits;
    }

    public Department getDepartment() {
        return department;
    }

    public RoomType getRequiredRoomType() {
        return requiredRoomType;
    }

    @Override
    public String toString() {
        return code + " - " + name;
    }

    @Override public boolean equals(Object other) {
        if(this==other)return true;
        if(other==null || getClass()!=other.getClass())return false;
        return getId()!=null && getId().equals(((Course)other).getId());
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}
