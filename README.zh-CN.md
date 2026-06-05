<p align="center">
  <a href="https://opentiny.design/tiny-engine" target="_blank" rel="noopener noreferrer">
    <img alt="OpenTiny Logo" src="logo.svg" height="100" style="max-width:100%;">
  </a>
</p>

<p align="center">Tiny Engine Backend Java是一个基于springboot框架的RESTful API，负责处理业务逻辑并向前端提供数据服务。

[English](README.md) | 简体中文


本地启动步骤：

* 点击 tiny-engine-backend-java 代码仓库右上角的 Fork 按钮，将上游仓库 Fork 到个人仓库
* Clone 个人仓库到本地
* 安装依赖 JDK17、Maven 3.5 以上
* 用 IDEA 按 Maven 项目方式打开仓库根目录，使用根 `pom.xml`
* 在本地 MySQL 中创建 `tiny_engine_data_java`，然后按文件名顺序执行 `docker-deploy-data/mysql/init` 下的全部 SQL
* 导入 MySQL 初始化脚本时请使用 UTF-8/utf8mb4 客户端字符集，避免中文初始化数据写坏
* 在 `app/src/main/resources/application-dev.yml` 中修改数据库连接配置
* 如果启用了 AI 服务密钥加密，启动前请在 IDEA 运行配置或部署环境中配置 `AI_SM4_KEY`，不要把真实密钥提交到仓库
* 在 `app` 模块中启动 `com.tinyengine.it.TinyEngineApplication` 进行本地开发
* 启动后访问 `http://localhost:9090/swagger-ui.html` 验证服务是否正常

### 不会 Java / Maven 也能启动

如果你是第一次接触 Java 项目，按下面做就行：

1. 打开 IDEA。
2. 不要点 `New Project`，直接点 `Open`。
3. 选择仓库根目录 `tiny-engine-backend-java`。
4. 如果 IDEA 弹窗提示 `Trust Project`、`Load Maven Project`、`Import Maven Project`，全部点同意。
5. 打开后确认左侧能看到 `pom.xml`、`app`、`base`。这说明项目已经按 Maven 方式导入，不是普通 Java 项目。
6. 打开 `File -> Project Structure -> Project`，把 `Project SDK` 设成 `JDK 17`。如果你没有单独安装 JDK，也可以直接选 IDEA 自带的 JBR。
7. 等待 IDEA 自动下载依赖；如果右下角还在转圈，就先不要启动。
8. 本地 MySQL 里创建数据库 `tiny_engine_data_java`。
9. 按文件名顺序导入 `docker-deploy-data/mysql/init` 下的全部 SQL。
10. 导入 MySQL 脚本时，请使用 `utf8mb4` 客户端字符集，否则中文初始化数据和表注释会乱码。
11. 修改 `app/src/main/resources/application-dev.yml` 里的数据库账号、密码、端口。
12. 如果启用了 AI 服务密钥加密，请在 IDEA Run Configuration 的环境变量里配置 `AI_SM4_KEY`；部署环境也要通过 Secret 或运行时环境变量注入同一个值，不要把真实密钥写进仓库。
13. 打开 `app/src/main/java/com/tinyengine/it/TinyEngineApplication.java`。
14. 点击 `main` 方法左边的绿色三角，选择运行。
15. 如果 IDEA 没自动生成运行配置，也可以直接使用仓库里的共享运行配置 `.run/TinyEngineApplication.run.xml`。
16. 启动成功后，访问 `http://localhost:9090/swagger-ui.html`。

启动成功时，日志里通常会看到这些信息：

* `The following 1 profile is active: "dev"`
* `Started TinyEngineApplication`
* `http-nio-9090`

如果启动失败，优先检查：

* `Project SDK` 是不是 `17`
* Maven 依赖是不是还没下载完
* `application-dev.yml` 的数据库配置是否与你本机一致
* MySQL 初始化脚本是否按顺序完整导入

