package com.gcerp.erp.quote;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuoteCreateRequest {
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    @NotNull(message = "报价金额不能为空")
    @DecimalMin(value = "0.01", message = "报价金额必须大于0")
    private BigDecimal totalAmount;

    private String quoteDesc;

    @NotBlank(message = "操作人不能为空")
    private String operator;
}
