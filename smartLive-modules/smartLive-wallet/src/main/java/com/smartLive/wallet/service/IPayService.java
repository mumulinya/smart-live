package com.smartLive.wallet.service;

import com.smartLive.wallet.domain.dto.UnifiedPayDTO;
import com.smartLive.wallet.domain.vo.PayStatusVO;
import com.smartLive.wallet.domain.vo.UnifiedPayVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 支付服务接口
 *
 * @author smartLive
 */
public interface IPayService {

    /**
     * 统一下单
     *
     * @param userId 用户ID
     * @param dto    下单请求
     * @return 支付参数
     */
    UnifiedPayVO unifiedOrder(Long userId, UnifiedPayDTO dto);

    /**
     * 查询支付状态
     *
     * @param paySn 支付流水号
     * @return 支付状态
     */
    PayStatusVO queryPayStatus(String paySn);

    /**
     * 处理微信支付回调
     *
     * @param request  请求
     * @param response 响应
     */
    void handleWechatCallback(HttpServletRequest request, HttpServletResponse response);

    /**
     * 处理支付宝支付回调
     *
     * @param request 请求
     * @return 响应结果 (success/fail)
     */
    String handleAlipayCallback(HttpServletRequest request);

    /**
     * 取消支付
     *
     * @param userId 用户ID
     * @param paySn  支付流水号
     */
    void cancelPay(Long userId, String paySn);

    /**
     * 查询支付记录列表
     *
     * @param userId   用户ID
     * @param page     当前页
     * @param pageSize 每页条数
     * @param status   状态筛选 (可选)
     * @return 分页结果
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.smartLive.wallet.domain.PaymentRecord> getPayList(Long userId, Integer page, Integer pageSize, Integer status);
}
