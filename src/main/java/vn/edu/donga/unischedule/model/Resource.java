package vn.edu.donga.unischedule.model;

import vn.edu.donga.unischedule.model.Enums.ResourceStatus;

public abstract class Resource {
    private Long id;
    private String code;
    private String name;
    private ResourceStatus status;

    protected Resource(Long id, String code, String name, ResourceStatus status) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    @Override public boolean equals(Object other) {
        if(this==other)return true;
        if(other==null || getClass()!=other.getClass())return false;
        return getId()!=null && getId().equals(((Resource)other).getId());
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}
