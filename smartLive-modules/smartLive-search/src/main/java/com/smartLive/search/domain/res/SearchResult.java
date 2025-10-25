package com.smartLive.search.domain.res;

import java.io.Serializable;

public class SearchResult<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private boolean success;
    private String code;
    private String message;
    private T data;
    private long timestamp = System.currentTimeMillis();
    
    // 无参构造
    public SearchResult() {}
    
    // 全参构造
    public SearchResult(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    // 成功方法
    public static <T> SearchResult<T> success(T data) {
        return new SearchResult<>(true, "200", "成功", data);
    }
    
    public static <T> SearchResult<T> success(String message, T data) {
        return new SearchResult<>(true, "200", message, data);
    }
    
    // 错误方法
    public static <T> SearchResult<T> error(String message) {
        return new SearchResult<>(false, "500", message, null);
    }
    
    public static <T> SearchResult<T> error(String code, String message) {
        return new SearchResult<>(false, code, message, null);
    }
    
    // getter 和 setter 方法
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "SearchResult{" +
                "success=" + success +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }
}