package com.homecare.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 历史库可能缺少首次/最近登录时间列（init.sql 仅在空库时执行），与实体对齐。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysUserLoginTimesSchemaMigration {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicBoolean migrated = new AtomicBoolean(false);

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener(ContextRefreshedEvent.class)
    public void ensureLoginTimeColumns(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        if (!migrated.compareAndSet(false, true)) {
            return;
        }
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = 'sys_user'",
                Integer.class);
        if (tableCount == null || tableCount == 0) {
            return;
        }
        addColumnIfMissing(
                "first_login_at",
                "ALTER TABLE sys_user ADD COLUMN first_login_at DATETIME NULL COMMENT '首次登录时间'");
        addColumnIfMissing(
                "last_login_at",
                "ALTER TABLE sys_user ADD COLUMN last_login_at DATETIME NULL COMMENT '最近登录时间'");
        addIndexIfMissing();
    }

    private void addColumnIfMissing(String columnName, String alterSql) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = ?",
                Integer.class,
                columnName);
        if (c != null && c == 0) {
            log.info("Adding missing column sys_user.{}", columnName);
            jdbcTemplate.execute(alterSql);
        }
    }

    private void addIndexIfMissing() {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = 'sys_user' "
                        + "AND index_name = 'idx_last_login_at'",
                Integer.class);
        if (c != null && c == 0) {
            log.info("Adding index idx_last_login_at on sys_user(last_login_at)");
            jdbcTemplate.execute("CREATE INDEX idx_last_login_at ON sys_user (last_login_at)");
        }
    }
}
