package com.smartLive.points.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.points.api.factory.RemotePointsFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 积分服务远程调用接口
 *
 * @author smartLive
 */
@FeignClient(contextId = "remotePointsService", value = ServiceNameConstants.POINTS_SERVICE, fallbackFactory = RemotePointsFallbackFactory.class)
public interface RemotePointsService {

    /**
     * 新增积分（订单完成时调用）
     *
     * @param userId  用户ID
     * @param amount  积分数量
     * @param bizId   业务ID（订单号）
     * @param description 描述
     * @return 操作结果
     */
    @PostMapping("/inner/points/add")
    Boolean addPoints(@RequestParam("userId") Long userId,
                         @RequestParam("amount") Integer amount,
                         @RequestParam("bizId") String bizId,
                         @RequestParam("description") String description);
}
