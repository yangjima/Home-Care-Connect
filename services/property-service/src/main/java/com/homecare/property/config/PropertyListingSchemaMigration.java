package com.homecare.property.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 为已有库扩展房源状态枚举（待审/驳回），与 init.sql 新装一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PropertyListingSchemaMigration {

    private final DataSource dataSource;

    @EventListener(ApplicationReadyEvent.class)
    public void migratePropertyStatusEnum() {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            String columnType = null;
            try (ResultSet rs = st.executeQuery(
                    "SELECT COLUMN_TYPE FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'property' AND COLUMN_NAME = 'status'")) {
                if (rs.next()) {
                    columnType = rs.getString(1);
                }
            }
            if (columnType == null) {
                return;
            }
            if (columnType.contains("pending")) {
                return;
            }
            log.info("Migrating property.status ENUM for listing approval");
            st.executeUpdate(
                    "ALTER TABLE property MODIFY COLUMN status "
                            + "ENUM('pending','rejected','vacant','occupied','reserved') NOT NULL DEFAULT 'vacant' "
                            + "COMMENT 'pending=待上架审核 rejected=已驳回 vacant=已上架空置 occupied=已租 reserved=下架/预定'");
        } catch (Exception e) {
            log.warn("property.status migration skipped: {}", e.getMessage());
        }
    }
}
