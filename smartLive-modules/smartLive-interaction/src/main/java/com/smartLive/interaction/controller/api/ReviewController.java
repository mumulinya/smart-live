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
import com.smartLive.interaction.service.IReviewService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论Controller
 * 
 * @author mumulin
 * @date 2025-09-21
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
        List<Review> list = reviewService.selectReviewList(review);
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
        List<Review> list = reviewService.selectReviewList(review);
        ExcelUtil<Review> util = new ExcelUtil<Review>(Review.class);
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
     * 获取评论列表
     */
    @GetMapping("/listReview")
    public Result listReview(Review  review,@RequestParam("current") Integer current){
        return Result.ok(reviewService.listReview(review,current));
    }
    /**
     * 添加评论
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
     */
    @PostMapping("/removeReview")
    public Result removeReview(@RequestBody Review review)
    {

        return Result.ok(reviewService.deleteReview(review));
    }
    @GetMapping("/of/user")
    public Result getReviewOfUser(Review review,@RequestParam("current") Integer current){
        return Result.ok(reviewService.getReviewOfUser(review,current));
    }
    /**
     * 获取评论
     */
    @GetMapping("/getReview/{id}")
    public Result getReviewById(@PathVariable("id")Long id){
        return Result.ok(reviewService.getReviewById(id));
    }

    /**
     * 判断当前用户是否评价过目标资源
     */
    @GetMapping("/isReview")
    public Result isReview(Review review){
        return Result.ok(reviewService.isReview(review));
    }
    /**
     * 获取评价总数
     * @return
     */
    @GetMapping("/getReviewCount")
    R<Integer> getReviewCount(Review review){
        return R.ok(reviewService.getReviewCount(review));
    }

    /**
     * 获取评论总数
     * @return
     */
    @GetMapping("/getReviewTotal")
    R<Integer> getReviewTotal(){
        return R.ok(reviewService.getReviewTotal());
    }

}
