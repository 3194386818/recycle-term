# 终端回收管理系统

> 基于 Vue 3 + Spring Boot 的终端回收全流程管理平台

## 概述

本系统为电信运营商终端回收业务提供完整的数字化管理方案。系统覆盖**任务下发、扫码回收、进度追踪、数据统计**全流程，支持 PC 端与手机端自适应，管理员可通过可视化看板实时掌握回收情况，并基于数据做出决策。

**核心能力：**
- 可视化展示回收情况（折线图、饼图、柱状图）
- 数据可按时间、状态、区域等维度筛选
- 支持 Excel 文件上传批量导入任务
- PC 端与手机端自适应
- 管理员权限与访问控制（JWT 认证）
- 系统内所有文字内容支持中文

## 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 前端框架 | Vue 3 + TypeScript | Composition API + `<script setup>` |
| 构建工具 | Vite | 快速热更新，生产构建 |
| UI 组件库 | Element Plus | 丰富的中文 UI 组件 |
| 图表库 | ECharts | 折线图、饼图、柱状图 |
| 扫码库 | html5-qrcode | 支持条形码 / 二维码扫描 |
| 后端框架 | Spring Boot 3.4.5 | RESTful API 服务 |
| ORM | Spring Data JPA + Hibernate | 自动建表，规范查询 |
| 数据库 | MySQL 8.0 | utf8mb4 字符集 |
| 认证 | JWT (jjwt) | 无状态 Token 认证 |
| 部署 | Nginx + systemd | HTTPS 反向代理，服务自启 |

## 系统架构设计

```
┌─────────────────────────────────────────────────────────┐
│                      Nginx (443/80)                      │
│              SSL 终止 + 静态资源 + 反向代理                 │
├────────────────────┬────────────────────────────────────┤
│   Vue 3 SPA (前端)  │        /api → Spring Boot          │
│   Element Plus      │        JWT 认证拦截器               │
│   ECharts           │        Controller → Service        │
│   html5-qrcode      │        Repository → MySQL          │
└────────────────────┴────────────────────────────────────┘
```

**目录结构：**

```
recycle-term/
├── recycle-term-frontend/          # 前端 Vue 3 项目
│   └── src/
│       ├── api/                    # API 封装（axios）
│       ├── types/                  # TypeScript 类型定义
│       ├── views/                  # 页面组件
│       │   ├── TaskList.vue        # 任务列表（前台首页）
│       │   ├── TaskDetail.vue      # 任务详情
│       │   ├── ScanPage.vue        # 扫码回收
│       │   └── admin/              # 管理后台页面
│       │       ├── Dashboard.vue   # 数据看板
│       │       ├── TaskManage.vue  # 任务管理
│       │       ├── BatchImport.vue # 批量导入
│       │       └── OperationLogs.vue # 操作日志
│       └── router/                 # 路由配置
├── recycle-term-backend/           # 后端 Spring Boot 项目
│   └── src/main/java/com/xiaohei/recycle/
│       ├── controller/             # REST 控制器
│       ├── service/                # 业务逻辑
│       ├── repository/             # 数据访问
│       ├── entity/                 # JPA 实体
│       ├── dto/                    # 数据传输对象
│       └── config/                 # 配置（CORS、JWT 拦截器）
└── data/                           # 示例数据文件
```

## 系统页面说明

### 1. 任务列表页（前台首页）

- **统计卡片**：展示总任务、待回收、已完成、已失败数量，点击可切换筛选
- **搜索框**：基于当前筛选结果进行二次关键词搜索（产品号、姓名、地址）
- **数据表格**：展示产品号、用户名称、用户地址、接入间、应回收、已回收、状态
- **操作**：复制产品号、扫码回收、切换状态（待回收/已上门/已完成/已失败）

### 2. 扫码回收页

- 自动调用手机/电脑摄像头扫描条形码或二维码
- 支持手动输入终端串码
- 实时显示已扫描列表，支持提交保存或清空

### 3. 任务详情页

- 完整展示任务信息（用户信息、工程信息、回收状态）
- 已扫描串码记录列表，支持继续扫码或删除记录

### 4. 管理后台

- **数据看板**：近 30 天回收趋势折线图 + 状态分布饼图 + 区域统计柱状图
- **任务管理**：支持搜索、新增、编辑、删除任务
- **批量导入**：支持 Excel 文件上传和 JSON 格式批量导入
- **操作日志**：记录管理员的添加、修改、删除、登录等操作

## 项目部署指南

### 环境要求

| 组件 | 版本 |
|------|------|
| JDK | 21+ |
| Node.js | 18+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Nginx | 1.14+ |

### 1. 数据库初始化

```sql
CREATE DATABASE IF NOT EXISTS recycle_term
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

启动后端后 JPA 会自动创建表结构。

### 2. 后端部署

```bash
cd recycle-term-backend
# 修改 src/main/resources/application.yml 中的数据库连接信息
mvn clean package -DskipTests
java -jar target/recycle-term-backend-1.0.0.jar --spring.profiles.active=prod
```

生产环境建议使用 systemd 管理服务：

```ini
# /etc/systemd/system/recycle-term.service
[Unit]
Description=Recycle Term Backend
After=network.target mysqld.service

[Service]
Type=simple
ExecStart=/usr/lib/jvm/java-21/bin/java -jar /opt/recycle-term/recycle-term-backend.jar --spring.profiles.active=prod
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

### 3. 前端部署

```bash
cd recycle-term-frontend
npm install
npm run build
# 将 dist/ 目录上传到服务器
```

### 4. Nginx 配置

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate     /etc/nginx/ssl/your_cert.crt;
    ssl_certificate_key /etc/nginx/ssl/your_cert.key;

    root /var/www/recycle-term;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        client_max_body_size 10m;
    }
}

server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}
```

## 项目演示

**在线访问：** https://qqxiaoheipp.fun

**管理后台：** https://qqxiaoheipp.fun/admin/login

| 账号 | 密码 |
|------|------|
| admin | admin123 |

---

*本项目为广东省电信规划设计院有限公司实习考核作品*
