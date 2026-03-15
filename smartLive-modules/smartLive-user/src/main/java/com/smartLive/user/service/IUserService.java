package com.smartLive.user.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.user.domain.Stats;
import com.smartLive.user.domain.User;
import com.smartLive.user.domain.VO.UserVO;

/**
 * 閻劍鍩汼ervice閹恒儱褰?
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface IUserService extends IService<User>
{
    /**
     * 閺屻儴顕楅悽銊﹀煕
     * 
     * @param id 閻劍鍩涙稉濠氭暛
     * @return 閻劍鍩?
     */
     User selectUserById(Long id);

    /**
     * 閺屻儴顕楅悽銊﹀煕閸掓銆?
     * 
     * @param user 閻劍鍩?
     * @return 閻劍鍩涢梿鍡楁値
     */
     List<User> selectUserList(User user);

    /**
     * 閺傛澘顤冮悽銊﹀煕
     * 
     * @param user 閻劍鍩?
     * @return 缂佹挻鐏?
     */
     int insertUser(User user);

    /**
     * 娣囶喗鏁奸悽銊﹀煕
     * 
     * @param user 閻劍鍩?
     * @return 缂佹挻鐏?
     */
     int updateUser(User user);

    /**
     * 閹靛綊鍣洪崚鐘绘珟閻劍鍩?
     * 
     * @param ids 闂団偓鐟曚礁鍨归梽銈囨畱閻劍鍩涙稉濠氭暛闂嗗棗鎮?
     * @return 缂佹挻鐏?
     */
     int deleteUserByIds(Long[] ids);

    /**
     * 閸掔娀娅庨悽銊﹀煕娣団剝浼?
     * 
     * @param id 閻劍鍩涙稉濠氭暛
     * @return 缂佹挻鐏?
     */
     int deleteUserById(Long id);

    /**
     * 閺嶈宓侀悽銊﹀煕閻絻鐦介崣椋庣垳閺屻儴顕楅悽銊﹀煕
     * @param phone 閹靛婧€閸?
     * @return 閻劍鍩?
     */
    User getUserInfoByPhone(String phone);

    /**
     * 閻絻鐦介崣椋庣垳閸掓稑缂撻悽銊﹀煕
     * @param phone 閹靛婧€閸?
     * @return 閻劍鍩?
     */
    User createUserByPhone(String phone);

    /**
     * 閺嶈宓侀悽銊﹀煕id閸掓銆冮弻銉嚄閻劍鍩涢崚妤勩€?
     * @param userIdList 閻劍鍩沬d閸掓銆?
     * @return 閻劍鍩涢崚妤勩€?
     */
  List<UserVO> getUserList(List<Long> userIdList);
    /**
     * 閺嶈宓侀悽銊﹀煕id閺屻儴顕楅悽銊﹀煕
     * @param id 閻劍鍩沬d
     * @return 閻劍鍩?
     */
    UserVO queryUserById(Long id);

    /**
     * 閼惧嘲褰囬悽銊﹀煕缂佺喕顓告穱鈩冧紖
     * @param userId 閻劍鍩沬d
     * @return 閻劍鍩涚紒鐔活吀娣団剝浼?
     */
    Stats getStats(Long userId);

    /**
     * 閸忋劑鍎撮崣鎴濈
     *
     * @return 閸忋劑鍎撮崣鎴濈缂佹挻鐏?
     */
    String allPublish();

    /**
     * 閸欐垵绔?
     *
     * @param
     * @return 閸欐垵绔风紒鎾寸亯
     */
    String publish( String[] ids);

    /**
     * 娣囶喗鏁奸悽銊﹀煕鐎靛棛鐖?
     * @param userId 閻劍鍩沬d
     * @param passwordDTO 鐎靛棛鐖淒TO
     * @return
     */
    Boolean updateUserPassWord(Long userId, com.smartLive.user.DTO.PasswordDTO passwordDTO);
    /**
     * 閺嶈宓侀悽銊﹀煕id閼惧嘲褰囬悽銊﹀煕閸氬秶袨
     * @param userId
     * @return
     */
    String getUserNameById(Long userId);
    /**
     * 閺嶈宓侀悽銊﹀煕id閺屻儴顕楅悽銊﹀煕娣団剝浼?
     * @param id
     * @return
     */
    UserVO queryUserInfoById(Long id);

    /**
     * 濞撳懐鎮婇悽銊﹀煕鐠囷附鍎忕紓鎾崇摠
     * @param userId 閻劍鍩沬d
     */
    void clearUserCache(Long userId);
    /**
     * 閺嶈宓侀悽銊﹀煕id閺屻儴顕楅悽銊﹀煕鐠囷附鍎?
     * @param userId 閻劍鍩沬d
     * @return 閻劍鍩涚拠锔藉剰
     */
    UserVO getUserById(Long userId);

    List<UserVO> searchUsers(String keyword);
}