详细请看[TinyEngine 官网-使用手册-平台开发指南-前后端代码本地启动联调](https://opentiny.design/tiny-engine#/help-center/course/dev/1200)章节


### 目录规则

开发前需要了解项目整体目录结构，并按照如下规则去进行目录规则编写代码

```
├── README.md
├── app                                                            // 基础服务
│   └── src                      
│       └── main 
│           └── java 
│               ├── com.tinyengine.it
│               │            ├── config                           // 配置文件
│               │            └── TinyEngineApplication            // 启动类，主入口
│               └── resource 
│                      ├── sql                                    // sql文件，包括创表文件和基础数据sql文件
│                      │   ├── h2                                 
│                      │   ├──mysql                               
│                      │   └──postgresql                          
│                      └── application.yml
│                      
│                                                                // 配置信息，一些主要用于全局管理springboot应用程序的各种配置，和mybatisplus相关的一些配置等等
└── base                                                         // 业务功能服务
    └── src 
        ├── main                    
        │   └── java          
        │       └── com
        │           └── tinyengine
        │               └── it
        │                   ├── common                            // 公共文件   
        │                   │   ├── base                          // 公共实体类
        │                   │   │   └── BaseEntity                
        │                   │   ├── enums                         // 公共枚举类
        │                   │   │   └── Enums                     
        │                   │   ├── exception                     // 公共异常类
        │                   │   │   └── ExceptionEnum             
        │                   │   ├── handler                       // 数据类型处理器
        │                   │   │   └── ListTypeHandler           
        │                   │   ├── log                           // 系统日志
        │                   │   │   └── SystemControllerLog       
        │                   │   └── utils                         // 工具类
        │                   │       └── Utils                     
        │                   ├── config                            // 配置类
        │                   │   └── AiChatConfig                      
        │                   ├── controller                        // 业务控制层
        │                   │   └── AppController                 
        │                   ├── gateway                           // 网关
        │                   │   └── ai
        │                   │       └── AiChatClient                  
        │                   ├── mapper                            // 数据访问层
        │                   │   └── AppMapper                     
        │                   ├── model                             // 模型实体类
        │                   │   ├── dto
        │                   │   │   └── BlockDto                  
        │                   │   └── entity
        │                   │       └── Block                     
        │                   └── service                           // 业务逻辑层
        │                       ├── app                           // app模块
        │                       │   ├── impl                      // app模块业务实现类
        │                       │   │   └── AppServiceImpl        
        │                       │   └── AppService                // app模块业务逻辑接口
        │                       ├── material                      // 物料模块
        │                       │   ├── impl                      // 物料模块业务实现类
        │                       │   │   └── BlockServiceImpl      
        │                       │   └── BlockService              // 以区块举例，这层为物料相关的业务逻辑接口
        │                       └── platform                      // 设计器模块
        │                           ├── impl                      // 设计器模块业务实现类
        │                           │   └── PlatformServiceImpl   
        │                           └── PlatformService           // 设计器模块业务逻辑接口
        └── test                                                  // test目录下放不同模块的测试用例，根据上面目录进行划分                 
            ├── java
            │    └── com 
            │        └── tinyengine
            │            └── it
            │                ├── common
            │                │   ├── base
            │                │   │   └── ResultTest
            │                │   ├── exception
            │                │   │   └── GlobalExceptionAdviceTest
            │                │   ├── handler
            │                │   │   └── ListTypeHandlerTest
            │                │   ├── log
            │                │   │   └── SystemLogAspectTest
            │                │   └── utils
            │                │       └── UtilsTest
            │                ├── controller
            │                │   └── AppControllerTest
            │                ├── gateway
            │                │   └── ai
            │                │       └── AiChatClientTest
            │                ├── mapper
            │                │   └── AppMapperTest
            │                └── service     
            │                    ├── app
            │                    │   └── impl
            │                    │       └── AppServiceImplTest
            │                    ├── material
            │                    │   └── impl
            │                    │       └── BlockServiceImplTest
            │                    └── platform
            │                        └── impl        
            │                            └── PlatformServiceImplTest              
            └── resources                     
```


### 接口返回规范

##### 1.返回格式

- 正常数据
```java
{
    "data": {
        "id": 1,
        "createdBy": "1"
    }
    "code": "200",
    "message": "操作成功",
    "error": null,
    "errMsg": null,
    "success": true
}
```
- 错误数据
```java
{
    "data": null,
    "code": "CM003",
    "message": "重复创建，请修改传入参数。",
    "error": {
        "code": "CM003",
        "message": "重复创建，请修改传入参数。"
    },
    "errMsg": "重复创建，请修改传入参数。",
    "success": false
}
```

### 使用手册

具体服务端使用文档请查看[TinyEngine 官网-使用手册-平台开发指南](https://opentiny.design/tiny-engine#/help-center/course/dev/90)


#### 本地运行时配置：

JDK17，
Maven 3.5以上即可，
mysql 8

MySQL 导入示例：
```bash
mysql --default-character-set=utf8mb4 -uroot -p tiny_engine_data_java < docker-deploy-data/mysql/init/01_create_all_tables_ddl_v1.0.0.mysql.sql
```

### 数据迁移前后数据库表映射

- 区块 ------->区块历史关联变化:
由nodejs的blocks__histories重构后直接在java数据库的t_block_history表里加了ref_id这个属性关联的区块表，就不需要blocks__histories关联表了

- 物料 ------->区块关联变化:
由nodejs的block_histories_materials__materials_user_blocks的物料区块历史关系表里的block-history_id字段去block_histories表里找block_id区块主键，再通过block_id去blocks表里找区块信息，为了查询更方便，在java数据库表里直接建了区块和物料的关系表r_material_block

***总结数据库表变化大概分为以下几种***

- 数据库表字段定义的更规范，由以前的驼峰变成下划线命名，比如isDefault -> is_default
- 数据库表字段定义的意义更清晰明了，比如app -> app_id，created_at -> created_time
- 数据库表里大部分新增了tenant_id、site_id、renter_id、platform_id字段以及设计器表t_platform和设计器历史表t_platform_history，由于要进行租户隔离
- 数据库表关系更清晰明了，删除了一些没用的表关系，比如区块和区块历史的表关系，是直接在区块历史表里t_block_history加区块的id即ref_id做为外键
- 删除了block_categories区块分类，把区块分组和分类合并成了区块分组，考虑到分组和分类差不多的功能和作用
- 预留了业务分类表t_business_category、物料与业务分类的关系表r_material_category


| nodejs数据库表 |           java数据库表           | 新增属性                                                                                    | 删除属性                                                                        |                                                                                                                          修改属性                                                                                                                           |
| :----- |:----------------------------:|:----------------------------------------------------------------------------------------|:----------------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| app_extensions |       t_app_extension        | tenant_id、site_id、renter_id                                                             |                                                                             |                                                                        app -> app_id、updated_by -> last_updated_by、created_at -> created_time、 updated_at -> last_updated_time、                                                                         |                                    
| apps |            t_app             | site_id、renter_id                                                                       | tpl-groups、created_by、updated_by                                            | platform -> platform_id、platform_history -> platform_history_id、 obs_url -> publish_url、home_page -> home_page_id、tenant -> tenant_id、createdBy -> created_by、updatedBy -> last_updated_by、 created_at -> created_time、 updated_at -> last_updated_time |
| block_groups |        t_block_group         | platform_id、tenant_id、site_id、renter_id                                                 |                                                                             |                                                               app -> app_id、 dec -> description、updated_by -> last_updated_by、created_at -> created_time、 updated_at -> last_updated_time                                                               |
| block_histories |       t_block_history        | framework、 tags、is_official、 public、is_default、tiny_reserved、platform_id、block_group_id |                                                                             |                                                           created_app -> app_id、block_id -> ref_id、updated_by -> last_updated_by、created_at -> created_time、 updated_at -> last_updated_time、                                                           |
| blocks |           t_block            | latest_version、 i18n                                                                    | created_by、updated_by、author、                                               |    name_cn -> name、current_history -> latest_history_id、occupier -> occupier_by、isOfficial -> is_official、isDefault -> is_default、createdBy -> created_by、updatedBy -> last_updated_by、 created_at -> created_time、 updated_at -> last_updated_time     |
| blocks_groups__block_groups_blocks |  r_block_group_block    | |                                                                             |block-group_id -> block_group_id、 |
|blocks_carriers_relations|t_block_carriers_relation|tenant_id、site_id、renter_id|                                                                             |block -> block_id、 host ->host_id、created_at -> created_time、updated_by -> last_updated_by、updated_at -> last_updated_time |
| block_histories_material_histories__material_histories_blocks |   r_material_history_block   |    |                                                                             |                   material-history_id ->  material_history_id、  block-history_id -> block_history_id                                                                                            |
| material_histories |      t_material_history      | image_url、build_info、tgz_url、 material_size、site_id、renter_id                           |                                                                             |                                                           material -> ref_id、 tenant -> tenant_id 、updated_by -> last_updated_by、created_at -> created_time、 updated_at -> last_updated_time                                                            |
| material_histories_components__user_components_mhs | r_material_history_component |                                                                                         |                                                                             |                                                                                     material-history_id -> material_history_id、 user-component_id ->  component_id                                                                                      |
| materials |          t_material          | material_category_id、material_size、tgz_url、unzip_tgz_root_path_url、unzip_tgz_files、tenant_id、site_id、renter_id                                                                                        | name_cn、user_components、latest                                              |                                              version -> latest_version、material_histories -> latest_history_id、isOfficial -> is_official、isDefault ->is_default、component_library -> component_library_id、                                              |
| materials_user_components__user_components_materials |     r_material_component     | |                                                                             |                                                                                                           user-component_id ->  component_id                                                                                                            |
| pages |            t_page            | latest_version、latest_history_id、tenant_id、site_id、renter_id| created_by、updated_by                                                       |                                                 app -> app_id、occupier -> occupier_by、createdBy -> created_by、updatedBy -> last_updated_by、created_at -> created_time、 updated_at -> last_updated_time                                                  |
| pages_histories |        t_page_history        | ref_id、version、app_id、depth、is_page、is_default、is_published、tenant_id、site_id、renter_id| time                                                                        |                                                                                updated_by -> last_updated_by、created_at -> created_time、 updated_at -> last_updated_time                                                                                |
| templates |       t_page_template        |name、status、is_preset、image_url、tenant_id、site_id、renter_id、platform_id、 | name_en、name_cn、thumbnail、tags、created_app、create_app、created_by、updated_by |content ->page_content、tpl_type ->type、createdBy -> created_by、updatedBy -> last_updated_by、created_at -> created_time、 updated_at -> last_updated_time |
| i18n_entries |         t_i18n_entry         | tenant_id、site_id、renter_id                                                             |                                                                             |                                                               host -> host_id、lang -> lang_id、 updated_by -> last_updated_by、created_at -> created_time、 updated_at -> last_updated_time                                                                |
| i18n_langs |         t_i18n_lang          |                                                                                         |                                                                             |                                                                                updated_by -> last_updated_by、created_at -> created_time、 updated_at -> last_updated_time                                                                                | 
| sources |         t_datasource         | platform_id、tenant_id、site_id、renter_id|                                                                             |                                                               app -> app_id、desc -> description、updated_by -> last_updated_by、created_at -> created_time、 updated_at -> last_updated_time                                                               |
| task_record |        t_task_record         | build_id、tenant_id、site_id、renter_id| uniqueId、created_by、updated_by                                              |             teamId ->team_id、taskTypeId ->task_type、taskName ->task_name、taskStatus ->task_status、taskResult ->task_result、createdBy -> created_by、updatedBy -> last_updated_by、created_at -> created_time、 updated_at -> last_updated_time             |
| user_components |         t_component          |name_en、tenant_id、site_id、renter_id | component                                                                   |                                            isOfficial ->is_official、isDefault -> is_default、library -> library_id、updated_by -> last_updated_by、created_at -> created_time、 updated_at -> last_updated_time                                             |
| users-permissions_user |            t_user            |enable、tenant_id、site_id、renter_id | provider、password、resetPasswordToken、confirmationToken、confirmed、blocked    |                                                                                updated_by -> last_updated_by、created_at -> created_time、 updated_at -> last_updated_time                                                                                |
| tenants |           t_tenant           | | created_by、updated_by                                                       |                                                         tenant_id -> org_code、createdBy -> created_by、updatedBy -> last_updated_by、created_at -> created_time、 updated_at -> last_updated_time                                                          |
| platforms                                                     | t_platform| latest_history_id、site_id、renter_id                                                                                                               |   is_java、created_by、updated_by                                                                          | theme -> theme_id、latest -> latest_version、material_history -> material_history_id、business_category -> business_category_id、tenant -> tenant_id、createdBy -> created_by、updatedBy -> last_updated_by、created_at -> created_time、updated_at -> last_updated_time    |
|platform_histories|t_platform_history|publish_url、image_url、tenant_id、site_id、renter_id | |platform -> ref_id、material_history -> material_history_id、created_at -> created_time、updated_by -> last_updated_by、updated_at -> last_updated_time |


### 🤝 参与贡献

如果你对我们的开源项目感兴趣，欢迎加入我们！🎉

参与贡献之前请先阅读[贡献指南](CONTRIBUTING.zh-CN.md)。

- 添加官方小助手微信 opentiny-official，加入技术交流群
- 加入邮件列表 opentiny@googlegroups.com

### 开源协议

[MIT](LICENSE)
