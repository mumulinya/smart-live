package com.smartLive.points.controller;

import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.web.page.TableDataInfo;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.points.domain.PointsLotteryPrize;
import com.smartLive.points.domain.PointsRecord;
import com.smartLive.points.domain.dto.PointsAdjustDTO;
import com.smartLive.points.mapper.PointsLotteryPrizeMapper;
import com.smartLive.points.service.IPointsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 积分管理端Controller
 *
 * @author smartLive
 */
@RestController
@RequestMapping("/points")
public class PointsAdminController extends BaseController {

    @Autowired
    private IPointsService pointsService;

    @Autowired
    private PointsLotteryPrizeMapper prizeMapper;

    // ==================== 积分管理 ====================

    /**
     * 查询积分流水列表
     */
    @RequiresPermissions("points:record:list")
    @GetMapping("/record/list")
    public TableDataInfo recordList(PointsRecord record) {
        startPage();
        List<PointsRecord> list = pointsService.selectRecordList(record);
        return getDataTable(list);
    }

    /**
     * 手动调整积分
     */
    @RequiresPermissions("points:record:adjust")
    @Log(title = "积分调整", businessType = BusinessType.UPDATE)
    @PostMapping("/adjust")
    public AjaxResult adjust(@RequestBody PointsAdjustDTO dto) {
        pointsService.adjustPoints(dto.getUserId(), dto.getType(), dto.getAmount(), dto.getReason());
        return success("操作成功");
    }

    // ==================== 抽奖奖品管理 ====================

    /**
     * 获取奖品配置列表
     */
    @RequiresPermissions("points:prize:list")
    @GetMapping("/lottery/prize/list")
    public TableDataInfo prizeList(PointsLotteryPrize prize) {
        startPage();
        LambdaQueryWrapper<PointsLotteryPrize> wrapper = new LambdaQueryWrapper<>();
        if (prize.getStatus() != null) {
            wrapper.eq(PointsLotteryPrize::getStatus, prize.getStatus());
        }
        if (prize.getType() != null && !prize.getType().isEmpty()) {
            wrapper.eq(PointsLotteryPrize::getType, prize.getType());
        }
        wrapper.orderByAsc(PointsLotteryPrize::getSort);
        List<PointsLotteryPrize> list = prizeMapper.selectList(wrapper);
        return getDataTable(list);
    }

    /**
     * 新增/编辑奖品
     */
    @RequiresPermissions("points:prize:edit")
    @Log(title = "奖品配置", businessType = BusinessType.INSERT)
    @PostMapping("/lottery/prize/save")
    public AjaxResult savePrize(@RequestBody PointsLotteryPrize prize) {
        if (prize.getId() != null) {
            // 编辑
            prize.setUpdateTime(LocalDateTime.now());
            prizeMapper.updateById(prize);
        } else {
            // 新增
            prize.setCreateTime(LocalDateTime.now());
            prizeMapper.insert(prize);
        }
        return success("操作成功");
    }

    /**
     * 删除奖品
     */
    @RequiresPermissions("points:prize:remove")
    @Log(title = "奖品配置", businessType = BusinessType.DELETE)
    @PostMapping("/lottery/prize/delete")
    public AjaxResult deletePrize(@RequestBody Long[] ids) {
        for (Long id : ids) {
            prizeMapper.deleteById(id);
        }
        return success("删除成功");
    }

    /**
     * 切换奖品状态
     */
    @RequiresPermissions("points:prize:edit")
    @Log(title = "奖品状态", businessType = BusinessType.UPDATE)
    @PostMapping("/lottery/prize/status")
    public AjaxResult changeStatus(@RequestBody PointsLotteryPrize prize) {
        PointsLotteryPrize update = new PointsLotteryPrize();
        update.setId(prize.getId());
        update.setStatus(prize.getStatus());
        update.setUpdateTime(LocalDateTime.now());
        prizeMapper.updateById(update);
        return success("操作成功");
    }
}
