package com.gcerp.erp.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcerp.erp.customer.Customer;
import com.gcerp.erp.customer.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectMapper projectMapper;
    private final CustomerMapper customerMapper;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public Project createProject(ProjectCreateRequest req) {
        Customer customer = customerMapper.selectById(req.getCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        Project project = new Project();
        String projectNo = generateProjectNo();
        project.setProjectNo(projectNo);
        String projectName = req.getProjectName() == null ? "" : req.getProjectName().trim();
        project.setProjectName(projectName.isEmpty() ? "未命名项目-" + projectNo : projectName);
        project.setCustomerId(req.getCustomerId());
        project.setProjectAddress(req.getProjectAddress());
        project.setProjectStatus("进行中");
        project.setProjectManager(req.getProjectManager());
        project.setRemark(req.getRemark());
        project.setIsDeleted(0);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.insert(project);
        project.setCustomerName(customer.getCustomerName());
        return project;
    }

    public List<Project> listProjects(Long customerId, String keyword) {
        LambdaQueryWrapper<Project> qw = new LambdaQueryWrapper<Project>()
                .eq(Project::getIsDeleted, 0);
        if (customerId != null) {
            qw.eq(Project::getCustomerId, customerId);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Project::getProjectNo, keyword)
                    .or()
                    .like(Project::getProjectName, keyword)
                    .or()
                    .like(Project::getProjectAddress, keyword)
                    .or()
                    .like(Project::getProjectManager, keyword));
        }
        qw.orderByDesc(Project::getUpdatedAt);
        List<Project> rows = projectMapper.selectList(qw);
        for (Project p : rows) {
            Customer c = customerMapper.selectById(p.getCustomerId());
            if (c != null) p.setCustomerName(c.getCustomerName());
        }
        return rows;
    }

    public Project getById(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || Integer.valueOf(1).equals(project.getIsDeleted())) {
            throw new IllegalArgumentException("项目不存在");
        }
        Customer c = customerMapper.selectById(project.getCustomerId());
        if (c != null) project.setCustomerName(c.getCustomerName());
        return project;
    }

    @Transactional
    public void deleteProject(Long projectId) {
        Project project = getById(projectId);
        Integer orderCnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM contract WHERE project_id = ?",
                Integer.class,
                projectId);
        if (orderCnt != null && orderCnt > 0) {
            throw new IllegalArgumentException("该项目已有工厂订单，不能删除");
        }
        project.setIsDeleted(1);
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
    }

    private String generateProjectNo() {
        String prefix = "PJ" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                .likeRight(Project::getProjectNo, prefix));
        return prefix + String.format("%04d", count + 1);
    }
}
