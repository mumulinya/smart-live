package com.smartLive.interaction.api;
import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.interaction.api.dto.CommentDTO;
import com.smartLive.interaction.api.factory.RemoteCommentFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    @GetMapping("/comment/getCommentCount")
    R<Integer> getCommentCount(@SpringQueryMap CommentDTO commentDTO);
    /**
     * 获取评论总数
     * @return
     */
    @GetMapping("/comment/getCommentTotal")
    R<Integer> getCommentTotal();
    @GetMapping("/comment/list")
    R<List<CommentDTO>> searchCommentList();
    @PostMapping("/comment/saveAiCreateComment")
    void saveAiCreateComment(@RequestBody List<CommentDTO> comments);
}
