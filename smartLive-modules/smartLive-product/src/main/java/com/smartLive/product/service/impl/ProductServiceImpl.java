package com.smartLive.product.service.impl;
import com.smartLive.common.core.constant.mq.InteractionMqConstants;
import com.smartLive.common.core.constant.mq.SearchMqConstants;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.enums.common.AuditStatusEnum;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.interaction.FeedTypeEnum;
import com.smartLive.common.core.enums.product.ItemActionType;
import com.smartLive.common.core.enums.product.ProductEnum;
import com.smartLive.common.core.enums.product.ProductStatusEnum;
import com.smartLive.common.core.enums.product.SalesTypeEnum;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.domain.FeedEventMessage;
import com.smartLive.common.rabbitmq.domain.ContentSyncMessage;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.CacheClient;
import com.smartLive.common.redis.util.RedisMultiCacheManager;
import com.smartLive.common.redis.util.ZSetIdManager;
import com.smartLive.interaction.api.DTO.StarDTO;
import com.smartLive.interaction.api.RemoteFollowService;
import com.smartLive.interaction.api.RemoteStarService;
import com.smartLive.interaction.api.DTO.FollowDTO;

import com.smartLive.product.domain.VO.ProductVO;
import com.smartLive.product.service.strategy.PurchaseStrategy;

import java.util.concurrent.TimeUnit;

