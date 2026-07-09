package com.gcerp.erp.audit;

import com.gcerp.erp.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
@CrossOrigin
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) String module,
                                                        @RequestParam(required = false) String operator,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String targetType,
                                                        @RequestParam(required = false) String targetId,
                                                        @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(operationLogService.list(module, operator, keyword, targetType, targetId, limit));
    }
}
