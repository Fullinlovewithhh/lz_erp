package com.gcerp.erp.customerorder;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_order")
public class CustomerOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String customerOrderNo;
    private String customerOrderName;
    private String remark;
    private String status;
    private Long serviceStaffId;
    private Long reviewEngineerId;
    private String reviewStatus;
    private String splitInstruction;
    private String settlementType;
    private String quoteStatus;
    private java.math.BigDecimal quoteTotalAmount;
    private java.math.BigDecimal priceAdjustmentAmount;
    private java.math.BigDecimal finalReceivableAmount;
    private String paymentStatus;
    private String cuttingReleaseStatus;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
