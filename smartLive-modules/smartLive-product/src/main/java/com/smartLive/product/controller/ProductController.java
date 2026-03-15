package com.smartLive.product.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.product.domain.VO.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.product.domain.Product;
import com.smartLive.product.service.IProductService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.page.TableDataInfo;

/**
 * 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粌娴风划娆愬緞鐎ｎ偄鍔呭┑鐘绘涧濡瑩寮抽弴銏♀拻濞达絿顭堢痪褔鏌涢妸褎鏆€规洖鎼…銊╁川椤栨瑧鐟濋梻浣瑰缁嬫垹鈧凹鍙冨畷鎰版倷閻戞鍘甸柣鐘妼椤︻垶宕锔藉亗闁硅揪闄勯悡?
 * 闂傚倷绀佸﹢杈╁垝椤栫偛绀夐柟鐑樻⒐椤愪粙鏌ｉ姀銏╃劸闁汇値鍣ｉ弻娑㈠焺閸愮偓鐣烽梺璇″灠閸婂潡寮婚妸銉㈡婵☆垳鍘ч。娲煟鎼达紕浠涢柨鏇樺灩閻ｇ兘濡烽埡濠冩櫇闂佹寧绻傚Λ娑溿亹椤栫偞鈷掑〒姘搐娴滄繈鏌よぐ鎺旂暫鐎殿喗鐓℃慨鈧柕鍫濇噸缁辫埖绻涙潏鍓ф偧妞わ絼绮欏畷鎴﹀箻鐎涙ê顎撻悗鐟板婢ф寮抽弴銏♀拻濞达絿顭堢痪褔鏌涢妸褎鏆€规洖鎼…銊╁醇濠㈩亗鍎遍湁闁挎繂鎳忛崯鐐电磼閳ь剛鈧綆鍠楅崑锝夋煙娴煎瓨娑ф鐐寸墬閵囧嫰濡搁敂鍓х厑濡炪倧璁ｇ粻鎾荤嵁閸ヮ剙绀堝ù锝勮濡叉挳姊绘担鍦菇闁告柨鐬奸埀顒佸嚬閸撶喕鐭鹃梺鍛婂姦閸犳牜绮堥崱娑欑厽婵☆垱瀵ч悵顏堟煛閸♀晛寮柡宀嬬磿娴狅妇鎷犻幓鎺戭潙闂備線鈧偛鑻崢鍝ョ磼閳ь剚鎷呮ウ鍨￠柡澶婄墐閺備線宕戦幘鏂ユ婵炲棙顭囬崝宄邦渻閵堝棙灏伴柟顔煎€块獮鍐醇閺囩偛鑰块梺纭呭焽閸斿本绂嶉幆顬″綊鏁愰崨顔藉創缂佹鍨垮铏规嫚閳ュ啿骞愰梺缁樺釜缁犳垹鍙呴梺绉嗗嫷娈旀俊顐ｏ耿閺屸剝寰勬繝鍕檸闂佽鎮堕崐婵嬪蓟閿濆绠ｆい鎾跺仜缁犲搫顪冮妶搴濈胺缂佽埖鑹鹃锝嗙節濮橆儵銊︺亜椤撶喎鐏ユ繛鍫熷灴濮婃椽宕ㄦ繝鍐ｆ嫽闂佸摜濮靛ú鐔风暦?
 *
 * @author smartLive
 * @date 2026-02-18
 */
@RestController
@RequestMapping("/product")
public class ProductController extends BaseController {
    @Autowired
    private IProductService productService;

