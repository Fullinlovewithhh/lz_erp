package com.gcerp.erp.quote;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class QuoteCalcResult {
    private BigDecimal areaM2;
    private BigDecimal billingQuantity;
    private BigDecimal baseAmount;
    private BigDecimal specialAdjustTotal;
    private BigDecimal finalUnitPrice;
    private BigDecimal amount;
    private List<Map<String, Object>> appliedRules = new ArrayList<>();
}

