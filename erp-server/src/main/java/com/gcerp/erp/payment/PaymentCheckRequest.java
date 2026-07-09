package com.gcerp.erp.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentCheckRequest {
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "到账状态不能为空")
    private String payStatus;

    private String payChannel;

    @NotBlank(message = "财务人员不能为空")
    private String operator;

    // 结构化留痕数据（JSON 字符串）
    private String customFields;

    // 扩展文本留痕（审批人/待补金额/审批状态）
    private String customText1;
    private String customText2;
    private String customText3;
}
