package com.smartLive.ai.service.rag;

import com.smartLive.ai.entity.vo.VoucherVO;

import java.util.List;

/**
 * Voucher RAG service.
 */
public interface IVoucherRagService {

    /**
     * Query vouchers.
     */
    List<VoucherVO> getVoucherList(VoucherVO voucherVo, String userMessage);

    /**
     * Place voucher order.
     */
    String orderVoucher(VoucherVO voucherVO);
}
