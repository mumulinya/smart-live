package com.smartLive.interaction.strategy.resource;

import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.blog.api.DTO.BlogDTO;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.common.core.utils.bean.BeanUtils;
import com.smartLive.interaction.domain.VO.BlogVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
public  class BlogResourceStrategy implements ResourceStrategy<BlogVO> {
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
    public List<BlogVO> getResourceList(List<Long> sourceIdList) {
        List<BlogDTO> blogDTOList= remoteBlogService.getBlogListByIds(sourceIdList);
        if (blogDTOList == null || blogDTOList.isEmpty()) {
            return new ArrayList<>();
        }
        List<BlogVO> blogVOList = blogDTOList.stream().map(blogDTO -> {
            BlogVO blogVO = new BlogVO();
            BeanUtils.copyProperties(blogDTO, blogVO);
            return blogVO;
        }).collect(Collectors.toList());
        return blogVOList;
    }

    /**
     * 获取资源内容
     *
     * @param sourceId
     */
    @Override
    public HashMap<String,String> getResourceContentById(Long sourceId) {
        BlogDTO blogDTO = remoteBlogService.getBlogById(sourceId);
        HashMap<String,String> map = new HashMap<>();
        map.put("title",blogDTO.getTitle());
        map.put("images",blogDTO.getImages());
        return map;
    }
}
