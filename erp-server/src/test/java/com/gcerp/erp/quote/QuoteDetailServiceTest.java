package com.gcerp.erp.quote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcerp.erp.audit.OperationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class QuoteDetailServiceTest {
    private QuoteDetailService service;

    @BeforeEach
    void setUp() {
        service = new QuoteDetailService(mock(JdbcTemplate.class), new ObjectMapper(), mock(OperationLogService.class));
    }

    @Test
    void calculatesAreaPriceWithMinimumAndOneOffCharge() {
        QuoteCalcRequest request = baseRequest("AREA", "100", 2);
        request.setWidthMm(new BigDecimal("500"));
        request.setHeightMm(new BigDecimal("1000"));
        request.setMinBillQuantity(new BigDecimal("1.5"));
        request.setCustomRules(List.of(rule("FIXED_ONCE", "80")));

        QuoteCalcResult result = service.calculate(request);

        assertThat(result.getAreaM2()).isEqualByComparingTo("1.0000");
        assertThat(result.getBillingQuantity()).isEqualByComparingTo("1.5");
        assertThat(result.getBaseAmount()).isEqualByComparingTo("150.00");
        assertThat(result.getAmount()).isEqualByComparingTo("230.00");
    }

    @Test
    void calculatesLengthPriceAndPerMeterCraft() {
        QuoteCalcRequest request = baseRequest("LENGTH", "30", 3);
        request.setLengthMm(new BigDecimal("2000"));
        request.setCustomRules(List.of(rule("FIXED_PER_METER", "5")));

        QuoteCalcResult result = service.calculate(request);

        assertThat(result.getBillingQuantity()).isEqualByComparingTo("6.0000");
        assertThat(result.getBaseAmount()).isEqualByComparingTo("180.00");
        assertThat(result.getAmount()).isEqualByComparingTo("210.00");
    }

    @Test
    void calculatesCountPrice() {
        QuoteCalcRequest request = baseRequest("COUNT", "12.5", 4);

        QuoteCalcResult result = service.calculate(request);

        assertThat(result.getBillingQuantity()).isEqualByComparingTo("4");
        assertThat(result.getAmount()).isEqualByComparingTo("50.00");
    }

    private QuoteCalcRequest baseRequest(String mode, String price, int quantity) {
        QuoteCalcRequest request = new QuoteCalcRequest();
        request.setPricingMode(mode);
        request.setBaseUnitPrice(new BigDecimal(price));
        request.setQuantity(quantity);
        request.setSelectedRuleIds(List.of());
        request.setCustomRules(List.of());
        return request;
    }

    private QuoteRuleCreateRequest rule(String mode, String value) {
        QuoteRuleCreateRequest rule = new QuoteRuleCreateRequest();
        rule.setRuleName("测试加价");
        rule.setAdjustMode(mode);
        rule.setAdjustValue(new BigDecimal(value));
        return rule;
    }
}
