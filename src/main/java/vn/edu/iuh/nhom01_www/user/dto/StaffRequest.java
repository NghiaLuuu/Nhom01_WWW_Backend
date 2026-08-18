package vn.edu.iuh.nhom01_www.user.dto;

import lombok.Data;

@Data
public class StaffRequest {
    private String email;
    private String fullName;
    private String password;
    private String staffCode;
    private String position;
    private String workStation;
    private Long roleId;
    private java.util.List<String> permissions;
}
