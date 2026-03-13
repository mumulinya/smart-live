package com.smartLive.product.job;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.enums.product.ItemActionType;
import com.smartLive.common.core.enums.product.ProductActivityTypeEnum;
import com.smartLive.common.core.enums.product.ProductStatusEnum;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.order.api.DTO.ProductSoldDTO;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.product.domain.Product;
import com.smartLive.product.service.IProductService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 秒杀商品生命周期管理 XXL-JOB 定时任务处理器
 * <p>
 * 包含三个独立的定时任务，分别负责秒杀活动的完整生命周期：
 * 1. 预热任务：活动开始前将库存加载到 Redis，防止瞬间流量击穿 DB
 * 2. 临期提醒任务：活动即将结束时通过 MQ 推送 Feed 流，营造紧迫感提升转化率
 * 3. 回收任务：活动结束后修改 DB 状态，将 Redis 剩余库存回写 MySQL，清理缓存
 * </p>
 *
 * @author smartLive
 */
@Component
@Slf4j
public class ProductSeckillJobHandler {

    @Autowired
    private IProductService productService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RemoteOrderService remoteOrderService;

    // ==================== 任务一：秒杀预热 ====================

    /**
     * 秒杀预热定时任务
     * <p>
     * 扫描即将在未来 2 小时内开始的秒杀商品，将库存和商品详情提前加载到 Redis 中，
     * 防止活动开始瞬间的海量请求直接穿透到数据库导致服务雪崩。
     * </p>
     * <p>建议 CRON: 0 0/1 * * * ?（每分钟执行一次）</p>
     */
    @XxlJob("seckillPreHeatJobHandler")
    public ReturnT<String> seckillPreHeatJobHandler() {
        log.info("======== 触发 XXL-JOB: 秒杀预热任务 seckillPreHeatJobHandler ========");

        Date now = new Date();
        // 计算预热窗口的截止时间（当前时间 + 预热窗口小时数）
        Date preHeatDeadline = new Date(now.getTime() + RedisConstants.SECKILL_PRE_HEAT_WINDOW_HOURS * 60 * 60 * 1000);

        // 1. 查询即将开始的秒杀商品：状态正常 + 秒杀类型 + 开始时间在未来2小时内
        List<Product> products = productService.lambdaQuery()
                .eq(Product::getStatus, ProductStatusEnum.NORMAL.getCode())
                .eq(Product::getActivityType, ProductActivityTypeEnum.SECKILL.getCode())
                .ge(Product::getBeginTime, now)           // 还未开始
                .le(Product::getBeginTime, preHeatDeadline) // 但在2小时内即将开始
                .list();

        if (CollUtil.isEmpty(products)) {
            log.info("暂无需要预热的秒杀商品");
            return ReturnT.SUCCESS;
        }

        int count = 0;
        for (Product product : products) {
            String stockKey = RedisConstants.SECKILL_STOCK_KEY + product.getId();

            // 2. 幂等检查：如果 Redis 中已经存在库存 Key，说明已经预热过，跳过
            if (redisService.hasKey(stockKey)) {
                log.info("商品 {} 已预热，跳过", product.getId());
                continue;
            }

            // 3. 将秒杀库存写入 Redis（String 类型，后续通过 Lua 脚本原子扣减）
            redisService.setCacheObject(stockKey, product.getStock());
            log.info("预热商品 {} 库存 {} 至 Redis", product.getId(), product.getStock());

            // 4. 将商品详情缓存到 Redis，防止活动瞬间的详情页查询击穿 DB
            String detailKey = RedisConstants.CACHE_PRODUCT_KEY + product.getId();
            if (!redisService.hasKey(detailKey)) {
                redisService.setCacheObject(detailKey, product, RedisConstants.CACHE_PRODUCT_TTL, TimeUnit.MINUTES);
            }

            count++;
        }

        log.info("======== 秒杀预热完成，本次预热 {} 个商品 ========", count);
        return ReturnT.SUCCESS;
    }

    // ==================== 任务二：秒杀临期提醒 ====================

