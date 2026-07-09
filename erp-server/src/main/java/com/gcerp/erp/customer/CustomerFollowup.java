package com.gcerp.erp.customer;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_followup")
public class CustomerFollowup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String content;
    private String nextPlan;
    private String creator;
    private LocalDateTime createdAt;
}
