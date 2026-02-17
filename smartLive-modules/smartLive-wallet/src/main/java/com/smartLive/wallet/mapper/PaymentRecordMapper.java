package com.smartLive.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.wallet.domain.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付流水Mapper
 *
 * @author smartLive
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}
