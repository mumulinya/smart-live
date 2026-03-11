package com.smartLive.interaction.controller.api;

import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.core.web.page.TableDataInfo;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.interaction.domain.Review;
import com.smartLive.interaction.domain.VO.ReviewVO;
import com.smartLive.interaction.service.IReviewService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评价（Review）管理控制层
 * 处理针对店铺和商品的图文评价，支持分页检索、多维排序（按热门/时间）以及同步至向量库（Milvus）进行 AI 检索。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/review")
public class ReviewController extends BaseController
{
    @Autowired
    private IReviewService reviewService;

    /**
     * 查询评论列表
     */
    @RequiresPermissions("review:review:list")
    @GetMapping("/list")
    public TableDataInfo list(Review review)
    {
        startPage();
        List<ReviewVO> list = reviewService.selectReviewList(review);
        return getDataTable(list);
    }

    /**
     * 导出评论列表
     */
    @RequiresPermissions("review:review:export")
    @Log(title = "评论", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Review review)
    {
        List<ReviewVO> list = reviewService.selectReviewList(review);
        ExcelUtil<ReviewVO> util = new ExcelUtil<ReviewVO>(ReviewVO.class);
        util.exportExcel(response, list, "评论数据");
    }

    /**
     * 获取评论详细信息
     */
    @RequiresPermissions("review:review:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(reviewService.selectReviewById(id));
    }

    /**
     * 新增评论
     */
    @RequiresPermissions("review:review:add")
    @Log(title = "评论", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Review review)
    {
        review.setCreateTime(DateUtils.getNowDate());
        review.setUpdateTime(DateUtils.getNowDate());
        return toAjax(reviewService.insertReview(review));
    }

    /**
     * 修改评论
     */
    @RequiresPermissions("review:review:edit")
    @Log(title = "评论", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Review review)
    {
        return toAjax(reviewService.updateReview(review));
    }

    /**
     * 删除评价（带权限控制）
     */
    @RequiresPermissions("review:review:remove")
    @Log(title = "评论", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(reviewService.deleteReviewByIds(ids));
    }

    /**
     * 分页查询评价列表
     * 支持热度（默认）与时间顺序混排。
     *
     * @param review  检索条件（资源 ID、类型等）
     * @param current 页码
     * @param sort    排序规则
     * @return 评价视图对象列表
     */
    @GetMapping("/listReview")
    public Result listReview(Review review, @RequestParam("current") Integer current, @RequestParam(value = "sort", required = false) String sort){
        return Result.ok(reviewService.listReview(review, current, sort));
    }
    /**
     * 添加评价
     * 允许用户对商品或服务提交新的评价。
     *
     * @param review 评价实体，包含评价内容、评分等信息
     * @return 操作结果
     */
    @PostMapping("/addReview")
    public Result addReview(@RequestBody Review review)
    {
        Integer i = reviewService.addReview(review);
        if (i > 0) {
            return Result.ok("添加成功");
        }
        return Result.fail("添加失败");
    }
    /**
     * 修改评价
     * 允许用户更新已提交的评价内容。
     *
     * @param review 评价实体，包含待更新的评价信息
     * @return 操作结果
     */
    @PutMapping("/updateReview")
    public Result updateReview(@RequestBody Review review)
    {
        Integer i = reviewService.updateReview(review);
        if (i > 0) {
            return Result.ok("修改成功");
        }
        return Result.fail("添加失败");
    }
    /**
     * 删除评价
     * 允许用户删除自己的评价。
     *
     * @param review 评价实体，通常包含评价ID
     * @return 操作结果
     */
    @PostMapping("/removeReview")
    public Result removeReview(@RequestBody Review review)
    {

        return Result.ok(reviewService.deleteReview(review));
    }
    /**
     * 获取当前用户的评价列表
     * 分页查询指定用户的所有评价。
     *
     * @param review  检索条件（如用户ID）
     * @param current 页码
     * @return 用户的评价列表
     */
    @GetMapping("/of/user")
    public Result getReviewOfUser(Review review,@RequestParam("current") Integer current){
        return Result.ok(reviewService.getReviewOfUser(review,current));
    }
    /**
     * 根据ID获取评价详情
     *
     * @param id 评价ID
     * @return 评价详情
     */
    @GetMapping("/getReview/{id}")
    public Result getReviewById(@PathVariable("id")Long id){
        return Result.ok(reviewService.getReviewById(id));
    }

    /**
     * 判断当前用户是否已评价目标资源
     *
     * @param review 包含用户ID和目标资源ID/类型
     * @return 是否已评价
     */
    @GetMapping("/isReview")
    public Result isReview(Review review){
        return Result.ok(reviewService.isReview(review));
    }
    /**
     * 获取指定条件的评价总数
     * @param review 检索条件（如资源ID、类型）
     * @return 评价总数
     */
    @GetMapping("/getReviewCount")
    R<Integer> getReviewCount(Review review){
        return R.ok(reviewService.getReviewCount(review));
    }

    /**
     * 获取所有评价的总数
     * @return 所有评价的总数
     */
    @GetMapping("/getReviewTotal")
    R<Integer> getReviewTotal(){
        return R.ok(reviewService.getReviewTotal());
    }

    /**
     * 全量发布评价数据到向量库（仅 Milvus）
     * 将所有评价数据同步至向量数据库，用于支撑语义搜索及 AI 辅助分析功能。
     *
     * @return 同步结果
     */
    @PostMapping("/allPublish")
    public AjaxResult allPublish() {
        return success(reviewService.allPublish());
    }

    /**
     * 将指定的评价数据同步至向量数据库（Milvus）
     * 用于支撑语义搜索及 AI 辅助分析功能。
     *
     * @param ids 评价 ID 数组
     * @return 同步结果
     */
    @PostMapping("/publish/{ids}")
    public AjaxResult publish(@PathVariable String[] ids) {
        return success(reviewService.publish(ids));
    }
}
