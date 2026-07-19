package com.gcerp.erp.orderflow;

import com.gcerp.erp.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.gcerp.erp.quote.QuoteCalcRequest;

@RestController
@RequestMapping("/api/order-flow")
@RequiredArgsConstructor
@CrossOrigin
public class OrderFlowController {
    private final OrderFlowService service;

    @GetMapping("/production-lines")
    public ApiResponse<List<Map<String, Object>>> productionLines(@RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(service.listProductionLines(enabled));
    }

    @PostMapping("/production-lines")
    public ApiResponse<Map<String, Object>> createProductionLine(@RequestBody ProductionLineRequest req) {
        return ApiResponse.ok(service.createProductionLine(req));
    }

    @PutMapping("/production-lines/{id}")
    public ApiResponse<Map<String, Object>> updateProductionLine(@PathVariable Long id,
                                                                 @RequestBody ProductionLineRequest req) {
        return ApiResponse.ok(service.updateProductionLine(id, req));
    }

    @GetMapping("/receiving-accounts")
    public ApiResponse<List<Map<String, Object>>> receivingAccounts(@RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(service.listReceivingAccounts(enabled));
    }

    @PostMapping("/receiving-accounts")
    public ApiResponse<Map<String, Object>> createReceivingAccount(@RequestBody ReceivingAccountRequest req) {
        return ApiResponse.ok(service.createReceivingAccount(req));
    }

    @PutMapping("/receiving-accounts/{id}")
    public ApiResponse<Map<String, Object>> updateReceivingAccount(@PathVariable Long id,
                                                                   @RequestBody ReceivingAccountRequest req) {
        return ApiResponse.ok(service.updateReceivingAccount(id, req));
    }

