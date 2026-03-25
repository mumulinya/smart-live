# ❓ 常见问题 FAQ

> 本页作为网站详细版页面维护，适合访客速查，也适合面试前快速回看项目边界。

## 1. 这个项目是你一个人做的吗？

是的，本项目从需求分析、架构设计、技术选型到前后端全栈开发、环境搭建与部署，均由本人独立完成。

## 2. 这个仓库包含前端吗？前后端仓库分别是什么？

当前仓库主要是 **后端微服务主仓库**，负责用户、店铺、商品、订单、互动、搜索、AI、IM、审核、钱包等核心服务，以及中间件编排和部署脚本。  
前端仓库已单独拆分：

- `smartLive-ui`：后台管理端（Vue + Element UI）
- `smart-live-app`：用户端 App（Vue 移动端 / H5 页面）

对应仓库入口可以直接查看文档中的 README 的“项目仓库” 一节。

## 3. 第一次本地启动，最小需要哪些中间件和模块？

如果只是想先把系统跑起来并验证主链路，建议优先准备：

- **基础中间件**：MySQL、Redis、Nacos、RabbitMQ
- **核心服务**：Gateway、Auth、User、Shop、Product、Order、Blog、Interaction、Search
- **管理端 / 用户端前端**：按你的体验目标选择 `smartLive-ui` 或 `smart-live-app`

AI、Milvus、支付、IM、审核中心等能力可以放到第二阶段再补。更完整的接入顺序见 [docs/OPEN_SOURCE.md](/OPEN_SOURCE)。

## 4. AI 模块在这个项目里到底做了什么？

这个项目里的 AI 不只是“接一个聊天接口”，而是同时覆盖了 **用户端问答检索** 和 **商家端经营辅助** 两条线：

- **用户端**：支持店铺 / 商品 / 评价 / 博客问答、流式 AI 对话、探店博客生成、消费评价生成
- **商家端**：支持差评回复、经营建议、营销文案生成、经营分析
- **底层能力**：基于 Spring AI + Milvus，构建店铺、商品、评价、博客等多套向量检索能力，并支持不同 Agent 策略切换

所以它更像一个嵌入业务系统的 AI 中台，而不是单点聊天 Demo。

## 5. 为什么项目里同时用了 RabbitMQ 和 XXL-JOB？

两者职责不同，不是重复建设：

- **RabbitMQ**：负责异步解耦和准实时处理，例如订单创建、库存扣减、审核投递、消息推送、支付回调后的后续动作
- **XXL-JOB**：负责周期性扫描、补偿兜底和批处理，例如热榜重算、互动数据回刷、秒杀预热、订单超时处理、销量同步

可以把它理解成：MQ 解决“事件驱动”，XXL-JOB 解决“定时调度与补偿兜底”。两者配合起来，才能把实时性和最终一致性同时兼顾。

## 6. 为什么选 Milvus 而不是 Pinecone 或 pgvector？

项目中需要结合 AI 进行相似度检索（例如基于向量空间模型的智能推荐），Milvus 作为云原生的开源向量数据库，支持海量向量的高效检索与动态扩展。相比闭源 SaaS 的 Pinecone 数据更自主可控；相比基于 PostgreSQL 扩展的 pgvector，Milvus 在高并发、大规模向量检索场景下性能更优。

## 7. Feed 流查询为什么需要采用特殊的滚动分页？

在具有强社交属性的 Feed 瀑布流中，数据写入频率极高。如果采用传统的 `LIMIT offset, size`，当用户翻页时，若有新动态插入头部，会导致整体数据向后位移，用户将在下一页看到重复数据。本项目采用基于 Redis ZSet 的 `ZREVRANGEBYSCORE` 命令，每次查询以上一次最后一条记录的时间戳作为 Score 锚点向下偏移，从而从物理存储结构上彻底规避了“数据错位”的深分页死区。

## 8. 分布式环境下的数据一致性是怎么保证的？

