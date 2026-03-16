package com.smartlive.chat.service.impl;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.core.constant.mq.ChatMqConstants;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.utils.StringUtils;
import com.smartlive.chat.domain.SystemNotice;
import com.smartlive.chat.domain.VO.SystemNoticeVO;
import com.smartlive.chat.dto.SystemNoticeCreateDTO;
import com.smartlive.chat.mapper.SystemNoticeMapper;
import com.smartlive.chat.service.ISystemNoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 系统通知业务处理实现类
 * 负责用户通知的分页查询、已读状态更新、未读计数以及通过 IM 实时推送信令。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Slf4j
@Service
public class SystemNoticeServiceImpl extends ServiceImpl<SystemNoticeMapper, SystemNotice> implements ISystemNoticeService {

    private static final int READ_STATUS_UNREAD = 0;
    private static final int READ_STATUS_READ = 1;
    private static final int PAGE_SIZE = 10;
    private static final ZoneOffset DEFAULT_ZONE_OFFSET = ZoneOffset.ofHours(8);
    private static final DateTimeFormatter NOTICE_ID_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Autowired
    private ObjectMapper objectMapper;

    
    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;

    @Override
    public List<SystemNoticeVO> queryNoticePage(Long userId, Integer current) {
        if (userId == null) {
            return List.of();
        }
        int pageNo = current == null || current < 1 ? 1 : current;

        Page<SystemNotice> page = this.lambdaQuery()
                .eq(SystemNotice::getUserId, userId)
                .orderByDesc(SystemNotice::getCreateTime)
                .page(new Page<>(pageNo, PAGE_SIZE));

        return page.getRecords().stream().map(this::toVO).toList();
    }

    @Override
    public boolean markRead(Long userId, String noticeId) {
        if (userId == null || StringUtils.isBlank(noticeId)) {
            return false;
        }
        Date now = DateUtils.getNowDate();
        LambdaUpdateWrapper<SystemNotice> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SystemNotice::getUserId, userId)
                .eq(SystemNotice::getNoticeId, noticeId)
                .eq(SystemNotice::getReadStatus, READ_STATUS_UNREAD)
                .set(SystemNotice::getReadStatus, READ_STATUS_READ)
                .set(SystemNotice::getReadAt, now)
                .set(SystemNotice::getUpdateTime, now);
        int affected = baseMapper.update(null, updateWrapper);
        if (affected > 0) {
            return true;
        }
        return this.lambdaQuery()
                .eq(SystemNotice::getUserId, userId)
                .eq(SystemNotice::getNoticeId, noticeId)
                .count() > 0;
    }

    @Override
    public int markAllRead(Long userId) {
        if (userId == null) {
            return 0;
        }
        Date now = DateUtils.getNowDate();
        LambdaUpdateWrapper<SystemNotice> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SystemNotice::getUserId, userId)
                .eq(SystemNotice::getReadStatus, READ_STATUS_UNREAD)
                .set(SystemNotice::getReadStatus, READ_STATUS_READ)
                .set(SystemNotice::getReadAt, now)
                .set(SystemNotice::getUpdateTime, now);
        return baseMapper.update(null, updateWrapper);
    }

    @Override
    public long countUnread(Long userId) {
        if (userId == null) {
            return 0;
        }
        return this.lambdaQuery()
                .eq(SystemNotice::getUserId, userId)
                .eq(SystemNotice::getReadStatus, READ_STATUS_UNREAD)
                .count();
    }

    @Override
    public SystemNoticeVO createAndPush(SystemNoticeCreateDTO createDTO) {
        if (createDTO == null || createDTO.getUserId() == null || createDTO.getSourceType() == null || StringUtils.isBlank(createDTO.getTitle())) {
            throw new IllegalArgumentException("system notice create params invalid");
        }

        Date now = DateUtils.getNowDate();
        SystemNotice notice = new SystemNotice();
        notice.setNoticeId(StringUtils.isNotBlank(createDTO.getNoticeId()) ? createDTO.getNoticeId() : generateNoticeId());
        notice.setUserId(createDTO.getUserId());
        notice.setSourceType(createDTO.getSourceType());
        notice.setSourceId(createDTO.getSourceId());
        notice.setAction(createDTO.getAction());
        notice.setTitle(createDTO.getTitle());
        notice.setExtraData(createDTO.getExtraData());
        notice.setContent(createDTO.getContent());
        notice.setRejectReason(createDTO.getRejectReason());
        notice.setPayload(toPayloadJson(createDTO.getPayload()));
        notice.setReadStatus(READ_STATUS_UNREAD);
        notice.setCreateTime(now);
        notice.setUpdateTime(now);

        this.save(notice);

        SystemNoticeVO noticeVO = toVO(notice);
        pushSystemNotice(notice.getUserId(), noticeVO);
        return noticeVO;
    }

    private SystemNoticeVO toVO(SystemNotice notice) {
        log.info("convert system notice to vo: {}", notice);
        SystemNoticeVO vo = new SystemNoticeVO();
        vo.setNoticeId(notice.getNoticeId());
        vo.setSourceType(notice.getSourceType());
        vo.setSourceId(notice.getSourceId());
        vo.setAction(notice.getAction());
        vo.setTitle(notice.getTitle());
        vo.setRejectReason(notice.getRejectReason());
        vo.setExtraData(notice.getExtraData());
        vo.setContent(notice.getContent());
        vo.setCreatedAt(toIsoOffsetTime(notice.getCreateTime()));
        vo.setRead(Integer.valueOf(READ_STATUS_READ).equals(notice.getReadStatus()));
        return vo;
    }

    private void pushSystemNotice(Long userId, SystemNoticeVO noticeVO) {
        try {
            Map<String, Object> payload = Map.of(
                    "type", "SYSTEM_MESSAGE",
                    "data", noticeVO
            );
            String json = objectMapper.writeValueAsString(payload);
            Map<String, Object> mqMap = Map.of(
                    "userId", userId,
                    "json", json
            );
            mqMessageSendUtils.sendMqMessage(ChatMqConstants.CHAT_DIRECT_EXCHANGE, "im.push.user", mqMap);
        } catch (Exception e) {
            log.error("push system notice failed, userId: {}, noticeId: {}", userId, noticeVO.getNoticeId(), e);
        }
    }

    private String toIsoOffsetTime(Date date) {
        if (date == null) {
            return null;
        }
        return Instant.ofEpochMilli(date.getTime())
                .atOffset(DEFAULT_ZONE_OFFSET)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private String generateNoticeId() {
        return "n_" + LocalDateTime.now().format(NOTICE_ID_TIME_FORMATTER) + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    private String toPayloadJson(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof String payloadJson) {
            return payloadJson;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("serialize system notice payload failed", e);
            return null;
        }
    }

}
