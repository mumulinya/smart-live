package com.smartLive.shop.service.impl;
import com.smartLive.common.core.constant.mq.SearchMqConstants;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;

import com.smartLive.common.core.enums.product.SalesTypeEnum;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.context.SecurityContextHolder;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.AppLoginUser;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.rabbitmq.domain.ContentSyncMessage;
import com.smartLive.common.core.enums.common.AuditStatusEnum;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.utils.StringUtils;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.RedisMultiCacheManager;
import com.smartLive.interaction.api.RemoteFollowService;
import com.smartLive.interaction.api.RemoteStarService;
import com.smartLive.interaction.api.DTO.BadReviewDTO;
import com.smartLive.interaction.api.DTO.ShopReviewAnalysisDTO;
import com.smartLive.interaction.api.DTO.ShopReviewSuggestDTO;
import com.smartLive.interaction.api.RemoteReviewService;
import com.smartLive.order.api.DTO.ProductSalesDTO;
import com.smartLive.order.api.DTO.ShopOrderAnalysisDTO;
import com.smartLive.order.api.DTO.ShopOrderSuggestDTO;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.system.api.RemoteUserService;
import com.smartLive.interaction.api.DTO.FollowDTO;
import com.smartLive.interaction.api.DTO.StarDTO;
import com.smartLive.shop.domain.ShopType;
import com.smartLive.shop.domain.VO.BadReviewVO;
import com.smartLive.shop.domain.VO.ProductSalesVO;
import com.smartLive.shop.domain.VO.ShopAnalysisVO;
import com.smartLive.shop.domain.VO.ShopSuggestVO;
import com.smartLive.shop.domain.VO.ShopVO;
import com.smartLive.shop.service.IShopTypeService;
import com.smartLive.common.redis.util.CacheClient;
import com.smartLive.common.redis.util.ZSetIdManager;
import org.apache.lucene.util.SloppyMath;
import org.springframework.beans.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartLive.shop.mapper.ShopMapper;
import com.smartLive.shop.domain.Shop;
import com.smartLive.shop.service.IShopService;
import jakarta.annotation.Resource;

/**
 * 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ラ梻鍌欑婢瑰﹪鎮￠崼銉ョ；闁糕剝绋戦悡婵嬫煛閸愶絽浜鹃梺閫涚┒閸斿矂锝炲鍫濆耿婵°倐鍋撴い顐熸櫅椤啴濡舵惔鈥茬按闂佺锕ョ换鍫ョ嵁閸愩剮鏃堝川椤撶喎绁舵俊鐐€栭幐楣冨磻濮椻偓楠炲鐣￠幏鏃€妫冮幃鈺呮濞戞鎹曟繝鐢靛仜閻即宕濋幋锕€绠?
 * 
 * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢妶鍥╃厠闂佸壊鍋呭ú宥夊焵椤掑﹦鐣电€规洖銈告慨鈧柕蹇嬪灩椤︹晛鈹戞幊閸娧呭緤娴犲鐤い鏍仜閸氬綊鏌涚仦鎯ь棜闁稿鎹囧畷妤佸緞婵犱礁顥氶梻鍌欑閹诧繝宕濋幋锕€绀夌€光偓閸曨厼绁﹂柣搴秵閸犳鍩?
 * 1. 缂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕闂佽鍠楀鑺ヤ繆閹间焦鏅滈柦妯侯槸娴煎酣姊绘笟鈧褏鎹㈤崱娑樼婵犻潧顑呯壕鍧楁煟閺冨牊娅滅紒璇叉閺屾洟宕煎┑鍥ф闂侀潻绲惧浠嬪蓟閻斿吋鎯炴い鎰╁€栭崳浼存⒑閸濆嫭婀伴柣鈺婂灦瀹曟椽鏁撻悩鑼槹濡炪倖甯掗ˇ顕€宕Δ浣风箚闁绘劦浜滈埀顒佸灴瀹曠銇愰幒鎴犵枃闂婎偄娲﹀ú婊堟儗濮樿埖鈷戞い鎺嗗亾缂佸鎸抽幃鐐裁洪鍛幈闂佽婢橀懟顖炲箠閹板叓鍥Ψ閳哄倵鎷绘繛杈剧悼閹虫捇顢氬鍛＜閻庯綆鍋勯悘鎾煙瀹曞洤鏋涙い銏＄洴閹瑩寮堕幋鐑嗗悈闂傚倷鑳剁划顖氼潖婵犳艾鍌ㄧ憸蹇涘箟閹绢喗鏅濋柛灞剧☉閳ь剙鐖奸弻锝夊箛椤忓棛銈峰┑锛勫亾閹倿寮诲☉銏犳闁割煈鍣导鍐⒑鐠団€虫灈闁搞垺鐓￠崺銏℃償閳锯偓閺嬪酣鏌熺€电校婵炲牊鎮傞弻?Elasticsearch 婵?Milvus 闂傚倸鍊搁崐鐑芥嚄閸洖绠犻柟鎹愵嚙鐟欙箓鎮楅敐搴″闁搞劍绻堥獮鏍庨鈧俊鑲╃棯閹佸仮闁哄本娲樼换娑㈠垂椤旂厧肖闂備焦鎮堕崐鏍洪悢鐓庤摕闁跨喓濮撮悙濠囨煃鏉炴壆鍔嶉柣搴ㄧ畺濮婅櫣鎷犻垾宕囦画缂備礁顑嗛幐鎯ｉ幇鏉跨闁规儳澧庨幊婵嬫⒑閸撹尙鍘涢柛鐘崇缁傛帡鎳栭埡鍐紳婵炶揪绲块幊鎾汇€傞幎鑺ョ厸闁告侗鍨版牎婵?
 * 2. 闂傚倸鍊搁崐宄懊归崶褜娴栭柕濞炬櫆閸婂潡鏌ㄩ弴妤€浜惧銈庡幖濞层倝鍩㈡惔銊ョ鐎规洖娲ㄩ弳?Redis 婵犵數濮烽弫鍛婃叏娴兼潙鍨傚┑鍌滎焾閺勩儵鏌″畵顔兼湰閸嶇敻姊洪棃娑辩叚閻庨潧鑻埢鎾寸鐎ｎ偆鍘遍梺闈涱槶閸ㄥ搫鈻嶉崶鈺冩／闁告瑣鍎抽惌娆撴煛瀹€鈧崰鏍嵁閸℃凹妲鹃梺鎸庣☉缁夊綊寮婚悢鍏煎仼閻忕偛褰ㄩ妷褏纾奸弶鍫涘妼濞搭噣鏌熼鍛偗鐎规洏鍔戦、娑橆煥閹邦喖鎯為梻鍌氬€峰ù鍥敋閺嶎厼绐楅柡宥庡幗閺呮繃銇勮箛鎾跺闁哄鑳堕埀顒€绠嶉崕閬嶅箠閹版澘姹查柛鈩冪⊕閻撴洟鏌熼柇锕€澧柡鍡欏仱閹粙顢涘璇蹭壕鐎规洖娲﹀▓鎯р攽椤斿浠滈柛瀣尰椤ㄣ儵鎮欓弻銉ュ及闂佽鍠栨晶鑺ョ閿曞倸纾归柣鏃傤焾椤忊晜绻濆閿嬫緲閳ь剚顨嗛幈銊╂倻閽樺锛涢梺缁樕戠粊鎾几閺嶃劎绠鹃柟瀵稿仜濞堢姴霉濠婂啰绉洪柡灞剧〒娴狅箓宕滆閳ь剚甯￠弻宥囩磼濡椿妫冮梺璇″枟椤ㄥ﹪寮幇顓熷劅闁炽儲鍓氬浠嬫⒒娴ｉ涓茬紓宥佸亾婵炲瓨绮犻崜娑樜ｉ幇鏉跨閻犲洩灏欓敍婊冣攽閻愭潙鐏﹂柣鐔村劤閸犲﹤顓兼径瀣ф嫼闁诲海娅㈤梽鍕熆濡绻嗛悹鎭掑妿绾惧吋銇勯弮鍥棄闁告繆娅ｉ埀顒冾潐濞叉粓鈥﹂崼銉⑩偓鏃堝礃椤忓懎鐝伴梻濠庡亽閸樺ジ藟濮樿埖鈷掑┑鐘查娴滄粍绻涚仦鍌氣偓鏇㈡箒闂佺粯鍨兼慨銈夊磻閿熺姵鐓忛煫鍥ь儏閻忣喚鐥崣銉х煓闁哄本绋撴禒锕傚礈瑜夋慨鍥煟韫囨挻绂嬮柛娆忓暣瀵鏁愰崱妤冪枃闂備礁鎲″褰掑垂閻㈠憡鍋╃€瑰嫭澹嬮弸搴ㄦ煙閻愵剚缍戞繛鍫涘劦濮婃椽妫冨☉姘暫闂佺锕ょ紞濠傤嚕閺屻儺鏁嗛柛鏇ㄥ厴閹锋椽姊洪崷顓х劸婵炲鍏橀崺銉﹀緞閹邦厾鍘藉┑掳鍊撻悞锕€鐣峰畝鍕厱闁宠鍎虫禍鐐繆閻愵亜鈧牜鏁繝鍥ㄥ€块柨鏇炲亰缂嶆牗淇婇妶鍛殶缁惧墽鍘ц灋闁绘鐗忕粻鎾趁瑰鍕畺缂佺粯鐩幃鈩冩償椤旀儳鎮戦柣搴ゎ潐濞叉牠鎮ラ崗闂寸箚闁归棿鐒﹂弲婵嬫煃瑜滈崜鐔煎箖?婵犵數濮烽弫鎼佸磻濞戙垺鍋ら柕濞у啫鐏婇悗骞垮劚濞诧箓寮冲鍕箚闁靛牆鎳忛崳褰掓煕閹寸姴孝闁宠鍨垮畷鎺戭潩椤撶偞娈橀梻浣虹帛閹稿爼宕曢柆宥嗙畳闂備焦瀵х换鍌炈囨潏銊ョ窞闁告洦鍊嬭ぐ鎺撴櫜闁告侗鍙庡Λ宀勬⒑閹肩偛鈧牕煤閻斿吋鍋傛い鎰剁畱閻愬﹪鏌曟径鍫濆姎濠殿喓鍨荤槐鎾存媴閹绘帊澹曢梻浣虹《閸撴繄绮欓幒妤佸亗?
 * 3. 闂傚倸鍊搁崐宄懊归崶銊х彾闁割偆鍠嗘禒鍫ユ煙闂傚顦﹂柦鍐枛閺屾洘绻濊箛娑欘€嶉梺鍛婄懃缁绘﹢寮诲澶婁紶闁告洦鍋€閸嬫挻绻濆銉㈠亾閸涙潙钃熼柕澶涘閸樼敻姊虹紒妯虹仸闁挎岸鏌ｈ箛瀣姢闁诡噮鍣ｉ幃銏焊娴ｈ鏉搁梻浣哥枃濡嫬螞濡ゅ懏鍊舵繛鍡樻尰閻撳啰鎲稿鍫濈婵犻潧妫岄弸宥夋煛閸ャ儱鐏╃痪鎯ь煼閺屾洘寰勯崱妯荤彆闂佺粯鎸婚悷鈺呭蓟閻旂⒈鏁囬柣鏃堫棑閻熴劑姊洪崨濠冣拹婵ǜ鍔戦獮澶愬箹娴ｇ懓浜遍梺鍓插亖閸ㄦ椽宕氬☉銏♀拺閺夌偞澹嗛ˇ锔剧磼婢跺骸鐓愮紒鍌涘浮閺佸啴宕掑☉妯规偅闁诲骸鍘滈崑鎾绘煃瑜滈崜鐔煎极瀹ュ應鏀介柛鈥崇箲閺傗偓婵＄偑鍊栧濠氬疾椤愩倗纾芥慨妯垮煐閻撴洟骞栧ǎ顒€鐏悘蹇ョ畵閺岋絽鈽夐崡鐐寸仌缂備胶濮电粙鎴﹀煡婢跺á鐔兼嚃閳轰礁绠?闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢埛姘そ婵¤埖寰勭€ｎ亙妲愰梻渚€娼ц墝闁哄懏鐩幏鎴︽偄鐏忎焦鏂€闂佺粯蓱瑜板啴寮抽悙鐑樼厱閹艰揪绲介弸鎴澢庨崶褝韬┑鈥崇埣瀹曘劑顢欓崗纰变画闂傚倷鑳堕、濠傗枍閺囥垺鍋￠柍鍝勬噹閽冪喖鏌ㄥ┑鍡╂Ц閹喖姊洪棃娑辨Ф闁稿海鍎ょ粋鎺撱偅閸愨斁鎷洪柡澶屽仦婢瑰棝藝閿斿墽纾界€广儱鎷戦煬顒侇殽閻愭潙濮堢紒缁樼箞瀹曘劑顢涘鍕帆闂備浇顕ч崙浠嬪箑閵夆晛绀冪憸宥壦夊顑芥斀闁绘劘鍩栬ぐ褏绱掗幓鎺撳仴闁炽儻绠撳畷濂告晲閸愌勭潖闂備胶绮敋缁剧虎鍘剧划濠氬箚瑜滈悢鍡涙偣妤︽寧顏犲褎娲栭埞鎴﹀焺閸愨晛鈧劙鏌＄仦鐣屝ユい褌绶氶弻娑滅疀閺冨倶鈧帗绻涢崱鎰仼妞ゎ偅绻勯幑鍕惞閻熺増鎲㈤梻鍌欑窔濞佳呮崲閸儱纾归柡宥庡幗閸嬪倿鏌涘畝鈧崑鐔煎矗閹剧粯鐓曢柕澶涚到婵′粙鎮樿箛锝呭籍闁哄瞼鍠栭獮鏍倷濞村浜炬繝闈涱儏缁犳牜鎲搁悧鍫濈瑨闁绘劕锕﹂幉绋款吋婢舵ɑ鏅滃銈嗗笂闂勫秵绂嶅鍫熺厸闁告劧绲芥禍楣冩⒑閹肩偛濡奸柣蹇旂箞椤㈡岸鏁愭径妯绘櫇闂佹寧妫佸Λ鍕闁秵鈷戦柛鎾村絻娴滄繄绱掔拠鎻掝仼閾伙綁鏌涢弴銊ョ仭闁抽攱鍨块弻娑樷攽閸℃浼€閻庢稒绻傞埞鎴︽倷閼碱剚鍕鹃梺鎼炲妺缁瑥鐣峰ú顏勭劦?
 * 4. 闂傚倸鍊峰ù鍥敋瑜庨〃銉х矙閸柭も偓鍧楁⒑椤掆偓缁夊澹曟繝姘厽闁哄啫娲ゆ禍鍦偓瑙勬尫缁舵岸寮诲☉銏犖ㄦい鏃傚帶椤晠姊洪崫鍕仴闁稿酣娼ч锝夊醇閺囩喎鈧兘鏌℃径瀣劸婵☆偄妫濆?Redis GEO 闂傚倸鍊搁崐宄懊归崶褜娴栭柕濞炬櫆閸ゅ嫰鏌ょ粙璺ㄤ粵婵炲懐濮垫穱濠囧Χ閸屾矮澹曢梻浣风串缁蹭粙鎮樺杈╃當闁绘梻鍘ч悞鍨亜閹哄棗浜惧銈庡亜缁绘ê鐣风粙璇炬棃鍩€椤掑嫮宓侀柕蹇ョ磿缁犻箖鏌涢埄鍏狀亝鎱ㄦ径鎰厸闁糕剝鐟ラ埢鍫熸叏婵犲啯銇濇俊顐㈠暣閸╋繝宕ㄩ鍛亾瀹ュ棛绡€闁靛繈鍨洪崵鈧┑鈽嗗亝缁诲牓鎮伴鈧畷姗€鍩￠崘顏嗘闂備礁鎲￠崝蹇涘疾濞戙埄鏁侀柟鍓х帛閸婂灚顨ラ悙鑼虎闁告梹纰嶇换娑氫沪閸屾艾顫囬悗娈垮枟閹倸鐣烽幒妤佸€风紒顔款潐鐎氫粙姊绘担鍛婂暈婵炶绠撳畷鎴﹀礋椤栵絾鏅滃銈嗗笒閸婅崵澹曟總鍛婂仯闁搞儯鍔庨崣鈧梺鍛婄懃鐎氼參銆冮妷鈺傚€烽悗鐢殿焾閳懓顪冮妶搴″箲闁告梹鍨甸悾鐑藉Ω閳哄﹥鏅ｉ梺缁樺姉婢ф宕板顓犵瘈闁汇垽娼ф禒锕傛煕閵娿儳鍩ｆ鐐村姍瀹曨偊濡烽姀鐘卞濠电偞鍨剁敮妤€鈻嶉崶顒佺厸濞撴艾娲ゅ▍宥嗩殽閻愯揪鑰块柟铏矒濡啫鈽夊Δ鍐尲闂傚倷娴囬褏鎹㈤幒妤€纾婚柣鏂垮悑閹偤骞栫紒鐗堝閸嬫捇骞掑Δ浣糕偓濠氭煠閹帒鍔氬ù鐙€鍨辩换娑㈠箻绾惧顥濋梺鎸庢穿婵″洦绔熼弴銏犻敜婵°倓鑳堕崢浠嬫⒑閹稿海绠撻柟宄邦儔閹繝鎮℃惔妯绘杸?
 *
 * @author smartLive
 * @date 2026-03-11
 */
