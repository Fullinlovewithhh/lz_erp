package com.gcerp.erp.payment;

import com.gcerp.erp.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/check")
    public ApiResponse<PaymentRecord> check(@Valid @RequestBody PaymentCheckRequest req) {
        return ApiResponse.ok(paymentService.checkPayment(req));
    }
}
