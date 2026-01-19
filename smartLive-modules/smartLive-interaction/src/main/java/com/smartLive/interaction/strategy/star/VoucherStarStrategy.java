package com.smartLive.interaction.strategy.star;

import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class VoucherStarStrategy implements StarStrategy {

    private final RemoteBlogService remoteBlogService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.VOUCHER_RESOURCE.getCode();
    }

    @Override
    public void transStarCountFromRedis2DB(Map<Long, Integer> updateMap) {
        log.info("正在调用代金券服务，同步数据");
//        // 调用博客服务的批量更新接口
//        Boolean b = remoteBlogService.updateStarCountBatch(updateMap);
//        if (b) {
//            log.info("同步数据成功");
//        } else {
//            log.info("同步数据失败");
//        }
    }
}