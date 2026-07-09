package com.gcerp.erp.contract;

import com.gcerp.erp.common.ApiResponse;
import jakarta.validation.Valid;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@CrossOrigin
public class ContractController {

    private final ContractService contractService;
    private final ContractCadService contractCadService;

    @PostMapping
    public ApiResponse<Contract> create(@Valid @RequestBody ContractCreateRequest req) {
        return ApiResponse.ok(contractService.createContract(req));
    }

    @GetMapping
    public ApiResponse<List<Contract>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(contractService.listByStatus(status));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Contract> updateStatus(@PathVariable Long id, @Valid @RequestBody ContractStatusUpdateRequest req) {
        return ApiResponse.ok(contractService.updateStatus(id, req));
    }

    @PutMapping("/{id}")
    public ApiResponse<Contract> update(@PathVariable Long id, @Valid @RequestBody ContractUpdateRequest req) {
        return ApiResponse.ok(contractService.updateContract(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        contractService.deleteContract(id);
        return ApiResponse.ok(true);
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<Map<String, Object>>> logs(@PathVariable Long id) {
        return ApiResponse.ok(contractService.listLogs(id));
    }

    @PostMapping("/{id}/cad")
    public ApiResponse<Map<String, Object>> uploadCad(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(contractCadService.upload(id, file));
    }

    @GetMapping("/{id}/cad")
    public ApiResponse<List<Map<String, Object>>> listCad(@PathVariable Long id) {
        return ApiResponse.ok(contractCadService.listByContract(id));
    }

    @GetMapping("/cad/files/{cadId}")
    public ResponseEntity<Resource> previewCad(@PathVariable Long cadId) throws IOException {
        Map<String, Object> row = contractCadService.getById(cadId);
        Path file = Path.of(String.valueOf(row.get("file_path")));
        if (!Files.exists(file)) return ResponseEntity.notFound().build();
        Resource resource = new UrlResource(file.toUri());
        String contentType = Files.probeContentType(file);
        if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(resource);
    }
}
