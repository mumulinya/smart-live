package com.smartLive.ai.service.rag;

import com.smartLive.ai.entity.vo.VoucherVO;

import java.util.List;

/**
 * 优惠券rag服务
 */
public interface IVoucherRagService {
    /**
     * 获取优惠券列表
     *
     * @param voucherVo
     * @param userMessage
     * @return
     */
    List<VoucherVO> getVoucherList(VoucherVO voucherVo, String userMessage);


    /**
     * 订单优惠券
     *
     * @param voucherVO
     * @return
     */
     String orderVoucher(VoucherVO voucherVO);
}
