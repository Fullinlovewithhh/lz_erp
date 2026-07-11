package com.gcerp.erp.customerorder;

import com.gcerp.erp.auth.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerOrderCadService {
    private static final Path ROOT = Path.of("upload", "customer-order-cad");
    private final JdbcTemplate jdbcTemplate;
    private final CustomerOrderService customerOrderService;

    public Map<String, Object> upload(Long customerOrderId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("CAD文件不能为空");
        CustomerOrder order = customerOrderService.getById(customerOrderId);
        String fileName = Path.of(file.getOriginalFilename() == null ? "cad-file" : file.getOriginalFilename()).getFileName().toString();
        String ext = extension(fileName);
        Path dir = ROOT.resolve(safe(order.getCustomerOrderNo()));
        Files.createDirectories(dir);
        String storedName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + UUID.randomUUID().toString().replace("-", "") + (ext == null ? "" : "." + ext);
        Path target = dir.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        Long userId = AuthContext.get() == null ? null : AuthContext.get().getId();
        jdbcTemplate.update("""
                INSERT INTO customer_order_cad_file(customer_order_id,file_name,file_ext,file_size,file_path,created_by,created_at)
                VALUES(?,?,?,?,?,?,NOW())
                """, customerOrderId, fileName, ext, file.getSize(), target.toString().replace("\\", "/"), userId);
        return jdbcTemplate.queryForMap("SELECT * FROM customer_order_cad_file WHERE id=LAST_INSERT_ID()");
    }

    public List<Map<String, Object>> list(Long customerOrderId) {
        return jdbcTemplate.queryForList("SELECT * FROM customer_order_cad_file WHERE customer_order_id=? ORDER BY id DESC", customerOrderId);
    }

    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 || index == fileName.length() - 1 ? null : fileName.substring(index + 1).toLowerCase();
    }

    private String safe(String value) {
        return value.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
