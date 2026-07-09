package com.gcerp.erp.payment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_record")
public class PaymentRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private BigDecimal amount;
    private String payStatus;
    private String payChannel;
    private String checkedBy;
    private LocalDateTime checkedAt;

    private String customFields;
    private String customText1;
    private String customText2;
    private String customText3;
}
