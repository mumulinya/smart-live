package com.smartLive.audit.controller;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.audit.domain.vo.AuditTaskVO;
import com.smartLive.audit.service.IAuditService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.web.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统一审核中心 Controller
 */
@RestController
@RequestMapping("/audit")
public class AuditController extends BaseController {

    @Autowired
    private IAuditService auditService;

    /**
     * 查询审核任务列表
     */
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody AuditTask auditTask) {
        startPage(); // 会从请求参数(pageNum, pageSize)中获取分页信息
        List<AuditTaskVO> list = auditService.selectAuditList(auditTask);
        return getDataTable(list);
    }

    /**
     * 获取审核详情
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(auditService.getAuditDetail(id));
    }

    /**
     * 提交审核结果
     * 参数：id, status (1-通过, 2-驳回), reason
     */
    @PostMapping("/action")
    public AjaxResult action(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(body.get("id").toString());
        Integer status = Integer.valueOf(body.get("status").toString());
        String reason = (String) body.get("reason");
        
        return toAjax(auditService.auditAction(id, status, reason));
    }
}
