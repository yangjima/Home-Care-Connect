package com.homecare.asset.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时保证关键表存在，避免历史数据库未初始化导致接口报错。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaBootstrapConfig {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureSecondhandItemTable() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = 'secondhand_item'",
                Integer.class
        );

        if (tableCount != null && tableCount > 0) {
            return;
        }

        log.warn("Table secondhand_item not found in current schema, creating it automatically.");
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS secondhand_item (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                        "publisher_id BIGINT NOT NULL COMMENT '发布用户ID'," +
                        "title VARCHAR(200) NOT NULL COMMENT '物品标题'," +
                        "category VARCHAR(50) NOT NULL COMMENT '分类'," +
                        "price DECIMAL(10,2) NOT NULL COMMENT '价格'," +
                        "`condition` ENUM('like_new', 'good', 'fair') NOT NULL COMMENT '新旧程度：几乎全新/良好/一般'," +
                        "description TEXT COMMENT '物品描述'," +
                        "images JSON COMMENT '图片列表（MinIO URL）'," +
                        "contact VARCHAR(100) COMMENT '联系方式'," +
                        "status TINYINT DEFAULT 1 COMMENT '状态：1=上架 0=下架'," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                        "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                        "INDEX idx_publisher (publisher_id)," +
                        "INDEX idx_category (category)," +
                        "INDEX idx_status (status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='二手物品表'"
        );
    }
}
