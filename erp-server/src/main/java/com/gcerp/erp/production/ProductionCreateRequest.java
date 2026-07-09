package com.gcerp.erp.production;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductionCreateRequest {
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    @NotBlank(message = "排产计划不能为空")
    private String processPlan;

    @NotBlank(message = "下单员不能为空")
    private String operator;

    private String assignee;
}
