package com.homecare.serviceorder.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaBootstrapConfig {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureServiceTypeSchema() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = 'service_type'",
                Integer.class
        );
        if (tableCount == null || tableCount == 0) {
            log.warn("Table service_type not found in current schema, creating it automatically.");
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS service_type (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "name VARCHAR(120) NOT NULL COMMENT '服务名称'," +
                            "description VARCHAR(500) NULL COMMENT '描述'," +
                            "category VARCHAR(100) NOT NULL COMMENT '分类'," +
                            "base_price DECIMAL(10,2) NOT NULL COMMENT '基础价格'," +
                            "unit VARCHAR(20) DEFAULT '次' COMMENT '单位'," +
                            "icon VARCHAR(100) NULL COMMENT '图标'," +
                            "status TINYINT DEFAULT 2 COMMENT '2=待审 1=上架 0=下架'," +
                            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                            "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                            "INDEX idx_status (status)," +
                            "INDEX idx_category (category)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务类型表'"
            );
            return;
        }

        addColumnIfMissing("service_type", "category",
                "ALTER TABLE service_type ADD COLUMN category VARCHAR(100) NOT NULL DEFAULT 'other' COMMENT '分类'");
        addColumnIfMissing("service_type", "base_price",
                "ALTER TABLE service_type ADD COLUMN base_price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '基础价格'");
        addColumnIfMissing("service_type", "unit",
                "ALTER TABLE service_type ADD COLUMN unit VARCHAR(20) DEFAULT '次' COMMENT '单位'");
        addColumnIfMissing("service_type", "icon",
                "ALTER TABLE service_type ADD COLUMN icon VARCHAR(100) NULL COMMENT '图标'");
        addColumnIfMissing("service_type", "status",
                "ALTER TABLE service_type ADD COLUMN status TINYINT DEFAULT 2 COMMENT '2=待审 1=上架 0=下架'");
    }

    private void addColumnIfMissing(String table, String column, String alterSql) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                table, column
        );
        if (c != null && c == 0) {
            log.info("Adding missing column {}.{}", table, column);
            jdbcTemplate.execute(alterSql);
        }
    }
}
