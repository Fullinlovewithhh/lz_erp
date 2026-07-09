package com.gcerp.erp.auth;

import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {
    private String token;
    private String username;
    private String displayName;
    private String roleCode;
    private List<String> allowedModules;
}
