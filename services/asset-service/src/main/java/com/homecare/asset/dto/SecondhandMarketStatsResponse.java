package com.homecare.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 跳蚤市场页统计（在售总数、本周新增）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecondhandMarketStatsResponse {

    private long totalOnSale;
    private long newThisWeek;
}
