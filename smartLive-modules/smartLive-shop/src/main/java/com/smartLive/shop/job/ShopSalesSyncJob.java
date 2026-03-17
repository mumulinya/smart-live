package com.smartLive.shop.job;

import com.smartLive.shop.service.IShopService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * 店铺销量同步定时任务。
 */
@Component
@Slf4j
public class ShopSalesSyncJob {

    @Autowired
    private IShopService shopService;

    @Autowired
    private ExecutorService executorService;

    /**
     * 执行店铺销量同步任务。
     */
    @XxlJob("shopSalesSyncJob")
    public ReturnT<String> executeSalesSync() {
        log.info("触发 XXL-JOB 定时任务：同步店铺销量数据(shopSalesSyncJob)");
        executorService.execute(() -> {
            try {
                shopService.syncSalesData();
                log.info("异步执行完成：同步店铺销量数据成功");
            } catch (Exception e) {
                log.error("异步执行失败：同步店铺销量数据异常", e);
            }
        });
        return ReturnT.SUCCESS;
    }
}
