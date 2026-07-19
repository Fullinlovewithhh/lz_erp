package com.gcerp.erp.customer;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@TableName("customer")
public class Customer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String customerCode;
    private String customerName;
    private String phone;
    private String address;
    private String level;
    private String owner;
    private BigDecimal defaultDiscountRate;
    private String customFields;
    private String customText1;
    private String customText2;
    private String customText3;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
