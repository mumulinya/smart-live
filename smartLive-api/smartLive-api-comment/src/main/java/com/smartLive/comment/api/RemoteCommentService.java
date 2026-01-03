package com.smartLive.comment.api;


import com.smartLive.comment.api.dto.CommentDTO;
import com.smartLive.comment.api.factory.RemoteCommentFallbackFactory;
import com.smartLive.common.core.constant.ServiceNameConstants;

import com.smartLive.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


//@FeignClient(contextId = "remoteCommentService", value = ServiceNameConstants.COMMENT_SERVICE, fallbackFactory = RemoteCommentFallbackFactory.class)
public interface RemoteCommentService {

    /**
     * 获取评论数量
     * @param userId
     * @return
     */
    @GetMapping("/comment/getCommentCount/{userId}")
    R<Integer> getCommentCount( @PathVariable("userId")Long userId);
    /**
     * 获取评论总数
     * @return
     */
    @GetMapping("/comment/getCommentTotal")
    R<Integer> getCommentTotal();
    @GetMapping("/comment/list")
    List<CommentDTO> searchCommentList();
    @PostMapping("/comment/saveAiCreateComment")
    void saveAiCreateComment(@RequestBody List<CommentDTO> comments);
}