@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    private static final java.time.format.DateTimeFormatter SHOP_ANALYSIS_TIME_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Autowired
    private ShopMapper shopMapper;
    @Autowired
    private IShopTypeService shopTypeService;
    
    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private RemoteStarService remoteStarService;
    @Autowired
    RemoteFollowService remoteFollowService;
    @Autowired
    private RemoteUserService remoteUserService;
    @Autowired
    private RemoteOrderService remoteOrderService;
    @Autowired
    private RemoteReviewService remoteReviewService;
    @Autowired
    private RedisMultiCacheManager redisMultiCacheManager;
    @Autowired
    private ZSetIdManager zSetIdManager;
    /**
     * 闂傚倸鍊峰ù鍥敋瑜忛幑銏ゅ箛椤旇棄搴婇梺鍦濠㈡绮诲ú顏呯厸濠㈣泛顭ú绨嗛梻鍌氬€峰ù鍥敋瑜庨〃銉х矙閸柭も偓鍧楁⒑椤掆偓缁夊澹曠紒妯圭箚妞ゆ牗绻傛禍鍦磼閳ь剚绻濋崶銊モ偓鐢告煥濠靛棝顎楀ù婊勭箓閳规垿顢欓悾宀€鐓侀梺闈涙搐鐎氫即鐛幒鎴悑闁搞儴鍩栬ⅵ闂備胶鎳撻崥瀣焽濞嗘挻鏅濋柕鍫濐槸閻撯€愁熆閼搁潧濮囩紒鐘差煼閺屾盯骞橀懠顒€濡藉銈呴鐏忔籍pVO
     * @param shop Shop闂傚倸鍊峰ù鍥敋瑜庨〃銉х矙閸柭も偓鍧楁⒑椤掆偓缁夊澹曠紒妯圭箚妞ゆ牗绻傛禍鍦磼閳?
     * @return ShopVO闂傚倸鍊峰ù鍥敋瑜嶉湁闁绘垼妫勯弸渚€鏌熼梻鎾闁逞屽厸閻掞妇鎹㈠┑瀣倞闁靛鍎冲Ο?
     */
    private ShopVO convertToShopVO(Shop shop) {
        if (shop == null) {
            return null;
        }
        ShopVO shopVO = new ShopVO();
        BeanUtils.copyProperties(shop, shopVO);
        return shopVO;
    }

    private boolean isVisibleShop(Shop shop) {
        return shop != null
                && Objects.equals(shop.getStatus(), 1)
                && Objects.equals(shop.getAuditStatus(), AuditStatusEnum.PASS.getCode());
    }

    private boolean isVisibleShop(ShopVO shopVO) {
        return shopVO != null
                && Objects.equals(shopVO.getStatus(), 1)
                && Objects.equals(shopVO.getAuditStatus(), AuditStatusEnum.PASS.getCode());
    }

    /**
     * 闂傚倸鍊峰ù鍥敋瑜忛幑銏ゅ箛椤旇棄搴婇梺鍦濠㈡绮诲ú顏呯厸濠㈣泛顭ú绨嗛梻鍌氬€搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹濠德板€曢幊宀勫焵椤掆偓閸燁垰顕ラ崟顖氱疀妞ゆ垟鏂傞崕鐢稿蓟濞戙垹绠涢柕濠忛檮閻濇洘绻濆▓鍨灍缂侇喖绉规俊鐢稿礋椤栨氨顔婇悗骞垮劚濞村倸危椤曗偓閺岀喖宕楅崗鑲╃▏闂佹寧娲忛崐婵嬪春閵夛箑绶為柟閭﹀墰椤旀帡姊洪崨濠冨矮闁绘帪绠撻、鎾崇暋椤ｆ樋VO闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹濠德板€曢幊宀勫焵椤掆偓閸燁垰顕ラ崟顖氱疀妞?
     * @param shopList Shop闂傚倸鍊峰ù鍥敋瑜庨〃銉х矙閸柭も偓鍧楁⒑椤掆偓缁夊澹曠紒妯圭箚妞ゆ牗绻傛禍鍦磼閳ь剚绻濋崶銊モ偓鐢告煥濠靛棝顎楅柡瀣〒缁辨帡鍩€椤掑嫬骞㈡俊顖氭贡缁犳岸姊洪棃娑氬闁瑰啿閰ｉ、鏃堝Ψ閳哄倻鍘?
     * @return ShopVO闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹濠德板€曢幊宀勫焵椤掆偓閸燁垰顕ラ崟顖氱疀妞?
     */
    private List<ShopVO> convertToShopVOList(List<Shop> shopList) {
        if (CollUtil.isEmpty(shopList)) {
            return new ArrayList<>();
        }
        return shopList.stream()
                .map(this::convertToShopVO)
                .collect(Collectors.toList());
    }
    /**
     * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢妶鍡椾粡濡炪倖鍔х粻鎴犲閸ф鐓欑紓浣靛灩濞呮﹢鏌℃担鍓插剱濞ｅ洤锕俊鍫曞礋椤撶偛顬夐梻浣侯焾濞寸兘宕伴幇顓犫攳濠电姴娲ゅ洿闂佸憡渚楅崰鏍р枍閵堝鈷?
     *
     * @param id 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ラ梻鍌欑婢瑰﹪鎮￠崼銉ョ；闁糕剝绋戦悡婵嬫煛閸屾氨姘ㄩ柡鈧禒瀣厽婵☆垵娅ｉ敍宥嗐亜閿濆棛鍙€闁?
     * @return 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅?
     */
    @Override
    public Shop selectShopById(Long id) {
        return shopMapper.selectShopById(id);
    }

    /**
     * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢妶鍡椾粡濡炪倖鍔х粻鎴犲閸ф鐓欑紓浣靛灩濞呮﹢鏌℃担鍓插剱濞ｅ洤锕俊鍫曞礋椤撶偛顬夐梻浣侯焾濞寸兘宕伴幇顓犫攳濠电姴娲ゅ洿闂佸憡渚楅崰鏍р枍閵堝鈷戠痪顓炴噺閻濐亪鏌ｉ悢鍙夋珚鐎殿喛顕ч埥澶愬閻樻鍞洪梻浣烘嚀閻忔繈宕鐐村仼濞寸姴顑嗛埛?
     *
     * @param shop 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅?
     * @return 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅?
     */
    @Override
    public List<Shop> selectShopList(Shop shop) {
        if (shop == null) {
            shop = new Shop();
        }
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId != null && !SecurityUtils.isAdmin(currentUserId)) {
            return selectShopListByUserId(currentUserId, shop);
        }
        return shopMapper.selectShopList(shop);
    }

    /**
     * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢敃鈧壕褰掓煟閻旂厧浜伴柣鏂挎閹便劌顪冪拠韫闁荤姳璀﹂崹鍫曞蓟濞戙垹绠涢柕濠忛檮閻濇洟姊虹粙鎸庢崳闁哥姵鍔栫粚杈ㄧ節閸ヮ灛褔鏌涘☉鍗炴灈婵炲懌鍊濆?
     *
     * @param shop 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅?
     * @return 缂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸婂潡鏌ㄩ弴鐐测偓鍝ョ不閺夊簱鏀介柣妯虹－椤ｆ煡鏌?
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertShop(Shop shop) {
        shop.setCreateTime(DateUtils.getNowDate());
        int i = shopMapper.insertShop(shop);
        if (i > 0) {
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                throw new BusinessException("user id is required");
            }
            if (shop.getId() == null) {
                throw new BusinessException("shop id was not generated");
            }
            Boolean relationSaved = remoteUserService.addUserShopRelation(userId, shop.getId());
            if (!Boolean.TRUE.equals(relationSaved)) {
                throw new BusinessException("failed to save user-shop relation");
            }
            flashShopListRedisCache(shop.getTypeId());
            //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭画闂佹寧娲栭崐鎼佸垂閸岀偞鐓曠憸搴ㄣ€冮崨瀛樺€块柛顭戝亖娴滄粓鏌熸潏鍓хɑ缁绢叀鍩栭妵鍕晜婵傚憡顎嶉梺闈涙搐鐎氫即鐛Ο灏栧亾濞戞顏堝焵椤掍礁濮夐柍褜鍓氶鏍窗閺囩姴鍨濇繛鍡楁禋濞兼牜鎲搁悧鍫濈瑨缂佺姵绋掗妵鍕疀閹炬惌妫ら梺浼欑秮缁犳牕顫?
            sendAuditMessage(shop);
        }
        return i;
    }

    /**
     * 婵犵數濮烽弫鎼佸磿閹寸姴绶ら柦妯侯棦瑜版帒纾奸柣鎰皺閻涖儱鈹戞幊閸婃洟骞婃惔銊ュ嚑濞撴埃鍋撻柡灞剧缁犳盯骞橀搹顐⑩偓顖炴⒑缁嬫寧鎹ｉ柛鐘冲姈缁岃鲸绻濋崶顬囨煕濞戝崬鏋涙繛鍛€濆?
     *
     * @param shop 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅?
     * @return 缂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸婂潡鏌ㄩ弴鐐测偓鍝ョ不閺夊簱鏀介柣妯虹－椤ｆ煡鏌?
     */
    @Override
    public int updateShop(Shop shop) {
        Shop oldShop = shop.getId() == null ? null : shopMapper.selectById(shop.getId());
        shop.setUpdateTime(DateUtils.getNowDate());
        int i = shopMapper.updateShop(shop);
        if(i > 0){
            Long shopId = shop.getId();
            flashShopRedisCache(shopId);
            if (oldShop != null && oldShop.getTypeId() != null) {
                flashShopListRedisCache(oldShop.getTypeId());
            }
            if (shop.getTypeId() != null && (oldShop == null || !Objects.equals(oldShop.getTypeId(), shop.getTypeId()))) {
                flashShopListRedisCache(shop.getTypeId());
            }
            //闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢埛姘そ婵¤埖寰勭€ｎ亙妲愰梻渚€娼ц墝闁哄懏鐩幏鎴︽偄鐏忎焦鏂€闂佺鏈划搴⌒掗埀顒勬⒒閸屾艾鈧嘲霉閸パ屾禆闁靛ň鏅滈崵鍕煠缁嬭法浠涙繛鍛У娣囧﹪濡堕崒姘闂?
            publish(new String[]{shopId.toString()});
            //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭画闂佹寧娲栭崐鎼佸垂閸岀偞鐓曠憸搴ㄣ€冮崨瀛樺€块柛顭戝亖娴滄粓鏌熸潏鍓хɑ缁绢叀鍩栭妵鍕晜婵傚憡顎嶉梺闈涙搐鐎氫即鐛Ο灏栧亾濞戞顏堝焵椤掍礁濮夐柍褜鍓氶鏍窗閺囩姴鍨濇繛鍡楁禋濞兼牜鎲搁悧鍫濈瑨缂佺姵绋掗妵鍕疀閹炬惌妫ら梺浼欑秮缁犳牕顫?
            sendAuditMessage(shop);
        }
        return i;
    }

    /**
     * 闂傚倸鍊峰ù鍥敋瑜庨〃銉х矙閸柭も偓鍧楁⒑椤掆偓缁夊澹曟繝姘厽闁哄啫娲ゆ禍鍦偓瑙勬尫缁舵岸寮诲☉銏犖ㄦい鏃傚帶椤晝绱撴担鎻掍壕闂佺硶鍓濈粙鎺楁偂閺囥垺鐓冮柍杞扮閺嗙喖鏌嶇粭鍝勨偓婵嬪蓟瀹ュ牜妾ㄩ梺鍛婃尰閻熲晞妫熼梻鍌氱墛缁嬫捇寮抽敂鑺ュ弿婵☆垰鐏濋悡鎰版煕鐎ｎ偄濮夌紒杈ㄦ尰閹峰懐鎷犻敍鍕Ш闁荤喐绮庢晶妤呭垂閸ф钃熼柣鏂垮悑閸嬪倿骞栨潏鍓хɑ闁硅尙鍘ч—鍐Χ閸愩劌顬堥梺鎸庢处娴滎亪鎮伴鍢夋棃宕ㄩ闂寸紦闂備礁鎲＄粙鎴︽晝閿曞倸绀夐柣銏犳啞閳锋垿鏌涘┑鍡楊仼妞ゅ繆鏅濈槐鎺旂磼濡櫣顑傞梺?
     * 1. 闂傚倸鍊搁崐鐑芥嚄閸撲礁鍨濇い鏍仜妗呭┑鐐村灟閸ㄥ綊鎮块悙顒傜瘈闂傚牊渚楅崕蹇曠磼閻樺磭澧柍瑙勫灴閹晠宕ｆ径瀣€风紓鍌欒閸嬫捇鏌涢埄鍐姇闁绘挾鍠愭穱濠囶敍濠靛棔姹楅柣銏╁灡閻╊垶寮婚敐鍛斀闁割偅绻冮悘鍫ユ⒑閸濆嫬顦柛鎾寸箞楠炲繘宕ㄩ弶鎴滅炊闂佸憡娲栭悘姘掓径鎰厽閹兼番鍊ゅ鎰版煙閸濄儱鍘撮柟顔惧仱閺佸啴宕掑杈╂毎闂備礁鎼崯顐︽偋閸℃瑧涓嶅┑鐘崇閸嬶綁鏌涢妷顖滃矝闁稿鎹囬幖褰掝敃閿濆棛妲曢梻鍌欐祰瀹曠敻宕伴幇顔煎灊閹兼番鍨哄▍鐘充繆閵堝倸浜鹃柣鎾卞€濋悡顐﹀炊閵娧€妲堥梺?
     * 2. 闂傚倷娴囬褏鈧稈鏅犻、娆撳冀椤撶偟鐛ラ梺鍝勭▉閸樿偐澹曢崷顓熷枑闁绘鐗嗘穱顖炴煛娴ｅ憡顥㈤柡宀嬬秮楠炲洭顢楁担鐟板壍闂備焦妞块崢浠嬨€冩繝鍥ц摕闁绘柨鎲＄紞鍥煙鐟欏嫬濮囬柟顔兼嚇濮?MQ 濠电姷鏁告慨鐑藉极閹间礁纾婚柣鎰惈閸ㄥ倿鏌ｉ姀鐘冲暈闁稿顑呴埞鎴︽偐閹绘帗娈銈嗘礋娴滃爼寮诲☉妯锋婵炲棙鍔楃粙鍥煟閻斿摜鎳曠紒鐘虫崌楠炲啫顫滈埀顒勫箖濞嗘劖濯撮柛鎰ㄦ櫓閳ь剚顨婂娲川婵犲啰鍙嗛柣搴㈠嚬閸橀箖骞戦姀鐘闁靛繒濮烽鎺楁⒑閹勭闁稿瀚埢鎾澄旈埀顒勫煘閹达附鍊烽柛娆忣槸濞咃綁姊绘担绋跨盎缂傚秳绶氶幃浼搭敋閳ь剙顕ｉ崐鐕佹闂佺粯鎸堕崕鐢稿蓟濞戞ǚ妲堥柛妤冨仜缁犺绻涚€涙鐭婇柣鏍с偢楠炲啫螖閳ь剟鍩㈤幘璇插瀭妞ゆ梻鏅禍鑸电節閻㈤潧浠滅€殿喖鐖奸弫鍐閵堝洤绁?(ES/Milvus) 缂傚倸鍊搁崐鎼佸磹瀹勯偊娓婚柟鐑樻⒒閻岸鏌涢锝嗙闁搞劌鍊块弻娑㈡晜鐠囨彃绠哄銈庡亝濞茬喖寮诲澶婁紶闁告洦鍓欏▍锝夊级?
     * 3. 缂傚倸鍊搁崐鎼佸磹閻戣姤鍊块柨鏇炲€搁拑鐔兼煏婵炵偓娅撻柡浣稿閺屾稑鈽夐崡鐐茬闂佸搫妫庨崐婵嬪蓟濞戙垹鐒洪柛鎰剁細缁姊洪柅鐐茶嫰婢ь垶鎮介妞诲亾瀹曞洦娈鹃梺闈浤涢埀顒勫磻閹炬枼妲堟繛鍡橆焽閸旂兘鎮?Redis 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佸湱鍎ら崵锕€鈽夊Ο閿嬵潔濠殿喗锕╅崢鍏肩韫囨搩娓婚柕鍫濇婵呯磼閻樺啿鐏╃紒顔肩墦瀹曞崬螣閸︻厾鐣鹃梻渚€娼ч悧鍡涘箠鎼搭煈鏁傞柕澶嗘櫆閻撳啰鎲稿鍫濈婵炲棙鎸搁悿顕€鏌熷▓鍨灍妞ゎ偅娲熼弻鐔煎箲閹伴潧娈紓浣哄Ь瀹曠敻鍩€椤掑喚娼愭繛鍙夌墪鐓ら柨鏇炲€哥粻鐔封攽閸屾碍鍟為柍閿嬪灴閺屾稑鈽夊鍫濆闂佺懓鍟块崯鎾蓟閵娾晛鍗虫俊顖氭惈椤洤顪冮妶鍐ㄧ仾闁煎綊绠栭崺銉﹀緞閹邦剦娼婇梺缁橆焽閺佹悂寮虫导瀛樷拻濞达絽鎲￠崯鐐层€掑顓ф疁鐎规洖缍婇幖褰掝敃閵忋垻鐛梻浣告啞濞诧箓宕归柆宥呯９闁割煈鍋嗙粻楣冩煙鐎电鍓卞ù鐓庡缁绘繃绻濋崒婊冾暫闂?
     *
     * @param ids 闂傚倸鍊搁崐鎼佸磹閹间礁纾圭紒瀣紩濞差亝鍋愰悹鍥皺閿涙盯姊洪悷鏉库挃缂侇噮鍨跺畷鎴︽晸閻樺磭鍘搁梺鎼炲劘閸斿秹鎯冮幋鐐簻闁挎棁鍋愰悾鐢告煛鐏炲墽娲村┑鈩冩倐婵＄柉顦村Δ鏃€绻濆▓鍨灈闁挎洩绲块崚鎺戔枎閹邦亞绠氶梺鍓插亝濞叉牠鎮欐繝鍥ㄧ厪濠电倯鍐仾妞ゆ柨绻樺缁樻媴閸涘﹨纭€闂佸憡顭嗛崶銊ヤ槐闂侀潧艌閺傚倿鍩€椤掆偓閸燁垳鎹㈠┑瀣倞鐟滃繘顢欓幋锔解拺闁告捁灏欓崢娑㈡煕鐎ｎ亝顥㈤柛鈹垮灲瀵剚鎯旈幘瀛樻澑闂備胶绮敋缁剧虎鍙冮妴鍌炲蓟閵夛妇鍘搁梺绯曞墲椤洤煤鐎涙ɑ鍙忓┑鐘插亞閻撹偐鈧娲栧畷顒冪亙婵炴挻鑹惧ú锕偹夐敐澶嬧拻?
     * @return 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佺粯鍔曢顓犵不妤ｅ啯鐓冪憸婊堝礈濮樿鲸宕叉繛鎴欏灩瀹告繃銇勯幘璺烘瀾鐎规洘濞婂铏规嫚閳ヨ櫕鐏嶆繝銏㈡嚀濡瑧绮嬮幒妤佹櫇闁稿本绋戦埀顒勬敱閵囧嫰骞掑鍥獥闂佸摜鍟块崑?
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteShopByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        List<Shop> shops = Arrays.stream(ids)
                .map(shopMapper::selectShopById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Long> shopIdList = Arrays.stream(ids)
                .map(id -> {
                    try {
                        return Long.valueOf(id);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int i = shopMapper.deleteShopByIds(ids);
        if (i > 0) {
            if (CollUtil.isNotEmpty(shopIdList)) {
                Boolean relationDeleted = remoteUserService.deleteUserShopRelationByShopIds(shopIdList.toArray(new Long[0]));
                if (!Boolean.TRUE.equals(relationDeleted)) {
                    throw new BusinessException("failed to delete user-shop relations");
                }
            }
            for (Long id : ids) {
                executorService.submit(() -> {
                    log.info("async delete shop thread={}, shopId={}", Thread.currentThread().getName(), id);
                    Long shopId = id;
                    ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                    contentSyncMessage.setId(shopId);
                    contentSyncMessage.setIndexName(EsIndexNameConstants.SHOP_INDEX_NAME);
                    contentSyncMessage.setType(GlobalBizTypeEnum.SHOP.getCode());
                    //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭画闂佹寧娲栭崐鎼佸垂閸屾埃鏀介柛灞剧矤閻掑墽绱掗悩鎻掔骇闁靛洤瀚版俊鍫曞炊閳轰胶绉碽bitMq濠电姷鏁告慨鐑藉极閹间礁纾婚柣鎰惈閸ㄥ倿鏌ｉ姀鐘冲暈闁稿顑呴埞鎴︽偐閹绘帗娈銈嗘礋娴滃爼寮诲☉妯锋婵炲棙鍔楃粙鍥╃磽娴ｆ彃浜鹃梺绯曞墲缁嬫帡鎮￠悢闀愮箚妞ゆ牗绻傛禍褰掓偨椤栨稓娲撮柡宀嬬到閳规垿宕煎┑鍡╂s闂傚倸鍊搁崐宄懊归崶褜娴栭柕濞炬櫆閸ゅ嫰鏌ょ粙璺ㄤ粵婵炲懐濮垫穱濠囧Χ閸屾矮澹曢梻?
                    mqMessageSendUtils.sendMqMessage(SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                    //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭画闂佹寧娲栭崐鎼佸垂閸屾埃鏀介柛灞剧矤閻掑墽绱掗悩鎻掔骇闁靛洤瀚版俊鍫曞炊閳轰胶绉碽bitmq濠电姷鏁告慨鐑藉极閹间礁纾婚柣鎰惈閸ㄥ倿鏌ｉ姀鐘冲暈闁稿顑呴埞鎴︽偐閹绘帗娈銈嗘礋娴滃爼寮诲☉妯锋婵炲棙鍔楃粙鍥╃磽娴ｆ彃浜鹃梺绯曞墲缁嬫帡鎮￠悢闀愮箚妞ゆ牗绻傛禍褰掓偨椤栨稓娲撮柡宀嬬到閳规垿宕煎┑鍠槒lvus闂傚倸鍊搁崐宄懊归崶褜娴栭柕濞炬櫆閸ゅ嫰鏌ょ粙璺ㄤ粵婵炲懐濮垫穱濠囧Χ閸屾矮澹曢梻?
                    mqMessageSendUtils.sendMqMessage(SearchMqConstants.MILVUS_SYNC_EXCHANGE, SearchMqConstants.MILVUS_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                });
            }
            Arrays.stream(ids)
                    .filter(Objects::nonNull)
                    .forEach(this::flashShopRedisCache);
            shops.stream()
                    .map(Shop::getTypeId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())
                    .forEach(this::flashShopListRedisCache);
        }
        return i;
    }

    /**
     * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佺粯鍔曢顓犵不妤ｅ啯鐓冪憸婊堝礈濮樿鲸宕叉繛鎴欏灩瀹告繃銇勯幘璺哄壉闁告柨顦靛铏规嫚閳ュ磭浠梺鍝勮閸斿繘鎮橀崘顔解拺闁告稑锕ｇ欢閬嶆煕閻樻剚娈滅€规洜鏁诲畷濂告偄閾忚鍟庨梻浣瑰缁诲倿骞婅箛鏇犵焼鐎光偓閸曨剛鍘遍梺瑙勫劤椤曨厾绮婚弽顓熷€?
     *
     * @param id 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ラ梻鍌欑婢瑰﹪鎮￠崼銉ョ；闁糕剝绋戦悡婵嬫煛閸屾氨姘ㄩ柡鈧禒瀣厽婵☆垵娅ｉ敍宥嗐亜閿濆棛鍙€闁?
     * @return 缂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸婂潡鏌ㄩ弴鐐测偓鍝ョ不閺夊簱鏀介柣妯虹－椤ｆ煡鏌?
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteShopById(Long id) {
        Shop shop = shopMapper.selectShopById(id);
        int i = shopMapper.deleteShopById(id);
        if (i > 0) {
            Long shopId = shop != null ? shop.getId() : id;
            if (shopId != null) {
                flashShopRedisCache(shopId);
            }

            if (shopId != null) {
                Boolean relationDeleted = remoteUserService.deleteUserShopRelationByShopId(shopId);
                if (!Boolean.TRUE.equals(relationDeleted)) {
                    throw new BusinessException("failed to delete user-shop relation");
                }
            }
            if (shop != null && shop.getTypeId() != null) {
                flashShopListRedisCache(shop.getTypeId());
            }
        }
        return i;
    }

    @Resource
    private CacheClient cacheClient;

    /**
     * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢妶鍥╃厠闂佺粯鍨堕弸鑽ょ礊閺嵮岀唵閻犺櫣灏ㄩ崝鐔兼煛?ID 闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯妞ゆ梻鍘ч～鈺呮⒑濮瑰洤鈧倝宕抽敐澶婅摕婵炴垯鍨归悞娲煕閹板吀绨村┑顔兼喘濮婅櫣娑甸崨顔惧涧闂佽崵鍟块弲鐘荤嵁閸愵厹浜归柟鐑樺灩閸婄偤姊虹紒妯虹伇濠殿喓鍊濋、鎾愁吋婢跺鎷?
     * 闂傚倸鍊搁崐鎼佸磹閻戣姤鍊块柨鏇氶檷娴滃綊鏌涢幇鐢靛帒婵炲樊浜滄儫闂佸疇妗ㄩ悞锕傛倵椤掑嫭鈷戦梻鍫熺〒婢ф洘銇勯敂璇茬仸闁诡喗锕㈤幖褰掝敃閵堝洨妲囧┑鐘垫暩婵挳宕愮紒妯绘珷闁规壆澧楅悡鏇㈡倵閿濆骸浜濈€规洖鐬奸埀顒侇問閸犳鍒婇悾宀€涓嶆繛鎴欏灩閸楁娊鏌ｉ幇顓у晱濞村吋鐗犲缁樻媴閸涘﹥鍎撳銈忓瘜閸嬪﹤顕ｉ幓鎺嗘婵☆垰绻戝浠嬪极閸愵喖鐒垫い鎺戝閺呮繈鏌曡箛瀣偓鏍疾閹间焦鐓熸俊顖氱仢閻ㄥ搫霉閻撳寒鍎忛柍瑙勫灴閹晝绱掑Ο濠氭暘闂備胶绮〃鍛崲閹版澘鐓濋柡鍐ㄧ墕椤懘鏌曢崼婵囶棤闁告ê宕埞鎴︻敊閺傘倓绶甸梺鍛婃尰缁嬫帡寮查崼鏇炵疀闁绘鐗忛崢鎾绘偡濠婂嫮鐭掔€规洘绮岄～婵嬪础閻愯尙浜伴梺鐟板悑閻ｎ亪宕濆畝鍕亗婵せ鍋撻柡灞糕偓鎰佸悑閹肩补鈧磭顔愮紓浣哄亾閸庡磭绱炴繝鍥ц摕闁靛牆鎮块崷顓涘亾閿濆骸浜濋柣婵囩墬缁绘繈濮€閿濆棛銆愰柣搴㈢煯閸楁娊鐛崘銊庢棃鍩€椤掑嫬鐓″璺号堥弸宥夊级閸稑濡奸柛婵囨そ閺屾盯鍩為幆褌澹曞┑锛勫亼閸婃牜鏁繝鍥ㄥ殑闁告挷鑳舵稉宥嗕繆椤栨繂鍚圭紒鐘荤畺閺屾盯顢曢妶鍛€婚梺璇茬箚閺呮繄妲愰幒鎾寸秶闁靛鍎茬拠鐐烘⒑鐠団€虫灀闁告挻绻堥獮鍡涘籍閸繍娼婇梺缁橈供閸嬪嫭绂嶆ィ鍐╃厪闁割偅绻冨婵堢棯閹冩倯缂佺粯鐩獮瀣倷閸偄娅樺┑鐘媰閸ワ附鍠氶梺鍝勭焿缁绘繂鐣烽崡鐐嶇喖宕崟鍨秿闂傚倷鑳堕幊鎾愁嚕閸洖绠伴柟鎯版閺嬩線鏌熼悧鍫熺凡鐎瑰憡绻冮妵鍕箻鐠虹儤鐏侀梺閫炲苯澧婚柛瀣濡叉劙骞樼拠鑼紲濠殿喗锕╅崗姗€宕戦幘骞夸汗闁圭儤鍨归鍥⒑瑜版帗锛熼柣鎺炵畵瀹曟垿濡舵径瀣帾婵犵數鍋熼崑鎾斥枍閸涱垳纾奸柍褜鍓熷畷鐔碱敍濞戞艾骞樺┑鐘愁問閸犳宕濋弴銏犵劦妞ゆ帊绀佹慨宥団偓娈垮櫘閸嬪嫰顢樻總绋垮窛妞ゅ繐鎷嬪鏃堟⒒娴ｇ瓔娼愮€规洘锚閳诲秹寮撮姀鐘殿唹闂佹寧绻傞ˇ浼存偂閸愵喗鍋℃繛鍡楃箰椤忣偅绻涢崣澶嬪唉闁哄矉绱曟禒锕傛嚍閵夈儲鐣紓鍌欒兌婵數绮欓幋锕€鐓″璺号堥弸搴㈢箾閸℃ê鐏﹂柕鍥ㄧ箖缁绘繄鍠婂Ο娲绘綉闂佺顑呭Λ娆撳疾鐠轰綍鏃堝礋椤愩倗鈽夐梻?
     * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鑼槷闂佸搫娲㈤崹鍦不閻樿櫕鍙忔俊鐐额嚙娴滈箖鎮楃憴鍕婵炲弶绮撻崺鈧い鎺嶈兌閳洟鎷戦崡鐏荤懓鈹冮崹顔瑰亾閺嶃劎鈹嶅┑鐘叉祩閺佸秵鎱ㄥ鍡楀绩缁绢厸鍋撶紓鍌氬€烽悞锕傚礉閺嶎厹鈧啯绻濋崶褑鎽曢梺闈涚墕濡瑧绱為崶顒佺厪濠电姴绻愰々顒勬煕濞嗗繑顥炵紒缁樼箓閳绘捇宕归鐣屼簮闂備礁鎼幊蹇曞垝鎼达絽鍨濋悗锝庝憾閸氬顭跨捄鐚村姛妞ゆ柨锕娲偡閹殿喗鎲肩紓渚囧枛閸熻儻鐏冩繛杈剧秮椤ユ挾绮诲顒夋富闁靛牆妫涙晶顒勬煏閸ャ劎娲寸€规洘鍨块獮妯肩磼濡鍔掓俊鐐€栭崝锕€顭块埀顒佺箾閿濆骸娅嶆慨濠勭帛閹峰懘宕ㄦ繝鍐ㄥ壍闂佽崵鍋為崙褰掑磻婵犲倻鏆﹂柡鍥ュ灩缁犵粯銇勯弴鐐村櫤闁哄拋浜滈埞鎴炲箠闁稿﹥娲滈埀顒佸搸閸斿秶绮嬪鍫涗汗闁圭儤鎸鹃崢鐢告⒑鐠団€崇€婚柛鎰ㄦ櫆閻︼絾淇婇悙顏勨偓鎴﹀磹閺囥垹绠犳慨妞诲亾鐎殿喖顭烽弫鎰緞濞戞氨鈼ゆ俊鐐€栧濠氬磻閹炬枼鏀介柍銉ㄥ皺閻瑩鏌＄仦鍓ф创闁糕晪绻濆畷鎺戭煥閸曨偄鐏￠梺璇插椤旀牠宕伴弴銏犵闁硅泛顫曢埀顑跨閳藉顫滈崱妯哄厞婵＄偑鍊栭幐楣冨磻濞戞瑦娅犲Δ锝呭暞閳锋帒霉閿濆拋娼熼柍褜鍏橀崑鎾绘⒑缁嬫鍎愰柟鐟版喘瀵鎮㈢喊杈ㄦ櫖濠电姴锕ら崰姘跺汲椤撶儐娓婚柕鍫濆暙閸旀粓鏌涢妸銉﹀仴鐎殿喖顭烽幃銏㈡偘閳ュ厖澹曞┑鐐村灦閻燁垶鎮為挊澶樼唵鐟滄粍绂嶉鍫濊摕婵炴垶菤閺嬪秹鎮归崶銊ョ祷闁哄棗锕弻锛勨偓锝庡亞婢х敻鏌＄仦鐐缂佺粯鐩畷褰掝敊閻熼澹曞┑掳鍊曢幊搴ㄦ偪椤斿皷鏀介柣妯哄级婢跺嫰鏌涚€ｅ墎绉柡灞剧洴婵＄兘鏁愰崨顓烆潛闂備礁鎲″鐟邦潩閵娧勵潟闁圭儤顨呮儫閻熸粌閰ｅ鎶筋敆閸曨剛鍘搁柣蹇曞仧閸犲骸煤閵堝洨涓嶉柟鐑橆殕閻撴瑩鏌涘┑鍡楊仾妞ゃ儲绮岄湁?
     *
     * @param id 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴顭ㄩ崼婵堢崶闁硅偐琛ラ崹鎯р槈濡攱鏂€闁诲函缍嗘禍鐐侯敊?ID
     * @return 婵犵數濮烽弫鍛婃叏椤撱垹绠柛鎰靛枛瀹告繃銇勯幘瀵哥畼闁硅娲熼弻锝嗘償閵堝孩缍堥梺璇″枛閸婂湱绮嬪鍥ㄥ磯闁惧繗顫夊▓楣冩⒑闂堟侗妾ч梻鍕瀹曟洟鎮㈤崗鑲╁弳濠电娀娼уΛ娆撍夐悩缁樼厸闁糕剝鐟ラ埢鍫熸叏婵犲啯銇濈€规洦鍋婃俊鐑藉Ψ閿旈敮鍋撶拠娴嬫斀闁宠棄妫楁禍婵堢磼鐠囪尙澧﹂柣?VO
     */
    @Override
    public ShopVO queryById(Long id) {
        //闂傚倸鍊峰ù鍥х暦閻㈢绐楅柟鎵閸嬶繝寮堕崼姘珔缂佽翰鍊曢湁闁绘ê妯婇崕蹇曠磼閹邦収娈滈柡灞剧☉閳藉宕￠悙鍏稿寲缂備焦鍎宠ぐ鐐靛垝濞嗘挸钃熼柣鏃傚帶缁犳氨鎲歌箛娑欐櫖鐎广儱顦伴悡娆撴煟閿濆懓瀚伴崯鍝ョ磽娴ｄ粙鍝洪悽顖滃仦缁傛帡鏁冮崒娑樻疅闂侀潧顦崕鏉壳庨鈧?
//        Shop shop = queryWithPassThrough(id);
        //婵犵數濮烽弫鎼佸磻濞戙垺鍋ら柕濞у啫鐏婇悗骞垮劚濞诧箓寮冲鍕箚闁靛牆鎳忛崳褰掓煕閹寸姴孝闁宠鍨垮畷鎺戭潩椤撶偞娈橀梻浣虹帛閹稿爼宕曢柆宥嗙畳闂備焦瀵х换鍌炲箠鎼粹槅鍤堟繛宸簼閻撴瑩鏌ｉ幋鐑囦緵婵炲牊姊荤槐鎺楀焵椤掑嫬绀冩い蹇庣娴滈箖鎮峰▎蹇擃仾缂佲偓閳ь剙鈹戦悙鑼勾闁告柨绉撮銉╁礋椤栨氨顦板銈嗙墬缁嬫垿鍩€椤掆偓閻忔氨鎹㈠☉銏犵闁绘垵妫旈惀顏勵渻閵堝懐绠伴柣妤€妫濆畷銉ф喆閸曗晙绨婚梺鍝勫€搁悘婵嬪煕閺冨牊鐓?
//        Shop shop = queryWithMutex(id);
        //闂傚倸鍊搁崐鎼佸磹妞嬪孩顐介柨鐔哄Т绾惧鏌涘☉鍗炲福闁挎繂顦粻鎶芥煛閸愶絽浜惧銈嗗姌婵倝濡甸崟顖氱疀闁告挷鑳惰摫濠电偛鐡ㄩ崵搴ㄥ磹濠靛钃熸繛鎴欏灩鍞梺鎸庣箓閹冲酣鈥栨径鎰拺缂侇垱娲樺▍鍛存煕婵犲倹鍋ョ€殿喖顭烽崺鍕礃閵娧呯嵁闂備礁澹婇崑鍛崲閸屾稒鍙忛柕鍫濐槹閸嬬姵鎱ㄥ鍡楀箻闁瑰啿娲弻锛勪沪閸撗勫垱濡ょ姷鍋涘ú顓€佸Δ鍛＜闁挎柨澧介ˇ顖滅磽閸屾艾鈧兘鎮為敃鍌氳埞缂備焦眉缁诲棝鏌涢妷顔煎闁绘帒鐏氶妵鍕箳瀹ュ牆鍘￠梺鎰佸灡濞茬喖寮婚妸銉僵闁煎摜鏁搁崢閬嶆⒑闂堟稓澧曞Δ鐘虫倐瀵悂濡堕崶鈺冿紲?
//        Shop shop = queryWithLogicalExpire(id);
        // 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鍨鹃幇浣告櫊婵犵數濮甸懝楣冩偪閻愵剛绡€闂傚牊渚楅崕蹇涙煛?CacheClient 闂傚倷娴囬褍顫濋敃鍌︾稏濠㈣埖鍔曠粻鏍煕椤愶絾绀€缁炬儳娼″娲敆閳ь剛绮旈幘顔藉剹婵°倕鎳忛崑锝夋煙椤撶喎绗掑┑鈥虫健閹绠涢弴鐔告瘓闂佽鍠栫紞濠傜暦閸洦鏁傞柛鏇ㄥ幗椤曟绻濋悽闈涗粶闁绘鎸荤粋宥夊醇閺囩偟鐣洪梻浣哥仢椤戝棙鍒婇幘顔界厱婵炴垶锕銉︾箾閸噥娈滄慨濠冩そ閺屽懘鎮欓懠璺侯伃婵犫拃灞界仭濞ｅ洤锕畷锝嗗緞婵犲嫷鍎屾繝鐢靛仜濡酣宕规禒瀣ㄢ偓渚€寮撮姀鈩冩珳闂佺硶鍓濆ú姗€宕径鎰拻濞达綀顫夐崑鐘绘煕鎼搭喖鐏︾€规洘绻傞鍏煎緞婵犲啯袣闂備胶顭堥張顒勫春閸愩剮锝夊醇閵夛妇鍘甸梺缁樺姦閸撴瑩鍩㈤弴銏＄厱婵炲棗绻愰顏勄庨崶褝韬柟宕囧Х閹瑰嫰宕崟顒€鍔掔紓鍌氬€风欢锟犲窗濡も偓閻ｆ繄绮欑捄銊︽缂備礁顑嗛娆忋€掓繝姘仯闁搞儺浜滅槐锕€顭跨憴鍕婵☆偄鎳橀、鏇㈠閳ユ剚妲辩紓鍌欑椤戝懘鎮ч幘宕囨殾闁硅揪绠戠粻濠氭偣閸ャ劌绲荤€殿喚鍏樺Λ鍛搭敃閵忊€愁槱濠电偛寮堕悧婊呭垝?
        Shop shop = cacheClient.queryWithLogicalExpireAndPassThrough(
                RedisConstants.CACHE_SHOP_KEY,
                RedisConstants.LOCK_SHOP_KEY,
                id,
                Shop.class,
                shopId -> query()
                        .eq("id", shopId)
                        .eq("status", 1)
                        .eq("audit_status", AuditStatusEnum.PASS.getCode())
                        .one(),
                RedisConstants.CACHE_SHOP_TTL,
                TimeUnit.MINUTES
        );
        //闂傚倸鍊搁崐鎼佸磹妞嬪孩顐介柨鐔哄Т绾惧鏌涘☉鍗炲福闁挎繂顦粻鎶芥煛閸愶絽浜惧銈嗗姌婵倝濡甸崟顖氱疀闁告挷鑳惰摫濠电偛鐡ㄩ崵搴ㄥ磹濠靛钃熸繛鎴欏灩鍞梺鎸庣箓閹冲酣鈥栨径鎰拺缂侇垱娲樺▍鍛存煕婵犲倹鍋ユ鐐插暙閻ｏ繝骞嶉搹顐も偓璇测攽閻愬弶顥為柛銊ф暬瀹曞搫鐣濋崟顑芥嫼闂佸憡绺块崕鍗炵摥闂備胶顢婂▍鏇㈠礉濡も偓鍗遍柟鐗堟緲缁犲鎮归崶顏勭毢妞ゆ梹甯￠幃妤冩喆閸曨剛顦ュ┑鐐额嚋缁犳捇鐛径瀣檮闁告稑艌閹锋椽姊洪崨濠勨槈闁挎洏鍊濋幃妯绘綇閳规儳浜鹃悷娆忓缁€鍫ユ煛娴ｅ壊鐓肩€?婵犵數濮烽弫鎼佸磻閻樿绠垫い蹇撴缁€濠囨煃瑜滈崜姘辨崲濞戞瑥绶為悗锝庡亞椤︿即鎮楀▓鍨珮闁稿锕ユ穱濠囧醇閺囩偞銇濇繛杈剧到閹测€斥枍婵犲洦鈷掑〒姘ｅ亾婵炰匠鍥佸洭顢橀姀鐘碉紵闂佸憡顨堥崕鎰板吹濡ゅ懏鐓涢柛鎰╁妼閳ь剝宕垫竟鏇㈠礂缁楄桨绨婚梺瑙勫閺呮盯鍩€椤掍緡娈旈柨鏇樺灮缁晝鈧鍚媏Client
//        Shop shop = cacheClient.queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class,this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null) {
            return null;
        }
        if (!isVisibleShop(shop)) {
            return null;
        }
        ShopVO shopVO = convertToShopVO(shop);
        // 濠电姷鏁告慨鐑藉极閹间礁纾婚柣鎰惈缁犳壆绱掔€ｎ偒鍎ラ柛銈嗘礋閺屾盯顢曢敐鍡欘槰闂佺顑呴崐鍧楀箖濡ゅ懏鏅查幖绮光偓宕囶唹濠电偛鐡ㄧ划宀€绱炴繝鍥ц摕闁绘梻鍘х粻姘辨喐瀹ュ姹查弶鍫涘妸娴滄粓鏌曢崼婵囧櫤闁搞倕娲弻鈥崇暆鐎ｎ剛袦闂佹寧绻勯崑銈夈€佸Δ鍛劦妞ゆ帊妞掔换鍡椕归悩宸剱闁绘挻娲熼弻宥夊传閸曨偀鍋撶拠瑁佹椽骞橀鐣屽幍濡炪倖鐗楅懝楣冨汲閿濆洠鍋撶憴鍕闁哥姵鐗犻妴渚€寮崼婵嗚€垮┑鐐村灦閸╁啴宕戦幘瓒佹椽顢旈崨顏呭闂備礁鎲＄粙鎴︽晝閵夆晜鍋傞柣鏂垮悑閻撴洟鏌￠崒姘变虎妞ゆ帇鍨介弻鐔哥瑹閸喖顫囧Δ鐘靛仦閻楁骞忛崨鏉戜紶闁靛／鍕啅濠电姷鏁搁崑鐐哄箰婵犳碍鍤屽Δ锝呭暞閺呮繈鏌ㄩ弬鍨挃闁绘粎绮穱濠囧Χ閸涱喖娅ら梺缁樻尰閻╊垶寮诲鍫闂佸憡鎸鹃崰鏍偘椤曗偓瀹曞崬鈽夊▎蹇庣綍闂備礁澹婇崑鎺楀磻閸涙澶愬棘鎼存挻鏂€闂佺粯鍔栧娆撴倶閿曞倹鐓欑紒瀣閸熺偞銇勯妸锝呭姦妤犵偞鐗楅幏鍛村传閵壯冨箑闂佽崵鍠愮划宀€绮旇ぐ鎺戞槬?
        isShopStared(shopVO);
        isShopFollowed(shopVO);
        return shopVO;
    }

    /**
     * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佹悶鍎洪崜娆戝瑜版帗鐓涚€广儱楠搁獮鏍煢閸愵亜鏋涢柡灞剧洴婵＄兘顢欓懡銈嗘濠电偛鐡ㄧ划宀€绱炴繝鍥ц摕闁绘梻鍘х粻姘辨喐瀹ュ姹查弶鍫涘妸娴滄粓鏌曢崼婵囧櫤闁搞倕娲弻鈥崇暆鐎ｎ剛袦闂佹寧绻勯崑銈夈€佸Δ鍛劦妞ゆ帊妞掔换鍡椕归悩宸剱闁绘挻娲熼弻宥夊传閸曨偀鍋撶拠瑁佽櫣鈧數纭堕崑鎾斥枔閸喗鐏嗛梺鍛婎殕婵炲﹪濡存担绯曟婵妫欓崓闈涱渻閵堝棙灏甸柛瀣⊕閺呰埖銈ｉ崘鈺冨幗闁瑰吋鐣崐銈咁焽閹扮増鐓熼柍鈺佸暞缁€鍫澝瑰鍜佺劸闁宠閰ｉ獮瀣攽閸℃瑤鍠婇梻鍌欐祰濞夋洟宕伴幘瀛樺弿閻庨潧鎽滅壕濂告煃閸濆嫭鍣洪柣鎾寸懄閵囧嫰寮介妸褏鐓€濡炪倧绲介崥瀣Φ閸曨垰唯闁挎洍鍋撻柣蹇撶摠椤ㄣ儵鎮欑€电鈷堥梺閫炲苯澧紒瀣浮閺佸啴鍩℃担鍙夌亖濠电姴锕ら悧濠囨偂?
     *
     * @param shopVO 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ョ紓鍌氬€风拋鏌ュ磻閹炬番浜滈柕濞у嫭姣堝┑顔硷攻濡炰粙鐛幇顓熷劅闁挎繂鍊归～宥夋⒒娴ｈ櫣甯涙い銊ユ瀹曟繂鈻庨幘鎵佸亾娴ｇ硶鏋庨柟鎯у暱瀹撳棝姊虹紒姗嗙劷闁轰焦鎮傞弫宥夋倷閻戞ǚ鎷哄┑顔炬嚀濞层倝鎮橀濮愪簻妞ゆ劧绲块崐纭塧red闂傚倸鍊峰ù鍥敋瑜忛幑銏ゅ箳濡も偓绾惧鏌ｉ弮鍌氬付缁炬儳顭烽弻锝夊箛椤掍焦鍎撻梺缁樺笒閻忔岸濡甸崟顖氱妞ゆ挾鍋涢～鈺冪磽娴ｅ壊妲稿褍娴烽崚?
     */
    private void isShopStared(ShopVO shopVO) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            //闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢敂钘変罕濠电姴锕ょ€氼噣銆呴弻銉︾厽闁归偊鍘鹃妶鎾煛鐎ｎ亞效闁哄本娲樺鍕醇濠靛柈鈺傜箾?婵犵數濮烽弫鎼佸磻閻愬搫鍨傞柛顐ｆ礀缁犱即鏌涘┑鍕姢闁活厽鎹囬弻娑㈩敃閿濆棛顦ラ梺鍝勵儎缁舵岸寮婚弴銏犻唶婵犲灚鍔栫瑧闂備浇妫勯崯浼村窗閺嶎厼钃熼柡鍥╁枎缁剁偞淇婇婊冨付閻㈩垬鍎靛娲箹閻愭祴鍋撻弴鐘亾濮橆偄宓嗙€殿喖顭烽弫鎾绘偐閼碱剦妲归梻鍌氬€搁悧濠勭矙閹惧顩烽柍鍝勬噺閳锋垿鎮归崶锝傚亾瀹曞洣鎴风紓鍌欐祰閸╂牠鎳濇ィ鍐炬晪闁挎繂顦儫闂佸疇妗ㄩ懗鍫曟偩妤ｅ啯鈷戠紓浣癸供濞堟棃鏌ｅΔ浣虹煀閾?
            shopVO.setIsStared(false);
            return;
        }
        //闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯妞ゆ棁宕甸弳妤佺箾鐎涙鐭婄紓宥咃躬瀵鎮㈤悡搴ｇ暰閻熸粌绉瑰铏綇閵婏絼绨婚梺闈涚墕閹冲繘宕甸崶顒佺厸鐎光偓鐎ｎ剛袣濡炪倧绠掗崺鏍Φ閸曨垰鍗抽柛鈩冾殕閸ｇ晫绱掗埀顒佺節閸曘劌浜炬鐐茬仢閸旀碍銇勯敂鍨祮鐎规洘鍨挎俊鎼佸煛閸屾粌甯惧┑鐘垫暩婵鎹㈤幒妤佸仼闂侇剙绉甸悡?
        Long userId = user.getId();
        StarDTO starDTO = new StarDTO();
        starDTO.setUserId(userId);
        starDTO.setSourceId(shopVO.getId());
        starDTO.setSourceType(GlobalBizTypeEnum.SHOP.getCode());
        //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佹悶鍎洪崜娆戝瑜版帗鐓涚€广儱楠搁獮鏍煢閸愵亜鏋涢柡灞剧洴婵＄兘顢欓懡銈嗘濠电偛鐡ㄧ划宀€绱炴繝鍥ц摕闁绘梻鍘х粻姘辨喐瀹ュ姹查弶鍫涘妸娴滄粓鏌曢崼婵囧櫤闁搞倕娲弻鈥崇暆鐎ｎ剛袦闂佹寧绻勯崑銈夈€佸Δ鍛劦妞ゆ帊妞掔换鍡椕归悩宸剱闁绘挻娲熼弻宥夊传閸曨偀鍋撶拠瑁佽櫣鈧數纭堕崑鎾斥枔閸喗鐏嗛梺鍛婎殕婵炲﹪濡存担绯曟婵妫欓崓闈涱渻閵堝棙灏甸柛瀣⊕閺呰埖銈ｉ崘鈺冨幗闁瑰吋鐣崐銈咁焽閹扮増鐓熼柍鈺佸暞缁€鍫澝瑰鍜佺劸闁宠閰ｉ獮瀣攽閸℃瑤鍠婇梻鍌欐祰濞夋洟宕伴幘瀛樺弿閻庨潧鎽滅壕濂告煃閸濆嫭鍣洪柣鎾寸懄閵囧嫰寮介妸褏鐓€濡炪倧绲介崥瀣Φ?
        Boolean isStared = remoteStarService.isStar(starDTO);
        shopVO.setIsStared(isStared);
    }
    /**
     * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佹悶鍎洪崜娆戝瑜版帗鐓涚€广儱楠搁獮鏍煢閸愵亜鏋涢柡灞剧洴婵＄兘顢欓懡銈嗘濠电偛鐡ㄧ划宀€绱炴繝鍥ц摕闁绘梻鍘х粻姘辨喐瀹ュ姹查弶鍫涘妸娴滄粓鏌曢崼婵囧櫤闁搞倕娲弻鈥崇暆鐎ｎ剛袦闂佹寧绻勯崑銈夈€佸Δ鍛劦妞ゆ帊妞掔换鍡椕归悩宸剱闁绘挻娲熼弻宥夊传閸曨偀鍋撶拠瑁佽櫣鈧數纭堕崑鎾斥枔閸喗鐏嗛梺鍛婎殕婵炲﹪濡存担绯曟婵妫欓崓闈涱渻閵堝棙灏甸柛瀣⊕閺呰埖銈ｉ崘鈺冨幗闁瑰吋鐣崐銈咁焽閹扮増鐓熼柍鈺佸暞缁€鍫澝瑰鍜佺劸闁宠閰ｉ獮瀣攽閸℃瑤鍠婇梻鍌欐祰濞夋洟宕伴幘瀛樺弿閻庨潧鎽滈惌鍡涙煕瀹€鈧崑鐐烘偂閿濆鍙撻柛銉╊棑閸掓澘霉濠婂嫮绠為柡宀嬬節瀹曞崬螖閸愩劉鍙烘俊銈囧Х閸嬬偤鎮у鍫濈劦妞ゆ帊鑳堕埊鏇㈡煥閺囨娅呴柍缁樻尭椤劑宕奸悢鍝勫箥?
     *
     * @param shopVO 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ョ紓鍌氬€风拋鏌ュ磻閹炬番浜滈柕濞у嫭姣堝┑顔硷攻濡炰粙鐛幇顓熷劅闁挎繂鍊归～宥夋⒒娴ｈ櫣甯涙い銊ユ瀹曟繂鈻庨幘鎵佸亾娴ｇ硶鏋庨柟鎯у暱瀹撳棝姊虹紒姗嗙劷闁轰焦鎮傞弫宥夋倷閻戞ǚ鎷哄┑顔炬嚀濞层倝鎮橀濮愪簻妞ゆ劧绲鹃妶绌檒lowed闂傚倸鍊峰ù鍥敋瑜忛幑銏ゅ箳濡も偓绾惧鏌ｉ弮鍌氬付缁炬儳顭烽弻锝夊箛椤掍焦鍎撻梺缁樺笒閻忔岸濡甸崟顖氱妞ゆ挾鍋涢～鈺冪磽娴ｅ壊妲稿褍娴烽崚?
     */
    private void isShopFollowed(ShopVO shopVO) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            //闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢敂钘変罕濠电姴锕ょ€氼噣銆呴弻銉︾厽闁归偊鍘鹃妶鎾煛鐎ｎ亞效闁哄本娲樺鍕醇濠靛柈鈺傜箾?婵犵數濮烽弫鎼佸磻閻愬搫鍨傞柛顐ｆ礀缁犱即鏌涘┑鍕姢闁活厽鎹囬弻娑㈩敃閿濆棛顦ラ梺鍝勵儎缁舵岸寮婚弴銏犻唶婵犲灚鍔栫瑧闂備浇妫勯崯浼村窗閺嶎厼钃熼柡鍥╁枎缁剁偞淇婇婊冨付閻㈩垬鍎靛娲箹閻愭祴鍋撻弴鐘亾濮橆偄宓嗙€殿喖顭烽弫鎾绘偐閼碱剦妲归梻鍌氬€搁悧濠勭矙閹惧顩烽柍鍝勬噺閳锋垿鎮归崶锝傚亾瀹曞洣鎴风紓鍌欐祰閸╂牠鎳濇ィ鍐╁仼鐎瑰嫭瀚堥弮鍫濆窛妞ゆ棁妫勯崝鎺撲繆閻愵亜鈧牠宕濊瀵板﹪骞嗚閺?
            shopVO.setIsFollowed(false);
            return;
        }
        //闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯妞ゆ棁宕甸弳妤佺箾鐎涙鐭婄紓宥咃躬瀵鎮㈤悡搴ｇ暰閻熸粌绉瑰铏綇閵婏絼绨婚梺闈涚墕閹冲繘宕甸崶顒佺厸鐎光偓鐎ｎ剛袣濡炪倧绠掗崺鏍Φ閸曨垰鍗抽柛鈩冾殕閸ｇ晫绱掗埀顒佺節閸曘劌浜炬鐐茬仢閸旀碍銇勯敂鍨祮鐎规洘鍨挎俊鎼佸煛閸屾粌甯惧┑鐘垫暩婵鎹㈤幒妤佸仼闂侇剙绉甸悡?
        Long userId = user.getId();
        FollowDTO followDTO = new FollowDTO();
        followDTO.setUserId(userId);
        followDTO.setSourceId(shopVO.getId());
        followDTO.setSourceType(GlobalBizTypeEnum.SHOP.getCode());
        Boolean isFollowed = remoteFollowService.isFollowed(followDTO);
        shopVO.setIsFollowed(isFollowed);
    }
    /**
     * 缂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧湱鎲搁悧鍫濈瑨缂佺姳鍗抽弻鐔兼⒒鐎电濡介梺绋款儍閸婃繈寮婚弴鐔虹鐟滃秹骞婇幇鐗堝亗闁哄啫鐗婇埛鎴犵磼婢跺﹥顥滈悗姘ュ妽缁傛帒顭ㄩ崼鐔哄幈闁诲函缍嗛崑鍛暦閸曨厾纾奸柣妯虹－閳藉鎮▎鎾寸厵缂佸娼￠妤佺箾閹碱厼鏋ら柍褜鍓濋～澶娒哄鈧弫鍐閵堝懐顔愰悷婊呭鐢晠寮崘顔界叆婵犻潧妫欓幖鎰殽閻愯尙校缂佺粯绻勯崰濠偽熼崫鍕ㄦ嫟婵＄偑鍊戦崝宀勫箠濮椻偓楠炲啳顦归柟顔界懇瀹曞綊顢曢锝囩◥婵犵數鍋炲娆撳触鐎ｎ亶鐒芥繛鍡樻尰鐎氬懘鏌ｉ弬鍨倯闁绘挸绻愰…鍧楁嚋濞堣法鍔烽梺鍛娚戦惄顖炲蓟濞戙垹围闁告侗鍙庢导鍐ㄎ旈悩闈涗沪闁绘绮撻崺鈧い鎺嶈兌閳洟鏌ㄩ弴妤佹珔闁崇粯鎸搁…銊╁醇閻斿搫骞嶉梻浣风串缁蹭粙寮甸鍕辈妞ゆ帒瀚悡鐔哥箾閹存繂鑸规繛鍛Ф閳ь剝顫夊ú妯煎垝瀹ュ绠柛娑欐綑娴肩娀鏌涢弴鐐典粵鐞氣晠姊婚崒娆愮グ妞ゆ泦鍛亾濞戞帗娅囩紒顔界懇楠炴帒顪冮悜鈺佷壕闁挎洖鍊搁悙濠冦亜閹哄棗浜鹃梺缁樻尰閿曘垽寮婚垾鎰佸悑閹肩补鈧磭顔愮紓浣瑰劤婢т粙骞婇幘璇茬厴闁硅揪绠戠壕鍏兼叏濮楀棗骞栭柡鍡楃墕闇夋繝濠傚閳藉鏌嶇憴鍕伌妞ゃ垺鐟╅幃鎯х暆閳ь剛妲愭导瀛樷拺闁硅偐鍋涙俊鍏笺亜椤撶姴鍘寸€规洘妞介幃娆徝圭€ｎ偒娼旈梻渚€娼ф蹇曟閺囥垹绀?
     *
     * @param id 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ョ紓鍌氬€风拋鏌ュ磻閹剧繝绻?
     * @return 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ョ紓鍌氬€风拋鏌ュ磻閹炬番浜滈柕濞у嫭姣堝┑顔硷攻濡炰粙鐛幇顓熷劅闁挎繂鍊归～宥呪攽閻樼粯娑ч柛濠勬嚀椤繘鎳￠妶鍛亰濡炪倖鎸鹃崰搴ｆ閻愮儤鍊堕柣鎰問閻掓儳霉濠婂懎浜剧紒缁樼洴楠炲鎮滈崱鏇犳／婵＄偑鍊曠换鎰偓姘倐閹虫捇骞愭惔娑楃盎闂婎偄娲﹂幐濠氬闯閸濆娊鐟扳攦閸喒鍋撳┑瀣摕闁跨喓濮寸壕鍏肩箾閹寸儑渚涢柛銈咁儑缁辨挻鎷呴幓鎺嶅闂備線鈧偛鎳忛崕鐣妉l
     */
    public ShopVO queryWithPassThrough(Long id) {

        //婵犵數濮烽弫鎼佸磻濞戙埄鏁嬫い鎾跺枑閸欏繘鏌℃径瀣閻熸瑥瀚峰Σ褰掑箹濞ｎ剙鐒烘繛鑲╁枛濮婃椽骞愭惔銏╂闂佽桨绶￠崳锝呯暦閹达箑绠婚柡鍌樺劜椤秴鈹戦悙鍙夘棡闁荤喆鍔戦崺鈧い鎺嗗亾婵炵》绻濆璇测槈閵忊剝娅滈梺鎼炲劀閸愩劎顓洪梻鍌欑濠€閬嶅磻閵娾晛鍨傜憸鐗堝笚閸嬧晠鏌ㄩ悢鍝勑㈢紒鐙欏洦鍋ｉ柟顓熷笒婵′粙鏌涚€ｆ柨娲﹂埛鎺懨归敐鍛暈闁瑰弶鎮傞弻娑㈡偐閺屻儺鈧鎽堕悙瀵哥瘈闂傚牊渚楅崕蹇曠磼閻樿櫕銇濋柡宀嬬秮婵偓闁靛繆鏅濋崝鎼佹⒑閸濆嫷鍎庣紒鑸靛哺瀵鏁愰崨鍌涙閸┾偓妞ゆ帒瀚崑瀣煕閳╁啰鎳呴柣?
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = redisService.getCacheObject(key);
        //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佹悶鍎洪崜娆戝瑜版帗鐓涚€广儱楠搁獮鏍煢閸愵亜鏋涢柡灞剧洴婵＄兘顢欓悡搴交闂備礁鎲￠悷銉╂晝閵夆晛桅闁告洦鍨伴崘鈧梺闈浤涚仦鐐啇闂傚倷鑳堕…鍫燁殽閹间焦鏅濇い蹇撶墕缁犳牗淇婇妶鍌氫壕闂佸疇妫勯ˇ顖炲煝瀹ュ鏁囩憸宀€鑺辨繝姘叆?
        if (StrUtil.isNotBlank(shopJson)) {
            //闂傚倸鍊峰ù鍥敋瑜忛埀顒佺▓閺呮繄鍒掑▎鎾崇婵°倓鐒﹀▍鏂库攽閻樺灚鏆╁┑顕呭弮瀹曟垿骞樺ǎ顑跨盎闂婎偄娲﹂幐鐐櫠濞戙垺鐓涢柛婊€绀佹晶浼存煃瑜滈崜娆撳储濠婂牆纾婚柟鍓х帛閻撳啰鎲稿鍫濈婵炲棙鍨靛鏌ユ⒒娴ｄ警娼掗柛鏇炵仛閻ｅ墎绱撴担鍝勑ｅ┑鐐诧躬瀵寮撮悢椋庣獮闂佸壊鍋呯换鍌炩€栫€ｎ剛纾藉ù锝呮惈鏍￠梺缁樻惈缁绘繂顕?
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return convertToShopVO(shop);
        }
        //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佹悶鍎洪崜娆戝瑜版帗鐓涚€广儱楠搁獮鏍煢閸愵亜鏋涢柡灞剧洴婵＄兘顢欓悡搴交闁诲海鎳撻幉锛勬崲閸儱钃熼柣鏃囥€€閸嬫挸鈽夊▍顓т簽缁參鍩￠崨顔惧幈濠殿喗顨呭Λ妤佹櫠閹绢喗鐓涚€光偓鐎ｎ剛袦婵犵鍓濋幃鍌涗繆閻戣棄唯妞ゆ棁宕靛Λ顖氣攽閻樻鏆柍褜鍓欓崯璺ㄧ棯瑜旈弻鐔碱敊閻撳簶鍋撻幖浣瑰仼闁绘垼妫勫敮闂佸啿鎼崐鐟扳枍閸℃稒鈷戦柛蹇涙？閼割亪鏌涙惔銏㈡创闁轰礁鍊垮畷婊嗩槾闁挎稒绮撻弻锝堢疀閺囩偘鍝楁繝娈垮枔閸婃繈鐛崱娑橀唶闁靛濡囬崢?
        if (shopJson != null) {
            return null;
        }
        Shop shop = this.getById(id);
        if (shop == null) {
            //闂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸閻ゎ喗銇勯幇鈺佺労闁搞倖娲熼弻娑㈩敃閿濆棗顦╅梺杞扮濡瑧鎹㈠☉銏犵闁绘劕顕▓銈囩磼閹冪稏缂侇喗鐟╁濠氭偄閻撳海鐣鹃悷婊冪箻閺佸秴顓兼径瀣幍闂佷紮绲介懟顖氭毄缂傚倷娴囨ご鍝ユ暜閻愬顩烽柨鏇炲€归崵宥夋煏婢跺牆鍔欐繛锝庡櫍濮?闂傚倸鍊峰ù鍥敋瑜忛幑銏ゅ箛椤旇棄搴婇梺褰掑亰閸庨潧鈽夊Ο閿嬵潔闂侀潧绻掓慨鐑藉储閹绢喗鈷戦柛婵嗗閳诲鏌涘Ο鍨汗濠㈣娲濋妵鎰板箳閹捐泛骞楅梻浣筋潐瀹曟ê鈻嶉弴銏犵闁挎繂顦伴悡娆撴煟閿濆懓瀚伴柍璇茬墦閺岋紕浠﹂崜褜鐏辩紓浣哄У閻╊垶寮幇鏉垮耿闁宠　鍋撻柟瀵割煫dis
            redisService.setCacheObject(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //闂傚倸鍊峰ù鍥敋瑜忛埀顒佺▓閺呮繄鍒掑▎鎾崇婵°倓鐒﹀▍鏃堟⒒閸屾瑧顦﹂柟璇х節閹兘濡烽埡浣圭€梺绋跨▉濞呮攰s
        redisService.setCacheObject(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return convertToShopVO(shop); // Convert Shop to ShopVO before returning

    }

    //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹濠德板€曢崯浼存儗濞嗘挻鐓欓悗鐢殿焾鍟哥紒鎯у綖缁瑩寮婚悢鐓庣畾鐟滃秹銆傚畷鍥╂／闁告瑣鍎抽惌娆撴煛瀹€鈧崰鏍嵁閸℃凹妲鹃梺鎸庣☉缁夊綊寮婚悢鍏煎仼閻忕偛褰ㄩ妷褏纾奸弶鍫涘妼濞搭喗顨ラ悙鏉戠伌妤犵偛娲、姗€鎮╅鐟颁壕濠电姴娲﹂埛鎴︽倵閸︻厼孝闁告艾缍婇弻娑樜熼悩鎻掝仾婵?
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 闂傚倸鍊搁崐鎼佸磹妞嬪孩顐介柨鐔哄Т绾惧鏌涘☉鍗炲福闁挎繂顦粻鎶芥煛閸愶絽浜惧銈嗗姌婵倝濡甸崟顖氱疀闁告挷鑳惰摫濠电偛鐡ㄩ崵搴ㄥ磹濠靛钃熸繛鎴欏灩鍞梺鎸庣箓閹冲酣鈥栨径鎰拺缂侇垱娲樺▍鍛存煕婵犲懎鍚规俊鍙夊姍楠炴帡骞嬮弮鈧～宥呪攽閻愬弶顥為柛銊ф暬閸╂盯宕奸妷锔规嫼闂佸憡绻傜€氼參藟閻愮儤鐓曢柡鍐ｅ亾闁荤喆鍎甸、姘舵晲婢舵ɑ鏅㈤梺绋挎湰缁嬫垿寮搁崒鐐粹拺闁告稑锕ョ粈鈧梺璇茬箲瀹€绋款嚕椤愶附鍋ㄩ柛娑樑堥幏?
     * 闂傚倸鍊搁崐鐑芥倿閿曗偓椤啴宕归鍛姺闂佺鍕垫當缂佲偓婢舵劖鍊甸柨婵嗛婢ф彃鈹戦鎸庣彧闁靛洤瀚伴獮鎺楀箣濠靛啫浜剧憸鐗堝笒缁愭鏌涢埄鍐姇闁绘挶鍎茬换婵嬫濞戞瑯妫￠梺闈╃到缂嶅﹪寮婚敍鍕ㄥ亾閿涘崬鍟╂竟鏇㈡⒒閸屾艾鈧绮堟笟鈧獮鏍敃閿旂粯鏅為梺鍛婃处閸樹粙顢曢懞銉﹀弿婵☆垰鐏濋悡鎰版煕鐎ｎ剙鏋戦柕鍥у瀵潙螖閳ь剚绂嶆ィ鍐┾拺缂備焦眉缁堕亶鏌涢悩鍐插摵妤犵偞鍨垮畷鐔碱敍濮橆偆鐐婇梻浣告啞濞诧箓宕㈡總鍛婂剹婵犻潧妫岄弨浠嬫煟閹邦厽缍戦柣蹇ョ畵閺岀喖宕ㄦ繝鍐ㄢ偓鎰亜閵忥紕澧电€规洘鍎奸ˇ顕€鏌＄€ｎ偅顥堥柡宀€鍠愬蹇涘礈瑜忛弳鐘绘⒑鐠囪尙绠烘繛鍛礈閹广垹鈹戦崶鈺冪槇闂佺鏈粙鎴濃枍瑜庣换婵嬪閿濆棛銆愰柣搴㈢煯閸楁娊鐛崘銊庢棃鍩€椤掑嫬鐓″璺号堥弸宥嗐亜閹炬鍊块崑妤呮倵鐟欏嫭绀堥柡浣割煼閵嗕礁鈻庨幘宕囶槶閻熸粌绻橀幃锟犲籍閸喓鍘甸梺缁樻尭濞寸兘骞楅悩缁樼厵闁告稑锕ョ亸锔锯偓瑙勬礃閸ㄥ潡鐛鈧顒勫Ψ閿旇姤婢戦梻鍌欑閹测€趁鸿箛娑樼閻忕偛澧介々鎻捨旈敐鍛殲闁绘挻娲熼幃姗€鎮欓弶鎴狀槰婵犮垼顫夐…鍥╂閹烘挾鐟归柛銉戝嫮浜梻鍌氭搐椤︾敻寮婚妸銉㈡斀闁糕剝鐟ラ·鈧紓浣鸿檸閸樺吋鏅舵惔锝嗩潟闁圭儤顨呯粻鐔兼倵閿濆簼鎲炬繛宸弮濮婃椽鏌呴悙鑼跺濠⒀屽灣缁辨帞鈧綆鍋勫ù顕€鏌嶉妷顖滅暤鐎规洖銈告慨鈧柍銉ュ暱缁ㄣ儵姊绘担鍛婂暈闁告棑绠撳畷浼村冀椤愮喎浜炬慨姗嗗墻濡偓闂佸搫鐭夌紞浣割嚕閹绢喗鍊锋繛鏉戭儏娴滈箖鏌ゆ慨鎰偓妤呮儗閸℃鐔嗛柤鎼佹涧婵牓鏌ｉ幘瀛樼闁绘搩鍋婂畷鍫曞Ω閿曗偓閺嗘绻濋埛鈧鍥ㄥ仹缂備浇椴哥敮鐐垫閹烘嚦鐔兼惞闁稓绀夐梺璇叉唉椤煤閺嶎厽鍋夊┑鍌滎焾缁犳牗绻涢崱妯虹亶闁稿鎸搁埥澶娾枎濞嗗繐褰嗙紓鍌欐祰妞村摜鍒掗幘鎰佹綎闁绘垶蓱婵粓鏌熷畡鎷屽闁告ǚ鍓濈换婵嬪煕閳ь剟宕熼崹顐ゆ殽闂備椒绱徊浠嬫倶濮樿京绠旈柣鏃傚帶閻掑灚銇勯幒鎴濐仼闁?
     * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐叉疄婵°倧绲介崯顐も偓姘槹閵囧嫰骞掗崱妞惧婵＄偑鍊ゆ禍婊堝疮閺夋垹鏆﹂柟鐑樺焾濞尖晠鏌ｉ幘鍐差劉妞ゆ挸娼″缁樼瑹閳ь剟鍩€椤掑倸浠滈柤娲诲灡閺呭墎鈧數纭堕崑鎾舵喆閸曨剛锛橀梺绋挎捣閺佸濡存担绯曟婵妫欓崓闈涱渻閵堝棙灏甸柛瀣姍瀹曟垿骞樼紒妯轰缓闂佸憡绋戦敃锕傚储閸楃偐鏀介柣鎰级椤ョ偤鏌涢弮鈧ú鐔绘＂闁诲函缍嗛崰妤呮偂閺囥垺鐓忛柛顐ｇ箖閸ｆ椽鏌熼惂鍝ユ偧缂佽鲸甯￠、娆撴偩鐏炴儳娅橀梻渚€鈧偛鑻晶鍓х磽瀹ュ懏顥炵紒鍌氱Ч閹瑩妫冨☉杈棥闂備礁鍚嬫禍浠嬪磿閺屻儱鐭楅煫鍥ㄦ⒒缁♀偓闂傚倸鐗婄粙鎴︻敂閳哄懏鐓曢柡鍐ｅ亾缂侇喗鎹囧濠氭晲婢跺﹦顔撻梺鍛婃处閸橀箖鏁嶉悢鍏尖拺鐟滅増甯╁Λ鎴濃攽閻愨晛浜惧┑鐘愁問閸犳帡宕戦幘缁樷拺闁哄倶鍎插▍鍛存煕閻曚礁鐏︽い銏＄懃椤撳吋寰勭€Ｑ勫缂備胶铏庨崢濂稿箯鐎ｎ喖鍚归柛鎰靛枟閻撴洟鏌ｅΟ铏癸紞濠⒀囦憾閺屻倕煤椤忓啯鍠氶梺鍝勮嫰濡顢樻總绋跨倞鐟滃繘濡堕敃鍌涒拻濞达綀妫勬禍瑙勩亜椤撶偟澧﹂柟顔矫～婵囨綇閳哄偊绱℃俊鐐€栭悧婊堝磻閻愮儤鍋傛繛鎴欏灪閻撴洟鎮橀悙鎻掆挃闁宠棄顦伴妵鍕Ψ閵夘喖鍓崇紓浣介哺鐢繝鐛径灞稿亾閿濆簼鎲炬繛宸弮濮婃椽鏌呴悙鑼跺濠⒀屽灣缁辨帞鈧綆鍋勫ù顕€鏌℃担绋挎殻妞ゃ垺娲熸俊鍫曞幢閳哄倻绋愬┑鐘垫暩婵炩偓婵炰匠鍏犳椽濡搁埞搴撳亾閸涘瓨鍊婚柦妯侯槺椤旀劕鈹戦悜鍥╃У闁告挻鐟╅崺鈧い鎺嶇婵秶鈧娲橀崹鍧楃嵁濮椻偓瀵敻宕归銏狀€忛梻鍌欑閸氬绮婇幘顔肩柧婵犻潧顑愰弫濠囨煛閸愩劎澧涢柣鎾跺枛閺岋絽螣閸濆嫮楠囬梺娲诲幗椤ㄥ﹪寮婚妶澶婄闁肩⒈鍓欓悡鐔兼⒑閸濆嫯瀚伴柣妤€绻橀崺鈧い鎺戝濞懷囨煙閼恒儳鐭屽瑙勬礃缁轰粙宕ㄦ繝鍕箺闂備礁鎼崯顐︽偋婵犲洤鏋侀梺顒€绉甸悡鏇㈡倵閿濆骸澧柍璇茬墦閺岋紕浠﹂崜褉妲堝銈庡亝缁诲牓銆佸鈧幃娆忣啅椤旂晫绋堥梻鍌氬€风欢姘焽閼姐倕绶ら柟顖嗗本瀵岄梺鍏间航閸庢壆鎹?
     *
     * @param id 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅?ID
     * @return 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭罕闂佸搫娲㈤崹鍦不閻樿绠规繛锝庡墮婵¤偐绱掗悩鍐插摵闁哄本鐩、鏇㈡偐閹绘帒顫氶梻浣虹帛鐢帡鈥﹂悜钘夎摕闁跨喓濮寸粈瀣亜閹扳晛鈧顢欐繝鍥ㄧ厽闁靛繆鏅涢悘鐘充繆椤愶絿绠撴い鏇秮瀹曞ジ寮撮悙闈涘箰濠电偠鎻徊鍧楀箠閹惧嚢鍥煛娴ｅ弶鏂€闂佺粯锕╅崑鍕妤ｅ啯鈷戦悹鎭掑妼閺嬫柨鈹戦鑺ュ唉鐎殿喖鎲＄粭鐔煎焵椤掑嫬钃熼柨婵嗘啒閺冨牆鐒垫い鎺戝閸嬪鏌涢埄鍐噮闁活厼鐗撻弻銊╁即閻愭祴鍋撹ぐ鎺撳亗闁绘柨鍚嬮悡蹇撯攽閻愯尙浠㈤柛鏃€纰嶉妵鍕疀婵犲啯鐝栫紓浣介哺鐢剝淇婂宀婃Ъ闂佸搫妫崜鐔煎蓟閿涘嫪娌柣鏃堟敱閹兼劕霉閼测晛鈻堥柡灞剧洴瀵挳濮€閳╁啴鏁繝鐢靛仜閻楀﹪骞冮崒鐐茶摕?
     */
    public Shop queryWithLogicalExpire(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        //婵犵數濮烽弫鎼佸磻濞戙埄鏁嬫い鎾跺枑閸欏繘鏌℃径瀣閻熸瑥瀚峰Σ褰掑箹濞ｎ剙鐒烘繛鑲╁枛濮婃椽骞愭惔銏╂闂佽桨绶￠崳锝呯暦閹达箑绠婚柡鍌樺劜椤秴鈹戦悙鍙夘棡闁荤喆鍔戦崺鈧い鎺嗗亾婵炵》绻濆璇测槈閵忊剝娅滈梺鎼炲劀閸愩劎顓洪梻鍌欑濠€閬嶅磻閵娾晛鍨傜憸鐗堝笚閸嬧晠鏌ㄩ悢鍝勑㈢紒鐙欏洦鍋ｉ柟顓熷笒婵′粙鏌涚€ｆ柨娲﹂埛鎺懨归敐鍛暈闁瑰弶鎮傞弻娑㈡偐閺屻儺鈧鎽堕悙瀵哥瘈闂傚牊渚楅崕蹇曠磼閻樿櫕銇濋柡宀嬬秮婵偓闁靛繆鍓濆В鍕磼閹冪稏缂侇喗鐟╁濠氭偄閻撳海鐣鹃悷婊冪箻閺佸秴顓兼径瀣幍?
        String shopJson = redisService.getCacheObject(key);
        //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佹悶鍎洪崜娆戝瑜版帗鐓涚€广儱楠搁獮鏍煢閸愵亜鏋涢柡灞剧洴婵＄兘顢欓悡搴交闂備礁鎲￠悷銉╂晝閵夆晛桅闁告洦鍨伴崘鈧梺闈浤涚仦鐐啇闂傚倷鑳堕…鍫燁殽閹间焦鏅濇い蹇撶墕缁犳牗淇婇妶鍌氫壕闂佸疇妫勯ˇ顖炲煝瀹ュ鏁囩憸宀€鑺辨繝姘叆?
        if (StrUtil.isBlank(shopJson)) {
            //婵犵數濮烽弫鎼佸磻閻愬搫鍨傞柛顐ｆ礀缁犱即鏌涘┑鍕姢闁活厽鎹囬弻鐔虹磼閵忕姵鐏嶉梺绋款儍閸婃繈寮婚弴鐔虹闁绘劦鍓氶悵鏃堟⒑閸︻収鏀伴柛鈺傜墵婵＄敻宕熼锝嗘櫍闂佺粯妫冮ˉ鎾活敄閸屾粎纾藉ù锝嗗絻娴滅偓绻濋姀锝呯厫闁告梹鐗犻幃锟犳偄閸忚偐鍘撻柡澶屽仦婵粙宕楀畝鍕厵闁伙絽鑻埢鍫ユ煙椤旇崵鐭欐い銏＄☉椤繃娼忛妸锕€鏋涘┑鐘垫暩閸嬬喖宕㈣閳ワ箑鐣￠柇锕€娈ㄥ銈嗗笒鐎氼剛绮绘导鏉戠閺夊牆澧界粔鍨繆瀹割喖澧扮紒?
            return null;
        }
        //闂傚倸鍊搁崐鐑芥嚄閸洏鈧焦绻濋崒妤佺亙濠电偞鍩堝鍧楀焵椤掆偓閹虫鐦紓浣圭〒閺呫劑姊婚崒娆戭槮闁圭⒈鍋婅棟闂侇剙绉寸壕鍧楁煙閻楀牊绶茬紒鈧崼鐔稿弿婵妫楁晶浼存煛閸☆厾鐣甸柡宀嬬磿娴狅妇鎷犻幓鎺戭潛缂傚倷鑳舵刊瀵哥礊娓氣偓瀵鎮㈤崗鑲╁姺闂佹寧娲嶉崑鎾愁熆瑜嶉…鐑藉蓟濞戙垹围闁搞儜鈧弸鍛存⒑?
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佹悶鍎洪崜娆戝瑜版帗鐓涚€广儱楠搁獮鏍煢閸愵亜鏋涢柡灞剧洴婵＄兘顢欓悡搴交闂備礁鎲￠悷銉╂晝閵夆晛桅闁告洦鍨伴崘鈧梺闈浤涚仦鐐啇闂傚倷鑳堕…鍫燁殽閹间焦鏅濇い蹇撳閸ゆ洟鏌涢锝嗙闁绘挻绋戦湁闁挎繂娲﹂崵鈧銈忚吂閺呮盯鍩為幋锔藉亹妞ゆ劦婢€婢?
        if (redisData.getExpireTime().isAfter(LocalDateTime.now())) {
            //闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢敂钘変罕濠电姴锕ょ€氼噣銆呴弻銉︾叆婵犻潧妫欐径鍕偓瑙勬礃閻擄繝寮诲☉銏犵疀闂傚牊绋掗悘鍫ユ煟鎼淬垼澹樻い锔炬暬瀵鏁愭径濠傚祮闂佺粯鍔栫粊鎾磻閹剧粯鍤掗柕鍫濇川閻掑吋绻濋姀锝呯厫闁告梹鐗犻幃锟犳偄閸忚偐鍘撻柡澶屽仦婵粙宕楀畝鍕厵闁伙絽鑻埢鍫ユ煙椤旇崵鐭欐い銏＄☉椤繃娼忛妸锕€鏋涘┑鐘垫暩閸嬬喖宕㈣閳ワ箑鐣￠柇锕€娈ㄥ銈嗗笒鐎氼剛绮绘导鏉戠閺夊牆澧界粔鍨繆瀹割喖澧存慨濠勭帛閹峰懘鎳為妷锝傚亾閸愵喗鐓犻悗鍦Т閻撴劙鏌￠崨鐗堢【閾绘牠鏌嶈閸撶喖骞?
            return shop;
        }
        //TODO 闂傚倸鍊风粈渚€骞栭位鍥敃閿曗偓閻ょ偓绻涢幋鐐╂（婵炲樊浜濋弲婵嬫煃瑜滈崜鐔煎极閸愵喖顫呴柕鍫濇濞呫垽姊虹紒姗嗙劸婵炲懏娲栭埢宥夊冀椤撶喎鈧敻鏌涜箛鎿冩Ц濞存粓绠栧娲焻閻愯尪瀚板褏澧楁穱濠囧矗婢跺﹤顫掗梺杞扮閸婂潡寮婚妸鈺婃晬婵﹩鍋勯ˉ姘節閻㈤潧浠﹂柛銊ョ埣閹兘鏌嗗鍡楁畬闂佺鍕垫畷闁绘挻娲熼弻銊モ攽閸℃銈╅梺缁樺笧閸嬫捇濡?
        //闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯闁挎繂鎳庨‖瀣渻閵堝倹娅嗛柣鎿勭節瀵鎮㈤崗鐓庘偓缁樹繆椤栨繍鍤欓梻澶婄Ч濮婅櫣鎷犻懠顒傤啋婵炲瓨绮忓▔娑綖?
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if (isLock) {
            //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭画闂佹寧娲栭崐褰掑磻鐎ｎ喗鐓熸俊顖涱儥閸ゆ瑧绱撴担鍙夋珚闁哄被鍔岄埞鎴﹀幢閳哄倐锔剧磽娴ｅ搫顎撶紓宥勭窔瀵鎮㈤崗鑲╁姺闂佹寧娲嶉崑鎾愁熆瑜滈崰妤呭Φ閸曨垼鏁冮柕鍫濇噳閺嬪懘姊洪崫鍕潶闁告柨鐭傞崺銉﹀緞婵炪垻鍠栧畷褰掝敋閸涱剛纾剧紓鍌氬€搁崐宄懊归崶銊ｄ粓闁归棿绀侀崹鍌炴煕椤垵鏋ら柡鍡畵閺屾洘寰勯崱妯荤彟闂佽桨绀佺粔鐢垫崲濠靛洨绡€闁稿本纰嶉悘鎾寸箾鐎电孝濠⒀呮櫕濡叉劙骞樼€涙ê顎撻柣鐔哥懃鐎氼噣宕ｉ崱妤婃富闁靛牆妫楅悘銉︾箾瀹割喖骞栭柣锝囧厴瀹曞綊顢曢敐鍛Τ闂備焦瀵х换鍌毭洪妸鈺傚亗?闂傚倸鍊风粈渚€骞栭位鍥敃閿曗偓閻ょ偓绻濇繝鍌涘櫧闁活厽鐟╅弻鈥愁吋鎼粹€崇闂侀€炲苯鍘哥紒鑸靛哺閻涱喚鈧綆鍠楅崑鎰版煟閵忋埄鏆滅紒杈ㄥ▕濮婄粯鎷呯粵瀣秷闂佺楠哥壕顓熺珶閺囥埄鏁囬柕蹇曞Х椤旀垿姊虹紒姗嗙劷缂侇噮鍨堕崺娑㈠箣閿旂晫鍘电紓鍌欑劍閿氱紒妞﹀應鍋撳☉娆戠畼缂?
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //闂傚倸鍊搁崐鎼佸磹閻戣姤鍊块柨鏇氶檷娴滃綊鏌涢幇鍏哥敖闁活厽鎹囬幃妤呮濞戞瑦鍠愮紒鎯у綖缁瑩寮婚悢鐓庣畾鐟滃秹銆傚畷鍥╂／闁告瑣鍎抽惌娆撴煛瀹€鈧崰鏍嵁閸℃凹妲鹃梺鎸庣☉缁夊綊寮?
                    this.saveHotShopRedis(id, 20L);
                } catch (Exception e) {

                } finally {
                    //闂傚倸鍊搁崐鎼佸磹閻戣姤鍊块柨鏇氶檷娴滃綊鏌涢幇闈涙灍闁哄懏绻堥弻鏇熷緞閸℃ɑ鐝斿┑鈽嗗亝閿氶柕鍡樺笒椤繈鏁愰崨顒€顥氶梻?
                    unLock(lockKey);
                }
            });
        }
        //闂傚倸鍊峰ù鍥敋瑜忛埀顒佺▓閺呮繄鍒掑▎鎾崇婵°倓鐒﹀▍鏃堟⒒閸屾瑧顦﹂柟璇х節閹兘濡烽埡浣圭€梺绋跨▉濞呮攰s
        return shop;

    }


    /**
     * 婵犵數濮烽弫鎼佸磻濞戙垺鍋ら柕濞у啫鐏婇悗骞垮劚濞诧箓寮冲鍕箚闁靛牆鎳忛崳褰掓煕閹寸姴孝闁宠鍨垮畷鎺戭潩椤撶偞娈橀梻浣虹帛閹稿爼宕曢柆宥嗙畳闂備焦瀵х换鍌炲箠鎼粹槅鍤堟繛宸簼閻撴瑩鏌ｉ幋鐑囦緵婵炲牊姊荤槐鎺楀焵椤掑嫬绀冩い蹇庣娴滈箖鎮峰▎蹇擃仾缂佲偓閳ь剙鈹戦悙鑼勾闁告柨绉撮銉╁礋椤栨氨顦板銈嗙墬缁嬫垿鍩€椤掆偓閻忔氨鎹㈠☉銏犵闁绘垵妫旈惀顏勵渻閵堝懐绠伴柣妤€妫濆畷銉ф喆閸曗晙绨婚梺鍝勫€搁悘婵嬪煕閺冨牊鐓熸い鎾跺枎閸濇椽鏌＄仦鍓ф创鐎殿喕绮欓幃浠嬫偨绾板鍚圭紓鍌氬€烽悞锕傚礉閺嶎厹鈧啯绻濋崶褑鎽曞┑鐐村灟閸ㄥ湱鐚惧澶嬬厱閻忕偞鍎抽悞褰掓煛鐎ｎ亝鍤囨慨濠呮閹即鍨鹃崗鍛棜闂傚倷鐒︾€笛呮崲閸岀倛鍥ㄥ閺夋垹鍘遍梺鐟板⒔缁垶鎮￠弴鐔虹闁糕剝顨嗙粋瀣繆椤栨氨澧﹂柡灞剧洴楠炴鎹勬潪鏉款棜婵＄偑鍊戦崝濠囧磿閻㈢绠栨繛鍡樻尰閸ゅ鏌ｉ姀銏犱化闁逞屽墯閸旀瑩骞冨畡閭︾叆闁告洦鍓涢崙锟犳⒑缁嬪尅宸ラ柣蹇旂箞椤㈡岸鏁愰崱娆戠槇濠殿喗锕╅崕鐢稿Ω閳哄倻鍘繝銏ｎ嚃閸ㄦ壆鈧凹鍠栭埢?
     *
     * @param id 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ョ紓鍌氬€风拋鏌ュ磻閹剧繝绻?
     * @return 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ラ梻鍌欑劍閻綊宕规繝姘モ偓鍌涚鐎ｎ亞锛涢梺瑙勫礃椤曆囧箲閼哥偣浜滈柟鎹愭硾娴犳帞绱掗銊ユ噽绾?
     */
    public Shop queryWithMutex(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        //婵犵數濮烽弫鎼佸磻濞戙埄鏁嬫い鎾跺枑閸欏繘鏌℃径瀣閻熸瑥瀚峰Σ褰掑箹濞ｎ剙鐒烘繛鑲╁枛濮婃椽骞愭惔銏╂闂佽桨绶￠崳锝呯暦閹达箑绠婚柡鍌樺劜椤秴鈹戦悙鍙夘棡闁荤喆鍔戦崺鈧い鎺嗗亾婵炵》绻濆璇测槈閵忊剝娅滈梺鎼炲劀閸愩劎顓洪梻鍌欑濠€閬嶅磻閵娾晛鍨傜憸鐗堝笚閸嬧晠鏌ㄩ悢鍝勑㈢紒鐙欏洦鍋ｉ柟顓熷笒婵′粙鏌涚€ｆ柨娲﹂埛鎺懨归敐鍛暈闁瑰弶鎮傞弻娑㈡偐閺屻儺鈧鎽堕悙瀵哥瘈闂傚牊渚楅崕蹇曠磼閻樿櫕銇濋柡宀嬬秮婵偓闁靛繆鍓濆В鍕磼閹冪稏缂侇喗鐟╁濠氭偄閻撳海鐣鹃悷婊冪箻閺佸秴顓兼径瀣幍?
        String shopJson = redisService.getCacheObject(key);
        //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佹悶鍎洪崜娆戝瑜版帗鐓涚€广儱楠搁獮鏍煢閸愵亜鏋涢柡灞剧洴婵＄兘顢欓悡搴交闂備礁鎲￠悷銉╂晝閵夆晛桅闁告洦鍨伴崘鈧梺闈浤涚仦鐐啇闂傚倷鑳堕…鍫燁殽閹间焦鏅濇い蹇撶墕缁犳牗淇婇妶鍌氫壕闂佸疇妫勯ˇ顖炲煝瀹ュ鏁囩憸宀€鑺辨繝姘叆?
        if (StrUtil.isNotBlank(shopJson)) {
            //闂傚倸鍊峰ù鍥敋瑜忛埀顒佺▓閺呮繄鍒掑▎鎾崇婵°倓鐒﹀▍鏂库攽閻樺灚鏆╁┑顕呭弮瀹曟垿骞樺ǎ顑跨盎闂婎偄娲﹂幐鐐櫠濞戙垺鐓涢柛婊€绀佹晶浼存煃瑜滈崜娆撳储濠婂牆纾婚柟鍓х帛閻撳啰鎲稿鍫濈婵炲棙鍨靛鏌ユ⒒娴ｄ警娼掗柛鏇炵仛閻ｅ墎绱撴担鍝勑ｅ┑鐐诧躬瀵寮撮悢椋庣獮闂佸壊鍋呯换鍌炩€栫€ｎ剛纾藉ù锝呮惈鏍￠梺缁樻惈缁绘繂顕?
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佹悶鍎洪崜娆戝瑜版帗鐓涚€广儱楠搁獮鏍煢閸愵亜鏋涢柡灞剧洴婵＄兘顢欓悡搴交闁诲海鎳撻幉锛勬崲閸儱钃熼柣鏃囥€€閸嬫挸鈽夊▍顓т簽缁參鍩￠崨顔惧幈濠殿喗顨呭Λ妤佹櫠閹绢喗鐓涚€光偓鐎ｎ剛袦婵犵鍓濋幃鍌涗繆閻戣棄唯妞ゆ棁宕靛Λ顖氣攽閻樻鏆柍褜鍓欓崯璺ㄧ棯瑜旈弻鐔碱敊閻撳簶鍋撻幖浣瑰仼闁绘垼妫勫敮闂佸啿鎼崐鐟扳枍閸℃稒鈷戦柛蹇涙？閼割亪鏌涙惔銏㈡创闁轰礁鍊垮畷婊嗩槾闁挎稒绮撻弻锝堢疀閺囩偘鍝楁繝娈垮枔閸婃繈鐛崱娑橀唶闁靛濡囬崢?
        if (shopJson != null) {
            return null;
        }

        // TODO 闂傚倸鍊峰ù鍥敋瑜庨〃銉х矙閸柭も偓鍧楁⒑椤掆偓缁夊澹曟繝姘厽闁哄啫娲ゆ禍鍦偓瑙勬尫缁舵岸寮诲☉銏犖ㄩ柕蹇婂墲閻濇牜绱掗幆褍缍栫紒顔界懇瀵鎮㈤悡搴ｇ暰閻熸粌绻橀弫宥咁吋婢跺鍘甸梺浼欑到閼活垶鍩㈤崼鐔稿弿濠电姴鍟妵婵囶殽閻愬瓨宕屾鐐村浮瀵剙鈹戦崟顐や紕缂?
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            //闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯闁挎繂鎳庨‖瀣渻閵堝倹娅嗛柣鎿勭節瀵鎮㈤崗鐓庘偓缁樹繆椤栨繍鍤欓梻澶婄Ч濮婅櫣鎷犻懠顒傤啋婵炲瓨绮忓▔娑綖?
            boolean isLock = tryLock(lockKey);
            //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佹悶鍎洪崜娆戝瑜版帗鐓涚€广儱楠搁獮鏍煢閸愵亜鏋涢柡灞剧洴婵＄兘顢欓悡搴交闂備礁鎲￠悷銉╂晝閵夆晛桅闁告洦鍨伴崘鈧梺闈浤涚仦鐐啇闂傚倷鑳堕…鍫燁殽閹间焦鏅濇い蹇撶墕閽冪喖鏌曢崼婵愭Ц缂佺姵姘ㄩ幉绋款吋閸涘偊缍侀幊锟犲Χ閸℃鐎鹃梻浣告惈椤︽壆鈧瑳鍥х獥婵☆垱妞垮▓浠嬫煟閹邦剚鈻曢柛搴㈡⒐椤ㄣ儵鎮欓崣澶婃灎濡炪們鍨洪〃濠囧春閳ь剚銇勯幒鎴濐伒缂?
            if (!isLock) {
                //闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯妞ゆ梻鍘ч～顏堟⒑缂佹ɑ灏柛搴ㄤ憾濠€渚€姊洪幐搴ｇ畵婵☆偅鐟╁畷鎴︽偐鐟佷胶鎳撻…銊╁礃閿曗偓娴狀噣姊虹拠鈥虫灈缂傚秴锕悰顕€宕堕鈧悡娑樏归敐鍥剁劸閸熷摜绱撻崒姘偓宄懊归崶銊ｄ粓闁告縿鍎插畷鏌ユ煕閹板吀绨撮柛瀣崌濡啫鈽夐弽褌鍒掓俊銈囧Х閸嬫盯顢栨径鎰畺闁冲搫鍟犻崑鎾诲捶椤撶倫娑㈡煕鐎ｎ偅宕岀€规洜顭堣灃濞达絽寮剁€氬ジ姊绘担鍛婅础缂侇噮鍨抽弫顕€鎮欓崣澶嬬槑闂?
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯妞ゆ梻鍘ч～顏堟⒑缂佹ɑ灏柛搴ㄤ憾濠€渚€姊洪幐搴ｇ畵婵炶绠撳畷鐢稿焵椤掑嫭鈷戦悹鍥皺缁犵増绻涘顔煎籍鐎殿喛顕ч埥澶愬閻樼數娼夐梻浣侯焾閺堫剛鍠婂澶嬪仧闁靛繈鍨荤壕钘壝归敐鍕煓闁告繆娅ｇ槐鎺旀嫚閹绘帩浼冮梺绯曟杹閸嬫挸顪冮妶鍡楀潑闁稿鎸荤换婵嬪焵椤掑嫭鐒肩€广儱鎳愰敍娑㈡⒑缂佹ɑ顥嗛柛瀣姇閻ｅ灚绗熼埀顒勫蓟濞戙垹唯闁靛繆鍓濋悵鏍磼閹冪稏缂侇喗鐟╁濠氭偄閻撳海鐣鹃悷婊冪箻閺佸秴顓兼径瀣幍闂佷紮绲介懟顖炲煝閸喐鍙忓┑鐘插暞閵囨繃顨ラ悙瀛樺磳妤犵偞甯″顒€鈹戦崟顐や紕缂?
            shop = this.getById(id);
            //濠电姷鏁告慨鐑姐€傞挊澹╋綁宕ㄩ弶鎴濈€銈呯箰閻楀棛绮堥崼鐔虹瘈闂傚牊绋撴晶鎰版煕鐎ｎ偅灏い顐ｇ箞椤㈡﹢鎮╅崘鍙夌彴闂傚倷绀侀幖顐︽嚐椤栫偞鍤愭い鏍剱閺佸洤鈹戦崒婊庣劸鐎瑰憡绻冮妵鍕冀閵娧呯厐濡炪倧鑵归弲鐘诲蓟閿濆棙鍎熼柨婵嗘噸閸栨牠姊虹粙鍖℃敾闁烩晩鍨伴悾宄懊洪鍕敤閻熸粍绮岄妴?
//            Thread.sleep(5000);
            if (shop == null) {
                //闂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸閻ゎ喗銇勯幇鈺佺労闁搞倖娲熼弻娑㈩敃閿濆棗顦╅梺杞扮濡瑧鎹㈠☉銏犵闁绘劕顕▓銈囩磼閹冪稏缂侇喗鐟╁濠氭偄閻撳海鐣鹃悷婊冪箻閺佸秴顓兼径瀣幍闂佷紮绲介懟顖氭毄缂傚倷娴囨ご鍝ユ暜閻愬顩烽柨鏇炲€归崵宥夋煏婢跺牆鍔欐繛锝庡櫍濮?闂傚倸鍊峰ù鍥敋瑜忛幑銏ゅ箛椤旇棄搴婇梺褰掑亰閸庨潧鈽夊Ο閿嬵潔闂侀潧绻掓慨鐑藉储閹绢喗鈷戦柛婵嗗閳诲鏌涘Ο鍨汗濠㈣娲濋妵鎰板箳閹捐泛骞楅梻浣筋潐瀹曟ê鈻嶉弴銏犵闁挎繂顦伴悡娆撴煟閿濆懓瀚伴柍璇茬墦閺岋紕浠﹂崜褜鐏辩紓浣哄У閻╊垶寮幇鏉垮耿闁宠　鍋撻柟瀵割煫dis
                redisService.setCacheObject(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //闂傚倸鍊峰ù鍥敋瑜忛埀顒佺▓閺呮繄鍒掑▎鎾崇婵°倓鐒﹀▍鏃堟⒒閸屾瑧顦﹂柟璇х節閹兘濡烽埡浣圭€梺绋跨▉濞呮攰s
            redisService.setCacheObject(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
        } finally {
            unLock(lockKey);
        }
        //闂傚倸鍊搁崐鎼佸磹閻戣姤鍊块柨鏇氶檷娴滃綊鏌涢幇闈涙灍闁哄懏绻堥弻鏇熷緞閸℃ɑ鐝斿┑鈽嗗亝閿氶棁澶愭煥濠靛棙鍣洪悹鎰ㄥ墲閵囧嫰鍩￠崒婊冨绩闂佸搫鐬奸崰鏍箖濠婂吘鐔烘嫚閺屻儳鈧椽姊绘担绛嬪殭缂佺粯蓱缁傚秶鎹勬笟顖涚稁?
        unLock(lockKey);
        return shop;

    }

    /**
     * 闂傚倸鍊峰ù鍥敋瑜忛幑銏ゅ箛椤旇棄搴婇梺鐟邦嚟婵潧鐣烽弻銉︾厱闁斥晛鍟伴埊鏇㈡煕鎼粹槄鏀婚柕鍥у瀵粙顢曢～顓熷媰闂備焦鎮堕崐鏍ь潖婵犳艾鐒垫い鎺戝€归崵鈧柣搴㈠嚬閸樺ジ鈥﹂崹顔ョ喖鎮℃惔锝囩摌缂傚倷绀侀幖顐ゅ枈娑斺暐s闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佸湱鍎ら崵锕€鈽夊Ο婊勬瀹曟﹢顢旈崟顐バ曢梺璇叉捣閹虫挾鈧矮鍗冲畷鎴炵節閸パ冩優闂佸搫娲㈤崹濠氬矗閹剧粯鐓曢柕澶涚到婵″潡鏌嶉娑欑闁?
     *
     * @param key 闂傚倸鍊搁崐鎼佸磹妞嬪海鐭嗗〒姘ｅ亾濠碉繝娼ч～婊堝焵椤掑嫬绠犳俊銈呭暞瀹曞鎮跺☉鎺戝⒉闁诲繋绶氬铏规兜閸涱喖娑ч梻鍌氬鐎氫即骞冮敓鐘参ㄩ柨鏃囨〃缁ㄥ姊洪崫鍕偓鎼佹倶濠靛绠栭柟杈鹃檮閻?
     * @return 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢妶鍌氫壕婵ê宕崢瀵糕偓瑙勬礉椤鈧潧銈稿鍫曞箣閻樺灚姣庢繝鐢靛仦閹稿宕洪崘顔肩；闁瑰墽绮悡娆撴煟閹邦垱顥夐柣蹇婃櫊閺岋綁鏁愯箛鏇犵槇閻庢鍠栨晶搴ㄥ箲閸曨垰惟闁挎洍鍋撳ù鐘茬秺濮婂宕掑▎鎺戝帯闂佺娅曢幑鍥箖濞差亜惟闁宠桨鑳堕ˇ顕€姊洪崫鍕窛闁哥姵鍔欏畷?
     */
    private boolean tryLock(String key) {
        boolean flag = redisService.setCacheObjectIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 闂傚倸鍊搁崐鎼佸磹閻戣姤鍊块柨鏇氶檷娴滃綊鏌涢幇闈涙灍闁哄懏绻堥弻鏇熷緞閸℃ɑ鐝斿┑鈽嗗亝閿氶柍钘夘樀楠炴ɑ锛愬┑鍫濆絼dis闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佸湱鍎ら崵锕€鈽夊Ο婊勬瀹曟﹢顢旈崟顐バ曢梺璇叉捣閹虫挾鈧矮鍗冲畷鎴炵節閸パ冩優闂佸搫娲㈤崹濠氬矗閹剧粯鐓曢柕澶涚到婵″潡鏌嶉娑欑闁?
     *
     * @param key 闂傚倸鍊搁崐鎼佸磹妞嬪海鐭嗗〒姘ｅ亾濠碉繝娼ч～婊堝焵椤掑嫬绠犳俊銈呭暞瀹曞鎮跺☉鎺戝⒉闁诲繋绶氬铏规兜閸涱喖娑ч梻鍌氬鐎氫即骞冮敓鐘参ㄩ柨鏃囨〃缁ㄥ姊洪崫鍕偓鎼佹倶濠靛绠栭柟杈鹃檮閻?
     */
    private void unLock(String key) {
        redisService.deleteObject(key);
    }

    /**
     * 婵犵數濮烽弫鎼佸磿閹寸姴绶ら柦妯侯棦濞差亝鏅滈柣鎰靛墮鎼村﹪姊虹粙璺ㄧ伇闁稿鍋ゅ畷鎴﹀Χ婢跺鍘繝鐢靛仧閸嬫挸鈻嶉崱娑欑厱濠电姴瀚敮娑㈡煏閸パ冾伃妤犵偞顭囬幑鍕儎閹哄鐏╅棁澶愭煟濞嗗苯浜鹃梺鎼炲妼閻栧ジ鎮伴鈧浠嬵敃閻旇渹澹曞┑鐐村灦閻熻鲸绗熷☉娆戠闁割偆鍠撻惌鎺楁煛瀹€鈧崰鏍箖閸撗傛勃闁绘劦鍓氶惁鎾绘煟鎼淬埄鍟忛柛锝庡櫍瀹曟垶绻濋崶褏鐣洪悷婊勬煥閻ｇ兘鎮℃惔妯绘杸闂佸壊鍋呯粙鎴炵娴煎瓨鈷掗柛灞剧懅椤︼箓鏌熺拠褏纾块柡渚囧櫍閹瑥霉鐎ｎ偆鈧椽姊洪崨濠庢畷濞存粎妫闂傚倸鍊搁崐鐑芥倿閿旈敮鍋撶粭娑樻噽閻瑩鏌熸潏楣冩闁稿顑夐弻娑㈠焺閸愵亖濮囬梺绋款儍閸斿秹濡甸崟顖氱疀闁割偅娲橀宥夋⒑缂佹ê绗掓い顓犲厴瀵鏁撻悩鑼槹濡炪倖鍔戦崐妤咁敊婵犲洦鐓熼柕蹇婃櫅閻忕姵淇婇锝囩畵妞ゆ洩缍佸畷濂稿即閻愰潧骞愬┑鐐舵彧缁插潡骞婇幘鍑板洭鍩℃担鍙夋杸闂佺粯锕╅崑鍕妤ｅ啯鈷戦悹鎭掑妼閺嬫柨鈹戦纰卞殶闁绘碍鍎抽鍏煎緞鐎ｎ剙骞堟俊鐐€栭崝褏寰婇崜褏鐭嗛柛鎰靛枟閻撱儲绻涢幋鐐跺闁哄鐩弻?
     *
     * @param id            闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ョ紓鍌氬€风拋鏌ュ磻閹剧繝绻?
     * @param expireSeconds 闂傚倸鍊搁崐鎼佸磹妞嬪孩顐介柨鐔哄Т绾惧鏌涘☉鍗炲福闁挎繂顦粻鎶芥煛閸愶絽浜惧銈嗗姌婵倝濡甸崟顖氱疀闁告挷鑳惰摫濠电偛鐡ㄩ崵搴ㄥ磹濠靛钃熸繛鎴欏灩鍞梺鎸庣箓閹冲酣鈥栨径鎰拺缂侇垱娲樺▍鍛存煕婵犲倹鍋ョ€殿喖顭烽弫鎾绘偐閼碱剦妲伴梻渚€娼ц噹闁告劦浜滈獮妤€鈹戦悩娈挎毌婵℃彃鎳樺畷鎴﹀礋椤愵偆鍔烽梺璺ㄥ枔婵挳寮告担琛″亾楠炲灝鍔氭い锔诲灦閺屽宕堕浣哄帾闂婎偄娲ら敃銉モ枍閸℃稒鐓曢悗锝冨妼閸旓附鎱?
     * @throws InterruptedException 濠电姷鏁告慨鐑姐€傞挊澹╋綁宕ㄩ弶鎴濈€銈呯箰閻楀棛绮堥崼鐔虹瘈闂傚牊绋撴晶鎰版煕鐎ｎ偅灏い顐ｇ箞椤㈡﹢鎮╅崘鍙夌彛濠碉紕鍋戦崐鏍哄鈧幃鐑藉煛閸涱厾鐤囧┑鐘诧工閻楀棝宕归崒娑栦簻闁逛即娼ф禍婵堟偖瑜庣换婵嬫偨闂堟稐娌梺鎼炲妼閻栧ジ鐛弽顓炵厸闁告侗鍘奸崑宥嗙箾鐎电孝妞ゆ垵鎳橀幏鎴︽偄閸忚偐鍘遍梺闈涱槹閸ㄧ敻鎳熼姘ｆ灁闁靛ň鏅滈埛鎴犵磼鐎ｎ偄顕滄繝鈧悧鍫熷弿婵☆垳顭堟慨鍌溾偓?
     */
    public void saveHotShopRedis(Long id, Long expireSeconds) throws InterruptedException {
        //闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢妶鍡椾粡濡炪倖鍔х粻鎴犲閸ф鐓欑紓浣靛灩濞呮﹢鏌℃担鍓插剱濞ｅ洤锕俊鍫曞礋椤撶偛顬夐梻浣侯焾濞寸兘宕伴幇顓犫攳濠电姴娲ゅ洿闂佸憡渚楅崰鏍р枍閵堝鈷戠痪顓炴噺閻濐亪鏌ｉ悢鍙夋珚鐎殿喖顭烽崺鍕礃閳轰緡鈧捇姊洪崨濠勭畵閻庢氨鍏樺鎶筋敇閵忊€斥偓?
        Shop shop = this.getById(id);
        Thread.sleep(2000);
        //闂傚倸鍊峰ù鍥敋瑜忛幑銏ゅ箛椤旇棄搴婇柣搴秵閳ь兙鍨圭紞濠傜暦閸洦鏁勯柨鏇楀亾妞わ附澹嗛幑銏犫攽鐎ｎ亞鍊為梺闈浤涢崘銊愩儵姊婚崒娆戭槮闁圭⒈鍋嗛埀顒佸嚬閸撴盯骞戦姀銈呯闁哄啠鍋撻柡鍡樼矒閺屾盯鍩勯崘顏佹缂備胶瀚忛崶銊у帗闁哄鍋炴竟鍡涘礉瀹ュ拋鐔嗛悹鍝勬惈椤忣亞绱掔紒妯肩疄闁糕斁鍋撳銈嗗笒鐎氼參寮查幖浣圭厽婵☆垰鐏濋惃娲煟椤愩垹顏慨濠冩そ瀹曟粓骞撻幒宥囧嚬婵犵數鍋涘鍫曟晝閵忋倕鏋?
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鑼槷闂佸搫绋侀崢浠嬪磻閿熺姵鐓忓璺烘濞呭懘鏌ｉ鐕佹疁闁哄本鐩幃娆戔偓娑欘焺椤ょ導s
        redisService.setCacheObject(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢妶鍥╃厠闂佺粯鍨堕弸鑽ょ礊閺嵮岀唵閻犺櫣灏ㄩ崝鐔兼煛閸℃劕鈧洟濡撮幒鎴僵闁挎繂鎳嶆竟鏇㈡煟鎼淬値娼愭繛璇х畵瀹曞湱鎹勯搹瑙勬闂佸憡顨堥崑鎰ｉ崼鐔虹闁糕剝锚婵洦绻涢崼鐕傝€挎慨濠勭帛閹峰懘宕ㄦ繝鍌涙畼闂備浇鍋愰幊鎾存櫠閻ｅ苯鍨濋柡鍐ㄧ墕缁犳岸鏌熼棃娑樻毐鐎电増妫冨濠氬磼濞嗘埈妲梺鍦拡閸嬪棝骞冮鈧、鏇㈡晝閳ь剛澹曢崸妤佺厵缂備降鍨瑰▍姗€鏌℃担鍓插剱濞ｅ洤锕俊鍫曞川椤斿吋顏犻梻浣圭湽閸娿倝宕抽敐澶婅摕婵炴垯鍨归悞娲煕閹板吀绨村┑顔兼喘濮婅櫣娑甸崨顔惧涧闂佽崵鍠嗛崕纾嬨亹娓氣偓濮婃椽骞栭悙鎻掑Х缂備浇灏慨銈夊疾閸洖绠甸柟?
     *
     * @param shopName 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴顭ㄩ崼婵堢崶闁硅偐琛ラ崹鎯р槈濡攱鏂€闁诲函缍嗘禍鐐侯敊閹达附鈷戞慨鐟版搐閻忣喗銇勯鐐靛ⅱ闁瑰弶鎸抽弫鍌炴煥椤栨矮澹曞Δ鐘靛仜閻忔繈宕濆顓犵?
     * @return 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴顭ㄩ崼婵堢崶闁硅偐琛ラ崹鎯р槈濡攱鏂€闁诲函缍嗘禍鐐侯敊閹达附鈷戞慨鐟版搐閻忊晠鏌ｈ箛鏃€鐨戦柍褜鍓氱喊宥呯暆閹间礁钃熼柕濞炬櫅缁秹鏌涢妷顔惧帥婵☆偄瀚板?
     */
    @Override
    public ShopVO getShopByShopName(String shopName) {
        Shop shop = query()
                .eq("name", shopName)
                .eq("status", 1)
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .one();
        return convertToShopVO(shop);
    }
    /**
     * 婵犵數濮烽弫鍛婃叏娴兼潙鍨傚┑鍌滎焾閺勩儵鏌″畵顔兼湰閸嶇敻姊洪棃娑辩劸闁稿孩鍨堕幆鏃堚€﹂幋鐐存珦闂備礁鍚嬫禍浠嬪磿闁秴绀夐柛娑樼摠閳锋垿鏌熺粙鍨劉妞ゃ儱妫楅埞鎴︻敊閸濆嫧鍋撳┑瀣闊洦娲嶉崑鎾绘晲鎼存ê浜鹃柕蹇曞Х缁夋椽鏌熼瑙勬珚鐎规洦鍋婂畷鐔碱敇閻愯尙绱伴梻鍌氬€峰ù鍥敋閺嶎厼绐楅柡宥庡亜椤ユ碍銇勯幘璺衡偓锝夋晲婢跺﹦鐤€闂佸搫顦伴娆忣焽婵犲洦鈷戠紓浣癸供閻掔偓绻涢崨顔界闁诡噣娼ч…銊╁醇閻斿弶瀚藉┑鐐舵彧缁蹭粙骞栭锝囶洸闁绘劦鍏涚换鍡涙煟閹邦厼顥嬮柣顓熺懇閺屾盯鍩為幆褌澹曞┑锛勫亼閸婃牜鏁繝鍥ㄥ€块柨鏇炲亰缂嶆牗淇婇妶鍛殶缁?
     * 闂傚倸鍊搁崐宄懊归崶顒€违闁逞屽墴閺屾稓鈧綆鍋呯亸浼存煏閸パ冾伃鐎殿喕绮欐俊姝岊槷婵℃彃鐗撳鐑樺濞嗗繒妲ｉ梺闈╃秶缂嶄礁顕ｇ拠娴嬫闁靛繒濮烽濠囨⒑閻熼偊鍤熼柛搴㈠姇铻為柕鍫濐槹閻撶喖鏌ｉ幇顓熺稇闁搞値鍓熼弻娑氣偓锝庝簼閸ｈ銇勯鐐村缂佺粯绻傞～婵嬵敆閸屾埃鍋撻悙鐑樷拺闂傚牊渚楅悡顓犵磼閻樺啿鐏辨い锝勭矙濮婂宕掑▎鎴犵崲濠电偘鍖犻崗鐐洴瀵噣宕掑鍛姸濠电偞鎸婚崺鍐磻閹惧灈鍋撶憴鍕┛缂傚秮鍋撳銈忕畱缂嶅﹪寮婚敍鍕勃閻犲洦褰冮～鍥ь渻閵堝啫鐏柣鐔叉櫊楠炲﹪寮介鐐靛幐闂佺鏈粙鎴﹀几濞嗗繆鏀介柣妯活問閺嗩垱淇婇幓鎺撳殗鐎规洖缍婇幃鍓т沪缂併垺缍楅梻浣告惈濞层劍鎱ㄩ悽绋跨婵犲﹤鐗婇悡銉╂煛閸モ晛浠滈柍褜鍓欓幗婊呭垝閸繄鏆嗛柍褜鍓熼獮澶愬箹娴ｇ懓浜遍梺鍓插亝缁诲嫰鎮烽妸鈺傜厽闁靛繆鏅涢悘鈥斥攽閻愯韬€殿喖顭锋俊鎼佸Ψ閵忊槅娼旀繝纰樻閸ㄦ娊宕㈣閵嗗倸煤椤忓應鎷洪柣鐘叉礌閳ь剝娅曢悘宥呪攽閻愭彃绾х紒顔奸叄瀹?闂傚倸鍊搁崐椋庢濮橆剦鐒藉┑鐘崇閳锋棃鏌涢弴銊ヤ航闁绘柨妫欐穱濠囶敍濞嗘帩鍔呴梺缁樺笂缁瑩寮诲☉銏犵疀闂傚牊绋掗悘宥夋⒑閸濆嫭濯奸柛鎾寸懇閳ワ妇鎹勯妸锕€纾繛鎾村嚬閸ㄥ崬鏆╃紓鍌氬€烽梽宥夊礉鐎ｎ喖纭€闁规儼妫勯拑鐔哥箾閹寸們姘跺几鎼淬劎鍙撻柛銉ｅ劚閸旀艾霉閸忕厧濮囬柍瑙勫灴閹晠顢曢～顓烆棜闂傚倷绀佸﹢閬嶆惞鎼淬劌绐楁俊銈呮噹绾惧鏌ｅΟ鑲╁笡闁绘挻娲熼弻宥夊煛娴ｅ憡鐏撳┑鐐茬墛濞叉粎妲愰幘鎰佸悑闁告侗鍨抽鎺楁⒑?(X,Y) 闂傚倸鍊峰ù鍥х暦閸偅鍙忛柣鎴ｆ绾惧潡鏌曢崼婵囧鐎规洘鐓￠弻娑㈠即閵娿儳浠氶梺閫炲苯澧柣妤冨Т閻ｇ兘宕奸弴銊︽櫌闂侀€炲苯澧扮紒顔碱煼楠炲酣鎳為妷褍骞嶉梻浣虹帛閸ㄥ爼鏁嬮梺璇茬箚閺呮繈鎯€椤忓牆绾ч悹鎭掑壉瑜旈弻?
     *
     * @param shop 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭罕闂佸搫娲㈤崹鍦不閻樿绠规繛锝庡墮婵¤偐绱掗悩鍐插摵闁哄本鐩、鏇㈡偐閹绘帒顫撶紓鍌欒兌婵箖锝炴径灞惧床婵犻潧娲ㄧ弧鈧梺绋挎湰缁嬫垵鈻嶉敐澶嬧拺缂佸顑欓崕蹇曠磼婢跺﹦绉虹€殿喖顭烽崺鍕礃閵娧呯嵁闂備胶纭堕埀顒€纾粻鏉棵瑰鍫㈢暫闁哄本绋戦埢搴ょ疀閿濆棌鏋旈梻浣侯焾椤戝啴宕濋幋婵愭綎闁告繂瀚呰瀹曟儼顦叉い顒€鐗撻弻锝嗘償閵忕姴姣堥梺鍛婃尵閸犳牠鐛崘顔奸敜婵°倐鍋撶紒鈧€ｎ偁浜滈柟鍝勭Ф椤︼附銇?(闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭罕闂佸搫娲㈤崹鍦不閻樿绠规繛锝庡墮婵¤偐绱掗悩鍐插摵闁哄本鐩、鏇㈡偐閹绘帒顫氶梻浣侯焾椤戝啴宕濋幋锕€钃熼柡鍥╁枔缁♀偓闂婎偄娲﹀ú婊堝汲閻樼粯鈷戠紓浣股戠亸鐗堢箾閼碱剙鏋涚€殿喛顕ч埥澶婎煥鎼粹懣顏堟⒒娴ｅ憡鍟炴慨濠傜秺閹囨偐鐠囪尙鐣?
     * @return 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢妶鍥╃厠闂佺粯鍨堕弸鑽ょ礊閺嵮岀唵閻犺櫣灏ㄩ崝鐔兼煛閸℃劕鈧洟婀侀梺鎸庣箓濞层倝宕濈€ｎ兘鍋撶憴鍕妞わ附澹嗛幑銏犫攽鐎ｎ偒妫冨┑鐐村灦閼归箖鍩涢崼銉︹拺閻犲洦褰冮銏㈡喐閺夊灝鏆ｆ繝鈧笟鈧铏圭磼濡浚浜滈锝夊醇閺囩偠鍩炴繝銏ｆ硾閻偐澹曟總鍛婂仯闁搞儯鍔庨崣鈧梺鍛婄懃鐎氼噣鍩€椤掑喚娼愭繛鍙壝悾婵嬪箹娴ｅ搫绁﹂棅顐㈡处閹搁箖寮抽崱娑欑厓鐟滄粓宕滃☉姘灊缂備焦顭囬梽鍕煕濞戞﹫鍔熼柛妯兼暬濮婅櫣绱掑Ο铏逛桓闂佹寧姘ㄧ槐鎺懳旀担鍝ヮ儌缂備浇椴哥敮锟犲灳閿曞倸绠ｆ繝闈涙閻嫰姊绘担绛嬪殐闁搞劋鍗冲畷顖炲级閹寸姵娈鹃梺缁樻⒒閳峰牓寮崘顔界叆婵犻潧妫欓ˉ鐘垫喐閺夎法效婵﹤顭峰畷鎺戔枎閹存繂顬夐梻浣侯焾鐎涒晜绻涙繝鍥╁祦闁哄稁鍙庨弫鍐煥閺囨浜鹃梺鎼炲妼閸婂綊濡甸崟顔剧杸闁圭偓鎯屽Λ锟犳⒑?
     */
    @Override
    public List<ShopVO> getShopByCondition(Shop shop) {
        QueryWrapper<Shop> wrapper = new QueryWrapper<>();
        // 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鍨鹃幇浣告櫊婵犵數濮甸懝楣冩偪閻愵剛绡€闂傚牊渚楅崕蹇涙煛?MySQL 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鎻掔€梺绋跨箰閸氬宕ｈ箛娑欑厪闁割偅绻嶅Σ鍛婃叏鐟欏嫮鍙€闁哄矉缍佸顕€宕掑顒€顬嗗┑鐘愁問閸ㄤ即濡堕幖浣歌摕闁靛鍎Σ鍫熶繆椤栨瑨顒熷ù灏栧亾闂傚倷绀侀幖顐⑽涢銏犵闁绘梻鍘ч拑鐔兼煥濠靛棭妲归柛濠勫厴閺岀喓绱掑Ο铏圭懖濠电偛鐗愰褔鍩為幋锔藉€烽柟缁樺笚閸婎垱绻濋姀銏″殌婵☆偅绻堥悰顕€骞嬮敃鈧粈瀣亜閹邦喖鏋戦柡鍌楀亾闂傚倷绀佹竟濠囧磻閹烘纾婚柛鈩冪☉閻?
        String distanceSql = "ST_Distance_Sphere(point(x, y), point(" + shop.getX() + ", " + shop.getY() + ")) as distance";
        wrapper.select("*, " + distanceSql);
        wrapper.eq("status", 1);
        wrapper.eq("audit_status", AuditStatusEnum.PASS.getCode());
        // 1. 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佸湱鍎ら崵锕€鈽夊Ο閿嬵潔濠殿喗锕╅崢鍏肩韫囨搩娓婚柕鍫濇婵呯磼閻樺啿鐏╅柣銉海椤﹀綊鏌＄仦鐣屝у┑锛勫厴婵＄兘濡疯閻涙挻淇?
        if (shop.getTypeId() != null) {
            wrapper.eq("type_id", shop.getTypeId());
        }

        // 2. 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢敃鈧壕鍦磼鐎ｎ偓绱╂繛宸簼閺呮繈鏌嶈閸撶喖寮崘顔碱潊闁炽儲鍓氶崵銈夋⒑閸濆嫷妲归柛銊ョ秺椤㈡瑩骞囬鑺ユ杸闂佺粯顭堥婊冾啅閵夆晜鐓欓柛鎰叀閸欏嫮鈧娲橀崹鐢割敇閸忕厧绶為悗锝庡墮楠炴劕鈹戦悩顔肩伇婵炲绋戣灋鐎光偓閸曨偆锛涢梺瑙勫礃椤曆囨儗濡も偓椤潡鎳滃妤婁簼缁?
        if (StringUtils.isNotBlank(shop.getName()) ||
                StringUtils.isNotBlank(shop.getArea()) ||
                StringUtils.isNotBlank(shop.getAddress())) {

            wrapper.and(w -> w
                    .like(StringUtils.isNotBlank(shop.getName()), "name", shop.getName())
                    .or()
                    .like(StringUtils.isNotBlank(shop.getArea()), "area", shop.getArea())
                    .or()
                    .like(StringUtils.isNotBlank(shop.getAddress()), "address", shop.getAddress())
            );
        }

        // 3. 闂傚倸鍊搁崐椋庢濮橆剦鐒藉┑鐘崇閳锋棃鏌涢弴銊ュ闁绘繂鐖奸弻娑㈠焺閸愵亖妲堢紓浣哄У閻楁洟鍩為幋锔藉亹闁圭粯甯楀▓鍙夌節濞堝灝鐏犳い鏇ㄥ弮閸┾偓妞ゆ巻鍋撶紒鐘茬Ч瀹曟洟宕￠悘缁樻そ婵℃悂鍩℃担绋挎闂備胶顭堥惉濂稿磻閻愮儤鍋傛繛鎴烇供閻斿棝鎮归搹鐟板妺妞ゃ儲鍨块弻娑氣偓锝庡亝瀹曞瞼鈧娲╃徊鎯ь嚗閸曨倠鐔告姜閹殿喚澶?
        if (shop.getX() != null && shop.getY() != null ) {
            wrapper.apply("ST_Distance_Sphere(point(x, y), point({0}, {1})) <= {2}",
                    shop.getX(), shop.getY(), 200000);
        }
        wrapper .orderByAsc("distance");
        return convertToShopVOList(list(wrapper));
    }

    /**
     * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢妶鍥╃厠闂佺粯鍨堕弸鑽ょ礊閺嵮岀唵閻犺櫣灏ㄩ崝鐔兼煛閸℃劕鈧洟濡撮幒鎴僵闁挎繂鎳嶆竟鏇㈡煟鎼淬値娼愭繛璇х畵瀹曞湱鎹勯搹瑙勬闂佸憡顨堥崑鎰ｉ崼鐔虹闁糕剝锚婵洦绻涢崼鐕佹附d闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹濠德板€曢幊宀勫焵椤掆偓閸燁垰顕ラ崟顖氱疀妞ゆ垟鏂傞崕鐢稿蓟濞戙垹绠涢梻鍫熺⊕閻忓牓姊洪挊澶婃殶闁哥姵鐗犲濠氬即閻旈绐炲┑鈽嗗灣閸樠呮暜閵夆晜鈷戦柟鑲╁仜閳ь剚娲滈埀顒佺煯閸楀啿顕ｇ拠娴嬫闁靛繆鈧厖绨婚梻鍌欑閻忔繈顢栭幇顑╂盯鏁冮崒娑掓嫼缂佺虎鍘奸幊蹇浰夋径鎰厽闊洦姊归惃鎴犵磼椤旇棄鍝洪柡灞芥椤撳ジ宕ㄩ姘ら梻鍌欑閹诧繝宕滈鍕窛妞ゆ牭绲捐ⅲ闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹濠德板€曢幊宀勫焵椤掆偓閸燁垰顕ラ崟顖氱疀妞?
     *
     * @param ids 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴顭ㄩ崼婵堢崶闁硅偐琛ラ崹鎯р槈濡攱鏂€闁诲函缍嗘禍鐐侯敊閹寸姷纾藉ù锝嗗絻娴滃墽绱撴担绋跨骇闁烩晩鍨伴～蹇撁洪鍕獩婵犵數濮撮崐濠氭偡閹绘帩娓婚柕鍫濆暙婵″吋銇勯敃鈧悘姘┍?
     * @return 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴顭ㄩ崼婵堢崶闁硅偐琛ラ崹鎯р槈濡攱鏂€闁诲函缍嗘禍鐐侯敊閹达附鈷戞慨鐟版搐閻忣喗銇勯鐐靛ⅱ缂侇喖鐗撳畷鍗炍熼崷顓犵暰闂備線娼ч悧鍡涘箠鎼搭煈鏁傞柕澶嗘櫆閻?
     */
    @Override
    public List<ShopVO> getShopList(List<Long> ids) {
        List<ShopVO> shopList = redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_SHOP_KEY,
                RedisConstants.LOCK_SHOP_KEY,
                ids,
                ShopVO.class,
                missingIds -> {
                    // 3. 婵犵數濮烽弫鍛婄箾閳ь剚绻涙担鍐叉硽閸ャ劎顩烽悗锝庝簽閿涙盯姊洪崨濠勨槈闁宦板姂瀹曟垿宕掗悙瀵稿帗闂佸疇妗ㄧ粈渚€寮抽悙鐑樼厱閹艰揪绲介弸娑㈡煛瀹€鈧崰鏍х暦濠婂棭妲婚梺鍛婃缁犳捇寮诲☉姘ｅ亾閿濆骸浜濈€规洖鐬奸埀顒侇問閸犳鍒婇悾宀€涓嶆繛鎴欏灩閸楁娊鏌ｉ幇顓у晱濞?(Lambda闂傚倸鍊峰ù鍥х暦閻㈢纾婚柣鎰暩閻瑩鐓崶銊р槈缂佲偓婢舵劗鍙撻柛銉ｅ妿閳藉绱掗悩鑼粵闁靛洤瀚粻娑㈠箻閹颁椒绱濋梻?
                    List<Shop> shops = query()
                            .in("id", missingIds)
                            .eq("status", 1)
                            .eq("audit_status", AuditStatusEnum.PASS.getCode())
                            .list();
                    return convertToShopVOList(shops);
                },
                ShopVO::getId,
                RedisConstants.CACHE_SHOP_TTL,
                TimeUnit.MINUTES
        );
        if (CollUtil.isEmpty(shopList)) {
            return Collections.emptyList();
        }
        return shopList.stream()
                .filter(this::isVisibleShop)
                .collect(Collectors.toList());
    }

    /**
     * 闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯妞ゆ梻鍘ч～鈺呮⒑濮瑰洤鈧倝宕抽敐澶婅摕婵炴垯鍨归悞娲煕閹板吀绨村┑顔兼喘濮婅櫣娑甸崨顔惧涧闂佽崵鍟块弲鐘差嚕婵犳碍鍋勭痪鎷岄哺閺呫垽姊虹粙鎸庢拱缂侇喖鐬肩划顓㈡晸閻樻枼鎷?
     *
     * @return 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴顭ㄩ崼婵堢崶闁硅偐琛ラ崹鎯р槈濡攱鏂€闁诲函缍嗘禍鐐侯敊閹达附鈷戞慨鐟版搐閻忣喗銇勯鐐靛ⅱ缂侇喖顭烽獮瀣偐閻㈢绱冲┑鐐舵彧缂嶁偓闁稿鍊块獮瀣攽閸愨晝浜?
     */
    @Override
    public Integer getShopTotal() {
        return query().count().intValue();
    }

    /**
     * 闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯妞ゆ梻鍘ч～鈺呮煟鎼淬垼澹樻い锔炬暬瀵顓奸崼顐ｎ€囬梻浣告啞閹稿鎳濇ィ鍐ｂ偓锕傚炊閵娧呯槇濠殿喗锕╅崜娑㈩敇閸濆嫧鏀介柣妯肩帛濞懷勭箾鐠囇呯暤闁诡噣娼ч…銊╁醇閻斿弶瀚?
     *
     * @param limit 闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯妞ゆ梻鍘ч～鈺呮⒑閸濆嫷鍎庣紒鈧笟鈧﹢渚€姊虹粙璺ㄧ闁告艾顑囩槐鐐哄箣閻愮數顔?
     * @return 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢敂钘変罕闂佸憡鍔﹂崰鏍婵犳碍鐓欓柟瑙勫姦閸ゆ瑧绱掗悪娆忔处閻撳啴鏌涘┑鍡楊仼闁逞屽墯閹倿骞冨鈧俊鐑藉煛閸屾粌骞愰梻浣告啞娓氭宕戦崟顖涘€堕柡灞诲劜閻?
     */
    @Override
    public List<ShopVO> getRecentShops(Integer limit) {
        return convertToShopVOList(query()
                .eq("status", 1)
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .orderByDesc("create_time")
                .last("limit " + limit)
                .list());
    }

    /**
     * 闂傚倸鍊搁崐椋庣矆娴ｈ櫣绀婂┑鐘插亞閻掔晫鎲搁弮鍫涒偓渚€寮介鐐茬獩濡炪倖妫佸Λ鍕嚕閸ф鈷戦柛鎰级閹牓鏌涢悤浣哥仸鐎规洑鍗冲畷鍗炩槈濞嗘垵寮抽梻浣告惈濞诧箓銆冮崨顔绢洸濡わ絽鍟崑鈩冪節婵犲倸顏紒鑸电叀閺屾稒绻濋崒婊€铏庨梺浼欑悼閸忔﹢銆佸☉銏″€烽柟缁樺笩閳ь剙鐏濋埞鎴︽倷閺夋垹浠搁梺鑽ゅ櫐缁犳挸鐣烽悽绋块唶闁靛鑵归幏铏圭磽娴ｅ壊鍎撴繛澶嬫礈缁鎮欓悜妯煎幈闂佸搫鍟犻崑鎾绘煕閵娿儳浠㈤柣锝囧厴楠炲酣鎳為妷锕€鍔掗梻渚€娼荤€靛矂宕㈡ィ鍐╂櫖婵犻潧顑嗛埛鎴︽偡濞嗗繐顏╅柛鏂诲€楅惀顏嗙磼閵忕姴绫嶅Δ鐘靛仦椤ㄥ﹪骞冮埡鍐＜婵☆垳鍘ч獮鍫ユ⒒娴ｅ憡璐￠柛搴涘€濋妴鍐幢濞戞ɑ鐎梻渚囧墮缁夌敻鎮?
     * 1. 濠电姷鏁告慨鐑藉极閹间礁纾婚柣鎰惈缁犱即鏌熼梻瀵割槮缂佺姷濮垫穱濠囶敍濠靛嫧鍋撻埀顒勬煛鐎ｎ亞效妤犵偞鐗曡彁妞ゆ巻鍋撳┑陇娉曢埀顒冾潐閹搁娆㈠璺鸿摕婵炴垯鍩勯弫鍐煥濠靛棙顥犳い锔哄劚閳规垿鎮欓崣澶婃闂佹悶鍔岄…鐑藉春閳ь剚銇勯幒鎴濃偓鍛婄椤栫偞鐓曞┑鐘插暞缁€瀣殽閻愬澧垫い銏℃礋閺佸倿鎸婃径澶岀泿闂傚倷绶氶埀顒傚仜閼活垱鏅堕鈧Λ浣瑰緞鐎ｎ剛鐦堟繝鐢靛Т閸婃悂寮抽敐鍡愪簻閹兼番鍩勫▓婊堟煛鐏炲墽娲村┑鈩冩倐婵＄兘鏁傞挊澶愭７闂傚倷鑳剁划顖氱暆閸濄儳涓嶉柡宥庡幖缁犵娀鏌ㄩ悢鍝勑㈤崶鎾⒑缁洖澧查柣鐕傜畱閳绘捇骞栨担鍏夋嫽婵炶揪绲介幉锟犲箟閹间焦鐓曢柣妯荤叀椤庢鎽堕悙瀵哥瘈闂傚牊渚楅崕蹇曠磼閻樿櫕銇濋柡宀嬬秮婵偓闁靛繆鏅濋崝鍝ョ磽娴ｆ彃浜鹃梺绯曞墲鐪夌紒璇叉閺屾洟宕煎┑鍥ф濡炪倕绻堥崕鐢稿蓟?Redis 缂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧湱鎲搁悧鍫濈瑨缂佺姳鍗抽弻鐔兼⒒鐎电濡介梺绋款儍閸婃繈寮婚弴鐔虹闁绘劦鍓氶悵鏃堝级?
     * 2. 濠电姷鏁告慨鐑藉极閹间礁纾婚柣鎰惈缁犱即鏌熼梻瀵割槮缂佺姷濮垫穱濠囶敍濠靛嫧鍋撻埀顒勬煛鐎ｎ亞效妤犵偞鐗曡彁妞ゆ巻鍋撳┑陇娉曢埀顒冾潐閹搁娆㈠璺鸿摕婵炴垯鍩勯弫鍐煥濠靛棙顥犳い锔哄劚閳规垿鎮欓崣澶婃闂佹悶鍔岄…鐑藉春閳ь剚銇勯幒鎴濃偓鍛婄椤栫偞鐓曞┑鐘插暞缁€瀣殽閻愬澧垫い銏℃礋閺佸倿鎸婃径澶岀泿闂傚倷绶氶埀顒傚仜閼活垱鏅堕鈧Λ?Redis GEO 缂傚倸鍊搁崐鎼佸磹瀹勯偊娓婚柟鐑樻⒒閻岸鏌涢锝嗙闁搞劌鍊块弻娑㈡晜鐠囨彃绠哄銈庡亝濞茬喖寮诲澶婁紶闁告洦鍓欏▍銈夋⒑缁嬪潡顎楃紒澶婄秺瀵鈽夐姀鐘插祮闂侀潧顭堥崕鎵姳閻撳簶鏀介柣鎰级閸ｅ綊鏌ｉ鐐测偓鎼侊綖韫囨拋娲敂閸曨厾鏆伴柣鐔哥矊闁帮綁鐛崱娑橀唶闁绘棁娅ｉ鏇㈡⒑缁嬭法绠抽柍宄扮墕閳绘挸顭ㄩ崨顏勪壕婵炲牆鐏濋弸娆戠磼椤旂晫鎳冮柣锝囧厴瀹曞ジ寮撮悙闈涘箰闂備礁鎲￠崝锕傛偂閸惊锝夋焼瀹ュ棌鎷洪柣鐘叉处瑜板啴顢楅姀銏㈢＜閻庯綆鍋勫ù顕€鏌℃担绋挎殻妞ゃ垺娲熸俊鍫曞幢閳哄倻绋愬┑鐘垫暩閸嬫稑螞濞嗘挸绠板┑鐘宠壘绾惧鏌曟繛鐐珕闁绘挸绻愰埞鎴︽倷閼碱兛铏庨梺閫炲苯澧い銊ワ躬楠炲﹤鈹戦崶銉ヤ簼闂佸憡鍔忛弬渚€骞忔繝姘拺闁告稑锕ユ径鍕煕鐎ｎ亜顏い銏℃椤㈡宕掑鍜冪床闂佽崵濮村ú锕傛偂閿熺姴鐭楅柛鏇ㄥ墻濞堜粙鏌ｉ幇顒夊殶濠⒀屼簼閵囧嫰顢樺鍐潎閻庤娲忛崝鎴︺€佸鈧幃娆撴偋閸繃鐏撻梻鍌氬€搁崐鐑芥倿閿旈敮鍋撶粭娑樻噽閻瑩鏌熷▎陇顕уú顓€佸鈧慨鈧柣姗€娼ф慨?
     *
     * @return 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佽法鍠撴慨瀵哥磼閳哄懏鈷戞い鎺嗗亾缂佸鎸抽幏鎴︽偄鐏忎焦鏂€闂佺粯蓱瑜板啴寮抽悢鎼炰簻闁冲搫鍟崢鎾煙椤旇偐绉虹€规洖鐖兼俊鎼佸Ψ瑜忛妶閿嬩繆閻愵亜鈧倝宕戞笟鈧畷鎰攽鐎ｎ亣鎽曢梺缁樻濞咃絿绮婚弮鈧换娑㈠箣閻愬棙鍨块弫宥咁吋婢跺鍘搁梺閫炲苯澧撮柟顔藉劤閻ｆ繈鍩€椤掑倸顥氶柦妯侯棦瑜版帗鏅查柛顐亝缂嶅牓鏌ｈ箛鎾剁闁告濞婂?
     */
    @Override
    public String flushCache() {
        //缂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧湱鎲搁悧鍫濈瑨缂佺姳鍗抽弻鐔兼⒒鐎电濡介梺绋款儍閸婃繈寮婚弴鐔虹瘈闊洦绋掗宥夋⒑缁嬫寧鎹ｉ柛鐘冲姈缁岃鲸绻濋崶顬囨煕濞戝崬鏋涙繛鍛€濆铏规兜閸涱喚褰ч梺鑽ゅ暱閺呯姴顕ｆ繝姘労闁告劏鏅涢鎾绘⒑閸涘﹦绠撻悗姘卞厴瀵娊顢橀姀鈥斥偓?
        String key = RedisConstants.CACHE_SHOP_lIST_KEY+"*";
        //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佺粯鍔曢顓犵不妤ｅ啯鐓冪憸婊堝礈濮樿鲸宕叉繛鎴欏灩瀹告繃銇勯幘鍗炵仼鐎殿喗婢樿灃闁绘﹢娼ф禒婊呯磼婢跺﹦绉虹€规洘妞介崺鈧い鎺嶉檷娴滄粓鏌熼悜妯虹仴妞ゅ繆鏅犻幃妤€顫濋澶嬓﹀銈庡弨濞夋洟骞戦崟顖涘仏闁哄鍨甸～鐘绘⒒娴ｅ憡鎯堟俊顐ｇ懇閵嗗啯绻濋崒銈嗙稁濠电偛妯婃禍婊呯不閺屻儲鐓欓梺顓ㄧ畱瀵儳霉閻橆偅娅囩紒杈ㄦ尰閹峰懘鎮烽悧鍫熸瘞濠电姷顣介埀顒冩珪閸犳﹢鏌涢埞鎯т壕?
        redisService.deleteObject(redisService.keys(key));
        List<ShopType> shopTypeList = shopTypeService.list();
        shopTypeList.forEach(shopType -> {
            List<Shop> shopList = query()
                    .eq("type_id", shopType.getId())
                    .eq("status", 1)
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .list();
            //缂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧湱鎲搁悧鍫濈瑨缂佺姳鍗抽弻鐔兼⒒鐎电濡介梺?
            redisService.setCacheObject(RedisConstants.CACHE_SHOP_lIST_KEY+shopType.getId(), shopList);
        });


        //缂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧湱鎲搁悧鍫濈瑨缂佺姳鍗抽弻鐔兼⒒鐎电濡介梺绋款儍閸婃繈寮婚弴鐔虹瘈闊洦绋掗宥夋⒑缁嬫寧鎹ｉ柛鐘冲姈缁岃鲸绻濋崶顬囨煕濞戝崬鏋涙繛鍛€濆铏规兜閸涱喚褰ч梺鑽ゅ暱閺呯姴顕ｇ拠娴嬫婵犲﹤瀛╂瓏闂傚倷绀侀幉鈥愁潖瑜版帗鍋嬮柣妯款嚙缁犳牠鏌曟径鍡樻珕闁哄懏褰冮…璺ㄦ崉閻氬瓨鏁鹃梺璇插瘨閸撶喎顫忛搹鍦＜婵妫欓悾鍫曟⒑缂佹﹩娈旀俊顐ｇ〒閸?
        List<Shop> list = query()
                .eq("status", 1)
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .list();
        //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佺粯鍔曢顓犵不妤ｅ啯鐓冪憸婊堝礈濮樿鲸宕叉繛鎴欏灩瀹告繃銇勯幘鍗炵仼鐎殿喗婢樿灃闁绘﹢娼ф禒婊呯磼婢跺﹦绉虹€规洘妞介崺鈧い鎺嶉檷娴滄粓鏌熼悜妯虹仴妞ゅ繆鏅犻幃妤€顫濋澶嬓﹀銈庡弨濞夋洟骞戦崟顖涘仏闁哄鍨甸～鐘绘⒒娴ｅ憡鎯堟俊顐ｇ懇閵嗗啯绻濋崒銈嗙稁濠电偛妯婃禍婊呯不閺屻儲鐓欓梺顓ㄧ畱瀵儳霉閻橆偅娅呴柍瑙勫灴閹瑩鎳犻鈧·鈧梻浣规偠閸斿矂鎮樺┑瀣仼闁绘垼妫勭粻鐟懊归敐鍛喐闁汇倐鍋撳┑锛勫亼閸婃牠骞愰悙顒佸弿闁圭虎鍠楅崐鍫曞级閸碍娅囩痪鍙ョ矙閺屾稓浠﹂幑鎰棟闂侀€炲苯澧紓宥咃躬閻涱噣骞囬弶璺啇婵炶揪绲块幊鎾寸?
        redisService.deleteObject(redisService.keys(RedisConstants.SHOP_GEO_KEY+"*"));
        //闂傚倸鍊搁崐鐑芥嚄閸洏鈧焦绻濋崒妤佺亙濠电偞鍨崹娲疾濠靛鐓忛柛顐ｇ箖缁屽灝鈹戦钘夆枙闁哄矉缍佹慨鈧柣妯哄暱閺嗗牓姊虹紒妯绘儓缂傚秳绶氬璇测槈濡攱鏂€闂佺硶鍓濋〃蹇旂婵傚憡鈷戦悹鍥у级閸炲鈧娲滈弫绋课?闂傚倸鍊搁崐椋庣矆娴ｉ潻鑰块梺顒€绉埀顒婄畵瀹曞ジ濡风€ｎ亝鍠橀柡灞芥椤撳ジ宕ㄩ婊冨箑闂佽娴烽幊鎾诲箟閳ヨ櫕绾痯eId闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佸湱鍎ら崵锕€鈽夊Ο閿嬵潔濠殿喗锕╅崜娆撴倶?id婵犵數濮烽弫鎼佸磻閻愬搫鍨傞柛顐ｆ礀缁犱即鏌熺紒銏犳灈缁炬儳顭烽弻鐔煎礈瑜忕敮娑㈡煟閹捐泛啸闁逞屽墰閹虫挾鈧凹鍘界粩鐔煎幢濞戞顔戦梺缁橆焽缁垶鎮″☉銏＄厱閻忕偛澧介幊鍛磼閹邦厽鈷掗柍褜鍓濋～澶娒哄Ο鐓庢瀳鐎广儱顦伴弲顒勬煙闁箑骞楀┑顖涙綑閵嗘帒顫濋悡搴ｄ户缂備浇椴搁敋闁宠鍨块幃娆撳级閹寸姳妗撻梻浣烘嚀閻ㄧ兘寮插鍛焿鐎广儱顦介弫鍌炴煕閺囨ê濡介柣蹇撳暣濮婃椽骞愭惔銏㈩槬闂佺锕ら幗婊堝箟?
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佸湱鍎ら崵锕€鈽夊Ο閿嬬€婚梺褰掑亰閸犳俺銇愭ィ鍐┾拺闁革富鍘奸崝瀣亜閵娿儲鍣界紒杈ㄥ浮閸╋繝宕ㄩ鎯у箥婵＄偑鍊栭悧鏇炍涘Δ鍛柈妞ゆ劏鎳ｈ閹鍩￠　鐚闂傚倸鍊搁崐鎼佸磹閻戣姤鍊块柨鏇氶檷娴滃綊鏌涢幇闈涙灍闁稿孩顨婇弻娑氫沪閹冩瘓闂?
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            //闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯闁靛繆鍓濋悵鏍煟韫囨挾绠抽柡浣割煼楠炲啳銇愰幒鎴犲€炲銈呯箰鐎氼喖袙閵忋倖鈷戦梺顐ゅ仜閼活垱鏅堕悧鍫涗簻妞ゆ挾濮甸悵?
            Long typeId = entry.getKey();
            String shopGeoKey = RedisConstants.SHOP_GEO_KEY + typeId;
            //闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯妞ゆ梻鍘ч～鈺呮煟閻斿摜鎳曞┑鈥虫喘閸┾偓妞ゆ巻鍋撻柛妯荤矒瀹曟垿骞樼紒妯煎帗閻熸粍绮撳畷婊冣槈閵忕姷鐣哄銈嗘磵閸嬫挻銇勯姀鈩冪闁轰礁鍟撮崺鈧い鎺戝閸ㄥ倿鏌涘畝鈧崑鐐哄磹閻㈠憡鐓ユ繝闈涙椤庢顭胯閸ㄥ爼寮婚敐澶嬫櫜濠㈣泛顦伴崰姘旈悩闈涗沪闁绘绮撻崺鈧い鎺嶈兌閳洟鏌ㄩ弴妤佹珔闁崇粯鎸搁…銊╁醇閻斿搫骞?
            List<Shop> shopList = entry.getValue();
            //闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢敃鈧壕鐟懊归悩宸劀缂傚秵鐗曢…璺ㄦ崉閻戞ɑ鎷遍梺绋跨箲缁捇寮婚妶澶婁紶闁靛闄勫В鍕⒑瀹曞洨甯涙俊顐㈠暣瀵?闂傚倷娴囬褍霉閻戣棄鏋佸┑鐘宠壘绾捐鈹戦悩鎻掓殶妞も晜鐓￠弻锝夊箛椤栨侗浠╅悗瑙勬尫缁舵岸寮诲☉銏犵労闁告劦浜栧Σ鍫㈢磽娴ｆ彃浜鹃梺鍛婂姀閺呮繄绮绘ィ鍐╃厱闁斥晛鍠氬▓妯肩磼閻樺啿顥嬮柍?
            for (Shop shop : shopList) {
                //闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鑼槷闂佸搫绋侀崢浠嬪磻閿熺姵鐓忓璺烘濞呭懘鏌ｉ鐕佹疁闁哄本鐩幃娆戔偓娑欘焺椤ょ導s  GEOADD key 缂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗霉閿濆洤鍔嬬€规洘鐓￠弻鐔兼焽閿曗偓閺嬫捇鏌?缂傚倸鍊搁崐鎼佸磹閻戣姤鍤勯柛顐ｆ礃閹偤骞栧ǎ顒€濡肩紒鈧崼鐔虹闁糕剝锚閻忓秹鏌?member
                redisService.addCacheGeoLocation(shopGeoKey, shop.getX(), shop.getY(), shop.getId().toString());
            }
        }
        return "shop geo cache rebuilt";
    }
    /**
     * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭画闂佹寧娲栭崐鎼佸垂閸岀偞鐓曠憸搴ㄣ€冮崨瀛樺€块柛顭戝亖娴滄粓鏌熸潏鍓хɑ缁绢叀鍩栭妵鍕晜婵傚憡顎嶉梺闈涙搐鐎氫即鐛Ο灏栧亾濞戞顏堝焵椤掍礁濮夐柍褜鍓氶鏍窗閺囩姴鍨濇繛鍡楃箳閺嗭箓鏌曟繝蹇擃洭缂佲檧鍋撻梻浣告啞濞叉﹢宕归鐐茬？闁汇垻顭堥拑鐔兼煟閺冨倵鎷￠柡浣告川閹茬顓兼径瀣偓鍫曟煟濡偐甯涢柣鎾存礋閺屽秵娼幍顔跨獥闂佸憡鏌ㄩ幗?
     *
     * @param shop 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ラ梻鍌欑劍閻綊宕规繝姘モ偓鍌涚鐎ｎ亞锛涢梺瑙勫礃椤曆囧箲閼哥偣浜滈柟鎹愭硾娴犳帞绱掗銊ユ噽绾?
     */
    private void sendAuditMessage(Shop shop) {
        AuditMessage auditMessage = AuditMessage.builder()
                .bizId(shop.getId())
                .bizType(GlobalBizTypeEnum.SHOP.getCode())
                .submitterId(SecurityContextHolder.getUserId())
                .auditContent(BeanUtil.beanToMap(shop))
                .createTime(shop.getCreateTime())
                .build();
        mqMessageSendUtils.sendMqMessage( AiAuditMqConstants.AUDIT_DIRECT_EXCHANGE,AiAuditMqConstants.AUDIT_ROUTING_KEY, auditMessage);
    }
    /**
     * 濠电姷鏁告慨鐑藉极閹间礁纾婚柣鎰惈缁犱即鏌熼梻瀵割槮缂佺姷濞€閺岀喖鎮ч崼鐔哄嚒闂佺粯鎸婚敃銏ゅ蓟閳ユ剚鍚嬮幖绮光偓宕囶啇缂傚倷鑳舵慨鎶藉础閹惰棄钃熸繛鎴欏灩鍞梺鐟扮摠缁诲啴宕抽悜妯诲弿闁挎繂鎳橀崣鍕叏婵犲啯銇濈€规洏鍔嶇换婵嬪磼濠婂懏鍣┑鐘殿暯濡插懘宕戦崨顖滅煓闁规崘顕х粻鏍ㄣ亜閺囨浜惧銈冨灪濞茬喖寮崒鐐村癄濠㈣泛锕らˉ澶愭⒒閸屾瑧顦﹂柟娴嬧偓瓒佹椽鏁冮崒姘憋紱闂佺硶鍓濈粙鎴澬ч崣澶夌箚闁靛牆鎳忛崳娲煃闁垮鐏撮柡宀€鍠栭、娑㈠幢濡も偓椤忣亪鏌涙惔銏″磳婵﹥妞藉畷婊堟嚑椤掆偓鐢儲绻涚€涙鐭岄柛瀣ㄥ€濋悰顕€宕橀埡渚囧殼闂佸搫顦伴崵锕傚煛閸愨晛鏋戦棅顐㈡处閺屻劎娆㈤悙鐑樼厱闁哄洢鍔岄悘锟犳煕?
     *
     * @param typeId 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佸湱鍎ら崵锕€鈽夊Ο閿嬵潔濠殿喗锕╅崢鍏肩韫囨稒鈷戠紓浣诡焽閵嗘帡鏌?
     */
    private void flashShopListRedisCache(Long typeId) {
        //濠电姷鏁告慨鐑藉极閹间礁纾婚柣鎰惈缁犱即鏌熼梻瀵割槮缂佺姷濞€閺岀喖鎮ч崼鐔哄嚒闂佺粯鎸婚敃銏ゅ蓟閳ユ剚鍚嬮幖绮光偓鑼晼缂備焦鍎宠ぐ鐐靛垝濞嗘挸钃熼柣鏃傚帶缁犳氨鎲歌箛娑欐櫖鐎广儱顦伴悡?
        redisService.deleteObject(RedisConstants.CACHE_SHOP_lIST_KEY+typeId);
    }

    /**
     * 濠电姷鏁告慨鐑藉极閹间礁纾婚柣鎰惈缁犱即鏌熼梻瀵割槮缂佺姷濞€閺岀喖鎮ч崼鐔哄嚒闂佺粯鎸婚敃銏ゅ蓟閳ユ剚鍚嬮幖绮光偓宕囶啇缂傚倷鑳舵慨鎶藉础閹惰棄钃熸繛鎴欏灩鍞梺鐟扮摠缁诲啴宕抽悜妯诲弿闁挎繂鎳橀崣鍕煛瀹€鈧崰鏍箖閳╁啯鍎熼柨婵嗘閸犳牗淇婇悙顏勨偓鎴﹀礉瀹€鍕櫇妞ゅ繐鐗忓畵浣逛繆閵堝懏鍣洪柛瀣ㄥ姂閺屾稑鈽夊鍫濅紣闂佸憡鎼╅崣鍐潖濞差亜宸濆┑鐘插暙椤︹晠姊洪棃鈺冪У闁革綇绲介悾鐑藉垂椤愩倗鐦堥梺鎼炲劘閸斿酣宕㈤棃娑辨富闁靛牆妫涙晶顒傜棯閺夎法效鐎规洘濞婇幃鎯х暆閳ь剛澹曟總鍛婄厵闁圭粯甯╅崕蹇斻亜閳哄﹤澧撮柡?
     *
     * @param id 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ョ紓鍌氬€风拋鏌ュ磻閹剧繝绻?
     */
    private void flashShopRedisCache(Long id){
        redisService.deleteObject(RedisConstants.CACHE_SHOP_KEY+id);
    }

    /**
     * 闂傚倸鍊搁崐鐑芥嚄閸洍鈧箓宕奸姀鈥冲簥闂佸壊鍋侀崕杈╃矆婢舵劖鐓欓弶鍫濆⒔閻ｉ亶姊婚崒銈呯仭缂佺粯绋戦蹇涱敊閼姐倗娉块梻浣规た閸樹粙銆冩繝鍥ц摕闁绘棁銆€閸嬫挸鈽夊▎妯煎姺缂備胶濮佃摫闁靛洤瀚粻娑㈠籍閳ь剛浜搁敂绛嬫闁绘劕顕晶閬嶆煃瑜滈崜姘辩矙閹烘鏅濋柍杞拌閺嬫梹绻濇繝鍌滃闁绘挻鐟╅弻锝夋晲閸涱喗鎷遍梺鍝勬－閸撴盯鍩€椤掍緡鍟忛柛锝庡櫍瀹曟垶绻濋崶褏鐣洪悷婊勬煥閻ｇ兘鎮℃惔妯绘杸闂佸壊鍋呯粙鎴炵娴煎瓨鈷掗柛灞剧懄缁佷即鏌涚€ｎ偄濮嶇€殿喚绮鍕箾閻愵剛鈧椽姊洪崫鍕偍闁搞劌婀辩划濠氬冀椤愮喎浜鹃悷娆忓缁€鈧┑鐐额嚋缁犳捇鏁愰悙鍝勫嵆闁靛骏绱曢崢顏堟椤愩垺澶勬繛鍙夌墪閺嗏晝绱撻崒娆戭槮闁兼椿鍨伴…鍥晸閻樿尪鎽?
     * 闂傚倸鍊搁崐鎼佸磹閻戣姤鍊块柨鏇氶檷娴滃綊鏌涢幇鐢靛帒婵炲樊浜滄儫闂佸疇妗ㄩ悞锕傛倵椤掑嫭鈷戦梻鍫熺〒婢ф洘銇勯敂璇茬仯缂侇喖鐗撳畷鍗炩槈濞嗘垵骞堥梻浣规灱閺呮盯宕导鏉戠厽闁靛牆顦伴悡鍐喐濠婂牆绀堟繛鍡樻嫴閸ヮ剚顥堟繛鎴濆船濞堛劍绻濋悽闈浶ｉ柤瑙勫劤閺侇喗淇婇悙顏勨偓銈夊储娴犲鍨傞梻鍫熺▓閺嬫棃鏌熸潏楣冩闁抽攱鍨垮鍫曟倷閺夋埈鈧粓鏌涜箛鎾瑰妞ゎ叀鍎婚ˇ铏亜閵娿儲鍤囬柟顕€绠栧畷褰掝敃閿濆拋鈧捇鏌ｉ悢鍝ユ噧閻庢凹鍓熷畷婵嬪川鐎涙ǚ鎷洪柣鐔哥懃鐎氼剟宕濋妶澶嬬厽闁靛牆鎳忓婵嬫煃瑜滈崜姘卞枈瀹ュ鍋￠柍鍝勬噹閽冪喐绻涢幋娆忕仼缂佺姵濞婇弻娑㈠焺閸忊晜鍨块幃鐢稿冀椤撶啿鎷洪柡澶屽仒閸楀啿顬婇悜鑺ョ厱濠电姴鍠氬▓婊堟煙椤栨浜炬俊鐐€曠换鎰涘▎蹇曟殾濠㈣埖鍔栭悡蹇撯攽閻愯尙浠㈤柣蹇ｄ邯閺岋綁骞樼捄鐩掋垽鏌嶇憴鍕伌妞ゃ垺鐟╁畷妤呮嚌閹殿噮鏀ㄦ繝鐢靛У椤旀牠宕伴弽顓ф晪妞ゆ挾鍊ｉ敐澶婄疀闁哄娉曢鎺楁⒑瑜版帩鏆掗柣鎺炵畱椤╁ジ濡搁埡鍌楁嫼闂傚倸鐗婃笟妤€危閸洘鐓曢幖娣€撻崥顐も偓鍨緲閿曘倗鍙呭銈呯箰閹冲骞?MQ 闂傚倸鍊搁崐椋庣矆娴ｈ櫣绀婂┑鐘插亞閻掔晫鎲歌箛鏇燁潟闁绘劕顕弧鈧梺鎼炲劀閸ヮ煉绱┑鐘垫暩閸嬫稑螣婵犲啰顩叉繝濠傛噺閸犲棝鏌ㄩ弴鐐测偓褰掑磹閸洘鐓熼柟閭︿簽缁侀攱淇婇悙顒佸€愰柡灞剧洴瀵剟宕归鍛棯缂傚倷鑳剁划顖炴儎椤栫偟宓侀悗锝庝簴閺€浠嬫煕椤愶絿绠ラ柡澶岊焾閳规垿鎮欏顔兼闂佸憡顭嗛崶褏鐤囧┑顔姐仜閸嬫挾鈧鍣崑鍕敇婵傜宸濇い鏍ㄧ⊕閻ｇ兘姊绘笟鈧埀顒傚仜閼活垱鏅堕幘顔界厱闁靛绠戦崝銈夋懚閿濆鐓犲┑顔藉姇閳ь剚鍔欏?ES 婵?Milvus 闂傚倸鍊搁崐宄懊归崶褜娴栭柕濞炬櫆閸ゅ嫰鏌ょ粙璺ㄤ粵婵炲懐濮垫穱濠囧Χ閸屾矮澹曢梻浣风串缁蹭粙鎮樺杈╃當闁绘梻鍘ч悞鍨亜閹哄棗浜惧銈嗘穿缂嶄線骞冮悾宀€鐭欓悹鎭掑妼婵附淇婇悙顏勨偓鏍暜閹烘柡鍋撳鐓庡閾荤偞鎱ㄥ璇蹭壕闂佸搫琚崐婵嗙暦濠婂牆绠甸柟鐑樺灥缁狅綁姊?
     *
     * @return 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭画闂佹寧娲栭崐鎼佸垂閸岀偞鐓熼柕蹇嬪灪閺嗏晠鏌涘Δ浣稿摵婵﹥妞藉畷褰掝敋閸涱厼澹嬫俊鐐€愰弲婵嬪礂濮椻偓楠炲啳銇愰幒鎴犲€為梺闈涱焾閸庡磭绮婂畡閭︽富闁靛牆楠告禍鏍煕婵犲啰绠炵€殿喛顕ч埥澶婎潨閸℃ê鍏婇梻浣虹帛閹哥霉闁垮顩烽柍鍝勫€荤弧鈧┑鐐茬墕閻忔繈寮搁幘缁樼厸闁告侗鍠氱粻鐐搭殽閻愯尙绠婚柟顔规櫇閸犲﹤螣鏉炴澘顥氭繝鐢靛仦閸ㄥ爼顢旀导鏉戠闁挎棁鍋愮粙?
     */
    @Override
    public String allPublish() {
        int page = PageConstants.PAGE_NUMBER;
        int pageSize = PageConstants.ES_PAGE_SIZE; // 濠电姷鏁告慨鐢割敊閺嶎厼闂い鏍ㄧ矊缁躲倝鏌ｉ敐鍛拱鐎规洖寮堕幈銊ヮ渻鐠囪弓澹曞┑?0闂?
        while (true) {
            // 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佸湱鍎ら崵锕€鈽夊Ο閿嬫杸闁诲函缍嗛埀顒夊幑閸庨亶鈥﹂崸妤佸殝闂傚牊绋戦～宀勬⒑閽樺鏆熼柛鐘崇墵瀵寮撮悢椋庣獮濠碘槅鍨抽崢褏鏁妷鈺傗拺?
            List<Shop> shops = query()
                    .eq("status", 1)
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .page(new Page<>(page, pageSize))
                    .getRecords();
            if (shops.isEmpty()) {
                break;
            }
            int finalPage = page;
            //婵犵數濮烽弫鎼佸磻閻樿绠垫い蹇撴缁€濠囨煃瑜滈崜姘辨崲濞戞瑥绶為悗锝庡亞椤︿即鎮楀▓鍨珮闁稿锕ユ穱濠囧醇閺囩偛鑰垮┑掳鍊曢崯浼存偘椤旂晫绡€婵炲牆鐏濋弸鐔兼煥濮樻墎鍋撶憴鍕闁告挻鐟╅崺銏ゅ箻缂佹ê浜归梺姹囧灮閺佹悂鏁嶅鍫熺厽閹兼惌鍨崇粔闈浢瑰鍐煟鐎规洘娲熼獮鍥敇閻樻鍟庨梻浣虹帛閹哥霉闁垮顩锋繛鎴炲焹閸嬫捇宕归锝囧嚒闁诲孩纰嶅姗€鎮鹃悜鑺ュ亜缁炬媽椴搁弲锝夋⒑缂佹ɑ鐓ラ柣銊︾箞瀹?
            executorService.submit(()->{
                List<ShopVO> voList = convertToShopVOList(shops);
                voList.forEach(vo -> vo.setLocation(vo.getY() + "," + vo.getX()));
                // 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭画闂佹寧娲栭崐鎼佸垂閸岀偞鐓曠憸搴ㄣ€冮崨瀛樺€块柛顭戝亖娴滄粓鏌熸潏鍓хɑ缁绢厼鐖奸弻娑㈠棘鐠恒剱銏°亜椤撯剝纭堕柟鍙夋尦瀹曠喖顢曢敐鍫㈡闂傚倷绀侀幖顐︽嚐椤栫偞鍎楀ù锝堟娑撳秵绻涢幋娆忕仼缂侇偄绉归弻娑氫沪閽樺鍋撻崼鏇炵？闁汇垻顭堥拑?
                sendShopBatchMessage(voList);
                log.info("async sync shop page thread={}, page={}, size={}", Thread.currentThread().getName(), finalPage, shops.size());
            });
            page++;
        }
        return "shop sales sync task submitted";
    }


    /**
     * 闂傚倸鍊搁崐椋庣矆娴ｈ櫣绀婂┑鐘插亞閻掔晫鎲歌箛鏇燁潟闁绘劕顕弧鈧梺鎼炲劀閸ヮ煉绱┑鐘垫暩閸嬫稑螣婵犲啰顩叉繝濠傜墛閸庢淇婇妶鍛櫤闁绘挻鐟╅幃妤€鈽夊▎妯煎姺缂備胶濮佃摫闁靛洤瀚粻娑㈠籍閳ь剛浜搁敂绛嬫闁绘劕顕晶閬嶆煃瑜滈崜姘辩矙閹烘鏅濋柍杞拌閺嬫梹绻濇繝鍌滃闁绘挻鐟╅弻锝夋晲閸涱喗鎷遍梺鍝勬－閸撶喖寮婚悢鍏兼優妞ゆ劑鍊栭崚娑㈡⒑鐠団€崇仩闁活厼鍊块獮鍡涘礋椤栨鈺呮煃閸濆嫬鈧宕㈤崡鐐╂斀闁绘顕滃銉╂煕閻樺啿鍝虹€规洘绻堝畷顒勫础閳虹灝s缂傚倸鍊搁崐鎼佸磹瀹勯偊娓婚柟鐑樻⒒閻岸鏌涢锝嗙闁搞劌鍊块弻娑㈡晜鐠囨彃绠哄?
     *
     * @param ids 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ョ紓鍌氬€风拋鏌ュ磻閹剧繝绻嗘俊銈咃梗闁垱鎱ㄦ繝鍕妺婵炵⒈浜獮宥夘敊閻撳寒鐎存繝鐢靛О閸ㄥ骞婇幘婢勬稑鈽夐姀鐘殿槴?
     * @return 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭画闂佹寧娲栭崐鎼佸垂閸岀偞鐓熼柕蹇嬪灪閺嗏晠鏌涘Δ浣藉妞ゎ亜鍟伴埀顒婄秵娴滄繈骞戦敐鍥╂／闁诡垎浣镐划闂佸搫鐬奸崰鏍嵁閸℃凹妾ㄩ梺鎼炲€楅崰搴ㄦ箒?
     */
    @Override
    public String publish(String[] ids) {
        if (ids == null || ids.length == 0) {
            return "ids are required";
        }
        // Convert to Long list
        List<Long> idList = Arrays.stream(ids)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        executorService.submit(() -> {
            log.info("async publish shop batch thread={}, ids={}", Thread.currentThread().getName(), idList);
            // Batch query
            List<Shop> shops = query()
                    .in("id", idList)
                    .eq("status", 1)
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .list();
            if (CollUtil.isNotEmpty(shops)) {
                List<ShopVO> voList = convertToShopVOList(shops);
                // Set location
                voList.forEach(vo -> vo.setLocation(vo.getY() + "," + vo.getX()));
                // Batch send message
                sendShopBatchMessage(voList);
            }
        });
        return "publish task submitted";
    }

    /**
     * 闂傚倸鍊搁崐椋庣矆娴ｈ櫣绀婂┑鐘插亞閻掔晫鎲歌箛鏇燁潟闁绘劕顕弧鈧梺鎼炲劀閸ヮ煉绱┑鐘垫暩閸嬫稑螣婵犲啰顩叉繝濠傜墛閸庢淇婇妶鍛櫤闁绘挻鐟╁鍫曞醇閻旈顦ㄩ梺鍛婅壘閸婂潡寮诲☉姘ｅ亾閿濆骸浜濋悘蹇斿缁辨帞鈧綆浜跺Ο鈧Δ鐘靛仜缁夊綊鐛鈧、娆撴偩鐏炶棄绠炲┑鐘垫暩婵挳鏁冮妶澶婄獥婵娉涚粈澶愭煕閻愭彃鍨倂us闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐叉疄婵°倧绲介崯顐も偓姘槹閵囧嫰骞掗幋婵愪紝闂佽桨绀佸Λ婵嬪蓟閿濆绠涙い鎾跺О閸嬬偛鈹戦埄鍐ㄧ祷闁绘鎹囧璇测槈閵忊晜鏅濋梺闈涚墕閹冲繘鎮楅搹鍦＝?
     *
     * @param shops 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ラ梻鍌欑劍閻綊宕规繝姘ｂ偓锕傚醇閵忋垻鐒奸梺绯曞墲鐪夌紒璇叉閺屾洟宕煎┑鍥ф濡炪倕绻堥崕鐢稿蓟?
     */
    private void sendShopBatchMessage(List<?> shops) {
        if (CollUtil.isEmpty(shops)) {
            return;
        }
        ContentBatchSyncMessage request = new ContentBatchSyncMessage();
        request.setIndexName(EsIndexNameConstants.SHOP_INDEX_NAME);
        request.setData(shops);
        request.setType(GlobalBizTypeEnum.SHOP.getCode());
        
        // 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭画闂佹寧娲栭崐鎼佸垂閸岀偞鐓曠憸搴ㄣ€冮崨瀛樺€块柛顭戝亖娴滄粓鏌熸潏鍓у埌闁告梻鏁婚弻娑㈡偐閹颁焦绁瞓bitmq濠电姷鏁告慨鐑藉极閹间礁纾婚柣鎰惈閸ㄥ倿鏌ｉ姀鐘冲暈闁稿顑呴埞鎴︽偐閹绘帗娈銈嗘礋娴滃爼寮诲☉妯锋婵炲棙鍔楃粙鍥⒑閸濆嫷鍎庣紒鑸靛哺瀵鏁愰崨鍌涙閸┾偓妞ゆ帒瀚崑瀣煕閳╁啰鎳呴柣顓炵墦閺屻劑寮撮悙娴嬪亾瑜版帗鍋傛繛鍡樺灩绾捐棄霉閿濆拋娼犳い蹇撴缁犳棃鏌熼悜姗嗘畷闁绘挻娲樼换娑㈠幢濡ゅ啰顔夐梺杞扮劍铻?
        mqMessageSendUtils.sendMqMessage( SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_BATCH_INSERT_ROUTING_KEY, request);
        // 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭画闂佹寧娲栭崐鎼佸垂閸岀偞鐓曠憸搴ㄣ€冮崨瀛樺€块柛顭戝亖娴滄粓鏌熸潏鍓у埌闁告梻鏁婚弻娑㈡偐閹颁焦绁瞓bitmq濠电姷鏁告慨鐑藉极閹间礁纾婚柣鎰惈閸ㄥ倿鏌ｉ姀鐘冲暈闁稿顑呴埞鎴︽偐閹绘帗娈銈嗘礋娴滃爼寮诲☉妯锋婵炲棙鍔楃粙鍥⒑閸濆嫷鍎庣紒鑸靛哺瀵鏁愰崨鍌涙閸┾偓妞ゆ帒瀚崑瀣煕閳╁啰鎳呴柣顓炵墦閺屻劑寮撮悙娴嬪亾瑜版帗鍋傛繛鍡樺灩绾捐棄霉閿濆拋娼犳い蹇撴缁犳棃鏌熼悜姗嗘畷闁绘挻娲樼换娑㈠幢濡ゅ啰顔夐梺璇″灠鐢珮vus
        mqMessageSendUtils.sendMqMessage( SearchMqConstants.MILVUS_SYNC_EXCHANGE, SearchMqConstants.MILVUS_SYNC_BATCH_INSERT_ROUTING_KEY, request);
    }

    /**
     * 闂傚倸鍊搁崐椋庣矆娴ｈ櫣绀婂┑鐘插亞閻掔晫鎲歌箛鏇燁潟闁绘劕顕弧鈧梺鎼炲劀閸ヮ煉绱┑鐘垫暩閸嬫稑螣婵犲啰顩叉繝濠傜墕绾偓闂佽鍎兼慨銈夊磹閸偆绠鹃柟瀵稿剱娴煎棝鏌熸潏楣冩闁稿鍊块弻宥堫檨闁告挾鍠庨～蹇撁洪鍕炊闂侀潧顦崕鏌ユ倵鐠囨祴鏀介柍钘夋娴滄繄绱掔拠鑼ⅵ闁绘侗鍠栬灃闁告侗鍠栨禒铏圭磽娴ｅ壊鍎撴繛澶嬫礋瀹曨偄螖閸涱喒鎷洪柣鐘叉礌閳ь剙纾禒顖涚節閳封偓閸曞灚鐤侀柦妯煎枛閺岀喐娼忔ィ鍐╊€嶉梺?(缂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸ゅ嫰鏌涢锝嗙缂佹劖顨婇弻锟犲炊閵夈儳鍔撮梺杞扮濞差參寮婚悢鍏尖拻閻庣數顭堟俊浠嬫⒑缂佹ê绗掓い顓犲厴瀵鏁撻悩鑼槹濡炪倖鍔戦崐妤咁敊婵犲洦鐓?
     * 1. 闂傚倸鍊搁崐鎼佸磹閻戣姤鍊块柨鏇氶檷娴滃綊鏌涢幇鐢靛帒婵炲樊浜滄儫闂佸疇妗ㄩ悞锕傛倵椤掑嫭鈷戦梻鍫熺〒婢ф洘銇勯敂璇茬仯缂侇喖鐗撳畷鍗炩槈濞嗘垵骞堥梻浣规灱閺呮盯宕板顑芥灁闁哄啫鐗婇悡鐔兼煥閺囨浜鹃梺缁樼墪閵堢顕ｆ繝姘亜闁稿繒鍘ф禒娲⒑閸撹尙鍘涢柛瀣閳绘挾浠︾粵瀣瘜闂侀潧鐗嗙换妤呭触閸岀偞鐓涢柛娑卞灠閳诲牊顨ラ悙鎻掓殻鐎规洖銈稿鎾倷閼煎灈鍋撻崹顔规斀闁宠棄妫楅悘锟犳煕閺冣偓鐢偟鍒掗崼鐔虹懝闁逞屽墴閵嗕礁顫濋懜鍨珳闂佸壊鍋侀崹濠氬级閹间焦鈷戦梻鍫熷喕缁憋繝鏌涘鈧悞锕€鏆╅梻鍌氬€搁崐鎼佸磹閹间礁纾归柟闂寸閻ゎ喗銇勯幇鈺佺労闁搞倖娲熼弻娑㈩敃閿濆棗顦╅梺?SQL 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭画闂侀潧顦弲娑氬閸︻厽鍠愰柣妤€鐗嗙粭鎺撴叏鐟欏嫮鍙€闁哄矉缍佸顒勫箰鎼粹剝娈樺┑鐐茬摠閸ゅ酣宕愬┑瀣摕婵炴垯鍨瑰敮濡炪倖姊婚崢褔锝為鍕ㄦ斀妞ゆ洖妫涚粙濠氭煕閺冣偓閻熲晛顕?
     * 2. 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢埛姘そ婵¤埖寰勭€ｎ亙妲愰梻渚€娼ц墝闁哄懏鐩幏鎴︽偄鐏忎焦鏂€闂佺粯锚瀵爼骞栭幇鐗堝仭婵炲棙鐟ч悾鍨殽閻愬澧遍柛鎺撳浮閹兘寮舵惔鎾村珶闂傚倷鑳堕…鍫ユ晝閵婏负浠堥柟闂寸缁犵偤鏌曟繛鍨姉闁衡偓娴犲鐓曢柡鍥ュ妼娴滄粌鈹戦濂稿弰婵﹨娅ｉ崠鏍即閻斿摜褰嗛梻浣哄劦閺呪晠宕规导鎼晪?Redis 闂傚倸鍊峰ù鍥х暦閸偅鍙忛柡澶嬪殮瑜版帒绀嬫い鏍ㄧ▓閹稿啴姊洪崨濠冨闁搞劍澹嗘竟鏇㈠锤濡ゅ啫褰勯梺鎼炲劘閸斿秶绮堥崼銏㈡／闁告瑣鍎抽惌娆撴煛瀹€鈧崰鏍嵁閸℃凹妲鹃梺鎸庣☉缁夊綊寮婚悢鍏煎仼閻忕偠袙閺嬪懘姊洪崫鍕拱缂佸鍨块崺銏℃償閵堝洨鏉搁梺闈涱槶閸庢壆妲愰敐澶嬧拻濞达絽鎲￠崯鐐烘煟閻旀潙鍔ら柟骞垮灩椤粓鍩€椤掆偓椤曪絿鎷犻懠顒佹畷闂佸憡鍔︽禍鐐哄级閹间焦鈷戦柛婵嗗瀹告繈鏌涚€ｎ偆鈯曢柟渚垮姂瀵濡烽敂鎯у箥闂備胶鍘у﹢杈ㄦ櫠濡ゅ嫨浜圭憸宥夊煘閹达箑鐏抽柧蹇ｅ亜缁愭稑鈹戦垾鍐茬骇闁告梹鐟╁顐﹀磼閻愬鍙嗗銈嗙墬閻喗绔熼弴鐐╂斀闁绘劖娼欓悘锔姐亜韫囷絽鏋涙い顓滃姂瀹曠厧鈹戦崼婵喰?
     *
     * @param updateMap 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅?ID -> 闂傚倸鍊搁崐宄懊归崶顒€违闁逞屽墴閺屾稓鈧綆鍋呭畷宀勬煛瀹€鈧崰鏍€佸☉姗嗙叆闁告劗鍋撳В澶嬩繆閻愵亜鈧垿宕曢弻銉ュ瀭闁汇垻纭堕埀顒婄畵閹虫顢涢敐鍠邦剟姊绘担渚劸妞ゆ垵鍟村畷鎰攽閸℃瑦娈?
     * @return 闂傚倸鍊搁崐椋庣矆娴ｉ潻鑰块梺顒€绉甸崑锟犳煙閹増顥夋鐐灲閺屽秹宕崟顐熷亾瑜版帒绾ч柛婵勫劤绾句粙鏌涚仦鎹愬闁逞屽墰閸忔﹢骞冮悙鐑樻櫇闁稿本姘ㄩ鍥ㄧ節閻㈤潧校缁炬澘绉瑰畷?
     */
    @Override
    public Boolean updateStarCountBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }

        // 闂傚倷娴囬褍霉閻戣棄纾婚柨婵嗩槸绾捐绻濋棃娑氬ⅱ缁炬崘濮ら幈銊ヮ潨閳ь剟路閸岀偛鏋佸┑鐘叉处閻撴洘绻涢幋鐑嗙劷闁圭晫濞€閺岋繝宕担绋款潽闂侀€涚┒閸斿矂锝炲鍫濆耿婵°倐鍋撻柍閿嬪笒閳规垿鎮欓懠顒佸嬀闂佺锕ョ换鍫濐嚕婵犳碍鏅柛鏇樺妼娴滈箖鏌ㄥ┑鍡涱€楀ù婊呭仱閺岋綁顢橀悜鍡楀壎闂佸搫鐭夌换婵嗙暦閸楃偐鏋庨煫鍥ㄦ⒒娴滎亪姊绘担鍛婃儓闁兼椿鍨堕幆灞炬媴閾忛€涚瑝濠电偞鍨崹褰掑础閹惰姤鐓熼柟瀛樼箖椤ョ偞銇勯妷锔剧煉婵﹦绮幏鍛村川婵犲倹娈橀梺姹囧焺閸亪寮查悩鑼殾闁硅揪绠戠粈瀣亜閺嶎煈鍤?闂傚倸鍊烽懗鍫曞磿閻㈢绀堟繝闈涚墛閺嗘粓鏌熼悜姗嗘當缂佺姷濞€閺岀喖骞戦幇顒傚帿閻?00)闂傚倸鍊搁崐鐑芥倿閿旈敮鍋撶粭娑樻噽閻瑩鏌熸潏楣冩闁搞倖鍔栭妵鍕冀閵娧呯厒缂佹儳褰炵划娆撳蓟閻旂厧绠查柟閭﹀墰閸橆偅绻濆▓鍨仩闁告艾顑夋俊鐢稿礋椤栨氨鐫勯梺绋挎湰閼圭偓绂掑鈧铏规嫚閺屻儺鈧銇勯鐘插幋鐎殿喖顭烽幃銏ゅ礂閻撳簼缂撻梻浣稿閻撳牓宕戦幒鎾额浄鐟滃繒妲愰幘瀛樺濞寸姴顑呴幗鐢电磽娴ｇ瓔鍤欓柣妤€妫濋敐鐐剁疀閺冨倿妾紓浣割儓濞夋洟鎮￠幋鐐电瘈闁靛骏绲介悡鎰版煥濮橆剦鐔?SQL 闂傚倸鍊峰ù鍥х暦閸偅鍙忛柡澶嬪殮瑜版帒纾奸柣鎰絻閸嬪秹姊洪懖鈺婃敯闁糕晛娲︾粋宥堛亹閹烘挾鍘介梺褰掑亰閸撴稒绂掑☉銏＄厵妞ゆ牗锚閸旓箓鏌＄仦鍓р槈闁宠棄顦灒闁稿繐鍚嬮崰鏍ㄧ節濞堝灝娅欑紒鏌ョ畺瀹曟繂鈻庨幘瀹犳憰闂佺粯妫侀崑鎰板矗閺囥垺鐓欑紓浣姑粭鎺戔攽椤曞棛鐣垫慨?
        // 婵犵數濮烽弫鍛婃叏閻戝鈧倹绂掔€ｎ亞鍔﹀銈嗗坊閸嬫捇鏌涢悢閿嬪仴闁糕斁鍋撳銈嗗坊閸嬫挾绱撳鍜冭含妤犵偛鍟灒閻犲洩灏欑粣鐐烘煟鎼搭垳绉甸柛瀣椤㈡捇骞樼紒妯锋嫼缂傚倷鐒﹂敃鈺呮倿閼恒儯浜滈柨婵嗗閻瑩鏌涢埞鎯т壕婵＄偑鍊栫敮濠勨偓娑崇秮楠炲鎮╅悽鐢靛幇闂傚倷绶￠崜娆戠矓閹绢喗鍊挎繛宸簼閻撴洟鏌熼弶鍨倎缂併劏鍋愰幃?30缂?闂傚倸鍊搁崐鐑芥倿閿曞倹鍎戠憸鐗堝笒缁€澶屸偓鍏夊亾闁逞屽墴閸┾偓妞ゆ帊绀侀崵顒勬煕閿濆繒鍒伴柣锝囧厴楠炲鏁傞懞銉︾彸闂備焦鎮堕崕顖炲礉瀹€鍕剭妞ゆ劧闄勯埛鎴犵磽娴ｅ顏呮叏閸ヮ剚鐓熼幒鎶藉礉鎼淬劍鍎夋い蹇撶墱閺佸秹鏌ｉ幇顒夊殶闁告ɑ鎮傚铏圭矙閹稿孩鎷遍梺鑽ゅ枂閸庨亶鎮鹃悽绋垮耿婵炴垶鐟ч崢閬嶆⒑閹稿海绠撴繛灞傚€濆畷婵堚偓锝庡墰绾惧ジ鎮楅敐搴濇喚婵℃彃鎲￠妵鍕箻閻愯棄浠悗瑙勬礈閸犳牠銆佸鈧幃鈺呮濞戞艾鈧偤姊婚崒娆愮グ妞ゆ泦鍛板С闁兼祴鏅涚欢銈囩磽娴ｈ鐒介柣婵嗙埣閺屾盯顢曢悩鎻掑闂?SQL 闂傚倸鍊烽懗鍫曞磿閻㈢绀堟繝闈涚墛閺嗘粓鏌熼悜姗嗘當缂佺姷濞€閺岀喖骞戦幇顒傚帿閻?4MB闂傚倸鍊搁崐鐑芥倿閿旈敮鍋撶粭娑樻噽閻瑩鏌熸潏楣冩闁搞倖鍔栭妵鍕冀閵娧呯窗婵炲瓨绮岀紞濠囧蓟濞戞ǚ鏋庨煫鍥风稻妤旀俊鐐€愰弲婵嬪礂濮椻偓瀵寮撮悢铏瑰骄濡炪倖鐗楅懝楣冾敂椤愶附鐓曢柟瀛樼矌閻瑦鎱ㄦ繝鍛仩闁瑰弶鎸冲畷鐔碱敂閸パ呭姷闂傚倷鑳堕…鍫⑩偓娑掓櫊楠炴劙骞庨挊澶岀暰?baseMapper
        if (updateMap.size() > 500) {
            // 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佸湱鍎ら崵锕€鈽夊Ο閿嬬€婚梺褰掑亰閸犳俺銇愭ィ鍐┾拺闁革富鍘奸崝瀣亜閵娿儲顥㈤柟顔惧仦缁绘繂顫濋鐘插箰闂備礁鎲″ú锕傚磻閸℃瑧涓嶉柟鎯板Г閸?(濠?00闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曚綅閸ヮ剦鏁冮柨鏇楀亾缂佲偓閸喓绡€闂傚牊渚楅悞鎯瑰鍛殌闂囧鏌ㄥ┑鍡樺櫤閻犳劏鍓濋妵鍕煛閸屾粌寮ㄩ梺鍝勬湰濞茬喎鐣烽悡搴樻斀闁糕檧鏅欑槐鎴炰繆閻愵亜鈧洜鎹㈠Δ浣典粓闁告縿鍎插畷?
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                baseMapper.updateStarCountBatch(batchMap);
            }
        } else {
            // 闂傚倸鍊搁崐宄懊归崶褜娴栭柕濞垮労濞撳鏌熼悜姗嗘當缁绢厸鍋撻梻浣筋潐閸庣厧螞閸曨垱鈷掓い鏍仦閻擄綁鐓崶銊р枔闁稿鎸搁埥澶婎潩椤掆偓閳锋洟姊婚崒娆掑厡闁硅櫕鎹囬、姘额敇閻旂寮块梺姹囧灮閺佺螞椤栨稏浜滈柡宥庡亜娴狅箓鏌ｉ幘瀵告噰闁哄瞼鍠栭、姗€鎮㈡搴ｆ噯闂備礁鎲″鍦垝瀹€鍕ㄢ偓锕傛嚄椤栵絾鞋闂備礁鎼幏瀣磻婵犲洨宓?
            baseMapper.updateStarCountBatch(updateMap);
        }
        updateMap.keySet().forEach(this::flashShopRedisCache);
        flushCache();
        return true;
    }

    /**
     * 闂傚倸鍊搁崐椋庣矆娴ｈ櫣绀婂┑鐘插亞閻掔晫鎲歌箛鏇燁潟闁绘劕顕弧鈧梺鎼炲劀閸ヮ煉绱┑鐘垫暩閸嬫稑螣婵犲啰顩叉繝濠傜墕绾偓闂佽鍎兼慨銈夊磹閸偆绠鹃柟瀵稿剱娴煎棝鏌熸潏楣冩闁稿鍊块弻宥堫檨闁告挾鍠庨～蹇撁洪鍕炊闂侀潧顦崕鏌ユ倵鐠囨祴鏀介柍钘夋娴滄繄绱掔拠鑼ⅵ闁绘侗鍠栬灃闁告侗鍠栨禒娲⒑閸撹尙鍘涢柛瀣閹灚瀵肩€涙ǚ鎷洪梺鍛婄☉閿曘儲寰勯崟顖涚厱闁挎繂楠稿▍宥団偓瑙勬处閸ㄨ泛鐣锋總绋课ㄩ柨鏃€鍎抽獮?
     *
     * @param updateMap 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴顭ㄩ崼婵堢崶闁硅偐琛ラ崹鎯р槈濡攱鏂€闁诲函缍嗘禍鐐侯敊閹寸姷纾藉ù锝嗗絻娴滃墽绱撴担绋跨骇闁烩晩鍨伴～蹇撁洪鍕獩婵犵數濮寸€氼噣鎮￠埀顒勬⒒娴ｈ銇熼柛妯挎濞嗐垹顫濈捄铏圭暰婵炶揪缍€椤宕曢悢鍏肩叆婵犻潧妫欓ˉ婊勩亜鎼淬垻娲存慨濠呮缁辨帒螣閾忛€涙闂佽棄鍟虫ご鍝ユ崲?
     * @return 闂傚倸鍊搁崐椋庣矆娴ｈ櫣绀婂┑鐘插亞閻掔晫鎲歌箛鏇燁潟闁绘劕顕弧鈧梺鎼炲劀閸ヮ煉绱┑鐘垫暩閸嬫稑螣婵犲啰顩叉繝濠傜墕绾偓闂佽鍎兼慨銈夊磹閸偆绠鹃柟瀵稿剱娴煎棝鏌熸潏楣冩闁稿鍊块弻宥堫檨闁告挻绋撳Σ鎰板箻鐠囪尙锛滃┑鐐叉閸ㄥ灚淇婃禒瀣拺闁革富鍙庨悞鐐亜椤撶姴鍘存?
     */
    /**
     * 闂傚倸鍊搁崐椋庣矆娴ｈ櫣绀婂┑鐘插亞閻掔晫鎲歌箛鏇燁潟闁绘劕顕弧鈧梺鎼炲劀閸ヮ煉绱┑鐘垫暩閸嬫稑螣婵犲啰顩叉繝濠傜墕绾偓闂佽鍎兼慨銈夊磹閸偆绠鹃柟瀵稿剱娴煎棝鏌熸潏楣冩闁稿鍊块弻宥堫檨闁告挾鍠庨～蹇撁洪鍕炊闂侀潧顦崕鏌ユ倵鐠囨祴鏀介柍钘夋娴滄繄绱掔拠鑼ⅵ闁绘侗鍠栬灃闁告侗鍠栨禒娲⒒閸屾氨澧涚紒瀣灴閹ɑ绻濋崘顏嗙槇闂侀潧楠忕徊鍓ф兜閻愵兙浜滈柨鏇炲€烽幉鐐亜閵忊剝绀冮柕鍫秮瀹曟﹢鍩￠崘銊ョ?
     *
     * @param updateMap 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴顭ㄩ崼婵堢崶闁硅偐琛ラ崹鎯р槈濡攱鏂€闁诲函缍嗘禍鐐侯敊閹寸姷纾藉ù锝嗗絻娴滃墽绱撴担绋跨骇闁烩晩鍨伴～蹇撁洪鍕獩婵犵數濮寸€氼噣鎮￠埀顒勬⒒娴ｈ銇熼柛鎾寸懇婵″爼骞栨担鍝ョ暰閻庡厜鍋撻柛鏇ㄥ亞閸婄偤姊洪棃娴ゆ盯鍩€椤掑嫬鍑犻柡宓偓閺€浠嬫煟濡偐甯涙繛鎳峰嫮绠鹃悹鍥囧懐鏆犻梺?
     * @return 闂傚倸鍊搁崐椋庣矆娴ｈ櫣绀婂┑鐘插亞閻掔晫鎲歌箛鏇燁潟闁绘劕顕弧鈧梺鎼炲劀閸ヮ煉绱┑鐘垫暩閸嬫稑螣婵犲啰顩叉繝濠傜墕绾偓闂佽鍎兼慨銈夊磹閸偆绠鹃柟瀵稿剱娴煎棝鏌熸潏楣冩闁稿鍊块弻宥堫檨闁告挻绋撳Σ鎰板箻鐠囪尙锛滃┑鐐叉閸ㄥ灚淇婃禒瀣拺闁革富鍙庨悞鐐亜椤撶姴鍘存?
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
                baseMapper.updateFansCountBatch(batchMap);
            }
        } else {
            baseMapper.updateFansCountBatch(updateMap);
        }
        updateMap.keySet().forEach(this::flashShopRedisCache);
        flushCache();
        return true;
    }
    /**
     * 闂傚倸鍊搁崐椋庣矆娴ｈ櫣绀婂┑鐘插亞閻掔晫鎲歌箛鏇燁潟闁绘劕顕弧鈧梺鎼炲劀閸ヮ煉绱┑鐘垫暩閸嬫稑螣婵犲啰顩叉繝濠傜墕绾偓闂佽鍎兼慨銈夊磹閸偆绠鹃柟瀵稿剱娴煎棝鏌熸潏楣冩闁稿鍊块弻宥堫檨闁告挾鍠栧濠氭偄閸忕厧浜楅柟鑹版彧缁查箖骞夐悡搴富闁靛牆鍟俊濂告煥閺囥劋閭柣娑卞枛铻栭柛娑卞枛娴犳椽姊洪崨濠勨槈闁挎洩绠撳畷锝夊幢濞戞瑢鎷洪梻渚囧亞閸嬫盯鎳熼娑欐珷闁圭虎鍠楅悡?
     *
     * @param updateMap 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ョ紓鍌氬€风拋鏌ュ磻閹剧繝绻嗘俊銈咃梗缁ㄨ偐绱掔紒妯兼创妤犵偛顑夐幃娆撳级閹寸媭鏆￠梻鍌氬€风粈浣规償濠婂懐绀婂ù锝呭閸ゆ洟鏌曟繛鐐珔妞ゎ偄鎳橀弻鏇㈠醇濠靛浂妫ら梺鍛娚戠敮鈥澄涢崨鎼晝闁靛繆鈧剚妲辨繝鐢靛仜瀵爼鏁冮姀銈呯畺濡わ絽鍟悞鑲┾偓骞垮劚濡盯宕㈤幖浣光拺闁告稑锕ユ径鍕煕濡厧甯堕柣鈽嗗弮濮?
     * @return 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢埛姘そ婵¤埖寰勭€ｎ亙妲愰梻渚€娼ц墝闁哄懏鐩幏鎴︽偄鐏忎焦鏂€闂佺粯蓱瑜板啴顢旈銈囨／闁诡垎浣镐划闂佸搫鐬奸崰鏍嵁閸℃凹妾ㄩ梺鎼炲€楅崰搴ㄦ箒?
     */
    @Override
    public Boolean updateSoldBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }
        // 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佸湱鍎ら崵锕€鈽夊Ο閿嬬€婚梺褰掑亰閸犳俺銇愭ィ鍐┾拺闁革富鍘奸崝瀣亜閵娿儲鍣界紒顔肩墦瀹曞崬鈽夊▎鎴濆箞闂備焦鏋奸弲娑㈠窗濮橆兘鏋旈柡鍐ㄥ€荤壕濂告煕濞嗗浚妲归柕鍥ㄧ箞閺岋紕浠︾拠鎻掝瀳闂佸疇妫勯ˇ顖濈亽闂佸吋绁撮弲娑欐叏鏉堛劎绡€鐎电増鐏氶崐鏇㈡嫊閸忕浜滈柡鍥ф濞村嫮绱為弽顓熺厪闊洤顑呴埀顒佹礋閿濈偤宕ㄧ€涙鍘藉┑鈽嗗灠閹碱偊寮抽柆宥嗙厓鐟滄粓宕滃☉銏犳瀬闁告縿鍎查～?SQL 闂傚倸鍊峰ù鍥х暦閸偅鍙忛柡澶嬪殮瑜版帒纾奸柣鎰絻閸嬪秹姊洪懖鈺婃敯闁糕晛娲︾粋宥堛亹閹烘挾鍘介梺褰掑亰閸撴盯骞楅悩瑁佺懓鈹冮崹顔瑰亾濠靛钃熸繛鎴欏灩鍞銈嗘瀹曠敻宕欓崷顓犵＝?
        if (updateMap.size() > 500) {
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                baseMapper.updateSoldBatch(batchMap);
            }
        } else {
            baseMapper.updateSoldBatch(updateMap);
        }
        // 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佽法鍠撴慨瀵哥磼閳哄懏鈷戞い鎺嗗亾缂佸鎸抽幏?Redis 闂傚倸鍊峰ù鍥х暦閸偅鍙忛柡澶嬪殮瑜版帒绀嬫い鏍ㄧ▓閹稿啴姊洪崨濠冨闁搞劍澹嗘竟鏇㈠锤濡ゅ啫褰勯梺鎼炲劘閸斿秶绮堥崼銏㈡／闁告瑣鍎抽惌娆撴煛瀹€鈧崰鏍嵁閸℃凹妲鹃梺鎸庣☉缁夊綊寮?
        updateMap.keySet().forEach(this::flashShopRedisCache);
        // 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佽法鍠撴慨瀵哥磼閳哄懏鈷戞い鎺嗗亾缂佸鎸抽幏鎴︽偄鐏忎焦鏂€闂佺粯锚瀵爼骞栭幇顔剧＜闁逞屽墴瀹曞崬螣閸︻厾鐣鹃梻渚€娼ч悧鍡涘箠鎼搭煈鏁傞柕澶嗘櫆閻撳啰鎲稿鍫濈婵炲棙鎸搁悿顕€鏌熷▓鍨灍妞ゎ偅娲熼弻鐔煎箲閹伴潧娈紓浣哄Ь瀹曠敻鍩€椤掑喚娼愭繛鍙夌墪鐓ら柨鏇炲€哥粻鐔封攽閸屾碍鍟為柍閿嬪灴閺岀喓绮欓幐搴㈠闯闂佸疇妫勯ˇ鐢稿蓟閿濆憘鏃€鎷呴悷鎵紦闁诲孩顔栭崰鎾诲礉閹寸偞鍙忛柍褜鍓熼弻鏇㈠醇濠靛浂妫ら梺鍛娚戠敮鈥澄涢崨鎼晝闁靛繆鈧剚妲遍梻浣告贡椤牓骞楀鍛潟闁绘劕顕悷褰掓煃瑜滈崜鐔奉嚕婵犳碍鏅搁柣妯垮皺閸婄偤姊洪崘鍙夋儓闁哥喍鍗抽幆渚€宕奸姀銏紳婵炶揪绲肩划娆撳传閸濆嫧鏀芥い鏍ㄧ懅閻瑥鈹戦敍鍕毈妤犵偛娲鍓佹崉椤垶鏁ら梻鍌欒兌缁垶宕濋弴銏″仱闁靛ň鏅涚壕鍧楁煏婢跺棙娅嗛柍閿嬪灴閺屾盯鏁傜拠鎻掔闂佸憡鏌ㄩ澶愬箖娴犲鏁嶆繛鎴烆焽椤戝倿姊洪崗鍏笺仢濞存粌鐖奸妴浣割潨閳ь剚鎱ㄩ埀顒勬煃閳轰礁鏆為柣?
        flushCache();
        return true;
    }

    /**
     * 闂傚倸鍊峰ù鍥敋瑜嶉～婵嬫晝閸岋妇绋忔繝銏ｆ硾閼活垶寮搁崼鈶╁亾楠炲灝鍔氶柟宄邦儏閵嗘帗绻濆顓犲幈闁诲繒鍋涙晶浠嬪Υ閹烘挶浜滈柍鍝勫暙閸樻挳鏌熼鑲╃Ш鐎规洖鐖兼俊鎼佸Ψ瑜忛妶閿嬩繆閻愵亜鈧倝宕戞笟鈧畷鎰攽鐎ｎ亣鎽曢梺缁樻濞咃絿绮婚弮鈧换娑㈠箣閻愬棙鍨圭划鍫濈暆閸曨剛鍘遍柣搴秵閸嬪懎鐣峰畝鈧埀顒侇問閸犳鍒婇悾宀€涓嶆繛鎴欏灩閸楁娊鏌ｉ幇顓у晱濞村吋鐗犲濠氬磼濞嗘垵濡介柣搴ｇ懗閸涱垳鐓撻梺鍦劋椤ㄥ棝宕电仦杞挎棃鏁愰崨顓熸缂備胶濮甸悧鐘诲蓟閻斿吋鍊锋い鎺嗗亾濠⒀屽灡缁绘盯姊婚弶鎴濈ギ闂佸搫鏈ú妯侯嚗閸曨垰閱囨繝闈涙琚橀梻鍌欑閹碱偄螞濞嗘挶鈧啯绻濋崒銈嗙稁濠电偛妯婃禍婵嬪磹闁垮浜滈柟鍝勭Ч濡惧嘲霉閻橆喖鐏叉慨濠呮閹即鍨鹃崗鍛棜闂傚倷绶氬褏鎹㈤崼銉ョ９闁哄稁鍘介崑鍌炴煕瀹€鈧崑鐔煎矗閹剧粯鐓曢柕澶涚到婵′粙鏌嶉柨瀣缂佺粯鐩畷鐓庘攽閸繄鍘介梻浣告惈閻ジ宕伴幘璇茬闁绘顕ч悘鎶芥煣韫囷絽浜炲ù婊呭仧缁?Redis 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佺粯鍔曢悺銊モ枍閻樺厖绻嗛柕鍫濇噺閸ｈ鎱ㄧ憴鍕弨闁哄矉缍佸顕€宕掑顑跨帛缂傚倷鑳舵慨鏉戭嚕閸洖桅闁告洦鍨扮猾宥夋煕閵夋垵鍊归惈蹇旂節?
     */
    @Override
    public void syncSalesData() {
        log.info("starting shop sales data sync");
        String countKeyPrefix = SalesTypeEnum.SHOP_SALES.getCountKeyPrefix();
        String dirtyKey = SalesTypeEnum.SHOP_SALES.getDirtyKey();
        String tempKey = dirtyKey + ":TEMP";

        // 闂傚倸鍊搁崐椋庣矆娴ｉ潻鑰块梺顒€绉甸崑锟犳煙閹増顥夋鐐灲閺屽秹宕崟顐熷亾瑜版帒绾ч柟闂寸劍閳锋帒霉閿濆懏鍟為柛鐔哄仱閺屻倛銇愰幒鏃傛毇閻庢鍠氶弫濠氥€佸Δ鍛闁割煈鍋呭▍宀勬⒒娴ｈ櫣甯涢柛鏃€顨婂畷鏇㈠Χ婢跺鈧灝鈹戦悩宕囶暡闁绘挻鐩弻娑㈠即閵娿儰绨界紓浣哄У婵炲﹪骞冩禒瀣垫晬婵﹩鍙庡Λ锕傛⒑閸濆嫯顫﹂柛鏂跨焸閸┿儲寰勬繛銏㈠枛瀹曞綊顢氶崨顒傜＞闂傚倸鍊峰鎺旀椤斿墽绀婇柛鈩冪☉缁€鍫ユ煟閺冨洤浜圭€规挷绶氶幃妤呮晲鎼粹剝鐏堢紓浣哄У缁嬫帡濡甸崟顖氱鐎广儱娴傚Σ顕€姊洪柅鐐茶嫰婢т即鏌℃担鍓茬吋鐎殿喖顭烽弫鎾绘偐閼碱剙鈧偤鎮峰鍐妤犵偞顨婇幃鈺冪磼濡厧骞愰梺璇茬箳閸嬬偤寮告繝姘卞彆妞ゆ帊绶″▓浠嬫煟閹邦厽缍戞繛鎼枟椤ㄣ儵鎮欓弶鎴炶癁閻庢鍠楅幐铏繆閹间焦鏅滈柤鎭掑劜缁额偅绻濋悽闈浶ｆい鏃€鐗犲畷鏉课旈崘鈺傛濠德板€曢崯顐﹀垂濠靛牃鍋撻獮鍨姎闁瑰嘲顑夐幃鐐寸鐎ｎ偆鍘卞銈嗗姉婵挳宕濆鍫熺厽闁规儳鍟块埀顒€顭烽崺鈧い鎺嗗亾缂佺姴绉瑰畷鏇㈡焼瀹撱儱娲獮蹇撶暆婵犲啯绶梻?
        redisService.syncDataWithSnapshot("shop sales sync", countKeyPrefix, dirtyKey, tempKey,
                this::updateSoldBatch,
                updateMap -> enqueueIds(RedisConstants.SHOP_CALC_QUEUE_KEY, updateMap.keySet()));
        log.info("shop sales data sync finished");
    }


    /**
     * 闂傚倸鍊搁崐宄懊归崶顒婄稏濠㈣泛顑囬々鎻捗归悩宸剰缂佲偓婢舵劖鐓欓弶鍫濆⒔閻ｉ亶鏌?ID 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹濠德板€撻懗鍫曟偟閸洘鐓曢柍鈺佸暟閳洟鏌涚€Ｑ勬珔闁宠鍨块幃鈺冣偓鍦Т椤ユ繄绱撴担鎻掍壕闂佺硶鍓濈粙鎺楁偂閺囥垺鐓冮柍杞扮閺嗙喖鏌嶉娑欑闁诡噮鍠栭埞鎴犫偓锝庡亐閹锋椽姊洪崨濠勨槈闁挎洏鍊楃划顓烆潩椤撴粈绨婚梺闈涱煭缁犳垶鎱ㄦ径瀣╃箚闁告瑥顦慨鍥煃鐟欏嫬鐏╅柍褜鍓ㄧ紞鍡涘磻閸曨剛顩锋繛宸簼閳锋垿姊婚崼鐔剁繁闁绘帡绠栭弻娑欑節閸愮偓鐤侀梺纭呮珪缁诲啴濡堕敐澶婄闁冲搫鍟獮鎰版⒒娴ｈ鍋犻柛搴㈢矒瀹曘劑顢欓挊澶嬬亪闂傚倸鍊风粈渚€骞栭锔藉殣妞ゆ牜鍋為弲婵囥亜韫囨挾澧曠紒鐘崇墬缁绘繃绻濋崒婊冣拡闂佽桨绀佸ú顓㈠蓟閺囷紕鐤€濠电姴鍠氬鎰磽娴ｄ粙鍝虹紒璇茬墦瀵鎮㈤崗鐓庝簵闁硅壈鎻徊楣冨箟閻撳寒娓婚柕鍫濆暙婵″ジ鏌ㄩ弴銊ら偗闁绘侗鍠栬灃闁告侗鍠栨禒铏圭磽娴ｅ壊鍎撴繛澶嬫礋椤㈡瑦绻濋崶銊㈡嫼闂侀潻瀵岄崢濂稿礉鐎ｎ偆绠鹃悹鍥囧懐鏆ら悗娈垮枔閸旀垿骞婇悙鍝勎ㄩ柨鏇楀亾鐎?
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

    @Override
    public Boolean updateReviewCountBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }

        // 闂傚倷娴囬褍霉閻戣棄纾婚柨婵嗩槸绾捐绻濋棃娑氬ⅱ缁炬崘濮ら幈銊ヮ潨閳ь剟路閸岀偛鏋佸┑鐘叉处閻撴洘绻涢幋鐑嗙劷闁圭晫濞€閺岋繝宕担绋款潽闂侀€涚┒閸斿矂锝炲鍫濆耿婵°倐鍋撻柍閿嬪笒閳规垿鎮欓懠顒佸嬀闂佺锕ョ换鍫濐嚕婵犳碍鏅柛鏇樺妼娴滈箖鏌ㄥ┑鍡涱€楀ù婊呭仱閺岋綁顢橀悜鍡楀壎闂佸搫鐭夌换婵嗙暦閸楃偐鏋庨煫鍥ㄦ⒒娴滎亪姊绘担鍛婃儓闁兼椿鍨堕幆灞炬媴閾忛€涚瑝濠电偞鍨崹褰掑础閹惰姤鐓熼柟瀛樼箖椤ョ偞銇勯妷锔剧煉婵﹦绮幏鍛村川婵犲倹娈橀梺姹囧焺閸亪寮查悩鑼殾闁硅揪绠戠粈瀣亜閺嶎煈鍤?闂傚倸鍊烽懗鍫曞磿閻㈢绀堟繝闈涚墛閺嗘粓鏌熼悜姗嗘當缂佺姷濞€閺岀喖骞戦幇顒傚帿閻?00)闂傚倸鍊搁崐鐑芥倿閿旈敮鍋撶粭娑樻噽閻瑩鏌熸潏楣冩闁搞倖鍔栭妵鍕冀閵娧呯厒缂佹儳褰炵划娆撳蓟閻旂厧绠查柟閭﹀墰閸橆偅绻濆▓鍨仩闁告艾顑夋俊鐢稿礋椤栨氨鐫勯梺绋挎湰閼圭偓绂掑鈧铏规嫚閺屻儺鈧銇勯鐘插幋鐎殿喖顭烽幃銏ゅ礂閻撳簼缂撻梻浣稿閻撳牓宕戦幒鎾额浄鐟滃繒妲愰幘瀛樺濞寸姴顑呴幗鐢电磽娴ｇ瓔鍤欓柣妤€妫濋敐鐐剁疀閺冨倿妾紓浣割儓濞夋洟鎮￠幋鐐电瘈闁靛骏绲介悡鎰版煥濮橆剦鐔?SQL 闂傚倸鍊峰ù鍥х暦閸偅鍙忛柡澶嬪殮瑜版帒纾奸柣鎰絻閸嬪秹姊洪懖鈺婃敯闁糕晛娲︾粋宥堛亹閹烘挾鍘介梺褰掑亰閸撴稒绂掑☉銏＄厵妞ゆ牗锚閸旓箓鏌＄仦鍓р槈闁宠棄顦灒闁稿繐鍚嬮崰鏍ㄧ節濞堝灝娅欑紒鏌ョ畺瀹曟繂鈻庨幘瀹犳憰闂佺粯妫侀崑鎰板矗閺囥垺鐓欑紓浣姑粭鎺戔攽椤曞棛鐣垫慨?
        // 婵犵數濮烽弫鍛婃叏閻戝鈧倹绂掔€ｎ亞鍔﹀銈嗗坊閸嬫捇鏌涢悢閿嬪仴闁糕斁鍋撳銈嗗坊閸嬫挾绱撳鍜冭含妤犵偛鍟灒閻犲洩灏欑粣鐐烘煟鎼搭垳绉甸柛瀣椤㈡捇骞樼紒妯锋嫼缂傚倷鐒﹂敃鈺呮倿閼恒儯浜滈柨婵嗗閻瑩鏌涢埞鎯т壕婵＄偑鍊栫敮濠勨偓娑崇秮楠炲鎮╅悽鐢靛幇闂傚倷绶￠崜娆戠矓閹绢喗鍊挎繛宸簼閻撴洟鏌熼弶鍨倎缂併劏鍋愰幃?30缂?闂傚倸鍊搁崐鐑芥倿閿曞倹鍎戠憸鐗堝笒缁€澶屸偓鍏夊亾闁逞屽墴閸┾偓妞ゆ帊绀侀崵顒勬煕閿濆繒鍒伴柣锝囧厴楠炲鏁傞懞銉︾彸闂備焦鎮堕崕顖炲礉瀹€鍕剭妞ゆ劧闄勯埛鎴犵磽娴ｅ顏呮叏閸ヮ剚鐓熼幒鎶藉礉鎼淬劍鍎夋い蹇撶墱閺佸秹鏌ｉ幇顒夊殶闁告ɑ鎮傚铏圭矙閹稿孩鎷遍梺鑽ゅ枂閸庨亶鎮鹃悽绋垮耿婵炴垶鐟ч崢閬嶆⒑閹稿海绠撴繛灞傚€濆畷婵堚偓锝庡墰绾惧ジ鎮楅敐搴濇喚婵℃彃鎲￠妵鍕箻閻愯棄浠悗瑙勬礈閸犳牠銆佸鈧幃鈺呮濞戞艾鈧偤姊婚崒娆愮グ妞ゆ泦鍛板С闁兼祴鏅涚欢銈囩磽娴ｈ鐒介柣婵嗙埣閺屾盯顢曢悩鎻掑闂?SQL 闂傚倸鍊烽懗鍫曞磿閻㈢绀堟繝闈涚墛閺嗘粓鏌熼悜姗嗘當缂佺姷濞€閺岀喖骞戦幇顒傚帿閻?4MB闂傚倸鍊搁崐鐑芥倿閿旈敮鍋撶粭娑樻噽閻瑩鏌熸潏楣冩闁搞倖鍔栭妵鍕冀閵娧呯窗婵炲瓨绮岀紞濠囧蓟濞戞ǚ鏋庨煫鍥风稻妤旀俊鐐€愰弲婵嬪礂濮椻偓瀵寮撮悢铏瑰骄濡炪倖鐗楅懝楣冾敂椤愶附鐓曢柟瀛樼矌閻瑦鎱ㄦ繝鍛仩闁瑰弶鎸冲畷鐔碱敂閸パ呭姷闂傚倷鑳堕…鍫⑩偓娑掓櫊楠炴劙骞庨挊澶岀暰?baseMapper
        if (updateMap.size() > 500) {
            // 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁嶉崟顒佹闂佸湱鍎ら崵锕€鈽夊Ο閿嬬€婚梺褰掑亰閸犳俺銇愭ィ鍐┾拺闁革富鍘奸崝瀣亜閵娿儲顥㈤柟顔惧仦缁绘繂顫濋鐘插箰闂備礁鎲″ú锕傚磻閸℃瑧涓嶉柟鎯板Г閸?(濠?00闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曚綅閸ヮ剦鏁冮柨鏇楀亾缂佲偓閸喓绡€闂傚牊渚楅悞鎯瑰鍛殌闂囧鏌ㄥ┑鍡樺櫤閻犳劏鍓濋妵鍕煛閸屾粌寮ㄩ梺鍝勬湰濞茬喎鐣烽悡搴樻斀闁糕檧鏅欑槐鎴炰繆閻愵亜鈧洜鎹㈠Δ浣典粓闁告縿鍎插畷?
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                baseMapper.updateReviewCountBatch(batchMap);
            }
        } else {
            // 闂傚倸鍊搁崐宄懊归崶褜娴栭柕濞垮労濞撳鏌熼悜姗嗘當缁绢厸鍋撻梻浣筋潐閸庣厧螞閸曨垱鈷掓い鏍仦閻擄綁鐓崶銊р枔闁稿鎸搁埥澶婎潩椤掆偓閳锋洟姊婚崒娆掑厡闁硅櫕鎹囬、姘额敇閻旂寮块梺姹囧灮閺佺螞椤栨稏浜滈柡宥庡亜娴狅箓鏌ｉ幘瀵告噰闁哄瞼鍠栭、姗€鎮㈡搴ｆ噯闂備礁鎲″鍦垝瀹€鍕ㄢ偓锕傛嚄椤栵絾鞋闂備礁鎼幏瀣磻婵犲洨宓?
            baseMapper.updateReviewCountBatch(updateMap);
        }
        updateMap.keySet().forEach(this::flashShopRedisCache);
        flushCache();
        return true;
    }

    /**
     * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢埛姘そ婵¤埖寰勭€ｎ亙妲愰梻渚€娼ц墝闁哄懏鐩幏鎴︽偄鐏忎焦鏂€闂佺粯蓱瑜板啴寮抽悙鐑樼厱閹艰揪绲介弸鎴澢庨崶褝韬┑鈥崇埣瀹曘劑顢欓崗纰变画闂傚倷鑳堕、濠傗枍閺囥垺鍋￠柍鍝勬噹閽冪喖鏌ㄥ┑鍡樺晵闁绘梻鍘ч崹鍌涖亜閺冣偓閸庡磭鏁崸妤佲拻?
     *
     * @param id     闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ョ紓鍌氬€风拋鏌ュ磻閹剧繝绻?
     * @param status 闂傚倸鍊搁崐鐑芥嚄閸撲礁鍨濇い鏍亹閳ь剨绠撳畷濂稿Ψ閵夛附袣闂備礁鎼粙渚€宕㈡總鍛婂€?
     * @return 缂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸婂潡鏌ㄩ弴鐐测偓鍝ョ不閺夊簱鏀介柣妯虹－椤ｆ煡鏌?
     */
    @Override
    public Boolean updateShopStatus(Long id, Integer status, String reason) {
        Shop shop = getById(id);
        // 闂傚倸鍊搁崐椋庣矆娴ｈ櫣绀婂┑鐘叉搐缂佲晠寮堕崼姘珕闁哄棙绮撻弻锝夊箛閻楀牊閿梺鎼炲妽缁诲牓寮婚敐澶婄闁挎繂妫Λ鍕磽娓氬洤浜滅紒澶婄秺楠炲啳銇愰幒鎴滅炊闂佸憡娲﹂崜姘跺磿閹剧粯鈷戦柛婵嗗閸ｆ椽鏌熼鐓庘偓鍨嚕鐠囨祴妲堟繝濠傛噽閺夋悂鏌ｆ惔顖滅У濞存粍绮嶉幈銊╁即閵忥紕鍘介柟鍏肩暘閸ㄥ吋绔熷Ο璁崇箚妞ゆ劧绲块妴鎺撲繆閸欏濮嶆鐐村浮楠炲寮埀顒勫棘閳ь剟姊绘担铏广€婇柛鎾寸箞閵嗗啴宕卞☉妯哄殤婵°倧绲介崯顖炲煕閹达附鐓欓柤娴嬫櫅娴犳粓鏌涢弬璇测偓鏇㈠煘閹达富鏁嶆慨妯煎帶濞堣泛鈹戦垾鍐茬骇闁告梹娲熼崺鐐哄箣閻橆偄浜鹃柨婵嗛娴滅偤鏌涢弮鍫熸锭闁宠鍨块、娆撴儗椤愵偂绨藉瑙勬礋椤㈡﹢鎮╅崗鍝ョ憹闂備礁鎼粙渚€宕㈡禒瀣亗婵炴垯鍨洪悡鏇㈡煙娴煎瓨娑ф鐐寸墵閺岋綁骞橀弶鎴濐潕缂備浇椴哥敮鈥愁嚕椤曗偓瀹曟儼顦查柟顖滃仧缁辨挻绗熼崶褎鐏嶉悗鍏夊亾闁归棿绀佺粻鏍ㄧ節婵犲倸鎮╅柣鏂垮悑閹偤鏌ｉ悢绋款棆缂佸宕电槐鎾诲磼濮橆兘鍋撻幖浣哥９闁绘垼濮ら崐鍧楁煥閺冨倸浜剧€规洘鐓￠弻娑氫沪閸撗呯厑闂佸搫妫欑划宥夊Φ閸曨垰绠涢柛鎾茶兌閺嗙娀姊?
        String rejectReason = AuditStatusEnum.isRejected(status) ? reason : null;
        Integer businessStatus = null;
        // 闂傚倸鍊风粈浣革耿闁秴鍌ㄧ憸鏃堝箖濞差亜惟闁崇懓绨遍崑鎾诲礃閳哄啰鐦堥梺鎼炲劀閸滀礁鏅ラ梻鍌欑婢瑰﹪鎮￠崼銉ョ；闁糕剝绋戦悡婵嬫煛閸愶絽浜鹃梺閫涚┒閸斿矂锝炲鍫濆耿婵°倐鍋撴い顐熸櫅椤啴濡舵惔鈥茬按闂佺锕ョ换鍫濐嚕婵犳碍鏅查柛娑樺€婚崰鏍х暦瑜版帩鏁婇柟顖嗗啰绱伴梻鍌氬€风欢姘跺焵椤掑倸浠滈柤娲诲灦瀹曘垽宕妷褏锛滈梻濠庡墯瑜板啴鍩€椤掍緡娈旈崡?=闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鑼槱閻熸粎澧楃敮鎺楀垂閸岀偞鐓欓柟顖滃椤ュ鏌＄€ｂ晝绐旈柡灞炬礋瀹曠厧鈹戦幇顓壯囨⒑?=闂傚倸鍊搁崐鐑芥嚄閸洍鈧箓宕奸妷顔芥櫈闂佺硶鍓濋悷銉╁垂濠靛绠规繛锝庡墮婵″ジ鏌?
        if (Objects.equals(status, AuditStatusEnum.PASS.getCode())) {
            businessStatus = 1;
        } else if (AuditStatusEnum.isRejected(status)) {
            businessStatus = 2;
        }
        UpdateWrapper<Shop> uw = new UpdateWrapper<Shop>()
                .set("audit_status", status)
                .set("reject_reason", rejectReason)
                .eq("id", id);
        if (businessStatus != null) {
            uw.set("status", businessStatus);
        }
        boolean updated = update(uw);
        if (updated) {
            flashShopRedisCache(id);
            if (shop != null && shop.getTypeId() != null) {
                flashShopListRedisCache(shop.getTypeId());
            }
            if (Objects.equals(status, AuditStatusEnum.PASS.getCode())) {
                publish(new String[]{id.toString()});
                String hotRankKey = RedisConstants.SHOP_HOT_RANK_KEY;
                double score = shop != null && shop.getCreateTime() != null ? (double) shop.getCreateTime().getTime() : (double) System.currentTimeMillis();
                redisService.setCacheZSet(hotRankKey, id.toString(), score);
                redisService.setCacheSet(RedisConstants.SHOP_CALC_QUEUE_KEY, id.toString());
            } else {
                redisService.removeCacheZSetObject(RedisConstants.SHOP_HOT_RANK_KEY, id.toString());
                redisService.removeCacheSet(RedisConstants.SHOP_CALC_QUEUE_KEY, id.toString());
                ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                contentSyncMessage.setId(id);
                contentSyncMessage.setIndexName(EsIndexNameConstants.SHOP_INDEX_NAME);
                contentSyncMessage.setType(GlobalBizTypeEnum.SHOP.getCode());
                mqMessageSendUtils.sendMqMessage(SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                mqMessageSendUtils.sendMqMessage(SearchMqConstants.MILVUS_SYNC_EXCHANGE, SearchMqConstants.MILVUS_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
            }
        }
        return updated;
    }

    /**
     * 闂傚倸鍊搁崐椋庣矆娓氣偓瀹曘儳鈧綆鍠栫壕鍧楁煙閹増顥夐幖鏉戯躬閺屻倝鎳濋幍顔肩墯婵炲瓨绮岀紞濠囧蓟濞戙垹唯妞ゆ梻鍘ч～顏堟⒑缁嬪尅鍔熺紒顕呭灦婵＄敻宕熼姘鳖啋闂佸憡顨堥崑鐔哥妤ｅ啯鈷戦柟绋挎捣閳藉鏌ｉ鐐测偓鍧楁偘椤曗偓瀵粙顢曢悢铚傚濠电偞鍨堕悷杈ㄧ瑹濞戞瑧绠鹃柛顐ゅ枔閻帡鏌″畝鈧崰鏍箖閸撗傛勃闁绘劦鍓氶惁鎾翠繆閵堝洤啸闁稿鍋熼弫顕€鏁撻悩闈涚ウ闂佸湱鍎ら〃鍛村础閹惰姤鐓忓┑鐘茬箻濡绢噣鏌℃径濠冨暈缂佺粯绻傞埢鎾诲垂椤旂晫浜堕梺姹囧焺閸ㄩ亶銆冮崼銉ョ闁圭儤顨嗛弲鏌ユ煕閵夘喚鍘涙繛鑲╁枎閳规垿鎮欓崣澶樻！闂佸憡姊瑰ú婊冣枎閵忋倖鍊烽柛顭戝亜閺嬫垿鎮楅崗澶婁壕闂侀€炲苯澧伴柛鎺撳浮閸╋繝宕橀敐鍛闂傚倸鐗婄粙鎺楀箟閸濄儳妫柟顖嗕礁浠梺鍝勭焿缂嶄礁顕ｉ鍕瀭妞ゆ柨褰ㄩ埡鍛拺閻犲洤寮堕崬澶嬨亜閺囧棗娲犻埀顒佹瀹曟﹢顢欓崲澹洦鐓曟繛鎴濆船閻忕喓绱掗鐐毈闁哄矉缍佹慨鈧柍杞拌兌娴狀參姊洪崷顓х劸闁哥喎娼￠幃楣冩倻閼恒儲娅滄繝銏ｆ硾椤戝棗鈻嶅鍫燁棅妞ゆ劑鍨烘径鍕箾閸欏鐭掗挊?
     * 婵犵數濮烽。钘壩ｉ崨鏉戠；闁规崘娉涚欢銈呂旈敐鍛殲闁稿顑夐弻锝呂熷▎鎯ф閺夆晜绻冪换婵嬪閿濆棛銆愰梺鍝勭墱閸撴盯鍩€椤掍礁鍤柛鎾跺枛瀵鈽夐姀鐘电杸闂佺绻愰幗婊堝礄瑜版帗鈷戦柛婵嗗椤ユ粎绱掔紒姗堣€跨€殿喖顭锋俊鎼佸Ψ閵忊槅娼旀繝鐢靛仜濡瑩宕硅ぐ鎺戠煑婵犻潧鐗忕壕?current=1, size=10
     * 濠电姷鏁告慨鐢割敊閺嶎厼绐楁俊銈呭暞閺嗘粍淇婇妶鍛殭闁搞劍绻堥幃姗€鎮欓崹顐ｇ彧闂佸搫妫寸粻鎾诲蓟閿濆绫嶉柍褜鍓熼獮鍐嚋閻㈡娲稿┑鐘诧工閸樻崘銇愰幒鎾存珳闂佸憡渚楅崢钘夆枔鐠鸿　鏀介柣鎰皺婢ф洟鏌ｉ弽褋鍋㈢€殿喖顭烽弫鎰緞婵犲孩缍傞梻浣稿暱閹碱偊宕銈嗘殰闁靛ě鍛紳婵炶揪绲鹃崹婵嬪触椤愩埄鐔嗙憸搴ㄣ€冮崼銉ョ劦?current=n, size=10
     *
     * @param current 婵犵數濮烽。顔炬閺囥垹纾婚柟杈剧畱绾捐淇婇妶鍛櫣闁哄绶氶弻鐔兼⒒鐎电濡介梺?
     * @param size    濠电姷鏁告慨鐢割敊閺嶎厼闂い鏍ㄧ矊缁躲倝鏌ｉ敐鍛拱鐎规洖寮堕幈銊ヮ渻鐠囪弓澹曞┑鐘殿暯閳ь剙纾幗鐘碘偓鍨緲鐎氫即鐛崶顒夋晢濞达綀銆€閸嬫捇顢楁担铏诡啎闂佸搫顦伴崹鐢稿箖閹寸偟绠鹃柛蹇氬亹閹冲啴鏌?
     * @param x       闂傚倸鍊搁崐鐑芥倿閿曗偓椤啴宕归鍛姺闂佺鍕垫當缂佲偓婢跺备鍋撻獮鍨姎妞わ富鍨跺浼村Ψ閿斿墽顔曢梺鐟邦嚟閸嬬偤鎯冮幋鐘垫／闁诡垎浣镐划闂佺粯鎼╅崑濠傜暦閸洖惟闁挎洍鍋撶痪鏉跨Т椤?
     * @param y       闂傚倸鍊搁崐鐑芥倿閿曗偓椤啴宕归鍛姺闂佺鍕垫當缂佲偓婢跺备鍋撻獮鍨姎妞わ富鍨跺浼村Ψ閿斿墽顔曢梺鐟邦嚟閸嬬偤鎯冮幋鐐扮箚妞ゆ劦鍋勯悘锟犳煏閸パ冾伃妤犵偞甯″畷鍗烆渻閹屾闂?
     * @return 闂傚倸鍊搁崐鐑芥嚄閸撲礁鍨濇い鏍仜閺勩儵鏌涢鐘插姎闁活厽顨婇弻娑㈠箛閳轰礁顥嬪┑鐐村灟閸╁嫰寮崱妞曞綊鏁愰崶銊ユ畬闂佸憡鏌ㄩ幊搴ㄥ煘閹寸偛绠犻梺绋匡攻濞茬喖鐛繝鍛杸婵炴垶顭囬悿鍥р攽閻愬弶顥為柟绋款煼閹繝寮撮姀锛勫幍闂佺粯鍨惰摫闁诡垰瀚换娑㈠醇閻旂儤鍣伴梺?
     */
    @Override
    public List<ShopVO> getHotShopRank(Integer current, Integer size, Double x, Double y) {
        int pageNo = current == null || current < 1 ? 1 : current;
        int pageSize = size == null || size < 1 ? 10 : size;

        // 1. 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸閻ゎ喗銇勯弽銊х細濞存粌缍婇弻娑㈠箛閸忓摜鑳洪梺缁樺笒閿曘倝婀侀梺缁樏Ο濠囧磿韫囨洜纾奸柣妯哄暱閻忊晠鏌熼幓鎺撳暈闁诡垱妫冮弫鎰板磼濮橆偄顥氭繝娈垮枟鑿ч柛搴ㄤ憾閸┾偓妞ゆ帊鑳剁粻鐐烘煟濞戝崬鏋ら柍褜鍓ㄧ紞鍡涘闯椤曗偓瀵偊宕掗悙鑼啇闁哄鐗嗘晶浠嬪箖婵傚憡鐓熸繛鎴炵墪閸旓附鎱ㄦ繝鍐┿仢鐎规洦鍋婂畷鐔碱敇閻戝棙娈奸梻鍌欒兌椤牓顢栭崱娑樼闁归棿璁查埀顒婄畵瀹曠螖娴ｅ憡鐤傚┑鐐舵彧缁蹭粙宕位澶娾攽鐎ｎ偄浠┑鐘诧工鐎氼剚鍎梻浣告啞娓氭宕归幎鑺ユ櫖婵犲﹤鍟犻弨浠嬫煟閹邦剙绾фい銉у仱閺屾盯濡搁妷銉殺缂備緡鍠涢褔鍩㈡惔銊ョ閻庣數顭堥獮?100 闂?
        if (pageNo * pageSize > 100) {
            return Collections.emptyList();
        }

        List<ShopVO> resultList;

        // 婵犵數濮烽弫鎼佸磻濞戙埄鏁嬫い鎾跺枑閸欏繘鎮楅悽鐢点€婇柛瀣尭閳藉骞掗幘瀵稿絽闂備線娼уΛ娆戞暜閳ュ磭鏆﹀┑鍌滎焾閸楁娊鏌ｉ幇顓熷剹婵☆偆鍠栧缁樻媴缁涘缍堝銈嗘⒐閻楃姴鐣烽弶璇炬棃宕ㄩ闂存偅?ZSet 闂傚倸鍊搁崐鐑芥嚄閸撲礁鍨濇い鏍仜閺勩儵鏌涢鐘插姎闁稿被鍔戦弻銈吤圭€ｎ偅婢掗梺绋款儐閹搁箖骞夐幘顔肩妞ゆ劗鍠愰悘鍡樹繆閵堝洤啸闁稿鍋熼弫顕€鏁撻悩闈涚ウ闂佸湱鍎ら〃鍛村础閹惰姤鐓忓┑鐘茬箻濡绢噣鏌℃径濠冨暈缂佺粯绻傞埢鎾诲垂椤旂晫浜堕梺姹囧焺閸ㄩ亶銆冮崼銉ョ闁圭儤顨呴柋鍥煏婢舵稓鐣辨繛鍫燂耿濮婅櫣娑甸崨顔兼锭闂傚倸瀚€氭澘鐣烽悽绋跨睄闁稿本顨呮禍楣冩煟閻斿憡绶叉い蹇ｄ簼缁绘繈濮€閳藉棛鍔烽悗鍨緲閿曨亜鐣风粙璇炬梹鎷呴崫鍕疄濠电姴鐥夐弶搴撳亾閹捐秮褰掑炊閵娧呭骄濠碘槅鍨遍惇瑙勭?ID 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢敂缁樻櫈闂佸憡娲﹂崢楣冩儗閸℃ぜ鈧帒顫濋敐鍛闁诲氦顫夊ú鈺冨緤妤ｅ啫围闁挎繂顦粈鍐煏婵炑冩湰瀛濋梻鍌氬€烽懗鍓佹兜閸洖绀堟繝闈涚墢閻瑩鐓崶銊р槈闁绘帒鐏氶妵鍕箳閸℃ぞ澹曟俊鐐€х粻鎾愁焽瑜旈、姘舵晲婢舵ɑ鏅濋梺鎸庢濡嫭绂嶉柆宥嗏拺闁告挻褰冩禍婵堢磼鐠囨彃顏┑?
        Page<Long> longPage = zSetIdManager.pageIds(RedisConstants.SHOP_HOT_RANK_KEY, null, current, SystemConstants.MAX_PAGE_SIZE);
        List<Long> shopIdList = longPage.getRecords();

        if (CollUtil.isEmpty(shopIdList)&&current == 1) {
            // ZSet 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鎻掔€梺缁樻尭濞寸兘寮抽敃鍌涚厽闁靛繈鍩勯悞楣冩煟閹剧摲鎴犳崲濞戙垹绠ｆ繛鍡楃箳娴犺偐绱撴担鍝勵€撶紓宥勭窔瀵鍨惧畷鍥ㄦ濡炪倖鏌ㄩ崥瀣闯閻戞ǜ浜滈柡鍥╁櫐閼版寧鎱ㄦ繝鍐┿仢鐎规洦鍋婂畷鐔碱敃閿濆棭鍟€婵犵數濮甸鏍窗濮樿泛鏋侀悹鍥ф▕閸ゆ洟鏌熼幆鏉啃撻柛瀣閺岋綁骞橀搹顐ｅ闯濡炪倖鏌ㄩˇ顖炩€旈崘顔嘉ч柛鈩冿供濮婃寧绻濆▓鍨灁闁稿﹥绻嗗Λ鐔兼偡濠婂嫭顥堢€殿喖顭烽弫鎰緞婵炩懇鏅犻弻鏇熷緞閸繂濮舵繛瀵稿Т閵堢顫忓ú顏勫窛濠电姴娴烽崝绋款渻閵堝骸浜滄い锔炬暬閻涱噣宕卞☉妯肩潉闂佸壊鍋呯换鍌氱暤閸℃娓婚柕鍫濇噽缁犵増淇婇锝囨创闁诡喕鍗虫俊鐑藉煛閸屾瀚奸梻浣告啞缁诲倻鈧凹鍨堕敐鐐烘晝閸屾稓鍘遍柟鍏肩暘閸斿骞夐崜褉鍋撶憴鍕闁轰礁顭烽妴浣割潨閳ь剚鎱ㄩ埀顒勬煟濡椿鍟忔俊宸灦濮婄粯绗熼埀顒€顭囪婢ф繈姊洪崫鍕櫤缂佸鍨块崺銏狀吋婢跺á褍顭跨捄渚剳闁告ê鎲＄换娑欐綇閸撗冨煂闂佸湱鈷堥崑濠囧Υ閸岀偞鍤冮柍瑙勫劤娴滈箖鎮峰▎蹇擃仾閻忓浚鍋嗙槐鎺楁嚋娴ｅ啫顥濋梺浼欑到閸㈣尪鐏掗柣鐐寸▓閸撴繈鎮楁繝姘棅妞ゆ劑鍨烘径鍕煙閸濄儺鐒鹃弫?ZSet闂傚倸鍊搁崐鐑芥倿閿旈敮鍋撶粭娑樻噽閻瑩鏌熸潏楣冩闁搞倖鍔栭妵鍕冀椤愵澀绮堕梺姹囧€ら崳锝夊蓟瀹ュ牜妾ㄩ梺鍛婃尵閸犳牕顕ｆ繝姘亜闁稿繒鈷堝Λ鍐ㄢ攽閻愭潙鐏ョ€规洦鍓熷畷婊堟偋閸垻鐦堝┑鐐茬墕閻忔繈寮搁幘缁樼厸闁告侗鍨板瓭闂佷紮绲介崲鏌ワ綖濠靛牊宕夐柛婵嗗閳ь剙鐏濋埞鎴︽倷閺夋垹浠搁梺缁橆殕閸ㄥ灝鐣烽幋锕€绠婚柡澶嬪灩缁愮偤鏌ｆ惔顖滅У闁稿鍊搁～婵嬫晝閸屾稈鎷洪梺鍛婄箓鐎氼剟鍩€椤掆偓濠€閬嶅焵椤掑倻鎳楅柛娑卞灣閻掑ジ姊?
            log.info("shop hot rank zset is empty, loading shop ids from database");
            List<Shop> dbList = query()
                    .eq("status", 1)
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .orderByDesc("sold")
                    .orderByDesc("create_time")
                    .list();

            if (CollUtil.isNotEmpty(dbList)) {
                final List<Shop> finalDbList = dbList;
                executorService.execute(() -> {
                    log.info("闂傚倸鍊搁崐鎼佸磹閻戣姤鍊块柨鏇氶檷娴滃綊鏌涢幇鍏哥敖闁活厽鎹囬幃妤呮濞戞瑦鍠愮紒鎯у綖缁瑩寮婚悢鐓庣畾鐟滃繘骞楅悩缁樼厱閹艰揪绲介弸鎴澢庨崶褝韬┑鈥崇埣瀹曘劑顢欓崗纰变画闂傚倷鑳堕、濠傗枍閺囥垺鍋￠柍鍝勬噹閽冪喖鏌ㄥ┑鍡╂Ц缂佺媴绲鹃妵鍕箻鐠鸿桨绮剁紓浣哄Т閹碱偊鈥?ZSet");
                    zSetIdManager.saveToZSet(RedisConstants.SHOP_HOT_RANK_KEY, finalDbList, Shop::getId, Shop::getCreateTime);

                    // 闂傚倸鍊搁崐宄懊归崶顒婄稏濠㈣泛顑囬々鎻捗归悩宸剰缂佲偓婢跺备鍋撻崗澶婁壕闂佸憡娲﹂崑鍡涙偂閹达附鈷戠紒顖涙礀婢ф煡鏌ｉ悤鍌炴闁告帒锕ョ缓浠嬪川婵犲嫬寮虫繝鐢靛仦閸ㄥ爼鏁嬪銈呮禋閸嬪﹪寮婚敓鐘插耿闁宠桨绀侀埛瀣攽椤旂》宸ユい顓炲槻閻ｅ嘲顫滈埀顒勩€侀弮鍫濈妞ゆ帒鍊甸崑鎾寸節濮橆厸鎷洪梺鍛婃崄鐏忔瑩宕㈠☉妯忕懓顭ㄦ惔婵嬪仐閻庢鍣崑鍕敇婵傜宸濇い鏍ㄧ⊕閻ｇ兘姊绘笟鈧埀顒傚仜閼活垱鏅堕幘顔界厵妞ゆ棁濮ら妵婵囶殽閻愬樊鍎旈柡浣稿暣閸┾偓妞ゆ帒鍊搁ˉ姘舵偡濞嗗繐顏紒鐘荤畺閺屾盯鍩勯崘鐐暭缂備胶濮垫繛濠囧蓟閿熺姴骞㈤柡鍥╁仜缁侇噣姊洪崫鍕効缂佺粯绻傞悾宄邦潨閳ь剟宕洪敓鐘茬＜婵炴垶鍩冮崑鎾活敍閻愮补鎷洪梺鑽ゅ枑婢瑰棝鎮鹃銏＄厱闁靛鍔嶉ˉ澶愭煟閿濆懎妲婚柍瑙勫灩閳ь剨缍嗘禍鐐村鐎ｎ喗鈷戠憸鐗堝俯閺嗘帡鏌ｉ幒鐐电暢闁愁亝鎮傚濠氬磼濞嗘劗銈伴悗瑙勬礈閺佽鐣烽幋婵冩婵☆垱绮嶇粙鎴﹀煡婢跺娼ㄩ柛顐岛閸嬫捇宕稿Δ浣哄帗闂侀潧顧€缁犳垶鏅堕娑氱闂傚倹娼欏畵鍡涙煛鐏炶濡奸柍瑙勫灴瀹曞崬螣娓氼垱袩闂傚倷妞掔槐顔剧礊娴ｅ湱顩查柛顐ｆ礀缁犳牠鎮规ウ瑁も偓鈧俊鎻掔墛缁绘盯宕卞Ο鍝勵潕闂佸憡鍩婄换婵嗩潖濞差亝顥堥柍鍝勫暟钃辨繝纰夌磿閸嬬姴螞閸曨垱鍋╅柣鎴犵摂閺佸倿鏌涢弴銊ュ箺婵炲牊娲熷娲川婵犲倸袝婵炲瓨绮嶉悧鐘茬暦閹达箑绠婚悺鎺嶇劍濡炰粙銆侀弮鍫濈妞ゆ枮鍕婵﹥妞藉畷婊堝箵閹哄秶鎸夐梻浣规偠閸斿瞼绱炴繝鍌滄殾婵犻潧娲ら閬嶆煛婢跺鐏╂い鏃€鍔欓弻鈩冨緞婵犲嫬顣烘繝鈷€鍌滅煓闁诡噣绠栭弻鍡楊吋閸℃瑥骞?
                    redisService.setCacheSet(RedisConstants.SHOP_CALC_QUEUE_KEY,
                            finalDbList.stream()
                                    .map(s -> String.valueOf(s.getId()))
                                    .collect(Collectors.toSet()));
                });

                // 闂傚倸鍊搁崐椋庣矆娴ｈ櫣绀婂┑鐘插亞閻掔晫鎲搁弮鍫涒偓渚€寮介鐐茬獩濡炪倖妫佸Λ鍕嚕閸ф鈷戦柛鎰级閹牓鏌熼崘鍙夋崳缂侇喖鐗撳畷鍗炩槈濞嗘垵骞堥梻浣规灱閺呮盯宕导鏉戠厽闁靛牆顦伴悡鍐喐濠婂牆绀堟繛鍡樻嫴閸ヮ剚鍋ㄩ柛婵勫劚缁侊箓鏌ｆ惔顖滅У闁革綆鍣ｅ顐﹀磼閻愬鍘遍梺鍝勬储閸斿本绂嶉悙鐑樼厵妞ゆ梻顑曢崑銏ゆ煙椤旂厧妲绘い顓滃姂瀹曘劑顢樿濮ｅ姊绘担鍛婅础妞ゎ厼鐗撻獮澶愭晸閻樿尙鏌ч梺鍓插亝濞叉﹢宕戦幇鐗堢厽闁归偊鍘奸悘濠勭磼閵娿儺鐓兼慨濠冩そ瀹曟粓鎳犻鈧敮銉╂⒑缁嬫鍎忛柟鍐查叄閸┿垽骞樼拠鏌ュ敹闂佸搫娲ㄩ崰鎾诲储?
                int start = (pageNo - 1) * pageSize;
                if (dbList.size() > start) {
                    dbList = dbList.subList(start, Math.min(start + pageSize, dbList.size()));
                    shopIdList = dbList.stream().map(Shop::getId).collect(Collectors.toList());
                } else {
                    shopIdList = Collections.emptyList();
                }
            }
            resultList = CollUtil.isEmpty(shopIdList) ? Collections.emptyList() : getShopList(shopIdList);
        } else {
            // 4. 闂傚倸鍊搁崐椋庣矆娴ｈ櫣绀婂┑鐘插亞閻掔晫鎲歌箛鏇燁潟闁绘劕顕弧鈧梺鎼炲劀閸ヮ煉绱┑鐘垫暩閸嬫稑螣婵犲啰顩叉繝濠傜墛鐎氬懘鏌ｉ弬鍨倯闁绘挸绻愰…鍧楁嚋濞堣法鍔烽梺鍛娚戦惄顖炲蓟濞戙垹围闁告侗鍙庢导鍐ㄎ旈悩闈涗沪闁绘绮撻崺鈧い鎺嶈兌閳洟鏌ㄩ弴妤佹珔闁崇粯鎸搁…銊╁醇閻斿搫骞嶉梻浣风串缁蹭粙寮甸鍕辈闁冲搫鎳忛悡娆忣渻鐎ｎ亪顎楅柍璇茬墦閺屸剝鎷呯粙搴撳亾閸ф绠板┑鐘插暙缁剁偛鈹戦悩鎻掍簽婵☆偄鐗撳濠氬磼濞嗘垵濡介柣搴ｇ懗閸涱垳鐓撻梺纭呮彧闂勫嫰宕戠€ｎ喗鐓曢柍鈺佸暢濞夋煡鏌涢妷顔煎闁藉啰鍠栭弻鏇熷緞濞戞氨鏆犳繛瀵稿У濡炶棄顫忓ú顏咁棃婵炴番鍊栭惄顖氱暦閵娾晩鏁囩憸搴も吀闂傚倸鍊峰ù鍥敋瑜忛埀顒佺▓閺呮繄鍒掑▎鎾崇闁哄洨鍠愰崳閿嬬節閻㈤潧袨闁搞劌銈搁敐鐐村緞鐏炵偓娈伴梺璺ㄥ枔婵挳鎷戦悢琛″亾閸忓浜鹃梺鍛婂姦娴滅偤寮堕幖浣光拺闁告繂瀚弳娆撴煕婵犲嫷鐒炬い鏇秮婵℃悂鍩￠崒婊冨箺闂備胶绮敋鐎殿喖澧庣划濠囧煛閸屾粎鐦堝銈庡亽閸欌偓闁稿繐鏈穱?
            resultList = getShopList(shopIdList);
        }

        resultList = resultList.stream()
                .filter(this::isVisibleShop)
                .collect(Collectors.toList());

        // 闂傚倸鍊峰ù鍥х暦閸偅鍙忕€规洖娲﹂浠嬫煏閸繃澶勬い顐ｆ礋閺岋繝宕堕妷銉т痪闂佺顑傞弲鐘诲蓟閵堝棙鍙忛柟閭﹀厴閸嬫捇寮借閺嗭箑顭跨捄渚剳缂佲檧鍋撻梻鍌氬€搁悧濠勭矙閹惧瓨娅犻梺顒€绉甸悡?
        if (x != null && y != null && CollUtil.isNotEmpty(resultList)) {
            for (ShopVO shopVO : resultList) {
                if (shopVO.getX() != null && shopVO.getY() != null) {
                    double distance = SloppyMath.haversinMeters(y, x, shopVO.getY(), shopVO.getX());
                    shopVO.setDistance(distance);
                }
            }
        }
        return resultList;
    }

    /**
     * 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢妶鍥╃厠闂佺粯鍨堕弸鑽ょ礊閺嵮岀唵閻犺櫣灏ㄩ崝鐔兼煛閸℃劕鈧洟濡撮幒鎴僵闁挎繂鎳嶆竟鏇㈡⒒娴ｇ瓔鍤冮柛鐘虫礈閸掓帒鈻庨幇顏嗙畾闂佸綊妫块悞锕傚疾濠靛鐓冪憸婊堝礈閻旂厧绠栭柨鐔哄Т閸欏﹪鏌ｉ鍛喊婵﹦绮幏鍛村川婵犲啫鍓甸梺鑽ゅ仦閸戝綊宕戞繝鍌滄殾闁哄洢鍨圭粻缁樸亜閺囩偞鍣洪柡鍜佷簻閳规垶骞婇柛濠冩礋楠炲﹥鎯旈敐鍥︾瑝闂侀潧顦弲婊堟偂閸愵亝鍠愭繝濠傜墕缁€鍫熺箾閹寸偟鎳呮い鈺冨厴閺屻劑寮撮悙娴嬪亾瑜版帒纾归柣鎴ｅГ閻撶姷鐥弶鍨埞濠⒀屽灡缁绘盯宕奸顫枈濠?
     *
     * @param
     * @param
     * @return
     */
    @Override
    public ShopAnalysisVO getShopAnalysis(Long shopId, String timeRange) {
        log.info("查询周期为:{}", timeRange);
        LocalDateTime[] range = buildShopAnalysisRange(timeRange);
        String startTime = formatShopAnalysisTime(range[0]);
        String endTime = formatShopAnalysisTime(range[1]);
        ShopOrderAnalysisDTO orderAnalysis = remoteOrderService.getShopOrderAnalysis(shopId, startTime, endTime);
        ShopReviewAnalysisDTO reviewAnalysis = remoteReviewService.getShopReviewAnalysis(shopId, startTime, endTime);
        ShopAnalysisVO result = buildEmptyShopAnalysis();
        if (orderAnalysis != null) {
            result.setTotalOrders(orderAnalysis.getTotalOrders() == null ? 0 : orderAnalysis.getTotalOrders());
            result.setTotalRevenue(orderAnalysis.getTotalRevenue() == null ? java.math.BigDecimal.ZERO : orderAnalysis.getTotalRevenue());
        }
        if (reviewAnalysis != null) {
            result.setAvgScore(normalizeOneDecimal(reviewAnalysis.getAvgScore()));
            result.setBadReviewCount(reviewAnalysis.getBadReviewCount() == null ? 0 : reviewAnalysis.getBadReviewCount());
        }
        normalizeShopAnalysis(result);
        return result;
    }

    @Override
    public ShopSuggestVO getShopSuggest(Long shopId, String timeRange) {
        log.info("time:{}", timeRange);
        ShopOrderSuggestDTO orderSuggest = remoteOrderService.getShopOrderSuggest(shopId, timeRange);
        ShopReviewSuggestDTO reviewSuggest = remoteReviewService.getShopReviewSuggest(shopId, timeRange);
        ShopSuggestVO result = buildEmptyShopSuggest();
        if (orderSuggest != null) {
            result.setWeekOrders(orderSuggest.getWeekOrders() == null ? 0 : orderSuggest.getWeekOrders());
            result.setHotProducts(toProductSalesVOList(orderSuggest.getHotProducts()));
            result.setSlowProducts(toProductSalesVOList(orderSuggest.getSlowProducts()));
        }
        if (reviewSuggest != null) {
            result.setPendingReviewCount(reviewSuggest.getPendingReviewCount() == null ? 0 : reviewSuggest.getPendingReviewCount());
            result.setAvgScore(normalizeOneDecimal(reviewSuggest.getAvgScore()));
            result.setBadReviewCount(reviewSuggest.getBadReviewCount() == null ? 0 : reviewSuggest.getBadReviewCount());
        }
        normalizeShopSuggest(result);
        return result;
    }
    private LocalDateTime[] buildShopAnalysisRange(String timeRange) {
        String normalized = StrUtil.isBlank(timeRange) ? "week" : timeRange.trim().toLowerCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        switch (normalized) {
            case "week":
                return new LocalDateTime[]{now.toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay(), now};
            case "month":
                return new LocalDateTime[]{now.withDayOfMonth(1).toLocalDate().atStartOfDay(), now};
            case "quarter":
                return new LocalDateTime[]{now.minusDays(90), now};
            default:
                throw new BusinessException("unsupported timeRange");
        }
    }

    private String formatShopAnalysisTime(LocalDateTime time) {
        return time.format(SHOP_ANALYSIS_TIME_FORMATTER);
    }

    private java.math.BigDecimal normalizeOneDecimal(java.math.BigDecimal value) {
        if (value == null) {
            return java.math.BigDecimal.ZERO;
        }
        return value.setScale(1, java.math.RoundingMode.HALF_UP);
    }

    private List<ProductSalesVO> toProductSalesVOList(List<ProductSalesDTO> source) {
        if (source == null) {
            return new ArrayList<>();
        }
        return source.stream().filter(Objects::nonNull).map(item -> new ProductSalesVO(item.getProductId(), item.getProductName() == null ? "" : item.getProductName(), item.getSalesCount() == null ? 0L : item.getSalesCount())).collect(Collectors.toList());
    }

    private List<BadReviewVO> toBadReviewVOList(List<BadReviewDTO> source) {
        if (source == null) {
            return new ArrayList<>();
        }
        return source.stream().filter(Objects::nonNull).map(item -> new BadReviewVO(item.getContent() == null ? "" : item.getContent(), item.getScore() == null ? 0 : item.getScore(), item.getCreateTime())).collect(Collectors.toList());
    }

    private ShopAnalysisVO buildEmptyShopAnalysis() {
        return new ShopAnalysisVO(0, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, 0);
    }

    private ShopSuggestVO buildEmptyShopSuggest() {
        return new ShopSuggestVO(0, 0, java.math.BigDecimal.ZERO, 0, new ArrayList<>(), new ArrayList<>());
    }

    private void normalizeShopAnalysis(ShopAnalysisVO result) {
        if (result.getTotalOrders() == null) {
            result.setTotalOrders(0);
        }
        if (result.getTotalRevenue() == null) {
            result.setTotalRevenue(java.math.BigDecimal.ZERO);
        }
        if (result.getAvgScore() == null) {
            result.setAvgScore(java.math.BigDecimal.ZERO);
        }
        if (result.getBadReviewCount() == null) {
            result.setBadReviewCount(0);
        }
    }

    private void normalizeShopSuggest(ShopSuggestVO result) {
        if (result.getWeekOrders() == null) {
            result.setWeekOrders(0);
        }
        if (result.getPendingReviewCount() == null) {
            result.setPendingReviewCount(0);
        }
        if (result.getAvgScore() == null) {
            result.setAvgScore(java.math.BigDecimal.ZERO);
        }
        if (result.getBadReviewCount() == null) {
            result.setBadReviewCount(0);
        }
        if (result.getHotProducts() == null) {
            result.setHotProducts(new ArrayList<>());
        }
        if (result.getSlowProducts() == null) {
            result.setSlowProducts(new ArrayList<>());
        }
    }
    @Override
    public List<Shop> selectShopListByUserId(Long userId, Shop shop) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<Long> shopIds = remoteUserService.getShopIdsByUserId(userId);
        if (CollUtil.isEmpty(shopIds)) {
            return Collections.emptyList();
        }
        return shopMapper.selectShopListByIdsAndCondition(shopIds, shop == null ? new Shop() : shop);
    }

    @Override
    public List<ShopVO> searchShops(String keyword) {
        String trimmedKeyword = keyword == null ? null : keyword.trim();
        List<Shop> shops = query()
                .select("id", "name", "status", "audit_status")
                .like(StrUtil.isNotBlank(trimmedKeyword), "name", trimmedKeyword)
                .eq("status", 1)
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .list();
        return convertToShopVOList(shops);
    }
}
