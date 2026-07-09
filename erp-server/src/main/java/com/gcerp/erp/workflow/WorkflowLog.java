package com.gcerp.erp.workflow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("workflow_log")
public class WorkflowLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private String fromStatus;
    private String toStatus;
    private String operator;
    private String remark;
    private LocalDateTime createdAt;
}
