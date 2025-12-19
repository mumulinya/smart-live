package com.smartLive.interaction.controller;

import com.smartLive.interaction.service.ILikeRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/likeRecord")
public class LikeRecordController {
    @Autowired
    private ILikeRecordService likeRecordService;
}
