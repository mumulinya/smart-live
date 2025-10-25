package com.smartLive.search.api.factory;

import com.smartLive.common.core.domain.EsBatchInsertRequest;
import com.smartLive.common.core.domain.EsInsertRequest;
import com.smartLive.common.core.domain.R;
import com.smartLive.search.api.RemoteSearchService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class RemoteSearchFallbackFactory implements FallbackFactory<RemoteSearchService> {
    @Override
    public RemoteSearchService create(Throwable cause) {
        return new RemoteSearchService() {
            /**
             * 插入或更新文档
             *
             * @param
             */
            @Override
            public R<Boolean> insertOrUpdate(EsInsertRequest request) {
                return R.fail("插入失败");
            }

            /**
             * 批量插入文档
             *
             * @param request
             */
            @Override
            public R<Boolean> batchInsert(EsBatchInsertRequest request) {
                return R.fail("批量插入失败");
            }

            /**
             * 删除文档
             *
             * @param indexName
             * @param id
             */
            @Override
            public R<Boolean> delete(String indexName, String id) {
                return R.fail("删除失败");
            }
        };
    }
}
