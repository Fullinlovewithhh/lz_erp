package com.gcerp.erp.customerorder;

import com.gcerp.erp.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-orders")
@RequiredArgsConstructor
@CrossOrigin
public class CustomerOrderController {
    private final CustomerOrderService customerOrderService;

    @PostMapping
    public ApiResponse<CustomerOrder> create(@Valid @RequestBody CustomerOrderRequest req) {
        return ApiResponse.ok(customerOrderService.create(req));
    }

    @GetMapping
    public ApiResponse<List<CustomerOrder>> list(@RequestParam(required = false) Long projectId,
                                                 @RequestParam(required = false) Long customerId,
                                                 @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(customerOrderService.list(projectId, customerId, keyword));
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
}
