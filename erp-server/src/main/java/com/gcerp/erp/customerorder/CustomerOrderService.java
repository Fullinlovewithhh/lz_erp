package com.gcerp.erp.customerorder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcerp.erp.project.Project;
import com.gcerp.erp.project.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerOrderService {
    private final CustomerOrderMapper customerOrderMapper;
    private final ProjectMapper projectMapper;

    @Transactional
    public CustomerOrder create(CustomerOrderRequest req) {
        Project project = projectMapper.selectById(req.getProjectId());
        if (project == null || Integer.valueOf(1).equals(project.getIsDeleted())) {
            throw new IllegalArgumentException("project not found");
        }
        Long customerId = req.getCustomerId() != null ? req.getCustomerId() : project.getCustomerId();
        if (!customerId.equals(project.getCustomerId())) {
            throw new IllegalArgumentException("customer order must belong to the selected project");
        }
        String orderNo = normalize(req.getCustomerOrderNo());
        if (orderNo == null) throw new IllegalArgumentException("customerOrderNo is required");
        Long dup = customerOrderMapper.selectCount(new LambdaQueryWrapper<CustomerOrder>()
                .eq(CustomerOrder::getProjectId, project.getProjectId())
                .eq(CustomerOrder::getCustomerOrderNo, orderNo)
                .eq(CustomerOrder::getIsDeleted, 0));
        if (dup != null && dup > 0) throw new IllegalArgumentException("customer order no already exists under this project");

        CustomerOrder row = new CustomerOrder();
        row.setCustomerId(customerId);
        row.setProjectId(project.getProjectId());
        row.setCustomerOrderNo(orderNo);
        row.setCustomerOrderName(normalize(req.getCustomerOrderName()));
        row.setRemark(req.getRemark());
        row.setStatus(normalize(req.getStatus()) == null ? "启用" : normalize(req.getStatus()));
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
                    .or()
                    .like(CustomerOrder::getCustomerOrderName, kw)
                    .or()
                    .like(CustomerOrder::getRemark, kw));
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
        Project project = projectMapper.selectById(req.getProjectId() != null ? req.getProjectId() : row.getProjectId());
        if (project == null || Integer.valueOf(1).equals(project.getIsDeleted())) {
            throw new IllegalArgumentException("project not found");
        }
        Long customerId = req.getCustomerId() != null ? req.getCustomerId() : project.getCustomerId();
        if (!customerId.equals(project.getCustomerId())) {
            throw new IllegalArgumentException("customer order must belong to the selected project");
        }
        String orderNo = normalize(req.getCustomerOrderNo());
        if (orderNo == null) throw new IllegalArgumentException("customerOrderNo is required");
        Long dup = customerOrderMapper.selectCount(new LambdaQueryWrapper<CustomerOrder>()
                .ne(CustomerOrder::getId, id)
                .eq(CustomerOrder::getProjectId, project.getProjectId())
                .eq(CustomerOrder::getCustomerOrderNo, orderNo)
                .eq(CustomerOrder::getIsDeleted, 0));
        if (dup != null && dup > 0) throw new IllegalArgumentException("customer order no already exists under this project");
        row.setCustomerId(customerId);
        row.setProjectId(project.getProjectId());
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
        row.setIsDeleted(1);
        row.setUpdatedAt(LocalDateTime.now());
        customerOrderMapper.updateById(row);
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
