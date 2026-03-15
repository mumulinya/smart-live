package com.smartLive.user.service.impl;
import com.smartLive.common.core.constant.mq.SearchMqConstants;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import com.smartLive.common.core.domain.AppLoginUser;
import com.smartLive.common.redis.util.CacheClient;
import com.smartLive.common.redis.util.RedisMultiCacheManager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.core.utils.bean.BeanUtils;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.rabbitmq.domain.ContentSyncMessage;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.interaction.api.RemoteFollowService;
import com.smartLive.interaction.api.RemoteLikeService;
import com.smartLive.interaction.api.RemoteStarService;
import com.smartLive.interaction.api.DTO.FollowDTO;
import com.smartLive.interaction.api.DTO.LikeDTO;
import com.smartLive.interaction.api.DTO.StarDTO;
import com.smartLive.user.api.domain.UserDTO;
import com.smartLive.user.domain.Stats;
import com.smartLive.user.domain.UserInfo;
import com.smartLive.user.domain.VO.UserInfoVO;
import com.smartLive.user.domain.VO.UserVO;
import com.smartLive.user.service.IUserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.smartLive.user.mapper.UserMapper;
import com.smartLive.user.domain.User;
import com.smartLive.user.service.IUserService;
import static com.smartLive.common.core.constant.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰銉︽叏閸︻厾鍊皉vice婵犵數鍋為崹鍫曞箰婵犳碍鍤岄柣鎰靛墯閸欏繘鏌ｉ弮鍥ㄣ€冩繛宀婁邯閺屾稓浠﹂幆褍姣堝┑鈩冪叀娴滃爼寮?
 *
 * @author mumulin
 * @date 2025-09-21
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService
{
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisService redisService;
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private IUserInfoService userInfoService;
    @Autowired
    private RedisMultiCacheManager redisMultiCacheManager;

    @Autowired
    private RemoteBlogService remoteBlogService;

    
    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private RemoteFollowService remoteFollowService;
    @Autowired
    private RemoteLikeService remoteLikeService;
    @Autowired
    private RemoteStarService remoteStarService;

    /**
     * 闂備浇顕х换鎰崲閹版澘绠烘俊鐐摫r闂備浇顕ф绋匡耿闁秴纾绘俊顖濆亹缁€濠囨倵閿濆骸浜濆┑顔界矋閵囧嫰骞掑鍥舵М闁瑰吋娼欓敃銈夊煡婢舵劕绠诲Λ鐗堢箓濞堟ⅴerVO
     * @param user User闂備浇顕ф绋匡耿闁秴纾绘俊顖濆亹缁€?
     * @return UserVO闂備浇顕уù鐑藉极閹间降鈧焦绻濋崶銊ョ樁?
     */
    private UserVO convertToUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 闂備浇顕х换鎰崲閹版澘绠烘俊鐐摫r闂傚倷绀侀幉锛勬暜濡ゅ懌鈧啯寰勯幇顑┿儵鏌涢幇銊︽珕濠殿喗绮嶉妵鍕箳瀹ュ浂妲柟鍏兼綑閿曘倝鍩ユ径鎰濡増绻傚▓姊rVO闂傚倷绀侀幉锛勬暜濡ゅ懌鈧啯寰勯幇顑?
     * @param userList User闂備浇顕ф绋匡耿闁秴纾绘俊顖濆亹缁€濠囨倵閿濆骸鏋涚紒鈧崱妯圭箚闁靛牆鎳庨銉╂煃?
     * @return UserVO闂傚倷绀侀幉锛勬暜濡ゅ懌鈧啯寰勯幇顑?
     */
    private List<UserVO> convertToUserVOList(List<User> userList) {
        if (userList == null || userList.isEmpty()) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::convertToUserVO)
                .collect(Collectors.toList());
    }
    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫤闁稿鍔戦弻鏇熺節韫囨洜鏆犻梺?
     *
     * @param id 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰銉︾箾閹寸偟鎳呴柍缁樻煥閳规垿鎮╁畷鍥舵殹闂?
     * @return 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰?
     */
    @Override
    public User selectUserById(Long id)
    {
        return userMapper.selectUserById(id);
    }

    /**
     * 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫤闁稿鍔戦弻鏇熺節韫囨洜鏆犻梺缁樻尰濞茬喖寮诲☉銏犵闁瑰灝鍟悾浠嬫⒑?
     *
     * @param user 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰?
     * @return 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰?
     */
    @Override
    public List<User> selectUserList(User user)
    {
        return userMapper.selectUserList(user);
    }

    /**
     * 闂傚倷绀侀幖顐﹀磹閻熼偊鐔嗘慨妞诲亾鐠侯垶鏌涢幇闈涙灍闁稿鍔戦弻鏇熺節韫囨洜鏆犻梺?
     *
     * @param user 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰?
     * @return 缂傚倸鍊搁崐鐑芥倿閿曞倸绠板┑鐘崇閸?
     */
    @Override
    public int insertUser(User user)
    {
        user.setCreateTime(DateUtils.getNowDate());
        return userMapper.insertUser(user);
    }

    /**
     * 婵犵數鍎戠徊钘壝归崒鐐茬獥婵°倕鎳庨弸浣糕攽閸屾碍鍟為柛瀣ㄥ姂閺屾洘绻濊箛鏇犳殸闂?
     *
     * @param user 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰?
     * @return 缂傚倸鍊搁崐鐑芥倿閿曞倸绠板┑鐘崇閸?
     */
    @Override
    public int updateUser(User user)
    {
        user.setUpdateTime(DateUtils.getNowDate());
        int i = userMapper.updateUser(user);
        if(i>0){
            clearUserCache(user.getId());
            AppLoginUser dto = UserContextHolder.getUser();
            //闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樺弶澶勯柛瀣ㄥ姂閺屾洘绻濊箛鏇犳殸闂佺粯鎸诲ú婊呮閹捐纾兼繛鎴炵懃绾板秹姊虹€癸附婢樻俊鐣岀磼瀹€鍕喚闁糕晛瀚板畷妯款槾缂佲偓閳?
            if(dto!=null){
                String tokenKey = dto.getToken();
                User userById = getById(user.getId());
                UserDTO userDTO= BeanUtil.copyProperties(userById, UserDTO.class);
                //闂備浇顕х€涒晝绮欓幒妤佹櫔闂?
                Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                        CopyOptions.create()
                                //闂傚倸顭崑鍕洪妸鈺佺柧妞ゆ劧绠戝Ч鏌ユ煙闁箑鏋ら柣顓熷哺閺屾稑鈹戦崱妤婁槐闂?
                                .setIgnoreNullValue(true)
                                //闂傚倷鑳堕、濠傗枖濞戭潿鈧懐寰婇悮鐖嶥to闂備浇顕х€涒晝绮欓幒妞尖偓鍐幢濞戣鲸鏅╅悗鍏夊亾闁告洦鍋嗛敍娆撴⒑閻撳孩璐″褎顨堢划濠囶敊鐏忔牗顫嶉梺瑙勫礃濞呮洟骞嗛崼銉︾厽闁愁垱鐟ラ幊鎰不閺冨牊鐓ラ柡鍐ㄦ搐閸斿灚銇?
                                .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? "" : fieldValue.toString()));
                //闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樻彃鏆熼柍缁樻礋閹鏁愭惔婵堢泿濡炪們鍊曢幊姗€寮婚敐澶娢╅柕澶堝労娴犻箖姊洪崫鍕棤闁哥姵鐗犻悰?
                redisService.setCacheMap(tokenKey,userMap);
                //闂備浇宕垫慨宕囩矆娴ｈ娅犲ù鐘差儐閸嬵亪鏌涢悙鎼瀫ken闂傚倷绀侀幖顐︽偋閸℃蛋鍥ㄥ閺夋垹鏌ч梺闈涱槴閺呮稓鈧?
                redisService.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
                UserContextHolder.removeUser();
            }
            //闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閸屾粎姣€闂傚倷娴囧銊╂嚄閼稿灚娅犳俊銈傚亾闁?
            publish(new String[]{user.getId().toString()});
            sendAuditMessage(user);
        }
        return i;
    }
    /**
     * 闂傚倷绀侀幉锟犳偡閿曞倸鍨傞柛褎顨呴悞鍨亜閹达絾纭舵い锔奸檮閵囧嫰骞樼€涙ê鈧劗鈧鍠曠划娆忕暦閵婏妇绡€闁告洘鍨崕鐢稿蓟?
     * @param user
     */
    private void sendAuditMessage(User user) {
        UserInfoVO userInfo = userInfoService.getByUserId(user.getId());
        UserVO userVO = convertToUserVO(user);
        userVO.setIntroduce(userInfo.getIntroduce());
        userVO.setBackgroundImage(userInfo.getBackgroundImage());
        userVO.setCity(userInfo.getCity());

        AuditMessage auditMessage = AuditMessage.builder()
                .bizId(user.getId())
                .bizType(GlobalBizTypeEnum.USER.getCode())
                .submitterId(user.getId())
                .auditContent(BeanUtil.beanToMap(userVO))
                .createTime(user.getCreateTime())
                .build();
        mqMessageSendUtils.sendMqMessage( AiAuditMqConstants.AUDIT_DIRECT_EXCHANGE,AiAuditMqConstants.AUDIT_ROUTING_KEY, auditMessage);
    }
    /**
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊呯矆閸℃稒鐓熸俊顖濆亹鐢盯鏌ｅ┑鍫濇灈闁哄矉缍侀敐鐐侯敆閳ь剚淇婃禒瀣厱?
     *
     * @param ids 闂傚倸鍊搁崐绋棵洪悩璇茬；闁瑰墽绮崑锟犳煛閸ャ劍鐨戞い锔肩畵閺屾盯濡搁妷褏楔濠殿喖锕ｇ划娆愪繆閸洖鐐婇柕濞у嫭顔忛梻鍌欑劍濡炲灝顭囬崸妤€绀夌€广儱顦弰銉︾箾閹寸偟鎳呴柍缁樻煥閳规垿鎮╁畷鍥舵殹闂?
     * @return 缂傚倸鍊搁崐鐑芥倿閿曞倸绠板┑鐘崇閸?
     */
    @Override
    public int deleteUserByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        int i = userMapper.deleteUserByIds(ids);
        if (i > 0) {
            clearUserCacheBatch(Arrays.asList(ids));
            for (Long id : ids) {
                executorService.submit(()->{
                    log.info("Deleting user {} from search indexes on thread {}", id, Thread.currentThread().getName());
                    ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                    contentSyncMessage.setId(id);
                    contentSyncMessage.setIndexName(EsIndexNameConstants.USER_INDEX_NAME);
                    contentSyncMessage.setType(GlobalBizTypeEnum.USER.getCode());
                    //闂傚倷绀侀幉锟犳偡閿曞倸鍨傚┑鍌滎焾缁犲弶銇勯妶鍥╁綅bbitMq婵犵數鍎戠徊钘壝洪悩璇茬婵犻潧娲ら閬嶆煕濞戞瑦缍戠紒鈧崱娑欑厽婵☆垵鍋愮敮娑㈡煟?
                    mqMessageSendUtils.sendMqMessage( SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                });
            }
        }
        return i;
    }

    /**
     * 闂傚倷绀侀幉锛勬暜閻愬绠鹃柍褜鍓氱换娑㈠川椤撱垹寮伴梺璇″枙缁瑦淇婇幖浣规櫇闁逞屽墴閹繝寮撮悙鍐ㄩ叄瀹曞爼鏁愰崨顓涙嫟濠?
     *
     * @param id 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰銉︾箾閹寸偟鎳呴柍缁樻煥閳规垿鎮╁畷鍥舵殹闂?
     * @return 缂傚倸鍊搁崐鐑芥倿閿曞倸绠板┑鐘崇閸?
     */
    @Override
    public int deleteUserById(Long id)
    {
        int i = userMapper.deleteUserById(id);
        if (i > 0) {
            clearUserCache(id);
        }
        return i;
    }

    /**
     * 闂傚倷绀侀幖顐ょ矓閻戞枻缍栧璺猴功閺嗐倕銆掑锝呬壕闂佽鍠曠划娆愪繆閹间焦鏅濋柍褜鍓熼幃锟犲即閵忥紕鍘藉銈庡亽閸樺墽绮婇悜鑺ョ厽闁挎棁娉曢惌娆戔偓瑙勬穿缁查箖藝閻楀牊鍎熼柨婵嗘噽娴狀垶姊绘担鍛婃儓閻炴凹鍋婂畷鏇㈠箻椤曞懏鏅┑掳鍊曢幊蹇涘磻閵娾晜鐓忓┑鐘茬箳閻ｉ亶鏌?
     *
     * @param phone 闂傚倷绀佺紞濠傤焽瑜旈、鏍幢濡炵粯鏁犻梺閫炲苯澧撮柡?
     * @return 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰?
     */
    @Override
    public User getUserInfoByPhone(String phone) {
        User user = query().eq("phone", phone).one();
        return user;
    }

    /**
     * 闂傚倷鐒﹀鍨熆閳ь剛绱掗幓鎺濈吋闁诡垯鐒︾粋鎺斺偓锝庝簽閻撴垶淇婇悙宸剰婵炲鍏樺畷褰掑箮閼恒儳鍘遍梺鍦劋閹搁箖鍩ユ径宀€纾介柛灞剧⊕瀹曞矂鏌熼鑽ょ煓濠碘剝鎮傞弫鍐焵椤掑嫭鍋?
     *
     * @param phone 闂傚倷绀佺紞濠傤焽瑜旈、鏍幢濡炵粯鏁犻梺閫炲苯澧撮柡?
     * @return 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰?
     */
    @Override
    public User createUserByPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);

        // 闂傚倷绀侀幉锛勬暜濡ゅ啯宕查柛宀€鍎戠紞鏍煙閻楀牊绶茬紒鈧畝鍕厸鐎规搩鍠栭懟顖氣枔閹间焦鐓欓柣鎾虫捣閹界姵鎱ㄦ繝鍜佸殭闁?UserInfo
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setFans(0);
        userInfo.setFollowee(0);
        userInfo.setLiked(0);
        userInfo.setCreateTime(DateUtils.getNowDate());
        userInfoService.save(userInfo);

        return user;
    }

    /**
     * 闂傚倷绀侀幖顐ょ矓閻戞枻缍栧璺猴功閺嗐倕銆掑锝呬壕闂佽鍠曠划娆愪繆閹间焦鏅濋柍褜鍓熼幃锟犲及閻偊姊绘担鍛婂暈閻㈩垱顨婇妴鍐╁緞閹邦儵銉╂煕閹伴潧鏋涢柦鍐枛閺屾洘寰勫Ο鐓庡弗闂佹悶鍊曠€氫即寮婚敐澶涚稏妞ゆ巻鍋撳┑鈥茬矙閺屾盯鍩￠崒婊勫垱閻庤娲橀懝鎹愮亙闂佸憡娲嶉弬渚€宕?
     *
     * @param userIdList 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰銉︾箾瀹€鈧幆鍕⒒娴ｅ憡鍟為悽顖涱殜閵嗗啯寰勯幇顑?
     * @return 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰銉︾箾閹存瑥鐏╃紒鈧崱妯圭箚闁靛牆鎳庨銉╂煃?
     */
    @Override
    public List<UserVO> getUserList(List<Long> userIdList) {
        // 1. Utilize RedisBatchCacheUtil for cached batch retrieval (UserVO with static info)
        List<UserVO> userVOList = redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_USER_KEY,
                RedisConstants.LOCK_USER_KEY,
                userIdList,
                UserVO.class,
                missingIds -> {
                    // DB Fallback
                    String idStr = StrUtil.join(",", missingIds);
                    List<User> users = query().in("id", missingIds)
                            .last("order by field(id," + idStr + ")")
                            .list();

                    return users.stream().map(user -> {
                        UserVO userVO = convertToUserVO(user);
                        if (userVO != null) {
                            UserInfoVO userInfo = userInfoService.getByUserId(user.getId());
                            if (userInfo != null) {
                                userVO.setIntroduce(userInfo.getIntroduce());
                                userVO.setCity(userInfo.getCity());
                                userVO.setBackgroundImage(userInfo.getBackgroundImage());
                            }
                        }
                        return userVO;
                    }).collect(Collectors.toList());
                },
                UserVO::getId,
                RedisConstants.CACHE_USER_TTL,
                TimeUnit.MINUTES
        );

        // 2. Populate dynamic info (isFollow) which depends on current user context
        if (CollUtil.isNotEmpty(userVOList)) {
            for (UserVO userVO : userVOList) {
               if (userVO != null) {
                   isFollow(userVO);
               }
            }
        }
        return userVOList;
    }

    /**
     * 闂傚倷绀侀幖顐ょ矓閻戞枻缍栧璺猴功閺嗐倕銆掑锝呬壕闂佽鍠曠划娆愪繆閹间焦鏅濋柍褜鍓熼幃锟犲及閻偊姊绘担鍛婃儓閻炴凹鍋婂畷鏇㈠箻椤曞懏鏅┑掳鍊曢幊蹇涘磻閵娾晜鐓忓┑鐘茬箳閻ｉ亶鏌?
     *
     * @param id 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰銉︾箾瀹€鈧幆?
     * @return 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰?
     */
    @Override
    public UserVO queryUserById(Long id) {
        UserVO userVO = cacheClient.queryWithLogicalExpireAndPassThrough(
                RedisConstants.CACHE_USER_KEY,
                RedisConstants.LOCK_USER_KEY,
                id,
                UserVO.class,
                this::loadUserDetailForCache,
                RedisConstants.CACHE_USER_TTL,
                TimeUnit.MINUTES
        );
        if(userVO != null){
            //闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柟缁㈠櫘閺佸淇婇妶鍛櫤闁稿鍔戦弻鏇熺節韫囨洜鏆犻梺缁樻尰濞茬喖寮婚敓鐘茬闂傚牊绋撴禒鈺呮⒑鐠団€崇仧缂佽埖鑹鹃悾宄拔旈崨顔藉劒濡炪倖鍔х徊鎯р枔濮椻偓閹嘲顭ㄩ崨顓ф毉闂佸湱顭堥幉锟犲疾閸洘鍋愮紓浣姑禒娲⒑闂堟侗鐓紒鐘冲灴閹?
            isFollow(userVO);
        }
        return userVO;
    }

    /**
     * 婵犵數鍎戠徊钘壝归崒鐐茬獥婵°倕鎳庨弸浣糕攽閸屾碍鍟為柛瀣ㄥ姂閺屾洘绻濊箛鏇犳殸闂佺粯鎸诲ú鐔煎箖濡ゅ懏顥堟繛鎴炵懐濡倝姊?
     *
     * @param userId 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰銉︾箾瀹€鈧幆?
     * @param passwordDTO 闂備浇顕ч柊锝咁焽瑜嶈灋婵炴垯鍨洪崑澶嬬箾閿濆棗顥扥
     * @return
     */
    @Override
    public Boolean updateUserPassWord(Long userId, com.smartLive.user.DTO.PasswordDTO passwordDTO) {
        User byId = getById(userId);
        if (byId == null) {
            return false;
        }
        if (byId.getPassword() == null) {
            throw new BusinessException("password not set");
        }
        if (passwordDTO.getNewPassword() == null) {
            throw new BusinessException("new password is required");
        }
        if (passwordDTO.getOldPassword() == null) {
            throw new BusinessException("old password is required");
        }
        String rawPassword = byId.getPassword();
        String oldPassword = passwordDTO.getOldPassword();
        if (!SecurityUtils.matchesPassword(oldPassword, rawPassword)) {
            throw new BusinessException("old password is incorrect");
        }
        byId.setPassword(SecurityUtils.encryptPassword(passwordDTO.getNewPassword()));
        boolean updated = updateById(byId);
        if (updated) {
            clearUserCache(byId.getId());
        }
        return updated;
    }

    /**
     * Aggregate user stats.
     */
    @Override
    public Stats getStats(Long userId) {
        CountDownLatch countDownLatch = new CountDownLatch(5);
        AppLoginUser user = UserContextHolder.getUser();

        Future<Integer> commonFollowCountFuture = executorService.submit(() -> {
            Integer commonFollowCount = 0;
            if (user != null && !Objects.equals(user.getId(), userId)) {
                Long currentUserId = user.getId();
                FollowDTO followDTO = new FollowDTO();
                followDTO.setSourceType(GlobalBizTypeEnum.USER.getCode());
                followDTO.setUserId(currentUserId);
                followDTO.setSourceId(userId);
                commonFollowCount = remoteFollowService.getCommonFollowCount(followDTO);
            }
            countDownLatch.countDown();
            return commonFollowCount;
        });
        Future<Integer> blogCountFuture = executorService.submit(() -> {
            log.info("Loading blog count for user {} on thread {}", userId, Thread.currentThread().getName());
            Integer blogCount = remoteBlogService.getBlogCount(userId);
            countDownLatch.countDown();
            return blogCount;
        });
        Future<Integer> likeCountFuture = executorService.submit(() -> {
            log.info("Loading received like count for user {} on thread {}", userId, Thread.currentThread().getName());
            Integer likeCount = remoteBlogService.getLikeCount(userId);
            countDownLatch.countDown();
            return likeCount;
        });
        Future<Integer> blogLikeCountFuture = executorService.submit(() -> {
            LikeDTO likeDTO = new LikeDTO();
            likeDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
            likeDTO.setUserId(userId);
            Integer blogLikeCount = remoteLikeService.getUserLikeCount(likeDTO);
            countDownLatch.countDown();
            return blogLikeCount;
        });
        Future<Integer> blogStarCountFuture = executorService.submit(() -> {
            StarDTO starDTO = new StarDTO();
            starDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
            starDTO.setUserId(userId);
            Integer collectCount = remoteStarService.getUserStarCount(starDTO);
            countDownLatch.countDown();
            return collectCount;
        });
        try {
            log.info("Waiting for user stats tasks to finish");
            countDownLatch.await();
            log.info("User stats aggregation finished");
            return Stats.builder()
                    .blogCount(blogCountFuture.get())
                    .commonFollowCount(commonFollowCountFuture.get())
                    .likeCount(likeCountFuture.get())
                    .blogLikeCount(blogLikeCountFuture.get())
                    .blogStarCount(blogStarCountFuture.get())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getUserNameById(Long userId) {
        return query().select("nick_name")
                .eq("id", userId)
                .one()
                .getNickName();
    }

    @Override
    public UserVO queryUserInfoById(Long id) {
        return cacheClient.queryWithLogicalExpireAndPassThrough(
                RedisConstants.CACHE_USER_KEY,
                RedisConstants.LOCK_USER_KEY,
                id,
                UserVO.class,
                this::loadUserDetailForCache,
                RedisConstants.CACHE_USER_TTL,
                TimeUnit.MINUTES
        );
    }

    @Override
    public String allPublish() {
        int page = PageConstants.PAGE_NUMBER;
        int pageSize = PageConstants.ES_PAGE_SIZE;
        while (true) {
            List<User> users = query()
                    .page(new Page<>(page, pageSize))
                    .getRecords();
            if (users.isEmpty()) {
                break;
            }
            int finalPage = page;

            executorService.submit(() -> {
                log.info("Publishing user page {} on thread {}", finalPage, Thread.currentThread().getName());
                List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
                List<UserInfoVO> userInfos = userInfoService.listByUserIds(userIds);
                Map<Long, UserInfoVO> userInfoMap = userInfos.stream().collect(Collectors.toMap(UserInfoVO::getUserId, userInfo -> userInfo));
                List<UserVO> userVOList = convertToUserVOList(users);
                userVOList.forEach(userVO -> {
                    UserInfoVO userInfo = userInfoMap.get(userVO.getId());
                    if (userInfo != null) {
                        userVO.setIntroduce(userInfo.getIntroduce());
                        userVO.setCity(userInfo.getCity());
                    }
                });
                sendUserBatchMessage(userVOList);
                log.info("Published user page {}, size {}", finalPage, users.size());
            });
            page++;
        }
        return "publish success";
    }

    @Override
    public String publish(String[] ids) {
        if (ids == null || ids.length == 0) {
            return "no ids to publish";
        }
        List<Long> idList = Arrays.stream(ids)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        executorService.submit(() -> {
            log.info("Publishing users {} on thread {}", idList, Thread.currentThread().getName());
            List<User> users = query().in("id", idList).list();
            if (CollUtil.isNotEmpty(users)) {
                List<UserInfoVO> userInfos = userInfoService.listByUserIds(idList);
                Map<Long, UserInfoVO> userInfoMap = userInfos.stream().collect(Collectors.toMap(UserInfoVO::getUserId, userInfo -> userInfo));
                List<UserVO> userVOList = convertToUserVOList(users);
                userVOList.forEach(userVO -> {
                    UserInfoVO userInfo = userInfoMap.get(userVO.getId());
                    if (userInfo != null) {
                        userVO.setIntroduce(userInfo.getIntroduce());
                        userVO.setCity(userInfo.getCity());
                    }
                });
                sendUserBatchMessage(userVOList);
            }
        });
        return "publish success";
    }

    private void sendUserBatchMessage(List<?> users) {
        if (CollUtil.isEmpty(users)) {
            return;
        }
        ContentBatchSyncMessage request = new ContentBatchSyncMessage();
        request.setIndexName(EsIndexNameConstants.USER_INDEX_NAME);
        request.setData(users);
        request.setType(GlobalBizTypeEnum.USER.getCode());
        mqMessageSendUtils.sendMqMessage(SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_BATCH_INSERT_ROUTING_KEY, request);
    }

    /**
     * 闂傚倷绀侀幉锛勬暜閸ヮ剙纾归柡宥庡幖閽冪喖鏌涢妷顔煎闁稿鍔戦弻鏇熺節韫囨洜鏆犻梺缁樻尰濞茬喖寮婚敓鐘茬闂傚牊绋撴禒鈺呮⒑鐠団€崇仧闁煎啿鐖奸獮鍐敋閳ь剟銆侀弴銏狀潊闁靛繆鎳ｉ鍫熲拺闁告稑锕ョ亸顓犵磼婢跺﹦鍩ｇ€殿噮鍋婃俊鑸靛緞婵犲嫷鍚呴柣搴ｆ嚀鐎氼厼顭垮Ο瑁や汗闁绘ê纾粻?
     * @param userVO
     */
    private void isFollow(UserVO userVO){
        if (userVO == null) {
            return;
        }
        FollowDTO followDTO=new FollowDTO();
        followDTO.setSourceType(GlobalBizTypeEnum.USER.getCode());
        followDTO.setSourceId(userVO.getId());
        Boolean isFollow = remoteFollowService.isFollowed(followDTO);
        userVO.setIsFollow(isFollow);
    }

    private UserVO loadUserDetailForCache(Long id) {
        User user = getById(id);
        if (user == null) {
            return null;
        }
        UserVO userVO = convertToUserVO(user);
        UserInfoVO userInfo = userInfoService.getByUserId(id);
        if (userInfo != null) {
            userVO.setIntroduce(userInfo.getIntroduce());
            userVO.setBackgroundImage(userInfo.getBackgroundImage());
            userVO.setCity(userInfo.getCity());
            userVO.setFans(userInfo.getFans());
            userVO.setFollowee(userInfo.getFollowee());
        }
        return userVO;
    }

    @Override
    public void clearUserCache(Long userId) {
        if (userId == null) {
            return;
        }
        redisService.deleteObject(RedisConstants.CACHE_USER_KEY + userId);
    }

    private void clearUserCacheBatch(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        userIds.stream().filter(Objects::nonNull).forEach(this::clearUserCache);
    }

    /**
     * 闂傚倷绀侀幖顐ょ矓閻戞枻缍栧璺猴功閺嗐倕銆掑锝呬壕闂佽鍠曠划娆愪繆閹间焦鏅濋柍褜鍓熼幃锟犲及閻偊姊绘担鍛婃儓閻炴凹鍋婂畷鏇㈠箻椤曞懏鏅┑掳鍊曢幊蹇涘磻閵娾晜鐓忓┑鐘茬箳閻ｉ亶鏌ｉ幘瀛樼闁诡喖缍婂畷鍫曟倻閼恒儺鈧秹姊?
     *
     * @param userId 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰銉︾箾瀹€鈧幆?
     * @return 闂傚倷鐒﹀鍨焽閸ф绀夌€广儱顦弰銉︾箾閹寸偟顣查悗姘哺閺屻劑寮崶顭戞闂?
     */
    @Override
    public UserVO getUserById(Long userId) {
        User userById = selectUserById(userId);
        if (userById != null) {
            return convertToUserVO(userById);
        }
        return null;
    }

    @Override
    public List<UserVO> searchUsers(String keyword) {
        String trimmedKeyword = keyword == null ? null : keyword.trim();
        List<User> users = query()
                .select("id", "nick_name")
                .like(StrUtil.isNotBlank(trimmedKeyword), "nick_name", trimmedKeyword)
                .list();
        return convertToUserVOList(users);
    }
}
