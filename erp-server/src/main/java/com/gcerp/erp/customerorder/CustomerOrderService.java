package com.gcerp.erp.customerorder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcerp.erp.auth.AuthContext;
import com.gcerp.erp.customer.Customer;
import com.gcerp.erp.customer.CustomerMapper;
import com.gcerp.erp.project.Project;
import com.gcerp.erp.project.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerOrderService {
    private final CustomerOrderMapper customerOrderMapper;
    private final ProjectMapper projectMapper;
    private final CustomerMapper customerMapper;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public CustomerOrder create(CustomerOrderRequest req) {
        Project project = requireOptionalProject(req.getProjectId());
        Long customerId = req.getCustomerId() != null ? req.getCustomerId() : (project == null ? null : project.getCustomerId());
        requireCustomer(customerId);
        validateProjectCustomer(project, customerId);
        String orderNo = requiredOrderNo(req.getCustomerOrderNo());
        validateUnique(customerId, orderNo, null);

        CustomerOrder row = new CustomerOrder();
        row.setCustomerId(customerId);
        row.setProjectId(project == null ? null : project.getProjectId());
        row.setCustomerOrderNo(orderNo);
        row.setCustomerOrderName(normalize(req.getCustomerOrderName()));
        row.setRemark(req.getRemark());
        row.setStatus(normalize(req.getStatus()) == null ? "启用" : normalize(req.getStatus()));
        row.setServiceStaffId(AuthContext.get() == null ? null : AuthContext.get().getId());
        row.setReviewStatus("待领取");
        row.setQuoteStatus("待拆单");
        row.setPaymentStatus("未收款");
        row.setCuttingReleaseStatus("未申请");
        row.setIsDeleted(0);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        customerOrderMapper.insert(row);
        return row;
    }

    public List<CustomerOrder> list(Long projectId, Long customerId, String keyword) {
        LambdaQueryWrapper<CustomerOrder> qw = new LambdaQueryWrapper<CustomerOrder>()
                .eq(CustomerOrder::getIsDeleted, 0);
        if (projectId != null) qw.eq(CustomerOrder::getProjectId, projectId);
        if (customerId != null) qw.eq(CustomerOrder::getCustomerId, customerId);
        String kw = normalize(keyword);
        if (kw != null) {
            qw.and(w -> w.like(CustomerOrder::getCustomerOrderNo, kw)
                    .or().like(CustomerOrder::getCustomerOrderName, kw)
                    .or().like(CustomerOrder::getRemark, kw));
        }
        qw.orderByDesc(CustomerOrder::getUpdatedAt);
        return customerOrderMapper.selectList(qw);
    }

    public CustomerOrder getById(Long id) {
        CustomerOrder row = customerOrderMapper.selectById(id);
        if (row == null || Integer.valueOf(1).equals(row.getIsDeleted())) {
            throw new IllegalArgumentException("customer order not found");
        }
        return row;
    }

    @Transactional
    public CustomerOrder update(Long id, CustomerOrderRequest req) {
        CustomerOrder row = getById(id);
        Project project = requireOptionalProject(req.getProjectId() != null ? req.getProjectId() : row.getProjectId());
        Long customerId = req.getCustomerId() != null ? req.getCustomerId() : row.getCustomerId();
        requireCustomer(customerId);
        validateProjectCustomer(project, customerId);
        String orderNo = requiredOrderNo(req.getCustomerOrderNo());
        validateUnique(customerId, orderNo, id);

        row.setCustomerId(customerId);
        row.setProjectId(project == null ? null : project.getProjectId());
        row.setCustomerOrderNo(orderNo);
        row.setCustomerOrderName(normalize(req.getCustomerOrderName()));
        row.setRemark(req.getRemark());
        row.setStatus(normalize(req.getStatus()) == null ? row.getStatus() : normalize(req.getStatus()));
        row.setUpdatedAt(LocalDateTime.now());
        customerOrderMapper.updateById(row);
        return row;
    }

    @Transactional
    public void delete(Long id) {
        CustomerOrder row = getById(id);
        Integer factoryCount = jdbcTemplate.queryForObject("""
                SELECT (SELECT COUNT(1) FROM factory_order WHERE customer_order_id=?)
                     + (SELECT COUNT(1) FROM contract WHERE customer_order_id=?)
                """, Integer.class, id, id);
        if (factoryCount != null && factoryCount > 0) throw new IllegalArgumentException("customer order has factory orders and cannot be deleted");
        row.setIsDeleted(1);
        row.setUpdatedAt(LocalDateTime.now());
        customerOrderMapper.updateById(row);
    }

    private void validateUnique(Long customerId, String orderNo, Long excludedId) {
        LambdaQueryWrapper<CustomerOrder> query = new LambdaQueryWrapper<CustomerOrder>()
                .eq(CustomerOrder::getCustomerId, customerId)
                .eq(CustomerOrder::getCustomerOrderNo, orderNo)
                .eq(CustomerOrder::getIsDeleted, 0);
        if (excludedId != null) query.ne(CustomerOrder::getId, excludedId);
        Long dup = customerOrderMapper.selectCount(query);
        if (dup != null && dup > 0) throw new IllegalArgumentException("customer order no already exists under this customer");
    }

    private Project requireOptionalProject(Long projectId) {
        if (projectId == null) return null;
        Project project = projectMapper.selectById(projectId);
        if (project == null || Integer.valueOf(1).equals(project.getIsDeleted())) throw new IllegalArgumentException("project not found");
        return project;
    }

    private void requireCustomer(Long customerId) {
        if (customerId == null) throw new IllegalArgumentException("customerId is required");
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) throw new IllegalArgumentException("customer not found");
    }

    private void validateProjectCustomer(Project project, Long customerId) {
        if (project != null && !customerId.equals(project.getCustomerId())) {
            throw new IllegalArgumentException("customer order must belong to the selected project");
        }
    }

    private String requiredOrderNo(String value) {
        String result = normalize(value);
        if (result == null) throw new IllegalArgumentException("customerOrderNo is required");
        return result;
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
