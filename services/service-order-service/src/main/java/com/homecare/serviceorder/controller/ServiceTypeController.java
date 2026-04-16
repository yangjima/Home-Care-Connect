package com.homecare.serviceorder.controller;

import com.homecare.serviceorder.common.Result;
import com.homecare.serviceorder.service.ServiceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 服务类型控制器
 */
@RestController
@RequestMapping("/service-types")
@RequiredArgsConstructor
public class ServiceTypeController {

    private final ServiceTypeService serviceTypeService;

    /**
     * 获取所有服务类型
     */
    @GetMapping
    public Result<List<?>> listServiceTypes(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        List<?> types = activeOnly
                ? serviceTypeService.listActiveServiceTypes()
                : serviceTypeService.listAllServiceTypes();
        return Result.success(types);
    }

    /**
     * 获取服务类型详情
     */
    @GetMapping("/{id}")
    public Result<Object> getServiceType(@PathVariable Long id) {
        Object type = serviceTypeService.getServiceTypeById(id);
        return Result.success(type);
    }
}
