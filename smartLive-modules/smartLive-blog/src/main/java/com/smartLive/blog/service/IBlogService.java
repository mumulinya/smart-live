package com.smartLive.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.blog.domain.Blog;
import com.smartLive.blog.domain.VO.BlogVO;
import com.smartLive.common.core.domain.ScrollResult;
import java.util.List;
import java.util.Map;

/**
 * 閸楁艾顓筍ervice閹恒儱褰?
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface IBlogService extends IService<Blog>
{
    /**
     * 閺屻儴顕楅崡姘吂
     * 
     * @param id 閸楁艾顓规稉濠氭暛
     * @return 閸楁艾顓?
     */
     Blog selectBlogById(Long id);

    /**
     * 閺屻儴顕楅崡姘吂閸掓銆?
     * 
     * @param blog 閸楁艾顓?
     * @return 閸楁艾顓归梿鍡楁値
     */
     List<Blog> selectBlogList(Blog blog);

     BlogVO selectBlogVoById(Long id);

     List<BlogVO> selectBlogVoList(Blog blog);

    /**
     * 閺傛澘顤冮崡姘吂
     * 
     * @param blog 閸楁艾顓?
     * @return 缂佹挻鐏?
     */
     int insertBlog(Blog blog);

    /**
     * 娣囶喗鏁奸崡姘吂
     * 
     * @param blog 閸楁艾顓?
     * @return 缂佹挻鐏?
     */
     int updateBlog(Blog blog);

    /**
     * 閹靛綊鍣洪崚鐘绘珟閸楁艾顓?
     * 
     * @param ids 闂団偓鐟曚礁鍨归梽銈囨畱閸楁艾顓规稉濠氭暛闂嗗棗鎮?
     * @return 缂佹挻鐏?
     */
     int deleteBlogByIds(Long[] ids);

    /**
     * 閸掔娀娅庨崡姘吂娣団剝浼?
     * 
     * @param id 閸楁艾顓规稉濠氭暛
     * @return 缂佹挻鐏?
     */
     int deleteBlogById(Long id);

    /**
     * 閺嶈宓侀崡姘吂ID閺屻儴顕楅崡姘瀮鐠囷附鍎?
     *
     * @param id 閸楁艾顓规稉濠氭暛
     * @return 閸楁艾顓圭拠锔藉剰VO
     */
    BlogVO queryBlogById(Long id);
    /**
     * 娣囨繂鐡ㄩ崡姘吂閿涘牆褰傜敮?閼藉顭堥敍?
     *
     * @param blog 閸楁艾顓圭€圭偘缍?
     * @return 閸楁艾顓笽D
     */
    Long saveBlog(Blog blog);

    /**
     * 閺屻儴顕楅張鈧悜顓炲触鐎广垹鍨悰?
     *
     * @param current 瑜版挸澧犳い鐢电垳
     * @return 閻戭參妫崡姘吂閸掓銆?
     */
    List<BlogVO> queryHotBlog(Integer current);

    /**
     * 閺屻儴顕楅幐鍥х暰閻劍鍩涢崣鎴濈閻ㄥ嫬宕ョ€广垹鍨悰?
     *
     * @param current 瑜版挸澧犳い鐢电垳
     * @param userId  閻劍鍩汭D
     * @return 閸楁艾顓归崚妤勩€?
     */
    List<BlogVO> queryBlogByUserId(Integer current, Long userId);


    /**
     * 閺屻儴顕楅幋鎴犳畱閸楁艾顓归崚妤勩€?
     *
     * @param blog    閸楁艾顓归弻銉嚄閺夆€叉閿涘牆瀵橀崥顐ゅЦ閹胶鐡戠粵娑⑩偓澶婂棘閺佸府绱?
     * @param current 瑜版挸澧犳い鐢电垳
     * @return 閹存垹娈戦崡姘吂閸掓銆?
     */
    List<BlogVO> queryMyBlog(Blog blog, Integer current);

    /**
     * 閺屻儴顕楅崡姘吂鐠囷附鍎忛敍鍫濆瘶閸氼偆鏁ら幋铚備繆閹垽绱?
     *
     * @param id 閸楁艾顓规稉濠氭暛
     * @return 閸楁艾顓圭拠锔藉剰VO
     */
    BlogVO getBlogById(Long id);



    /**
     * 閺屻儴顕楅幐鍥х暰閸掑棛琚稉瀣畱閸楁艾顓归崚妤勩€?
     *
     * @param typeId  閸掑棛琚獻D
     * @param current 瑜版挸澧犳い鐢电垳
     * @return 閸楁艾顓归崚妤勩€?
     */
    List<BlogVO> queryBlogByCategory(Long typeId, Integer current);

    /**
     * 閺嶈宓両D閸掓銆冮幍褰掑櫤閼惧嘲褰囬崡姘吂閿涘牆鎯堥悽銊﹀煕娣団剝浼呴妴浣哄仯鐠х偟濮搁幀渚婄礆
     *
     * @param sourceIdList 閸楁艾顓笽D閸掓銆?
     * @return 閸楁艾顓归崚妤勩€?
     */
    List<BlogVO> getBlogListByIds(List<Long> sourceIdList);
    /**
     * 缂冾噣銆?閸欐牗绉风純顕€銆婇崡姘吂
     *
     * @param blog 閸楁艾顓圭€圭偘缍嬮敍鍫濆瘶閸氱嵒D閸滃瞼鐤嗘い鍓佸Ц閹緤绱?
     * @return 閹垮秳缍旂紒鎾寸亯
     */
    boolean isPin(Blog blog);
    /**
     * 閹靛綊鍣洪弴瀛樻煀閸楁艾顓归悙纭呯閺?
     *
     * @param updateMap 閸楁艾顓笽D娑撳海鍋ｇ挧鐐存殶閻ㄥ嫭妲х亸?
     * @return 閺囧瓨鏌婄紒鎾寸亯
     */
    Boolean updateLikeCountBatch(Map<Long, Integer> updateMap);
    /**
     * 閹靛綊鍣洪弴瀛樻煀閸楁艾顓圭拠鍕啈閺?
     *
     * @param updateMap 閸楁艾顓笽D娑撳氦鐦庣拋鐑樻殶閻ㄥ嫭妲х亸?
     * @return 閺囧瓨鏌婄紒鎾寸亯
     */
    Boolean updateCommentCountBatch(Map<Long, Integer> updateMap);
    /**
     * 閹靛綊鍣洪弴瀛樻煀閸楁艾顓归弨鎯版閺?
     *
     * @param updateMap 閸楁艾顓笽D娑撳孩鏁归挊蹇旀殶閻ㄥ嫭妲х亸?
     * @return 閺囧瓨鏌婄紒鎾寸亯
     */
    Boolean updateStarCountBatch(Map<Long, Integer> updateMap);
    /**
     * 閸忋劑鍎撮崣鎴濈閸楁艾顓?
     *
     * @return 閸忋劑鍎撮崣鎴濈缂佹挻鐏?
     */
    String allPublish();

    /**
     * 閹靛綊鍣洪崣鎴濈閸楁艾顓归懛鐭盨缁便垹绱?
     *
     * @param ids 閸楁艾顓笽D閺佹壆绮?
     * @return 閸欐垵绔风紒鎾寸亯
     */
    String publish(String[] ids);
    /**
     * 閼惧嘲褰囬崡姘吂閻愮绂愰弫?
     *
     * @param sourceId 閸楁艾顓笽D
     * @return 閻愮绂愰弫浼村櫤
     */
    Integer getBlogLikeCount(Long sourceId);
    /**
     * 閼惧嘲褰囬崡姘吂閺€鎯版閺?
     *
     * @param sourceId 閸楁艾顓笽D
     * @return 閺€鎯版閺佷即鍣?
     */
    Integer getBlogStarCount(Long sourceId);
    /**
     * 閼惧嘲褰囬崡姘吂閹粯鏆?
     *
     * @return 閸楁艾顓归幀缁樻殶
     */
    Integer getBlogTotal();
    /**
     * 閺屻儴顕楅悽銊﹀煕閸楁艾顓归弫浼村櫤
     *
     * @param userId 閻劍鍩汭D
     * @return 閸楁艾顓归弫浼村櫤
     */
    Integer getBlogCount(Long userId);

    /**
     * 閺屻儴顕楅悽銊﹀煕閸楁艾顓归懢宄扮繁閻ㄥ嫭鈧崵鍋ｇ挧鐐存殶
     *
     * @param userId 閻劍鍩汭D
     * @return 閻愮绂愰幀缁樻殶
     */
    Integer getLikeCount(Long userId);
    /**
     * 閸掗攱鏌婇崡姘吂缂傛挸鐡ㄩ敍鍫ｎ嚊閹?+ 閸掓銆?+ 閸掑棛琚敍?
     *
     * @return 閸掗攱鏌婄紒鎾寸亯
     */
    String flashCache();
    /**
     * 閺囧瓨鏌婇崡姘吂閻樿埖鈧緤绱欑€光剝鐗抽柅姘崇箖/閹锋帞绮烽敍?
     *
     * @param targetId 閸楁艾顓笽D
     * @param status   閸楁艾顓归悩鑸碘偓?
     * @param reason   閹锋帞绮烽崢鐔锋礈閿涘牓鈧俺绻冮弮鏈佃礋null閿?
     * @return 閺囧瓨鏌婄紒鎾寸亯
     */
    Boolean updateBlogStatus(Long targetId, Integer status, String reason);

    List<BlogVO> searchBlogs(String keyword);
}
