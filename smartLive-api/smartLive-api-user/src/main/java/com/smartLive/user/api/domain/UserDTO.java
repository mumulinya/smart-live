package com.smartLive.user.api.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
    private Boolean isFollow;
    private String introduce;
    private String password;
    /** 粉丝数 */
    private Integer fans;
    /** 关注数 */
    private Integer followee;
}
