package com.smartLive.interaction.service;

import com.smartLive.common.core.domain.ScrollResult;
public interface IFeedService {
    ScrollResult queryFeedList(Integer feedType, Long max, Integer offset);
}
