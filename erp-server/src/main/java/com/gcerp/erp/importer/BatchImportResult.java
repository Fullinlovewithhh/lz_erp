package com.gcerp.erp.importer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BatchImportResult {
    private Integer total;
    private Integer success;
    private Integer failed;
}
