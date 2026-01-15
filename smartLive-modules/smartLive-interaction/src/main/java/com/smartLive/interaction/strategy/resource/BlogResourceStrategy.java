package com.smartLive.interaction.strategy.resource;

import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.blog.api.dto.BlogDto;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.domain.vo.ResourceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public  class BlogResourceStrategy implements ResourceStrategy {
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
    public List<ResourceVO> getResourceList(List<Long> sourceIdList) {

        List<BlogDto> blogList= remoteBlogService.getBlogListByIds(sourceIdList);
        if (blogList == null) {
            return null;
        }
        List<ResourceVO> resourceVOList = blogList.stream().map(blog -> ResourceVO.builder()
                .id(blog.getId())
                .userAvatar(blog.getIcon())
                .userName(blog.getName())
                .images(blog.getImages())
                .content(blog.getContent())
                .title(blog.getTitle())
                .liked(blog.getLiked())
                .isLike(blog.getIsLike())
                .build()
        ).collect(Collectors.toList());
        return resourceVOList;
    }
}
