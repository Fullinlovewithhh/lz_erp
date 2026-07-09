package com.gcerp.erp.quote;

import com.gcerp.erp.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
@CrossOrigin
public class QuoteController {

    private final QuoteService quoteService;

    @PostMapping
    public ApiResponse<Quotation> create(@Valid @RequestBody QuoteCreateRequest req) {
        return ApiResponse.ok(quoteService.createQuote(req));
    }
}
