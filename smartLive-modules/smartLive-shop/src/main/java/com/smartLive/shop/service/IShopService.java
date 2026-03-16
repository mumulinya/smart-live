package com.smartLive.shop.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.shop.domain.Shop;
import com.smartLive.shop.domain.VO.ShopAnalysisVO;
import com.smartLive.shop.domain.VO.ShopSuggestVO;
import com.smartLive.shop.domain.VO.ShopVO;

/**
 * 搴楅摵涓氬姟濂戠害鎺ュ彛
 * 定义了店铺的基础维护、多级缓存管理（含穿透、击穿解决）、热门排行榜计算及地理位置检索逻辑。
 *
 * @author smartLive
 * @date 2026-03-11
 */
public interface IShopService extends IService<Shop> {
    /**
     * 鏌ヨ搴楅摵
     *
     * @param id 搴楅摵涓婚敭
     * @return 搴楅摵
     */
    Shop selectShopById(Long id);

    /**
     * 鏌ヨ搴楅摵鍒楄〃
     *
     * @param shop 搴楅摵
     * @return 搴楅摵闆嗗悎
     */
    List<Shop> selectShopList(Shop shop);

    /**
     * 鏂板搴楅摵
     *
     * @param shop 搴楅摵
     * @return 缁撴灉
     */
    int insertShop(Shop shop);

    /**
     * 淇敼搴楅摵
     *
     * @param shop 搴楅摵
     * @return 缁撴灉
     */
    int updateShop(Shop shop);

    /**
     * 鎵归噺鍒犻櫎搴楅摵
     *
     * @param ids 闇€瑕佸垹闄ょ殑搴楅摵涓婚敭闆嗗悎
     * @return 缁撴灉
     */
    int deleteShopByIds(Long[] ids);

    /**
     * 鍒犻櫎搴楅摵淇℃伅
     *
     * @param id 搴楅摵涓婚敭
     * @return 缁撴灉
     */
    int deleteShopById(Long id);


    /**
     * 鏍规嵁涓婚敭鏌ヨ鍟嗛摵璇︽儏 (鏁村悎缂撳瓨閫昏緫)
     * 鏀寔閫氳繃閫昏緫杩囨湡鍙婁簰鏂ラ攣绛栫暐瑙ｅ喅楂樺苟鍙戜笅鐨勭紦瀛樺嚮绌块棶棰樸€?     *
     * @param id 鍟嗛摵 ID
     * @return 鍟嗛摵璇︽儏 VO (鍚敹钘忋€佸叧娉ㄧ姸鎬?
     */
    ShopVO queryById(Long id);

    /**
     * 鏍规嵁鍟嗛摵鍚嶇О鏌ヨ鍟嗛摵淇℃伅
     *
     * @param shopName 鍟嗛摵鍚嶇О
     * @return 鍟嗛摵璇︽儏
     */
    ShopVO getShopByShopName(String shopName);

    /**
     * 鏍规嵁鏉′欢鏌ヨ鍟嗛摵淇℃伅
     *
     * @param shop 鎼滅储鏉′欢
     * @return 鎼滅储缁撴灉
     */
    List<ShopVO> getShopByCondition(Shop shop);

    /**
     * 鏍规嵁鍟嗛摵id鍒楄〃鏌ヨ鍟嗛摵淇℃伅鍒楄〃
     *
     * @param ids 鍟嗛摵id鍒楄〃
     * @return 鍟嗛摵鍒楄〃
     */
    List<ShopVO> getShopList(List<Long> ids);

    /**
     * 鎵嬪姩閲嶇疆骞跺埛鏂板晢閾哄叏閲忕紦瀛?     * 鍖呭惈搴楅摵鍩烘湰淇℃伅缂撳瓨鍙婂湴鐞嗕綅缃?(GEO) 绱㈠紩缂撳瓨銆?     *
     * @return 鎵ц缁撴灉鎻忚堪
     */
    String flushCache();

    /**
     * 鍏ㄩ儴鍙戝竷搴楅摵
     *
     * @return 鍏ㄩ儴鍙戝竷缁撴灉
     */
    String allPublish();

    /**
     * 鎵归噺鍙戝竷搴楅摵鑷矱S鍜孧ilvus绱㈠紩
     *
     * @param ids 搴楅摵ID鏁扮粍
     * @return 鍙戝竷缁撴灉
     */
    String publish(String[] ids);

    /**
     * 鑾峰彇鍟嗛摵鎬绘暟
     *
     * @return 鍟嗛摵鎬绘暟
     */
    Integer getShopTotal();

    /**
     * 鑾峰彇鏈€杩戝晢閾?     *
     * @param limit 鑾峰彇鏁伴噺
     * @return 鏈€杩戝晢閾?     */
    List<ShopVO> getRecentShops(Integer limit);

    /**
     * 鎵归噺鏇存柊鍟嗛摵鏀惰棌鏁?     *
     * @param updateMap 鍟嗛摵id鍜屾敹钘忔暟
     * @return 鏇存柊缁撴灉
     */
    Boolean updateStarCountBatch(Map<Long, Integer> updateMap);

    /**
     * Batch update shop fans count.
     *
     * @param updateMap shopId -> fansCount
     * @return update result
     */
    Boolean updateFansCountBatch(Map<Long, Integer> updateMap);

    /**
     * 鎵归噺鏇存柊鍟嗛摵璇勪环鏁?     *
     * @param updateMap 鍟嗛摵id鍜岃瘎浠锋暟
     * @return 鎵归噺鏇存柊缁撴灉
     */
    Boolean updateReviewCountBatch(Map<Long, Integer> updateMap);

    /**
     * 鏇存柊搴楅摵鐘舵€?     *
     * @param id     搴楅摵ID
     * @param status 鐘舵€?     * @param reason 鎷掔粷鍘熷洜锛堥€氳繃鏃朵负null锛?     * @return 缁撴灉
     */
    Boolean updateShopStatus(Long id, Integer status, String reason);

    /**
     * 鑾峰彇鐑棬搴楅摵鎺掕姒?     * 缁撳悎 Redis 鐑害鍒嗗簭鍒椾笌鐢ㄦ埛鍦扮悊浣嶇疆 (x,y) 璁＄畻缁煎悎鎺掑悕銆?     *
     * @param current 椤电爜
     * @param size    姣忛〉鏁伴噺
     * @param x       鐢ㄦ埛褰撳墠缁忓害 (鍙€?
     * @param y       鐢ㄦ埛褰撳墠绾害 (鍙€?
     * @return 鍖呭惈鐑害璇勫垎涓庤窛绂讳俊鎭殑搴楅摵 VO 鍒楄〃
     */
    List<ShopVO> getHotShopRank(Integer current, Integer size, Double x, Double y);

    /**
     * 鎵归噺鏇存柊閿€閲?     *
     * @param updateMap 搴楅摵id鍜岄攢閲?     * @return 缁撴灉
     */
    Boolean updateSoldBatch(Map<Long, Integer> updateMap);

    /**
     * 鍚屾閿€閲忔暟鎹粠 Redis 鍒版暟鎹簱
     */
    void syncSalesData();

    /**
     * 鏍规嵁鐢ㄦ埛id鏌ヨ鎵€灞炲簵閾?     * @param userId
     * @param shop
     * @return
     */
    List<Shop> selectShopListByUserId(Long userId, Shop shop);
    ShopAnalysisVO getShopAnalysis(Long shopId, String timeRange);

    ShopSuggestVO getShopSuggest(Long shopId, String timeRange);
    List<ShopVO> searchShops(String keyword);
}
