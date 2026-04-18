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
            ensureSecondhandItemColumns();
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
                        "original_price DECIMAL(10,2) NULL COMMENT '原价（展示划线价）'," +
                        "`condition` ENUM('like_new', 'good', 'fair') NOT NULL COMMENT '新旧程度：几乎全新/良好/一般'," +
                        "description TEXT COMMENT '物品描述'," +
                        "images JSON COMMENT '图片列表（MinIO URL）'," +
                        "contact VARCHAR(100) COMMENT '联系方式'," +
                        "location VARCHAR(200) NULL COMMENT '自提/交易地点'," +
                        "status TINYINT DEFAULT 2 COMMENT '状态：2=待上架审核 1=已上架 0=下架'," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                        "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                        "INDEX idx_publisher (publisher_id)," +
                        "INDEX idx_category (category)," +
                        "INDEX idx_status (status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='二手物品表'"
        );
    }

    /** 历史库由旧版 bootstrap 建表时可能缺少列，与实体 / init.sql 对齐 */
    private void ensureSecondhandItemColumns() {
        addSecondhandColumnIfMissing("original_price",
                "ALTER TABLE secondhand_item ADD COLUMN original_price DECIMAL(10,2) NULL COMMENT '原价（展示划线价）'");
        addSecondhandColumnIfMissing("location",
                "ALTER TABLE secondhand_item ADD COLUMN location VARCHAR(200) NULL COMMENT '自提/交易地点'");
        addSecondhandColumnIfMissing("store_id",
                "ALTER TABLE secondhand_item ADD COLUMN store_id BIGINT NULL COMMENT '所属店铺ID'");
        addSecondhandColumnIfMissing("image",
                "ALTER TABLE secondhand_item ADD COLUMN image VARCHAR(255) NULL COMMENT '封面图'");
        addSecondhandColumnIfMissing("view_count",
                "ALTER TABLE secondhand_item ADD COLUMN view_count BIGINT DEFAULT 0 COMMENT '浏览量'");
        addSecondhandColumnIfMissing("expire_time",
                "ALTER TABLE secondhand_item ADD COLUMN expire_time DATETIME NULL COMMENT '过期时间'");
    }

    private void addSecondhandColumnIfMissing(String columnName, String alterSql) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE table_schema = DATABASE() AND table_name = 'secondhand_item' AND column_name = ?",
                Integer.class,
                columnName
        );
        if (c != null && c == 0) {
            log.info("Adding missing column secondhand_item.{}", columnName);
            jdbcTemplate.execute(alterSql);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureProcurementProductColumns() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = 'procurement_product'",
                Integer.class
        );
        if (tableCount == null || tableCount == 0) {
            log.warn("Table procurement_product not found in current schema, creating it automatically.");
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS procurement_product (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "name VARCHAR(200) NOT NULL COMMENT '商品名称'," +
                            "description TEXT NULL COMMENT '商品描述'," +
                            "category VARCHAR(100) NULL COMMENT '商品分类'," +
                            "price DECIMAL(10,2) NOT NULL COMMENT '价格'," +
                            "stock INT NOT NULL DEFAULT 0 COMMENT '库存'," +
                            "unit VARCHAR(20) DEFAULT '件' COMMENT '计价单位'," +
                            "images JSON NULL COMMENT '图片列表'," +
                            "supplier_id BIGINT NULL COMMENT '商家ID'," +
                            "status VARCHAR(10) DEFAULT '2' COMMENT '状态：2=待审 1=上架 0=下架'," +
                            "sales_count INT DEFAULT 0 COMMENT '累计销量'," +
                            "product_tag VARCHAR(20) NULL COMMENT '角标'," +
                            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                            "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                            "INDEX idx_supplier (supplier_id)," +
                            "INDEX idx_status (status)," +
                            "INDEX idx_category (category)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本地商城商品表'"
            );
        }
        addProcurementColumnIfMissing("unit",
                "ALTER TABLE procurement_product ADD COLUMN unit VARCHAR(20) DEFAULT '件' COMMENT '计价单位'");
        addProcurementColumnIfMissing("sales_count",
                "ALTER TABLE procurement_product ADD COLUMN sales_count INT DEFAULT 0 COMMENT '累计销量'");
        addProcurementColumnIfMissing("product_tag",
                "ALTER TABLE procurement_product ADD COLUMN product_tag VARCHAR(20) NULL COMMENT '角标'");
    }

    private void addProcurementColumnIfMissing(String columnName, String alterSql) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE table_schema = DATABASE() AND table_name = 'procurement_product' AND column_name = ?",
                Integer.class,
                columnName
        );
        if (c != null && c == 0) {
            log.info("Adding missing column procurement_product.{} ", columnName);
            jdbcTemplate.execute(alterSql);
        }
    }
}
