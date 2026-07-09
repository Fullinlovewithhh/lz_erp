package com.gcerp.erp.importer;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BatchImportRequest {
    // 每一行数据用“字段名:字段值”表示。
    @NotEmpty(message = "导入数据不能为空")
    private List<Map<String, Object>> rows;

    // 支持“忽略重复”模式，导入时遇到唯一键冲突会跳过。
    private boolean ignoreDuplicate = true;
}
