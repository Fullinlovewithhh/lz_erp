package com.gcerp.erp.payment;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin
public class FinanceVoucherController {

    private static final Path ROOT = Paths.get("upload", "finance-voucher").toAbsolutePath().normalize();
    private final FinanceVoucherService financeVoucherService;

    @PostMapping("/{contractId}/vouchers")
    public ApiResponse<Map<String, Object>> upload(@PathVariable Long contractId,
                                                   @RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "operator", required = false) String operator) throws IOException {
        return ApiResponse.ok(financeVoucherService.upload(contractId, file, operator));
    }

    @GetMapping("/{contractId}/vouchers")
    public ApiResponse<List<Map<String, Object>>> list(@PathVariable Long contractId) {
        return ApiResponse.ok(financeVoucherService.listByContract(contractId));
    }

    @DeleteMapping("/vouchers/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        int affected = financeVoucherService.deleteById(id);
        if (affected <= 0) return ApiResponse.fail("凭据不存在");
        return ApiResponse.ok("删除成功");
    }

    @GetMapping("/vouchers/files/{id}")
    public ResponseEntity<Resource> preview(@PathVariable Long id) throws IOException {
        Map<String, Object> row = financeVoucherService.getById(id);
        Path file = Paths.get(String.valueOf(row.get("file_path"))).toAbsolutePath().normalize();
        if (!Files.exists(file) || !file.startsWith(ROOT)) return ResponseEntity.notFound().build();
        Resource resource = new UrlResource(file.toUri());
        String contentType = Files.probeContentType(file);
        if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(resource);
    }
}