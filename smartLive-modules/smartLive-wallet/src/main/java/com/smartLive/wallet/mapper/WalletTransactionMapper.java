package com.smartLive.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.wallet.domain.WalletTransaction;
import org.apache.ibatis.annotations.Mapper;

/**
 * Wallet transaction mapper.
 */
@Mapper
public interface WalletTransactionMapper extends BaseMapper<WalletTransaction> {
}
