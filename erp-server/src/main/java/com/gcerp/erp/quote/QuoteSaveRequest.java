package com.gcerp.erp.quote;

import lombok.Data;

import java.util.List;

@Data
public class QuoteSaveRequest {
    private Long contractId;
    private Long assignmentId;
    private String serviceStaff;
    private String engineer;
    private String operator;
    private String quoteDesc;
    private Boolean submitted;
    private List<QuoteCalcRequest> items;
}
