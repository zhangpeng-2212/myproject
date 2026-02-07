## myproject

### 监控平台模块（monitor-platform-backend）

该模块是一个简易的运维监控与异常检测平台示例，技术栈：

- 后端：Spring Boot（纯文件存储，不依赖数据库）
- 前端：原生 HTML + CSS + JavaScript（通过 Spring Boot 静态资源提供）

#### 构建方式

使用指定 Maven 私服 settings 进行构建，例如：

```bash
mvn -s D:\tools\apache-maven-3.6.1\conf\settings-szzx-new.xml -f monitor-platform-backend\pom.xml clean package
```

> 注意：私服地址、仓库账号等通过 `settings-szzx-new.xml` 管理，本项目的 `pom.xml` 不直接写死私服信息。

#### 启动后端

构建完成后，在 `monitor-platform-backend\target` 目录下会生成可执行 JAR 包，例如：

```bash
cd monitor-platform-backend
java -jar target\monitor-platform-backend-0.0.1-SNAPSHOT.jar
```

默认监听端口为 `8080`，可在 `monitor-platform-backend\src\main\resources\application.yml` 中调整。

#### 访问前端页面

后端启动后，直接在浏览器访问：

```text
http://localhost:8080/index.html
```

即可使用简易运维监控平台：

- 左侧：服务列表 + 新增服务表单
- 右侧：服务详情、最近指标（模拟 responseTime）、异常事件列表
- 按钮：
  - “生成模拟指标”：为当前服务生成一批随机响应时间数据
  - “触发异常检测”：基于均值 + 标准差对最新数据进行异常判定
  - “刷新数据”：重新拉取指标与异常事件