    /**
     * 闂傚倷绀侀幉锛勬暜閹烘嚦娑樷攽鐎ｎ€儱顭块懜闈涘闁藉啰鍠栭弻鏇熷緞濡厧甯ラ梺鎼炲€曠€氫即寮诲☉銏″亜闂佸灝顑愬Λ鐐烘⒑闁稑灏冮柛銉戝拋妲存繝寰锋澘鈧洜鈧哎鍔戦崺鈧?
     * 闂備浇宕垫慨鏉懨洪姀銈呯？闁哄被鍎遍拑鐔兼煟閺傝法娈遍柡瀣墵閺岋繝宕堕敐鍡楁珰闂佺顑嗛幑鍥х暦閹烘鍊烽柟缁樺笩缁?Ruoyi 闂傚倷娴囧銊╂嚄閼稿灚娅犳俊銈傚亾闁伙絽鐏氶幏鍛存倻濡桨绮ч梻渚€娼ч…顓熶繆閸ャ劎鐝堕柟閭﹀枤绾捐棄霉閿濆浂鐒炬い寰板嫮绠鹃柛鈩冨姇閻忔煡鏌℃担鍝バ㈤柣锝嗙箞瀹曟﹢顢旈崱鈺佹暭闂傚倷鐒﹂幃鍫曞磿閹惰棄纾绘繛鎴炲焹閸嬫捇宕归顒冣偓璺ㄢ偓瑙勬磸閸庣敻銆侀弴銏℃櫜闁稿本鐭竟鏇犵磽閸屾瑧鍔嶉拑閬嶆煢閸屾凹鍎旈柡灞稿墲閹峰懘鎮烽柇锔筋棆婵?TableDataInfo 闂備浇顕уù鐑藉极閹间降鈧焦绻濋崶銊ョ樁?
     *
     * @param product 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍕妽濞存嚎鍊濋弻娑㈠Ψ閿濆懎顬夋繝娈垮枔閸ㄤ粙寮婚埄鍐ㄧ窞闁糕€崇箰娴滈箖鏌涘▎蹇ｆ▓闁?
     * @return 闂傚倷绀侀幉锛勬暜閹烘嚦娑樷攽鐎ｎ€儱顭块懜闈涘閻熸瑱绠撻獮鏍ㄦ綇閸撗吷戦梺浼欑秮娴滃爼寮诲☉銏″亜闂佸灝顑愬Λ鐐烘⒑闁稑灏冮柛銉戝啫鎸ら梻浣告啞閹稿棙鎷呮搴ㄢ攺闂備浇顕уù鐑藉极閹间降鈧焦绻濋崶銊ョ樁闂佸憡娲﹂崹鎵矆閸℃ü绻嗛柕鍫濇噹椤忋儵鏌?(ProductVO)
     */
    @RequiresPermissions("business:product:list")
    @GetMapping("/list")
    public TableDataInfo list(Product product) {
        startPage();
        List<ProductVO> list = productService.selectProductList(product);
        return getDataTable(list);
    }

    /**
     * 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎缂佲偓閸岀偞鐓忓┑鐐靛亾濞呭懏绻涢崨顐⑩偓婵嬪蓟閻旇　鍋撳☉娆樼劷濠⒀冪－缁辨帞鎷犻幓鎺撴濡炪値鍘归崝鎴炰繆閸洖宸濇い鏂跨毞閸嬫捇鎮介崨濠勫幗闂侀潧顭堥崕閬嶎敂閳哄懏鐓涢柛娑卞亜閻忚尙鈧娲﹂崑鍛村箯閸涱垳鐭欓悹鎭掑妺缁挳姊?(AjaxResult 闂傚倷绀侀幉锟犳偋閺囥垹绠犻柟鎯у殺?
     * 闂備焦鐪归崺鍕垂娴兼潙绠烘繝濠傜墕閺嬩線鏌曢崼婵囶棤妞も晜鐓￠獮鏍偓娑欘焽缁犳﹢鏌ｉ悢鍛婂磳闁哄瞼鍠庨悾锟犳焽閿斿彨褍顪冮妶鍐ㄧ仼妞ゆ垵顦甸悰顔锯偓锝庡枛閸愨偓闂侀潧顭拋锝嗩殽韫囨稒鈷掗柛灞剧缁€宀勬煕鐎ｎ偅灏い顓℃硶閹瑰嫰鎼归崜鎰剁稻缁绘盯鎮℃惔銏犳畻閻庤娲樺畝绋跨暦閸楃偐妲堥柡宥冨€曟禍鎯ь熆閼搁潧濮堥柛搴＄Ч閺屾盯寮撮妸銉ょ盎濡炪們鍊曢幊鎰閹烘绫嶉柟瀛樼箘娴犫晛鈹戦悩娆屽亾闁稿鎹囧?
     *
     * @param product 闂備礁鎼ˇ顐﹀疾濞戞◤娲晝閸屾氨顔呴梺闈涚墕椤︻垳鐥閺屽秹濡烽妷銉ч獓濡?
     * @return 闂傚倷绀侀幉锟犳偋閺囥垹绠犻幖娣妼缁犳岸鏌涢鐘插姎闁汇値鍣ｉ弻娑㈠焺閸愮偓鐣烽梺?VO 闂傚倷绀侀幉锛勬暜濡ゅ懌鈧啯寰勯幇顑┿儵鏌涢幇闈涙灍闁?AjaxResult
     */
    @GetMapping("/productList")
    public AjaxResult productList(Product product) {
        List<ProductVO> list = productService.selectProductList(product);
        return success(list);
    }

