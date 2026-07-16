package com.gcerp.erp.customerorder;

import com.gcerp.erp.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/customer-orders")
@RequiredArgsConstructor
@CrossOrigin
public class CustomerOrderController {
    private final CustomerOrderService customerOrderService;
    private final CustomerOrderCadService customerOrderCadService;

    @PostMapping
    public ApiResponse<CustomerOrder> create(@Valid @RequestBody CustomerOrderRequest req) {
        return ApiResponse.ok(customerOrderService.create(req));
    }

    @GetMapping
    public ApiResponse<List<CustomerOrder>> list(@RequestParam(required = false) Long customerId,
                                                 @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(customerOrderService.list(customerId, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerOrder> detail(@PathVariable Long id) {
        return ApiResponse.ok(customerOrderService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerOrder> update(@PathVariable Long id, @Valid @RequestBody CustomerOrderRequest req) {
        return ApiResponse.ok(customerOrderService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        customerOrderService.delete(id);
        return ApiResponse.ok(true);
    }

    @PostMapping("/{id}/cad-files")
    public ApiResponse<Map<String, Object>> uploadCad(@PathVariable Long id,
                                                      @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(customerOrderCadService.upload(id, file));
    }

    @GetMapping("/{id}/cad-files")
    public ApiResponse<List<Map<String, Object>>> listCad(@PathVariable Long id) {
        return ApiResponse.ok(customerOrderCadService.list(id));
    }
}
