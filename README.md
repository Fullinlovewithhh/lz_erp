# Lonzvine_Fac_erp

全屋定制工厂 ERP 系统（前后端分离）。

## 项目结构

1. `erp-server`：Java Spring Boot 后端
2. `erp-web`：前端（Vite）
3. `erp-doc`：项目文档

## 本地启动

### 1. 启动后端

1. 在 MySQL 中执行：`erp-server/init.sql`
2. 修改：`erp-server/src/main/resources/application.yml`（数据库连接）
3. 启动：

```powershell
cd erp-server
mvn spring-boot:run
```

### 2. 启动前端

```powershell
cd erp-web
npm install
npm run dev
```

默认访问：`http://localhost:5173`

## 已实现功能（核心）

1. 合同/订单创建与状态流转
2. 报价管理、报价明细与报价日志
3. 财务收款审核与凭证上传
4. 生产任务下发与状态更新
5. 客户管理、跟进记录、需求管理
6. 基础资料管理（人员、产品、材料）
7. 批量导入与自定义字段预留

## 登录与权限

系统支持账号密码登录和角色权限控制，管理员可在“账号权限”页面维护账号、角色、启停状态、密码重置。

## 数据库迁移

数据库迁移说明见：

- `README_迁移部署说明.md`