    /**
     * 闂備浇顕уù鐑藉极閹间礁鍌ㄧ憸鏂跨暦閻㈠壊鏁囬柕蹇曞Х閺屽牓姊虹化鏇炲⒉妞ゃ劌妫濋獮鍡涘磼閻愬鍘遍梺鍦劋閹尖晛鈻撳▎鎾寸厪?
     */
    @RequiresPermissions("business:product:export")
    @Log(title = "product", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Product product) {
        List<Product> list = productService.selectProductEntityList(product);
        ExcelUtil<Product> util = new ExcelUtil<Product>(Product.class);
        util.exportExcel(response, list, "product list");
    }

    /**
     * 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎缂佺姳鍗抽弻娑㈠Ψ閹存繃鍣烘慨?ID 闂傚倷鐒﹂惇褰掑礉瀹€鈧埀顒佸嚬閸撶喎顕ｉ弻銉ヮ潊闁靛牆鎳嶇槐鑸电箾鏉堝墽鎮奸柣鈩冩瀹曞灚绻濋崟顓狅紳婵炶揪绲介幉锟犓夊▎寰㈠酣宕惰闊剚顨?
     * 闂備礁鎼ˇ顐﹀疾濠婂牆钃熼柕濞垮剭?ProductVO闂傚倷鐒︾€笛呯矙閹达附鍤愭い鏍ㄥ閸嬫捇宕归顒冣偓璺ㄢ偓瑙勬磸閸庣敻銆侀弴銏狀潊闁靛繆鍓濋鍌炴⒒閸屾瑧鍔嶉柛搴㈠▕楠炴牠顢曢敃鈧粻鏍煛閸モ晛啸闁活厽鎹囬弻銈夊礈閹绘帒骞嬮梺绋款儐閹稿骞忛崨顖滅煓閻犳亽鍔夐崑鎾绘嚒閵堝洨锛滃銈嗗姂閸婃鏁☉姘ｅ亾濞堝灝鏋熼悗绗涘喚鐒藉┑鐘宠壘缁犮儲銇勯弮鍌氫壕闁诡垽缍佸铏圭矙鐠恒劎顔囬梺姹囧€曞ú锔剧矉閹烘閱囬柣鏃囥€€閺€鎶芥⒑閺傘儲娅呴柛鐔叉櫇濡?
     *
     * @param id 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粌娴风划娆愬緞鐎ｎ剛鐦堝┑顔斤供閸樿棄鈻?ID
     * @return 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粌绻橀獮蹇涙偐缂佹ɑ娅嗛梺鑺ッˇ顖涚珶?
     */
    @RequiresPermissions("business:product:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(productService.selectProductById(id));
    }

    /**
     * 闂傚倷绀侀幖顐﹀磹閻熼偊鐔嗘慨妞诲亾鐠侯垶鏌涢幇闈涙灈闁汇値鍣ｉ弻娑㈠焺閸愮偓鐣烽梺?
     */
    @RequiresPermissions("business:product:add")
    @Log(title = "product", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Product product) {
        return toAjax(productService.insertProduct(product));
    }

    /**
     * 婵犵數鍎戠徊钘壝归崒鐐茬獥婵°倕鎳庨弸浣糕攽閸屾粠鐒鹃柣銈庡櫍閺屾盯鍩勯崘鐐暦闂?
     */
    @RequiresPermissions("business:product:edit")
    @Log(title = "product", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Product product) {
        return toAjax(productService.updateProduct(product));
    }

