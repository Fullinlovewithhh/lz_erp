package com.gcerp.erp.customer;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_requirement")
public class CustomerRequirement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String requirementType;
    private String requirementDesc;
    private String budgetRange;
    private String stylePreference;
    private String creator;
    private LocalDateTime createdAt;
}
