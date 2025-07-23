package kynux.cloud.discordPlus.data;


public class PermissionSyncData {
    
    private final String permission;
    private final String roleId;
    private final String roleName;
    private final int priority;
    
    public PermissionSyncData(String permission, String roleId, String roleName, int priority) {
        this.permission = permission;
        this.roleId = roleId;
        this.roleName = roleName;
        this.priority = priority;
    }
    
    public String getPermission() {
        return permission;
    }
    
    public String getRoleId() {
        return roleId;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public int getPriority() {
        return priority;
    }
    
    @Override
    public String toString() {
        return String.format("PermissionSyncData{permission='%s', roleId='%s', roleName='%s', priority=%d}", 
                permission, roleId, roleName, priority);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PermissionSyncData that = (PermissionSyncData) obj;
        return permission.equals(that.permission) && roleId.equals(that.roleId);
    }
    
    @Override
    public int hashCode() {
        return permission.hashCode() + roleId.hashCode();
    }
}
