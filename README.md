
## 平台简介
开发 Web / 小程序多端智慧商户平台，解决本地商户引流难、用户决策复杂问题；含商户展示、AI 推荐等 4 大核心功能，微服务架构拆分10+模块，支持高并发与个性化体验。
* [后台管理页面](https://gitee.com/mumulinya/smart-live-ui)
* [前端页面](https://gitee.com/mumulinya/smart-live-html)
*  [ai模块](https://gitee.com/mumulinya/smart-live-ai)
## 系统模块
~~~
com.smartLive
├── smartLive-html            // 前台模块 [8081]     
├── smartLive-ui              // 后台模块 [80]
├── smartLive-ai              // ai模块 [9216]
├── smartLive-sentinel        // 限流模块 [8718]
├── smartLive-seata-server    // 分布式事务模块 [7091]
├── smartLive-gateway         // 网关模块 [8080]
├── smartLive-auth            // 认证中心 [9200]
├── smartLive-api             // 接口模块
│       └── smartLive-api-blog                            // 博客接口
│       └── smartLive-api-comment                         // 评论接口
│       └── smartLive-api-follow                          // 关注接口
│       └── smartLive-api-marketing                       // 营销接口
│       └── smartLive-api-order                           // 订单接口
│       └── smartLive-api-search                          // 搜索接口
│       └── smartLive-api-shop                            // 店铺接口
│       └── smartLive-api-system                          // 系统接口
│       └── smartLive-api-user                            // 用户接口
├── smartLive-common          // 通用模块
│       └── smartLive-common-core                         // 核心模块
│       └── smartLive-common-datascope                    // 权限范围
│       └── smartLive-common-datasource                   // 多数据源
│       └── smartLive-common-log                          // 日志记录
│       └── smartLive-common-redis                        // 缓存服务
│       └── smartLive-common-seata                        // 分布式事务
│       └── smartLive-common-security                     // 安全模块
│       └── smartLive-common-sensitive                    // 数据脱敏
│       └── smartLive-common-swagger                      // 系统接口
├── smartLive-modules         // 业务模块
│       └── smartLive-index                               // 首页模块 [9215]                                 // 代码生成 [9202]
│       └── smartLive-blog                                // 博客模块 [9214]    
│       └── smartLive-chat                                // 聊天模块 [9213]                                // 博客模块 [9205]    
│       └── smartLive-comment                             // 评论模块 [9212]                                  // ai模块 [9204]
│       └── smartLive-file                                // 文件服务 [9211]
│       └── smartLive-follow                              // 关注模块 [9210]                                  // ai模块 [9204]
│       └── smartLive-gen                                 // 代码生成 [9209]
│       └── smartLive-job                                 // 定时任务 [9208]
│       └── smartLive-map                                 // 地图模块 [9207]
│       └── smartLive-marketing                           // 营销模块 [9206]                                  // ai模块 [9204]
│       └── smartLive-order                               // 订单模块 [9205]                                  // ai模块 [9204]
│       └── smartLive-search                              // 搜索模块 [9204]                                  // ai模块 [9204]
│       └── smartLive-shop                                // 店铺模块 [9203]                                  // ai模块 [9204]
│       └── smartLive-system                              // 系统模块 [9202]
│       └── smartLive-user                                // 用户模块 [9201]

├── smartLive-visual       // 图形化管理模块
│       └── smartLive-visual-monitor                      // 监控中心 [9100]
├──pom.xml                // 公共依赖
~~~

## 内置功能
1. 用户管理：提供用户注册登录、个人信息维护、账号安全等基础功能。
2. 地图管理：对接第三方地图服务，支持地理定位、范围搜索、地图标点等功能。
3. 博客管理：提供博客发布、分类管理、内容编辑、阅读统计等功能。
4. 店铺管理：支持店铺入驻、店铺信息管理、营业状态配置与店铺审核流程。
5. 订单管理：实现订单创建、支付、取消等全生命周期管理。
6. 营销管理：提供优惠券、秒杀等营销功能。
7. 首页管理：负责首页展示、资源聚合、热门内容与推荐信息的统一输出。
8. 搜索管理：提供基于 ES 的统一搜索能力，包括用户、店铺、博客、优惠券等内容检索。
9. 评论管理：支持内容评论、回复、点赞、评论审核等互动功能。
10. 关注管理：提供用户关注、取关、粉丝列表、关注动态等社交互动能力。
11. 聊天管理：基于 WebSocket 实现即时通讯，支持私聊、群聊与消息记录。
12. 限流管理：基于sentinel实现，对系统接口进行限流，防止恶意攻击。
13. 事务管理：基于seata实现，保证分布式事务的可靠性。
14. 系统用户管理：用户是系统操作者，该功能主要完成系统用户配置。
15. 部门管理：配置系统组织机构（公司、部门、小组），树结构展现支持数据权限。
16. 岗位管理：配置系统用户所属担任职务。
17. 菜单管理：配置系统菜单，操作权限，按钮权限标识等。
18. 角色管理：角色菜单权限分配、设置角色按机构进行数据范围权限划分。
19. 字典管理：对系统中经常使用的一些较为固定的数据进行维护。
20. 参数管理：对系统动态配置常用参数。
21. 通知公告：系统通知公告信息发布维护。
22. 操作日志：系统正常操作日志记录和查询；系统异常信息日志记录和查询。
23. 登录日志：系统登录日志记录查询包含登录异常。
24. 在线用户：当前系统中活跃用户状态监控。
25. 定时任务：在线（添加、修改、删除)任务调度包含执行结果日志。
26. 代码生成：前后端代码的生成（java、html、xml、sql）支持CRUD下载 。
27. 系统接口：根据业务代码自动生成相关的api接口文档。
28. 服务监控：监视当前系统CPU、内存、磁盘、堆栈等相关信息。
29. 在线构建器：拖动表单元素生成相应的HTML代码。
30. 连接池监视：监视当前系统数据库连接池状态，可进行分析SQL找出系统性能瓶颈。
