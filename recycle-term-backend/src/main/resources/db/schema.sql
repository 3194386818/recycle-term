CREATE DATABASE IF NOT EXISTS recycle_term DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE recycle_term;

-- 回收任务
CREATE TABLE IF NOT EXISTS recycle_task (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone_number    VARCHAR(20)       COMMENT '用户号码',
    product_id      VARCHAR(20)       COMMENT '产品号（020开头）',
    user_name       VARCHAR(50)       COMMENT '用户名称',
    user_address    VARCHAR(500)      COMMENT '用户地址',
    area            VARCHAR(50)       COMMENT '区域',
    engineer_name   VARCHAR(50)       COMMENT '工程师姓名',
    engineer_phone  VARCHAR(20)       COMMENT '工程师手机号',
    detail_desc     TEXT              COMMENT '详情说明',
    terminals       TEXT              COMMENT '应回收终端串码',
    expected_count  INT DEFAULT 0     COMMENT '应回终端数量',
    fttr_count      INT DEFAULT 0     COMMENT 'FTTR主光猫数量',
    access_room     VARCHAR(100)      COMMENT '接入间名称',
    category        VARCHAR(50)       COMMENT '分类',
    dev_dept        VARCHAR(100)      COMMENT '发展部门',
    dev_person      VARCHAR(50)       COMMENT '发展员工',
    need_visit      TINYINT DEFAULT 0 COMMENT '是否上门',
    status          TINYINT DEFAULT 0 COMMENT '状态：0待回收 1已上门 2已完成 3已失败 4审核成功 5审核失败 6已归档',
    completed       TINYINT DEFAULT 0 COMMENT '是否完成',
    completed_at    DATETIME          COMMENT '完成时间',
    fail_reason     VARCHAR(500)      COMMENT '失败原因',
    review_remark   VARCHAR(500)      COMMENT '审核备注',
    reviewer_id     BIGINT            COMMENT '审核管理员ID',
    remark          VARCHAR(500)      COMMENT '备注',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_completed (completed),
    INDEX idx_completed_at (completed_at)
) COMMENT '终端回收任务表';

-- 实际回收串码记录
CREATE TABLE IF NOT EXISTS terminal_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT NOT NULL   COMMENT '关联任务ID',
    serial_number   VARCHAR(100)      COMMENT '终端串码',
    scanned_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '扫描时间',
    INDEX idx_task_id (task_id),
    INDEX idx_scanned_at (scanned_at),
    UNIQUE KEY uk_task_serial (task_id, serial_number)
) COMMENT '终端串码回收记录表';
