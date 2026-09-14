package vn.edu.donga.unischedule.model;

import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.model.Enums.UserStatus;

import java.time.LocalDateTime;

public abstract class User {
    private Long id;
    private byte[] avatarData;
    public byte[] getAvatarData() { return avatarData == null ? null : avatarData.clone(); }
    private final java.beans.PropertyChangeSupport changes = new java.beans.PropertyChangeSupport(this);
    public void addAvatarListener(java.beans.PropertyChangeListener listener) { changes.addPropertyChangeListener("avatar",listener); }
    public void removeAvatarListener(java.beans.PropertyChangeListener listener) { changes.removePropertyChangeListener("avatar",listener); }
    public void setAvatarData(byte[] data) { byte[] old=avatarData;avatarData = data == null ? null : data.clone();changes.firePropertyChange("avatar",old,avatarData); }
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private UserStatus status;
    private LocalDateTime lastLogin;

    protected User(Long id, String username, String password, String fullName, String email, String phone,
                   Role role, UserStatus status) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.lastLogin = null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Override
    public String toString() {
        return fullName;
    }

    @Override public boolean equals(Object other) {
        if(this==other)return true;
        if(other==null || getClass()!=other.getClass())return false;
        return getId()!=null && getId().equals(((User)other).getId());
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}
