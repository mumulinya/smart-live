package com.smartlive.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartlive.chat.domain.SystemNotice;
import com.smartlive.chat.domain.VO.SystemNoticeVO;
import com.smartlive.chat.dto.SystemNoticeCreateDTO;

import java.util.List;

public interface ISystemNoticeService extends IService<SystemNotice> {

    List<SystemNoticeVO> queryNoticePage(Long userId, Integer current);

    boolean markRead(Long userId, String noticeId);

    int markAllRead(Long userId);

    long countUnread(Long userId);

    SystemNoticeVO createAndPush(SystemNoticeCreateDTO createDTO);
}
