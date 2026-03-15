package com.smartLive.blog.controller;

import com.smartLive.blog.domain.Blog;
import com.smartLive.blog.domain.VO.BlogVO;
import com.smartLive.blog.service.IBlogService;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.core.web.page.TableDataInfo;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 闂備礁鎲￠〃鍡涙嚌妤ｅ喚鏁囬柛锔诲幘娑撳秹鏌ㄥ☉妯侯仾闁稿﹦鍋ら弻鐔烘尒婢跺﹤鏆欓柣鎾村灴閺?
 * 闂備礁婀辩划顖炲礉閹烘梹顐介柛顭戝亞椤╂煡鏌涢埄鍐炬當閻熸瑱绠撻幃妤呯嵁閸喚浼勫┑鐐茬墛閸ㄥ灝鐣烽敐澶婄婵犻潧鐗滄导宀勬⒑闂堟稒顥欐俊鐐村浮楠炲牓濡搁埡浣哄摋濡ょ姷鍋涚花閬嶅磻閹惧瓨濯撮柛娑橈工閳ь剛鏁婚弻娑樷枎閹邦剦妫嗗銈嗘尰閹倿骞冮崼鏇炲耿婵°倐鍋撻柣蹇旂懇閹泛鈽夐弽褍顬嗘繛瀛樼矋鐢€崇暦閿濆棛绡€闁告洘鍨熼弲鐘茬暦濠靛惟鐟滄粍瀵奸崒鐐寸厽闁规惌鍘煎顔戒繆?闂備礁鎲＄敮鎺懳涘☉姘仏?濠电偞鍨堕幖鈺傜濞嗗警褎寰勯幇顒傤攨濠殿喗锕╅崢浠嬫倵椤旂晫绠剧紓鍫㈠Ь閸忓本绻涢崼鐔风伌鐎殿喖顭锋俊鐑解€﹂幋婵囩暠闂備浇顫夋禍浠嬪磿鏉堫偁浜规繛鎴欏灩杩?
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/blog")
public class BlogController extends BaseController
{
    @Autowired
    private IBlogService blogService;

    /**
     * 闂備礁鎼悮顐﹀磿閹绢噮鏁嬫俊銈呮噹绾偓濠殿喗锕╅崜锕傚触閸岀偞鐓曢柟鐑樻惄濞堟瑩鏌曢崱妤€鏆ｉ柡浣哥Ч瀹曠厧鈹戦崼鐔告闂備礁鎼ˇ顖炲疮閺夋埈鐎舵繛宸簻缁犲磭鎲稿澶婃槬婵°倕鎳忛弲?
     * 闂備礁鎼ˇ顖炲疮閺夋埈鐎? business:blog:list
     */
    @RequiresPermissions("business:blog:list")
    @GetMapping("/list")
    public TableDataInfo list(Blog blog)
    {
        startPage();
        List<Blog> list = blogService.selectBlogList(blog);
        return getDataTable(list);
    }

    /**
     * 闂佽娴烽弫鎼佸储瑜斿畷鐢割敇閵忕姷顢呭┑顔斤供閸擄箓宕ラ崒鐐寸厱闁圭儤鎼╁▓娆撴煏閸℃鏆ｉ柡浣哥Ч瀹曠厧鈹戦崼鐔告闂備礁鎼ˇ顖炲疮閺夋埈鐎舵繛宸簻缁犲磭鎲稿澶婃槬婵°倕鎳忛弲?
     * 闂備礁鎼ˇ顖炲疮閺夋埈鐎? business:blog:export
     */
    @RequiresPermissions("business:blog:export")
    @Log(title = "blog", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Blog blog)
    {
        List<Blog> list = blogService.selectBlogList(blog);
        ExcelUtil<Blog> util = new ExcelUtil<Blog>(Blog.class);
        util.exportExcel(response, list, "blog list");
    }

    /**
     * 闂備礁鍚嬮崕鎶藉床閼艰翰浜归柛銉墮绾偓濠殿喗锕╅崜锕傚触閸岀偞鐓曢柟鐑樻惄濞堟瑩鏌?
     */
    @GetMapping("/blogList")
    public AjaxResult blogList(Blog blog)
    {
        List<Blog> list = blogService.selectBlogList(blog);
        return success(list);
    }

