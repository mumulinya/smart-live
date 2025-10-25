package com.smartLive.search.listener;

import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.domain.EsBatchInsertRequest;
import com.smartLive.common.core.domain.EsInsertRequest;
import com.smartLive.search.domain.BlogDoc;
import com.smartLive.search.domain.ShopDoc;
import com.smartLive.search.domain.UserDoc;
import com.smartLive.search.domain.VoucherDoc;
import com.smartLive.search.service.IBlogEsService;
import com.smartLive.search.service.IShopEsService;
import com.smartLive.search.service.IUserEsService;
import com.smartLive.search.service.IVoucherEsService;
import com.smartLive.search.utils.EsTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class EsSyncListener {

    // 注入4个实体的Service
    @Autowired
    private IVoucherEsService voucherEsService;
    @Autowired
    private IUserEsService userEsService;
    @Autowired
    private IShopEsService shopEsService;
    @Autowired
    private IBlogEsService blogEsService;

    // ==================== 单条插入 ====================
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.ES_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_VOUCHER_INSERT),
            @QueueBinding(value = @Queue(name = MqConstants.ES_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_USER_INSERT),
            @QueueBinding(value = @Queue(name = MqConstants.ES_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_SHOP_INSERT),
            @QueueBinding(value = @Queue(name = MqConstants.ES_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_BLOG_INSERT)
    })
    public void handleSingleInsert(EsInsertRequest request) {
        log.info("接收单条插入请求: {}", request);
        try {
            // 1. 转换数据为实体类
            Object convertedData = convertData(request.getDataType(), (Map<String, Object>) request.getData());
            if (convertedData == null) {
                log.error("单条插入失败：未知dataType={}", request.getDataType());
                return;
            }
            // 2. 根据数据类型调用对应的Service
// 替换原来的 switch 表达式为传统 switch 语句
            boolean success = false; // 定义变量接收结果
            switch (request.getDataType()) {
                case "voucher":
                    success = voucherEsService.insertOrUpdate(
                            request.getIndexName(),
                            request.getId().toString(),
                            (VoucherDoc) convertedData
                    );
                    break;
                case "user":
                    success = userEsService.insertOrUpdate(
                            request.getIndexName(),
                            request.getId().toString(),
                            (UserDoc) convertedData
                    );
                    break;
                case "shop":
                    success = shopEsService.insertOrUpdate(
                            request.getIndexName(),
                            request.getId().toString(),
                            (ShopDoc) convertedData
                    );
                    break;
                case "blog":
                    success = blogEsService.insertOrUpdate(
                            request.getIndexName(),
                            request.getId().toString(),
                            (BlogDoc) convertedData
                    );
                    break;
                default:
                    log.error("未知dataType：{}", request.getDataType());
                    success = false; // 默认失败
            }
            log.info("单条插入结果: {}", success);
        } catch (Exception e) {
            log.error("单条插入失败", e);
        }
    }

    // ==================== 批量插入 ====================
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.ES_BATCH_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_VOUCHER_BATCH_INSERT),
            @QueueBinding(value = @Queue(name = MqConstants.ES_BATCH_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_SHOP_BATCH_INSERT),
            @QueueBinding(value = @Queue(name = MqConstants.ES_BATCH_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_USER_BATCH_INSERT),
            @QueueBinding(value = @Queue(name = MqConstants.ES_BATCH_INSERT_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_BLOG_BATCH_INSERT),
            // 其他实体的批量插入绑定...
    })
    public void handleBatchInsert(EsBatchInsertRequest request) {
        log.info("接收批量插入请求: {}", request);
        try {
            // 1. 转换数据列表为实体类列表
            List<?> convertedList = convertDataList(request.getDataType(), (List<Object>) request.getData());
            if (convertedList == null) {
                log.error("批量插入失败：未知dataType={}", request.getDataType());
                return;
            }
            // 2. 根据数据类型调用对应的Service
            boolean success = false;
            switch (request.getDataType()) {
                case "voucher":
                    success = voucherEsService.batchInsert(
                            request.getIndexName(),
                            (List<VoucherDoc>) convertedList,
                            data -> data.getId().toString() // Long转String
                    );
                    break;
                case "user":
                    success = userEsService.batchInsert(
                            request.getIndexName(),
                            (List<UserDoc>) convertedList,
                            data -> data.getId().toString()
                    );
                    break;
                case "shop":
                    success = shopEsService.batchInsert(
                            request.getIndexName(),
                            (List<ShopDoc>) convertedList,
                            data -> data.getId().toString()
                    );
                    break;
                case "blog":
                    success = blogEsService.batchInsert(
                            request.getIndexName(),
                            (List<BlogDoc>) convertedList,
                            data -> data.getId().toString()
                    );
                    break;
                default:
                    log.error("未知dataType：{}", request.getDataType());
                    success = false;
            }
            log.info("批量插入结果: {}", success);
        } catch (Exception e) {
            log.error("批量插入失败", e);
        }
    }

    // ==================== 删除 ====================
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(name = MqConstants.ES_DELETE_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_VOUCHER_DELETE),
            @QueueBinding(value = @Queue(name = MqConstants.ES_DELETE_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_USER_DELETE),
            @QueueBinding(value = @Queue(name = MqConstants.ES_DELETE_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_BLOG_DELETE),
            @QueueBinding(value = @Queue(name = MqConstants.ES_DELETE_QUEUE, declare = "true"),
                    exchange = @Exchange(name = MqConstants.ES_EXCHANGE),
                    key = MqConstants.ES_ROUTING_SHOP_DELETE),
    })
    public void handleDelete(EsInsertRequest request) {
        log.info("接收删除请求: id={}", request.getId());
        try {
            boolean success = false;
            switch (request.getDataType()) {
                case "voucher":
                    success = voucherEsService.delete(request.getIndexName(), request.getId().toString());
                    break;
                case "user":
                    success = userEsService.delete(request.getIndexName(), request.getId().toString());
                    break;
                case "shop":
                    success = shopEsService.delete(request.getIndexName(), request.getId().toString());
                    break;
                case "blog":
                    success = blogEsService.delete(request.getIndexName(), request.getId().toString());
                    break;
                default:
                    log.error("未知dataType：{}", request.getDataType());
                    success = false;
            }
            log.info("删除结果: {}", success);
        } catch (Exception e) {
            log.error("删除失败", e);
        }
    }

    // ==================== 数据转换工具方法 ====================
    private Object convertData(String dataType, Map<String, Object> dataMap) {
        switch (dataType) {
            case "voucher": return EsTool.convertToObject(dataMap, VoucherDoc.class);
            case "user": return EsTool.convertToObject(dataMap, UserDoc.class);
            case "shop": return EsTool.convertToObject(dataMap, ShopDoc.class);
            case "blog": return EsTool.convertToObject(dataMap, BlogDoc.class);
            default: return null;
        }
    }

    private List<?> convertDataList(String dataType, List<Object> dataList) {
        switch (dataType) {
            case "voucher": return EsTool.convertList(dataList, VoucherDoc.class);
            case "user": return EsTool.convertList(dataList, UserDoc.class);
            case "shop": return EsTool.convertList(dataList, ShopDoc.class);
            case "blog": return EsTool.convertList(dataList, BlogDoc.class);
            default: return null;
        }
    }
}