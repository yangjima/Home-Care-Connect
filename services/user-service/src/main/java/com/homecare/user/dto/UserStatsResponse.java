package com.homecare.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户管理页顶部统计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {

    private long total;
    private long serviceStaff;
    private long distributor;
    /** 最近 N 分钟内有登录记录的用户数 */
    private long online;
}
