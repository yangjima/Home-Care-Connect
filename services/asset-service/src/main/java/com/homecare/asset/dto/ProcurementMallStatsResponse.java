package com.homecare.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 本地商城首页统计（已上架商品总数）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementMallStatsResponse {

    private long totalOnShelf;
}
