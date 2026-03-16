package com.smartLive.blog.service.impl;
import com.smartLive.common.core.constant.mq.InteractionMqConstants;
import com.smartLive.common.core.constant.mq.SearchMqConstants;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.blog.domain.Blog;
import com.smartLive.blog.domain.VO.BlogVO;
import com.smartLive.blog.mapper.BlogMapper;
import com.smartLive.blog.service.IBlogService;
import com.smartLive.common.core.domain.AppLoginUser;
import com.smartLive.common.rabbitmq.domain.*;
import org.springframework.beans.BeanUtils;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.enums.ContentStatusEnum;
import com.smartLive.common.core.enums.common.AuditStatusEnum;
import com.smartLive.common.core.enums.interaction.FeedTypeEnum;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.CacheClient;
import com.smartLive.common.redis.util.ZSetIdManager;
import com.smartLive.interaction.api.RemoteLikeService;
import com.smartLive.interaction.api.RemoteStarService;
import com.smartLive.interaction.api.DTO.LikeDTO;
import com.smartLive.interaction.api.DTO.StarDTO;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.DTO.ShopDTO;
import com.smartLive.user.api.RemoteAppUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import cn.hutool.core.util.StrUtil;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.smartLive.common.redis.util.RedisMultiCacheManager;

/**
 * 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╂喐閻楀牆绗掗柛鎰ㄥ亾缂傚倷绀侀鍫濃枖閺囩喍绻嗛柛銉戔偓濡插牓鏌熼崜褜妫庡瑙劽埞鎴︻敊閹勭€鹃悗鍨緲鐎氭澘鐣烽悢纰辨晣闁绘柨鐨濋崑鎾绘倷閻戞ê鈧敻鏌涢敂璇插箹濞寸姍鍕垫闁绘劕寮跺婵嬫煟?
 * 濠电姷鏁搁崑鐐哄垂瑜版帒鏋佺紒瀣紩閻戞绡€闁告劦浜跺ú鎼佹⒑缁洖澧叉い銊ユ噽閹叉挳鏁冮崒娑氬幐闂佺鏈喊宥夊箹閹邦喗鍠愰柡澶婄仢閳ь剙顭峰顐︻敋閳ь剙鐣峰鈧弫鍌炴偡妫颁礁顥氬┑鐐舵彧缁茶偐鎷冮敃鈧…鍥晸閻樺磭鍘告繝銏ｆ硾濡绂嶆ィ鍐┾拺缂佸娉曠粻浼存煟閵堝懏澶勭紒鍌涘浮瀹曞ジ濡烽妷搴涘劚閳藉骞橀弶鎴闂傚倷绀侀幉锟犳嚌妤ｅ啫瀚夋い鎺戝閺佸棝鏌ｉ幇顒佹儓闁活厽顨呴埞鎴︽偐閹绘帊绨藉┑鈽嗗灠椤戝洨妲愰幒妤佸亼婵炲棗绻戦崳钘夘熆鐠哄搫顏柟顔筋殔閳藉鈻庡Ο鐓庡Ш婵＄偑鍊栭幐濠氬箖閸岀偛绠栭柛顐ｆ礀楠炪垺绻涢崱妯虹仸闁诡垽绲剧换娑㈠箣閻愭潙闉嶉梺鐓庢贡閸嬫捇骞堥妸鈺傚亜闁稿繗鍋愰崝鐢告⒑閸濆嫬顏╃紒缁樺笧閺侇噣宕卞☉娆戝帾?
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Service
@Slf4j
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService
{
    private static final long BLOG_LIST_ZSET_SLOT = 0L;

    @Autowired
    private BlogMapper blogMapper;
    
    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;
    @Autowired
    private RedisService redisService;

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private RemoteAppUserService remoteAppUserService;
    @Autowired
    private RemoteShopService remoteShopService;
    @Autowired
    private RemoteLikeService remoteLikeService;
    @Autowired
    private RemoteStarService remoteStarService;
    @Autowired
    private RedisMultiCacheManager redisMultiCacheManager;
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private ZSetIdManager zSetIdManager;

    /**
     * 闂備浇顕х换鎰崲閹版澘绠伴柣顔鹃兏g闂備浇顕ф绋匡耿闁秴纾绘俊顖濆亹缁€濠囨倵閿濆骸浜濆┑顔界矋閵囧嫰骞掑鍥舵М闁瑰吋娼欓敃銈夊煡婢舵劕绠婚柛鎾茬劍椤姬ogVO
     * @param blog Blog闂備浇顕ф绋匡耿闁秴纾绘俊顖濆亹缁€?
     * @return BlogVO闂備浇顕уù鐑藉极閹间降鈧焦绻濋崶銊ョ樁?
     */
    private BlogVO convertToBlogVO(Blog blog) {
        if (blog == null) {
            return null;
        }
        BlogVO blogVO = new BlogVO();
        BeanUtils.copyProperties(blog, blogVO);
        return blogVO;
    }

    /**
     * 闂備浇顕х换鎰崲閹版澘绠伴柣顔鹃兏g闂傚倷绀侀幉锛勬暜濡ゅ懌鈧啯寰勯幇顑┿儵鏌涢幇銊︽珕濠殿喗绮嶉妵鍕箳瀹ュ浂妲柟鍏兼綑閿曘倝鍩ユ径鎰闁告挷鐒﹂～婕gVO闂傚倷绀侀幉锛勬暜濡ゅ懌鈧啯寰勯幇顑?
     * @param blogList Blog闂備浇顕ф绋匡耿闁秴纾绘俊顖濆亹缁€濠囨倵閿濆骸鏋涚紒鈧崱妯圭箚闁靛牆鎳庨銉╂煃?
     * @return BlogVO闂傚倷绀侀幉锛勬暜濡ゅ懌鈧啯寰勯幇顑?
     */
    private List<BlogVO> convertToBlogVOList(List<Blog> blogList) {
        if (CollUtil.isEmpty(blogList)) {
            return new ArrayList<>();
        }
        return blogList.stream()
                .map(this::convertToBlogVO)
                .collect(Collectors.toList());
    }

    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫣缁绢厸鍋撴繝娈垮枟閿曗晠宕滈敃鍌氳Е?
     * 
     * @param id 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥偡濞嗗繐顏柍缁樻煥閳规垿鎮╁畷鍥舵殹闂?
     * @return 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫?
     */
    @Override
    public Blog selectBlogById(Long id)
    {
        return blogMapper.selectBlogById(id);
    }

    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫣缁绢厸鍋撴繝娈垮枟閿曗晠宕滈敃鍌氳Е闁稿瞼鍋為悡鏇㈡煙閻戞ɑ鎯勬繛鍫熺懇閺?
     * 
     * @param blog 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫?
     * @return 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫?
     */
    @Override
    public List<Blog> selectBlogList(Blog blog)
    {
        return blogMapper.selectBlogList(blog);
    }

    @Override
    public BlogVO selectBlogVoById(Long id)
    {
        Blog blog = blogMapper.selectBlogById(id);
        BlogVO blogVO = convertToBlogVO(blog);
        if (blogVO == null) {
            return null;
        }
        fillBlogAdminNames(Collections.singletonList(blogVO));
        return blogVO;
    }

    @Override
    public List<BlogVO> selectBlogVoList(Blog blog)
    {
        List<Blog> blogList = blogMapper.selectBlogList(blog);
        List<BlogVO> blogVOList = convertToBlogVOList(blogList);
        fillBlogAdminNames(blogVOList);
        return blogVOList;
    }

    /**
     * 闂傚倷绀侀幖顐﹀磹閻熼偊鐔嗘慨妞诲亾鐠侯垶鏌涢幇闈涙灈缁绢厸鍋撴繝娈垮枟閿曗晠宕滈敃鍌氳Е?
     * 
     * @param blog 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫?
     * @return 缂傚倸鍊搁崐鐑芥倿閿曞倸绠板┑鐘崇閸?
     */
    @Override
    public int insertBlog(Blog blog)
    {
        blog.setCreateTime(DateUtils.getNowDate());
        int i = blogMapper.insertBlog(blog);
        if(i > 0){
            //濠电姷鏁搁崕鎴犵礊閳ь剚銇勯弴鍡楀閸欏繘鏌ｆ惔锛勫€伴梻鍌欐祰濡椼劑鎳楅懜鍨珷婵°倐鍋撻柣?
            publish(new String[]{blog.getId().toString()});
            //闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閳╁啫鎮卍is缂傚倸鍊搁崐鎼佸磹瑜版帒绠伴柟闂寸劍閸?
            redisService.deleteObject(RedisConstants.CACHE_HOT_BLOG_KEY+blog.getTypeId());
        }
        return i;
    }

    /**
     * 婵犵數鍎戠徊钘壝归崒鐐茬獥婵°倕鎳庨弸浣糕攽閸屾粠鐒剧痪顓涘亾婵犳鍠楅敃鈺呭礈閿曞倸瑙?
     * 
     * @param blog 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫?
     * @return 缂傚倸鍊搁崐鐑芥倿閿曞倸绠板┑鐘崇閸?
     */
    @Override
    public int updateBlog(Blog blog)
    {
        blog.setUpdateTime(DateUtils.getNowDate());
        int i = blogMapper.updateBlog(blog);
        if(i > 0){
            //闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閸屾粎姣€闂傚倷娴囧銊╂嚄閼稿灚娅犳俊銈傚亾闁?
            publish(new String[]{blog.getId().toString()});
            blog=getById(blog.getId());
            //闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹达絾纭舵い锔奸檮閵囧嫰骞樼€涙ê鈧劗鈧鍠曠划娆忕暦閵婏妇绡€闁告洘鍨崕鐢稿蓟?
            sendAuditMessage(blog);
            flashRedisBlogCache(blog.getId());
            flashRedisBlogListCache();
        }
        return i;
    }

    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊呯矆閸℃稒鐓熸俊顖濆亹鐢盯鏌ｅ┑鍫濇灈闁哄本绋戣灃濞达綀妫勯ˉ婵嗩渻?
     * 
     * @param ids 闂傚倸鍊搁崐绋棵洪悩璇茬；闁瑰墽绮崑锟犳煛閸ャ劍鐨戞い锔肩畵閺屾盯濡搁妷褏楔濠殿喖锕ｇ划娆愪繆閸洖鐐婇柕濞у嫭顔忛梻鍌欑閹诧繝銆冮崱娑欏殞濡わ絽鍠氶弫鍥偡濞嗗繐顏柍缁樻煥閳规垿鎮╁畷鍥舵殹闂?
     * @return 缂傚倸鍊搁崐鐑芥倿閿曞倸绠板┑鐘崇閸?
     */
    @Override
    public int deleteBlogByIds(Long[] ids)
    {
        int i = blogMapper.deleteBlogByIds(ids);
        //闂傚倷绀侀幉锛勬暜閻愬绠鹃柍褜鍓氱换娑㈠川椤撶姭鏀穝闂傚倷娴囧銊╂嚄閼稿灚娅犳俊銈傚亾闁?
        if (i > 0) {
        CountDownLatch latch=new CountDownLatch(ids.length);
        for (Long id : ids) {
            executorService.submit(()->{
               log.info("Deleting blog id {} from search indexes", id);
               sendBlogDeleteSyncMessage(id);
               //闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閳╁啫鎮卍is缂傚倸鍊搁崐鎼佸磹瑜版帒绠伴柟闂寸劍閸?
               flashRedisBlogCache(id);
               latch.countDown();
           });
        }
        try {
            //缂傚倸鍊烽悞锔剧矙閹次诲洭鏌嗗鍡椾槐闂佺鎻粻鎴犵不閵夆晜鐓冪憸婊堝礈閻斿鍤曢柛顭戝亜缁剁偛顭跨捄渚剰閹兼潙锕娲川婵犲啰鍘搁梺閫炲苯澧痪缁㈠弮瀵啿顫濋懜鐢靛幍?
            log.info("Waiting for blog delete tasks to finish");
            latch.await();
            log.info("Blog delete tasks finished, refreshing cache");
            flashRedisBlogListCache();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        }
        return 1;
    }

    /**
     * 闂傚倷绀侀幉锛勬暜閻愬绠鹃柍褜鍓氱换娑㈠川椤撱垹寮伴悗瑙勬磵閳ь剚鍓氬鈺傘亜閹烘埈妲搁柟顔兼噺缁绘盯鏁愰崨顔绢槺闂佸憡鎸荤换鍐偓?
     * 
     * @param id 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥偡濞嗗繐顏柍缁樻煥閳规垿鎮╁畷鍥舵殹闂?
     * @return 缂傚倸鍊搁崐鐑芥倿閿曞倸绠板┑鐘崇閸?
     */
    @Override
    public int deleteBlogById(Long id)
    {
        int i = blogMapper.deleteBlogById(id);
        if(i > 0){
            sendBlogDeleteSyncMessage(id);
            //闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閳╁啫鎮卍is缂傚倸鍊搁崐鎼佸磹瑜版帒绠伴柟闂寸劍閸?
            flashRedisBlogCache(id);
            flashRedisBlogListCache();
        }
        return i;
    }

    /**
     * 闂傚倷绀侀幖顐ょ矓閻戞枻缍栧璺猴功閺嗐倕銆掑锝呬壕閻庤娲嶉埀顒佸墯濞尖晜銇勯幒鎴Ц闁诡喖灏嘍闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫣缁绢厸鍋撴繝娈垮枟閿曗晠宕㈡禒瀣€垫い鏇楀亾闁诡喖缍婂畷鍫曟倻閼恒儺鈧秹姊?
     *
     * @param id 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥偡濞嗗繐顏柍缁樻煥閳规垿鎮╁畷鍥舵殹闂?
     * @return 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥煕閿旇骞楅悗姘哺閺屻劑寮崶顭戞闂佸憡鎸惧▍濠?
     */
    @Override
    public BlogVO queryBlogById(Long id) {
        Blog blog = cacheClient.queryWithLogicalExpireAndPassThrough(
                RedisConstants.CACHE_BLOG_KEY,
                RedisConstants.LOCK_BLOG_KEY,
                id,
                Blog.class,
                blogId -> query()
                        .eq("id", blogId)
                        .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                        .eq("audit_status", AuditStatusEnum.PASS.getCode())
                        .one(),
                RedisConstants.CACHE_BLOG_TTL,
                TimeUnit.MINUTES
        );
        if (blog == null) {
            throw new BusinessException("blog not found");
        }
        BlogVO blogVO = convertToBlogVO(blog);
        queryBlogUser(blogVO);
        isBlogLiked(blogVO);
        isBlogStared(blogVO);
        return blogVO;
    }
    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫣閻庢艾顦甸弻宥堫檨闁告挾鍠栭獮鍐煥閸偅鏅ｉ梺缁樺姌鐏忣亣顤傞梻浣筋嚙妤犲摜绮诲澶婂瀭鐟滅増甯掗崹鍌涖亜韫囨挻顥為柛?
     *
     * @param current 闂佽崵鍠愮划搴㈡櫠濡ゅ懎绠伴柛娑橈攻濞呯娀鏌ｅΟ鐓庝缓濞存粍绮撻弻锝夊閵忊剝姣勯梺?
     * @return 闂傚倷鑳剁划顖炲春閸儱鐭楅柛鎰╁壆濞戙埄鏁嗛柛鏇ㄥ亞椤㈠懎鈹戦鏂や緵闁告搫绠撳畷銉╁磼閻愬鍘遍梺鍦劋閹尖晛鈻撳▎鎾寸厪?
     */
    @Override

    public List<BlogVO> queryHotBlog(Integer current) {
        // 婵犵數鍋涢顓熸叏鐎电硶鍋撳☉鎺撴珔闁靛棙甯″畷濂稿即閻愭惌妫熼梻浣筋潐椤旀牠宕板Δ鍛仼?ZSet 闂傚倷鑳剁划顖炲春閸儱鍌ㄩ柤娴嬫杹閸嬫挾鎲撮崟顐熸灆濡ょ姷鍋炵敮锟犵嵁閹烘鍗抽柕濠忛檮閺夊憡绻濆▓鍨灍閻㈩垱顨堥崚鎺楀醇閵夛箑娈橀梺纭呮彧闂勫嫰宕甸弮鍌楀亾閻熸澘顏繝銏☆焽瀵板﹪宕稿Δ浣哄幈濠德板€撳ù鍥ㄧ珶濡眹浜?ID 闂傚倷绀侀幖顐︻敄閸曨厾鐭嗗〒姘ｅ亾鐎规洩绲鹃妶锝夊礃閵娧屾Т闂備胶纭堕崜婵堢矙韫囨稑鐒垫い鎺嗗亾妞わ箓娼ч锝夘敃閿旇棄浜遍梺鍓插亝缁诲嫰濡?
        Page<Long> longPage = zSetIdManager.pageIds(RedisConstants.BLOG_HOT_RANK_KEY, null, current, SystemConstants.MAX_PAGE_SIZE);
        List<Long> blogIdList = longPage.getRecords();

        if (CollUtil.isNotEmpty(blogIdList)) {
            return getBlogListByIds(blogIdList);
        }else if(current==1){
            // ZSet 闂傚倷绀侀幉锟犲垂閻撳海鏆﹂柣銏㈩焾閻撴ɑ绻涢幋娆忕仼缂佺媴缍侀弻鈥崇暤椤斿吋鍣烘い鏇燂耿濮婃椽宕崟顐ｆ婵犳鍠氶弫璇差嚕閹惰姤鍋勯柣鎾虫捣椤斿顪冮妶鍡橆梿濠殿喓鍊濊棟鐟滄棃寮婚敐澶娢╅柕澶堝労娴犲ジ姊洪崨濠庣劸妞ゎ偄顦甸獮鍡涘籍閸繂宓嗗銈呯箰濡盯鎮伴妷鈺傗拺闁告繂瀚～锕傛煕鎼淬劋鎲剧€规洜鏁婚、姗€濮€閻樻妲梻渚€娼ц墝闁哄懏绋戦埢宥夊Χ婢跺鍘告繛杈剧到閹碱偊銆傞懖鈹惧亾鐟欏嫭灏紒鑸佃壘閻ｅ嘲螣鐞涒剝鐎婚棅顐㈡处閹哥效?ZSet闂傚倷鐒︾€笛呯矙閹达附鍤愭い鏍仜閻ゎ噣鏌嶈閸撶喖寮婚悢鍏碱棃婵炴垵宕崜鐗堢節濞堝灝鏋撻柡鍛Т閻ｅ嘲顫濈捄鍝勮€垮┑鐐村灦閻楁垿宕戦幘鏉戠窞閻庯綆鍋傚锕傛⒑閸濆嫬鈧湱鈧瑳鍛焼闁?
            List<Blog> dbList = query()
                    .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .orderByDesc("liked")
                    .orderByDesc("create_time")
                    .list();

            List<Blog> blogList = new ArrayList<>();
            if (CollUtil.isNotEmpty(dbList)) {
                final List<Blog> finalDbList = dbList;
                executorService.execute(() -> {
                    log.info("闂傚倸鍊烽悞锕併亹閸愵亞鐭撻悗闈涙憸绾句粙鏌熼幑鎰靛殭缁绢厸鍋撴繝娈垮枟閿曗晠宕滈敃鍌氳Е闁稿瞼鍋為悡鐔兼煙閹碱厼骞栨鐐搭殕閵?ZSet");
                    zSetIdManager.saveToZSet(RedisConstants.BLOG_HOT_RANK_KEY, finalDbList, Blog::getId, Blog::getCreateTime);

                    // 闂傚倷娴囬～澶嬬娴犲绀夐煫鍥ㄦ礃瀹曞弶淇婇姘倯濠殿垰銈搁弻銊モ槈濡警浼€闂佸搫妫崜鐔奉潖婵犳艾绀冩い蹇撳閻庡姊洪崜鑼帥濞存粎鍋熼崚鎺楀垂椤愩垻绐炴繝鐢靛仦閸庤櫕绂嶉悙顒傜闁糕剝顨堢粻鎶芥煛閸℃洖宓嗛柡宀嬬節瀹曟﹢濡歌閻撶喖姊洪崫鍕棦濞存粏娉涢悾宄扳枎閹炬潙娈熼梺闈涱檧闂勫嫰顢欏鍡欑瘈闁靛骏绲剧涵楣冩倵濮樼厧娅嶆鐐插暞缁绘繈宕堕妸銉ュ闂備胶绮弻銊︾珶婵犲洤绀夐柍褜鍓熷鍝勑ч崶褍顬堥柣搴㈠嚬閸ㄥ爼宕归幆褜鐓ラ柛顐ｇ箘椤︹晠妫呴銏″闁规瓕宕甸埀顒佺閻擄繝寮昏閹风娀鎳犻顐犲灲閺岀喖顢欐總绋垮及闂佽鍠楃划鎾汇€佸鈧幃娆忣啅椤斿吋鏆梻?
                    if (RedisConstants.BLOG_CALC_QUEUE_KEY != null) {
                        redisService.setCacheSet(RedisConstants.BLOG_CALC_QUEUE_KEY, finalDbList.stream().map(b -> String.valueOf(b.getId())).collect(Collectors.toSet()));
                    }
                });

                // 闂傚倷绀佺紞濠傤焽瑜旈、鏍川椤旇棄寮块梺鍐叉惈閹冲海绮堥崱娑欑厱闁斥晛鍟伴埣銈夋煃瑜滈崜娆撍囬悽鍝ュ祦閻庯綆鍣弫鍌炴煕閺囥劌浜為柟顔笺偢閹嘲顭ㄩ崨顓ф毉闂佸湱顭堥幉锟犲疾閸洘鍋愰柣鎰灊缁ㄥ姊洪崜鑼帥闁稿鎳庨埢鎾诲醇閺囩喓鍘?
                int start = (current - 1) * SystemConstants.MAX_PAGE_SIZE;
                int pageSize = SystemConstants.MAX_PAGE_SIZE;
                if (dbList.size() > start) {
                    blogList = dbList.subList(start, Math.min(start + pageSize, dbList.size()));
                }
            }

            List<BlogVO> voList = convertToBlogVOList(blogList);
            queryBlogListUserMessage(voList);
            queryBlogListIsLike(voList);
            return voList;
        }
        return Collections.emptyList();
    }


    /**
     * 婵犵數鍎戠徊钘壝洪敂鐐床闁稿瞼鍋為崑銈夋煏婵炵偓娅呯痪顓涘亾婵犳鍠楅敃鈺呭礈閿曞倸瑙﹂柛宀€鍋為悡銉︾箾閹寸儐鐒藉褎娲滈幉鎼佸级閸喒鍋撴繝姘瀬?闂傚倷娴囨竟鍫澪涢崟顖€鍥焼瀹ヤ讲鍋撻敃鍌氶唶闁靛闄勫▍?
     *
     * @param blog 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥煕閿旇骞愰柛瀣崌瀹曠兘顢橀悙鎵挼缂?
     * @return 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╃磼濡ゅ啯銇?
     */
    @Override
    public Long saveBlog(Blog blog) {
        blog.setUserId(UserContextHolder.getUser().getId());
        blog.setCreateTime(DateUtils.getNowDate());
       if (blog.getShopId() != null && !blog.getShopId().toString().isEmpty()) {
           String firstShopIdStr = blog.getShopId().toString().split(",")[0];
           Long firstShopId = Long.valueOf(firstShopIdStr);
           ShopDTO shopDTO = remoteShopService.getShopById(firstShopId);
           if(shopDTO!= null){
               blog.setTypeId(shopDTO.getTypeId());
           }
        }
        // 婵犵數鍎戠徊钘壝洪敂鐐床闁稿瞼鍋為崑銈夋煏婵炵偓娅呯紒鐘冲▕閺屾洝绠涢弴鐐愩儵鏌￠崱姗嗘畼缂佽鲸甯掕灃濞达綀顕栧鍨渻?
        boolean success = save(blog);
        if (!success) {
            throw new BusinessException("failed to save blog");
        }
        //婵犵數鍎戠徊钘壝洪敂鐐床闁稿瞼鍋為崑銈夋煏婵炵偓娅嗛柛銈嗗笒椤法鎹勯搹鍦姼闂?
        if (blog.getStatus() != null && blog.getStatus().intValue() == ContentStatusEnum.DRAFT.getCode()) {
            return blog.getId();
        }
        //闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹达絽鍔甸柛鐘愁浉bbitMq濠电姷鏁搁崑鐐哄垂閻㈠憡鍋嬪┑鐘插暙椤?闂傚倷娴囬～澶嬬娴犲绀夐柟杈剧畱閻掑灚銇勯幋锝呭姷闁稿繐鐭傞弻锝夋偆娴ｉ鏁栭梺鐟板槻椤戝骞嗛崒婊呯煔鐎光偓閳ь剛妲愰幘瀛樺闁革富鍘介崳顕€姊哄ú璇插箹婵炶濡囩划?
        FeedEventMessage feedEventMessage = FeedEventMessage.builder()
                //婵犵數鍋涢悺銊у垝瀹€鍕垫晞闁告洦鍋€閺嬪酣鏌曡箛瀣伄閻忓繒鏁婚幃褰掑炊椤忓嫮姣㈤梺?
                .feedType(FeedTypeEnum.USER_FEED.getCode())
                //闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹寸偛顕滅紒浣峰嵆閺屽秷顧侀柛鎾寸懇楠炲﹪骞囬钘変粡濡炪倖鍔х粻鎴﹀垂?
                .sourceType(GlobalBizTypeEnum.USER.getCode())
                .sourceId(blog.getUserId())
                //闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹达絾纭堕柛鏂跨Ч閺岋繝宕卞▎蹇旂亪濡ょ姷鍋為崹鍧椼€佸▎鎾崇闁规澘鐏氶拺澶愭⒒?
                .bizType(GlobalBizTypeEnum.BLOG.getCode())
                .bizId(blog.getId())
                .publishTime(blog.getCreateTime())
                .build();
        mqMessageSendUtils.sendMqMessage(InteractionMqConstants.INTERACT_FEED_EXCHANGE, InteractionMqConstants.INTERACT_FEED_ROUTING_KEY, feedEventMessage);
        //濠电姷鏁搁崕鎴犵礊閳ь剚銇勯弴鍡楀閸欏繘鏌ｆ惔锛勫€伴梻鍌欐祰濡椼劑鎳楅懜鍨珷婵°倐鍋撻柣?
        publish(new String[]{blog.getId().toString()});
        //濠电姷鏁搁崕鎴犵礊閳ь剚銇勯弴鍡楀閸欏繘鏌ｉ幇顔煎妺闁稿鍔戦弻鏇熺節韫囨洜鏆犻梺缁樻尰濡茬闂傚倷娴囧銊╂嚄閼稿灚娅犳俊銈傚亾闁?
        String actionType=UserResourceActionTypeConstants.USER_RESOURCE_ACTION_PUBLISH;
        //闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹达絾纭舵い锔奸檮閵囧嫰骞樼€涙ê鈧劗鈧鍠曠划娆忕暦閵婏妇绡€闁告洘鍨崕鐢稿蓟?
        sendAuditMessage(blog);
        String  id = blog.getUserId()+"_"+actionType+"_"+GlobalBizTypeEnum.BLOG.getBizDomain()+"_"+blog.getId().toString();
        UserResourceMessage userResourceMessage = UserResourceMessage.builder()
                .indexName(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME)
                .id(id)
                .userId(blog.getUserId())
                .sourceId(blog.getId())
                .sourceType(GlobalBizTypeEnum.BLOG.getCode())
                .actionType(actionType)
                .data(blog)
                .build();
        mqMessageSendUtils.sendMqMessage( SearchMqConstants.ES_SYNC_EXCHANGE,SearchMqConstants.ES_SYNC_USER_RESOURCE_INSERT_ROUTING_KEY, userResourceMessage);
        //闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閳╁啫鎮卍is缂傚倸鍊搁崐鎼佸磹瑜版帒绠伴柟闂寸劍閸?
        redisService.deleteObject(RedisConstants.CACHE_HOT_BLOG_KEY+blog.getTypeId());
        //闂備礁鎼ˇ顐﹀疾濠婂牆钃熼柕濞垮剭濞差亜绾у┑?
        return blog.getId();
    }

    /**
     * 闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹达絾纭舵い锔奸檮閵囧嫰骞樼€涙ê鈧劗鈧鍠曠划娆忕暦閵婏妇绡€闁告洘鍨崕鐢稿蓟?
     * @param blog
     */
    private void sendAuditMessage(Blog blog) {
        AuditMessage auditMessage = AuditMessage.builder()
                .bizId(blog.getId())
                .bizType(GlobalBizTypeEnum.BLOG.getCode())
                .submitterId(blog.getUserId())
                .auditContent(BeanUtil.beanToMap(blog))
                .createTime(blog.getCreateTime())
                .build();
        mqMessageSendUtils.sendMqMessage( AiAuditMqConstants.AUDIT_DIRECT_EXCHANGE,AiAuditMqConstants.AUDIT_ROUTING_KEY, auditMessage);
    }

    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫣缂佺媴缍侀弻鐔衡偓鐢殿焾鏍￠梺浼欑秮娴滃爼寮诲☉妯滄梹鎷呴挊澶樻婵＄偑鍊栭幐鍝ョ礊婵犲倻鏆︽慨姗嗗劦閺冨牆绀嬮柍瑙勫劤娴?
     *
     * @param b       闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╂喐閻楀牆绗掗柦鍐枛閺屾洘寰勫Ο鐓庡弗闂佹悶鍊曠€氫即寮婚埄鍐ㄧ窞闁糕€崇箰娴滈箖鏌涘▎蹇ｆ▓闁稿鍔戝娲箰鎼达絺妲堥梺缁橆殔濡盯鍩€椤掆偓閻忔岸鏁冮姀鐘垫殾闁靛鏅╅弫鍐煏閸繃鍟楅柨鏇炲€归悡娆愩亜閹达絽鍔甸柛蹇撶焸閺岋綁鍩勯崘銊т患缂備緡鍣ｅ褔鍩ユ径鎰厬闁宠桨妞掓竟鏇熺節閵忥絾纭鹃柨鏇樺€曡灋婵せ鍋撻柡灞稿墲閹峰懐绮欑捄銊ф晨缂?
     * @param current 闂佽崵鍠愮划搴㈡櫠濡ゅ懎绠伴柛娑橈攻濞呯娀鏌ｅΟ鐓庝缓濞存粍绮撻弻锝夊閵忊剝姣勯梺?
     * @return 闂傚倷鑳堕幊鎾绘偤閵娾晛鍨傞柣鐔稿閺嬫棃鏌熸潏鍓х暠缁绢厸鍋撴繝娈垮枟閿曗晠宕滈敃鍌氳Е闁稿瞼鍋為悡鏇㈡煙閻戞ɑ鎯勬繛鍫熺懇閺?
     */
    @Override
    public List<BlogVO> queryMyBlog(Blog b,Integer current) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            throw new BusinessException("user not logged in");
        }
        // 闂傚倷绀侀幖顐ょ矓閻戞枻缍栧璺猴功閺嗐倕銆掑锝呬壕闂佽鍠曠划娆愪繆閹间焦鏅濋柍褜鍓熼幃锟犲即閵忥紕鍘搁柣蹇曞仩椤曆囧礉閵夛负浜?
        Page<Blog> page = query()
                .eq("user_id", user.getId())
                .orderByDesc("pin")
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔荤暗濞存粌缍婇弻鐔煎箚瑜嶉弳杈ㄣ亜閵堝懏鍣规い顏勫暣婵″爼宕ㄩ娆戦┏闂備礁鎼Λ娑㈠窗閺嶎厾宓?
        List<Blog> blogList = page.getRecords();
        List<BlogVO> voList = convertToBlogVOList(blogList);
        queryBlogListUserMessage(voList);
        queryBlogListIsLike(voList);
        return voList;
    }
    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫣缂佺姳鍗抽弻娑㈠Ψ閹存繃鍣烘慨锝呴叄濮婂搫煤缂佹ê鈻忛梺鍛婃煥缁夌懓鐣烽崫鍕ㄦ闁靛繒濮烽悡鎴︽⒑閻熸壆鎽犵紒璇插椤㈡棃骞橀鐣屽幗闂侀潧顭堥崕閬嶎敂閳哄啠鍋撶憴鍕８闁稿孩濞婇崺鈧い鎺嗗亾缂佺姴绉瑰畷纭呫亹閹烘垵鐎銈呯箰濡瑩宕?
     *
     * @param current 闂佽崵鍠愮划搴㈡櫠濡ゅ懎绠伴柛娑橈攻濞呯娀鏌ｅΟ鐓庝缓濞存粍绮撻弻锝夊閵忊剝姣勯梺?
     * @param userId  闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰銉︽叏閸︻厽瀚?
     * @return 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╂喐閻楀牆绗掔紒鈧崱妯圭箚闁靛牆鎳庨銉╂煃?
     */
    @Override
    public List<BlogVO> queryBlogByUserId(Integer current, Long userId) {
        Page<Blog> page = query()
                .eq("user_id", userId)
                .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .orderByDesc("pin")
                .orderByAsc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        List<BlogVO> voList = convertToBlogVOList(records);
        queryBlogListIsLike(voList);
        return voList;
    }
    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫣缁绢厸鍋撴繝娈垮枟閿曗晠宕滈敃鍌氳Е闁稿瞼鍋為崑锝夋煕閵壯冨幋婵＄虎鍣ｉ弻娑欐償閿涘嫮顔掗梺杞扮缁夌懓鐣烽悡搴樻斀闁糕剝鐟﹂埢澶愭⒒娴ｅ憡鍟為柛鎴濈秺瀹曟垿宕卞☉妯荤€梺闈涚墕椤︻垳绮婚敐澶嬬叄婵﹩鍓欓埀顒勵棑閹广垽宕卞☉娆戝幍濡炪倖鎸嗛崘顏冪礄缂?
     *
     * @param id 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥偡濞嗗繐顏柍缁樻煥閳规垿鎮╁畷鍥舵殹闂?
     * @return 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥煕閿旇骞楅悗姘哺閺屻劑寮崶顭戞闂佸憡鎸惧▍濠?
     */
    @Override
    public BlogVO getBlogById(Long id) {
        Blog blog = query().eq("id", id).one();
        BlogVO blogVO = convertToBlogVO(blog);
        queryBlogUser(blogVO);
        return blogVO;
    }

    /**
     * 闂傚倷绀侀幖顐ょ矓閻戞枻缍栧璺猴功閺嗐倕鈽夐幙鍐╂儞闂傚倷绀侀幉锛勬暜濡ゅ懌鈧啯寰勯幇顑┿儵鏌涢幇闈涙灈缂佺姰鍎抽幉鎼佹偋閸繄鐟ㄥ┑鐐叉噷閸婃繈寮婚妶澶婄畾鐟滃秴危閼姐倖鍠愰柡澶嬪閸犳鈧娲嶉埀顒佸墯濞尖晜銇勯幒鎴Ц闁诡喖鎳樺娲箰鎼达絺妲堥梺缁橆殔濡繈鐛崱娑橀唶闁靛绠戞禒娲⒑闂堟侗鐓紒鐘冲灴閹繝寮撮悙鍐ㄩ叄瀹曞爼鏁愰崨顓涙嫟濠电偛顕崢褔骞婂Ο璁崇箚閻庢稒蓱婵挳鏌涘┑鍡楊仹闁告牗鐗犻弻锝夋偐濞嗗繑鍣瑰ù鐘欏啠鏀芥い鏃囧Г閸婃劖顨ラ悙鈺佷壕濠电偞鎸婚懝楣冩晝閿斿墽鐭?
     *
     * @param sourceIdList 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╃磼濡ゅ啯銇熼梻鍌欑閹诧紕鏁Δ鍛偓鍐╁緞閹邦儵?
     * @return 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╂喐閻楀牆绗掔紒鈧崱妯圭箚闁靛牆鎳庨銉╂煃?
     */
    @Override
    public List<BlogVO> getBlogListByIds(List<Long> sourceIdList) {
        if (CollUtil.isEmpty(sourceIdList)) {
            return Collections.emptyList();
        }
        List<Blog> blogList = redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_BLOG_KEY,
                RedisConstants.LOCK_BLOG_KEY,
                sourceIdList,
                Blog.class,
                missingIds -> query()
                        .in("id", missingIds)
                        .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                        .eq("audit_status", AuditStatusEnum.PASS.getCode())
                        .list(),
                Blog::getId,
                RedisConstants.CACHE_BLOG_TTL,
                TimeUnit.MINUTES
        );
        if (CollUtil.isEmpty(blogList)) {
            return Collections.emptyList();
        }
        Map<Long, Blog> blogMap = blogList.stream()
                .filter(blog -> blog != null && blog.getId() != null)
                .collect(Collectors.toMap(Blog::getId, Function.identity(), (v1, v2) -> v1));
        List<Blog> orderedBlogList = new ArrayList<>(sourceIdList.size());
        for (Long blogId : sourceIdList) {
            Blog blog = blogMap.get(blogId);
            if (blog != null
                    && Objects.equals(blog.getStatus(), ContentStatusEnum.PUBLISHED.getCode().shortValue())
                    && Objects.equals(blog.getAuditStatus(), AuditStatusEnum.PASS.getCode())) {
                orderedBlogList.add(blog);
            }
        }
        if (CollUtil.isEmpty(orderedBlogList)) {
            return Collections.emptyList();
        }
        List<BlogVO> voList = convertToBlogVOList(orderedBlogList);
        queryBlogListUserMessage(voList);
        queryBlogListIsLike(voList);
        return voList;
    }

    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊堟嫅閻斿吋鐓忓鑸殿焽閸樻盯鏌涢妶鍛伃闁哄本绋戣灃濞达綀妫勯ˉ婵嗩渻閵堝棙灏紓宥咃工椤曪絾瀵奸幖顓熸櫖濠殿喗锕╅崢濂稿礈閵娾晜鐓熼柣鎰綑閸ゎ剟鏌涚€ｎ亝顥㈢€规洘鍨块弫鎰板炊閵娿儳姣?
     * @param blogList
     */
    private void queryBlogListIsLike(List<BlogVO> blogVOList) {
        if (CollUtil.isEmpty(blogVOList)) {
            return;
        }
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            blogVOList.forEach(vo -> {
                if (vo != null) {
                    vo.setIsLike(false);
                }
            });
            return;
        }
        // Extract IDs
        List<Long> blogIds = blogVOList.stream()
                .map(BlogVO::getId)
                .collect(Collectors.toList());

        // Batch check Likes
        LikeDTO likeDTO = new LikeDTO();
        likeDTO.setUserId(user.getId());
        likeDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
        Map<Long, Boolean> likeMap = remoteLikeService.getIsLikeBatch(likeDTO, blogIds);
        log.info("likeMap: {}", likeMap);
        blogVOList.forEach(vo -> {
            if (vo != null) {
                vo.setIsLike(likeMap.getOrDefault(vo.getId(), false));
            }
        });
    }

    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊堟嫅閻斿吋鐓忓鑸殿焽閸樻盯鏌涢妶鍛伃闁哄本绋戣灃濞达綀妫勯ˉ婵嗩渻閵堝棙灏紓宥咃工椤曪絾瀵奸幖顓熸櫖濠殿喗锕╅崢濂稿礈閵娾晜鐓熼柣鎰綑閸ゎ剟鏌涚€ｎ亝鍤囩€殿噮鍋夐妵鎰板箳閹惧磭妾?
     * @param blogList
     */
    private void queryBlogListIsStar(List<BlogVO> blogVOList) {
        if (CollUtil.isEmpty(blogVOList)) {
            return;
        }
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            blogVOList.forEach(vo -> {
                if (vo != null) {
                    vo.setIsStared(false);
                }
            });
            return;
        }
        // Extract IDs
        List<Long> blogIds = blogVOList.stream()
                .map(BlogVO::getId)
                .collect(Collectors.toList());

        // Batch check Stars
        StarDTO starDTO = new StarDTO();
        starDTO.setUserId(user.getId());
        starDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
        Map<Long, Boolean> starMap = remoteStarService.getIsStarBatch(starDTO, blogIds);

        blogVOList.forEach(vo -> {
            if (vo != null) {
                vo.setIsStared(starMap.getOrDefault(vo.getId(), false));
            }
        });
    }

    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊堟嫅閻斿吋鐓忓鑸殿焽閸樻盯鏌涢妶鍛伃闁哄本绋戣灃濞达綀妫勯ˉ婵嗩渻閵堝棙灏紓宥咃工椤曪綁宕归銏㈢獮婵犵數濮撮崐鍝ョ礊濮椻偓濮婂搫效閸パ冾瀳闁诲孩鍑归崳锝咁嚕椤愶箑围濠㈣泛锕﹂娲⒑閻愵剝澹橀柛濠囶棑閹广垽宕卞☉娆戝幍?
     * @param blogList
     */
    private void queryBlogListUserMessage(List<BlogVO> blogVOList) {
        fillBlogUserNames(blogVOList);
    }

    private void fillBlogAdminNames(List<BlogVO> blogVOList) {
        fillBlogUserNames(blogVOList);
        fillBlogShopNames(blogVOList);
    }

    private void fillBlogUserNames(List<BlogVO> blogVOList) {
        if (CollUtil.isEmpty(blogVOList)) {
            return;
        }
        List<BlogVO> validBlogVOList = blogVOList.stream()
                .filter(Objects::nonNull)
                .toList();
        if (CollUtil.isEmpty(validBlogVOList)) {
            return;
        }
        List<Long> userIds = validBlogVOList.stream()
                .map(BlogVO::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        List<com.smartLive.user.api.domain.UserDTO> userList = remoteAppUserService.getUserList(userIds);
        if (CollUtil.isEmpty(userList)) {
            return;
        }
        Map<Long, com.smartLive.user.api.domain.UserDTO> userMap = userList.stream().collect(Collectors.toMap(
                com.smartLive.user.api.domain.UserDTO::getId,
                Function.identity(),
                (v1, v2) -> v1
        ));
        validBlogVOList.forEach(vo -> {
            com.smartLive.user.api.domain.UserDTO user = userMap.get(vo.getUserId());
            if (user != null) {
                String nickName = user.getNickName() == null ? "" : user.getNickName();
                vo.setName(nickName);
                vo.setUserName(nickName);
                vo.setIcon(user.getIcon());
            }
        });
    }

    private void fillBlogShopNames(List<BlogVO> blogVOList) {
        if (CollUtil.isEmpty(blogVOList)) {
            return;
        }
        List<BlogVO> validBlogVOList = blogVOList.stream()
                .filter(Objects::nonNull)
                .toList();
        if (CollUtil.isEmpty(validBlogVOList)) {
            return;
        }
        List<Long> shopIds = validBlogVOList.stream()
                .map(BlogVO::getShopId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(shopIds)) {
            return;
        }
        List<ShopDTO> shopList = remoteShopService.getShopList(shopIds);
        if (CollUtil.isEmpty(shopList)) {
            return;
        }
        Map<Long, String> shopNameMap = shopList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ShopDTO::getId, shop -> shop.getName() == null ? "" : shop.getName(), (left, right) -> left));
        validBlogVOList.forEach(vo -> {
            if (vo.getShopId() != null) {
                vo.setShopName(shopNameMap.getOrDefault(vo.getShopId(), ""));
            }
        });
    }

    /**
     * 缂傚倸鍊搁崐鎼佸窗濮樿泛闂柨婵嗩槸杩濇繝鐢靛Т濞层倗娑甸埀顒€鈹戦鏂や緵闁告搫绠撳畷?
     *
     * @param blog
     * @return 缂傚倸鍊搁崐鐑芥倿閿曞倸绠板┑鐘崇閸?
     */
    @Override
    public boolean isPin(Blog blog) {
        boolean update = this.lambdaUpdate()
                .eq(Blog::getId, blog.getId())
                .set(Blog::getPin, blog.getPin())
                .update();
        if (update) {
            //闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樻彃顏痪鍙ョ矙閺岀喖骞嗚閿涘秹鏌?
            flashRedisBlogCache(blog.getId());
        }
        return update;
    }

    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊堝础閹惰姤鍊垫繛鎴烆伆閹达附鍋傞柍褜鍓熷娲川婵犲孩鐣烽梺鐓庣秺缁犳牠銆佸Ο瑁や汗闁圭儤鎸搁埀顒勵棑閻ヮ亪顢橀悙鏉戞濡炪倐鏅濋弫濠氬蓟?
     *
     * @param updateMap 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╃磼濡ゅ啯銇熸繝鐢靛仦閸ㄥ爼骞愮粙妫靛綊鎮滈挊澶岋紱闂佹寧绻傚ú锔句焊鎼淬劍鐓熼柟閭﹀墰閹界姵绻涢崼婵堝煟闁哄矉缍佹俊鎼佸Ψ閵夘喕鐥繝纰樺墲閸庡綊宕ㄩ鍌氬⒕?
     * @return 闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樻彃顏痪鎯с偢閺岀喖骞嗚閸ょ喎霉?
     */
    @Override
    public Boolean updateLikeCountBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }

        // 闂佽娴烽崑锝夊磹濠靛牏纾芥慨姗€顤傞弫濠囨煕濞戞鎽犻柡鍜佸墯閹便劌顫滈崱妤€鈷掑┑鐐茬湴閸婃繈寮婚敍鍕ㄥ亾閿濆骸浜為柣顓烆儔閺岋繝宕卞▎蹇旂亪闂佸搫鑻惌浣虹不濞戙垹鍗抽柣鎴濇椤ユ粓姊绘担鍛婂暈閻㈩垪鏅犲畷鎴﹀礋椤栨?闂備胶鍎甸崜婵堟暜閹烘绠犻柟鎹愬煐瀹?00)闂傚倷鐒︾€笛呯矙閹达附鍤愭い鏍ㄧ矌绾句粙鏌熼幑鎰厫濠殿垰鍚嬮妵鍕籍閸屾艾浠橀梺璇查椤兘寮婚悢鍏煎仺闁割煈鍋掓禒褏绱撴担浠嬪摵缂佽鐗嗛～蹇旂附缁嬭法鐓戞繝銏ｅ煐閿氬?SQL 闂備浇宕垫慨鏉懨归崒鐐插偍闁肩鍩囨禍褰掓煟閹邦剛浠涢柟顖樺劦閺屾稑鈽夊Ο鍏兼喖濠殿噯绲鹃崝娆撳蓟閻旇偐鍙曢柟缁樺笒婵箓姊?
        // 婵犵數濮烽。浠嬪焵椤掆偓閸熷潡鍩€椤掆偓缂嶅﹪骞冨Ο璇茬窞閻庯綆鍋勯鎾绘⒑缂佹﹩鐒芥い锝勭矙閸┾偓妞ゆ帊瀛ｉ幋鐘电煔闂侇剙绉撮悞娲煕閹板吀绨肩悮?30缂?闂傚倷鐒﹂惇褰掑礉瀹€鈧埀顒佸嚬閸ｏ絽鐣烽幋锔芥櫜闁搞儯鍔岄惃顐︽⒑缂佹ê濮囬柣掳鍔庨惀顏堫敍閻愬鍘搁梺绋挎湰閻熴儱鐣甸崱娑欑厱闁挎繂娲ら崝瀣磼鐎ｎ亶妲告い鎾炽偢瀹曠喖顢橀悩闈涚倞闂備浇顕х花鑲╁緤缂佹鐝堕柛顐犲劚閸?SQL 闂備胶鍎甸崜婵堟暜閹烘绠犻柟鎹愬煐瀹?4MB闂傚倷鐒︾€笛呯矙閹达附鍤愭い鏍ㄧ缚娴滃綊鏌涘▎蹇ｆШ妞も晝鍏橀弻鏇熺珶椤栨艾顏柛鎴滅矙濮婅櫣鎲撮崟顏囧焻闂佺瀛╅幐鎶藉箚?baseMapper
        if (updateMap.size() > 500) {
            // 闂傚倷绀侀幉锛勬暜閹烘嚦娑樷枎閹邦喚褰鹃梺鍦劋椤ㄥ棝鎮炴繝姘厸闁告洦鍋嗙粻鎶芥偨?(濠?00闂傚倷绀侀幖顐λ囬锕€绀堟繝闈涱焾娴滅懓霉閿濆懏璐℃い鈺傜叀閺屾洟宕煎┑鍡╀紑濡炪倕绻楁ご鍝ユ崲?
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                baseMapper.updateLikeCountBatch(batchMap);
            }
        } else {
            // 闂傚倷娴囧銊ヮ渻閹烘纭€闁规儼妫勯梻顖炴煣韫囨稈鍋撳☉姘⒕闂備胶鎳撻顓熸叏閻㈢數妫い鏍仦閻撴瑩鏌熼鐔风瑨闁告梹绮岄…鑳槾闁哄拋鍋婇獮?
            baseMapper.updateLikeCountBatch(updateMap);
        }
        flashCache();
        return true;
    }

    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊堝础閹惰姤鍊垫繛鎴烆伆閹达附鍋傞柍褜鍓熷娲川婵犲孩鐣烽梺鐓庣秺缁犳牠銆佸鈧畷鐑筋敇閻旈褰撮梻浣告啞濞诧箓宕抽鈧畷顖炲醇閵夛妇鍘?
     *
     * @param updateMap 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╃磼濡ゅ啯銇熸繝鐢靛仦閸ㄥ爼骞愮粙娆炬僵闁挎洖鍊归崕濠囧箹鏉堝墽绋诲┑顖氥偢閺岋綁骞嬮悙鍡樺灥閳绘捇宕奸弴鐔哄幗闂侀潧顭堥崕閬嶎敂椤忓懍绻嗛柟顖ｅ幖閹冲秹鎮?
     * @return 闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樻彃顏痪鎯с偢閺岀喖骞嗚閸ょ喎霉?
     */
    @Override
    public Boolean updateCommentCountBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }

        // 闂佽娴烽崑锝夊磹濠靛牏纾芥慨姗€顤傞弫濠囨煕濞戞鎽犻柡鍜佸墯閹便劌顫滈崱妤€鈷掑┑鐐茬湴閸婃繈寮婚敍鍕ㄥ亾閿濆骸浜為柣顓烆儔閺岋繝宕卞▎蹇旂亪闂佸搫鑻惌浣虹不濞戙垹鍗抽柣鎴濇椤ユ粓姊绘担鍛婂暈閻㈩垪鏅犲畷鎴﹀礋椤栨?闂備胶鍎甸崜婵堟暜閹烘绠犻柟鎹愬煐瀹?00)闂傚倷鐒︾€笛呯矙閹达附鍤愭い鏍ㄧ矌绾句粙鏌熼幑鎰厫濠殿垰鍚嬮妵鍕籍閸屾艾浠橀梺璇查椤兘寮婚悢鍏煎仺闁割煈鍋掓禒褏绱撴担浠嬪摵缂佽鐗嗛～蹇旂附缁嬭法鐓戞繝銏ｅ煐閿氬?SQL 闂備浇宕垫慨鏉懨归崒鐐插偍闁肩鍩囨禍褰掓煟閹邦剛浠涢柟顖樺劦閺屾稑鈽夊Ο鍏兼喖濠殿噯绲鹃崝娆撳蓟閻旇偐鍙曢柟缁樺笒婵箓姊?
        // 婵犵數濮烽。浠嬪焵椤掆偓閸熷潡鍩€椤掆偓缂嶅﹪骞冨Ο璇茬窞閻庯綆鍋勯鎾绘⒑缂佹﹩鐒芥い锝勭矙閸┾偓妞ゆ帊瀛ｉ幋鐘电煔闂侇剙绉撮悞娲煕閹板吀绨肩悮?30缂?闂傚倷鐒﹂惇褰掑礉瀹€鈧埀顒佸嚬閸ｏ絽鐣烽幋锔芥櫜闁搞儯鍔岄惃顐︽⒑缂佹ê濮囬柣掳鍔庨惀顏堫敍閻愬鍘搁梺绋挎湰閻熴儱鐣甸崱娑欑厱闁挎繂娲ら崝瀣磼鐎ｎ亶妲告い鎾炽偢瀹曠喖顢橀悩闈涚倞闂備浇顕х花鑲╁緤缂佹鐝堕柛顐犲劚閸?SQL 闂備胶鍎甸崜婵堟暜閹烘绠犻柟鎹愬煐瀹?4MB闂傚倷鐒︾€笛呯矙閹达附鍤愭い鏍ㄧ缚娴滃綊鏌涘▎蹇ｆШ妞も晝鍏橀弻鏇熺珶椤栨艾顏柛鎴滅矙濮婅櫣鎲撮崟顏囧焻闂佺瀛╅幐鎶藉箚?baseMapper
        if (updateMap.size() > 500) {
            // 闂傚倷绀侀幉锛勬暜閹烘嚦娑樷枎閹邦喚褰鹃梺鍦劋椤ㄥ棝鎮炴繝姘厸闁告洦鍋嗙粻鎶芥偨?(濠?00闂傚倷绀侀幖顐λ囬锕€绀堟繝闈涱焾娴滅懓霉閿濆懏璐℃い鈺傜叀閺屾洟宕煎┑鍡╀紑濡炪倕绻楁ご鍝ユ崲?
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                baseMapper.updateCommentCountBatch(batchMap);
            }
        } else {
            // 闂傚倷娴囧銊ヮ渻閹烘纭€闁规儼妫勯梻顖炴煣韫囨稈鍋撳☉姘⒕闂備胶鎳撻顓熸叏閻㈢數妫い鏍仦閻撴瑩鏌熼鐔风瑨闁告梹绮岄…鑳槾闁哄拋鍋婇獮?
            baseMapper.updateCommentCountBatch(updateMap);
        }
        flashCache();
        return true;
    }

    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊堝础閹惰姤鍊垫繛鎴烆伆閹达附鍋傞柍褜鍓熷娲川婵犲孩鐣烽梺鐓庣秺缁犳牠銆佸Ο瑁や汗闁圭儤鍨归崫妤呮⒑鐠団€崇仯濠⒀勵殜钘熼柟杈鹃檮閻?
     *
     * @param updateMap 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╃磼濡ゅ啯銇熸繝鐢靛仦閸ㄥ爼骞愰崫銉㈠亾濞戞帗娅婄€殿噮鍋夐妵鎰板箳閹惧磭妾梻濠庡亜濞诧箑顫忛悷鎵虫灁闁割偅娲橀悡鐔兼煏婵炲灝鍔氭い蹇ｄ簼娣囧﹪骞嗛鐐村櫢闁?
     * @return 闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樻彃顏痪鎯с偢閺岀喖骞嗚閸ょ喎霉?
     */
    @Override
    public Boolean updateStarCountBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }

        // 闂佽娴烽崑锝夊磹濠靛牏纾芥慨姗€顤傞弫濠囨煕濞戞鎽犻柡鍜佸墯閹便劌顫滈崱妤€鈷掑┑鐐茬湴閸婃繈寮婚敍鍕ㄥ亾閿濆骸浜為柣顓烆儔閺岋繝宕卞▎蹇旂亪闂佸搫鑻惌浣虹不濞戙垹鍗抽柣鎴濇椤ユ粓姊绘担鍛婂暈閻㈩垪鏅犲畷鎴﹀礋椤栨?闂備胶鍎甸崜婵堟暜閹烘绠犻柟鎹愬煐瀹?00)闂傚倷鐒︾€笛呯矙閹达附鍤愭い鏍ㄧ矌绾句粙鏌熼幑鎰厫濠殿垰鍚嬮妵鍕籍閸屾艾浠橀梺璇查椤兘寮婚悢鍏煎仺闁割煈鍋掓禒褏绱撴担浠嬪摵缂佽鐗嗛～蹇旂附缁嬭法鐓戞繝銏ｅ煐閿氬?SQL 闂備浇宕垫慨鏉懨归崒鐐插偍闁肩鍩囨禍褰掓煟閹邦剛浠涢柟顖樺劦閺屾稑鈽夊Ο鍏兼喖濠殿噯绲鹃崝娆撳蓟閻旇偐鍙曢柟缁樺笒婵箓姊?
        // 婵犵數濮烽。浠嬪焵椤掆偓閸熷潡鍩€椤掆偓缂嶅﹪骞冨Ο璇茬窞閻庯綆鍋勯鎾绘⒑缂佹﹩鐒芥い锝勭矙閸┾偓妞ゆ帊瀛ｉ幋鐘电煔闂侇剙绉撮悞娲煕閹板吀绨肩悮?30缂?闂傚倷鐒﹂惇褰掑礉瀹€鈧埀顒佸嚬閸ｏ絽鐣烽幋锔芥櫜闁搞儯鍔岄惃顐︽⒑缂佹ê濮囬柣掳鍔庨惀顏堫敍閻愬鍘搁梺绋挎湰閻熴儱鐣甸崱娑欑厱闁挎繂娲ら崝瀣磼鐎ｎ亶妲告い鎾炽偢瀹曠喖顢橀悩闈涚倞闂備浇顕х花鑲╁緤缂佹鐝堕柛顐犲劚閸?SQL 闂備胶鍎甸崜婵堟暜閹烘绠犻柟鎹愬煐瀹?4MB闂傚倷鐒︾€笛呯矙閹达附鍤愭い鏍ㄧ缚娴滃綊鏌涘▎蹇ｆШ妞も晝鍏橀弻鏇熺珶椤栨艾顏柛鎴滅矙濮婅櫣鎲撮崟顏囧焻闂佺瀛╅幐鎶藉箚?baseMapper
        if (updateMap.size() > 500) {
            // 闂傚倷绀侀幉锛勬暜閹烘嚦娑樷枎閹邦喚褰鹃梺鍦劋椤ㄥ棝鎮炴繝姘厸闁告洦鍋嗙粻鎶芥偨?(濠?00闂傚倷绀侀幖顐λ囬锕€绀堟繝闈涱焾娴滅懓霉閿濆懏璐℃い鈺傜叀閺屾洟宕煎┑鍡╀紑濡炪倕绻楁ご鍝ユ崲?
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                baseMapper.updateStarCountBatch(batchMap);
            }
        } else {
            // 闂傚倷娴囧銊ヮ渻閹烘纭€闁规儼妫勯梻顖炴煣韫囨稈鍋撳☉姘⒕闂備胶鎳撻顓熸叏閻㈢數妫い鏍仦閻撴瑩鏌熼鐔风瑨闁告梹绮岄…鑳槾闁哄拋鍋婇獮?
            baseMapper.updateStarCountBatch(updateMap);
        }
        flashCache();
        return true;
    }

    /**
     * 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎缁绢厸鍋撴繝娈垮枟閿曗晠宕滈敃鍌氳Е闁稿瞼鍋為悡娆愩亜閹烘垵鈧崵鏁☉銏＄厸?
     *
     * @return 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╂喐閻楀牆绗掔紒鐘冲灩缁辨挻鎷呴懖鈩冨灥閳?
     */
    @Override
    public Integer getBlogTotal() {
        return query().count().intValue();
    }
    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫤闁稿鍔戦弻鏇熺節韫囨洜鏆犻梺缁樻尰濞茬喖寮诲☉妯滄梹鎷呴挊澶樻婵＄偑鍊栭幐鍝ョ礊婵犲偆鍤曟い鎰剁到椤曢亶鏌℃径瀣仸妞?
     *
     * @param userId 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰銉︽叏閸︻厽瀚?
     * @return 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╂喐閻楀牆绗掗柡瀣╃闇夐柛蹇撳悑缂嶆垶绻?
     */
    @Override
    public Integer getBlogCount(Long userId) {
        //闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫣闁哄绀侀湁闁稿繐鍚嬬紞鎴炵箾?
        Long count = query().eq("user_id", userId).count();

        // 闂傚倷鑳堕崕鐢稿疾濞戙垺鍋ら柕濞у嫭娈伴柣搴㈢⊕钃卞┑顔界矋閵囧嫰骞掑鍥舵М闁瑰吋娼欓敃顏堝蓟閵娿儮妲堟俊顖濆亹閸旑喚绱撴担鍓叉Ч闁圭鍟块悾宄扳枎閹扳晛浜伴梺鍓茬厛閸犳艾危濞差亝鈷戦柟鑲╁仜閸斻倗绱掔紒妯肩畼婵″弶鍔栫€靛ジ寮堕幋鐙€鍚呮俊鐐€曠换鎰板箠韫囨稒鍊?
        return count.intValue();
    }

    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫤闁稿鍔戦弻鏇熺節韫囨洜鏆犻梺缁樻尰濞茬喖寮诲☉妯滄梹鎷呴挊澶樻婵＄偑鍊栭幐鍝ョ礊婵犲洤绠犻柕蹇曞Х閺嗗鏌熷▓鍨灓缂佺姵甯″鍝勑ч崶褍顬堥柣搴㈠嚬閸欏啴宕洪埀顒併亜閹烘垵鏆欓柛姘秺閺屾稓鈧絽澧庨幃濂告煙缁夊棗娲﹂崐鐑芥倵濞戞鎴︻敊?
     *
     * @param userId 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰銉︽叏閸︻厽瀚?
     * @return 闂傚倷鑳剁划顖炲礉濡ゅ懌鈧焦绻濋崟顓犵効闂佸湱澧楀妯肩不閹寸姷纾藉ù锝堝亗閹存績鏋?
     */
    @Override
    public Integer getLikeCount(Long userId) {
        List<Blog> blogList = query().eq("user_id", userId).list();
        return blogList.stream().mapToInt(Blog::getLiked).sum();
    }

    /**
     * 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎缁绢厸鍋撴繝娈垮枟閿曗晠宕滈敃鍌氳Е闁稿瞼鍋為悡鐔兼煙闁箑娅橀柡鈧幍顔剧＜闁稿本绋戞慨宥団偓?
     *
     * @param sourceId 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╃磼濡ゅ啯銇?
     * @return 闂傚倷鑳剁划顖炲礉濡ゅ懌鈧焦绻濋崟顓犵効闂佸湱澧楀姗€寮告担璇ュ綊宕楅崗鑲╃▏濠?
     */
    @Override
    public Integer getBlogLikeCount(Long sourceId) {
        return query()
                .select("liked")
                .eq("id", sourceId).one()
                .getLiked();
    }

    /**
     * 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎缁绢厸鍋撴繝娈垮枟閿曗晠宕滈敃鍌氳Е闁稿瞼鍋為悡娑㈡煃瑜滈崜鐔肩嵁閸℃稒鍋嬮柛顐亝椤ユ棃姊?
     *
     * @param sourceId 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╃磼濡ゅ啯銇?
     * @return 闂傚倷娴囬妴鈧柛瀣崌閺岀喖顢涘鍐炬毉濡炪們鍎查崹鍧楀蓟閳╁啯濯寸€瑰嫭婢樼粊顕€姊?
     */
    @Override
    public Integer getBlogStarCount(Long sourceId) {
        return query()
                .select("stared")
                .eq("id", sourceId)
                .one()
                .getStared();
    }

    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫣缂佺姳鍗抽弻娑㈠Ψ閹存繃鍣烘慨锝呴叄濮婃椽宕ㄦ繝鍌滅懆濠碘槅鍋呯粙鎾诲箖椤曗偓椤㈡洟鏁愰崶锝嗙亙闂佽鍑界紞鍡涘礈濮樿埖鍋╅梺顒€绉甸悡鏇熶繆閵堝嫭绁板瑙勆戦妵鍕箻瀹曞洨楔閻庤娲橀懝鎹愮亙闂佸憡娲嶉弬渚€宕?
     *
     * @param typeId  闂傚倷绀侀幉锛勬暜閹烘嚦娑樷槈濮橆厼浠忛梺缁樼〒閻?
     * @param current 闂佽崵鍠愮划搴㈡櫠濡ゅ懎绠伴柛娑橈攻濞呯娀鏌ｅΟ鐓庝缓濞存粍绮撻弻锝夊閵忊剝姣勯梺?
     * @return 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╂喐閻楀牆绗掔紒鈧崱妯圭箚闁靛牆鎳庨銉╂煃?
     */
    @Override
    public List<BlogVO> queryBlogByCategory(Long typeId, Integer current) {
        //婵犵數鍋涢顓熸叏鐎靛摜鐜荤€圭娼恑s闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫣缂佲偓閸℃稒鐓曢柍鈺佸暟閹冲棙銇勯顒傜暤闁哄本绋戣灃濞达綀妫勯ˉ婵嗩渻?
        String key= RedisConstants.CACHE_BLOG_TYPE_KEY + typeId+":"+ current;
        List<Long> blogIdList = getBlogIdListFromRedis(key, SystemConstants.MAX_PAGE_SIZE);
        if (CollUtil.isNotEmpty(blogIdList)) {
            return getBlogListByIds(blogIdList);
        }
        Page<Blog> page = query()
                .select("images","liked","user_id","title","id")
                .eq("type_id", typeId)
                .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔荤暗濞存粌缍婇弻鐔煎箚瑜嶉弳杈ㄣ亜閵堝懏鍣规い顏勫暣婵″爼宕ㄩ娆戦┏闂備礁鎼Λ娑㈠窗閺嶎厾宓?
        List<Blog> blogList = page.getRecords();
        List<BlogVO> voList = convertToBlogVOList(blogList);
        if(blogList!= null&&blogList.size()>0){
            // 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸鎱ㄦ繝鍐ㄦ暏og闂傚倷绀侀幖顐︽偋閸℃蛋鍥敍閻愯尙鐓戦悷婊勬楠炲棝宕橀鑲╊槹濡炪倖鎸鹃崰鎰枔閵忋倖鈷戦柛锔诲弾濡孩绻涢幘璇℃綈缂佸矁椴哥换婵嬪炊瑜忛悾?
            queryBlogListUserMessage(voList);
            queryBlogListIsLike(voList);
            //闂傚倷鑳堕、濠傗枖濞戙垺鏅濋柕澶嗘櫆閸嬪倿鏌ㄥ☉妯侯仾閻庢碍宀搁弻鏇＄疀鐎ｎ亞浼勫┑鐐差槶閸ㄤ粙寮婚敍鍕ㄥ亾閿濆骸浜為柣顓烇躬閺屾盯鎮㈤崨濠傜３閻庤娲橀〃濠勭槵缂備焦姊规禍鐥爏
            saveBlogIdListToRedis(key, blogList, RedisConstants.CACHE_HOT_BLOG_TTL, TimeUnit.DAYS);
        }
        return voList;
    }

    /**
     * 闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樺弶鎼愮痪顓涘亾婵犳鍠楅敃鈺呭礈閿曞倸瑙﹂柛宀€鍋為悡鐔镐繆椤栨氨浠㈤柣鎾村姍閺屽秷顧侀柛蹇旂〒濞嗐垽濡堕崶顭戞綗濠电偛妫欓崹鐔煎磻閹捐绀傚璺猴工閳峰姊虹紒姗嗘畷濠电偛锕濠氬川椤撗勫兊閻庤娲栧ú銊╂偩?闂傚倷绀佺紞濠囧绩鏉堚晜鏆滈柣鎰版涧閸ㄦ繈鏌ｉ幋锝呅撻柡?
     *
     * @param targetId 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╃磼濡ゅ啯銇?
     * @param status   闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╂喐閻楀牆绗氶柛瀣姍閺屻倝宕妷顔芥瘜闂?
     * @return 闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樻彃顏痪鎯с偢閺岀喖骞嗚閸ょ喎霉?
     */
    @Override
    public Boolean updateBlogStatus(Long targetId, Integer status, String reason) {
        // 闂傚倷绀佺紞濠囧绩鏉堚晜鏆滈柣鎰版涧閸ㄦ繈鏌ｉ幋锝嗩棄缂侇偄绉归幃褰掑传閸曨剚鍎撻梺鍝勬噺閹倿寮诲☉婊呯杸閻庯綆浜滄慨鏇㈡煟鎼淬垼澹樻俊顐ｇ〒濡叉劙骞掗幋鏃€鏂€闂佺硶鍓濋〃鍡涘几妤ｅ啯鈷戦柟鑲╁仜閸斻倕鈹戦姘煎殶婵″弶鍔曢埞鎴犫偓锝庡亞閸旈潧鈹戦鐭亞澹曢鐘典笉闁哄稁鍘介悡娑㈡煕閹伴潧骞栭柣鎾村姉缁辨帡寮崶褍鎯炵紓渚囧枟瀹€鎼佸箖濠婂吘鐔兼惞閻熸壆绋荤紓鍌氬€搁崐鐑芥倿閿旂偓宕查柛灞剧矋閺嗘粍銇勯幇鍓佺暠闁?
        String rejectReason = AuditStatusEnum.isRejected(status) ? reason : null;
        Short businessStatus = null;
        if (Objects.equals(status, AuditStatusEnum.PASS.getCode())) {
            businessStatus = ContentStatusEnum.PUBLISHED.getCode().shortValue();
        } else if (AuditStatusEnum.isRejected(status)) {
            businessStatus = ContentStatusEnum.OFF.getCode().shortValue();
        }
        UpdateWrapper<Blog> uw = new UpdateWrapper<Blog>()
                .set("audit_status", status)
                .set("reject_reason", rejectReason)
                .eq("id", targetId);
        if (businessStatus != null) {
            uw.set("status", businessStatus);
        }
        boolean updated = update(uw);
        if (updated) {
            flashRedisBlogCache(targetId);
            flashRedisBlogListCache();
            if (Objects.equals(status, AuditStatusEnum.PASS.getCode())) {
                publish(new String[]{targetId.toString()});
            } else if (AuditStatusEnum.isRejected(status)) {
                sendBlogDeleteSyncMessage(targetId);
            }
        }
        return updated;
    }

    /**
     * 闂傚倷鑳堕…鍫㈡崲閸儱绀夐柟杈剧畱绾惧潡鏌熺紒銏犳灈闁活厽顨婇弻鐔衡偓娑欘焽缁犳ɑ銇勮箛鏃€灏﹂柡灞剧☉铻栧ù锝堟椤ユ繂顪?
     *
     * @return 闂傚倷鑳堕…鍫㈡崲閸儱绀夐柟杈剧畱绾惧潡鏌熺紒銏犳灈闁活厽顨婇弻鐔衡偓娑欘焽缁犳ɑ銇勮箛鎿冨殶缂佽鲸鎸婚幏鍛喆閸曨偊鐎洪梻?
     */
    @Override
    public String allPublish() {
        int page = PageConstants.PAGE_NUMBER;
        int pageSize =5; // 濠电姵顔栭崳顖滃緤閻ｅ本宕叉慨妞诲亾濠?0闂?
        while (true) {
            // 闂傚倷绀侀幉锛勬暜閹烘嚦娑樷攽鐎ｎ€儱顭块懜闈涘闁藉啰鍠栭弻鏇熷緞濡厧甯ラ梺?
            List<Blog> blogs = query()
                    .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .page(new Page<>(page, pageSize))
                    .getRecords();
            if (blogs.isEmpty()) {
                break;
            }
            //婵犵數鍋犻幓顏嗙礊閳ь剚绻涙径瀣鐎殿噮鍋婃俊鍫曞幢濡ゅ啰鐛繝娈垮枟閿氱€规洦鍓欓埢鎾绘偐閻㈢數锛滈柣搴秵娴滃爼宕曢幇顒夌唵閻熸瑥瀚ù顕€鏌＄仦鐣岀劮缂佺姵鐩顒傛崉閵娧佸仭闂?
            int finalPage = page;
            executorService.execute(() -> {
                List<BlogVO> voList = convertToBlogVOList(blogs);
                queryBlogListUserMessage(voList);
                // 闂傚倷绀侀幉锛勬暜濡ゅ啰鐭欓柟瀵稿Х绾句粙鏌熼幑鎰厫閻庢碍宀稿娲垂椤曞懎鍓冲┑鐘亾閻庢稒锕╁▓浠嬫煟閹邦垰鐓愮痪顓炲⒔閹叉悂寮堕崹顔瑰亾閸ф钃?
                sendBlogBatchSyncMessage(voList);
                log.info("Thread {} published blog page {}, size {}", Thread.currentThread().getName(), finalPage, blogs.size());
            });
            page++;
        }
        return "publish success";
    }

    /**
     * 闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柣銏㈡暩閸楁岸鏌ｉ幋锝嗩棄缁绢厸鍋撴繝娈垮枟閿曗晠宕滈敃鍌氳Е?
     *
     * @param
     * @return 闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柣銏㈡暩閸楁艾顪冪€ｎ亝鎹ｇ痪鎯с偢閺岀喖骞嗚閸ょ喎霉?
     */
    @Override
    public String publish(String[] ids) {
        if (ids == null || ids.length == 0) {
            return "no ids to publish";
        }
        // Convert to Long list
        List<Long> idList = Arrays.stream(ids)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        executorService.submit(() -> {
            log.info("Publishing blogs {} on thread {}", idList, Thread.currentThread().getName());
            // Batch query
            List<Blog> blogs = query()
                    .in("id", idList)
                    .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .list();
            if (CollUtil.isNotEmpty(blogs)) {
                List<BlogVO> voList = convertToBlogVOList(blogs);
                // Batch populate user info
                queryBlogListUserMessage(voList);
                // Batch send message
                sendBlogBatchSyncMessage(voList);
            }
        });
        return "publish success";
    }

    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊堟儗濡ゅ懏鐓欓弶鍫熷礃閸氬倿鏌涚€ｎ偅灏扮紒瀣樀楠炲秹骞橀鐣屽幈濠殿喗锕╅崜姘耿閻楀牄浜滈柟鐑樻煥娴犳粓鏌嶉挊澶樻█鐎规洖銈搁幃銏ゅ礈娴ｄ警妲?
     * @param blogs
     */
    private void sendBlogBatchSyncMessage(List<?> blogs) {
        if (CollUtil.isEmpty(blogs)) {
            return;
        }
        ContentBatchSyncMessage request = new ContentBatchSyncMessage();
        request.setIndexName(EsIndexNameConstants.BLOG_INDEX_NAME);
        request.setData(blogs);
        request.setType(GlobalBizTypeEnum.BLOG.getCode());
        mqMessageSendUtils.sendMqMessage(
                SearchMqConstants.ES_SYNC_EXCHANGE,
                SearchMqConstants.ES_SYNC_BATCH_INSERT_ROUTING_KEY,
                request);
        mqMessageSendUtils.sendMqMessage(
                SearchMqConstants.MILVUS_SYNC_EXCHANGE,
                SearchMqConstants.MILVUS_SYNC_BATCH_INSERT_ROUTING_KEY,
                request);
    }

    private void sendBlogDeleteSyncMessage(Long id) {
        ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
        contentSyncMessage.setId(id);
        contentSyncMessage.setIndexName(EsIndexNameConstants.BLOG_INDEX_NAME);
        contentSyncMessage.setType(GlobalBizTypeEnum.BLOG.getCode());
        mqMessageSendUtils.sendMqMessage(
                SearchMqConstants.ES_SYNC_EXCHANGE,
                SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY,
                contentSyncMessage);
        mqMessageSendUtils.sendMqMessage(
                SearchMqConstants.MILVUS_SYNC_EXCHANGE,
                SearchMqConstants.MILVUS_SYNC_DELETE_ROUTING_KEY,
                contentSyncMessage);
    }
    /**
     * 闂傚倷绀侀幉锛勬暜閿熺姴缁╅梺顒€绉撮拑鐔封攽閻樺弶鎼愮痪顓涘亾婵犳鍠楅敃鈺呭礈閿曞倸瑙﹂柛灞剧矌绾句粙鏌涚仦鍓р姇閻忓浚鍙冮弻锝嗗箠闁告梹鍨垮顐㈩吋婢跺﹪鍞堕梺鎸庣箓妤犵鈻撳鍫熲拺?+ 闂傚倷绀侀幉锛勬暜濡ゅ懌鈧啯寰勯幇顑?+ 闂傚倷绀侀幉锛勬暜閹烘嚦娑樷槈濮橆厼浠忓銈嗗姧闂勫嫰寮?
     *
     * @return 闂傚倷绀侀幉锛勬暜閿熺姴缁╅梺顒€绉撮拑鐔封攽閻樻彃顏痪鎯с偢閺岀喖骞嗚閸ょ喎霉?
     */
    @Override
    public String flashCache() {
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_HOT_BLOG_KEY+"*"));
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_BLOG_KEY+"*"));
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_BLOG_TYPE_KEY+"*"));
        return null;
    }
    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸鎱ㄦ繝鍐ㄦ暏og闂傚倷绀侀幖顐︽偋閸℃蛋鍥敍閻愯尙鐓戦悷婊勬楠炲棝宕橀鑲╊槹濡炪倖鎸鹃崰鎰枔閵忋倖鈷戦柛锔诲弾濡孩绻涢幘璇℃綈缂佸矁椴哥换婵嬪炊瑜忛悾?
     * @param blogVO
     */
    private void queryBlogUser(BlogVO blogVO){
        if (blogVO == null) {
            return;
        }
        fillBlogUserNames(Collections.singletonList(blogVO));
    }
    /**
     * 闂傚倷绀侀幉锛勬暜閸ヮ剙纾归柡宥庡幖閽冪喖鏌涢妷顔荤暗濞存粌缍婇弻鐔煎箚瑜嶉弳杈ㄣ亜閵堝懏鍤囬柡宀嬬秮閿濈偤顢楅埀顒佷繆娴犲鐓曢柍鍝勫€诲ú瀵糕偓娈垮枙閸楁娊銆佸☉姗嗘僵妞ゆ挾鍋涙晶楣冩煟鎼淬値娼愰柣鈩冩礈娴滅鈻庨幋婵嗙亰闂佽法鍠撴慨鐢稿磹妞嬪海妫い鎾跺仦閸ｈ姤銇?
     * @param blog
     */
    private void isBlogLiked(BlogVO blogVO) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            //闂傚倷绀侀幖顐︽偋濠婂嫮顩查柣鎰ゴ閺嬪秹鏌曟径鍫濆Ω濞?婵犵數鍋為崹鍫曞箰閸濄儳鐭撻柛顐ｆ礀閺嬩線鏌曢崼婵愭Ц闁藉啰鍠栭弻鏇熷緞濡厧甯ラ梺鎼炲€曠€氫即寮婚敓鐘茬闂傚牊绋撴禒鈺呮⒑鐠団€崇仧缂佽埖宀搁獮鍐ㄢ枎閹寸偛鍘归梺鍛婁緱閸ㄧ厧霉閳?
            blogVO.setIsLike(false);
            return;
        }
        //闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔荤暗濞存粌缍婇弻鐔煎箚瑜嶉弳杈ㄣ亜閵堝懏鍤囬柡宀嬬秬椤﹁埖銇勯弴鍡楁噽缁€濠勨偓骞垮劚椤︿即宕戦妸鈺傜厪濠电姴绻掗悾閬嶆煟?
        Long userId = user.getId();
        LikeDTO likeDTO = new LikeDTO();
        likeDTO.setUserId(userId);
        likeDTO.setSourceId(blogVO.getId());
        likeDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
        //闂傚倷绀侀幉锛勬暜閸ヮ剙纾归柡宥庡幖閽冪喖鏌涢妷顔荤暗濞存粌缍婇弻鐔煎箚瑜嶉弳杈ㄣ亜閵堝懏鍤囬柡宀嬬秮閿濈偤顢楅埀顒佷繆娴犲鐓曢柍鍝勫€诲ú瀵糕偓娈垮枙閸楁娊銆佸☉姗嗘僵妞ゆ挾鍋涙晶楣冩煟鎼淬値娼愰柣鈩冩礈娴滅鈻庨幋婵嗙亰闂佽法鍠撴慨鐢稿磹妞嬪海妫い鎾跺仦閸ｈ姤銇?
        Boolean isLike = remoteLikeService.isLike(likeDTO);
        blogVO.setIsLike(isLike);
    }

    /**
     * 闂傚倷绀侀幉锛勬暜閸ヮ剙纾归柡宥庡幖閽冪喖鏌涢妷顔荤暗濞存粌缍婇弻鐔煎箚瑜嶉弳杈ㄣ亜閵堝懏鍤囬柡宀嬬秮閿濈偤顢楅埀顒佷繆娴犲鐓曢柍鍝勫€诲ú瀵糕偓娈垮枙閸楁娊銆佸☉姗嗘僵妞ゆ挾鍋涙晶楣冩煟鎼淬値娼愰柣鈩冩礈娴滅鈻庨幋婵嗙亰闂佽法鍠撴慨瀵哥磼閳哄懏鐓欐い鏍ㄧ矊椤ｅ吋銇勯妷锕€鐏撮柡灞剧☉铻栧ù锝堟椤ユ繂顪?
     * @param blog
     */
    private void isBlogStared(BlogVO blogVO) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            //闂傚倷绀侀幖顐︽偋濠婂嫮顩查柣鎰ゴ閺嬪秹鏌曟径鍫濆Ω濞?婵犵數鍋為崹鍫曞箰閸濄儳鐭撻柛顐ｆ礀閺嬩線鏌曢崼婵愭Ц闁藉啰鍠栭弻鏇熷緞濡厧甯ラ梺鎼炲€曠€氫即寮婚敓鐘茬闂傚牊绋撴禒鈺呮⒑鐠団€崇仧缂佽埖鑹鹃锝夊Ω閳轰胶鐣鹃梺缁橆殔閻楁粌螞?
            blogVO.setIsStared(false);
            return;
        }
        //闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔荤暗濞存粌缍婇弻鐔煎箚瑜嶉弳杈ㄣ亜閵堝懏鍤囬柡宀嬬秬椤﹁埖銇勯弴鍡楁噽缁€濠勨偓骞垮劚椤︿即宕戦妸鈺傜厪濠电姴绻掗悾閬嶆煟?
        Long userId = user.getId();
        StarDTO starDTO = new StarDTO();
        starDTO.setUserId(userId);
        starDTO.setSourceId(blogVO.getId());
        starDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
        //闂傚倷绀侀幉锛勬暜閸ヮ剙纾归柡宥庡幖閽冪喖鏌涢妷顔荤暗濞存粌缍婇弻鐔煎箚瑜嶉弳杈ㄣ亜閵堝懏鍤囬柡宀嬬秮閿濈偤顢楅埀顒佷繆娴犲鐓曢柍鍝勫€诲ú瀵糕偓娈垮枙閸楁娊銆佸☉姗嗘僵妞ゆ挾鍋涙晶楣冩煟鎼淬値娼愰柣鈩冩礈娴滅鈻庨幋婵嗙亰闂佽法鍠撴慨瀵哥磼閳哄懏鐓欐い鏍ㄧ矊椤ｅ吋銇?
        Boolean isStared = remoteStarService.isStar(starDTO);
        blogVO.setIsStared(isStared);
    }
    /**
     * 婵?Redis ZSet 婵犵數鍋為崹鍫曞箹閳哄懎鐭楅柍褜鍓氶〃銉╂倷閼碱剛顔掗悗瑙勬穿缂嶄礁顕ｉ崐鐕佹Щ闁荤喐鐟辩粻鎾诲箖濡ゅ啯瀚氶柍鈺佸暞濞堢檳闂傚倷绀侀幉锛勬暜濡ゅ懌鈧啯寰勯幇顑?
     *
     * @param key  缂傚倸鍊搁崐鎼佸磹瑜版帒绠伴柟闂寸劍閸嬨倝鏌曟繛鐐珕闁?
     * @param size 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎闁哄绀侀湁闁稿繐鍚嬬紞鎴炵箾?
     * @return 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╃磼濡ゅ啯銇熼梻鍌欑閹诧紕鏁Δ鍛偓鍐╁緞閹邦儵?
     */
    private List<Long> getBlogIdListFromRedis(String key, long size) {
        Page<Long> idPage = zSetIdManager.pageIds(buildBlogListZSetPrefix(key), BLOG_LIST_ZSET_SLOT, 1L, size);
        if (idPage == null || CollUtil.isEmpty(idPage.getRecords())) {
            return Collections.emptyList();
        }
        return idPage.getRecords();
    }

    /**
     * 闂備浇顕х换鎰崲閹邦儵娑樜旈埀顒勬偩閻戣棄唯鐟滃宕戦幘鑸靛珰闁斥晛鍟▓鐧夐梻鍌欑閹诧紕鏁Δ鍛偓鍐╁緞閹邦儵銉╂煕閹般劍娅囨い鈺呮敱閵囧嫯顦撮柛銈団偓宄甸梺鑽ゅ枑缁繘宕洪崶顒€鍨傞柛锔诲幗椤洟鏌嶉埡浣告殶闁崇懓绉电换婵嬫濞戞瑯妫ら梺鍦櫕婵炩偓闁哄本鐩獮鎺楀籍閳ь剟鎮甸弨鍗峣s缂傚倸鍊搁崐鎼佸磹瑜版帒绠伴柟闂寸劍閸?
     *
     * @param key     缂傚倸鍊搁崐鎼佸磹瑜版帒绠伴柟闂寸劍閸嬨倝鏌曟繛鐐珕闁?
     * @param blogList 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╂喐閻楀牆绗掔紒鈧崱妯圭箚闁靛牆鎳庨銉╂煃?
     * @param timeout  闂備礁鎼ˇ顐﹀疾濞戞◤娲晝閳ь剟鏁冮姀銈嗘櫢闁绘灏欓惈鍕⒑閸撴彃浜濇繛鍙夛耿閺?
     * @param unit     闂傚倷绀侀幖顐﹀疮椤愶附鍋夐柣鎾冲濞戙垹閿ゆ俊銈傚亾缁绢厸鍋撻梻浣告惈濞诧箑顫濋妸褏鐭?
     */
    private void saveBlogIdListToRedis(String key, List<Blog> blogList, long timeout, TimeUnit unit) {
        if (key == null || key.isEmpty() || CollUtil.isEmpty(blogList)) {
            return;
        }
        long scoreSeed = System.currentTimeMillis();
        List<BlogListRankItem> rankItems = new ArrayList<>(blogList.size());
        for (int i = 0; i < blogList.size(); i++) {
            Blog blog = blogList.get(i);
            if (blog == null || blog.getId() == null) {
                continue;
            }
            rankItems.add(new BlogListRankItem(blog.getId(), new Date(scoreSeed - i)));
        }
        if (CollUtil.isEmpty(rankItems)) {
            return;
        }
        String zSetKey = buildBlogListZSetKey(key);
        redisService.deleteObject(key);
        redisService.deleteObject(zSetKey);
        zSetIdManager.saveToZSet(zSetKey, rankItems, BlogListRankItem::getId, BlogListRankItem::getScoreTime);
        redisService.expire(zSetKey, timeout, unit);
    }

    /**
     * 闂傚倷绀侀幖顐︻敄閸涱垪鍋撳鐓庡缂佽鲸鎹囬獮妯兼嫚閼碱剦鏀ㄦ繝娈垮枟閿曗晠宕滈敃鍌氳Е闁稿瞼鍋為悡鏇㈡煙閻戞ɑ鎯勬繛鍫熺懇閺屾洟宕卞Δ鈧崝瀛瞖t缂傚倸鍊搁崐鎼佸磹瑜版帒绠伴柟闂寸劍閸嬨倝鏌曟繛鐐珕闁绘挶鍎查妵鍕籍閸屾艾浠樺銈冨€曢幊鎰閹捐纾兼俊銈傚亾濞?
     *
     * @param key 闂傚倷绀侀幉锟犫€﹂崶顒€绐楅幖鎼厜缂嶆牠鏌熼柇锕€鏋ょ痪鍙ョ矙閺岀喖骞嗚閿涘秹鏌熼悾灞解枅婵?
     * @return ZSet闂傚倸鍊烽悞锕傤敄濞嗘挸绐楅柡宥冨妽濞呯娀鏌ｅΟ鍏兼毄缁惧彞绮欓弻?
     */
    private String buildBlogListZSetPrefix(String key) {
        return key + ":";
    }

    /**
     * 闂傚倷绀侀幖顐︻敄閸涱垪鍋撳鐓庡缂佽鲸鎹囬獮妯兼嫚閼碱剦鏀ㄦ繝娈垮枟閿曗晠宕滈敃鍌氳Е闁稿瞼鍋為悡鏇㈡煙閻戞ɑ鎯勬繛鍫熺懇閺屾洟宕卞Δ鈧崝瀛瞖t缂傚倸鍊搁崐鎼佸磹瑜版帒绠伴柟闂寸劍閸嬨倝鏌曟繛褉鍋撻柛瀣崌閹兘鎮ч崼鐔稿闂備礁鎼Λ瀵哥礊娓氣偓瀵濡搁埡濠冩櫓闂佺绻掗崢褔鍩€?
     *
     * @param key 闂傚倷绀侀幉锟犫€﹂崶顒€绐楅幖鎼厜缂嶆牠鏌熼柇锕€鏋ょ痪鍙ョ矙閺岀喖骞嗚閿涘秹鏌熼悾灞解枅婵?
     * @return ZSet闂備浇顕уù鐑藉箠閹捐瀚夋い鎺戝濮规煡鏌ㄥ┑鍡╂Ч闁绘挶鍎查妵鍕棘閸喗鍊梺?
     */
    private String buildBlogListZSetKey(String key) {
        return buildBlogListZSetPrefix(key) + BLOG_LIST_ZSET_SLOT;
    }

    /**
     * 闂傚倷绀侀幉锟犮€冮崱娑欏殞濡わ絽鍠氶弫鍥╂喐閻楀牆绗掔紒鈧崱妯圭箚闁靛牆鎳庨銉╂煃瑜滈崜娆撳疮閹绢喚宓侀柟鐑橆殔缁犳娊鏌￠崶銉ュ闁哄倵鍋撴繝纰夌磿閸嬫垿宕愬Δ鈧埢宥夊閵忊槅娼熷┑鐘绘涧椤戝棝宕戦妸鈺傜厪濠电偛鐏濋埀顒侇殜閹虫繄绱炵粈顣媡缂傚倸鍊搁崐鎼佸磹瑜版帒绠伴柟闂寸劍閸嬨倝鏌曟繛鐐珔缂佺姵濞婇弻鐔兼倻濡櫣浠肩紒?
     * 闂備浇顕х换鎰崲鐎ｎ€㈠綊宕堕锕€顦扮换婵嬪炊瑜忛、鍛攽椤旀枻渚涢柛鎿勭畵瀹曘儲绗熺粙銈呪攽閻愭潙鐏﹂柟鍝ヮ焾椤繈鏁冮崒娑樺墾闁瑰吋鐣崝宀€绮堥崱娑欑厱闁斥晛鍙愰幋位鍥樄婵﹤顭峰畷鎺戔攽閸パ勯敪缂傚倷娴囨ご鍝ユ崲閸愵亞绠旈柣鏂跨殱閺岋妇鈧懓瀚垾顩氶梻鍌欑閹诧繝宕濋弽顓熷仭闁靛鏅涢惌妤呮煕閳╁啰鈽夌紒顐㈢Ч閺岋繝宕奸妷锔介敪闂佸疇顕х换姗€寮?
     */
    private static class BlogListRankItem {
        private final Long id;
        private final Date scoreTime;

        private BlogListRankItem(Long id, Date scoreTime) {
            this.id = id;
            this.scoreTime = scoreTime;
        }

        private Long getId() {
            return id;
        }

        private Date getScoreTime() {
            return scoreTime;
        }
    }
    /**
     * 濠电姷鏁搁崑鐐哄箰閹间礁绠犻柟鐗堟緲閻撴﹢鏌″搴″幍濞存粌缍婇弻鐔煎箚瑜嶉弳杈ㄣ亜閵堝懏鍤囬柡灞剧☉铻栧ù锝堟椤ユ繂顪冮妶鍡樺碍婵犫偓鏉堚晜顫曢柟鎹愵嚙缁犺崵鈧娲栧ú锕傚箟?
     *
     * @param blogId
     */
    private void flashRedisBlogCache(Long blogId) {
        //濠电姷鏁搁崑鐐哄箰閹间礁绠犻柟鐗堟緲閻撴﹢鏌″搴′簽缁惧彞绮欓弻鐔煎箚瑜忛敍宥夋煙?
        redisService.deleteObject(RedisConstants.CACHE_BLOG_KEY+blogId);
    }
    /**
     * 濠电姷鏁搁崑鐐哄箰閹间礁绠犻柟鐗堟緲閻撴﹢鏌″搴″箹缁绢厸鍋撴繝娈垮枟閿曗晠宕滈敃鍌氳Е闁稿瞼鍋為悡鏇㈡煙閻戞ɑ鎯勬繛鍫熺懇閺屾洟宕卞Δ鈧弳鐔虹磼鏉堛劍宕屾鐐疵悾鐑藉炊閵婏箑鏋?
     *
     * @param
     */
    private void flashRedisBlogListCache() {
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_BLOG_TYPE_KEY+"*"));
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_HOT_BLOG_KEY+"*"));
    }

    @Override
    public List<BlogVO> searchBlogs(String keyword) {
        String trimmedKeyword = keyword == null ? null : keyword.trim();
        List<Blog> blogList = query()
                .select("id", "title", "status", "audit_status")
                .like(StrUtil.isNotBlank(trimmedKeyword), "title", trimmedKeyword)
                .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .list();
        return convertToBlogVOList(blogList);
    }
}
