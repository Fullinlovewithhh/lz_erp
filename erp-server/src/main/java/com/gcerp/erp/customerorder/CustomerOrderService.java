package com.gcerp.erp.customerorder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcerp.erp.auth.AuthContext;
import com.gcerp.erp.customer.Customer;
import com.gcerp.erp.customer.CustomerMapper;
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
    private final CustomerMapper customerMapper;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public CustomerOrder create(CustomerOrderRequest req) {
        Long customerId = req.getCustomerId();
        requireCustomer(customerId);
        String orderNo = requiredOrderNo(req.getCustomerOrderNo());
        validateUniqueOrderNo(orderNo, null);

        CustomerOrder row = new CustomerOrder();
        row.setCustomerId(customerId);
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

    public List<CustomerOrder> list(Long customerId, String keyword) {
        LambdaQueryWrapper<CustomerOrder> qw = new LambdaQueryWrapper<CustomerOrder>()
                .eq(CustomerOrder::getIsDeleted, 0);
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
        Long customerId = req.getCustomerId() != null ? req.getCustomerId() : row.getCustomerId();
        requireCustomer(customerId);
        String orderNo = requiredOrderNo(req.getCustomerOrderNo());
        validateUniqueOrderNo(orderNo, id);

        row.setCustomerId(customerId);
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

    private void requireCustomer(Long customerId) {
        if (customerId == null) throw new IllegalArgumentException("customerId is required");
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) throw new IllegalArgumentException("customer not found");
    }

    private void validateUniqueOrderNo(String orderNo, Long excludedId) {
        LambdaQueryWrapper<CustomerOrder> query = new LambdaQueryWrapper<CustomerOrder>()
                .eq(CustomerOrder::getCustomerOrderNo, orderNo);
        if (excludedId != null) query.ne(CustomerOrder::getId, excludedId);
        Long count = customerOrderMapper.selectCount(query);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("客户订单号已存在，不允许重复");
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
