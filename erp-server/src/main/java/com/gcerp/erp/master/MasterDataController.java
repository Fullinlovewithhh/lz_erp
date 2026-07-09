package com.gcerp.erp.master;

import com.gcerp.erp.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/master")
@RequiredArgsConstructor
@CrossOrigin
public class MasterDataController {

    private static final Path UPLOAD_DIR = Paths.get("upload", "master");
    private final MasterDataService masterDataService;

    @GetMapping("/products")
    public ApiResponse<List<Map<String, Object>>> products(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(masterDataService.listProducts(keyword));
    }

    @GetMapping("/materials")
    public ApiResponse<List<Map<String, Object>>> materials(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(masterDataService.listMaterials(keyword));
    }

    @GetMapping("/staff")
    public ApiResponse<List<Map<String, Object>>> staff(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(masterDataService.listStaff(keyword));
    }

    @GetMapping("/{module}/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable String module, @PathVariable Long id) {
        Map<String, Object> row = masterDataService.getOne(module, id);
        if (row == null) return ApiResponse.fail("记录不存在");
        return ApiResponse.ok(row);
    }

    @PutMapping("/{module}/{id}")
    public ApiResponse<String> update(@PathVariable String module,
                                      @PathVariable Long id,
                                      @RequestBody Map<String, Object> body,
                                      @RequestHeader(value = "X-Operator", required = false) String operator) {
        int affected = masterDataService.updateOne(module, id, body, operator);
        if (affected <= 0) return ApiResponse.fail("更新失败，记录不存在或无可更新字段");
        return ApiResponse.ok("更新成功");
    }

    @DeleteMapping("/{module}/{id}")
    public ApiResponse<String> delete(@PathVariable String module,
                                      @PathVariable Long id,
                                      @RequestHeader(value = "X-Operator", required = false) String operator) {
        int affected = masterDataService.deleteOne(module, id, operator);
        if (affected <= 0) return ApiResponse.fail("删除失败，记录不存在");
        return ApiResponse.ok("删除成功");
    }

    @PostMapping("/uploads/image")
    public ApiResponse<String> uploadImageTemp(@RequestParam("module") String module,
                                               @RequestParam("file") MultipartFile file) throws IOException {
        if (!"product".equals(module) && !"material".equals(module)) {
            return ApiResponse.fail("module 仅支持 product 或 material");
        }
        return ApiResponse.ok(saveImage(file, module));
    }

    @PostMapping("/products/{id}/image")
    public ApiResponse<String> uploadProductImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        String url = saveImage(file, "product");
        int affected = masterDataService.updateProductImage(id, url);
        if (affected <= 0) return ApiResponse.fail("产品不存在，图片未保存");
        return ApiResponse.ok(url);
    }

    @PostMapping("/materials/{id}/image")
    public ApiResponse<String> uploadMaterialImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        String url = saveImage(file, "material");
        int affected = masterDataService.updateMaterialImage(id, url);
        if (affected <= 0) return ApiResponse.fail("材料不存在，图片未保存");
        return ApiResponse.ok(url);
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> viewImageLegacy(@PathVariable String filename) throws IOException {
        Path file = UPLOAD_DIR.resolve(filename).normalize();
        return buildFileResponse(file);
    }

    @GetMapping("/files/{module}/{filename:.+}")
    public ResponseEntity<Resource> viewImage(@PathVariable String module, @PathVariable String filename) throws IOException {
        Path file = UPLOAD_DIR.resolve(module).resolve(filename).normalize();
        return buildFileResponse(file);
    }

    private ResponseEntity<Resource> buildFileResponse(Path file) throws IOException {
        if (!Files.exists(file) || !file.startsWith(UPLOAD_DIR)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(file.toUri());
        String contentType = Files.probeContentType(file);
        if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(resource);
    }

    private String saveImage(MultipartFile file, String module) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("上传文件不能为空");
        Path moduleDir = UPLOAD_DIR.resolve(module);
        Files.createDirectories(moduleDir);

        String ext = getExt(file.getOriginalFilename());
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = time + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = moduleDir.resolve(filename).normalize();
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/api/master/files/" + module + "/" + filename;
    }

    private String getExt(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}

