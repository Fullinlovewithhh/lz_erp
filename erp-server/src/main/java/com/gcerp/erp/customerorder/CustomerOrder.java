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
    private Long projectId;
    private String customerOrderNo;
    private String customerOrderName;
    private String remark;
    private String status;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
