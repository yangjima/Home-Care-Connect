package com.homecare.serviceorder.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 服务列表页展示用服务人员信息
 */
@Data
public class StaffPublicResponse {

    private Long id;
    /** 展示姓名（与原型一致：李/王/张/刘师傅轮替） */
    private String name;
    /** 技能与单量摘要 */
    private String skillsLabel;
    private BigDecimal rating;
    private Integer completedOrders;
    private String avatar;
}
