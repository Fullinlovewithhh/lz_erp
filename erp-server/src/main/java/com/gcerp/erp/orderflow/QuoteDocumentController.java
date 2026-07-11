package com.gcerp.erp.orderflow;

import com.gcerp.erp.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order-flow")
@RequiredArgsConstructor
@CrossOrigin
public class QuoteDocumentController {
    private final QuoteDocumentService service;

    @PostMapping("/quote-attachments")
    public ApiResponse<Map<String, Object>> uploadQuoteAttachment(@RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(service.uploadAttachment(file));
    }

    @GetMapping("/company-profile")
    public ApiResponse<Map<String, Object>> companyProfile() {
        return ApiResponse.ok(service.companyProfile());
    }

    @PutMapping("/company-profile")
    public ApiResponse<Map<String, Object>> updateCompanyProfile(@RequestBody CompanyProfileRequest req) {
        return ApiResponse.ok(service.updateCompanyProfile(req));
    }

    @PostMapping("/customer-orders/{id}/quote-pdfs")
    public ApiResponse<Map<String, Object>> generatePdf(@PathVariable Long id, @RequestBody QuotePdfRequest req) throws Exception {
        return ApiResponse.ok(service.generatePdf(id, req));
    }

    @GetMapping("/customer-orders/{id}/quote-pdfs")
    public ApiResponse<List<Map<String, Object>>> listPdfs(@PathVariable Long id) {
        return ApiResponse.ok(service.listPdfs(id));
    }

    @GetMapping("/quote-pdfs/{id}/download")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long id) throws IOException {
        Map<String, Object> row = service.pdf(id);
        Path path = Path.of(String.valueOf(row.get("file_path")));
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=quote-v" + row.get("pdf_version") + ".pdf")
                .body(resource);
    }

    public record CompanyProfileRequest(String companyName, String logoPath, String companyAddress, String contactPhone) {}
    public record QuotePdfRequest(BigDecimal taxRate, Integer validDays, String quoteRemark) {}
}