import com.smartLive.system.api.RemoteUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.smartLive.product.mapper.ProductMapper;
import com.smartLive.product.domain.Product;
import com.smartLive.product.service.IProductService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粌娴风划娆愬緞婵犲孩鍍靛銈嗗坊閸嬫挻銇勯鐘测偓婵嗩潖濞差亶鏁冮柨婵嗘－濞硷紕绱撴笟鈧·鍌炲磻閸℃稑鐒垫い鎺嗗亾婵犫偓闁秴纾婚柣鏃囧亹瀹撲線鏌涢妷銏℃珖閻?
 * 
 * 闂傚倷绀侀幖顐ょ矓閸洍鈧箓宕奸姀銏㈠婵°倧绲介崯顖炲吹閸屾壕鍋撻崗澶婁壕闂佸憡鍔戦崝宀勭嵁鐎ｎ喗鈷?
 * 1. 闂傚倸鍊烽悞锕併亹閸愵煁娲Ω閳轰焦鐎梺闈涚墕濞层劍銇欓幎鑺ョ厸鐎广儱楠告禍婵囩箾閸剛绋诲ǎ鍥э躬椤㈡稑顪冪拠韫閻庤娲栧ú銊┿€侀崨瀛樷拺闁圭娴烽埥澶嬬節閳ь剟鈥旂猾绔巋aseStrategy闂傚倷鐒︾€笛呯矙閹次诲洦娼忛埡浣哥亰闂佺粯鍨堕…鍥煝閺冨牊鐓冪憸婊堝礈濮橆剦鍤楅柛鏇ㄥ灠缁€瀣亜閹捐泛鏋庨柍褜鍓涢崑銈夊蓟閿熺姴妞藉ù锝呮啞閻濇繈姊洪柅鐐茶嫰婢ь垰菐閸ャ劍銇濈€规洘婢樿灒闁煎鍊愰弸娆愪繆椤愶富鏆掗柤鍐茬埣閹兘濮€閵忋垻锛滄繝銏ｆ硾椤戝懐绮顓犵闁绘挸楠搁弳锝嗩殽閻愭煡鍙勫┑顔瑰亾闂佹寧绻傞幊蹇涙倶鎼淬劍鈷掑〒姘搐娴滄繃淇婇崣澶婄婵炲棎鍨介、鏇㈡晝閳ь剝绻?
 * 2. 闂傚倷娴囨竟鍫熴仈閹间礁钃熼柕濠忛檮閸?Elasticsearch (ES) 婵?Milvus (闂傚倷绀侀幉锛勫枈瀹ュ鍨傞柛褎顨呴梻顖炴煟閹寸倖鎴﹀磿? 闂備浇顕ф绋匡耿闁秴纾婚柣鏃囧亹瀹撲線鏌涢妷顔煎闁活厽顨堥埀顒€鍘滈崑鎾绘煃瑜滈崜鐔风暦閹烘惟闁靛鍠栧▓銊╂⒑閸濆嫭宸濋柛瀣枛瀵埖绂掔€ｎ偆鍘?闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊呮喆閿曞倹鍊堕柣鎰祷濡炬悂鏌涢弬鍨伃闁?
 * 3. 闂傚倷鑳剁涵鍫曞疾閻愬樊娴栭柕濞у棗小?RabbitMQ 闂傚倷绀侀幉锟犳偡閿曞倸鍨傚┑鍌滎焾缁犳煡鏌ㄥ┑鍡╂Ц缂佲偓閸愵喗鐓曟繛鎴濆船瀵箖鏌涢悢閿嬪枠闁诡喗顨婂畷妤佸緞婵犲倵鎷ら梻浣虹帛椤ㄥ棙绻涙繝鍐х箚閻庢稒顭囬埢鏃€鎱ㄦ繝鍐┬ 濠电姷鏁搁崑鐔烘崲濠靛牊顐芥慨妯挎硾閻掑灚銇勯幒宥囧妽闁汇劍鍨块弻锝夊箼閸曨厾鐦堥悗瑙勬穿缁插墽鎹㈠┑瀣＜婵犲﹤瀚▓銈夋⒑鐠囨彃顒㈢紒瀣浮閳ワ箓宕奸敐鍡磽闂傚倷鑳堕…鍫ヮ敄閸儲鏅搁柕澶嗘櫅閻?
 * 4. 闂傚倸鍊搁崐绋课涘Δ鈧灋婵炲棙鎸搁崹?Redis 闂備浇顕ф绋匡耿闁秴纾婚柣鏃囧亹瀹撲線鏌涢妷锝呭缂佸墎鍋為幈銊ヮ潨閳ь剚绂嶉崼鏇熷仒闁规儳澧庣壕浠嬫煕鐏炲墽鈯曢悘蹇ｅ弮閺岋絾骞婇柛鏃€鍨甸悾宄扳攽鐎ｎ偄浜归梺缁樺灦椤洭濡舵导瀛樷拺闂傚牊绋掗ˉ娆撴煠閸愯尙鍩ｇ€规洘娲濋妵鎰板箳閹绢垱瀚介梻渚€娼ч…鍫ュ磿閹绘巻鏋嶇€光偓閸曨剛鍘卞┑鐘绘涧閹虫劙宕濆澶嬪仺妞ゆ牗绋掔亸鐢告煙椤旇棄顥嬪ù鐙呭缁辨帡濮€閿熺姵袙 ZSet 缂傚倸鍊搁崐椋庣矆娓氣偓閹椽濡搁敂钘夊伎闂侀潧绻堥崐鏇㈡儗濡も偓閳规垿鎮╅幓鎺嶇敖闂佸搫顦…宄邦潖濞差亜绠甸柟鐑樻⒒椤旀帡姊虹悰鈥充壕闂侀潧绻堥崐鏍偂閸岀偟鍙撻柛銉ｅ妽缁€鍫ユ煕閳哄啫鈻堥柟顔款潐鐎靛ジ顢欓挊澶樻闂?
 *
 * @author smartLive
 * @date 2026-02-18
 */
@Service
@Slf4j
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService
{
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private RedisService redisService;

    @Autowired
    private RemoteStarService remoteStarService;
    @Autowired
    private RemoteFollowService remoteFollowService;
    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private Map<String, PurchaseStrategy> purchaseStrategyMap;
    @Autowired
    private RedisMultiCacheManager redisMultiCacheManager;
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private ZSetIdManager zSetIdManager;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫣闁汇値鍣ｉ弻娑㈠焺閸愮偓鐣烽梺?
     *
     * @param id 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粌娴风划娆愬緞鐎ｎ剛鐦堝┑顔斤供閸樿棄鈻?
     * @return 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜濇导?
     */
    @Override
    public ProductVO selectProductById(Long id)
    {
        Product product = productMapper.selectProductById(id);
        return convertToProductVO(product);
    }

    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫣闁汇値鍣ｉ弻娑㈠焺閸愮偓鐣烽梺璇″灠閸婂潡骞冨Δ鍛嵍妞ゆ挾鍋樼划褏绱撻崒姘毙㈡い鎴濐樀瀵偄顓兼径濠囧敹濠电娀娼уΛ娆撳汲閵夆晜鈷掑〒姘搐閺嬫棃鏌涢弬娆惧剱缂佺粯鐩畷鍫曨敆娴ｉ晲缂撴俊鐐€ら崢鐣屾暜閻愮數鐭嗗鑸靛姈閻撴洘鎱ㄥΟ鐓庡付濞存粍绮庣槐鎺楀Ω瑜庨弳顒勬煛鐏炵喎瀚慨婊兾涙０浣藉厡缂佺姵宀稿铏规嫚閳ュ啿瀛ｉ梺鎼炲妿閺咁偄危?
     *
     * @param id 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粌娴风划娆愬緞鐎ｎ剛鐦堝┑顔斤供閸樿棄鈻?
     * @return 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粌绻橀崺鈧い鎺嗗亾婵犫偓闁秴纾绘俊顖濆亹缁€?
     */
    @Override
    public Product selectProductEntityById(Long id)
    {
        Product product = productMapper.selectProductById(id);
        return product;
    }

    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫣闁汇値鍣ｉ弻娑㈠焺閸愮偓鐣烽梺璇″灠閸婂潡寮诲☉銏犵闁瑰灝鍟悾浠嬫⒑?(闂備浇顕ф绋匡耿闁秴纾绘俊顖濆亹缁€?
     *
     * @param product 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇?
     * @return 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粍鏌ㄩ～蹇涘锤濡も偓楠炪垺淇婇妶鍕槮闁?
     */
    @Override
    public List<Product> selectProductEntityList(Product product)
    {
        if (product == null)
        {
            product = new Product();
        }
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId != null && !SecurityUtils.isAdmin(currentUserId))
        {
            List<Long> shopIds = remoteUserService.getShopIdsByUserId(currentUserId);
            if (CollUtil.isEmpty(shopIds))
            {
                return Collections.emptyList();
            }
            product.setShopIds(shopIds);
        }
        List<Product> productList = productMapper.selectProductList(product);
        // Previously querySeckill was called here, but now fields are merged.
        return productList;
    }


    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫣闁汇値鍣ｉ弻娑㈠焺閸愮偓鐣烽梺璇″灠閸婂潡寮诲☉銏犵闁瑰灝鍟悾浠嬫⒑闂堟稒顥滈柛鐔告尦瀵偄顓兼径濠冨祶缂佺偓澹嗘繛鈧柡?
     *
     * @param product 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粍鏌ㄩ锝嗙節濮橆儵銊︺亜椤撶喎鐏ユ繛鍫熷灴濮婃椽宕崟鍨㈤梺閫炲苯澧€殿喖鐖奸、?
     * @return 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜濇导鎰版⒒閸屾艾鈧螞濡も偓铻炴俊銈呮噹缁?
     */
    @Override
    public List<ProductVO> selectProductList(Product product)
    {
        List<Product> productList = selectProductEntityList(product);
        return convertToProductVOList(productList);
    }

    /**
     * 闂傚倷绀侀幖顐﹀磹閻熼偊鐔嗘慨妞诲亾鐠侯垶鏌涢幇闈涙灈闁汇値鍣ｉ弻娑㈠焺閸愮偓鐣烽梺?
     *
     * @param product 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇?
     * @return 缂傚倸鍊搁崐鐑芥倿閿曞倸绠板┑鐘崇閸?
     */
    @Override
    @Transactional
    public int insertProduct(Product product)
    {
        product.setCreateTime(DateUtils.getNowDate());

        // 婵犵數鍎戠徊钘壝洪敂鐐床闁稿瞼鍋為崑銈夋煏婵炵偓娅呴柣銈庡櫍閺屾盯鍩勯崘鐐暦闂?
        int i = productMapper.insertProduct(product);
        if(i > 0){
            // 闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹达絾纭舵い锔奸檮閵囧嫰骞樼€涙ê鈧劗鈧鍠曠划娆忕暦閵婏妇绡€闁告洘鍨崕鐢稿蓟?
            sendAuditMessage(product);
        }
        return i;
    }

    /**
     * 闂備礁婀遍崢褔鎮洪妸鈹库偓鍐╃節閸パ咁啈闂佹眹鍨婚…鍫ユ儗濡ゅ懏鐓欓悗娑欘焽缁犳ɑ銇勮箛鏃€灏﹂柡宀嬬節瀹曟帒鈽夊▎鎴濆殥闂備礁鎼幊宀勫垂閽樺鏆︽い鎰剁悼閻熺懓鈹戦悩鎻掝伀妞わ富鍣ｅ娲川婵犲嫭鍣梺鎼炲妼濞硷繝宕洪埀顒併亜閹存繄浠㈢紒銊х潔濠电姷鏁搁崑鐐哄垂閻㈠憡鍋嬪┑鐘插暙椤曢亶鏌涘☉娆愮稇闁告濞婇幃妤€鈽夊▍铏灴閹繝鍩€椤掑嫭鈷戞繛鑼帛婵炲洭鏌涢弬璺ㄐх€规洖鎼埥澶愬閻樻妲烽梻渚€娼ч…顓㈡嚈瑜版帒纾?
     *
     * @param product 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粌绻橀崺鈧い鎺嗗亾婵犫偓闁秴纾绘俊顖濆亹缁€?
     */
    public void sendNewProductMessageToMQ(Product product){
        String[] shopIds = product.getShopId().split(",");
        for(String shopId : shopIds){
            FeedEventMessage feedEventMessage= FeedEventMessage
                    .builder()
                    .feedType(FeedTypeEnum.SHOP_FEED.getCode())
                    .sourceType(GlobalBizTypeEnum.SHOP.getCode())
                    .sourceId(Long.valueOf(shopId))
                    .bizType(GlobalBizTypeEnum.PRODUCT.getCode()) // KEEP VOUCHER TYPE FOR NOW
                    .bizId(product.getId())
                    .publishTime(DateUtils.getNowDate())
                    .action(ItemActionType.NEW_ITEM.getCode())
                    .build();
            mqMessageSendUtils.sendMqMessage(
                    InteractionMqConstants.INTERACT_FEED_EXCHANGE,
                    InteractionMqConstants.INTERACT_FEED_ROUTING_KEY,
                    feedEventMessage);
        }
    }
    /**
     * 闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹达絾纭舵い锔肩畵閺岋繝宕ㄩ銏紙閻庤娲﹂崑鍛村箯閸涱垳鐭欓柛鐔峰暞椤ㄥ﹪寮婚悢纰辨晞閻犳亽鍔屾俊浠嬫⒑閻熻埇鍋㈤柡鈧柆宥呯闁告侗鍘愭惔銊ｂ偓渚€鏌ㄧ€ｎ剛顔曢梺鐟扮摠缁诲嫭鏅跺☉妯忓綊鎮℃惔銏犳畻闂佽桨绀佺粔鐟扮暦婵傜唯闁挎梻鍎甸崑鎾诲冀椤?闂傚倸鍊烽悞锕併亹閸愵亞鐭撻柣銏㈩焾閽冪喎鈹戦悩鎻掓殶闁崇粯鏌ㄩ埞鎴︽偐閹绘帗娈舵繛?闂傚倷绀侀幉锟犮€冮崨鏉戠柈闁秆勵殕閸庡秹鏌曡箛銉х？闁崇粯妫冮幃妤呮晲鎼粹€茬敖婵炲濮撮幗婊呮閹烘垟妲堟慨妤€妫欓鍡欑磽?
     *
     * @param productId      闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜滅划?
     * @param itemActionType 闂傚倷绀侀幉锟犲蓟閿濆绀夐悗锝庡墰缁€濠囨煛閸愩劌鈧崵浜搁悽鍛婂仯闁搞儺浜滈惃娲煃?
     */
    @Override
    public void sendProductActionMessageToMQ(Long productId, ItemActionType itemActionType){
        FeedEventMessage feedEventMessage= FeedEventMessage
                .builder()
                .feedType(FeedTypeEnum.ITEM_FEED.getCode())
                .sourceType(GlobalBizTypeEnum.PRODUCT.getCode()) // KEEP VOUCHER
                .sourceId(productId)
                .bizType(GlobalBizTypeEnum.PRODUCT.getCode()) // KEEP VOUCHER
                .bizId(productId)
                .publishTime(DateUtils.getNowDate())
                .action(itemActionType.getCode())
                .build();
        mqMessageSendUtils.sendMqMessage(
                InteractionMqConstants.INTERACT_FEED_EXCHANGE,
                InteractionMqConstants.INTERACT_FEED_ROUTING_KEY,
                feedEventMessage);
    }

    /**
     * 婵犵數鍎戠徊钘壝归崒鐐茬獥婵°倕鎳庨弸浣糕攽閸屾粠鐒鹃柣銈庡櫍閺屾盯鍩勯崘鐐暦闂?
     *
     * @param product 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇?
     * @return 缂傚倸鍊搁崐鐑芥倿閿曞倸绠板┑鐘崇閸?
     */
    @Override
    public int updateProduct(Product product)
    {
        product.setUpdateTime(DateUtils.getNowDate());
        int i = productMapper.updateProduct(product);
        if(i > 0){
            // 闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹达絾纭舵い锔奸檮閵囧嫰骞樼€涙ê鈧劗鈧鍠曠划娆忕暦閵婏妇绡€闁告洘鍨崕鐢稿蓟?
            sendAuditMessage(product);

            // 婵犵數鍎戠徊钘壝归崒鐐茬獥婵°倕鎳庨弸浣糕攽閸屾粠鐒鹃柣銈庡櫍閺屾盯鍩勯崘鐐暦闂佽鍨伴崐鍧楀蓟濞戞﹩娼╂い鎺嗗亾闁搞倛浜槐鎺撴綇閵娧呯杽濠殿喖锕ら妶绋跨暦閿濆棗绶炴俊顖氭惈椤ユ繈姊绘担渚劸缂佺粯鍨甸敃銏ゅ捶椤撶噥鍋ㄩ梺缁樺灱婵倝宕愰崸妤佸仯濡わ附瀵ч鐘绘煙閻ｅ苯鈻堥柡灞剧洴閺佸倿宕崟顐€辩紓鍌欑贰閻撳牓宕滃☉姘灊闁哄啫鐗嗙粻鎶芥煙閹屽殶缂佷線顥撶槐鎾诲磼濮橆兘鍋撹ぐ鎺戠闁归棿鐒﹂崑銈夋煏婵犲繐顩柍缁樻⒐閵囧嫰骞樼捄鍝勫闂佹悶鍊戦崐婵嬪蓟閵堝棭妲归幖鎼枟椤ユ挸顪冮妶搴′壕缂佺姵鎹囬悰顔芥償閵娿儺娼婇梺鎸庣箓閹冲繘鎮樻惔銊︹拺闁圭娴烽埥澶愭煛閸偄澧摶鐐烘煏閸繍妲哥紒鐙欏洦鐓冮柕澶堝劚缁狙勩亜閹邦亞鐭欓柡灞剧洴楠炴帡寮惔鎾寸€版繝鐢靛仜閹冲酣宕樼猾銉緄s闂備礁婀遍崢褔鎮洪妸鈺佺闁归棿鐒﹂崑?
            if(product.getActivityType() != null && product.getActivityType() == 1){
                redisService.deleteObject(RedisConstants.SECKILL_STOCK_KEY + product.getId());
                //闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樻彃顏柣顓熸崌閺岀喖鎮滃Ο璇查瀺缂備焦褰冮…鐑界嵁閺嶎偀鍋撳☉娅虫垹浜搁銏＄厽?
                redisService.setCacheObject(RedisConstants.SECKILL_STOCK_KEY + product.getId(), product.getStock());
            }

            // 闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閸屾艾鐩冮梻鍌欐祰濡椼劑鎳楅懜鍨珷婵°倐鍋撻柣?
            publish(new String[]{product.getId().toString()});
            clearProductCache(product.getId());
        }
        return i;
    }

    /**
     * 婵犵數鍎戠徊钘壝归崒鐐茬獥婵°倕鎳庨弸浣糕攽閸屾粠鐒鹃柣銈庡櫍閺屾盯鍩勯崘鐐暦闂佽鍨伴崐鍧楀蓟閿濆憘鐔煎垂椤旀儳甯块梻?
     *
     * @param product 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇?
     * @return 婵犵數鍎戠徊钘壝归崒鐐茬獥婵°倕鎳庨弸渚€鏌ゅù瀣珖缁炬儳銈搁弻鐔煎箚瑜滈崵鐔访?
     */
    @Override
    public Boolean changeStatus(Product product) {
        boolean b = updateById(product);
        if (b){
            // 闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閸屾艾鐩冮梻鍌欐祰濡椼劑鎳楅懜鍨珷婵°倐鍋撻柣?
            publish(new String[]{product.getId().toString()});
            clearProductCache(product.getId());
            if (product.getStatus() == 1){
                // 婵犵數鍋為崹鍫曞箰閹间焦鏅濋柕澶嗘櫆閸婂爼鏌ㄩ弴鐐测偓褰掑疾椤掑嫭鍊堕柣鎰煐椤ュ绱掗崣澶嬨仢婵﹥妞介、鏇㈠焺閸愵亞鍘竡濠电姷鏁搁崑鐐哄垂閻㈠憡鍋嬪┑鐘插暙椤曢亶鏌涘☉娆愮稇闁告濞婇幃妤€鈽夊▍铏灴閹繝鍩€椤掑嫭鈷戞繛鑼帛婵炲洭鏌涢弬璺ㄐх€规洖鎼埥澶愬閻樻妲烽梻渚€娼ч…顓㈡嚈瑜版帒纾?
                sendProductActionMessageToMQ(product.getId(), ItemActionType.RESHELF);
            }
        }
        return b;
    }

    /**
     * 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粍鏌ㄩ～蹇涙寠婢舵ê鎮戦梺鍛婃处閸樿偐绮?
     *
     * @param id 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜炲Λ?
     * @return 缂傚倸鍊搁崐鐑芥倿閿曞倸绠板┑鐘崇閸?
     */
    @Override
    public int priceReduced(Long id) {
        sendProductActionMessageToMQ(id, ItemActionType.PRICE_DROP);
        return 1;
    }

    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊呯矆閸℃稒鐓熸俊顖濆亹鐢盯鏌ｅ┑鍫濇灈闁哄本鐩幃銏ゆ煥鐎ｅ灚顥ｉ梻?
     *
     * @param ids 闂傚倸鍊搁崐绋棵洪悩璇茬；闁瑰墽绮崑锟犳煛閸ャ劍鐨戞い锔肩畵閺屾盯濡搁妷褏楔濠殿喖锕ｇ划娆愪繆閸洖鐐婇柕濞у嫭顔忛梻鍌欑閹芥粓宕版惔鈭ユ稑螖閸涱厾顔愰悷婊冩捣缁瑦寰勭€ｎ剛鐦堝┑顔斤供閸樿棄鈻?
     * @return 缂傚倸鍊搁崐鐑芥倿閿曞倸绠板┑鐘崇閸?
     */
    @Override
    public int deleteProductByIds(Long[] ids)
    {
        int i = productMapper.deleteProductByIds(ids);
        // 闂傚倷绀侀幉锛勬暜閻愬绠鹃柍褜鍓氱换娑㈠川椤撶姭鏀穝闂傚倷娴囧銊╂嚄閼稿灚娅犳俊銈傚亾闁?
        if (i > 0) {
            List<Long> productIds = Arrays.asList(ids);
            clearProductCacheBatch(productIds);
            List<String> seckillKeys = productIds.stream()
                    .filter(Objects::nonNull)
                    .map(id -> RedisConstants.SECKILL_STOCK_KEY + id)
                    .toList();
            if (CollUtil.isNotEmpty(seckillKeys)) {
                redisService.deleteObject(seckillKeys);
            }
            for (Long id : ids) {
                executorService.submit(()->{
                    log.info("Deleting product {} from search indexes on thread {}", id, Thread.currentThread().getName());
                    ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                    contentSyncMessage.setId(id);
                    contentSyncMessage.setIndexName(EsIndexNameConstants.PRODUCT_INDEX_NAME); // KEEP VOUCHER INDEX
                    contentSyncMessage.setType(GlobalBizTypeEnum.PRODUCT.getCode()); // KEEP VOUCHER
                    // 闂傚倷绀侀幉锟犳偡閿曞倸鍨傚┑鍌滎焾缁犲弶銇勯妶鍥╁綅bbitMq婵犵數鍎戠徊钘壝洪悩璇茬婵犻潧娲ら閬嶆煕濞戞瑦缍戠紒鈧崱娑欑厽婵☆垵鍋愮敮娑㈡煟濠靛牆澹媠闂傚倷娴囧銊╂嚄閼稿灚娅犳俊銈傚亾闁?
                    mqMessageSendUtils.sendMqMessage( SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                    // 闂傚倷绀侀幉锟犳偡閿曞倸鍨傚┑鍌滎焾缁犲弶銇勯妶鍥╁綅bbitmq婵犵數鍎戠徊钘壝洪悩璇茬婵犻潧娲ら閬嶆煕濞戞瑦缍戠紒鈧崱娑欑厽婵☆垵鍋愮敮娑㈡煟濠靛牆濡砳lvus闂傚倷娴囧銊╂嚄閼稿灚娅犳俊銈傚亾闁?
                    mqMessageSendUtils.sendMqMessage( SearchMqConstants.MILVUS_SYNC_EXCHANGE, SearchMqConstants.MILVUS_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                });
            }
        }
        return 1;
    }

    /**
     * 闂傚倷绀侀幉锛勬暜閻愬绠鹃柍褜鍓氱换娑㈠川椤撱垹寮伴悗瑙勬处閸ㄨ泛鐣烽崡鐐嶆梹鎷呴悷鏉垮婵犵數鍎戠徊钘壝洪悩璇茬婵犻潧娲ら?
     *
     * @param id 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粌娴风划娆愬緞鐎ｎ剛鐦堝┑顔斤供閸樿棄鈻?
     * @return 缂傚倸鍊搁崐鐑芥倿閿曞倸绠板┑鐘崇閸?
     */
    @Override
    public int deleteProductById(Long id)
    {
        int rows = productMapper.deleteProductById(id);
        if (rows > 0) {
            clearProductCache(id);
            redisService.deleteObject(RedisConstants.SECKILL_STOCK_KEY + id);
        }
        return rows;
    }


    /**
     * 闂備浇宕垫慨鐢稿礉閿曞倸鍌ㄦ繛宸簻缁狀垶姊洪鈧粔鎾偪椤曗偓閺屾盯鍩勯崘鐐暦闂佽鍨伴崐鍧楀蓟閵娿儮妲堟俊顖滃帶閳京绱撴笟鍥ф灓闁稿簺鍊楃划娆愬緞閹邦剛鍔﹀銈嗗笒鐎氼剛绮堥崱娑欑厱闁斥晛鍠氬▓鏃傜磼閸欏銇濇慨濠冩そ椤㈡洟鏁愰崱鈺傚仴缂傚倷绶氶·鍌炲磻婵犲洤鏋?
     * 
     * 濠电姷鏁搁崑鐔烘崲濠靛鍤勯柛顐ｆ礀閻撴繂鈹戦崒姘暈闁?
     * 1. 闂傚倷绀侀幖顐ょ矙閸曨厽宕叉繝闈涱儐閸嬫ɑ绻涢崱妯诲碍闁汇値鍣ｉ弻娑㈠焺閸愮偓鐣烽梺璇″灠閸婂潡骞冨Δ鈧埥澶娾枍椤撗傜盎闁挎洏鍨介、鏃堝醇濠靛牏鏆犻柣鐔哥矊椤戝宕洪埀?
     * 2. 闂傚倷绀侀幖顐ょ矓閻戞枻缍栧璺猴功閺?activityType (0婵犵數鍋為崹鍫曞箰妤ｅ啫纾块柕鍫濇噳閺嬪秵鎱ㄥ璇蹭壕闂? 1婵犵數鍋為崹鍫曞箲娴ｇ硶鏋嶉柨婵嗩檧缂嶆牗淇婇妶鍛櫣缂佺嫏鍥ㄧ厓? 婵?purchaseStrategyMap 闂傚倷绀侀幉锟犲蓟閿濆绀夌€广儱顦悞鍨亜閹寸偛顕滅紒浣哄椤ㄣ儵鎮欓懠顒傤啋閻庤娲╃紞浣割嚕娴犲惟闁靛鍨虹€垫牠姊洪懡銈呬沪缂佸鍨块幆澶嬬附閸涘﹤浠遍梺闈涱槴閺呮粓宕戦埡鍛厪濠㈣埖绋撳畝娑㈡煕?
     * 3. 缂傚倸鍊烽悞锔剧矙閹烘鍋嬫繝濠傜墕濮规煡鏌熼悧鍫熺凡缂佲偓閸愵喗鐓曟繛鎴炵懄缂嶆垿鏌涢弬璇插姦闁诡喖缍婇獮鍥敊閸忚偐浜為梻浣规た閸樹粙銆冮崱妤婂殫闁告洦鍨扮粈瀣亜閹捐泛鏋庨柍褜鍓涢崑銈夊蓟濞戞粎鐤€婵﹩鍙€閸氬倻绱撻崒姘毙㈡い鎴濐樀楠炲棝宕橀鑲╊槹濡炪倖鍔戦崐鏍儍閹达附鐓熼幖娣灮閳洟鎷戦崡鐑嗙唵鐟滃酣骞冮崒姘辨殾婵炲棙甯為悿鈧柣搴€ラ崘褍顥氬┑鐐存尰閸╁啴宕戦幘缈犵箚闁硅鍔栭鐘绘煙瀹勬壆绉烘い銏★耿閹倖鎷呴崗澶嬪瘲闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柛妤冨剱閸ゆ洖顪冪€ｎ亜顒㈠┑顖氥偢閺屾洝绠涢弴鐐愩垻绱掗埀顒傗偓锝庡枟閻撶喖鏌曡箛瀣仼闁哄鍨块弻娑㈠Ω閿曗偓閸濆搫鈹?
     *
     * @param productId 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜滅划?
     * @param userId    闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰銉︽叏閸︻厽瀚?
     * @return 闂傚倷绀侀幖顐︽偋閸愵喖纾婚柟鎯у绾捐棄霉閿濆懏鎲稿褎鏌ㄨ灃闁绘ê寮堕崰姗€鏌熼鑽ょ煓闁诡喓鍨介幃婊兾熼崗鍏碱啅闂備浇宕垫慨鎶芥⒔瀹ュ鍨傞柣鐔稿閺?ID
     */
    @Override
    @Transactional
    public Long purchaseProduct(Long productId, Long userId) {
        Product product = getById(productId);
        if (product == null) {
            throw new RuntimeException("product not found");
        }
        // 闂傚倷绀侀幖顐ょ矓閻戞枻缍栧璺猴功閺嗐倕霉閿濆牜鍤夐柧蹇氼潐鐎氭岸鏌熺紒妯洪唶婵°倕鍟扮壕鍏笺亜閹扳晛鍔撮柛銈嗙懇閺岋絾鎯旈敐鍛箣闂佸搫鑻幊蹇擃嚗閸曨厸鍋撻敐搴濇喚濞寸姴顕槐鎾存媴閸︻厸濮囬梺缁橆殕缁挸顕ｉ幎钘夌疀闁绘鐗婂▍?-闂傚倷绀侀幖顐﹀箯鐎ｎ喖闂柨婵嗩槸閻? 1-缂傚倸鍊风粈渚€藝閹殿喗鏆滄俊銈傚亾妞?
        String strategyName = "NormalPurchaseStrategy";
        if (product.getActivityType() != null && product.getActivityType() == 1) {
            strategyName = "SeckillPurchaseStrategy";
        }

        PurchaseStrategy strategy = purchaseStrategyMap.get(strategyName);
        if (strategy == null) {
             throw new RuntimeException("缂傚倸鍊风欢锟犲垂闂堟稓鏆﹂柣銏ゆ涧閸ㄦ繃绻涘顔荤凹闁绘挴鍋撻柣搴＄畭閸庡崬煤閵娾晛鍑犻柛宀€鍋為埛鎴︽煙缁嬫寧鎹ｉ柟鍐叉閵囧嫰寮村Ο琛″亾濠靛鏋佺€广儱鎷嬪鈺傘亜閹捐泛鏋庣紒鎲嬬畵濮婃椽鎮烽幍顔荤驳闂佺瀛╂繛濠傜暦閸濆嫮鏆嗛柍褜鍓熼崺鈧い鎺戝€归弳鈺呮煙閾忣偅灏甸柤娲憾瀵濡烽敃鈧崜顓㈡⒑閸涘﹥澶勯柛鐘崇墵瀵悂鎮╁ù瀣潔闂佸搫鍟犻崑鎾愁熆閻熺増顥㈤柟顔芥そ婵＄兘鍩￠崒姘偅?" + strategyName);
        }

        return strategy.purchase(userId, product);
    }

    /**
     * 闂傚倷绀侀幖顐ょ矓閻戞枻缍栧璺猴功閺嗐倕霉閿濆牜娼愰柛搴ｅ枑娣囧﹪濡堕崨顔兼闂佺娅曢悷鈺呭蓟閿涘嫪娌悹鍥ㄥ絻婵倕顪冮妶鍡樼妞ゃ劌锕ら悾鐑筋敍閻愭彃鑰垮┑掳鍊撻悞锕傚磿瀹€鍕拺闁告稑锕ょ粭姘亜閵娿儺妯€濠?
     *
     * @param product 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粍鏌ㄩ锝嗙節濮橆儵銊︺亜椤撶喎鐏ユ繛鍫熷灴濮婃椽宕崟鍨㈤梺閫炲苯澧€殿喖鐖奸、鏇熺鐎ｎ偆鍙嗗┑鐐村灦椤洦鏅堕弴銏″€甸柛顭戝亾閼拌法鈧娲忛崕鐢稿箖瀹曞洨鍗氶柟鐓庮洿Id闂傚倷绀侀幉锛勫垝瀹€鍕仼闁烩晝鑵恊gory闂?
     * @return 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜濇导鎰版⒒娴ｅ憡鍟為悽顖涱殜閵嗗啯寰勯幇顑?
     */
    @Override
    public List<ProductVO> queryProductOfShop(Product product) {
        List<Product> products = query()
                .apply("FIND_IN_SET({0}, shop_id)", product.getShopId())
                .eq("category", product.getCategory())
                .eq("status", ProductStatusEnum.ON_SHELF.getCode())
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .orderByAsc("create_time")
                .list();
        if (products.isEmpty()) {
            return null;
        }
        return convertToProductVOList(products);
    }


    /**
     * 闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹达絾纭舵い锔奸檮閵囧嫰骞樼€涙ê鈧劗鈧鍠曠划娆忕暦閵婏妇绡€闁告洘鍨崕鐢稿蓟閻旂⒈鏁囩憸宥夋倶閻樼粯鐓曢柍杞扮贰閸斿摌
     *
     * @param product 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粌绻橀崺鈧い鎺嗗亾婵犫偓闁秴纾绘俊顖濆亹缁€?
     */
    private void sendAuditMessage(Product product) {
        AuditMessage auditMessage = AuditMessage.builder()
                .bizId(product.getId())
                .bizType(GlobalBizTypeEnum.PRODUCT.getCode()) // KEEP VOUCHER
                .submitterId(Long.valueOf(product.getShopId().split(",")[0]))
                .auditContent(BeanUtil.beanToMap(product))
                .createTime(product.getCreateTime())
                .build();
        mqMessageSendUtils.sendMqMessage( AiAuditMqConstants.AUDIT_DIRECT_EXCHANGE,AiAuditMqConstants.AUDIT_ROUTING_KEY, auditMessage);
    }

    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫣缂佲偓閸岀偞鐓忓┑鐐靛亾濞呭懘鏌涢弬璇插姦闁哄本鐩幃銏ゆ煥鐎ｅ灚顥ｉ梻渚€鈧稑灏冮柛銉戝拋妲存繝寰锋澘鈧洜鈧哎鍔戦崺鈧い鎺嗗亾闁哥喐鎸冲顐㈩吋婢跺﹪鍞跺┑鐘绘涧濞层倝宕滈悽鍛婄厵闁绘挸娴烽幗鐘炽亜閵娿儻韬鐐搭殜瀹曞綊顢曢妶鍌涙暤闂備焦鏋奸弲娑㈠疮閳哄啯顫曢柡宥庡幗閻?
     *
     * @return 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粍鏌ㄩ悾宄邦潩椤戔晜妫冨畷鐔煎煘閹傚?
     */
    @Override
    public List<Product> listProduct( ) {
        return query().list();
    }

    /**
     * 闂備浇顕х换鎰崲閹版澘绠哄┑澶岀殹duct闂備浇顕ф绋匡耿闁秴纾绘俊顖濆亹缁€濠囨倵閿濆骸浜濆┑顔界矋閵囧嫰骞掑鍥舵М闁瑰吋娼欓敃銈夊煡婢舵劕绠婚柛鎾茬劍閳诲窎oductVO
     *
     * @param product 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粌绻橀崺鈧い鎺嗗亾婵犫偓闁秴纾绘俊顖濆亹缁€?
     * @return 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜濇导鎰版⒑鐠囧弶鎹ｉ柡浣规倐閵嗕焦绻濋崶銊ョ樁?
     */
    private ProductVO convertToProductVO(Product product) {
        if (product == null) {
            return null;
        }
        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(product, productVO);
        return productVO;
    }

    /**
     * 闂備浇顕х换鎰崲閹版澘绠哄┑澶岀殹duct闂傚倷绀侀幉锛勬暜濡ゅ懌鈧啯寰勯幇顑┿儵鏌涢幇銊︽珕濠殿喗绮嶉妵鍕箳瀹ュ浂妲柟鍏兼綑閿曘倝鍩ユ径鎰闁告挷鐒﹂埢宸杘ductVO闂傚倷绀侀幉锛勬暜濡ゅ懌鈧啯寰勯幇顑?
     *
     * @param productList 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粌绻橀崺鈧い鎺嗗亾婵犫偓闁秴纾绘俊顖濆亹缁€濠囨倵閿濆骸鏋涚紒鈧崱妯圭箚闁靛牆鎳庨銉╂煃?
     * @return 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜濇导鎰版⒒娴ｅ憡鍟為悽顖涱殜閵嗗啯寰勯幇顑?
     */
    private List<ProductVO> convertToProductVOList(List<Product> productList) {
        if (productList == null || productList.isEmpty()) {
            return new ArrayList<>();
        }
        List<ProductVO> voList = new ArrayList<>(productList.size());
        for (Product product : productList) {
            voList.add(convertToProductVO(product));
        }
        return voList;
    }

    /**
     * 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎闁汇値鍣ｉ弻娑㈠焺閸愮偓鐣烽梺璇″灠閸婂潡寮婚悢纰辨晢闁稿本绮岀粭锟犳⒑?
     *
     * @return 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粍妫冮悰顕€鍩€椤掑倻纾藉ù锝堝亗閹存績鏋?
     */
    @Override
    public Integer getProductTotal() {
        return query().eq("activity_type", 0).count().intValue();
    }

    /**
     * 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎闁汇値鍣ｉ弻娑㈠焺閸愮偓鐣烽梺璇″灠閸婂潡寮诲☉銏犵闁瑰灝鍟悾浠嬫⒑?
     *
     * @param sourceIdList 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜炲Λ娑㈡⒒娴ｅ憡鍟為悽顖涱殜閵嗗啯寰勯幇顑?
     * @return 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粍鏌ㄩ悾宄邦潩椤戔晜妫冨畷鐔煎煘閹傚?
     */
    @Override
    public List<Product> getProductListByIds(List<Long> sourceIdList) {
        List<Product> productList = redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_PRODUCT_KEY,
                RedisConstants.LOCK_PRODUCT_KEY,
                sourceIdList,
                Product.class,
                missingIds -> lambdaQuery()
                        .in(Product::getId, missingIds)
                        .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())
                        .eq(Product::getAuditStatus, AuditStatusEnum.PASS.getCode())
                        .list(),
                Product::getId,
                RedisConstants.CACHE_PRODUCT_TTL,
                TimeUnit.MINUTES
        );
        if (CollUtil.isEmpty(productList)) {
            return Collections.emptyList();
        }
        return productList.stream()
                .filter(product -> product != null
                        && Objects.equals(product.getStatus(), ProductStatusEnum.ON_SHELF.getCode())
                        && Objects.equals(product.getAuditStatus(), AuditStatusEnum.PASS.getCode()))
                .collect(Collectors.toList());
    }


    /**
     * 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎闁汇値鍣ｉ弻娑㈠焺閸愮偓鐣烽梺?
     *
     * @param id 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜炲Λ?
     * @return 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜濇导?
     */
    @Override
    public ProductVO getProductById(Long id) {
        Product product = cacheClient.queryWithLogicalExpireAndPassThrough(
                RedisConstants.CACHE_PRODUCT_KEY,
                RedisConstants.LOCK_PRODUCT_KEY,
                id,
                Product.class,
                productId -> query()
                        .eq("id", productId)
                        .eq("status", ProductStatusEnum.ON_SHELF.getCode())
                        .eq("audit_status", AuditStatusEnum.PASS.getCode())
                        .one(),
                RedisConstants.CACHE_PRODUCT_TTL,
                TimeUnit.MINUTES
        );
        if (product == null){
            return null;
        }
        if (!Objects.equals(product.getStatus(), ProductStatusEnum.ON_SHELF.getCode())
                || !Objects.equals(product.getAuditStatus(), AuditStatusEnum.PASS.getCode())) {
            return null;
        }
        ProductVO productVO = convertToProductVO(product);
        // 闂傚倷绀侀幉锛勬暜閸ヮ剙纾归柡宥庡幖閽冪喖鏌涢妷顔煎闁告瑥锕ラ妵鍕冀閵娧屾殹闂佺楠搁敃顏堝蓟閿熺姴鐒垫い鎺戝缁犳岸鏌ｅΔ鈧悧婊兾?
        StarDTO starDTO=new StarDTO();
        starDTO.setSourceType(GlobalBizTypeEnum.PRODUCT.getCode()); // KEEP
        starDTO.setSourceId(id);
        Boolean isStar = remoteStarService.isStar(starDTO);
        productVO.setIsStar(isStar);
        // 闂傚倷绀侀幉锛勬暜閸ヮ剙纾归柡宥庡幖閽冪喖鏌涢妷顔煎闁告瑥锕ラ妵鍕冀閵娧屾殹闂佺楠搁敃顏堝蓟濞戞粎鐤€婵﹩鍘捐ぐ褔姊?
        FollowDTO followDTO=new FollowDTO();
        followDTO.setSourceType(GlobalBizTypeEnum.PRODUCT.getCode()); // KEEP
        followDTO.setSourceId(id);
        Boolean isFollow = remoteFollowService.isFollowed(followDTO);
        productVO.setIsFollow(isFollow);
        return productVO;
    }

    /**
     * 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎闁稿﹦绮妵鍕箻閸楃偟浠鹃梺鎸庣☉閻倿寮诲☉銏″亜闂佸灝顑愬Λ鐐烘⒑闁稑灏冮柛銉戝拋鍞洪梻浣告贡閸庛倕煤閿曞倸缁╅柤鎭掑劜閸欏繘鏌熼悜妯诲暗闁绘粏宕电槐鎺撴綇閵娧勫櫚閻庢鍠撻崝鎴︺€佸☉妯锋婵☆垰鍚嬪暩闂傚倷绀侀幉锛勬暜閹烘嚦娑樷攽鐎ｎ€儱顭跨捄渚剳闁崇粯妫冮獮鏍垝閸忓浜剧€规洖娲犻崑鎾绘嚒閵堝洨锛滃銈嗗姧缁辨洘绋夐懠顒傜＜?
     * 
     * 闂備浇顕ф绋匡耿闁秴纾婚柣鏃囧亹瀹撲線鏌涢妷銏℃珖缁炬儳銈搁弻娑㈠焺閸愵厼顥濋柧浼欑節濮?
     * 1. 婵犵數鍋炲娆撳触鐎ｎ喗鏅梻浣告啞钃辩紒瀣笧缁?Redis ZSet ( hot_rank:product:xxx ) 婵犵數鍋為崹鍫曞箹閳哄懎鐭楅柍褜鍓氶〃銉╂倷閼碱剛顔掗悗瑙勬穿缂嶄礁顕ｉ崐鐕佹Щ闂佹悶鍊栫敮锟犲箖鐟欏嫮鐟瑰┑鐘插閿涚喖姊烘导娆戞偧缂侇噮鍨崇紓鎾寸鐎ｎ偄浠奸柣蹇曞仜婢т粙寮?ID 闂傚倷绀侀幉锛勬暜閹烘嚦娑樷攽鐎ｎ€儱顭跨捄渚剱闁稿海鍠栭悡顐﹀炊閵婏箑纰嶉梺璇茬箲閻熲晠寮?
     * 2. 闂?ZSet 闂傚倷绀侀幉锟犲垂閻撳海鏆﹂柣銏㈩焾閻撴ɑ绻涢幋鐐垫噮闁崇粯妫冮弻锟犲磼濞戞顦ㄧ紓浣插亾閻庯綆鈧厽鐩弫鎰板幢濞嗗繆鎷伴梻渚€娼уΛ妤吽囬柆宥呯闁绘绮悡銉╂倵閿濆骸澧柛鏇㈢畺濮婃椽骞愭惔锝傛闂佸吋妞块崹璺虹暦瑜版帩鏁嶆繛鎴炲嚬濞村嫰鏌熼崗鑲╂殬闁搞劌鎼埢鎾诲醇閺囩喓鍘甸柣鐘叉礌閳ь剝娅曢悘鍫㈢磽娴ｉ潧濮€濞存粠鍓熼獮蹇涙偐閸偄鐝伴梺鍦帛鐢偤骞?(闂傚倷绀佸﹢閬嶁€﹂崼銉嬪洭鎮界粙璺ㄥ摋闂侀潧绻堥崐鏍偂?闂傚倷绀侀幖顐﹀疮椤愶附鍋夐柣鎾冲?闂傚倷鐒︾€笛呯矙閹达附鍤愭い鏍仦閸ゆ劙鏌ｉ弬娆炬疇闁搞倖娲熼弻娑氫沪閸欍儳绻侀梺鍛婃煥鐎氫即骞冨畡鎵虫瀻婵炲棙鍨归弳鐘绘偡濠婂嫭绶查柛濠傛健瀵崵浠︾粵瀣倯闂佸憡渚楅崢钘夆枍?ZSet 婵犵數鍋涢顓熸叏妤ｅ喚鏁嬬憸搴ㄥ箞閵娾晜鍋勯柣鎾冲?
     * 3. 闂備礁鎼ˇ顐﹀疾濠婂牆钃熼柕濞垮剭濞差亜鍐€妞ゆ挾鍠庨崜?VO 婵犵數鍋為崹鍫曞箹閳哄懎鐭楅柛鎰电厛閻庤埖銇勯弮鍌氫壕闁?hotScore (闂傚倷鑳剁划顖炲春閸儱鍌ㄩ柤娴嬫杹閸嬫挾鎲撮崟顐熸灆閻?闂傚倷鐒︾€笛呯矙閹寸偟闄勯柡鍐ㄥ€婚惌姘舵煠閸濄儲鏆╂い鈺傜叀楠炴牜鍒掗崗澶婁壕闁归鐒﹂悗顐ょ磽閸屾瑧鍔嶆俊顐㈢箻瀹曡瀵肩€涙ê鍓甸悗骞垮劚濞层劑鎯岄崱娑欑厽闁瑰浼濋鍡欑當闁瑰墽绮悡鏇熸叏濮楀棗澧扮€涙繈鎮楀▓鍨灓闁稿繑锕㈤獮鍐煥閸偅鏅ｉ梺缁樺姇濡﹪宕ú顏呪拺缂佸灏呴崝鐔搞亜閹寸偞绀嬮柛鈹惧亾?
     *
     * @param current  闂佽崵鍠愮划搴㈡櫠濡ゅ懎绠伴柛娑橈攻濞呯娀鏌ｅΟ鐓庝缓濞?
     * @param size     婵犵绱曢崑鎴﹀磹濠靛棭鐒界憸鏃€淇婇悽绋跨倞闁宠　鍋撻柣?
     * @param category 缂傚倸鍊风粈渚€藝椤栨粎鐭撻柛鎾茬劍閸?(1:婵犵數鍋涢顓熷垔鐎靛摜绀婂〒姘ｅ亾鐎规洝娅曢妶锝夊礃閵娧屾Т? 2:闂傚倷鐒﹂幃鍫曞磿椤栫偛鍨傚┑鍌滎焾缁愭淇婇婵嗗惞闂傚嫬瀚穱濠囧Χ閸涱喖顎涘┑鈽嗗灙閸?
     * @return 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粍妫冮悰顕€骞掑Δ鈧粻鎶芥煛閸ャ儱濡介柡鍌楀亾濠电姵顔栭崰妤冩暜濡ゅ啫鍨濈€光偓閸曨偄鐎銈呯箰濡瑩宕?
     */
    @Override
    public List<ProductVO> getHotProductRank(Integer current, Integer size, Integer category) {
        int pageNo = current == null || current < 1 ? 1 : current;
        int pageSize = size == null || size < 1 ? 10 : size;

        // 1. 闂傚倸鍊搁崐鎼佸疮椤栨縿浜归柛鎰典簷閻掑﹤霉閻樺樊鍎忕紒鐘冲灩閹插憡鎯旈敐鍌氫壕婵ǜ鍎遍埀顒佺箞閻涱喚鈧綆鍣弫鍌炲箹鏉堝墽鎮奸柣娑栧劦濮婃椽宕崟顓烆暤闂佺顑嗛幐鎼佲€﹂崸妤佸殝濞达絽鍟ˉ婵嬫偡濠婂嫬惟闁搞儜鍜佸斀闂備礁缍婇崑濠冪閻愬瓨浜ら柡鍐ㄧ墛閻?100 闂傚倷绀侀幉锟犳嚌閸撗呯煋閻犻缚銆€閺嬫棃鏌熺€电孝闁搞劍绻傝灃闁挎繂鎳庨弳濠囨煕鐎ｎ偅灏扮€垫澘瀚埀顒婄到閻忔岸姊煎鍫熲拺?
        if (pageNo * pageSize > 100) {
            return Collections.emptyList();
        }

        // 2. 闂傚倷绀侀幖顐ょ矓閻戞枻缍栧璺猴功閺?category 闂傚倷绀侀幉锟犲礉閺囥垹鐤柛褎顨嗛崑鈺呮煕閹炬せ鍋撻柛瀣崌閹粙宕归銏＄暚缂傚倷闄嶉崝宥夋偂閿熺姴绠?Redis Key
        String hotRankKey = RedisConstants.PRODUCT_HOT_RANK_KEY;
        if (ProductEnum.VOUCHER.getCode().equals(category)) {
            hotRankKey += "voucher";
        } else if (ProductEnum.SET_MEAL.getCode().equals(category)) {
            hotRankKey += "deal";
        } else {
            return Collections.emptyList();
        }

        List<ProductVO> resultList;

        // 3. 婵犵數鍋涢顓熸叏鐎电硶鍋撳☉鎺撴珔闁靛棙甯″畷濂稿即閻愭惌妫熼梻浣筋潐椤旀牠宕板Δ鍛仼?ZSet 闂傚倷鑳剁划顖炲春閸儱鍌ㄩ柤娴嬫杹閸嬫挾鎲撮崟顐熸灆濡ょ姷鍋炵敮锟犵嵁閹烘鍗抽柕濠忛檮閺夊憡绻濆▓鍨灍閻㈩垱顨堥崚鎺楀醇閵夛箑娈橀梺纭呮彧闂勫嫰宕甸弮鍌楀亾閻熸澘顏繝銏☆焽瀵板﹪宕稿Δ浣哄幈闂佺粯妫冮弨閬嵥夊鍫熺厵?ID 闂傚倷绀侀幉锛勬暜閹烘嚦娑樷攽鐎ｎ€儱顭块懜闈涘闁哄绶氶弻锝呂旈埀顒勬偋閸℃瑧鐭?
        Page<Long> longPage = zSetIdManager.pageIds(hotRankKey, null, current, SystemConstants.MAX_PAGE_SIZE);
        List<Long> productIdList = longPage.getRecords();

        if (CollUtil.isEmpty(productIdList)&&current == 1) {
            // ZSet 闂傚倷绀侀幉锟犲垂閻撳海鏆﹂柣銏㈩焾閻撴ɑ绻涢幋娆忕仼缂佺媴缍侀弻鈥崇暤椤斿吋鍣烘い鏇燂耿濮婃椽宕崟顐ｆ婵犳鍠氶弫璇差嚕閹惰姤鍋勯柣鎾虫捣椤斿顪冮妶鍡橆梿濠殿喓鍊濊棟鐟滄棃寮婚敐澶娢╅柕澶堝労娴犲ジ姊洪崨濠庣劸妞ゎ偄顦甸獮鍡涘籍閸繂宓嗗銈呯箰濡盯鎮伴妷鈺傗拺闁告繂瀚～锕傛煕鎼淬劋鎲剧€规洜鏁婚、姗€濮€閻樻妲梻渚€娼ц墝闁哄懏绋戦埢宥夊Χ婢跺鍘告繛杈剧到閹碱偊銆傞懖鈹惧亾鐟欏嫭灏紒鑸佃壘閻ｅ嘲螣鐞涒剝鐎婚棅顐㈡处閹哥效?ZSet闂傚倷鐒︾€笛呯矙閹达附鍤愭い鏍仜閻ゎ噣鏌嶈閸撶喖寮婚悢鍏碱棃婵炴垵宕崜鐗堢節濞堝灝鏋撻柡鍛Т閻ｅ嘲顫濈捄鍝勮€垮┑鐐村灦閻楁垿宕?
            log.info("Product hot rank cache miss for key {}, loading from database", hotRankKey);
            List<Product> dbList = query()
                    .eq("category", category)
                    .eq("status", ProductStatusEnum.ON_SHELF.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .orderByDesc("sold")
                    .orderByDesc("create_time")
                    .list();

            if (CollUtil.isEmpty(dbList)) return Collections.emptyList();

            // 婵犵數鍋涢顓熸叏閹绢喖绠犻幖娣€戞禍褰掓煕閵夘喖澧紒鈧?100
            List<Product> finalDbList = dbList.stream().limit(100).collect(Collectors.toList());
            final String finalHotRankKey = hotRankKey;
            // 闂佽瀛╅鏍窗閹烘纾婚柟鍓х帛閻撴洘鎱ㄥΟ鐓庡付闁诲繒濮风槐鎺撴綇閵婏箑纰嶅銈庡亖閸ㄨ姤淇婇悿顖ｆЩ濠电偛鐗忔慨椋庢閹烘柡鍋撻敐搴濇喚闁稿骸绻橀弻娑㈡偄閸涘﹤纾抽悗?ZSet
            executorService.execute(() -> {
                try {
                    zSetIdManager.saveToZSet(finalHotRankKey, finalDbList, Product::getId, Product::getCreateTime);
                    // 婵犵绱曢崑鎴﹀磹濞戞﹩娴栭柕濞у懐鐓旈梺鍓插亝濞叉牜绮?ID 婵犵數濞€濞佳囁囬柆宥呯；婵炴垯鍨归惌妤呮煕閳╁啰鈯曢柣鎾冲€婚埀顒€绠嶉崕鍗灻洪妸鈺佹辈妞ゆ帒瀚悡鐔兼煙閹碱厼骞栨鐐寸墵閹娑甸崨顔惧涧缂備礁鍊圭敮鐐哄箯閻樿绠甸柟鐑樼箖閸栫娀姊婚崒姘偓鎼佸疮娴兼潙绐楅幖娣妼閸ㄥ倹銇勮箛鎾跺闁哄拋鍓涢埀顒€鍘滈崑鎾绘煃瑜滈崜鐔煎箖濞差亜绠ｆ繝鍨姇濞堫偊姊洪崨濠佺繁闁告鍘ч埢宥夋晸閻樺磭鍘搁梺鍛婁緱閸嬪嫭鎱ㄩ崒鐐寸厱闁挎繂妫欓崐鎰偓娈垮櫘閸撶喖鐛箛鎾舵殕濠电姴鍊归崐搴ｇ磽閸屾瑨鍏屽┑顔炬暬閵嗗啴宕卞☉杈ㄦ櫌濠电姴锕ら幊蹇涘汲濠婂牊鐓曟繝闈涘閸旀粓鏌ｉ敐澶夋喚闁哄本鐩獮鎺楀箣閻戝棙锛侀梻?
                    if (RedisConstants.PRODUCT_CALC_QUEUE_KEY != null) {
                        redisService.setCacheSet(RedisConstants.PRODUCT_CALC_QUEUE_KEY, finalDbList.stream().map(p -> String.valueOf(p.getId())).collect(Collectors.toSet()));
                    }
                } catch (Exception e) {
                    log.error("Failed to rebuild product hot rank zset", e);
                }
            });

            // 闂傚倷绀佺紞濠傤焽瑜旈、鏍川椤旇棄寮块梺鍐叉惈閸熺娀宕?dbList 闂備礁鎼ˇ顐﹀疾濠婂懐鐭欓柡宥庡幑閳ь兛绶氶獮瀣晝閳ь剛绮堥崱娑欑厱闁斥晛鍟伴埣銈夋煃?
            int start = (pageNo - 1) * pageSize;
            int end = Math.min(start + pageSize, finalDbList.size());
            if (start >= finalDbList.size()) return Collections.emptyList();

            List<Product> pageList = finalDbList.subList(start, end);
            resultList = convertToProductVOList(pageList);
            return resultList;
        }

        // 6. 闂傚倷绀侀幉锛勬暜閹烘嚦娑樷枎閹邦喚褰鹃梺鍦劋椤ㄥ懘鎷戦悢鍏肩厪濠㈣埖顭囬崢娑㈡煕閵堝懎顏柡灞稿墲閹峰懘宕崟鍨瘔闁诲氦顫夐幐鐑芥倿閿曞倸绠氶柡鍐ㄧ墕缁犲ジ鏌涢弴銊ュ箻妞わ富鍠栬灃闁绘﹢娼ф禒鈺呮煕閳哄倻澧遍柍?ZSet 闂傚倷鐒﹂惇褰掑礉瀹€鈧埀顒佸嚬閸ｏ絽鐣烽弴鐘冲枂闁告洦鍓欓崝鍛存⒑閹稿孩纾甸柛瀣崌閺屾盯濡搁妷鈺佸及閻庢鍣崑鍛偓鐢靛帶鑿愭い鎴ｆ娴滈箖骞栧ǎ顒€鐏柛搴ｅ枛閻擃偊宕堕妸锔炬缂備胶濮撮…鐑藉箖鐠鸿　妲堟俊顖滅帛閹烽亶鏌ｉ姀鈺佺仭闁圭顭烽獮鍡涘籍閸モ晝鏉稿┑鐐村灦钃辨い蹇曞枔缁?
        List<Product> products = new ArrayList<>();
        if (CollUtil.isNotEmpty(productIdList)) {
            List<Product> unsortedProducts = query()
                    .in("id", productIdList)
                    .eq("status", ProductStatusEnum.ON_SHELF.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .list();
            // 闂傚倷绀佸﹢閬嶁€﹂崼銉嬪洭鏌嗗鍛厬?pageIds 闂傚倷鐒﹂惇褰掑礉瀹€鈧埀顒佺煯閸楄櫕淇婄€涙ɑ鍎熼柕蹇娾偓鍐插Τ闂傚鍋勫ú锔剧矙閹存績鏋嶉柕蹇嬪灮绾?
            Map<Long, Product> productMap = unsortedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
            for (Long id : productIdList) {
                Product p = productMap.get(id);
                if (p != null) {
                    products.add(p);
                }
            }
        }

        // 7. 闂備礁鎼ˇ閬嶅磿閹版澘绀堟慨姗嗗墰閺嗭箓鏌涘▎蹇ｆШ闁?VO
        resultList = convertToProductVOList(products);
        return resultList;
    }

    @Override
    public List<ProductVO> searchProducts(String keyword) {
        String trimmedKeyword = keyword == null ? null : keyword.trim();
        List<Product> products = query()
                .select("id", "name", "status", "audit_status")
                .like(StrUtil.isNotBlank(trimmedKeyword), "name", trimmedKeyword)
                .eq("status", ProductStatusEnum.ON_SHELF.getCode())
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .list();
        return convertToProductVOList(products);
    }

    /**
     * 濠电姷鏁搁崕鎴犵礊閳ь剚銇勯弴鍡楀閸欏繘鏌ｉ幇顒傛憼闁稿海鍠栭弻鐔煎箚瑜忛敍宥夋煙?(闂傚倸鍊风欢锟犲磻閸涱収娼╅柕濞炬櫆閸?
     *
     * @param id 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜炲Λ?
     * @return 濠电姷鏁搁崕鎴犵礊閳ь剚銇勯弴鍡楀閸欏繘鏌ｉ幇顔芥毄缁炬儳銈搁弻鐔煎箚瑜滈崵鐔访?
     */
    @Override
    public int addStock(Long id) {
        log.info("Sending product restock feed event");
        // 闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹达絾纭堕柛鏂跨Ф缁辨帗寰勭仦钘夊箣濡ょ姷鍋樼欢姘躲€佸☉銏犖ч柛銉㈡櫓濡茬兘姊绘担鍛婃儓婵炲眰鍊濋幃娲Ω閳轰浇鎽曟繝鐢靛Т濞诧箓宕戦妸鈺傜厪濠电姴绻掗悾閬嶆煟閹惧瓨绀嬮柡灞剧洴瀵剛鎷犻幓鎺濈€撮梻?
        sendProductActionMessageToMQ(id, ItemActionType.RESTOCK);
        return 1;
    }

    /**
     * 闂傚倷鑳堕…鍫㈡崲閸儱绀夐柟杈剧畱绾惧潡鏌熺紒銏犳灈闁活厽顨婇弻鐔衡偓娑欘焽缁犳ɑ銇?
     *
     * @return 闂傚倷鑳堕…鍫㈡崲閸儱绀夐柟杈剧畱绾惧潡鏌熺紒銏犳灈闁活厽顨婇弻鐔衡偓娑欘焽缁犳ɑ銇勮箛鎿冨殶缂佽鲸鎸婚幏鍛喆閸曨偊鐎洪梻?
     */
    @Override
    public String allPublish() {
        int page = PageConstants.PAGE_NUMBER;
        int pageSize = PageConstants.ES_PAGE_SIZE;
        while (true) {
            // 闂傚倷绀侀幉锛勬暜閹烘嚦娑樷攽鐎ｎ€儱顭块懜闈涘闁藉啰鍠栭弻鏇熷緞濡厧甯ラ梺?
            List<Product> products = query()
                    .eq("status", ProductStatusEnum.ON_SHELF.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .page(new Page<>(page, pageSize))
                    .getRecords();
            if (products.isEmpty()) {
                break;
            }

            int finalPage = page;
            executorService.submit(()->{
                log.info("Publishing product page {} on thread {}", finalPage, Thread.currentThread().getName());
                // 闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹达絾纭堕柛鏂跨Т椤法鎲撮崟顐ｈ癁闂佸搫鑻惌浣虹不濞戙垹绫嶉柛灞藉€堕崕鐢稿蓟?
                sendProductBatchMessage(products);
                log.info("Published product page {}, size {}", finalPage, products.size());
            });
            page++;
        }
        return "publish success";
    }


    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊堟儗濡ゅ懏鐓欓悗娑欘焽缁犳ɑ銇勮箛鏃€灏﹂柡灞剧洴閹垽鏌ㄧ€ｅ灚顥ｉ梻渚€鈧稑灏冮柛銉ｅ妼濞堟粓姊哄ú璇插箻婵炶绠撳娲川婵犲嫮鐓€濡炪倖甯為崹濡塿us缂傚倸鍊峰鎺旂矚閸洖鍨傞柛锔诲幗椤?
     *
     * @param ids 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜滅划姘舵⒒娴ｈ姤銆冮柣鎺為檮缁旂喖宕卞▎蹇撶亰?
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
            log.info("Publishing products {} on thread {}", idList, Thread.currentThread().getName());
            // Batch query
            List<Product> products = query()
                    .in("id", idList)
                    .eq("status", ProductStatusEnum.ON_SHELF.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .list();
            if (CollUtil.isNotEmpty(products)) {
                // Batch send message
                sendProductBatchMessage(products);
            }
        });
        return "publish success";
    }

    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊堟儗濡ゅ懏鐓欓弶鍫熷礃閸氬倿鏌涚€ｎ偅灏扮紒瀣樀楠炲秹骞橀鐣屽幈濠电姴锕ら崰姘跺礉閸炲崹vus闂傚倷绀侀幉锟犳嚌妤ｅ啫瀚夋い鎺戝閺佸棝鏌ｉ幇顓犮偞婵℃彃鐗撻弻娑㈩敃閵堝懏鐎虹紓?
     *
     * @param products 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粍鏌ㄩ悾宄邦潩椤戔晜妫冨畷鐔煎煘閹傚?
     */
    private void sendProductBatchMessage(List<Product> products) {
        if (CollUtil.isEmpty(products)) {
            return;
        }
        ContentBatchSyncMessage request = new ContentBatchSyncMessage();
        request.setIndexName(EsIndexNameConstants.PRODUCT_INDEX_NAME);
        request.setData(products);
        request.setType(GlobalBizTypeEnum.PRODUCT.getCode());

        // 闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹达絽鍔甸柛鐘愁浉bbitmq濠电姷鏁搁崑鐐哄垂閻㈠憡鍋嬪┑鐘插暙椤曢亶鏌涘☉娆愮稇闁哄绶氶弻锝呂旈埀顒勬偋閸℃瑧鐭堥柨鏇炲€归悡娆戠磽娴ｅ顏嗙箔閹烘鐓曟繛鍡楃箲閺佹Ξ
        mqMessageSendUtils.sendMqMessage( SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_BATCH_INSERT_ROUTING_KEY, request);
        // 闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹达絽鍔甸柛鐘愁浉bbitmq濠电姷鏁搁崑鐐哄垂閻㈠憡鍋嬪┑鐘插暙椤曢亶鏌涘☉娆愮稇闁哄绶氶弻锝呂旈埀顒勬偋閸℃瑧鐭堥柨鏇炲€归悡娆戠磽娴ｅ顏嗙箔閹烘鐓曟繛鍡楃箲閹常lvus
        mqMessageSendUtils.sendMqMessage( SearchMqConstants.MILVUS_SYNC_EXCHANGE, SearchMqConstants.MILVUS_SYNC_BATCH_INSERT_ROUTING_KEY, request);
    }

    /**
     * 濠电姷鏁搁崑鐐哄箰閹间礁绠犻柟鐗堟緲閻撴﹢鏌″搴″箹缁绢厸鍋撻梻浣告惈濞诧箑鐣濋幖浣稿惞閻庯綆鍠楅悡鏇㈡煟閺冨牊鏁辨い銉︾矒閺岀喐鎷呴崘鍙夊櫧缁惧彞绮欓弻鐔煎箚瑜忛敍宥夋煙閻ｅ苯鈻堥柡灞诲妼閳藉螣閻撳簶鍙㈡俊鐐€栧ú姗€鎯勯鐐靛祦?+ 缂傚倸鍊风粈渚€藝閹殿喗鏆滄俊銈傚亾妞ゎ厼娲畷姗€鍩￠崒姘Τ闂備浇顫夐崕宕囧椤撱垹绠洪柣妯肩帛閻?
     *
     * @param productId 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜滅划?
     */
    private void clearProductCache(Long productId) {
        if (productId == null) {
            return;
        }
        redisService.deleteObject(RedisConstants.CACHE_PRODUCT_KEY + productId);
    }

    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７闂佸憡绻傜€氥劑鍩€椤掍礁娴€规洏鍔戦、娑樜旀担瑙勭彈闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粌绻掑Σ鎰板箻鐠囪尙顔掗悗瑙勬礀濞诧箓骞夋總鍛娾拺闁圭娴烽埥澶愭煟濡や礁濮嶆い銏¤壘閳藉濮€閻樼數鏆?+ 缂傚倸鍊风粈渚€藝閹殿喗鏆滄俊銈傚亾妞ゎ厼娲畷姗€鍩￠崒姘Τ闂備浇顫夐崕宕囧椤撱垹绠洪柣妯肩帛閻?
     *
     * @param productIds 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜滅划姘舵⒒閸屾艾鈧螞濡も偓铻炴俊銈呮噹缁?
     */
    private void clearProductCacheBatch(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return;
        }
        List<String> productKeys = productIds.stream()
                .filter(Objects::nonNull)
                .map(id -> RedisConstants.CACHE_PRODUCT_KEY + id)
                .toList();
        if (CollUtil.isNotEmpty(productKeys)) {
            redisService.deleteObject(productKeys);
        }
    }
    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊堝础閹惰姤鍊垫繛鎴烆伆閹达附鍋傞柍褜鍓熷娲传閵夈儲鐎诲┑鈽嗗亝椤ㄥ﹪鐛弽顒夋Ь闂佸綊顥撴繛鈧€规洜鍠栭、鏃堝幢濡ゅ啰鏆伴梻?
     *
     * @param updateMap 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜滅划姘攽閻愭潙鐏﹂柟鍝ヮ焾椤繈鏁冮崒娑樺墾闁瑰吋鐣崹濠氥€呴悜鑺モ拺闁割煈鍣崕鎰箾閸繄鍩ｉ柡宀嬬秮婵℃悂濡烽妷顔荤棯婵犵鍓濋崕褰掑川椤掑倸澧?
     * @return 闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樻彃顏痪鎯с偢閺岀喖骞嗚閸ょ喎霉?
     */
    @Override
    public Boolean updateReviewCountBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }
        if (updateMap.size() > 500) {
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                productMapper.updateReviewCountBatch(batchMap);
            }
        } else {
            productMapper.updateReviewCountBatch(updateMap);
        }
        clearProductCacheBatch(updateMap.keySet());
        return true;
    }
    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊堝础閹惰姤鍊垫繛鎴烆伆閹达附鍋傞柍褜鍓熷娲传閵夈儲鐎诲┑鈽嗗亝椤ㄥ﹪鐛弽顒夋▌閻庢鍠撻崝鎴︾嵁閸℃稒鍋嬮柛顐亝椤ユ棃姊?
     *
     * @param updateMap 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜滅划姘攽閻愭潙鐏﹂柟绋挎憸閳ь剚绋堥弲鐘差嚕椤愩儯浜归柟鐑樻尭閻у嫰妫呴銏″婵﹦鎳撻埢鎾诲醇閺囩喓鍘介梺闈涱焾閸庨亶顢旈鍛箚闁诡垼鍘奸幊宥夋倿?
     * @return 闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樻彃顏痪鎯с偢閺岀喖骞嗚閸ょ喎霉?
     */
    @Override
    public Boolean updateStarCountBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }
        if (updateMap.size() > 500) {
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                productMapper.updateStarCountBatch(batchMap);
            }
        } else {
            productMapper.updateStarCountBatch(updateMap);
        }
        clearProductCacheBatch(updateMap.keySet());
        return true;
    }

    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊堝础閹惰姤鍊垫繛鎴烆伆閹达附鍋傞柍褜鍓熷娲传閵夈儲鐎诲┑鈽嗗亝椤ㄥ﹪鐛弽顒夋Ь闂佸憡甯楃敮妤€顕ラ崟顒€绶炵€光偓婵犲倷鎮ｉ梻?
     *
     * @param updateMap 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜滅划姘攽閻愭潙鐏﹂柟鍝ヮ焾闇夐柣鎴ｅГ閸庢鏌涢妷锝呭闁崇粯姊圭换婵嬫濞戞瑥顦╁┑鐐茬墕閻栧ジ寮婚敐澶娢╅柕澶堝労娴犳儳鈹戦埄鍐ㄥ姲闁告侗鍓涢崜?
     * @return 闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樻彃顏痪鎯с偢閺岀喖骞嗚閸ょ喎霉?
     */
    @Override
    public Boolean updateFansCountBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }
        if (updateMap.size() > 500) {
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                productMapper.updateFansCountBatch(batchMap);
            }
        } else {
            productMapper.updateFansCountBatch(updateMap);
        }
        clearProductCacheBatch(updateMap.keySet());
        return true;
    }

    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊堝础閹惰姤鍊垫繛鎴烆伆閹达附鍋傞柍褜鍓熷缁樻媴閸濄儲鐨戦梺绋款儐閹瑰洤顫?
     *
     * @param updateMap 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜滅划姘攽閻愭潙鐏﹂柟鍛婃倐閺屽﹪鏁愭径濠勫摋闂侀潧绻堥崐鏍偂閸岀偟鍙撻柛銉ｅ妽缁€鍐煟閿濆鎲鹃柡宀嬬節瀹曠喖顢橀悙鎰╁灲閺?
     * @return 闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樻彃顏痪鎯с偢閺岀喖骞嗚閸ょ喎霉?
     */
    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊堝础閹惰姤鍊垫繛鎴烆伆閹达附鍋傞柍褜鍓熷缁樻媴閸濄儲鐨戦梺绋款儐閹瑰洤顫?
     *
     * @param updateMap 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜滅划姘攽閻愭潙鐏﹂柟鍛婃倐閺屽﹪鏁愭径濠勫摋闂侀潧绻堥崐鏍偂閸岀偟鍙撻柛銉ｅ妽缁€鍐煟閿濆鎲鹃柡宀嬬節瀹曠喖顢橀悙鎰╁灲閺?
     * @return 闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樻彃顏痪鎯с偢閺岀喖骞嗚閸ょ喎霉?
     */
    @Override
    public Boolean updateSoldBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }
        // 闂傚倷绀侀幉锛勬暜閹烘嚦娑樷枎閹邦喚褰鹃梺鍦劋閹稿摜娆㈤悙鐑樼厱闁哄洢鍔岄獮妤呮煕婵犲嫬浠遍柡灞诲妼閳藉螣閼测晛濮辨繝寰枫倕钄兼い鏇嗗浄缍?SQL 闂備浇宕垫慨鏉懨归崒鐐插偍闁肩鍩囨禍褰掓煟閹邦剛鎽犲ù婧垮€濋弻娑㈠Ψ椤旂厧顫┑?
        if (updateMap.size() > 500) {
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                productMapper.updateSoldBatch(batchMap);
            }
        } else {
            productMapper.updateSoldBatch(updateMap);
        }
        // 濠电姷鏁搁崑鐐哄箰閹间礁绠犳俊顖濄€€閺嬪秹骞栧ǎ顒€濡奸柣顓燁殕娣囧﹪濡堕崒姘濠电娀娼ч崐褰掓偋閻樿尙鏆︽い鎰╁焺閸氬鏌涘☉鍗炴灍闁绘繃绮撳娲川婵犲嫮鐣遍梺鐓庣枃濞夋盯鍩㈤幘璇茬闁绘鏁搁敍婵嬫⒑鐟欏嫬鍔ょ痪缁㈠弮楠?
        clearProductCacheBatch(updateMap.keySet());
        return true;
    }

    /**
     * 闂備浇顕у锕傦綖婢跺苯鏋堢€广儱鎷嬪〒濠氭煕鐏炲墽銆掓い鈺冨厴閹綊宕堕妸銉хシ濡炪値鍋侀崐婵嬪蓟閻旇櫣绠旀繛鎴炆戠粈宀勬煕鐎ｎ偅宕岀€殿喗濯界粻娑㈠即閻愭浼栭梻鍌欑劍鐎笛呯矙閹烘鍤屽Δ锝呭暙缁犳牠鏌熼悙顒€澧繛闂村嵆閺屾洘寰勯崼婵嗗濠电偛鎳庨惌鍌炲蓟濞戞ǚ鏀介柛娑卞灣閻涖垽姊虹悰鈥充壕闂侀潧绻堥崐鏍偂閸岀偟鍙撻柛銉ｅ妽閳锋劖绻涢崼婵堝煟闁哄瞼鍠撻幏鐘诲灳閾忣偆浜炵紓?Redis 闂傚倷绀侀幉锛勬暜閻愬瓨娅犳俊銈呮噹濮规煡鏌ｉ弮鍌氬付缂佺姴寮堕妵鍕籍閸パ傛睏濠?
     */
    @Override
    public void syncSalesData() {
        log.info("Starting product sales data sync");
        String countKeyPrefix = SalesTypeEnum.PRODUCT_SALES.getCountKeyPrefix();
        String dirtyKey = SalesTypeEnum.PRODUCT_SALES.getDirtyKey();
        String tempKey = dirtyKey + ":TEMP";

        // 闂傚倷绀佸﹢閬嶆偡閹惰棄骞㈤柍鍝勫€归弶鎼佹⒒娴ｅ憡鍟為柤褰掔畺瀵敻顢楅崟鍨櫌闂佺粯鍔楅崕銈夋倿婵犳碍鐓涢柛鏇ㄥ亞缁犳娊鎮介姘棦闁哄被鍔岄埥澶娢熼崹顕呬純闂備椒绱紞鍡涘礈閻旇偐宓侀悗锝庡枛缁犳稒銇勯幒宥囶槮闁逞屽墰閺佸寮婚敓鐘茬倞鐟滃繘骞楅悩缁樼厸閻忕偞鏋婚煬顒勬煙椤旂晫鐭掓い銏★耿閹瑥顔忛鍏兼毌闂備浇宕垫慨宕囨閵堝洦顫曢柡鍥ュ灪閸嬧晛鈹戦悩宕囶暡闁抽攱鎹囬弻锝夊棘閸喗些闂?
        redisService.syncDataWithSnapshot("product sales", countKeyPrefix, dirtyKey, tempKey,
                this::updateSoldBatch,
                updateMap -> enqueueIds(RedisConstants.PRODUCT_CALC_QUEUE_KEY, updateMap.keySet()));
        log.info("Product sales data sync finished");
    }



    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７闂佹儳绻橀埀顒佺〒閸?ID 闂備浇顕х€涒晝绮欓幒妤佹櫔闂?Redis Set 闂傚倸鍊搁崐绋课涘Δ鈧灋婵°倕鎳庣粻鏍箹濞ｎ剙鐏柍缁樻⒐閵囧嫰骞橀崡鐐差瀷缂備讲鍋撻柛鏇ㄥ亖閳ь剚甯掗～婵嬵敇閻橆喒鍋撻幒妤佺厱閹兼番鍊ゅ鎰版煙椤栨稒顥堥柡浣稿暞缁楃喖宕惰閸婂海绱撻崒娆掑厡濠殿喚鏁婚妴鍐╃節閸嬫枻缍侀弫鍐磼濮橀硸妲存繝纰夌磿閸嬬偤宕曢幎钘夌厺闊洦绋掗悡鐔兼煏韫囧﹥娅囬柛妯荤矌缁?
     */
    private void enqueueIds(String key, Collection<Long> ids) {
        if (key == null || CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            if (id != null) {
                redisService.setCacheSet(key, id.toString());
            }
        }
    }

    /**
     * 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎闁汇値鍣ｉ弻娑㈠焺閸愮偓鐣烽梺璇″灠閸婂潡寮婚敓鐘茬劦妞ゆ帒瀚粻姘舵煟濡も偓閻楁粌螞韫囨稒鈷?
     *
     * @param sourceId 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜滅划?
     * @return 闂傚倷娴囬妴鈧柛瀣崌閺岀喖顢涘鍐炬毉濡炪們鍎查崹鍧楀蓟閳╁啯濯寸€瑰嫭婢樼粊顕€姊?
     */
    @Override
    public Integer getProductStarCount(Long sourceId) {
        Product product = lambdaQuery()
                .select(Product::getStars)
                .eq(Product::getId, sourceId)
                .one();
        return product != null ? product.getStars() : 0;
    }
    /**
     * 闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樺弶鎼愰柣銈庡櫍閺屾盯鍩勯崘鐐暦闂佽鍨伴崐鍧楀蓟閿濆憘鐔煎垂椤旀儳甯块梻渚€鈧偛鑻崢鍝ョ磼闊彃鈧危閹邦兘鏋庨柟瀛樻煥娴滈箖鏌涜箛鎿冩Ц濠⒀勭叀閺岋絽螖娴ｇ硶鏋欓梺鍝勮嫰閹冲酣锝炲┑鍡欐殾闁搞儮鏅濋弳?闂傚倷绀佺紞濠囧绩鏉堚晜鏆滈柣鎰版涧閸ㄦ繈鏌ｉ幋锝呅撻柡?
     *
     * @param id     闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜滅划?
     * @param status 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇閻熸粍妫冮獮鍐煛閸涱喖娈濈紒鍓у閿氬ù?
     * @return 闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樻彃顏痪鎯с偢閺岀喖骞嗚閸ょ喎霉?
     */
    @Override
    public Boolean updateProductStatus(Long id, Integer status, String reason) {
        Product product = getById(id);
        if (product == null) return false;

        Integer finalStatus = product.getStatus() == null ? ProductStatusEnum.OFF_SHELF.getCode() : product.getStatus();

        // 婵犵數濮烽。浠嬪焵椤掆偓閸熷潡鍩€椤掆偓缂嶅﹪骞冨Ο璇茬窞閻忕偟顭堟禍楣冩煕韫囨搩妲稿褎鐓￠弻锝呂旀担绯曟灆闂佸搫鑻幊搴綖濠靛棛鏆﹂柛銉㈡櫇閺?
        if (Objects.equals(status, AuditStatusEnum.PASS.getCode())) {
            finalStatus = ProductStatusEnum.ON_SHELF.getCode();
            // 婵犵數濮烽。浠嬪焵椤掆偓閸熷潡鍩€椤掆偓缂嶅﹪骞冨Ο璇茬窞闁归偊鍓欏宄邦渻閵堝棛澧紒顔惧Т椤洭鏁撻悩宕囧幐婵犮垼娉涘Λ妤佺妤ｅ啯鈷戦柛婵勫劚閺嬪孩淇婇锝庢疁妤?
            if (product.getActivityType() != null && product.getActivityType() == 1) {
                Date now = DateUtils.getNowDate();
                // 闂傚倷绀侀幉锛勬暜閸ヮ剙纾归柡宥庡幖閽冪喖鏌涢妷顔煎闁告瑥锕ラ妵鍕冀閵娧屾殹闂佺楠搁敃顏堝蓟閿熺姴閱囨い鎰剁磿閻╁酣姊虹化鏇熸珔闁兼椿鍨堕、姘舵晲婢跺﹦鍔﹀銈嗗笂閻掞箓宕ｈ箛娑欏€甸柨婵嗛娴滄繃銇勮熁閸ャ劉鎷?
                if (product.getBeginTime() != null && product.getBeginTime().getTime() > now.getTime()) {
                    finalStatus = ProductStatusEnum.OFF_SHELF.getCode(); // 闂傚倷绀侀幖顐︽偋濠婂嫮顩叉繛鍡樺灩缁犳棃鏌涘畝鈧崑娑㈠礄?
                }

                // 闂傚倷绀佸﹢閬嶁€﹂崼銉嬪洭鎮介幖鐐╁亾閸岀偛鐓涢柛娑卞枛閳ь剛绮妵鍕箻鐠虹洅銉р偓瑙勬礀閻栧ジ寮诲☉妯锋瀻婵炲棙鍨甸崺宀勬⒑閹肩偛濡奸柣蹇斿哺楠炲牓濡歌閸嬫捇鏁愭惔婵堢泿濡炪倖娲﹀﹢姊昫is闂備礁婀遍崢褔鎮洪妸鈺佺闁归棿鐒﹂崑?
                long preHeatTime = now.getTime() + (RedisConstants.SECKILL_PRE_HEAT_WINDOW_HOURS * 60 * 60 * 1000);
                if (product.getBeginTime() == null || product.getBeginTime().getTime() <= preHeatTime) {
                    redisService.setCacheObject(RedisConstants.SECKILL_STOCK_KEY + product.getId(), product.getStock());
                }
            }
            // 闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹达絾纭堕柛鏂跨Ф缁辨帗寰勭仦钘夊箣濡ょ姷鍋樼欢姘躲€佸☉妯锋婵☆垰鐏濋幃鍛存⒒閸屾瑧璐伴柛瀣€荤划鍫熸媴鐟欏嫬寮块梺鍐叉惈閹冲海绮?
            sendNewProductMessageToMQ(product);
        } else if (AuditStatusEnum.isRejected(status)) {
            // 闂備浇顕ф鎼佸储濠婂牆绀堟繝闈涱儐閸嬪鏌ц箛锝呬簴濞寸姵宀搁弻娑㈠箛椤掆偓缁狙呯磼閳ь剟鍩€椤掑嫭鈷戦柛娑橈龚婢规绱掗妸銊ヤ汗闁瑰箍鍨归～婵堟崉閾忚鍞跺┑鐐舵彧缂嶄線藟閹剧粯鍋＄憸鐗堝笚閻撴洟鏌￠崘鈺傚暗缁炬儳鐏濋埞鎴︻敊閼恒儱鈧劖顨ラ悙鈺佷壕濠电偠鎻徊鍧楀蓟婢跺瞼鐭嗛悗锝庝簴濡插牓鏌熼悙顒€澧柛搴㈠姍閺岋綀绠涢妷褏鏆ら梺杞扮缁夋挳鎮惧┑瀣妞ゅ繐妫涚粈鍡涙⒒娴ｄ警鐒剧紒缁樺姉缁柨鐣烽崶鈺冪暥闁诲繒鍋熼崑鎾寸閵堝鐓曢柕澶涚到婵′粙鏌涢埡浣糕偓鍧楀蓟閿濆憘鐔煎垂椤旀儳甯块梻渚€鈧偛鑻崢鎼佹煟閹虹偛顩柟宄扮秺閸┾剝瀵?
            finalStatus = ProductStatusEnum.OFF_SHELF.getCode();
        }

        // 闂傚倷绀佺紞濠囧绩鏉堚晜鏆滈柣鎰版涧閸ㄦ繈鏌ｉ幋锝嗩棄缂侇偄绉归幃褰掑传閸曨剚鍎撻梺鍝勬噺閹倿寮诲☉婊呯杸閻庯綆浜滄慨鏇㈡煟鎼淬垼澹樻俊顐ｇ〒濡叉劙骞掗幋鏃€鏂€闂佺硶鍓濋〃鍡涘几妤ｅ啯鈷戦柟鑲╁仜閸斻倕鈹戦姘煎殶婵″弶鍔曢埞鎴犫偓锝庡亞閸旈潧鈹戦鐭亞澹曢鐘典笉闁哄稁鍘介悡娑㈡煕閹伴潧骞栭柣鎾村姉缁辨帡寮崶褍鎯炵紓渚囧枟瀹€鎼佸箖濠婂吘鐔兼惞閻熸壆绋荤紓鍌氬€搁崐鐑芥倿閿旂偓宕查柛灞剧矋閺嗘粍銇勯幇鍓佺暠闁?
        String rejectReason = AuditStatusEnum.isRejected(status) ? reason : null;
        boolean b = update(new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Product>()
                .set("status", finalStatus)
                .set("audit_status", status)
                .set("reject_reason", rejectReason)
                .eq("id", id));
        if(b){
            clearProductCache(id);
            // 婵犵數濮烽。浠嬪焵椤掆偓閸熷潡鍩€椤掆偓缂嶅﹪骞冨Ο璇茬窞閻忕偟顭堟禍楣冩煕韫囨搩妲稿褎鐓￠弻锝呂旀担绯曟灆闂佸搫鑻幊搴綖濠靛棛鏆﹂柛銉㈡櫇閺嗩偊姊绘担鐟邦嚋缂佸鍨块幊鐔碱敍閻愭彃鐎銈嗙墬缁牓鎮炴繝鍥ㄧ厱闁斥晛鍠氬▓鏃傗偓鐢靛閹瑰洭寮诲☉銏犵睄闁稿本顕撮姀銈嗙厱婵炲棗绻戦幉顩媎is闂傚倷鑳剁划顖炲春閸儱鍌ㄥù鐘差儌閳ь剚姊圭粭鐔煎焵椤掆偓閻ｇ兘顢旈崟顓熸畷闂佸憡鍔栭崕鎶藉箖娴ｈ櫣纾藉ù锝呮惈鏍″銈冨妼閿曘倕宓勯梺鎸庢礀閸婂摜绮?
            if (Objects.equals(status, AuditStatusEnum.PASS.getCode())) {
                publish(new String[]{id.toString()});
                String rankKeySuffix = (product.getCategory() != null && product.getCategory() == 1) ? "voucher" : "deal";
                String hotRankKey = RedisConstants.PRODUCT_HOT_RANK_KEY + rankKeySuffix;
                double score = product.getCreateTime() != null ? (double) product.getCreateTime().getTime() : (double) System.currentTimeMillis();
                redisService.setCacheZSet(hotRankKey, id.toString(), score);
                redisService.setCacheSet(RedisConstants.PRODUCT_CALC_QUEUE_KEY, id.toString());
            } else {
                ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                contentSyncMessage.setId(id);
                contentSyncMessage.setIndexName(EsIndexNameConstants.PRODUCT_INDEX_NAME);
                contentSyncMessage.setType(GlobalBizTypeEnum.PRODUCT.getCode());
                mqMessageSendUtils.sendMqMessage(SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                mqMessageSendUtils.sendMqMessage(SearchMqConstants.MILVUS_SYNC_EXCHANGE, SearchMqConstants.MILVUS_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
            }
        }
        return b;
    }
    /**
     * 闂傚倷绀佸﹢閬嶃€傞鎯х筏濞寸姴顑呴梻顖炴煟閹寸倖鎴﹀磿閻斿吋鐓欓柟顖嗗拑绱為梺鍦櫕婵炩偓闁哄被鍔岄埥澶娢熼悡搴毇缂傚倷闄嶉崝蹇旂椤掑嫬鐒?1闂傚倷鐒︾€笛呯矙閹寸偟闄勯柡鍐ㄥ€搁崹婵嬪箹濞ｎ剙鐏柛搴ｅ枛閺岀喖骞嗚閿涘秹鏌?0闂傚倷绀侀幖顐﹀疮閹惰棄鏄ラ柡宥庡幖閸ㄥ倹鎱ㄥΟ鎸庣【缂佲偓婢舵劖鐓熼柡鍥╁仜閳ь剝宕电划?
     *
     * @param id 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜滅划?
     * @return 闂傚倷绀佸﹢閬嶃€傞鎯х筏濞寸姴顑呴梻顖炴煣韫囨挻璐＄痪鎯с偢閺岀喖骞嗚閸ょ喎霉?
     */
    @Override
    public boolean deductStock(Long id) {
        // Simple update: stock = stock - 1 where id = id and stock > 0
        return update().setSql("stock = stock - 1").eq("id", id).gt("stock", 0).update();
    }
    /**
     * 闂傚倷娴囬鏍储瑜版帒鍨傜憸鐗堝吹閸ヮ剙鐭楀璺侯儏閸斿懘姊虹憴鍕姢缁剧虎鍙冮獮妤呮偐缂佹鍙嗗┑鐐村灦椤洦鏅堕弴鐘电＜闁靛鍔嬮崥顐︽煃?1闂傚倷鐒︾€笛呯矙閹达附鍎斿┑鍌滎焾閺嬩線鏌曢崼婵囶棤妞も晜鐓￠獮鏍庨鈧埀顑惧€濆畷銉╁磼閻愬鍘卞┑掳鍊撻悞锔剧矆閳ь剟鎮峰鍕凡婵炲眰鍊濋崺?闂備胶鍎甸崜婵堟暜閹烘绠犻煫鍥ㄦ惄濞撳鏌涘畝鈧崑娑㈡倷婵犲洦鐓熼柟閭﹀墻閸ょ喐绻涙径濠忚含闁?
     *
     * @param id 闂傚倷绀侀幗婊堝窗鎼粹垾娑樜旈崨顓狀啇濡炪値浜滅划?
     * @return 闂傚倷娴囬鏍储瑜版帒鍨傜憸鐗堝吹閸ヮ剚鍤戞い鎺戝€婚敍婊堟⒑鐟欏嫬鍔ゆい鏇ㄥ幗缁?
     */
    @Override
    public boolean recoverStock(Long id, Long userId) {
        recoverRedisStockAndEligibility(id, userId);
        // Simple update: stock = stock + 1 where id = id
        return update().setSql("stock = stock + 1").eq("id", id).update();
    }

    @Override
    public boolean recoverRedisStockAndEligibility(Long productId, Long userId) {
        if (productId == null) return false;
        
        // 1. 闂傚倷娴囬鏍储瑜版帒鍨傜憸鐗堝吹?Redis 婵犵妲呴崑鍛熆濡皷鍋撳鐓庡箺闁哄懎鐖煎畷銊︾節閸愩劌濡抽梻浣筋潐閸庡磭澹曢銏犵?
        String stockKey = "seckill:stock:" + productId;
        redisService.incrementCacheValue(stockKey, 1);
        
        // 2. 缂傚倸鍊风粈渚€藝椤栫偐鈧箑鐣￠幍铏€洪柟鍏肩暘閸斿秹宕戦妸鈺傜厪濠电姴绻掗悾閬嶆煟閹惧瓨绀嬫慨濠冩そ楠炴捇骞掗弬婵勫灪閵囧嫰鏁冮埀顒€顕ｉ崜浣虹煓濠㈣泛澶囬崑鎾绘晲鎼存繄鐩庣紓浣插亾閻庯綆鍠楅埛鎺楁煕閺囥劌澧┑顔兼喘閺屾盯鍩￠崒銈嗙暭闂佺懓鍢查澶婎嚕閸撲焦宕夐柕濠庣仢?(婵犵數濮烽。浠嬪焵椤掆偓閸熷潡鍩€椤掆偓缂嶅﹪骞冨Ο璇茬窞闁归偊鍓涢宀勬⒑瑜版帒浜板ù婊呭仦濞煎寮Λ?userId)
        if (userId != null) {
            String orderKey = "seckill:order:" + productId;
            Long l = stringRedisTemplate.opsForSet().remove(orderKey, userId.toString());
            log.info("闂佽姘﹂～澶愭偤閺囩姳鐒婃繛鍡楁捣閺勫倿姊婚崒姘偓鍝モ偓姘ュ姂瀹曟劕螖閸涱厽鐎梺闈涚墕椤︻垳绮婚敐澶嬬叄婵﹩鍓欓埀顒€顭烽幃鐑藉箻缂佹鍘卞┑掳鍊撻悞锔剧矆鐎ｎ偂绻嗛柣鎰靛墯閵囨繄鈧? productId={}, userId={}, result={}", productId, userId, l);
        }
        
        log.info("闂佽娴烽幊鎾诲箟闄囬妵鎰板礃椤妞藉鍊燁檨闁?Redis 缂傚倸鍊风粈渚€藝閹殿喗鏆滄俊銈傚亾妞ゎ厼娲畷姗€顢欓崗鑲┾偓顒勬⒑缂佹﹩娈旈柣妤€妫涚划? productId={}, userId={}", productId, userId);
        return true;
    }

}
