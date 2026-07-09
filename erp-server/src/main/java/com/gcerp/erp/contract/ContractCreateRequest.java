package com.gcerp.erp.contract;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContractCreateRequest {
    private Long customerId;
    private Long projectId;
    private Long customerOrderId;
    private String factoryOrderPrefix;
    private String factoryOrderNo;

    @NotBlank(message = "客户完整单号不能为空")
    private String customerOrderNo;

    @NotBlank(message = "客户姓名不能为空")
    private String customerName;

    private String customerPhone;
    private String customerAddress;
    private String demandDesc;
    private String customFields;
}
