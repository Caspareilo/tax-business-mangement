# Docker 一键运行说明

本项目可以通过 Docker Compose 一次启动：

- MySQL 8.0：保存系统数据。
- Spring Boot 3.5：提供后端 API。
- Vue 3 + Nginx：提供网页并把 `/api` 转发给后端。

访问入口统一为：<http://localhost:18080>

## 1. 安装 Docker

Windows 推荐安装 Docker Desktop：

1. 安装并启动 Docker Desktop。
2. 等待左下角显示 `Engine running`。
3. 打开 PowerShell，执行：

```powershell
docker --version
docker compose version
docker info
```

三条命令都能正常输出，说明 Docker 可以使用。

## 2. 最简单的启动方式

项目根目录双击以下任意一个文件：

```text
START.bat
docker-start.bat
```

更推荐使用醒目的 `START.bat`。如果双击没有出现窗口，请右键选择“以管理员身份运行”，或在 PowerShell 中执行下方命令。

```text
START.bat
```

脚本会自动：

1. 构建后端和前端镜像。
2. 启动 MySQL、Spring Boot、Nginx。
3. 等待后端健康检查通过。
4. 自动打开 <http://localhost:18080>。

首次构建需要下载基础镜像和依赖，通常需要数分钟。之后再次启动会快很多。

## 3. 命令行启动

在项目根目录执行：

```powershell
docker compose up -d --build
```

查看状态：

```powershell
docker compose ps
```

查看日志：

```powershell
docker compose logs -f --tail=200
```

停止系统：

```powershell
docker compose down
```

也可以直接双击 `docker-stop.bat` 或 `docker-logs.bat`。

## 4. 访问地址

| 服务 | 地址 |
| --- | --- |
| 系统页面 | <http://localhost:18080> |
| 后端 Swagger | <http://localhost:18081/swagger-ui.html> |
| 后端健康检查 | <http://localhost:18081/actuator/health> |

MySQL 默认不开放到宿主机，只在 Docker 内部网络中供后端访问。

## 5. 演示账号

密码统一为 `Demo@1234`：

| 账号 | 角色 |
| --- | --- |
| `admin` | 系统管理员 |
| `clerk` | 业务经办人 |
| `reviewer` | 审核人员 |
| `analyst` | 统计分析员 |
| `viewer` | 只读访客 |

登录页另有“注册账号”页签。新账号默认分配 `VIEWER` 角色，注册成功后可返回登录页使用新账号登录。注册不需要管理员操作，也不能在前端选择高权限角色。

![最新登录与注册界面](docs/screenshots/01-login-register-1440x900.png)

## 6. 修改端口或数据库密码

复制：

```text
.env.docker.example -> .env
```

然后修改 `.env`：

```dotenv
APP_PORT=18080
BACKEND_PORT=18081
MYSQL_DATABASE=tax_business
MYSQL_USER=tax_app
MYSQL_PASSWORD=请替换
MYSQL_ROOT_PASSWORD=请替换
```

修改数据库密码后，旧数据卷仍使用原密码，需要同时重建数据卷，或在 MySQL 中修改用户密码。`.env` 已加入 `.gitignore`，不要提交真实密码。

## 7. 数据保存与重置

MySQL 数据保存在 Docker 命名卷 `tax-business-mysql-data` 中。执行 `docker compose down` 不会删除数据。

如需彻底清空演示数据并恢复到首次启动的种子状态：

```powershell
docker compose down -v
docker compose up -d --build
```

`down -v` 会删除项目数据库卷，操作不可恢复，只应在演示数据需要重置时使用。

## 8. 常见问题

### 提示 docker 不是命令

Docker Desktop 尚未安装，或安装后终端没有重新打开。安装并启动 Docker Desktop，再重新运行脚本。

### 提示 Docker Engine 未运行

打开 Docker Desktop，等待进度完成并显示 `Engine running`。

### 18080 或 18081 端口被占用

复制 `.env.docker.example` 为 `.env`，改成：

```dotenv
APP_PORT=28080
BACKEND_PORT=28081
```

重新运行启动脚本，访问 <http://localhost:28080>。

### 首次启动较慢

需要下载 MySQL、JDK、Maven、Node 和 Nginx 镜像，并构建前后端，属于正常现象。

### 页面打开但接口报错

运行：

```powershell
docker compose ps
docker compose logs --tail=200 backend
docker compose logs --tail=200 mysql
```

确认三个服务均为 `running` / `healthy`。

## 9. 文件说明

| 文件 | 用途 |
| --- | --- |
| `compose.yaml` | MySQL、后端、前端服务编排 |
| `backend/Dockerfile` | Maven 多阶段构建 Spring Boot JAR |
| `frontend/Dockerfile` | Node 构建 Vue，再由 Nginx 提供静态文件 |
| `frontend/nginx.conf` | SPA 路由与 `/api` 反向代理 |
| `START.bat` | Windows 推荐一键启动入口 |
| `docker-start.bat` | 兼容入口，功能与 `START.bat` 相同 |
| `docker-stop.bat` | 停止容器，保留数据库 |
| `docker-logs.bat` | 实时查看日志 |
| `.env.docker.example` | 端口和数据库密码配置示例 |

## 10. 实际验证结果

- Docker：`29.9.2`
- Docker Compose：`v5.6.1`
- MySQL：`8.0.46`
- 容器状态：`tax-mysql`、`tax-backend`、`tax-frontend` 均为 `healthy`
- 前端入口：<http://localhost:18080> 返回 HTTP 200
- 后端健康检查：<http://localhost:18081/actuator/health> 返回 `UP`
- 容器内登录：`admin / Demo@1234` 成功，加载 11 个菜单
- Dashboard API：成功返回 8 条初始演示申报
- Nginx `/api` 同源代理：登录、会话 Cookie、Dashboard 均正常
- 容器重启：三个服务重新健康，MySQL 命名卷数据保留
- `docker-start.bat` / `scripts/start-docker.ps1`：首次构建和缓存后重启均验证通过
