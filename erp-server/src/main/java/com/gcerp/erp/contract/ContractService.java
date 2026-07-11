package com.gcerp.erp.contract;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcerp.erp.customer.Customer;
import com.gcerp.erp.customer.CustomerMapper;
import com.gcerp.erp.customerorder.CustomerOrder;
import com.gcerp.erp.customerorder.CustomerOrderMapper;
import com.gcerp.erp.project.Project;
import com.gcerp.erp.project.ProjectMapper;
import com.gcerp.erp.workflow.WorkflowLog;
import com.gcerp.erp.workflow.WorkflowLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContractService {
    private static final String STATUS_QUOTING = "报价中";
    private static final String STATUS_WAIT_CUSTOMER = "待客户确认";
    private static final String STATUS_WAIT_FINANCE = "待财务确认";
    private static final String STATUS_PAID = "财务已收款";
    private static final String STATUS_WAIT_SCHEDULE = "待排产";
    private static final String STATUS_PRODUCING = "生产中";
    private static final String STATUS_DONE = "已完成";
    private static final String STATUS_WAIT_SHIP = "待发货";
    private static final String STATUS_SHIPPED = "已发货";

    private static final Map<String, Set<String>> FLOW_RULES = Map.of(
            STATUS_QUOTING, Set.of(STATUS_WAIT_CUSTOMER),
            STATUS_WAIT_CUSTOMER, Set.of(STATUS_WAIT_FINANCE),
            STATUS_WAIT_FINANCE, Set.of(STATUS_PAID),
            STATUS_PAID, Set.of(STATUS_WAIT_SCHEDULE),
            STATUS_WAIT_SCHEDULE, Set.of(STATUS_PRODUCING),
            STATUS_PRODUCING, Set.of(STATUS_DONE),
            STATUS_DONE, Set.of(STATUS_WAIT_SHIP),
            STATUS_WAIT_SHIP, Set.of(STATUS_SHIPPED),
            STATUS_SHIPPED, Set.of()
    );

    private static final Map<String, String> LEGACY_STATUS_MAP = Map.ofEntries(
            Map.entry("PENDING_DESIGN", STATUS_QUOTING),
            Map.entry("PENDING_QUOTE", STATUS_WAIT_FINANCE),
            Map.entry("QUOTING", STATUS_WAIT_FINANCE),
            Map.entry("PENDING_CONFIRM", STATUS_WAIT_CUSTOMER),
            Map.entry("WAIT_CUSTOMER_CONFIRM", STATUS_WAIT_CUSTOMER),
            Map.entry("PENDING_PAYMENT", STATUS_WAIT_FINANCE),
            Map.entry("WAIT_PAYMENT", STATUS_WAIT_FINANCE),
            Map.entry("PENDING_FINANCE_CONFIRM", STATUS_WAIT_FINANCE),
            Map.entry("FINANCE_CONFIRMED", STATUS_PAID),
            Map.entry("PAID", STATUS_PAID),
            Map.entry("PAYMENT_CONFIRMED", STATUS_PAID),
            Map.entry("PENDING_PRODUCTION", STATUS_WAIT_SCHEDULE),
            Map.entry("TO_PRODUCTION", STATUS_WAIT_SCHEDULE),
            Map.entry("IN_PRODUCTION", STATUS_PRODUCING),
            Map.entry("PRODUCING", STATUS_PRODUCING),
            Map.entry("COMPLETED", STATUS_DONE),
            Map.entry("DONE", STATUS_DONE),
            Map.entry("PENDING_SHIPMENT", STATUS_WAIT_SHIP),
            Map.entry("WAIT_SHIP", STATUS_WAIT_SHIP),
            Map.entry("SHIPPED", STATUS_SHIPPED)
    );

    private final ContractMapper contractMapper;
    private final WorkflowLogMapper workflowLogMapper;
    private final JdbcTemplate jdbcTemplate;
    private final CustomerMapper customerMapper;
    private final ProjectMapper projectMapper;
    private final CustomerOrderMapper customerOrderMapper;

    @Transactional
    public synchronized Contract createContract(ContractCreateRequest req) {
        Customer customer = requireCustomer(req.getCustomerId());
        Project project = requireProject(req.getProjectId(), req.getCustomerId());
        CustomerOrder customerOrder = resolveCustomerOrder(req.getCustomerOrderId(), req.getCustomerOrderNo(), project, customer);

        String factoryOrderPrefix = normalizeFactoryOrderPrefix(req.getFactoryOrderPrefix());
        String factoryOrderNo = generateFactoryOrderNo(factoryOrderPrefix);
        Integer duplicate = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM contract WHERE contract_no = ? OR factory_order_no = ?",
                Integer.class,
                factoryOrderNo,
                factoryOrderNo);
        if (duplicate != null && duplicate > 0) {
            factoryOrderNo = generateFactoryOrderNo(factoryOrderPrefix);
        }

        Contract contract = new Contract();
        contract.setContractNo(factoryOrderNo);
        contract.setCustomerId(req.getCustomerId());
        contract.setProjectId(req.getProjectId());
        contract.setCustomerOrderId(customerOrder.getId());
        contract.setFactoryOrderNo(factoryOrderNo);
        contract.setCustomerOrderNo(customerOrder.getCustomerOrderNo());
        contract.setCustomerName(customer.getCustomerName());
        contract.setCustomerPhone(customer.getPhone());
        contract.setCustomerAddress(customer.getAddress());
        contract.setCustomerNameSnapshot(contract.getCustomerName());
        contract.setCustomerPhoneSnapshot(contract.getCustomerPhone());
        contract.setCustomerAddressSnapshot(contract.getCustomerAddress());
        contract.setProjectNameSnapshot(project.getProjectName());
        contract.setDemandDesc(req.getDemandDesc());
        contract.setStatus(STATUS_QUOTING);
        contract.setProductionProgress(null);
        contract.setCustomFields(req.getCustomFields());
        contract.setCreatedAt(LocalDateTime.now());
        contract.setUpdatedAt(LocalDateTime.now());
        contractMapper.insert(contract);
        insertLog(contract.getId(), null, contract.getStatus(), "系统", "创建工厂订单");
        hydrateProjectFields(contract);
        return contract;
    }

    @Transactional
    public List<Contract> listByStatus(String status) {
        String normalizedFilter = normalizeStatus(status);
        LambdaQueryWrapper<Contract> qw = new LambdaQueryWrapper<>();
        if (normalizedFilter != null && !normalizedFilter.isBlank()) {
            validateStatus(normalizedFilter);
            qw.eq(Contract::getStatus, normalizedFilter);
        }
        qw.orderByDesc(Contract::getCreatedAt);
        List<Contract> list = contractMapper.selectList(qw);
        for (Contract c : list) {
            String oldStatus = c.getStatus();
            String normalized = normalizeStatus(oldStatus);
            if (normalized != null && !normalized.equals(oldStatus)) {
                c.setStatus(normalized);
                c.setUpdatedAt(LocalDateTime.now());
                contractMapper.updateById(c);
                insertLog(c.getId(), oldStatus, normalized, "系统", "兼容迁移历史状态");
            }
            hydrateProjectFields(c);
        }
        return list;
    }

    @Transactional
    public Contract updateStatus(Long contractId, ContractStatusUpdateRequest req) {
        Contract contract = getById(contractId);
        String from = normalizeStatus(contract.getStatus());
        String to = normalizeStatus(req.getTargetStatus());
        validateStatus(to);
        if (!to.equals(from)) {
            Set<String> allowed = FLOW_RULES.getOrDefault(from, Set.of());
            boolean allowManualBackfill = STATUS_PAID.equals(from) && STATUS_WAIT_FINANCE.equals(to);
            if (!allowed.contains(to) && !allowManualBackfill) {
                throw new IllegalArgumentException("invalid status transition: " + from + " -> " + to);
            }
        }
        contract.setStatus(to);
        if (STATUS_PRODUCING.equals(to)) {
            int p = req.getProductionProgress() == null ? 0 : req.getProductionProgress();
            contract.setProductionProgress(Math.max(0, Math.min(100, p)));
        } else if (STATUS_DONE.equals(to) || STATUS_WAIT_SHIP.equals(to) || STATUS_SHIPPED.equals(to)) {
            contract.setProductionProgress(100);
        } else {
            contract.setProductionProgress(null);
        }
        contract.setUpdatedAt(LocalDateTime.now());
        contractMapper.updateById(contract);
        insertLog(contractId, from, to, req.getOperator(), req.getRemark());
        hydrateProjectFields(contract);
        return contract;
    }

    @Transactional
    public Contract businessAdvance(Long contractId, String expectedStatus, String targetStatus, String operator, String remark) {
        Contract contract = getById(contractId);
        String from = normalizeStatus(contract.getStatus());
        String to = normalizeStatus(targetStatus);
        if (to == null) throw new IllegalArgumentException("target status is required");
        contract.setStatus(to);
        if (STATUS_PRODUCING.equals(to)) {
            contract.setProductionProgress(0);
        } else if (STATUS_DONE.equals(to) || STATUS_WAIT_SHIP.equals(to) || STATUS_SHIPPED.equals(to)) {
            contract.setProductionProgress(100);
        }
        contract.setUpdatedAt(LocalDateTime.now());
        contractMapper.updateById(contract);
        insertLog(contractId, from, to, operator, remark);
        hydrateProjectFields(contract);
        return contract;
    }

    @Transactional
    public Contract updateContract(Long contractId, ContractUpdateRequest req) {
        Contract contract = getById(contractId);
        Customer customer = requireCustomer(req.getCustomerId());
        Project project = requireProject(req.getProjectId(), req.getCustomerId());
        CustomerOrder customerOrder = resolveCustomerOrder(req.getCustomerOrderId(), req.getCustomerOrderNo(), project, customer);

        String factoryOrderNo = normalizeFactoryOrderNo(req.getFactoryOrderNo());
        if (factoryOrderNo != null && !isValidFactoryOrderNo(factoryOrderNo)) {
            throw new IllegalArgumentException("factory order no must start with L or V");
        }
        String currentFactoryOrderNo = normalizeFactoryOrderNo(contract.getFactoryOrderNo() == null ? contract.getContractNo() : contract.getFactoryOrderNo());
        if (factoryOrderNo != null && !factoryOrderNo.equals(currentFactoryOrderNo)) {
            Integer duplicate = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM contract WHERE id <> ? AND (contract_no = ? OR factory_order_no = ?)",
                    Integer.class,
                    contractId,
                    factoryOrderNo,
                    factoryOrderNo);
            if (duplicate != null && duplicate > 0) throw new IllegalArgumentException("factory order no already exists");
            contract.setContractNo(factoryOrderNo);
            contract.setFactoryOrderNo(factoryOrderNo);
        } else if (contract.getFactoryOrderNo() == null) {
            contract.setFactoryOrderNo(contract.getContractNo());
        }

        contract.setCustomerId(req.getCustomerId());
        contract.setProjectId(req.getProjectId());
        contract.setCustomerOrderId(customerOrder.getId());
        contract.setCustomerOrderNo(customerOrder.getCustomerOrderNo());
        contract.setCustomerName(customer.getCustomerName());
        contract.setCustomerPhone(customer.getPhone());
        contract.setCustomerAddress(customer.getAddress());
        contract.setCustomerNameSnapshot(contract.getCustomerName());
        contract.setCustomerPhoneSnapshot(contract.getCustomerPhone());
        contract.setCustomerAddressSnapshot(contract.getCustomerAddress());
        contract.setProjectNameSnapshot(project.getProjectName());
        contract.setDemandDesc(req.getDemandDesc());
        contract.setCustomFields(req.getCustomFields());
        contract.setUpdatedAt(LocalDateTime.now());
        contractMapper.updateById(contract);
        hydrateProjectFields(contract);
        return contract;
    }

    @Transactional
    public void deleteContract(Long contractId) {
        getById(contractId);
        Integer quoteCnt = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM quotation WHERE contract_id = ?", Integer.class, contractId);
        Integer assignCnt = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM quote_assignment WHERE contract_id = ?", Integer.class, contractId);
        Integer payCnt = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM payment_record WHERE contract_id = ?", Integer.class, contractId);
        Integer productionCnt = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM production_task WHERE contract_id = ?", Integer.class, contractId);
        if (positive(quoteCnt) || positive(assignCnt) || positive(payCnt) || positive(productionCnt)) {
            throw new IllegalArgumentException("factory order has linked records and cannot be deleted");
        }
        jdbcTemplate.update("DELETE FROM contract_cad_file WHERE contract_id = ?", contractId);
        jdbcTemplate.update("DELETE FROM workflow_log WHERE contract_id = ?", contractId);
        contractMapper.deleteById(contractId);
    }

    public Contract getById(Long contractId) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) throw new IllegalArgumentException("factory order not found");
        hydrateProjectFields(contract);
        return contract;
    }

    public List<Map<String, Object>> listLogs(Long contractId) {
        return jdbcTemplate.queryForList("""
                SELECT id, contract_id, from_status, to_status, operator, remark, created_at, 'WORKFLOW' AS log_type, 'STATUS_CHANGE' AS action
                FROM workflow_log
                WHERE contract_id = ?
                ORDER BY created_at DESC, id DESC
                """, contractId);
    }

    private Customer requireCustomer(Long customerId) {
        if (customerId == null) throw new IllegalArgumentException("customerId is required");
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) throw new IllegalArgumentException("customer not found");
        return customer;
    }

    private Project requireProject(Long projectId, Long customerId) {
        if (projectId == null) throw new IllegalArgumentException("projectId is required");
        Project project = projectMapper.selectById(projectId);
        if (project == null || Integer.valueOf(1).equals(project.getIsDeleted())) {
            throw new IllegalArgumentException("project not found");
        }
        if (!customerId.equals(project.getCustomerId())) {
            throw new IllegalArgumentException("project must belong to the selected customer");
        }
        return project;
    }

    private CustomerOrder requireCustomerOrder(Long customerOrderId, Long projectId, Long customerId) {
        if (customerOrderId == null) throw new IllegalArgumentException("customerOrderId is required");
        CustomerOrder customerOrder = customerOrderMapper.selectById(customerOrderId);
        if (customerOrder == null || Integer.valueOf(1).equals(customerOrder.getIsDeleted())) {
            throw new IllegalArgumentException("customer order not found");
        }
        if (!projectId.equals(customerOrder.getProjectId()) || !customerId.equals(customerOrder.getCustomerId())) {
            throw new IllegalArgumentException("customer order must belong to the selected project");
        }
        return customerOrder;
    }

    private CustomerOrder resolveCustomerOrder(Long customerOrderId, String customerOrderNo, Project project, Customer customer) {
        if (customerOrderId != null) {
            return requireCustomerOrder(customerOrderId, project.getProjectId(), customer.getId());
        }
        String orderNo = normalizeText(customerOrderNo);
        if (orderNo == null) throw new IllegalArgumentException("customerOrderNo is required");

        CustomerOrder existing = customerOrderMapper.selectOne(new LambdaQueryWrapper<CustomerOrder>()
                .eq(CustomerOrder::getProjectId, project.getProjectId())
                .eq(CustomerOrder::getCustomerOrderNo, orderNo)
                .eq(CustomerOrder::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (existing != null) {
            if (!customer.getId().equals(existing.getCustomerId())) {
                throw new IllegalArgumentException("customer order must belong to the selected customer");
            }
            return existing;
        }

        CustomerOrder created = new CustomerOrder();
        created.setCustomerId(customer.getId());
        created.setProjectId(project.getProjectId());
        created.setCustomerOrderNo(orderNo);
        created.setCustomerOrderName(orderNo);
        created.setStatus("启用");
        created.setIsDeleted(0);
        created.setCreatedAt(LocalDateTime.now());
        created.setUpdatedAt(LocalDateTime.now());
        customerOrderMapper.insert(created);
        return created;
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }

    private String generateFactoryOrderNo(String orderPrefix) {
        String prefix = normalizeFactoryOrderPrefix(orderPrefix) + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long maxSeq = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(CAST(SUBSTRING(factory_order_no, 10) AS UNSIGNED)), 0)
                FROM contract
                WHERE factory_order_no LIKE ?
                """, Long.class, prefix + "%");
        long next = (maxSeq == null ? 0 : maxSeq) + 1;
        return prefix + String.format("%03d", next);
    }

    private void hydrateProjectFields(Contract contract) {
        if (contract == null) return;
        if (contract.getFactoryOrderNo() == null) contract.setFactoryOrderNo(contract.getContractNo());
        if (contract.getCustomerOrderId() != null) {
            CustomerOrder customerOrder = customerOrderMapper.selectById(contract.getCustomerOrderId());
            if (customerOrder != null && !Integer.valueOf(1).equals(customerOrder.getIsDeleted())) {
                contract.setCustomerOrderNo(customerOrder.getCustomerOrderNo());
            }
        }
        if (contract.getProjectId() == null) return;
        Project project = projectMapper.selectById(contract.getProjectId());
        if (project == null) return;
        contract.setProjectNo(project.getProjectNo());
        contract.setProjectName(project.getProjectName());
    }

    private String normalizeText(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeFactoryOrderNo(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String normalizeFactoryOrderPrefix(String value) {
        String prefix = normalizeText(value);
        if (prefix == null) return "V";
        prefix = prefix.toUpperCase();
        return "L".equals(prefix) ? "L" : "V";
    }

    private boolean isValidFactoryOrderNo(String value) {
        return value != null && (value.startsWith("L") || value.startsWith("V"));
    }

    private String normalizeStatus(String status) {
        String value = normalizeText(status);
        if (value == null) return null;
        return LEGACY_STATUS_MAP.getOrDefault(value, value);
    }

    private void validateStatus(String status) {
        if (!FLOW_RULES.containsKey(status)) {
            throw new IllegalArgumentException("invalid order status: " + status);
        }
    }

    private void insertLog(Long contractId, String from, String to, String operator, String remark) {
        WorkflowLog log = new WorkflowLog();
        log.setContractId(contractId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperator(operator == null || operator.isBlank() ? "系统" : operator);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        workflowLogMapper.insert(log);
    }
}
