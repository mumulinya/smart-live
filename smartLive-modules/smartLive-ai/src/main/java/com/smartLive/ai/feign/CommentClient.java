package com.smartLive.ai.feign;

import com.smartLive.comment.api.dto.CommentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "smartLive-comment")
public interface CommentClient {
    @GetMapping("/comment/list")
    List<CommentDTO> searchCommentList();

    @PostMapping("/comment/saveAiCreateComment")
    void saveAiCreateComment(@RequestBody List<CommentDTO> comments);
}
