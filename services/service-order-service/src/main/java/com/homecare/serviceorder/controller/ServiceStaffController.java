package com.homecare.serviceorder.controller;

import com.homecare.serviceorder.common.Result;
import com.homecare.serviceorder.dto.StaffPublicResponse;
import com.homecare.serviceorder.service.ServiceStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 服务人员（用户端展示）
 */
@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
public class ServiceStaffController {

    private final ServiceStaffService serviceStaffService;

    /**
     * 优秀服务人员列表（公开）
     */
    @GetMapping
    public Result<List<StaffPublicResponse>> listStaff() {
        return Result.success(serviceStaffService.listPublicStaff());
    }
}
