package com.smartLive.product.job;

import com.smartLive.product.service.IProductService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * 商品销量同步定时任务 (XXL-JOB)
 */
@Component
@Slf4j
public class ProductSalesSyncJob {

    @Autowired
    private IProductService productService;

    @Autowired
    private ExecutorService executorService;

    /**
     * 定时同步商品销量数据从 Redis 到数据库
     */
    @XxlJob("productSalesSyncJob")
    public ReturnT<String> executeSalesSync() {
        log.info("触发 XXL-JOB 定时任务：同步商品销量数据(productSalesSyncJob)");
        executorService.execute(() -> {
            try {
                productService.syncSalesData();
                log.info("异步执行完成：同步商品销量数据成功");
            } catch (Exception e) {
                log.error("异步执行失败：同步商品销量数据异常", e);
            }
        });
        return ReturnT.SUCCESS;
    }
}