    @GetMapping("/hardware-items")
    public ApiResponse<List<Map<String, Object>>> hardwareItems(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.listHardwareItems(keyword));
    }

    @PostMapping("/hardware-items")
    public ApiResponse<Map<String, Object>> createHardwareItem(@RequestBody HardwareItemRequest req) {
        return ApiResponse.ok(service.createHardwareItem(req));
    }

    @GetMapping("/review-pool")
    public ApiResponse<List<Map<String, Object>>> reviewPool() {
        return ApiResponse.ok(service.listReviewPool());
    }

    @PostMapping("/customer-orders/{id}/claim-review")
    public ApiResponse<Map<String, Object>> claimReview(@PathVariable Long id) {
        return ApiResponse.ok(service.claimReview(id));
    }

    @PostMapping("/customer-orders/{id}/split-drafts")
    public ApiResponse<Map<String, Object>> addSplitDraft(@PathVariable Long id, @RequestBody SplitDraftRequest req) {
        return ApiResponse.ok(service.addSplitDraft(id, req));
    }

    @GetMapping("/customer-orders/{id}/split-drafts")
    public ApiResponse<List<Map<String, Object>>> splitDrafts(@PathVariable Long id) {
        return ApiResponse.ok(service.listSplitDrafts(id));
    }

    @PostMapping("/customer-orders/{id}/submit-split")
    public ApiResponse<List<Map<String, Object>>> submitSplit(@PathVariable Long id) {
        return ApiResponse.ok(service.submitSplit(id));
    }

    @GetMapping("/factory-orders")
    public ApiResponse<List<Map<String, Object>>> factoryOrders(@RequestParam(required = false) Long customerOrderId,
                                                                @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.listFactoryOrders(customerOrderId, status));
    }

    @PostMapping("/factory-orders/{id}/assign")
    public ApiResponse<Map<String, Object>> assign(@PathVariable String id, @RequestBody AssignmentRequest req) {
        return ApiResponse.ok(service.assignQuoteOwner(id, req.userId(), req.reason(), false));
    }

    @PostMapping("/factory-orders/{id}/claim")
    public ApiResponse<Map<String, Object>> claim(@PathVariable String id) {
        return ApiResponse.ok(service.assignQuoteOwner(id, null, "订单池领取", true));
    }

    @PostMapping("/factory-orders/{id}/quotes")
    public ApiResponse<Map<String, Object>> saveQuote(@PathVariable String id, @RequestBody QuoteRequest req) {
        return ApiResponse.ok(service.saveQuote(id, req));
    }

    @GetMapping("/factory-orders/{id}/quotes")
    public ApiResponse<List<Map<String, Object>>> quotes(@PathVariable String id) {
        return ApiResponse.ok(service.listQuotes(id));
    }

    @PostMapping("/factory-orders/{id}/discount-requests")
    public ApiResponse<Map<String, Object>> requestDiscount(@PathVariable String id,
                                                             @RequestBody DiscountRequest req) {
        return ApiResponse.ok(service.requestDiscount(id, req));
    }

    @PostMapping("/discount-requests/{id}/approve")
    public ApiResponse<Map<String, Object>> approveDiscount(@PathVariable Long id,
                                                             @RequestBody ApprovalRequest req) {
        return ApiResponse.ok(service.approveDiscount(id, req));
    }

    @PostMapping("/factory-orders/{id}/external-quotes")
    public ApiResponse<Map<String, Object>> saveExternalQuote(@PathVariable String id,
                                                               @RequestBody ExternalQuoteRequest req) {
        return ApiResponse.ok(service.saveExternalQuote(id, req));
    }

    @GetMapping("/factory-orders/{id}/external-quotes")
    public ApiResponse<List<Map<String, Object>>> externalQuotes(@PathVariable String id) {
        return ApiResponse.ok(service.listExternalQuotes(id));
    }

    @GetMapping("/quotes/{quoteId}")
    public ApiResponse<Map<String, Object>> quoteDetail(@PathVariable Long quoteId) {
        return ApiResponse.ok(service.getQuote(quoteId));
    }

    @PostMapping("/customer-orders/{id}/price-adjustments")
    public ApiResponse<Map<String, Object>> requestPriceAdjustment(@PathVariable Long id,
                                                                   @RequestBody PriceAdjustmentRequest req) {
        return ApiResponse.ok(service.requestPriceAdjustment(id, req));
    }

    @PostMapping("/price-adjustments/{id}/approve")
    public ApiResponse<Map<String, Object>> approvePriceAdjustment(@PathVariable Long id,
                                                                   @RequestBody ApprovalRequest req) {
        return ApiResponse.ok(service.approvePriceAdjustment(id, req));
    }

    @PostMapping("/customer-orders/{id}/quote-confirmations")
    public ApiResponse<Map<String, Object>> confirmCustomerQuote(@PathVariable Long id,
                                                                 @RequestBody QuoteConfirmationRequest req) {
        return ApiResponse.ok(service.confirmCustomerQuote(id, req));
    }

    @PostMapping("/customer-orders/{id}/payment-plans")
    public ApiResponse<List<Map<String, Object>>> createPaymentPlan(@PathVariable Long id,
                                                                    @RequestBody PaymentPlanRequest req) {
        return ApiResponse.ok(service.createPaymentPlan(id, req));
    }

    @PostMapping("/customer-orders/{id}/payment-plans/approve")
    public ApiResponse<Boolean> approvePaymentPlan(@PathVariable Long id, @RequestBody ApprovalRequest req) {
        service.approvePaymentPlan(id, req);
        return ApiResponse.ok(true);
    }

    @GetMapping("/payment-reminders")
    public ApiResponse<List<Map<String, Object>>> paymentReminders() {
        return ApiResponse.ok(service.listPaymentReminders());
    }

    @GetMapping("/commercial-orders")
    public ApiResponse<List<Map<String, Object>>> commercialOrders() {
        return ApiResponse.ok(service.listCommercialOrders());
    }

    @GetMapping("/customer-orders/{id}/commercial")
    public ApiResponse<Map<String, Object>> commercialDetail(@PathVariable Long id) {
        return ApiResponse.ok(service.commercialDetail(id));
    }

    @GetMapping("/work-queues")
    public ApiResponse<Map<String, Object>> workQueues() {
        return ApiResponse.ok(service.workQueues());
    }

    @PostMapping("/customer-orders/{id}/receipts")
    public ApiResponse<Map<String, Object>> submitReceipt(@PathVariable Long id, @RequestBody ReceiptRequest req) {
        return ApiResponse.ok(service.submitReceipt(id, req));
    }

    @PostMapping("/receipts/{id}/confirm")
    public ApiResponse<Map<String, Object>> confirmReceipt(@PathVariable Long id, @RequestBody ApprovalRequest req) {
        return ApiResponse.ok(service.confirmReceipt(id, req));
    }

    @PostMapping("/customer-orders/{id}/cutting-release")
    public ApiResponse<Map<String, Object>> requestCuttingRelease(@PathVariable Long id,
                                                                  @RequestBody CuttingReleaseRequest req) {
        return ApiResponse.ok(service.requestCuttingRelease(id, req));
    }

    @PostMapping("/cutting-release/{id}/finance-confirm")
    public ApiResponse<Map<String, Object>> financeConfirmRelease(@PathVariable Long id,
                                                                  @RequestBody ApprovalRequest req) {
        return ApiResponse.ok(service.financeConfirmRelease(id, req));
    }

    @PostMapping("/cutting-release/{id}/director-approve")
    public ApiResponse<Map<String, Object>> directorApproveRelease(@PathVariable Long id,
                                                                   @RequestBody ApprovalRequest req) {
        return ApiResponse.ok(service.directorApproveRelease(id, req));
    }

    @PostMapping("/cutting-release/{id}/release")
    public ApiResponse<Map<String, Object>> releaseToCutting(@PathVariable Long id) {
        return ApiResponse.ok(service.releaseToCutting(id));
    }

    public record ProductionLineRequest(String lineCode, String lineName, String quoteMode, Boolean enabled, Integer sortOrder) {}
    public record ReceivingAccountRequest(String accountCode, String accountName, String paymentMethod,
                                          String bankName, String accountNoMasked, Boolean enabled, Integer sortOrder) {}
    public record HardwareItemRequest(String hardwareCode, String hardwareName, String specification, String unit,
                                      BigDecimal salePrice, String stockMode, Boolean enabled) {}
    public record SplitDraftRequest(String factoryOrderName, Long productionLineId, String orderType,
                                    String parentFactoryOrderId, String remark, Integer sortOrder) {}
    public record AssignmentRequest(Long userId, String reason) {}
    public record QuoteRequest(BigDecimal discountRate, String quoteDesc, Boolean submit, List<QuoteCalcRequest> items) {}
    public record DiscountRequest(BigDecimal requestedRate, String reason) {}
    public record ExternalQuoteRequest(String externalQuoteNo, BigDecimal finalAmount, LocalDate quoteDate,
                                       String attachmentPath, Boolean customerConfirmed,
                                       LocalDateTime confirmedAt, String remark) {}
    public record PriceAdjustmentRequest(BigDecimal finalAmount, String reason) {}
    public record ApprovalRequest(Boolean approved, String remark) {}
    public record QuoteConfirmationRequest(Long pdfId, String confirmationMethod, LocalDateTime confirmedAt,
                                           String customerContact, String confirmationRemark, String attachmentPath) {}
    public record PaymentInstallmentRequest(Integer installmentNo, BigDecimal plannedAmount, LocalDate dueDate) {}
    public record PaymentPlanRequest(String settlementType, Boolean customerConfirmed,
                                     List<PaymentInstallmentRequest> installments) {}
    public record ReceiptRequest(Long paymentPlanId, Long receivingAccountId, BigDecimal actualAmount,
                                 LocalDateTime receivedAt, String paymentMethod, String payerName,
                                 String bankReference, String voucherPath) {}
    public record CuttingReleaseRequest(String requestType, Boolean bossVerbalApproved,
                                        LocalDateTime bossConfirmedAt, String requestReason) {}
}
