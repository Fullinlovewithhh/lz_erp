package com.gcerp.erp.customer;

import com.gcerp.erp.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@CrossOrigin
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ApiResponse<Customer> create(@Valid @RequestBody CustomerCreateRequest req) {
        return ApiResponse.ok(customerService.createCustomer(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<Customer> update(@PathVariable Long id, @Valid @RequestBody CustomerCreateRequest req) {
        return ApiResponse.ok(customerService.updateCustomer(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ApiResponse.ok(true);
    }

    @GetMapping
    public ApiResponse<List<Customer>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(customerService.listCustomers(keyword));
    }

    @PostMapping("/followups")
    public ApiResponse<CustomerFollowup> createFollowup(@Valid @RequestBody CustomerFollowupRequest req) {
        return ApiResponse.ok(customerService.createFollowup(req));
    }

    @GetMapping("/{customerId}/followups")
    public ApiResponse<List<CustomerFollowup>> followups(@PathVariable Long customerId) {
        return ApiResponse.ok(customerService.listFollowups(customerId));
    }

    @PostMapping("/requirements")
    public ApiResponse<CustomerRequirement> createRequirement(@Valid @RequestBody CustomerRequirementRequest req) {
        return ApiResponse.ok(customerService.createRequirement(req));
    }

    @GetMapping("/{customerId}/requirements")
    public ApiResponse<List<CustomerRequirement>> requirements(@PathVariable Long customerId) {
        return ApiResponse.ok(customerService.listRequirements(customerId));
    }
}
