package com.smartLive.wallet.controller;

import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.wallet.domain.dto.WalletAdjustDTO;
import com.smartLive.wallet.service.IWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Wallet admin controller.
 */
@RestController
@RequestMapping({"/wallet", "/admin/wallet"})
public class WalletAdminController extends BaseController {

    @Autowired
    private IWalletService walletService;

    @RequiresPermissions("wallet:balance:adjust")
    @Log(title = "Wallet balance adjust", businessType = BusinessType.UPDATE)
    @PostMapping("/adjust")
    public AjaxResult adjust(@RequestBody WalletAdjustDTO dto) {
        BigDecimal newBalance = walletService.adjustBalance(
                dto == null ? null : dto.getUserId(),
                dto == null ? null : dto.getAmount(),
                dto == null ? null : dto.getType(),
                dto == null ? null : dto.getRemark()
        );
        Map<String, Object> data = new HashMap<>();
        data.put("newBalance", newBalance);
        return success(data);
    }
}