    /**
     * 秒杀临期提醒定时任务
     * <p>
     * 扫描距离结束时间只剩 30 分钟以内的秒杀商品，
     * 通过 MQ 向互动模块发送 SOON_END 消息，触发 Feed 流推送给关注了该商品的用户，
     * 制造"即将结束"的紧迫感，提升最后时刻的抢购转化率。
     * </p>
     * <p>
     * 注意：该任务 **绝不修改商品状态**，仅做营销触达。
     * 通过 Redis 防重 Key 避免同一商品被多次通知。
     * </p>
     * <p>建议 CRON: 0 0/5 * * * ?（每5分钟执行一次）</p>
     */
    @XxlJob("seckillSoonEndJobHandler")
    public ReturnT<String> seckillSoonEndJobHandler() {
        log.info("======== 触发 XXL-JOB: 秒杀临期提醒任务 seckillSoonEndJobHandler ========");

        Date now = new Date();
        // 计算临期窗口的截止时间（当前时间 + 临期窗口分钟数）
        Date soonEndDeadline = new Date(now.getTime() + RedisConstants.SECKILL_SOON_END_WINDOW_MINUTES * 60 * 1000);

        // 1. 查询即将结束的秒杀商品：状态正常 + 秒杀类型 + 结束时间在未来30分钟内
        List<Product> products = productService.lambdaQuery()
                .eq(Product::getStatus, ProductStatusEnum.NORMAL.getCode())
                .eq(Product::getActivityType, ProductActivityTypeEnum.SECKILL.getCode())
                .gt(Product::getEndTime, now)              // 还未结束（排除已过期的）
                .le(Product::getEndTime, soonEndDeadline)  // 但在30分钟内即将结束
                .list();

        if (CollUtil.isEmpty(products)) {
            log.info("暂无即将结束的秒杀商品");
            return ReturnT.SUCCESS;
        }

        int notifiedCount = 0;
        for (Product product : products) {
            String notifyKey = RedisConstants.SECKILL_NOTIFY_KEY + product.getId();

            // 2. 防重检查：如果已经发送过临期通知，跳过（避免每5分钟重复推送）
            if (redisService.hasKey(notifyKey)) {
                log.info("商品 {} 已发送临期通知，跳过", product.getId());
                continue;
            }

            // 3. 通过 MQ 发送 SOON_END 消息到互动模块，触发 Feed 流推送
            //    复用 ProductServiceImpl 中已有的 sendProductActionMessageToMQ 方法
            productService.sendProductActionMessageToMQ(product.getId(), ItemActionType.SOON_END);
            log.info("发送秒杀临期提醒: 商品 {} 即将在 {} 结束", product.getId(), product.getEndTime());

            // 4. 设置防重 Key，有效期为临期窗口时长（30分钟），防止重复通知
            redisService.setCacheObject(notifyKey, "1", RedisConstants.SECKILL_SOON_END_WINDOW_MINUTES, TimeUnit.MINUTES);

            notifiedCount++;
        }

        log.info("======== 秒杀临期提醒完成，本次通知 {} 个商品 ========", notifiedCount);
        return ReturnT.SUCCESS;
    }

    // ==================== 任务三：秒杀结束回收 ====================

    /**
     * 秒杀结束回收定时任务
     * <p>
     * 扫描结束时间已过期的秒杀商品，执行三步回收操作：
     * 1. 将 Redis 中的真实剩余库存回写到 MySQL，保证数据最终一致性
     * 2. 将商品状态修改为"已下架"
     * 3. 清理 Redis 中的秒杀库存 Key 和防重通知 Key，释放内存
     * </p>
     * <p>建议 CRON: 0 0/5 * * * ?（每5分钟执行一次）</p>
     */
    @XxlJob("seckillRecoveryJobHandler")
    public ReturnT<String> seckillRecoveryJobHandler() {
        log.info("======== 触发 XXL-JOB: 秒杀结束回收任务 seckillRecoveryJobHandler ========");

        Date now = new Date();

        // 1. 查询已过期的秒杀商品：状态正常 + 秒杀类型 + 结束时间早于当前
        List<Product> products = productService.lambdaQuery()
                .eq(Product::getStatus, ProductStatusEnum.NORMAL.getCode())
                .eq(Product::getActivityType, ProductActivityTypeEnum.SECKILL.getCode())
                .le(Product::getEndTime, now)  // 结束时间已过
                .list();

        if (CollUtil.isEmpty(products)) {
            log.info("暂无需要回收的秒杀商品");
            return ReturnT.SUCCESS;
        }

        int recoveredCount = 0;
        for (Product product : products) {
            try {
                String stockKey = RedisConstants.SECKILL_STOCK_KEY + product.getId();

                // 1. 修改商品状态为"已过期"，标志秒杀活动正式结束
                product.setStatus(ProductStatusEnum.EXPIRED.getCode());
                productService.updateById(product);

                // 2. 清理 Redis 缓存：删除秒杀库存 Key
                redisService.deleteObject(stockKey);
                // 清理防重通知 Key（如果存在的话）
                redisService.deleteObject(RedisConstants.SECKILL_NOTIFY_KEY + product.getId());
                // 清理商品详情缓存，让下次查询重新从 DB 加载最新状态
                redisService.deleteObject(RedisConstants.CACHE_PRODUCT_KEY + product.getId());

                recoveredCount++;
                log.info("商品 {} 秒杀回收完成：下架 + 库存回写 + 缓存清理", product.getId());

            } catch (Exception e) {
                // 单个商品回收失败不影响其他商品，记录日志后继续
                log.error("商品 {} 秒杀回收失败", product.getId(), e);
            }
        }

        log.info("======== 秒杀结束回收完成，本次回收 {} 个商品 ========", recoveredCount);
        return ReturnT.SUCCESS;
    }

