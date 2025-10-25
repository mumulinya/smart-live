package com.smartLive.search;

import com.smartLive.search.domain.*;
import com.smartLive.search.service.Impl.UnifiedDataInsertService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SpringBootTest
public class UnifiedDataInsertTest {

    @Autowired
    private UnifiedDataInsertService unifiedDataInsertService;

    /**
     * 测试统一插入所有类型数据
     */
    @Test
    public void testInsertAllTypes() throws IOException {
        // 1. 插入博客数据
        BlogDoc blogDoc = new BlogDoc();
        blogDoc.setId(1L);
        blogDoc.setShopId(1001L);
        blogDoc.setTypeId(1L);
        blogDoc.setUserId(2001L);
        blogDoc.setTitle("测试博客标题");
        blogDoc.setContent("博客内容测试");
        blogDoc.setLiked(100);
        blogDoc.setComments(20);
        blogDoc.setCreateTime(new Date());
        blogDoc.setIcon("blog_icon.jpg");
        blogDoc.setName("博主名称");

        boolean blogResult = unifiedDataInsertService.insertToEs(
            UnifiedDataInsertService.IndexType.BLOG, blogDoc);
        System.out.println("博客插入结果: " + blogResult);

        // 2. 插入店铺数据
        ShopDoc shopDoc = new ShopDoc();
        shopDoc.setId(1L);
        shopDoc.setName("测试餐厅");
        shopDoc.setTypeId(1L);
        shopDoc.setArea("商圈A");
        shopDoc.setAddress("详细地址");
        shopDoc.setX(116.3974);
        shopDoc.setY(39.9093);
        shopDoc.setSold(500);
        shopDoc.setComments(200);
        shopDoc.setScore(45);
        shopDoc.setOpenHours("10:00-22:00");
        shopDoc.setCreateTime(new Date());

        boolean shopResult = unifiedDataInsertService.insertToEs(
            UnifiedDataInsertService.IndexType.SHOP, shopDoc);
        System.out.println("店铺插入结果: " + shopResult);

        // 3. 插入用户数据
        UserDoc userDoc = new UserDoc();
        userDoc.setId(1L);
        userDoc.setNickName("测试用户");
        userDoc.setIcon("user_icon.jpg");
        userDoc.setIsFollow(false);
        userDoc.setIntroduce("个性签名测试");
        userDoc.setCity("北京");
        userDoc.setCreateTime(new Date());

        boolean userResult = unifiedDataInsertService.insertToEs(
            UnifiedDataInsertService.IndexType.USER, userDoc);
        System.out.println("用户插入结果: " + userResult);

        // 4. 插入优惠券数据
        VoucherDoc voucherDoc = new VoucherDoc();
        voucherDoc.setId(1L);
        voucherDoc.setShopId(1001L);
        voucherDoc.setTitle("测试优惠券");
        voucherDoc.setSubTitle("副标题");
        voucherDoc.setRules("使用规则");
        voucherDoc.setPayValue("1000");
        voucherDoc.setActualValue(2000L);
        voucherDoc.setType(0);
        voucherDoc.setStatus(1);
        voucherDoc.setStock(100);
        voucherDoc.setBeginTime(new Date());
        voucherDoc.setEndTime(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000));
        voucherDoc.setShopName("测试店铺");
        voucherDoc.setTypeId(1L);
        voucherDoc.setCreateTime(new Date());

        boolean voucherResult = unifiedDataInsertService.insertToEs(
            UnifiedDataInsertService.IndexType.VOUCHER, voucherDoc);
        System.out.println("优惠券插入结果: " + voucherResult);
    }

    /**
     * 测试批量插入
     */
    @Test
    public void testBatchInsert() throws IOException {
        // 批量插入博客数据
        List<BlogDoc> blogList = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            BlogDoc blog = new BlogDoc();
            blog.setId((long) i);
            blog.setTitle("批量博客 " + i);
            blog.setContent("内容 " + i);
            blog.setLiked(i * 10);
            blog.setCreateTime(new Date());
            blogList.add(blog);
        }

        boolean result = unifiedDataInsertService.batchInsertToEs(
            UnifiedDataInsertService.IndexType.BLOG, blogList);
        System.out.println("批量插入结果: " + result);
    }

    /**
     * 测试单个类型插入
     */
    @Test
    public void testSingleTypeInsert() throws IOException {
        // 只插入用户数据
        UserDoc user = new UserDoc();
        user.setId(100L);
        user.setNickName("单一用户");
        user.setCity("上海");
        user.setCreateTime(new Date());

        boolean result = unifiedDataInsertService.insertToEs(
            UnifiedDataInsertService.IndexType.USER, user);
        System.out.println("单一类型插入结果: " + result);
    }
}