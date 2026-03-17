package com.smartLive.interaction.strategy.impl;

import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Default ResourceStrategy.
 */
@Component
@Slf4j
public class DefaultResourceStrategy implements ResourceStrategy<Object> {

    @Override
    public Integer getType() {
        return -1;
    }

    @Override
    public List<Object> getResourceList(List<Long> sourceIdList) {
        log.info("DefaultResourceStrategy.getResourceList: {}", sourceIdList);
        return Collections.emptyList();
    }

    @Override
    public Object getResourceById(Long sourceId) {
        return null;
    }

    @Override
    public Long getResourceId(Object data) {
        return null;
    }

    @Override
    public HashMap<String, String> getResourceContentById(Long sourceId) {
        return new HashMap<>();
    }
}
