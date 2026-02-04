package com.smartLive.order.controller;

import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.core.web.page.TableDataInfo;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.order.domain.VO.OrderVO;
import com.smartLive.order.domain.Order;
import com.smartLive.order.service.IOrderService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单表Controller
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/inner/order")
public class InnerOrderController extends BaseController
{
    @Autowired
    private IOrderService orderService;

    /**
     * 获取订单数量
     * @param userId
     * @return
     */
    @GetMapping("/getOrderCount/{userId}")
    Integer getCommentCount(@PathVariable("userId")Long userId){
        return orderService.getOrderCount(userId);
    }
    
    /**
     * 获取订单总数
     * @return
     */
    @GetMapping("/getOrderTotal")
    Integer getOrderTotal(){
        return orderService.getOrderTotal();
    }
    
    /**
     * 修改订单评论状态
     * @param orderId
     * @return
     */
    @PutMapping("/updateOrderReviewStatus/{orderId}")
    Integer updateOrderReviewStatus(@PathVariable("orderId") Long orderId) {
        return orderService.updateOrderReviewStatus(orderId);
    }
}