package com.gcerp.erp.project;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectCreateRequest {
    @NotNull(message = "客户不能为空")
    private Long customerId;

    private String projectName;

    private String projectAddress;
    private String projectManager;
    private String remark;
}
