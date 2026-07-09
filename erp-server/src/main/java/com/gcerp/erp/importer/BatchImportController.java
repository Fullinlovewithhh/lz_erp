package com.gcerp.erp.importer;

import com.gcerp.erp.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@CrossOrigin
public class BatchImportController {

    private final BatchImportService batchImportService;

    /**
     * 通用批量导入接口。
     * module 可选值：客户档案、产品资料、材料资料、工艺规则、价格规则、合同订单
     */
    @PostMapping("/{module}")
    public ApiResponse<BatchImportResult> batchImport(@PathVariable String module,
                                                      @Valid @RequestBody BatchImportRequest req) {
        return ApiResponse.ok(batchImportService.importRows(module, req));
    }

    @GetMapping("/modules")
    public ApiResponse<Map<String, Set<String>>> modules() {
        return ApiResponse.ok(batchImportService.getModuleFieldMap());
    }
}
