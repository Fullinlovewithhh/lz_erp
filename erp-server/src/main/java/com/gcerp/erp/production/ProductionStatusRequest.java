package com.gcerp.erp.production;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductionStatusRequest {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotBlank(message = "目标状态不能为空")
    private String taskStatus;

    @NotBlank(message = "操作人不能为空")
    private String operator;

    private Integer progressPercent;
}

