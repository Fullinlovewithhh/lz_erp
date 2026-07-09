package com.gcerp.erp.payment;

import com.gcerp.erp.contract.Contract;
import com.gcerp.erp.contract.ContractService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
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

@Service
public class FinanceVoucherService {

    private static final Path ROOT = Paths.get("upload", "finance-voucher");
    private static final long MAX_SIZE = 20L * 1024L * 1024L;

    private final JdbcTemplate jdbcTemplate;
    private final ContractService contractService;

    public FinanceVoucherService(JdbcTemplate jdbcTemplate, ContractService contractService) {
        this.jdbcTemplate = jdbcTemplate;
        this.contractService = contractService;
    }

    public Map<String, Object> upload(Long contractId, MultipartFile file, String operator) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("上传文件不能为空");
        if (file.getSize() > MAX_SIZE) throw new IllegalArgumentException("文件大小不能超过20MB");
        String ext = getExt(file.getOriginalFilename());
        if (!isAllowed(ext)) throw new IllegalArgumentException("仅支持图片或PDF文件");

        Contract contract = contractService.getById(contractId);
        String contractNo = contract.getContractNo();

        String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = ROOT.resolve(contractNo);
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName).normalize();
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        jdbcTemplate.update(
                "INSERT INTO finance_voucher(contract_id,contract_no,file_name,file_ext,file_size,file_path,created_by,created_at) VALUES(?,?,?,?,?,?,?,NOW())",
                contractId, contractNo, fileName, ext, file.getSize(), target.toString().replace("\\", "/"), operator
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return jdbcTemplate.queryForMap("SELECT * FROM finance_voucher WHERE id = ?", id);
    }

    public List<Map<String, Object>> listByContract(Long contractId) {
        return jdbcTemplate.queryForList("SELECT * FROM finance_voucher WHERE contract_id = ? ORDER BY id DESC", contractId);
    }

    public int deleteById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM finance_voucher WHERE id = ?", id);
        if (rows.isEmpty()) return 0;
        String path = String.valueOf(rows.get(0).get("file_path"));
        try { Files.deleteIfExists(Paths.get(path)); } catch (Exception ignored) {}
        return jdbcTemplate.update("DELETE FROM finance_voucher WHERE id = ?", id);
    }

    public Map<String, Object> getById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM finance_voucher WHERE id = ?", id);
        if (rows.isEmpty()) throw new IllegalArgumentException("凭据不存在");
        return rows.get(0);
    }

    private static String getExt(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }

    private static boolean isAllowed(String ext) {
        return ext.matches("\\.(jpg|jpeg|png|webp|bmp|gif|pdf)");
    }
}