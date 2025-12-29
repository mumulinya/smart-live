package com.smartLive.ai.tools;

import com.smartLive.ai.entity.vo.VoucherVO;
import com.smartLive.ai.service.rag.impl.VoucherRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoucherTools {


    private final VoucherRagService voucherRagService;
    @Tool(name = "listVoucher",description = "列出某类商铺或特定店铺的可用优惠券。示例：'星巴克有什么优惠？'")
    public List<VoucherVO> listVoucher(
            @ToolParam(description = "店铺名称", required = false)
            String shopName,
            @ToolParam(description = "店铺类型", required = false)
            Long typeId,
            @ToolParam(description = "0,普通券；1,秒杀券", required = false)
            Integer type,
            @ToolParam(description = "用户所在的地区", required = false)
            String district,
            @ToolParam(description = "用户原始消息，用于RAG查询", required = false)
            String userMessage
    ) throws ExecutionException, InterruptedException, TimeoutException {
        log.info("查询的店铺名称为{},券的类型：{}，查询的类别：{}", shopName, type, typeId);
        VoucherVO voucher = new VoucherVO();
        voucher.setShopName(shopName);
        voucher.setType(type);
        voucher.setTypeId(typeId);
        //rag查询
        List<VoucherVO> voucherList = voucherRagService.getVoucherList(voucher, userMessage);
        voucherList.forEach(voucherVO -> log.info("🔍 返回搜索结果：{}", voucherVO));
        return voucherList;
    }



    @Tool(name = "orderVoucher",description = "下单代金券，示例：我要下单xxx店铺的xxx代金券")
    public String orderVoucher(
            @ToolParam(description = "店铺id，精确查询", required = false)
            Long shopId,
            @ToolParam(description = "店铺名称", required = false)
            String shopName,
            @ToolParam(description = "0,普通券；1,秒杀券  默认为普通券", required = false)
            Integer type,
            @ToolParam(description = "代金券名称，", required = false)
            String voucherName,
            @ToolParam(description = "用户id", required = false)
            Long userId,
            @ToolParam(description = "用户原始消息，用于RAG查询", required = false)
            String userMessage
    ) {
        log.info("抢购优惠券 | shopId={}, shopName={}, type={}, voucherName={}, userId={},userMessage={}",
                shopId, shopName, type, voucherName, userId,userMessage);
        VoucherVO query = new VoucherVO();
        query.setShopId(shopId);
        query.setShopName(shopName);
        query.setTitle(voucherName);
        query.setType(type);
        query.setUserId(userId);
        return   voucherRagService.orderVoucher( query);
    }
}
