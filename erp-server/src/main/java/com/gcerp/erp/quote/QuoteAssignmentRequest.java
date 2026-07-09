package com.gcerp.erp.quote;

import lombok.Data;

@Data
public class QuoteAssignmentRequest {
    private Long contractId;
    private String serviceStaff;
    private String engineer;
    private String status;
    private String remark;
}