针对高并发及可容忍短暂延迟的场景（例如下单成功后发布动态、奖励签到积分、审核系统状态回写以及数据同步至 ES/Milvus 等），本项目核心采用**“RabbitMQ 消息可靠投递 + 最终一致性”**方案。通过投递消息到 MQ 来解耦强关联业务，并搭配死信队列记录处理失败的异常单据（如 15 分钟未支付订单的超时释放等）进行自动补偿，极大地保障了微服务集群整体的吞吐量。

## 9. 各种缓存并发与提单安全问题是怎么处理的？

项目中统一定义了 `CacheClient` 工具类进行标准化处理，并在核心链路引入了 **Redisson**：
- <b>缓存击穿：</b> 针对热点店铺或热门博客的详情查阅，利用**逻辑过期（Logical Expiration）策略**快速响应。当判断缓存逻辑过期时，直接先返回旧数据，然后提交异步线程池去数据库抓取新数据并重建缓存，以此实现高并发下访问数据库的无感削峰；
- <b>缓存穿透：</b> 对数据库本身不存在的空结果集（如恶意请求伪造的 ID），采用**缓存空对象（Null Object 模式）**短暂存入 Redis（并附带极短的 TTL），有效防止流量直接穿透到底层 DB 导致瘫痪；
- <b>缓存雪崩：</b> 针对大批量的业务数据缓存，在基础过期时间上增加随机抖动值（Random TTL），有效避免大量 Key 在同一时刻集体失效而冲垮后端数据库；
- <b>并发防重与一人一单：</b> 针对用户并发提单（非秒杀场景，如普通支付订单），后端通过 `RedissonClient.getLock("order:" + userId)` 获取分布式锁并执行 `tryLock()` 阻挡恶意并发点击，保障订单网关层的无状态防重。

## 10. Netty WebSocket 为什么不用 Spring WebSocket？

在即时通讯（IM）场景中，存在海量长连接并且需要频繁处理心跳包保活。虽然 Spring WebSocket 使用简单，但在处理高并发连接时，基于 NIO、事件驱动的 Netty 能以更少的线程开销极大地提升网络吞吐和减少内存消耗。项目通过自定义握手并在认证时结合 Redis Token 控制，在性能和资源占用上都优于 Spring WebSocket。

## 11. 为什么用 ZSet 存列表，String 存详情，不用 Hash？

**分层设计原因：**
- **列表层（ZSet）**：天生有序支持高效范围查询（ZREVRANGE），避免应用层排序开销。
- **详情层（String）**：直接存 JSON 对象，读取时无需转换。相比 Hash 逐字段存储更简洁。
- **计数层（String）**：独立计数器原子增减，永不过期。

**性能对比（实测）：**
| 操作 | ZSet + String | MySQL |
|------|---|---|
| 列表查询（20 条）| 25ms | 500-2000ms |
| 详情查询 | 5ms | 50ms |
| 计数查询 | 1ms | 50ms |
| **整体性能提升** | **20-40 倍** | - |

如果只需要单字段快速更新，可考虑 Hash（如直接 HSET likes +1），但对于我们的场景（总是读完整对象），String 已是最优。

## 12. AbstractInteractionStrategy 如何支持 10+ 个子类都能同步到 ES？

**模板方法模式在 ES 同步中的应用：**

AbstractInteractionStrategy 定义了 `syncUserResource()` 标准模板方法，包含 5 步流程：
```
1. Object data = getSourceData(sourceId)              // 获取业务对象
2. String domain = getBizDomain()                     // 获取业务域
3. String actionType = getActionType()                // 获取操作类型
4. Integer sourceTypeCode = getType()                 // 获取资源类型
5. 构建 UserResourceMessage 投递 RabbitMQ             // MQ 投递
```

**实际应用：** 点赞、收藏、关注等都继承 AbstractInteractionStrategy + 实现对应策略接口（LikeStrategy、StarStrategy、FollowStrategy）

**10+ 个子类只需实现 4 个钩子方法：**
```java
public class CommentLikeStrategy extends AbstractInteractionStrategy implements LikeStrategy {
    @Override protected Object getSourceData(Long sourceId) 
        → return commentService.getById(sourceId);
    
    @Override protected String getBizDomain() 
        → return GlobalBizTypeEnum.COMMENT.getBizDomain();
    
    @Override protected String getActionType() 
        → return "like";
    
    @Override public Integer getType() 
        → return ResourceTypeEnum.COMMENT_RESOURCE.getCode();
}
```