    /**
     * 闂備礁鎲＄敮锟犲绩闁秴钃熷┑鐘叉搐绾偓濠殿喗锕╅崜锕傚触閸岀偞鍋ｉ柛銉у厴濡绢噣鏌涙惔锛勭缂佽鲸鎹囬獮妯虹暦閸ャ劍鍠栧┑鐐村灦閹稿摜鈧稈鏅濋弫顕€骞橀懜闈涗粧闁诲繒鍋熼搹搴°€掗悜鑺ョ厪?
     * 闂傚倷绶￠崑鍕磹婵犳艾鏋侀柕鍫濇椤╂煡骞栧ǎ顒€鐏繛鍫濈埣閺岀喓鎷犻垾铏亪闂侀潧娲ゅú顓炍涢崘顔碱潊闁宠埖瀵х换鍫濈暦閿濆憘鐔煎传閸曟垶顨婇弻鐔衡偓娑欘焽婢ц京绱掗幉瀣洭闁逞屽墯缁嬫帡鏁嬬紓浣介哺閹搁箖寮鈧畷銊︾節閸愮偓袧闂備焦鐪归崝宀€鈧凹鍘介弲璺侯煥閸繄顦梺闈浥堥弲娑樷枍閵忋垻纾介柛鎰劤閺嬫棃鏌?
     *
     * @return 闂備礁鎲＄敮锟犲绩闁秴钃熷┑鐘叉搐缁狅綁鏌熼柇锕€澧い顐ゅ█閺屸€愁吋閸涱喚鈹涢梺?
     */
    @GetMapping("/flushCache")
    public AjaxResult flushCache() {
        return success(blogService.flashCache());
    }

