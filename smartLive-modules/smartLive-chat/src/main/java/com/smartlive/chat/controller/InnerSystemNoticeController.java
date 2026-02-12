package com.smartlive.chat.controller;

import com.smartlive.chat.domain.VO.SystemNoticeVO;
import com.smartlive.chat.dto.SystemNoticeCreateDTO;
import com.smartlive.chat.service.ISystemNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inner/chat/notice/system")
public class InnerSystemNoticeController {

    @Autowired
    private ISystemNoticeService systemNoticeService;

    @PostMapping("/create")
    public SystemNoticeVO create(@RequestBody SystemNoticeCreateDTO createDTO) {
        return systemNoticeService.createAndPush(createDTO);
    }
}
