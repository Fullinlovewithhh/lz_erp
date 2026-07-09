package com.gcerp.erp.quote;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QuoteCalcRequest {
    private String productName;
    private String materialStructure;
    private String handleColor;
    private BigDecimal widthMm;
    private BigDecimal heightMm;
    private BigDecimal thicknessMm;
    private Integer quantity;
    private String hingeHole;
    private String processDesc;
    private String attachmentName;
    private String unit;
    private BigDecimal baseUnitPrice;
    private List<Long> selectedRuleIds;
    private List<QuoteRuleCreateRequest> customRules;
}

