package com.gcerp.erp.customerorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomerOrderRequest {
    @NotNull(message = "projectId is required")
    private Long projectId;

    private Long customerId;

    @NotBlank(message = "customerOrderNo is required")
    private String customerOrderNo;

    private String customerOrderName;
    private String remark;
    private String status;
}
