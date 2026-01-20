package com.smartLive.interaction.strategy.resource;

import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.blog.api.dto.BlogDto;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public  class BlogResourceStrategy implements ResourceStrategy<BlogDto> {
    @Autowired
    private RemoteBlogService remoteBlogService;
    /**
     * 策略标识 (USER / SHOP)
     */
    @Override
    public Integer getType() {
        return ResourceTypeEnum.BLOG_RESOURCE.getCode();
    }

    /**
     * 获取资源列表
     *
     * @param sourceIdList
     */
    @Override
    public List<BlogDto> getResourceList(List<Long> sourceIdList) {

        List<BlogDto> blogList= remoteBlogService.getBlogListByIds(sourceIdList);
        if (blogList == null) {
            return null;
        }
        blogList.forEach(blogDto -> {
            blogDto.setDataType("blog");
        });
        return blogList;
    }
}
