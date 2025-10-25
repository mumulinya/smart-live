package com.smartLive.search.api;
import com.smartLive.common.core.constant.ServiceNameConstants;

import com.smartLive.common.core.domain.EsBatchInsertRequest;
import com.smartLive.common.core.domain.EsInsertRequest;
import com.smartLive.common.core.domain.R;
import com.smartLive.search.api.factory.RemoteSearchFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;



@FeignClient(contextId = "remoteSearchService", value = ServiceNameConstants.SEARCH_SERVICE, fallbackFactory = RemoteSearchFallbackFactory.class)
public interface RemoteSearchService {
    /**
     * 插入或更新文档
     */
    @PostMapping("/search/insert")
    R<Boolean> insertOrUpdate(@RequestBody EsInsertRequest  request);

    /**
     * 批量插入文档
     */
    @PostMapping("/search/batchInsert")
    R<Boolean> batchInsert(@RequestBody EsBatchInsertRequest request);
    /**
     * 删除文档
     */
    @DeleteMapping("/search/{indexName}/{id}")
     R<Boolean> delete(
            @PathVariable("indexName") String indexName,
            @PathVariable("id") String id);
}