    // ==================== 任务四：商品库存同步 ====================

    /**
     * 商品库存同步定时任务
     * <p>
     * 以订单表数据为准，重新计算商品真实剩余库存并同步回 MySQL。
     * 公式：真实库存 = 初始库存 - 已完成订单数量
     *
     * </p>
     * <p>建议 CRON: 0 0 2 * * ?（每天凌晨2点执行）</p>
     */
    @XxlJob("seckillStockCheckJobHandler")
    public ReturnT<String> seckillStockCheckJobHandler() {
        log.info("======== 触发 XXL-JOB: 商品库存同步任务 productStockSyncJobHandler ========");

        try {
            // 1. 只查秒杀商品
            List<Product> products = productService.lambdaQuery()
                    .eq(Product::getStatus, ProductStatusEnum.NORMAL.getCode())
                    .eq(Product::getActivityType, ProductActivityTypeEnum.SECKILL.getCode())
                    .le(Product::getBeginTime, new Date())  // 已开始
                    .ge(Product::getEndTime, new Date())    // 未结束
                    .list();

            if (CollUtil.isEmpty(products)) {
                log.info("暂无需要同步的商品");
                return ReturnT.SUCCESS;
            }

            // 2. RPC 获取各商品已完成订单数量
            List<ProductSoldDTO> soldList = remoteOrderService.countProductSold();
            Map<Long, Long> soldMap = soldList.stream()
                    .collect(Collectors.toMap(
                            ProductSoldDTO::getProductId,
                            ProductSoldDTO::getSoldCount
                    ));

            int syncCount = 0;
            for (Product product : products) {
                try {
                    // 3. 真实库存 = 初始库存 - 已售数量
                    Long soldCount = soldMap.getOrDefault(product.getId(), 0L);
                    Integer realStock = product.getStock() - soldCount.intValue();
                    realStock = Math.max(realStock, 0); // 防止负数

                    // 4. 有差异才更新，减少不必要的数据库操作
                    if (!realStock.equals(product.getStock())) {
                        log.warn("商品 {} 库存不一致！当前={} 实际={} 修正中...",
                                product.getId(), product.getStock(), realStock);

                        productService.update(new UpdateWrapper<Product>()
                                .set("stock", realStock)
                                .set("sold", soldCount)
                                .eq("id", product.getId())
                        );

                        // 5. 清理 Redis 缓存
                        redisService.deleteObject(RedisConstants.CACHE_PRODUCT_KEY + product.getId());
                        //6. 重新写入redis 缓存
                        redisService.setCacheObject(RedisConstants.CACHE_PRODUCT_KEY + product.getId(), realStock);
                        syncCount++;
                    }

                } catch (Exception e) {
                    log.error("商品 {} 库存同步失败", product.getId(), e);
                }
            }

            log.info("======== 商品库存同步完成，本次修正 {} 个商品 ========", syncCount);
            return ReturnT.SUCCESS;

        } catch (Exception e) {
            log.error("商品库存同步任务异常", e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "同步失败：" + e.getMessage());
        }
    }
}
