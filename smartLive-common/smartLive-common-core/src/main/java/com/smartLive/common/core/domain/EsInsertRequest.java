package com.smartLive.common.core.domain;

import lombok.Data;
import org.apache.poi.ss.formula.functions.T;

@Data

public class EsInsertRequest {
    private String indexName;
    private Long  id;
    private Object data;
    private String dataType; // shop, blog, user, voucher
}
