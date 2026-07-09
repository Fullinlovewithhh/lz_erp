package com.gcerp.erp.production;

import com.gcerp.erp.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productions")
@RequiredArgsConstructor
@CrossOrigin
public class ProductionController {

    private final ProductionService productionService;

    @PostMapping
    public ApiResponse<ProductionTask> create(@Valid @RequestBody ProductionCreateRequest req) {
        return ApiResponse.ok(productionService.createTask(req));
    }

    @PutMapping("/status")
    public ApiResponse<ProductionTask> updateStatus(@Valid @RequestBody ProductionStatusRequest req) {
        return ApiResponse.ok(productionService.updateTaskStatus(req));
    }

    @GetMapping
    public ApiResponse<List<ProductionTask>> list(@RequestParam Long contractId) {
        return ApiResponse.ok(productionService.listByContractId(contractId));
    }
}
