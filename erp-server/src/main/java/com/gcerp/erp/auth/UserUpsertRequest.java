package com.gcerp.erp.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserUpsertRequest {
    @NotBlank(message = "账号不能为空")
    private String username;
    @NotBlank(message = "显示名不能为空")
    private String displayName;
    @NotBlank(message = "角色不能为空")
    private String roleCode;
    private Integer enabled = 1;
    private String password;
}
