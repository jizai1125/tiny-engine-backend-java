# AGENTS.md

本文件适用于仓库 `D:\myspace\tiny-engine-backend-java`。

## 项目事实

- 技术栈：Spring Boot + Maven 多模块。
- 根工程：`pom.xml`
- 子模块：`app`、`base`
- 启动类：`app/src/main/java/com/tinyengine/it/TinyEngineApplication.java`
- 本地开发默认 profile：`dev`
- 本地开发默认端口：`9090`
- 当前仓库真实 Java 版本：`17`

## 启动与验证

- 在 IDEA 中不要新建普通 Java 项目，直接 `Open` 仓库根目录，让 IDEA 按 Maven 项目导入根 `pom.xml`。
- 可以直接使用 IDEA 自带的 JBR 和 Maven，不要求系统全局安装 `java` / `mvn`。
- 本地数据库默认使用 MySQL，开发库名是 `tiny_engine_data_java`。
- 初始化 MySQL 时，按 `app/src/main/resources/sql/mysql/` 或 `docker-deploy-data/mysql/init/` 下 SQL 的文件名顺序导入。
- 导入 MySQL 初始化脚本时必须使用 `utf8mb4` 客户端字符集，否则中文种子数据、表注释、字段注释会乱码。
- 仓库已提供共享运行配置：`.run/TinyEngineApplication.run.xml`。
- Swagger 地址：`http://localhost:9090/swagger-ui.html`

## 修改约定

- 优先做最小改动，不扩散到无关模块。
- 涉及启动、数据库、README、SQL 初始化脚本时，优先保持 `README.zh-CN.md` 与真实仓库状态一致。
- 如果改了 MySQL 初始化相关内容，优先同步 `app/src/main/resources/sql/mysql/` 与 `docker-deploy-data/mysql/init/`。
- 如果改了登录拦截或本地开发可访问性，记得检查 Swagger/OpenAPI 路径是否仍可匿名访问。

## 常用验证

- 仅编译：`mvn -q -pl app -am -DskipTests compile`
- 启动 `app` 模块：优先用 IDEA 运行 `TinyEngineApplication`
- 如果要命令行启动，注意不要对根聚合工程直接跑 `spring-boot:run`，要针对 `app` 模块运行

## 已知注意点

- README 旧内容如果出现 `JDK 1.8`，以根 `pom.xml` 为准，真实要求是 `JDK 17`。
- 这个仓库可能存在用户未提交改动；不要回滚与当前任务无关的文件。
