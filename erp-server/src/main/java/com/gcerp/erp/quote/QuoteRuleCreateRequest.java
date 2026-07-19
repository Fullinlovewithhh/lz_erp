package com.gcerp.erp.quote;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuoteRuleCreateRequest {
    private Long sourceRuleId;
    private String ruleCategory;
    private String ruleName;
    private String chargeReason;
    private String adjustMode;
    private BigDecimal adjustValue;
    private String unitDesc;
    private BigDecimal minAreaM2;
    private BigDecimal minCharge;
    private BigDecimal maxCharge;
    private BigDecimal ruleQuantity;
    private Integer enabled;
    private Boolean discountEligible;
    private String remark;
}
