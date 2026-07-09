package com.gcerp.erp.quote;

import com.gcerp.erp.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quote-details")
@RequiredArgsConstructor
@CrossOrigin
public class QuoteDetailController {

    private final QuoteDetailService quoteDetailService;

    @PostMapping("/calculate")
    public ApiResponse<QuoteCalcResult> calculate(@RequestBody QuoteCalcRequest req) {
        return ApiResponse.ok(quoteDetailService.calculate(req));
    }

    @PostMapping("/save")
    public ApiResponse<Map<String, Object>> save(@RequestBody QuoteSaveRequest req) {
        return ApiResponse.ok(quoteDetailService.save(req));
    }

    @PutMapping("/quotes/{quoteId}")
    public ApiResponse<Map<String, Object>> updateQuote(@PathVariable Long quoteId, @RequestBody QuoteSaveRequest req) {
        return ApiResponse.ok(quoteDetailService.updateQuote(quoteId, req));
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listByQuote(@RequestParam Long quoteId) {
        return ApiResponse.ok(quoteDetailService.listByQuoteId(quoteId));
    }

    @GetMapping("/quotes")
    public ApiResponse<List<Map<String, Object>>> listQuotes(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(quoteDetailService.listQuotes(keyword));
    }

    @GetMapping("/quotes/{quoteId}")
    public ApiResponse<Map<String, Object>> getQuote(@PathVariable Long quoteId) {
        return ApiResponse.ok(quoteDetailService.getQuote(quoteId));
    }

    @GetMapping("/quotes/{quoteId}/logs")
    public ApiResponse<List<Map<String, Object>>> quoteLogs(@PathVariable Long quoteId) {
        return ApiResponse.ok(quoteDetailService.listQuoteLogs(quoteId));
    }

    @DeleteMapping("/quotes/{quoteId}")
    public ApiResponse<Map<String, Object>> deleteQuote(@PathVariable Long quoteId,
                                                         @RequestHeader(value = "X-Operator", required = false) String operator) {
        return ApiResponse.ok(quoteDetailService.deleteQuote(quoteId, operator));
    }

    @PostMapping("/assignments")
    public ApiResponse<Map<String, Object>> createAssignment(@RequestBody QuoteAssignmentRequest req) {
        return ApiResponse.ok(quoteDetailService.createAssignment(req));
    }

    @PutMapping("/assignments/{assignmentId}")
    public ApiResponse<Map<String, Object>> updateAssignment(@PathVariable Long assignmentId, @RequestBody QuoteAssignmentRequest req) {
        return ApiResponse.ok(quoteDetailService.updateAssignment(assignmentId, req));
    }

    @GetMapping("/assignments")
    public ApiResponse<List<Map<String, Object>>> listAssignments(@RequestParam(required = false) Long contractId,
                                                                  @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(quoteDetailService.listAssignments(contractId, keyword));
    }
}
