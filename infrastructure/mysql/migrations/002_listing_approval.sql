-- 上架审批：房源增加待审/驳回；其余表沿用 TINYINT 扩展取值（2=待审）
-- 在已有库上执行一次（Docker: mysql -uroot -p... homecare < 002_listing_approval.sql）

ALTER TABLE property
    MODIFY COLUMN status ENUM('pending', 'rejected', 'vacant', 'occupied', 'reserved')
    NOT NULL DEFAULT 'vacant'
    COMMENT 'pending=待上架审核 rejected=已驳回 vacant=已上架(空置) occupied=已租 reserved=下架/预定';

-- 采购商品、二手：1=已上架 0=下架 2=待审核（应用层约定）
-- service_type：1=已上架 0=禁用 2=待上架审核
