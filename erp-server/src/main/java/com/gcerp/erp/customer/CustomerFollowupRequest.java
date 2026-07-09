package com.gcerp.erp.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomerFollowupRequest {
    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @NotBlank(message = "跟进内容不能为空")
    private String content;

    private String nextPlan;

    @NotBlank(message = "记录人不能为空")
    private String creator;
}
