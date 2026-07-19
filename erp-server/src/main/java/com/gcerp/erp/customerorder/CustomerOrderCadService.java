package com.gcerp.erp.customerorder;

import com.gcerp.erp.auth.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public CustomerOrder createWithFiles(CustomerOrderRequest req, MultipartFile[] files) throws IOException {
        validateFiles(files);
        CustomerOrder order = customerOrderService.create(req);
        saveVersion(order, List.of(files), 1, "创建客户订单");
        return order;
    }

    @Transactional
    public Map<String, Object> upload(Long customerOrderId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("CAD文件不能为空");
        CustomerOrder order = customerOrderService.getById(customerOrderId);
        int version = nextVersion(customerOrderId);
        return saveVersion(order, List.of(file), version, "追加客户资料").getFirst();
    }

    private List<Map<String, Object>> saveVersion(CustomerOrder order, List<MultipartFile> files,
                                                   int version, String remark) throws IOException {
        jdbcTemplate.update("UPDATE customer_order_cad_file SET is_current=0 WHERE customer_order_id=?", order.getId());
        List<Map<String, Object>> saved = new java.util.ArrayList<>();
        for (MultipartFile file : files) saved.add(saveFile(order, file, version, remark));
        return saved;
    }

    private Map<String, Object> saveFile(CustomerOrder order, MultipartFile file, int version, String remark) throws IOException {
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
                INSERT INTO customer_order_cad_file(customer_order_id,file_name,file_ext,file_size,file_path,
                version_no,is_current,version_remark,created_by,created_at)
                VALUES(?,?,?,?,?,?,1,?,?,NOW())
                """, order.getId(), fileName, ext, file.getSize(), target.toString().replace("\\", "/"), version, remark, userId);
        return jdbcTemplate.queryForMap("SELECT * FROM customer_order_cad_file WHERE id=LAST_INSERT_ID()");
    }

    public List<Map<String, Object>> list(Long customerOrderId) {
        return jdbcTemplate.queryForList("SELECT * FROM customer_order_cad_file WHERE customer_order_id=? ORDER BY version_no DESC,id", customerOrderId);
    }

    private void validateFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) throw new IllegalArgumentException("至少上传一个CAD文件");
        boolean hasCad = false;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) throw new IllegalArgumentException("上传文件不能为空");
            String ext = extension(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
            if ("dwg".equals(ext) || "dxf".equals(ext)) hasCad = true;
        }
        if (!hasCad) throw new IllegalArgumentException("至少包含一个DWG或DXF文件");
    }

    private int nextVersion(Long customerOrderId) {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(version_no),0)+1 FROM customer_order_cad_file WHERE customer_order_id=?",
                Integer.class, customerOrderId);
        return version == null ? 1 : version;
    }

    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 || index == fileName.length() - 1 ? null : fileName.substring(index + 1).toLowerCase();
    }

    private String safe(String value) {
        return value.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