    /**
     * 闂備礁鍚嬮崕鎶藉床閼艰翰浜归柛銉墮绾偓濠殿喗锕╅崜锕傚触閸岀偞鍋ｉ柛銉ュ槻椤╊剛绱掗鑲╃缂佸矂浜堕崺鍕礃瑜忕粈鈧梻浣瑰缁嬫垿鎮ч崱娑欏仺妞ゆ劧绠戠痪褔鏌涢幇闈涙灍妞ゅ孩鐟╅弻鐔烘尒婢跺﹤鏆欓柣鎾村灴閺?
     * 闂備礁鎼ˇ顖炲疮閺夋埈鐎? business:blog:query
     */
    @RequiresPermissions("business:blog:query")
//    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(blogService.selectBlogById(id));
    }

    /**
     * 濠电儑绲藉ù鍌炲窗濡ゅ懎鏋佸┑鍌滎焾绾偓濠殿喗锕╅崜锕傚触?
     */
    @Log(title = "blog", businessType = BusinessType.UPDATE)
    @PutMapping
    public Result edit(@RequestBody Blog blog)
    {
        return Result.ok(blogService.updateBlog(blog));
    }

    /**
     * 闂備礁鎲＄敮鐐寸箾閳ь剚绻涢崨顓㈠弰鐎规洍鈧剚娼╂い鎺戭槸閹懘姊洪崨濠傜瑲妞ゃ劌顦垫俊鎾礃椤旇姤娅栭梺鍓插亝缁诲嫰寮妸鈺傜厸濠㈣泛锕ら弳锝嗕繆閻愵剚銇濇鐐村笩椤︽煡鏌涢埡鍌ゆ疁闁?
     * 闂備礁鎼ˇ顖炲疮閺夋埈鐎? business:blog:remove
     */
    @RequiresPermissions("business:blog:remove")
    @Log(title = "blog", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(blogService.deleteBlogByIds(ids));
    }

    /**
     * 闂備礁鎲＄敮鐐寸箾閳ь剚绻涢崨顓㈠弰鐎规洩绻濆鏉戭潩鐠虹儤鐤傞梻浣告啞椤ㄥ棝鎳濇ィ鍐炬晣?
     */
    @Log(title = "blog", businessType = BusinessType.DELETE)
    @DeleteMapping("remove/{id}")
    public Result removeById(@PathVariable("id") Long id)
    {
        return Result.ok(blogService.deleteBlogById(id));
    }

    /**
     * 闂備礁鎼崐鐟邦熆濮椻偓璺柛鎰靛枛绾偓濠殿喗锕╅崢浠嬫倵?
     * @param blog 闂備礁鎲￠〃鍡涙嚌妤ｅ喚鏁囬柛锔诲幐閸嬫捇宕烽鐐扮钵缂?
     * @return 闂備胶鎳撻悘婵堢矓瀹曞洨绀婇柡鍐ㄥ€荤壕濂告煙閹屽殶濞?
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog ) {
        return Result.ok(blogService.saveBlog(blog));
    }

    /**
     * 闂備礁鎼悮顐﹀磿閹绢噮鏁嬫俊銈呮噹缁狅綁鏌熺€电校闁伙綁浜堕弻娑樜旀担渚殹闂?
     * @param blog 闂備礁鎲￠〃鍡涙嚌妤ｅ喚鏁囩憸鐗堝笒閽冪喖鏌曟径妯煎帥闁搞倕瀚伴弻鈩冨緞閸″繐浜鹃柛娆忣樈閸?
     * @param current 闁荤喐绮庢晶妤呭箰閸涘﹥娅犻柣妯煎仺娴滄粓鏌ｉ姀銏℃毄闁?
     * @return 闂備胶鎳撻悺銊╁垂閻熸壋鏋旈柟杈剧畱绾偓濠殿喗锕╅崢浠嬫倵椤曗偓閺屾盯骞掗幙鍐╃暯闂?
     */
    @GetMapping("/of/me")
    public Result queryMyBlog(Blog blog,@RequestParam(value = "current", defaultValue = "1") Integer current) {
        List<BlogVO> blogList=blogService.queryMyBlog(blog,current);
        return Result.ok(blogList);
    }

    /**
     * 闂備礁鎼悮顐﹀磿閹绢噮鏁嬫俊銈呮噺閸婄粯銇勯幘鍗炵仾闁挎稑鐭傞弻娑樜旀担渚殹闂?
     * @param current 闁荤喐绮庢晶妤呭箰閸涘﹥娅犻柣妯煎仺娴滄粓鏌ｉ姀銏℃毄闁?
     * @return 闂備胶绮崺鍫ュ矗閸愩剮娑㈩敆閸曨偆顢呭┑顔斤供閸樹粙鎮楅鈧弻娑㈠箳閹垮啯鐣介梺?
     */
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {

        return Result.ok(blogService.queryHotBlog(current));
    }
    /**
     * 闂備礁鎼粔鐑斤綖婢跺﹦鏆ゅ〒姘ｅ亾鐎规洘宀稿畷鍗炩枎閹烘繃钑夐梻浣告惈鐞氼偊宕曢幘顕呮晪婵°倕鎳庣涵鈧┑顔斤供閸擄箓宕?
     * @param typeId 闂備礁鎲＄敮鎺懳涘☉姘仏闁绘稓鐪?
     * @param current 闁荤喐绮庢晶妤呭箰閸涘﹥娅犻柣妯煎仺娴滄粓鏌ｉ姀銏℃毄闁?
     * @return 闂備礁鎲＄敮鎺懳涘☉姘仏妞ゆ劧绲块埢鏃堟倵閿濆骸澧柣锝変憾閺屾稑螖娴ｈ棄顥濆銈嗘尭缂嶅﹤鐣峰顑╂棃宕熼埞鎯т壕?
     */
    @GetMapping("/category/{typeId}")
    public Result queryBlogByCategory(@PathVariable("typeId") Long typeId,@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return Result.ok(blogService.queryBlogByCategory(typeId,current));
    }
    /**
     * 闂備礁鎼悮顐﹀磿閹绢噮鏁嬫俊銈呮噺閸嬨劑鏌曟繝蹇曠暠闁绘挻娲熼弻娑樷枎閹邦剛浠寸紓浣规⒒閸忔﹢骞嗛崘顔肩妞ゆ劑鍨艰闂?
     * @param current 闁荤喐绮庢晶妤呭箰閸涘﹥娅犻柣妯煎仺娴滄粓鏌ｉ姀銏℃毄闁?
     * @param userId 闂備焦妞垮鍧楀礉瀹ュ鏄ユ慨鍦嫃
     * @return 闂備焦妞垮鍧楀礉瀹ュ鏄ユ繛鎴欏灩閻鏌熺€涙绠樻い蹇旀尦閺岋綁濡搁妷銉患閻庤娲忛崕鎶藉焵椤掆偓缁犲秹宕硅ぐ鎺戝瀭妞ゅ繐妫欓崑?
     */
    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("userId") Long userId) {
        return Result.ok(blogService.queryBlogByUserId(current, userId));
    }
    /**
     * 闂備礁鎼悮顐﹀磿閹绢噮鏁嬫俊銈呮噹绾偓濠殿喗锕╅崢浠嬫倵椤曗偓閹綊宕堕悜鑺ヮ€嶉梺?
     * @param id 闂備礁鎲￠〃鍡涙嚌妤ｅ喚鏁囩紒妤冩た
     * @return 闂備礁鎲￠〃鍡涙嚌妤ｅ喚鏁囬柛锔诲幗鐎氭岸鏌ㄩ弮鍥棄闁?
     */
    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {

        return Result.ok(blogService.queryBlogById(id));
    }
    /**
     * 闂佽崵濮崇粈浣规櫠娴犲鍋?闂備礁鎲￠悷锕傛偋濡ゅ啰鐭撻柣鎴ｆ绾偓濠殿喗锕╅崜锕傚触閸屾粎纾介柛鎰劤濞呭秹鏌?
     * 
     * @param blog 闂備礁鎲￠〃鍡涙嚌妤ｅ喚鏁囬柛锔诲幐閸嬫捇宕烽鐐扮钵缂傚倸绉撮澶愬极瀹ュ閱囬柕澶涚細缁垶姊洪崨濠勫ⅹ闁瑰啿閰ｉ獮?id 闂?isPin 闂備胶绮…鍫ュ春閺嶎厼鐒垫い鎴ｆ硶缁涘繒绱?
     * @return 闂備胶鎳撻悘婵堢矓瀹曞洨绀婇柡鍐ㄧ墕缁狅綁鏌熼柇锕€澧い顐ゅТ鑿愰柛銉到婢ф彃霉閻撳孩鍤囨鐐差儔瀵€燁槹濞存粌銈搁幃褰掑箛閳轰礁濮曢悷婊堫暒閼冲墎鍒?
     */
    @PutMapping("/isPin")
    public Result isPin(@RequestBody Blog blog){
        boolean pin = blogService.isPin(blog);
        if (pin){
            return Result.ok("pin updated");
        }else
            return Result.fail("pin update failed");
    }
    /**
     * 闂備胶顭堢换鍫ュ礉閹达箑闂繛宸簻閻鏌熺€涙绠樻い?闂備礁鎲￠懝楣冨嫉椤掑嫷鏁嗛柣鎰惈绾偓濠殿喗锕╅崜锕傚触閸岀偞鐓涘ù锝呮惈椤ｈ偐鈧鎸风欢姘跺箠濞戙垺鎯炴い鎰剁到娴犮垻绱撴担椋庘敀闁搞劌婀遍懞杈ㄦ綇閵娧€鏋栭柟鑲╄ˉ閸撴繈宕愭繝姘拺闁圭粯甯炵粻鎵磼?
     *
     * @return 闂佽崵鍠愰悷杈╃不閹达絻浜归柛灞剧矌绾惧ジ鏌熼幆褜鍤熷ù?
     */
    @PostMapping("/allPublish")
    public AjaxResult allPublish() {
        return success(blogService.allPublish());
    }

    /**
     * 闂備礁缍婂褏绱炴繝鍥ч棷婵炲樊浜滈惌妤呮煙鐎涙绠樻い?闂備礁鎲￠懝楣冨嫉椤掑嫷鏁嗛柣鎰惈缁犱即鏌涢妷鎴濇噺濮?ID 闂備焦鐪归崝宀€鈧凹鍓欓悾鐑藉Ψ瑜夐崑?
     *
     * @param ids 闂備礁鎲￠〃鍡涙嚌妤ｅ喚鏁?ID 闂備浇妗ㄩ悞锔界珶閸℃瑥鍨?
     * @return 闂佽崵鍠愰悷杈╃不閹达絻浜归柛灞剧矌绾惧ジ鏌熼幆褜鍤熷ù?
     */
    @PostMapping("/publish/{ids}")
    public AjaxResult allPublish(@PathVariable String[] ids) {
        return success(blogService.publish(ids));
    }

    @GetMapping("/search")
    public Result searchBlogs(@RequestParam(name = "keyword", required = false) String keyword) {
        return Result.ok(blogService.searchBlogs(keyword));
    }
}
