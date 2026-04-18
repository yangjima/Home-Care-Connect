package com.homecare.serviceorder.service;

import com.homecare.serviceorder.dto.StaffPublicResponse;

import java.util.List;

/**
 * 服务人员查询
 */
public interface ServiceStaffService {

    List<StaffPublicResponse> listPublicStaff();
}
