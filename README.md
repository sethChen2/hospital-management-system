# 医院管理系统

基于 Spring Boot + Vue 2 的医院预约挂号管理系统，包含管理员、医生、患者三类角色的完整业务流程。

## 技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | Spring Boot 2.7.18、MyBatis-Plus 3.5.5、MySQL 8、Redis、JWT、EasyPOI |
| 前端 | Vue 2.7、Vue Router 3、Vuex 3、Element-UI 2.15、ECharts 5、Axios |
| 构建 | Maven Wrapper（后端）、Vue CLI 5（前端） |

## 目录结构

```
├── hospital-server/                 # 后端 Spring Boot 服务
│   ├── sql/                         # 建库、初始化与演示数据脚本
│   │   ├── 01_schema.sql
│   │   ├── 02_data.sql
│   │   └── 03_demo.sql
│   ├── src/main/java/com/hospital/  # 控制器、服务、Mapper、实体等
│   ├── src/main/resources/
│   │   ├── application.yml          # 公共配置（敏感项读取环境变量）
│   │   ├── application-local.yml.example
│   │   ├── mapper/                  # MyBatis XML
│   │   └── logback-spring.xml
│   └── pom.xml
└── hospital-web/                    # 前端 Vue 工程
    ├── src/
    ├── public/
    ├── package.json
    └── vue.config.js                # 开发端口 8081，/api 代理到 8080
```

## 环境要求

- JDK 8+
- Maven 3.6+（可直接使用项目自带的 `mvnw`）
- Node.js 16+ 与 npm
- MySQL 8.0
- Redis 5+

## 快速开始

### 1. 初始化数据库

按顺序执行 `hospital-server/sql` 下的脚本：

```sql
mysql -uroot -p < hospital-server/sql/01_schema.sql
mysql -uroot -p < hospital-server/sql/02_data.sql
mysql -uroot -p < hospital-server/sql/03_demo.sql
```

脚本会自动创建 `hospital` 库（utf8mb4）。

### 2. 配置本地密码

复制配置模板并填写本机 MySQL / Redis 密码：

```bash
cd hospital-server/src/main/resources
copy application-local.yml.example application-local.yml   # Windows
# cp application-local.yml.example application-local.yml    # macOS / Linux
```

`application-local.yml` 已被 `.gitignore` 忽略，不会提交到仓库。

也可以不建该文件，改用环境变量：

| 环境变量 | 说明 | 默认值 |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | 数据库地址、端口、库名 | localhost / 3306 / hospital |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号、密码 | root / 空 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接信息 | localhost / 6379 / 空 |
| `JWT_SECRET` | JWT 签名密钥 | 演示密钥 |
| `SPRING_PROFILES_ACTIVE` | 激活的配置环境 | local |

### 3. 启动后端

```bash
cd hospital-server
.\mvnw.cmd spring-boot:run        # Windows
# ./mvnw spring-boot:run          # macOS / Linux
```

打包运行：

```bash
cd hospital-server
.\mvnw.cmd clean package -DskipTests
java -jar target/hospital-server.jar
```

后端服务地址：<http://localhost:8080>

### 4. 启动前端

```bash
cd hospital-web
npm install
npm run serve
```

访问地址：<http://localhost:8081>

## 默认账号

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 管理员 | admin | 123456 |
| 医生 | doctor01 ~ doctor06 | 123456 |
| 患者 | patient01 ~ patient03 | 123456 |

## 说明

- 仓库仅包含前后端源码与 SQL 脚本，课程文档、图表与 PPT 等资料未纳入版本管理。
- `hospital-server/target`、`hospital-web/node_modules`、`hospital-web/dist` 等构建产物均被忽略。