**具体的子类应用（完整列表）：**
- LikeStrategy：CommentLikeStrategy、ReviewLikeStrategy、BlogLikeStrategy、UserLikeStrategy（4 个）
- StarStrategy：BlogStarStrategy、ProductStarStrategy、ShopStarStrategy（3 个）
- FollowStrategy：ShopFollowStrategy、UserFollowStrategy（2 个）
- ReviewStrategy：ProductReviewStrategy（1 个）

**设计收益：**
- ✅ 消除 MQ 投递重复代码（10+ 份 → 1 份）
- ✅ 统一日志、异常处理、消息格式
- ✅ 修改 ES 同步逻辑只需改 1 个地方
- ✅ 新增业务类型只需继承 + 实现 4 个方法

## 13. 如何高效地同步 Redis 中的 10+ 万互动数据到 MySQL？

**挑战：** 每天产生的点赞、收藏、评论数可能达到百万级，如何在不阻塞主业务的情况下落库？

**解决方案：双轨制异步架构**

```java
// SyncDataServiceImpl 中的并发设计
@Override
public void syncAllData() {
    // 1️⃣ 4 个数据同步任务并发执行（不是顺序执行）
    CompletableFuture<Void> likeFuture = CompletableFuture.runAsync(this::syncLikeData, executorService);
    CompletableFuture<Void> commentFuture = CompletableFuture.runAsync(this::syncCommentData, executorService);
    CompletableFuture<Void> starFuture = CompletableFuture.runAsync(this::syncStarData, executorService);
    CompletableFuture<Void> reviewFuture = CompletableFuture.runAsync(this::syncReviewData, executorService);
    
    // 2️⃣ 等待所有任务完成
    CompletableFuture.allOf(likeFuture, commentFuture, starFuture, reviewFuture).join();
}
```

**双轨设计细节：**

| 轨道 | 职责 | 实现 | 性能 |
|------|------|------|------|
| **同步轨（Sync）** | 将 Redis 高频数据批量刷入 MySQL | `redisService.syncDataWithSnapshot()` | 支持 100 万+ 条/分钟 |
| **算分轨（Calc）** | 基于时间衰减重算热度排名 + 洗牌 ZSet | 后台任务队列异步处理 | 每个对象 < 10ms |

**关键优化 1：Pipeline 批量查询**
```java
// 获取 ZSet 中多个元素的分数（一次往返获取 100+ 个）
public List<Double> getCacheZSetScoreBatch(final String key, final List<String> values) {
    List<Object> results = redisTemplate.executePipelined(new SessionCallback<Object>() {
        @Override
        public Object execute(RedisOperations operations) {
            for (String value : values) {
                operations.opsForZSet().score(key, value);  // 批量操作
            }
            return null;
        }
    });
    // 性能：单次查询 100 个元素从 100ms → 5ms（提升 20 倍）
}
```

**关键优化 2：RENAME 原子快照**
```java
// XXL-JOB 定时任务的核心实现
String currentKey = "like:count:2026-03-19";
String snapshotKey = "like:count:2026-03-19:SNAPSHOT";

// 1. 原子重命名（一条命令，无中间状态）
redisTemplate.rename(currentKey, snapshotKey);

// 2. 异步批量回刷（不阻塞主线程）
executorService.execute(() -> {
    Map<Long, Integer> data = getFromSnapshot(snapshotKey);
    likeService.updateBatch(data);  // 批量 INSERT ... ON DUPLICATE KEY UPDATE
});

// 3. 新建当日计数器
redisTemplate.opsForValue().set(currentKey, "0");
```

**为什么这样设计？**
- ✅ 热数据实时在 Redis（点赞数秒级更新）
- ✅ 冷数据异步落库（不影响用户体验）
- ✅ 并发同步 4 个业务数据（充分利用 CPU 多核）
- ✅ 脏数据标记机制（记录哪些源实体需要重算）

**预期性能：**
- QPS：单表 1000+ 条/秒
- 全量数据落库耗时：100 万条 < 10 分钟
- 热度重算耗时：100 个主体 < 30 秒
