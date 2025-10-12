package com.smartLive.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Stats {
    private Integer blogCount;
    private Integer followCount;
    private Integer fansCount;
    private Integer likeCount;
}
