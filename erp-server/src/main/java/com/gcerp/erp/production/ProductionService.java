package com.gcerp.erp.production;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcerp.erp.contract.ContractService;
import com.gcerp.erp.contract.ContractStatusUpdateRequest;
import com.gcerp.erp.workflow.OrderStatusConst;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductionService {

    private final ProductionTaskMapper productionTaskMapper;
    private final ContractService contractService;

    @Transactional
    public ProductionTask createTask(ProductionCreateRequest req) {
        contractService.businessAdvance(
                req.getContractId(),
                OrderStatusConst.待排产,
                OrderStatusConst.生产中,
                req.getOperator(),
                "下发生产任务"
        );

        ProductionTask task = new ProductionTask();
        task.setContractId(req.getContractId());
        task.setTaskNo(generateTaskNo());
        task.setProcessPlan(req.getProcessPlan());
        task.setAssignee(req.getAssignee());
        task.setTaskStatus("生产中");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        productionTaskMapper.insert(task);
        return task;
    }

    @Transactional
    public ProductionTask updateTaskStatus(ProductionStatusRequest req) {
        ProductionTask task = productionTaskMapper.selectById(req.getTaskId());
        if (task == null) {
            throw new IllegalArgumentException("生产任务不存在");
        }
        task.setTaskStatus(req.getTaskStatus());
        task.setUpdatedAt(LocalDateTime.now());
        productionTaskMapper.updateById(task);

        if ("已完成".equals(req.getTaskStatus())) {
            contractService.businessAdvance(
                    task.getContractId(),
                    OrderStatusConst.生产中,
                    OrderStatusConst.已完成,
                    req.getOperator(),
                    "生产任务完成"
            );
        } else if ("生产中".equals(req.getTaskStatus())) {
            ContractStatusUpdateRequest statusReq = new ContractStatusUpdateRequest();
            statusReq.setTargetStatus(OrderStatusConst.生产中);
            statusReq.setOperator(req.getOperator());
            statusReq.setRemark("更新生产进度");
            statusReq.setProductionProgress(req.getProgressPercent());
            contractService.updateStatus(task.getContractId(), statusReq);
        }
        return task;
    }

    public List<ProductionTask> listByContractId(Long contractId) {
        return productionTaskMapper.selectList(new LambdaQueryWrapper<ProductionTask>()
                .eq(ProductionTask::getContractId, contractId)
                .orderByDesc(ProductionTask::getCreatedAt));
    }

    private String generateTaskNo() {
        String prefix = "SC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = productionTaskMapper.selectCount(new LambdaQueryWrapper<ProductionTask>()
                .likeRight(ProductionTask::getTaskNo, prefix));
        return prefix + String.format("%04d", count + 1);
    }
}

