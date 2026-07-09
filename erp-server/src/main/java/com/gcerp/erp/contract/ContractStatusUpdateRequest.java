package com.gcerp.erp.contract;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContractStatusUpdateRequest {
    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    @NotBlank(message = "操作人不能为空")
    private String operator;

    private String remark;
    private Integer productionProgress;
}

