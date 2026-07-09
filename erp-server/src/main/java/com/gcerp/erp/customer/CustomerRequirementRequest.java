package com.gcerp.erp.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomerRequirementRequest {
    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @NotBlank(message = "需求类型不能为空")
    private String requirementType;

    @NotBlank(message = "需求描述不能为空")
    private String requirementDesc;

    private String budgetRange;
    private String stylePreference;

    @NotBlank(message = "记录人不能为空")
    private String creator;
}
