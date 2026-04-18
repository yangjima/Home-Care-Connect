package com.homecare.serviceorder.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homecare.serviceorder.dto.StaffPublicResponse;
import com.homecare.serviceorder.entity.ServiceStaff;
import com.homecare.serviceorder.repository.ServiceStaffRepository;
import com.homecare.serviceorder.service.ServiceStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务人员列表
 */
@Service
@RequiredArgsConstructor
public class ServiceStaffServiceImpl implements ServiceStaffService {

    private static final String[] DISPLAY_NAMES = {"李师傅", "王师傅", "张师傅", "刘师傅"};

    private final ServiceStaffRepository serviceStaffRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<StaffPublicResponse> listPublicStaff() {
        List<ServiceStaff> rows = serviceStaffRepository.selectPublicWithUser();
        List<StaffPublicResponse> out = new ArrayList<>();
        int i = 0;
        for (ServiceStaff s : rows) {
            StaffPublicResponse r = new StaffPublicResponse();
            r.setId(s.getId());
            r.setName(DISPLAY_NAMES[i % DISPLAY_NAMES.length]);
            i++;
            int orders = s.getOrderCount() != null ? s.getOrderCount() : 0;
            r.setSkillsLabel(buildSkillsShort(s.getSkills()));
            r.setRating(s.getStarRating());
            r.setCompletedOrders(orders);
            r.setAvatar(s.getAvatar());
            out.add(r);
        }
        return out;
    }

    /** 与原型一致：技能行与单量分行展示 */
    private String buildSkillsShort(String skillsJson) {
        if (skillsJson == null || skillsJson.isBlank()) {
            return "社区服务 · 专业认证";
        }
        try {
            List<String> arr = objectMapper.readValue(skillsJson, new TypeReference<List<String>>() {});
            if (arr == null || arr.isEmpty()) {
                return "社区服务 · 专业认证";
            }
            if (arr.size() >= 2) {
                return arr.get(0) + " · " + arr.get(1);
            }
            return arr.get(0) + " · 专业服务";
        } catch (Exception e) {
            return "社区服务 · 专业认证";
        }
    }
}
