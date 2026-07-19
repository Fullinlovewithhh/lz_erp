package com.gcerp.erp.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final CustomerFollowupMapper followupMapper;
    private final CustomerRequirementMapper requirementMapper;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public Customer createCustomer(CustomerCreateRequest req) {
        Customer customer = new Customer();
        customer.setCustomerCode(generateCustomerCode());
        applyRequest(customer, req);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        customerMapper.insert(customer);
        return customer;
    }

    @Transactional
    public Customer updateCustomer(Long id, CustomerCreateRequest req) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new IllegalArgumentException("customer not found");
        }
        applyRequest(customer, req);
        customer.setUpdatedAt(LocalDateTime.now());
        customerMapper.updateById(customer);
        return customerMapper.selectById(id);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new IllegalArgumentException("customer not found");
        }
        Integer orderCnt = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM customer_order WHERE customer_id = ? AND is_deleted = 0", Integer.class, id);
        Integer legacyOrderCnt = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM contract WHERE customer_id = ?", Integer.class, id);
        if ((orderCnt != null && orderCnt > 0) || (legacyOrderCnt != null && legacyOrderCnt > 0)) {
            throw new IllegalArgumentException("该客户已有订单，不能删除");
        }
        customerMapper.deleteById(id);
    }

    public List<Customer> listCustomers(String keyword) {
        LambdaQueryWrapper<Customer> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.like(Customer::getCustomerName, keyword)
                    .or()
                    .like(Customer::getCustomerCode, keyword)
                    .or()
                    .like(Customer::getPhone, keyword)
                    .or()
                    .like(Customer::getAddress, keyword)
                    .or()
                    .like(Customer::getOwner, keyword)
                    .or()
                    .like(Customer::getLevel, keyword)
                    .or()
                    .like(Customer::getCustomFields, keyword);
        }
        qw.orderByDesc(Customer::getUpdatedAt);
        return customerMapper.selectList(qw);
    }

    @Transactional
    public CustomerFollowup createFollowup(CustomerFollowupRequest req) {
        ensureCustomerExists(req.getCustomerId());
        CustomerFollowup followup = new CustomerFollowup();
        followup.setCustomerId(req.getCustomerId());
        followup.setContent(req.getContent());
        followup.setNextPlan(req.getNextPlan());
        followup.setCreator(req.getCreator());
        followup.setCreatedAt(LocalDateTime.now());
        followupMapper.insert(followup);
        return followup;
    }

    public List<CustomerFollowup> listFollowups(Long customerId) {
        ensureCustomerExists(customerId);
        return followupMapper.selectList(new LambdaQueryWrapper<CustomerFollowup>()
                .eq(CustomerFollowup::getCustomerId, customerId)
                .orderByDesc(CustomerFollowup::getCreatedAt));
    }

    @Transactional
    public CustomerRequirement createRequirement(CustomerRequirementRequest req) {
        ensureCustomerExists(req.getCustomerId());
        CustomerRequirement requirement = new CustomerRequirement();
        requirement.setCustomerId(req.getCustomerId());
        requirement.setRequirementType(req.getRequirementType());
        requirement.setRequirementDesc(req.getRequirementDesc());
        requirement.setBudgetRange(req.getBudgetRange());
        requirement.setStylePreference(req.getStylePreference());
        requirement.setCreator(req.getCreator());
        requirement.setCreatedAt(LocalDateTime.now());
        requirementMapper.insert(requirement);
        return requirement;
    }

    public List<CustomerRequirement> listRequirements(Long customerId) {
        ensureCustomerExists(customerId);
        return requirementMapper.selectList(new LambdaQueryWrapper<CustomerRequirement>()
                .eq(CustomerRequirement::getCustomerId, customerId)
                .orderByDesc(CustomerRequirement::getCreatedAt));
    }

    private void applyRequest(Customer customer, CustomerCreateRequest req) {
        customer.setCustomerName(req.getCustomerName());
        customer.setPhone(req.getPhone());
        customer.setAddress(req.getAddress());
        customer.setLevel(req.getLevel());
        customer.setOwner(req.getOwner());
        if (req.getDefaultDiscountRate() != null) {
            if (req.getDefaultDiscountRate().signum() < 0 || req.getDefaultDiscountRate().compareTo(java.math.BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("客户标准折扣必须在0到1之间");
            }
            customer.setDefaultDiscountRate(req.getDefaultDiscountRate());
        }
        customer.setCustomFields(req.getCustomFields());
    }

    private void ensureCustomerExists(Long customerId) {
        if (customerId == null || customerMapper.selectById(customerId) == null) {
            throw new IllegalArgumentException("customer not found");
        }
    }

    private String generateCustomerCode() {
        String prefix = "KH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .likeRight(Customer::getCustomerCode, prefix));
        return prefix + String.format("%04d", count + 1);
    }
}
