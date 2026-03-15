package com.smartLive.shop.controller;

import java.util.List;

import com.smartLive.common.core.context.SecurityContextHolder;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.shop.domain.VO.ShopAnalysisVO;
import com.smartLive.shop.domain.VO.ShopSuggestVO;
import com.smartLive.shop.domain.VO.ShopVO;
import jakarta.servlet.http.HttpServletResponse;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.enums.common.AuditStatusEnum;
import com.smartLive.common.core.web.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.shop.domain.Shop;
import com.smartLive.shop.service.IShopService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.page.TableDataInfo;

/**
 * 闁圭厧鐡ㄩ〃濠囧箺閻㈢數涓嶉柨娑樺閸婄偤鏌熺挩澶婂暙閻撴垿鎮?
 * 闂佸湱绮崝鎺旀閸偅鍎熸俊銈呮噺閹虫瑩鏌?CRUD 缂傚倷绀侀悺銊︽叏閵忋倕违濞达絽鎼崝浼存⒒閸屾ê濡介悽顖涙尵閹壆浠︾紒銏￠槣闂佸憡甯掑Λ婵嬪Υ婢舵劕钃熼柕澶樼厛閸ゅ嫰鏌曢崱鏇狀槮婵犫偓閹绢喗鍋犻柛鈩冩礈缁夊绱撻崘鎯ф灍闁诡喖纾Σ鎰板閻樺弶灏嬬紓浣风┒閸ㄥ湱妲愰埡鍛闁靛ň鏅涘▍銈夋煕濞嗘劕鐏ョ紒鏃傚厴瀹曟繈鎮㈢拠鎻掑箣闂?
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@RestController

@RequestMapping("/shop")
public class ShopController extends BaseController {
    @Autowired
    private IShopService shopService;

    /**
     * 闂佸憡甯掑Λ婵嬪Υ婢舵劕钃熼柕澶樼厛閸ゅ嫰骞栫€涙﹩娈滈柟鐣屾暬瀹曟艾螖閸曗斁鍋?(闂佸憡鑹炬姝屻亹鐎靛摜涓嶉柨娑樺閸婄偤鏌涢敂鎯у妺婵?
     * 闂佽　鍋撴い鏍ㄧ☉閻︻噣鏌″鍫綈鐎规挷鐒﹂幆鏂课旈崨顔藉殘闂佸憡鑹剧粔鎯扳叿闂侀潧妫旂粈渚€宕规惔锝囧暗婵懓娲犻崑鎾存媴閸涘﹤瀣€闂佺鈧崑鎾剁磼濞戞﹩妲搁柣鈯欏啠鏋斿ù锝呭暟缁犲鎮跺☉妯垮妞わ箑娼″鍫曞灳閸欏鍋ㄥ┑鈽嗗灲缁辨洜娑甸敃鍌氱鐟滅増甯掔敮鎶芥煏?
     *
     * @param shop 闁圭厧鐡ㄩ〃濠囧箺閻㈢钃熼柕澶樼厛閸ゅ嫰鎮楅崷顓炰粧缂?
     * @return 闂佸憡甯掑Λ婵嬪Υ婢舵劕瑙﹂幖杈剧稻閻ｉ亶骞栫€涙﹩娈滈柟鐣屾暬瀵偊鎮ч崼婵堛偊闁荤偞绋忛崝宥夋偋?
     */
    @RequiresPermissions("business:shop:list")
    @GetMapping("/list")
    public TableDataInfo list(Shop shop) {
        startPage();
        List<Shop> list = shopService.selectShopList(shop);
        return getDataTable(list);
    }

    /**
     * 闂佸搫琚崕鎾敋濡や焦鍎熸俊銈呮噺閹虫瑩鏌涢幒鎿冩畽闁?
     */
    @GetMapping("/shopList")
    public AjaxResult shopList(Shop shop) {
        List<Shop> list = shopService.selectShopList(shop);
        return success(list);
    }
    /**
     * 闁诲海鏁搁崢褔宕甸鐔稿劅婵°倕鎳忛幊娆撴煕閹烘搩娈欓柕?
     */
    @RequiresPermissions("business:shop:export")
    @Log(title = "shop", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Shop shop) {
        List<Shop> list = shopService.selectShopList(shop);
        ExcelUtil<Shop> util = new ExcelUtil<Shop>(Shop.class);
        util.exportExcel(response, list, "shop list");
    }

    /**
     * 闂佸吋鍎抽崲鑼躲亹閸ャ劍鍎熸俊銈呮噺閹虫瑩鎮归崶宄邦洭缂侇喖绻戠粚閬嶅焺閸愌呯
     */
    @RequiresPermissions("business:shop:query")
