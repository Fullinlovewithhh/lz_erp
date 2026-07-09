package com.gcerp.erp.quote;

import com.gcerp.erp.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quote-rules")
@RequiredArgsConstructor
@CrossOrigin
public class QuoteRuleController {

    private final QuoteRuleService quoteRuleService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String category) {
        return ApiResponse.ok(quoteRuleService.list(keyword, category));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody QuoteRuleCreateRequest req,
                                                   @RequestHeader(value = "X-Operator", required = false) String operator) {
        return ApiResponse.ok(quoteRuleService.create(req, operator));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id,
                                                   @RequestBody QuoteRuleCreateRequest req,
                                                   @RequestHeader(value = "X-Operator", required = false) String operator) {
        return ApiResponse.ok(quoteRuleService.update(id, req, operator));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id,
                                      @RequestHeader(value = "X-Operator", required = false) String operator) {
        int affected = quoteRuleService.delete(id, operator);
        if (affected <= 0) return ApiResponse.fail("规则不存在");
        return ApiResponse.ok("删除成功");
    }
}

