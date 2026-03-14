package com.smartLive.ai.strategy.merchant.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.ai.domain.AiMerchantSession;
import com.smartLive.ai.domain.DTO.MerchantChatDTO;
import com.smartLive.ai.service.chat.support.MerchantMessageChatMemoryManager;
import com.smartLive.ai.service.merchant.IAiMerchantMessageService;
import com.smartLive.ai.service.merchant.IAiMerchantSessionService;
import com.smartLive.ai.service.rag.IShopRagService;
import com.smartLive.ai.strategy.merchant.AbstractMerchantAiStrategy;
import com.smartLive.shop.api.DTO.BadReviewDTO;
import com.smartLive.shop.api.DTO.ProductSalesDTO;
import com.smartLive.shop.api.DTO.ShopAnalysisDTO;
import com.smartLive.shop.api.DTO.ShopSuggestDTO;
import com.smartLive.shop.api.RemoteShopService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component("suggestStrategy")
public class SuggestAiStrategy extends AbstractMerchantAiStrategy {

    public SuggestAiStrategy(@Qualifier("merchantStrategyChatClient") ChatClient merchantStrategyChatClient,
                             IAiMerchantSessionService merchantSessionService,
                             IAiMerchantMessageService merchantMessageService,
                             MerchantMessageChatMemoryManager memoryManager,
                             RemoteShopService remoteShopService,
                             IShopRagService shopRagService,
                             ObjectMapper objectMapper) {
        super(merchantStrategyChatClient, merchantSessionService, merchantMessageService, memoryManager,
                remoteShopService, shopRagService, objectMapper);
    }

    @Override
    protected String buildScenePrompt(MerchantChatDTO dto, AiMerchantSession session) {
        ShopSuggestDTO suggest = convertAjaxData(
                remoteShopService.getShopSuggest(session.getShopId()),
                ShopSuggestDTO.class,
                new ShopSuggestDTO()
        );
        ShopAnalysisDTO analysis = convertAjaxData(
                remoteShopService.getShopAnalysis(session.getShopId(), "week"),
                ShopAnalysisDTO.class,
                new ShopAnalysisDTO()
        );
        ShopPromptContext shopContext = getShopPromptContext(session.getShopId());

        return """
                场景 4：经营建议 OPERATION_SUGGESTION

                当前任务：基于店铺当前经营数据，输出一份偏执行层的改进方案。

                【店铺信息】
                店铺名称：%s
                店铺类型：%s
                建议周期：本周

                【经营摘要】
                营业额：%s
                订单数：%s
                客单价：%s
                复购率：暂无数据（复购人数：%s）
                退款率：暂无数据
                差评率：暂无数据（低分评价数：%s）
                平均评分：%s
                热销商品：%s
                滞销商品：%s
                主要问题：%s

                【商家补充要求】
                %s

                【输出要求】
                请按以下格式输出：
                1. 本周优先动作：3条。
                2. 本月优化方向：3条。
                3. 重点关注指标：3个，并说明关注原因。
                4. 预期效果：1段，说明如果执行到位，最可能改善什么。

                补充规则：
                1. 建议必须具体，能落地执行，避免泛泛而谈。
                2. 建议优先围绕评价、商品、转化、复购、服务体验几个方向展开。
                3. 不得承诺一定增长多少，只能使用“有望提升”“预计改善”这类保守表达。
                """.formatted(
                shopContext.getShopName(),
                shopContext.getShopType(),
                defaultDecimal(analysis.getTotalRevenue()),
                defaultNumber(suggest.getWeekOrders()),
                defaultDecimal(analysis.getAvgOrderPrice()),
                defaultNumber(analysis.getRepurchaseCount()),
                defaultNumber(suggest.getBadReviewCount()),
                defaultDecimal(analysis.getAvgScore()),
                formatProductSales(suggest.getHotProducts()),
                formatProductSales(suggest.getSlowProducts()),
                formatBadReviewIssues(suggest.getBadReviewList()),
                resolveInstruction(dto)
        );
    }

    private String formatProductSales(List<ProductSalesDTO> products) {
        if (products == null || products.isEmpty()) {
            return "暂无数据";
        }
        List<String> items = new ArrayList<>();
        for (ProductSalesDTO product : products) {
            if (product == null) {
                continue;
            }
            items.add(defaultText(product.getProductName()) + "(销量" + defaultLongNumber(product.getSalesCount()) + ")");
        }
        return items.isEmpty() ? "暂无数据" : String.join("、", items);
    }

    private String formatBadReviewIssues(List<BadReviewDTO> badReviewList) {
        if (badReviewList == null || badReviewList.isEmpty()) {
            return "暂无数据";
        }
        List<String> items = new ArrayList<>();
        for (BadReviewDTO review : badReviewList) {
            if (review == null || !StringUtils.hasText(review.getContent())) {
                continue;
            }
            items.add("- [" + defaultNumber(review.getScore()) + "分][" + formatDate(review.getCreateTime()) + "] " + review.getContent().trim());
            if (items.size() >= 5) {
                break;
            }
        }
        return items.isEmpty() ? "暂无数据" : String.join("\n", items);
    }
}