    /**
     * 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粍鏌ㄩ锝夘敆閸曨偆楠囧┑鐐叉閸嬫挸顭块弮鍫熺厵闁绘挸娴烽幗鐘绘煙閸涘﹥鍊愰柟?
     */
    @PostMapping("/addStock/{id}")
    public AjaxResult addStock(@PathVariable("id") Long id) {
        return toAjax(productService.addStock(id));
    }

    /**
     * 闂傚倷绀佺紞濠傤焽瑜旈、鏍川椤旇棄寮块梺鍐叉惈閸燁垶宕伴崱娑欑叆闁哄洦顨呮禍楣冩偡濠婂嫭绶查柛濠傜仢閻ｇ兘顢涢悙鎻掕€垮┑掳鍊撻悞锕傚磿瀹€鍕拻闁稿本鑹鹃鈺冪磼婢跺鈻曢柟顔藉▕椤㈡盯鎮欑€电濮烽梻浣告惈鐎氼剛鎹㈤幒鏃€鏆?
     * 闂備浇宕垫慨鏉懨洪姀銈呯？闁肩⒈鍓氶弳婊堟煙缂併垹鏋涢柣顓燁殜閺屻劌鈹戦崱妤婁紑缂備焦鍔栭〃濠囧蓟閿熺姴鐐婄憸蹇涘箺閻樼粯鐓涢悘鐐存灮闊剛鈧娲﹂崹璺虹暦閸楃倣鏃€鎷呴悷鏉垮闂傚倷鐒﹂惇褰掑礉瀹€鈧埀顒佸嚬閸ｏ綁鎮伴鈧畷鍫曞Ω瑜滃ù鍕⒑閸涘﹤鐏熼柛濠冾殘缁牊寰勭€ｅ灚顔旈梺缁樺姌閸╂牜娑甸崜褎鍠愰柣妤€鐗忛崣鈧梺纭呮珪椤ㄥ﹤鐣烽幇鐗堝亜婵犲﹤瀚粈瀣偓瑙勬穿缂嶄線鐛€ｎ喗鍋愮€规洖娲㈤崑娑㈡⒒娴ｄ警鐒剧紒缁樺姉婢规洟顢橀悙鈺傜亖闂佺鎻梽鍕煕閺冨倵鍋撻獮鍨姎閻庢凹鍣ｉ悰顕€宕卞☉娆屾嫼濡炪倖鍔х徊楣冩儍閹达附鐓熼柟鍨缁♀偓濡ょ姷鍋涢澶婎嚕娴犲惟鐟滃秹鎮樻潏鈺冪＝濞达絽婀遍妴鎺楁煕鐎ｎ偅灏悡銈夋煕瑜庨〃鍛?
     */
    @PostMapping("/priceReduced/{id}")
    public AjaxResult priceReduced(@PathVariable("id") Long id) {
        return toAjax(productService.priceReduced(id));
    }

