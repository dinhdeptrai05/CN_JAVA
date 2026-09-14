package vn.edu.donga.unischedule.model;

public class Department {
    private Long id;
    private String code;
    private String name;

    public Department(Long id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override public boolean equals(Object other) {
        if(this==other)return true;
        if(other==null || getClass()!=other.getClass())return false;
        return getId()!=null && getId().equals(((Department)other).getId());
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}
