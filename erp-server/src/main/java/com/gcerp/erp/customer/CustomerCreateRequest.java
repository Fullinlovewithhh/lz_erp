package com.gcerp.erp.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CustomerCreateRequest {
    @NotBlank(message = "客户名称不能为空")
    private String customerName;
    private String phone;
    private String address;
    private String level;
    private String owner;
    private BigDecimal defaultDiscountRate;
    private String customFields;
}
