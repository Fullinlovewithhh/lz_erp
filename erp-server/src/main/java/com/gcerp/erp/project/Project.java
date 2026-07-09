package com.gcerp.erp.project;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project")
public class Project {
    @TableId(type = IdType.AUTO)
    private Long projectId;
    private String projectNo;
    private String projectName;
    private Long customerId;
    private String projectAddress;
    private String projectStatus;
    private String projectManager;
    private String remark;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String customerName;
}
