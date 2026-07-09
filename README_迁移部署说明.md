# ERP 项目交付迁移说明

本文用于把当前 ERP 系统完整迁移给他人，包含代码、数据库、配置与启动步骤。

## 1. 交付内容清单

需要交付：

1. 项目源码目录（`erp-server`、`erp-web`、`erp-doc` 等）
2. 数据库完整导出文件：`gc_erp_full.sql`
3. 本说明文件：`README_交付迁移说明.md`

不要交付（可删除后再打包）：

1. `erp-web/node_modules`
2. `erp-web/dist`
3. `erp-server/target`
4. 日志文件（`*.log`、`logs/`）
5. 本机 IDE 配置（`.idea`、`.vscode`）

## 2. 你的机器上导出数据库（关键）

在你本机执行（Windows CMD/PowerShell 都可以）：

```bash
mysqldump -u root -p --databases gc_erp --routines --triggers --events --default-character-set=utf8mb4 > gc_erp_full.sql
```

说明：

1. 输入 MySQL 密码后会导出完整库（结构 + 数据 + 过程/触发器）
2. 导出的 `gc_erp_full.sql` 请和源码一起交付

## 3. 对方机器准备环境

对方需安装：

1. MySQL 8.x
2. JDK 17（建议）
3. Maven 3.8+
4. Node.js 18+（建议 LTS）

## 4. 对方机器导入数据库

将 `gc_erp_full.sql` 放到任意目录后执行：

```bash
mysql -u root -p < gc_erp_full.sql
```

导入成功后确认数据库存在：

1. 数据库名：`gc_erp`
2. 主要表如：`contract`、`quotation`、`payment_record`、`workflow_log`

## 5. 修改配置（按对方环境）

### 5.1 后端数据库配置

文件：

`erp-server/src/main/resources/application.yml`

至少确认这三项：

1. `spring.datasource.url`
2. `spring.datasource.username`
3. `spring.datasource.password`

### 5.2 前端代理配置

文件：

`erp-web/vite.config.js`

默认代理通常是：

```js
proxy: {
  '/api': {
    target: 'http://127.0.0.1:8080',
    changeOrigin: true
  }
}
```

如果后端不在本机，请改 `target`。

## 6. 启动系统

### 6.1 启动后端

```bash
cd erp-server
mvn spring-boot:run
```

### 6.2 启动前端

```bash
cd erp-web
npm install
npm run dev
```

前端默认访问地址：

`http://localhost:5173`

## 7. 局域网访问（可选）

若需要同一局域网访问前端，把 `erp-web/vite.config.js` 的 `server` 改为：

```js
server: {
  host: '0.0.0.0',
  port: 5173
}
```

然后重启前端，并放行防火墙端口 `5173` 和后端端口 `8080`。

## 8. 常见问题排查

1. 前端能开但数据为空：检查前端代理 `target` 是否指向正确后端
2. 后端启动失败：优先检查 `application.yml` 中数据库账号密码
3. 登录后报 SQL 异常：确认导入的是完整 `gc_erp_full.sql`
4. 图片无法显示：检查 `erp-server/upload` 目录是否一并迁移（如需历史图片）

## 9. 推荐交付方式

1. 使用 Git 仓库交付源码
2. 数据库用 `gc_erp_full.sql` 单独交付
3. 再附一份“账号清单”（初始管理员账号、角色权限说明）

