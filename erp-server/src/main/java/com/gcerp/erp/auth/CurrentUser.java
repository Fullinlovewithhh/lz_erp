package com.gcerp.erp.auth;

import lombok.Data;

import java.util.List;

@Data
public class CurrentUser {
    private Long id;
    private String username;
    private String displayName;
    private String roleCode;
    private List<String> allowedModules;
}
