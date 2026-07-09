package com.gcerp.erp.contract;

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
public class ContractCadService {

    private static final Path CAD_ROOT = Paths.get("upload", "cad");

    private final JdbcTemplate jdbcTemplate;
    private final ContractService contractService;

    public ContractCadService(JdbcTemplate jdbcTemplate, ContractService contractService) {
        this.jdbcTemplate = jdbcTemplate;
        this.contractService = contractService;
    }

    public Map<String, Object> upload(Long contractId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        Contract contract = contractService.getById(contractId);
        String contractNo = contract.getContractNo();
        String ext = getExt(file.getOriginalFilename());
        String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = CAD_ROOT.resolve(contractNo);
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName).normalize();
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        jdbcTemplate.update(
                "INSERT INTO contract_cad_file(contract_id,contract_no,file_name,file_ext,file_size,file_path,created_at) VALUES(?,?,?,?,?,?,NOW())",
                contractId, contractNo, fileName, ext, file.getSize(), target.toString().replace("\\", "/")
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return jdbcTemplate.queryForMap("SELECT * FROM contract_cad_file WHERE id = ?", id);
    }

    public List<Map<String, Object>> listByContract(Long contractId) {
        return jdbcTemplate.queryForList("SELECT * FROM contract_cad_file WHERE contract_id = ? ORDER BY id DESC", contractId);
    }

    public Map<String, Object> getById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM contract_cad_file WHERE id = ?", id);
        if (rows.isEmpty()) throw new IllegalArgumentException("CAD 文件不存在");
        return rows.get(0);
    }

    private String getExt(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }
}

