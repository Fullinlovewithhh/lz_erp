package com.gcerp.erp.quote;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("quotation")
public class Quotation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private BigDecimal totalAmount;
    private String quoteDesc;
    private String createdBy;
    private LocalDateTime createdAt;
}
