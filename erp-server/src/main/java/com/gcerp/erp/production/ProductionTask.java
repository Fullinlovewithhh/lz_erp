package com.gcerp.erp.production;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("production_task")
public class ProductionTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private String taskNo;
    private String processPlan;
    private String assignee;
    private String taskStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
