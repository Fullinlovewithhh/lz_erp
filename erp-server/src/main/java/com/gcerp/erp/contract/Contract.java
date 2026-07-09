package com.gcerp.erp.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contract")
public class Contract {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String contractNo;
    private Long customerId;
    private Long projectId;
    private Long customerOrderId;
    private String factoryOrderNo;
    private String customerOrderNo;
    private String customerNameSnapshot;
    private String customerPhoneSnapshot;
    private String customerAddressSnapshot;
    private String projectNameSnapshot;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private String demandDesc;
    private String status;
    private Integer productionProgress;

    // 自定义扩展字段（JSON）
    private String customFields;
    private String customText1;
    private String customText2;
    private String customText3;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String projectNo;

    @TableField(exist = false)
    private String projectName;
}
