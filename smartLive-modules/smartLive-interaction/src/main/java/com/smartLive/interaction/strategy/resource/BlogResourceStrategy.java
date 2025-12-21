package com.smartLive.interaction.strategy.resource;

import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.blog.api.dto.BlogDto;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.domain.blog.Blog;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.domain.vo.ResourceVO;
import com.smartLive.interaction.domain.vo.SocialInfoVO;
import com.smartLive.user.api.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component("blogResource")
public abstract class BlogResourceStrategy implements ResourceFetcherStrategy {
    @Autowired
    private RemoteBlogService remoteBlogService;
    /**
     * 策略标识 (USER / SHOP)
     */
    @Override
    public String getType() {
        return ResourceTypeEnum.BLOGRESOURCE.getStrategyName();
    }

    /**
     * 获取资源列表
     *
     * @param sourceIdList
     */
    @Override
    public List<ResourceVO> getResourceList(List<Long> sourceIdList) {

        R<List<BlogDto>> blogSuccess = remoteBlogService.getBlogListByIds(sourceIdList);
        if (blogSuccess.getCode() != 200) {
            return null;
        }
        List<BlogDto> blogList = blogSuccess.getData();
        List<ResourceVO> resourceVOList = blogList.stream().map(blog -> ResourceVO.builder()
                .id(blog.getId())
                .userAvatar(blog.getIcon())
                .userName(blog.getName())
                .cover(blog.getImages())
                .content(blog.getContent())
                .isLike(true)
                .build()
        ).collect(Collectors.toList());
        return resourceVOList;
    }
}