    /**
     * 婵犵數鍎戠徊钘壝归崒鐐茬獥婵°倕鎳庨弸浣糕攽閸屾粠鐒鹃柣銈庡櫍閺屾盯鍩勯崘鐐暦闂佽鍨伴崐鍧楀箖濡ゅ懎绀傚璺猴工閳峰绻濋姀锝庢綈閻㈩垽绻濋獮鍐煛閸涱喖娈濈紒鍓у閿氬ù?
     * 闂傚倷娴囬妴鈧柛瀣尰閵囧嫰寮介妸褉妲堥梺?ON_SHELF(闂佽姘﹂～澶愭偤閺囩儐鍤曢柟鎯у缁犳棃鏌涘畝鈧崑娑㈠礄?, OFF_SHELF(闂佽姘﹂～澶愭偤閺囩儐鍤曢柟鎯у缁犳棃鏌涘☉娆愮稇闁?, EXPIRED(闂佽姘﹂～澶愭偤閺囩姳鐒婃い蹇撴瀹曟煡鏌涢幇闈涙灈閻? 闂傚倷鑳剁划顖炩€﹂崼銉ユ槬闁哄稁鍘奸悞鍨亜閹达絾纭堕柛鏂跨Ф缁辨帗绗熸繝鍥€嶅┑?
     *
     * @param id 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇?ID
     * @param status 闂傚倷鑳堕崕鐢稿疾閳哄懎绐楁俊銈呮噺閸嬪鏌ㄥ┑鍡╂Ч闁稿鍔欓弻銈夊传閵夘喗姣岄梺绋款儐閹稿骞忛崨瀛樻優闁荤喐澹嗘禒?
     * @param reason 闂傚倷绀侀崥瀣儑瑜版帒纾块梺顒€绋侀弫鍥煙闂傚顦﹂柛鎰ㄥ亾缂傚倷绀侀鍫ユ晸閵夛妇顩插┑鍌氭啞閸嬶綁鏌熼幍铏珔闁告梻鏁哥槐鎺撴綇閵娧呯暫缂備浇顕ч幊姗€銆侀弴銏狀潊闁绘娅曢鐔兼⒒娴ｅ湱婀介柛鏂跨灱閳ь剚鍑归崜鐔镐繆閻㈢绠ｉ柣妯哄暱椤忔悂姊洪棃娑辨Ф闁稿酣浜跺顒冾樄闁?
     */
    @PostMapping("/updateProductStatus")
    public Boolean updateProductStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status, @RequestParam(value = "reason", required = false) String reason) {
        return productService.updateProductStatus(id, status, reason);
    }

    /**
     * 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎闁稿﹦绮妵鍕箻閸楃偟浠鹃梺鎸庣☉閻倿寮诲☉銏″亜闂佸灝顑愬Λ鐐烘⒑闁稑灏冮柛銉戝拋鍞洪梻浣告贡閸庛倕煤閿曞倸缁╅柤鎭掑劜閸欏繘鏌熼悜妯诲暗闁绘粏宕电槐鎺撴綇閵娧勫櫚濡ょ姷鍋為崝鏍ь嚗閸曨偆鏆嗛柛鎰亾閽戝姊绘担鍛婂暈閻㈩垪鏅犲畷鎴﹀礋椤撴繄鍓ㄥ銈嗙墱閸嬫稓绮堥崱娑欑厱闁斥晛鍟粈鈧梺杞拌閺呯姴顫忓ú顏勭煑濠㈣泛锕︽禒鎾⒑?闂傚倷鐒﹂幃鍫曞磿椤栫偛鍨傚┑鍌滎焾缁愭淇婇婵嗗惞闂傚嫬瀚穱濠囧Χ閸涱喖顎涘┑鈽嗗灙閸嬫捇姊?
     * 婵犵妲呴崑鎾跺緤妤ｅ啯鍋嬮柣妯款嚙杩濇繝銏ｆ硾閺堫剛鈧凹鍓熼弻娑㈠箛閸忓摜鍑归梺鍝勬缁绘﹢寮婚妸銉㈡婵炲棙鍨归崣婵堢磽?current=1, size=10, category=1 闂?2
     * 濠电姵顔栭崰妤冩暜濡ゅ啫鍨濋悘鐐垫櫕閺嗭箓鏌ｉ弮鈧幃鑸电濠婂厾褰掓晲閸涱厽娈跺┑鐐茬墕閻栧ジ寮婚敐澶婎潊闁冲搫鍟敮銊х磽娴ｆ垝鍚褎顨堥埀?current=n, size=10, category=1 闂?2
     *
     * @param current  婵犵绱曢崑鎴﹀磹濡ゅ懎鏋侀柟闂寸劍閸嬪绻濇繝鍌滃闁哄拋鍓涢埀顒€鍘滈崑鎾绘煕閺囥劌骞栫紒妤嬬節閺?1
     * @param size     濠电姵顔栭崳顖滃緤閻ｅ本宕叉慨妞诲亾濠碘€崇摠瀵板嫰骞囬浣衡偓顒佺箾閺夋垵鎮戞繛鍏肩懃閳诲秹濡舵径瀣弳濠电偞鍨堕…鍥倿閸濄儳纾兼い鏇炴噹閻忥綁鏌?10
     * @param category 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粌绻掗崚鎺楀棘濞嗘儳鎮戦梺鎼炲労娴滆泛顭?(1:婵犵數鍋涢顓熷垔鐎靛摜绀婂〒姘ｅ亾鐎规洝娅曢妶锝夊礃閵娧屾Т? 2:闂傚倷鐒﹂幃鍫曞磿椤栫偛鍨傚┑鍌滎焾缁愭淇婇婵嗗惞闂傚嫬瀚穱濠囧Χ閸涱喖顎涘┑鈽嗗灙閸? 闂傚倸顭崑鍕洪妶澶婄疇鐎广儱娲﹂～鏇㈡煕椤愶絾绀冮柡鍜佸墴閹﹢鎮欓懜娈挎濠碘槅鍋呴敃銏ゅ蓟?ProductEnum
     */
    @GetMapping("/hot/rank")
    public Result getHotProductRank(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "category") Integer category) {
        if (category == null || (category != 1 && category != 2)) {
            return Result.fail("闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎闁汇値鍣ｉ弻娑㈠焺閸愮偓鐣烽梺璇″灠閸婂潡寮婚悢鍏煎€绘俊顖濇椤戝嫬顪冮妶鍐ㄧ仾闁搞劏娉涢銉╁礋椤掍胶绉跺銈嗗姂閸庢盯寮撮姀锛勫弳濠电偞鍨堕悷銊︾珶濮椻偓閺岋繝宕ㄩ銏紙閻庤娲﹂崑鍛村箯閸涱垳鐭欓悹鎭掑妷閸嬫捇鎳￠妶鍥╋紲濡炪倖鍔戦崕娲吹濞嗘垟鍋撳▓鍨珮闁告挾鍠庨?category 婵犵數鍋為崹鍫曞箰閸濄儳鐭撻柟缁㈠枛缁犳牠骞栫划瑙勵€嗛柡?闂備礁婀遍崢褔鎮洪妸鈺佽摕鐟滃繘骞?闂?)");
        }
        return Result.ok(productService.getHotProductRank(current, size, category));
    }

    /**
     * 闂傚倷绀侀幉锛勬暜閻愬绠鹃柍褜鍓氱换娑㈠川椤撱垹寮伴悗瑙勬处閸ㄨ泛鐣烽崡鐐嶆梹鎷呴悷鏉垮
     */
    @RequiresPermissions("product:product:remove")
    @Log(title = "product", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        return toAjax(productService.deleteProductByIds(ids));
    }

    /**
     * 闂傚倷鑳堕…鍫㈡崲閸儱绀夐柟杈剧畱闂傤垱绻涘顔荤盎闁活厽顨婇弻鐔衡偓娑欘焽缁犳ɑ銇勮箛鏃€灏﹂柡灞剧洴閹垽鏌ㄧ€ｅ灚顥ｉ梻渚€鈧稑灏冮柛鏇ㄥ幐閺€鎶芥⒑閺傘儲娅呴柛鐔叉櫇濡?
     */
    @PostMapping("/allPublish")
    public AjaxResult allPublish() {
        return success(productService.allPublish());
    }

    /**
     * 闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柣銏㈡暩閸楁岸鏌ｉ幋锝嗩棄闁汇値鍣ｉ弻娑㈠焺閸愮偓鐣烽梺璇″灠閸婃瓕褰侀梺鎼炲劵缁茶姤鏅堕幓鎹?
     */
    @PostMapping("/publish/{ids}")
    public AjaxResult allPublish(@PathVariable String[] ids) {
        return success(productService.publish(ids));
    }

    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍕妽闁稿海鍠愭穱濠囧Χ閸涱喖娅ら梺纭呮珪閻熲晠寮婚敐澶娢╅柕澶堝労娴犲ジ姊洪崫鍕櫡闁搞劏妫勯悾鐑筋敃閿濆棗顎撶紓浣割儓濞夋洘绂掑鑸电厽?
     *
     * @param product
     * @return 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粍鏌ㄩ悾宄邦潩椤戔晜妫冨畷鐔煎煘閹傚?
     */
    @GetMapping("/listByShop")
    public Result queryProductOfShop(Product product) {
        List<ProductVO> productList = productService.queryProductOfShop(product);
        return Result.ok(productList);
    }

    /**
     * 闂傚倷绀侀幖顐ょ矓閻戞枻缍栧璺猴功閺嗐倕銆掑锝呬壕閻庤娲﹂崹璺虹暦閸楃倣鏃€鎷呴悷鏉垮id闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎闁汇値鍣ｉ弻娑㈠焺閸愮偓鐣烽梺?
     * @param id
     * @return
     */
    @GetMapping(value = "/getProductById/{id}")
    public Result getProductById(@PathVariable("id") Long id) {
        return Result.ok(productService.getProductById(id));
    }

    @GetMapping("/search")
    public Result searchProducts(@RequestParam(name = "keyword", required = false) String keyword) {
        return Result.ok(productService.searchProducts(keyword));
    }

    /**
     * 闂備浇宕垫慨鐢稿礉閿曞倸鍌ㄦ繛宸簻缁?闂傚倷鑳堕、濠勭礄娴兼潙鍨傚┑鍌滎焾缁愭鎱ㄥ璇蹭壕閻庤娲﹂崹璺虹暦閸楃倣鏃€鎷呴悷鏉垮闂傚倷娴囬～澶嬬娴犲纾块弶鍫亖娴?(闂傚倷绀侀幖顐ょ矓閸洍鈧箓宕奸姀銏㈠闂佺粯鍔﹂崜娑㈠煝閺冣偓閹便劌顫滈崱妤€顫╁?
     * 闂傚倷绀侀幉锟犲礉閺囥垹绠犳慨妞诲亾鐎规洘娲熼獮姗€顢欓挊澶嬬杹婵＄偑鍊曠换鎰偓姘煎墲椤ゅ嫰姊绘担鍛婃儓缂佸鍏橀敐鐐村緞婵犲嫭娈兼繛鎾村焹閸嬫挾鈧娲﹂崹璺虹暦閸楃倣鏃€鎷呴悷鏉垮缂傚倸鍊风欢锟犲磻婢舵劦鏁嬬憸鏃堝箖濡ゅ懏鍊婚柤鎭掑劜濞呮牠姊洪崜鎻掍簽闁哥姵鍔欓幃鐢割敋閳ь剙顫?缂傚倸鍊风粈渚€藝閹殿喗鏆滄俊銈傚亾妞ゎ厼娲畷姗€顢欓懞銉︾彨濠电姰鍨煎▔娑㈩敄閸涙潙鐓曢柡鍐ㄧ墛閸嬬姵绻涢幋鐑嗙劸閻庢氨澧楅妵鍕即閵娿儲鐏嶉梺璇″灡濡啫顕ｉ鈧畷鎺戔槈濮橆厽顔忛梻鍌欑濠€閬嶃€傞鎯х筏濞寸姴顑呴梻顖炴煟閹寸倖鎴﹀磿閻斿吋鐓欓柟顖嗗拑绱為梺鍦櫕婵炩偓闁哄本绋戦…銊╁礃椤忓棔绱橀梻浣芥〃閻掞箓骞戦崶顒傚祦閻庯綆鍠栫粻娑欍亜閺嶇數鍒伴柟顔兼嚇濮婃椽宕ㄦ繝搴㈢暦濠电偛寮剁划鎾诲箖濞差亜唯闁冲搫鍊告禒?
     *
     * @param productId 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇?ID
     * @return 闂備礁鎼ˇ顐﹀疾濠婂牆钃熼柕濞垮剭濞差亜鍐€妞ゆ挾鍠庢禒娲⒑缂佹ɑ鈷掓い顓炵墦閹﹢宕堕浣哄幗闂侀潧顭堥崕閬嶎敂椤掍降浜滈柟鎯х－缁夎櫣鈧?ID闂傚倷鐒︾€笛呯矙閹达附鍤愭い鏍仜妗呴梺鐟邦嚟婵數鈧艾顭烽弻鏇熷緞閸繂濮庨梺璇茬箳閸犳牠寮婚悢鍏煎殝闁汇垻顣藉Σ鍫ユ⒑閹肩偛鈧倝宕㈡總鍓叉晪闁挎繂顦壕鍏兼叏濡搫鑸归柣?
     */
    @PostMapping("/purchase/{id}")
    public Result purchaseProduct(@PathVariable("id") Long productId) {
        // 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔荤暗濞存粌缍婇弻鐔煎箚瑜嶉弳杈ㄣ亜閵堝懏鍤囬柡宀嬬秬椤﹁埖銇勯弴鍡楁噽缁€濠勨偓骞垮劚椤︿即宕戦妸鈺傜厪濠电姴绻掗悾閬嶆煟?ID
        Long userId = UserContextHolder.getUser().getId();
        Long orderId = productService.purchaseProduct(productId, userId);
        return Result.ok(orderId);
    }
}
