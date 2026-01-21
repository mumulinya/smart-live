package com.smartLive.interaction.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.interaction.api.DTO.CommentDTO;
import com.smartLive.interaction.api.factory.RemoteCommentFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


@FeignClient(contextId = "remoteCommentService", value = ServiceNameConstants.INTERACTION_SERVICE, fallbackFactory = RemoteCommentFallbackFactory.class)
public interface RemoteCommentService {

    /**
     * 获取评论数量
     * @param commentDTO
     * @return
     */
    @GetMapping("/inner/comment/getCommentCount")
    Integer getCommentCount(@SpringQueryMap CommentDTO commentDTO);
    /**
     * 获取评论总数
     * @return
     */
    @GetMapping("/inner/comment/getCommentTotal")
    Integer getCommentTotal();
    @GetMapping("/inner/comment/list")
    List<CommentDTO> searchCommentList();
    @PostMapping("/inner/comment/saveAiCreateComment")
    void saveAiCreateComment(@RequestBody List<CommentDTO> comments);
}
