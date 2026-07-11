package com.gcerp.erp.customerorder;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerOrderRequest {
    private Long projectId;

    private Long customerId;

    @NotBlank(message = "customerOrderNo is required")
    private String customerOrderNo;

    private String customerOrderName;
    private String remark;
    private String status;
}