//    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(shopService.selectShopById(id));
    }

    /**
     * 闂佸搫鍊瑰姗€路閸愨晜鍎熸俊銈呮噺閹?
     */
    @RequiresPermissions("business:shop:add")
    @Log(title = "shop", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Shop shop) {
        return toAjax(shopService.insertShop(shop));
    }

    /**
     * 婵烇絽娴傞崰妤呭极閸忓吋鍎熸俊銈呮噺閹?
     */
    @RequiresPermissions("business:shop:edit")
    @Log(title = "shop", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Shop shop) {
        return toAjax(shopService.updateShop(shop));
    }

    /**
     * 闂佸憡甯炴繛鈧繛鍛閹柨螖閸涱喗鍤?
     */
    @RequiresPermissions("business:shop:remove")
    @Log(title = "shop", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(shopService.deleteShopByIds(ids));
    }

    /**
     * 闂佸搫绉烽～澶婄暤娓氣偓瀹曨垶宕卞☉娆愬殘闂佸憡鑹剧粔鎯扳叿闂佺绻戞繛濠囧极椤撶姭鍋撳☉娆樻當闁搞劌瀛╅妵鍕垂椤愩倕寮ㄩ柣鐘叉储閸ㄥ綊寮查姀銈嗙叄閺夌偤顣︾换鍡涙煙?
     *
     * @param name    闂佸摜鍠庡Λ婵嬪箺閻㈢瑙︾€广儱娉﹂悙鍝勭闁规儼濮ら弳娑㈡倵?
     * @param area    闂佸憡鐗曢幖顐︽偂濞嗘挸绀傞柟鎯板Г閺嗘盯鎮?
     * @param current 婵＄偑鍊楅弫鎼佹偉?
     * @return 闂佸摜鍠庡Λ婵嬪箺閻㈢绀嗘俊銈呭閳?
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "area",required = false) String area,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 闂佸搫绉烽～澶婄暤娴ｈ櫣灏甸悹鍥皺閳ь剛鍏樺畷姘跺幢濞戞牑鍋撴径鎰摕闁靛鐓堥崵?
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .like(StrUtil.isNotBlank(area), "address", area)
                .eq("status", 1)
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 闁哄鏅滈弻銊ッ洪弽顓炴瀬闁绘鐗嗙粊?
        return Result.ok(page.getRecords());
    }

    /**
     * 闂佸憡甯￠弨閬嶅蓟婵犲嫮纾介柟鎯х－閹?
     */
    @GetMapping("/flushCache")
    public AjaxResult flushCache() {
        return success(shopService.flushCache());
    }

    /**
     * 闂佺绻堥崝鎴﹀闯濞差亜鐭楅柟瀛樼箘椤忔挳骞栫€涙﹩娈滈柟?
     */
    @PostMapping("/allPublish")
    public AjaxResult allPublish() {
        return success(shopService.allPublish());
    }

    /**
     * 闂佸憡鐟﹂崹鐢电博闁垮鍎熸俊銈呮噺閹?
     */
    @PostMapping("/publish/{ids}")
    public AjaxResult allPublish(@PathVariable String[] ids) {
        return success(shopService.publish(ids));
    }
    /**
     * 闂佸吋鍎抽崲鑼躲亹閸ヮ剚鍊绘い鎾卞灪閿涘矂骞栫€涙﹩娈滈柟鐣屾暬楠炴帡骞掗弴銊︽杸濠?(婵犮垹鐖㈤崒婊庝紘缂傚倷鑳堕崰搴ㄥ垂鎼搭潿浜滈柛顭戝亜婢跺秹鏌?
     * 
     * 闂佸搫绉堕…鍫㈢紦妤ｅ啯鐒婚柡鍕箳鐢棝鏌?
     * 1. 婵炴潙鍚嬮敋闁告ɑ绋掔粋?Redis 缂傚倸鍊归幐鎼佹偤閵婏妇鈻旀い鎾偓宕囶唵闂佸憡鐟﹂悧鏇㈠礄閿熺姴绠板璺鸿嫰閸斾即骞栨潏鍓х暠闁搞劌閰ｉ獮鎺楀箳閹寸姷顣查梺姹囧妼鐎氼剛鑺遍悽鍛婄叄闁告繂瀚喊宥夋煕閹烘挶浠犻柍?
     * 2. 闂佸吋鐪归崕鑼躲亹娴ｅ湱鐟规繛鎴炆戠紞蹇涙煛?(x,y)闂佹寧绋戦懟顖炲垂椤栫偞鍤傞柡鍌氱仢琚熼梺闈╅檮婢瑰棝骞冩繝鍐╁鐎广儱娲ㄩ弸鍌炴偣閻戞绠樻い顒€娲﹀璇测槈濠婂孩鏂€闂佸搫顦崯鎾闯缁嬫鍤楁い鏃囨硶濞堝爼鏌?
     * 3. 婵☆偓绲鹃悧鐘诲Υ婢舵劕绠抽柕濞垮妼缁€鍐煕閿旀儳鍔嬫繛鍛懇閺屽懎顫濋鍌氱厬婵?current=1, size=10闂?
     *
     * @param current 閻熸粎澧楅幐鍛婃櫠閻樼偨浜滈柣銏㈡暩閸?
     * @param size    濠殿噯绲界换姗€濡存径鎰瀬濞村吋娼欏▍?
     * @param x       闂佹椿娼块崝宥夊春濞戞嚎浜归柟鎯у暱椤ゅ懐绱撴担鍝ョ閻?(闂佸憡鐟崹鍫曞焵?
     * @param y       闂佹椿娼块崝宥夊春濞戞嚎浜归柟鎯у暱椤ゅ懐绱掔紒銏犲閻?(闂佸憡鐟崹鍫曞焵?
     * @return 闂佺粯鍩堥崣鍐ㄎ涢浣瑰劅婵°倕鎳忛幊?VO 闂佸憡甯楅〃澶愬Υ?
     */
    @GetMapping("/hot/rank")
    public Result getHotShopRank(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y) {
        return Result.ok(shopService.getHotShopRank(current, size, x, y));
    }

    /**
     * 闂佸搫绉烽～澶婄暤娑旂敠闂佸搫琚崕鎾敋濡ゅ懎鐤柛鈩冪⊕閹虫瑥菐閸ワ絽澧插ù?
     *
     * @param id 闂佸摜鍠庡Λ婵嬪箺缁€绲?
     * @return 闂佸摜鍠庡Λ婵嬪箺閻㈠灚瀚氶柨鏃囨閸撲即鏌℃担鍝勵暭鐎?
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        ShopVO shop = shopService.queryById(id);
        if (shop == null) {
            return Result.fail("shop not found");
        }
        return Result.ok(shop);
    }

    /**
     * 闂佸搫绉烽～澶婄暤娴ｇ懓绶炴慨姗嗗亰閸ゅ骞栫€涙﹩娈滈柟?ID 闂佸綊娼х紞濠囧闯濞差亜钃熼柕澶樼厛閸ゅ嫰骞栫€涙﹩娈滈柟鐣屾暩閹风娀鏁傞挊澶婂
     * 闁汇埄鍨伴幗婊堝极閵堝棛顩查幖娣€曢弸鐘绘煟濡炵粯娅堟繝褍鎳樻俊瀛樻媴鐟欏嫭姣嗛梺鑺ッ换鎰板Φ濞嗘垹椹冲鑸靛姈娴犳﹢鎮烽弴姘鳖槮闁活偄绉剁划鍫ュ传閸曞灚鈻兼繛鎴炴惄娴滄粎鑺遍悽鍛婄叄闁告繂瀚崬銊х棯椤撴稑浜炬繛锝呮礌閸撴繃瀵奸崨瀛樺剭闁告洦鍋呯花姘舵煛閸滀礁鐏涢柍?
     *
     * @param ids 闂備緡鍋呴〃鍛般亹閸ф绀嗛柛鈩冪⊕椤撻箖鏌ｉ妸銉ヮ仼缂併劎鏁婚弻?ID 闁诲孩绋掗〃鍫ヮ敄娴ｅ湱鈻?(婵炴挻鑹鹃鍛淬€? "1,2,3")
     * @return 闁圭厧鐡ㄩ〃濠囧箺閻㈠灚瀚氶柨鏃囨閸?VO 闂佸憡甯楅〃澶愬Υ?
     */
    @GetMapping("/listByIds")
    public Result queryShopByIds(@RequestParam("ids") String ids) {
        if (StrUtil.isBlank(ids)) {
            return Result.ok(new java.util.ArrayList<>());
        }
        java.util.List<Long> idList = java.util.Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(java.util.stream.Collectors.toList());
        java.util.List<ShopVO> shops = shopService.getShopList(idList);
        if (shops == null) {
            return Result.ok(new java.util.ArrayList<>());
        }
        shops = shops.stream()
                .filter(shop -> shop != null
                        && java.util.Objects.equals(shop.getStatus(), 1)
                        && java.util.Objects.equals(shop.getAuditStatus(), AuditStatusEnum.PASS.getCode()))
                .collect(java.util.stream.Collectors.toList());
        return Result.ok(shops);
    }

    @GetMapping("/analysis/{shopId}")
    public AjaxResult getShopAnalysis(@PathVariable("shopId") Long shopId,
                                      @RequestParam(value = "timeRange", defaultValue = "week") String timeRange) {
        return success(shopService.getShopAnalysis(shopId, timeRange));
    }

    @GetMapping("/suggest/{shopId}")
    public AjaxResult getShopSuggest(@PathVariable("shopId") Long shopId,
                                     @RequestParam(value = "timeRange", defaultValue = "week") String timeRange) {
        return success(shopService.getShopSuggest(shopId, timeRange));
    }
    @GetMapping("/search")
    public Result searchShops(@RequestParam(name = "keyword", required = false) String keyword) {
        return Result.ok(shopService.searchShops(keyword));
    }
}
