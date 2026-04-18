-- ============================================================
-- 居服通 (Home Care Connect) 数据库初始化脚本
-- 数据库: homecare
-- 编码: utf8mb4
-- ============================================================

-- 如果数据库不存在则创建
CREATE DATABASE IF NOT EXISTS homecare DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE homecare;

-- ============================================================
-- 1. 门店表 store
-- ============================================================
CREATE TABLE IF NOT EXISTS store (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL COMMENT '门店名称',
    address         VARCHAR(500) NOT NULL COMMENT '门店地址',
    phone           VARCHAR(20) COMMENT '门店电话',
    manager_id      BIGINT COMMENT '店长用户ID',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_manager (manager_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店表';

-- ============================================================
-- 2. 用户表 sys_user
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名/账号',
    password        VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name       VARCHAR(100) COMMENT '真实姓名',
    gender          VARCHAR(10) COMMENT '性别：male/female',
    phone           VARCHAR(20) UNIQUE COMMENT '手机号',
    email           VARCHAR(100) UNIQUE COMMENT '邮箱',
    role            ENUM('admin', 'store_manager', 'service_staff', 'distributor', 'user', 'supplier', 'tenant') NOT NULL DEFAULT 'user' COMMENT '角色',
    avatar          VARCHAR(255) COMMENT '头像URL',
    store_id        BIGINT COMMENT '所属门店（店长/服务人员）',
    status          VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/inactive/banned',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_role (role),
    INDEX idx_store (store_id),
    INDEX idx_phone (phone),
    INDEX idx_email (email),
    FOREIGN KEY (store_id) REFERENCES store(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 3. 房源表 property
-- ============================================================
CREATE TABLE IF NOT EXISTS property (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id        BIGINT NOT NULL COMMENT '所属门店ID',
    title           VARCHAR(200) NOT NULL COMMENT '房源标题',
    type            ENUM('apartment', 'studio', 'suite', 'villa') NOT NULL COMMENT '房型：公寓/单间/套间/别墅',
    rent_price      DECIMAL(10,2) NOT NULL COMMENT '月租价格',
    area            DECIMAL(8,2) NOT NULL COMMENT '面积（平方米）',
    floor           INT COMMENT '楼层',
    address         VARCHAR(500) NOT NULL COMMENT '详细地址',
    description     TEXT COMMENT '详细描述',
    facilities      JSON COMMENT '配套设施：["空调","热水器","洗衣机"]',
    status          ENUM('pending', 'rejected', 'vacant', 'occupied', 'reserved') DEFAULT 'pending' COMMENT 'pending=待上架审核 rejected=已驳回 vacant=已上架空置 occupied=已租 reserved=下架/预定',
    manager_id      BIGINT NOT NULL COMMENT '负责店长ID',
    view_count      INT DEFAULT 0 COMMENT '浏览次数',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_store (store_id),
    INDEX idx_price (rent_price),
    INDEX idx_manager (manager_id),
    INDEX idx_type (type),
    FOREIGN KEY (store_id) REFERENCES store(id) ON DELETE CASCADE,
    FOREIGN KEY (manager_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房源表';

-- ============================================================
-- 4. 房源图片表 property_image
-- ============================================================
CREATE TABLE IF NOT EXISTS property_image (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    property_id BIGINT NOT NULL COMMENT '房源ID',
    url         VARCHAR(500) NOT NULL COMMENT '图片URL（MinIO）',
    type        ENUM('cover', 'detail', 'floor_plan') NOT NULL COMMENT '图片类型：封面/详情/户型图',
    sort_order  INT DEFAULT 0 COMMENT '排序',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_property (property_id),
    FOREIGN KEY (property_id) REFERENCES property(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房源图片表';

-- ============================================================
-- 5. 分销人员表 distributor
-- ============================================================
CREATE TABLE IF NOT EXISTS distributor (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL UNIQUE COMMENT '关联用户ID',
    bind_code       VARCHAR(20) NOT NULL UNIQUE COMMENT '推广绑定码',
    commission_rate DECIMAL(5,4) DEFAULT 0.0100 COMMENT '佣金比例，默认1%',
    total_deals     INT DEFAULT 0 COMMENT '总成交单数',
    total_commission DECIMAL(12,2) DEFAULT 0.00 COMMENT '累计佣金',
    status          TINYINT DEFAULT 1 COMMENT '状态：1=正常 0=禁用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_bind_code (bind_code),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分销人员表';

-- ============================================================
-- 6. 看房记录表 property_viewing
-- ============================================================
CREATE TABLE IF NOT EXISTS property_viewing (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    property_id     BIGINT NOT NULL COMMENT '房源ID',
    user_id         BIGINT NOT NULL COMMENT '预约用户ID',
    distributor_id  BIGINT COMMENT '陪同分销人员ID',
    appointment_time DATETIME NOT NULL COMMENT '预约看房时间',
    status          ENUM('pending', 'completed', 'cancelled') DEFAULT 'pending' COMMENT '状态：待看房/已完成/已取消',
    remark          VARCHAR(500) COMMENT '备注',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_property (property_id),
    INDEX idx_user (user_id),
    INDEX idx_distributor (distributor_id),
    INDEX idx_status (status),
    FOREIGN KEY (property_id) REFERENCES property(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (distributor_id) REFERENCES distributor(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='看房记录表';

-- ============================================================
-- 7. 佣金记录表 commission
-- ============================================================
CREATE TABLE IF NOT EXISTS commission (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    property_id     BIGINT NOT NULL COMMENT '房源ID',
    distributor_id  BIGINT NOT NULL COMMENT '分销人员ID',
    deal_price      DECIMAL(12,2) NOT NULL COMMENT '成交租金',
    commission_rate DECIMAL(5,4) NOT NULL COMMENT '佣金比例',
    commission_amount DECIMAL(12,2) NOT NULL COMMENT '佣金金额',
    status          ENUM('pending', 'settled', 'cancelled') DEFAULT 'pending' COMMENT '状态：待结算/已结算/已取消',
    settled_at      DATETIME COMMENT '结算时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_distributor (distributor_id),
    INDEX idx_property (property_id),
    INDEX idx_status (status),
    FOREIGN KEY (property_id) REFERENCES property(id) ON DELETE CASCADE,
    FOREIGN KEY (distributor_id) REFERENCES distributor(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='佣金记录表';

-- ============================================================
-- 8. 服务类型表 service_type
-- ============================================================
CREATE TABLE IF NOT EXISTS service_type (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '服务名称：保洁/维修/疏通等',
    category    ENUM('cleaning', 'repair', 'other') NOT NULL COMMENT '分类：保洁/维修/其他',
    description TEXT COMMENT '服务描述',
    base_price  DECIMAL(10,2) NOT NULL COMMENT '基础价格',
    unit        VARCHAR(20) DEFAULT '次' COMMENT '计费单位',
    icon        VARCHAR(100) COMMENT '图标URL',
    status      TINYINT DEFAULT 1 COMMENT '状态：2=待上架审核 1=已上架 0=禁用',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务类型表';

-- ============================================================
-- 9. 服务人员表 service_staff
-- ============================================================
CREATE TABLE IF NOT EXISTS service_staff (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL UNIQUE COMMENT '关联用户ID',
    store_id    BIGINT NOT NULL COMMENT '所属门店ID',
    skills      JSON COMMENT '技能标签：["保洁","换锁"]',
    status      ENUM('available', 'busy', 'off') DEFAULT 'available' COMMENT '状态：可用/忙碌/离线',
    rating      DECIMAL(3,2) DEFAULT 5.00 COMMENT '评分',
    total_orders INT DEFAULT 0 COMMENT '累计完成订单数',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_store (store_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (store_id) REFERENCES store(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务人员表';

-- ============================================================
-- 10. 服务订单表 service_order
-- ============================================================
CREATE TABLE IF NOT EXISTS service_order (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(50) NOT NULL UNIQUE COMMENT '订单编号：SVC+yyyyMMdd+6位序号',
    user_id         BIGINT NOT NULL COMMENT '下单用户ID',
    staff_id        BIGINT COMMENT '指派服务人员ID',
    service_type_id BIGINT NOT NULL COMMENT '服务类型ID',
    address         VARCHAR(500) NOT NULL COMMENT '服务地址',
    appointment_time DATETIME NOT NULL COMMENT '预约时间',
    description     TEXT COMMENT '服务描述/备注',
    price           DECIMAL(10,2) NOT NULL COMMENT '订单价格',
    status          ENUM('pending', 'assigned', 'accepted', 'in_progress', 'completed', 'cancelled') DEFAULT 'pending' COMMENT '订单状态',
    completed_at    DATETIME COMMENT '完成时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_user (user_id),
    INDEX idx_staff (staff_id),
    INDEX idx_order_no (order_no),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (staff_id) REFERENCES service_staff(id) ON DELETE SET NULL,
    FOREIGN KEY (service_type_id) REFERENCES service_type(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务订单表';

-- ============================================================
-- 11. 评价表 review
-- ============================================================
CREATE TABLE IF NOT EXISTS review (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL COMMENT '评价用户ID',
    target_type ENUM('property', 'service', 'staff') NOT NULL COMMENT '评价对象类型',
    target_id   BIGINT NOT NULL COMMENT '评价对象ID',
    rating      TINYINT NOT NULL COMMENT '评分1-5',
    content     VARCHAR(500) COMMENT '评价内容',
    images      JSON COMMENT '评价图片列表',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    INDEX idx_target (target_type, target_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';

-- ============================================================
-- 12. 采购商品表 procurement_product
-- ============================================================
CREATE TABLE IF NOT EXISTS procurement_product (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT NOT NULL COMMENT '供应商用户ID',
    name        VARCHAR(200) NOT NULL COMMENT '商品名称',
    category    VARCHAR(50) NOT NULL COMMENT '分类：家具/家电/日用品等',
    price       DECIMAL(10,2) NOT NULL COMMENT '价格',
    description TEXT COMMENT '商品描述',
    images      JSON COMMENT '商品图片列表（MinIO URL）',
    stock       INT DEFAULT 0 COMMENT '库存数量',
    unit        VARCHAR(20) DEFAULT '件' COMMENT '计价单位',
    sales_count INT DEFAULT 0 COMMENT '累计销量',
    product_tag VARCHAR(20) NULL COMMENT '角标：热卖/新品/特惠',
    status      TINYINT DEFAULT 2 COMMENT '状态：2=待上架审核 1=已上架 0=下架',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category),
    INDEX idx_supplier (supplier_id),
    INDEX idx_status (status),
    FOREIGN KEY (supplier_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购商品表';

-- ============================================================
-- 13. 二手物品表 secondhand_item
-- ============================================================
CREATE TABLE IF NOT EXISTS secondhand_item (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    publisher_id BIGINT NOT NULL COMMENT '发布用户ID',
    title       VARCHAR(200) NOT NULL COMMENT '物品标题',
    category    VARCHAR(50) NOT NULL COMMENT '分类',
    price       DECIMAL(10,2) NOT NULL COMMENT '价格',
    original_price DECIMAL(10,2) NULL COMMENT '原价（展示划线价）',
    condition   ENUM('like_new', 'good', 'fair') NOT NULL COMMENT '新旧程度：几乎全新/良好/一般',
    description TEXT COMMENT '物品描述',
    images      JSON COMMENT '图片列表（MinIO URL）',
    contact     VARCHAR(100) COMMENT '联系方式',
    location    VARCHAR(200) NULL COMMENT '自提/交易地点',
    status      TINYINT DEFAULT 2 COMMENT '状态：2=待上架审核 1=已上架 0=下架',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_publisher (publisher_id),
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='二手物品表';

-- ============================================================
-- 初始化测试数据
-- ============================================================

-- 插入测试门店
INSERT INTO store (name, address, phone) VALUES
('居服通旗舰店', '北京市朝阳区建国路88号', '010-12345678'),
('居服通二分店', '上海市浦东新区世纪大道100号', '021-87654321');

-- 插入测试用户（密码均为 123456，BCrypt加密后的值）
-- 123456 的 BCrypt 加密结果（用固定salt生成，方便测试）
INSERT INTO sys_user (username, password, phone, role, status) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.R8w.B1kcXBN7v3CqUy', '13800000001', 'admin', 1),
('manager1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.R8w.B1kcXBN7v3CqUy', '13800000002', 'store_manager', 1),
('manager2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.R8w.B1kcXBN7v3CqUy', '13800000003', 'store_manager', 1),
('staff1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.R8w.B1kcXBN7v3CqUy', '13800000004', 'service_staff', 1),
('staff2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.R8w.B1kcXBN7v3CqUy', '13800000005', 'service_staff', 1),
('user1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.R8w.B1kcXBN7v3CqUy', '13800000006', 'user', 1),
('distributor1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.R8w.B1kcXBN7v3CqUy', '13800000007', 'distributor', 1),
('supplier1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.R8w.B1kcXBN7v3CqUy', '13800000008', 'supplier', 1);

UPDATE sys_user SET real_name = '李女士' WHERE username = 'user1';
UPDATE sys_user SET real_name = '张先生' WHERE username = 'distributor1';

-- 更新门店的店长ID
UPDATE store SET manager_id = 2 WHERE id = 1;
UPDATE store SET manager_id = 3 WHERE id = 2;

-- 插入服务人员关联
INSERT INTO service_staff (user_id, store_id, skills, status) VALUES
(4, 1, '["保洁","开荒保洁","日常清洁"]', 'available'),
(5, 2, '["家电维修","水管维修","疏通"]', 'available');

-- 插入服务类型
INSERT INTO service_type (name, category, description, base_price, unit) VALUES
('日常保洁', 'cleaning', '专业保洁人员上门进行日常清洁服务，包括地面、桌面、卫生间清洁等', 150.00, '次'),
('开荒保洁', 'cleaning', '新装修房屋或长期空置房屋的全面清洁，包括除灰、擦窗、地面清洁等', 300.00, '次'),
('家电维修', 'repair', '空调、冰箱、洗衣机等家用电器的故障维修', 100.00, '次'),
('水管维修', 'repair', '水管漏水、水龙头更换、下水道疏通等', 80.00, '次'),
('开锁换锁', 'other', '专业开锁、换锁服务，包括门锁、抽屉锁等', 120.00, '次');

-- 插入房源测试数据
INSERT INTO property (store_id, title, type, rent_price, area, floor, address, description, facilities, status, manager_id) VALUES
(1, '地铁口精装公寓', 'apartment', 2500.00, 45.50, 5, '朝阳区建国路88号', '精装修，家电齐全，临近地铁，交通便利', '["空调","热水器","洗衣机","冰箱","宽带"]', 'vacant', 2),
(1, '阳光充足主卧', 'suite', 1950.00, 28.00, 8, '朝阳区建国路90号', '南向主卧采光好，有独立卫生间，家具配套齐全', '["空调","热水器","床","衣柜","书桌"]', 'vacant', 2),
(1, '温馨单间出租', 'studio', 1200.00, 18.00, 3, '朝阳区建国路92号', '小户型单间，适合单身人士或学生，性价比高', '["空调","热水器","床","衣柜"]', 'vacant', 2),
(1, '豪华两室一厅', 'apartment', 4500.00, 80.00, 12, '朝阳区建国路88号', '两室一厅，装修豪华，家电全配，可拎包入住', '["空调","热水器","洗衣机","冰箱","电视","沙发"]', 'vacant', 2),
(2, '浦东精装一居室', 'apartment', 3200.00, 55.00, 6, '浦东新区世纪大道100号', '浦东核心地段，地铁上盖，精装交付', '["空调","热水器","洗衣机","冰箱","宽带","智能门锁"]', 'vacant', 3);

-- 插入房源图片
INSERT INTO property_image (property_id, url, type, sort_order) VALUES
(1, 'https://minio:9000/homecare-property/1/cover.jpg', 'cover', 0),
(1, 'https://minio:9000/homecare-property/1/detail1.jpg', 'detail', 1),
(1, 'https://minio:9000/homecare-property/1/floor_plan.jpg', 'floor_plan', 2),
(2, 'https://minio:9000/homecare-property/2/cover.jpg', 'cover', 0),
(3, 'https://minio:9000/homecare-property/3/cover.jpg', 'cover', 0),
(4, 'https://minio:9000/homecare-property/4/cover.jpg', 'cover', 0),
(5, 'https://minio:9000/homecare-property/5/cover.jpg', 'cover', 0);

-- 插入分销人员
INSERT INTO distributor (user_id, bind_code, commission_rate, total_deals, total_commission) VALUES
(7, 'DIST20260001', 0.0100, 3, 750.00);

-- 插入采购商品测试数据（与本地商城原型分类、销量展示对齐）
INSERT INTO procurement_product (supplier_id, name, category, price, description, stock, status, unit, sales_count, product_tag) VALUES
(8, '得力办公文具套装（含笔筒、回形针、便签）', '办公用品', 68.00, '社区团购优选，含笔筒、回形针、便签', 200, 1, '/套', 286, '热卖'),
(8, '家用清洁工具套装（拖把、抹布、洗洁精）', '清洁用品', 45.00, '拖把、抹布、洗洁精组合', 120, 1, '/套', 158, NULL),
(8, '家用维修工具箱（螺丝刀、锤子、扳手套装）', '维修工具', 128.00, '螺丝刀、锤子、扳手套装', 80, 1, '/套', 92, '新品'),
(8, '办公室绿植盆栽组合（绿萝、吊兰、文竹）', '绿植盆栽', 88.00, '绿萝、吊兰、文竹组合', 40, 1, '/组', 64, NULL),
(8, '社区安防摄像头套装（4台装，含录像存储）', '安防设备', 680.00, '4台装，含录像存储方案', 25, 1, '/套', 38, '特惠'),
(8, '84消毒液 / 75%酒精消毒液套装（大桶装）', '清洁用品', 35.00, '大桶装，满足日常消杀', 300, 1, '/桶', 312, NULL),
(8, '折叠会议桌椅套装（2桌8椅，适合社区活动）', '生活物资', 880.00, '2桌8椅，适合社区活动', 15, 1, '/套', 25, NULL),
(8, '应急物资储备包（包含手电筒、电池、蜡烛等）', '生活物资', 55.00, '手电筒、电池、蜡烛等', 100, 1, '/包', 120, NULL);

-- 插入二手物品测试数据（分类与原型 Tab 对齐）
INSERT INTO secondhand_item (publisher_id, title, category, price, original_price, condition, description, contact, location, status) VALUES
(6, '实木双人床（1.8米宽，配床垫）搬家急售', '家具家居', 680.00, 1800.00, 'good', '搬家急售，床体稳固', '13800000006', '朝阳区·望京', 1),
(6, '小米55寸4K智能电视，使用一年，屏幕完美', '数码电器', 1200.00, 2500.00, 'like_new', '使用一年，屏幕完美', '13800000006', '海淀区·五道口', 1),
(7, '品牌女装连衣裙，M码，仅试穿吊牌还在', '服饰箱包', 128.00, 380.00, 'like_new', '仅试穿吊牌还在', '13800000007', '东城区·东直门', 1),
(6, '儿童绘本全套30本，适合3-6岁，低价出', '书籍文具', 68.00, 180.00, 'fair', '全套30本', '13800000006', '丰台区·宋家庄', 1),
(7, '北欧风布艺沙发，三人位，颜色百搭', '家具家居', 980.00, 2200.00, 'good', '三人位布艺沙发', '13800000007', '朝阳区·三元桥', 1),
(6, '捷安特山地自行车，适合通勤，偶尔使用', '运动户外', 450.00, 800.00, 'like_new', '偶尔使用车况好', '13800000006', '海淀区·中关村', 1),
(7, '大型绿植琴叶榕，高1.2米，花盆一并送', '绿植宠物', 88.00, 200.00, 'good', '高1.2米含花盆', '13800000007', '西城区·金融街', 1),
(6, 'Switch游戏机+健身环大冒险，全套配件齐全', '数码电器', 1680.00, 3000.00, 'like_new', '全套配件齐全', '13800000006', '朝阳区·国贸', 1);

-- 确认数据插入成功
SELECT '数据初始化完成' AS status;
