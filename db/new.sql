-- MySQL 8.0 schema for ZhiGuang authentication service
create database if not exists  zg_auth ;
use zg_auth;

-- 更新 users 表字段注释
ALTER TABLE users MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID';
ALTER TABLE users MODIFY COLUMN phone VARCHAR(32) NULL COMMENT '手机号';
ALTER TABLE users MODIFY COLUMN email VARCHAR(128) NULL COMMENT '邮箱';
ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(128) NULL COMMENT '密码哈希值';
ALTER TABLE users MODIFY COLUMN nickname VARCHAR(64) NOT NULL COMMENT '昵称';
ALTER TABLE users MODIFY COLUMN avatar TEXT NULL COMMENT '头像URL';
ALTER TABLE users MODIFY COLUMN bio VARCHAR(512) NULL COMMENT '个人简介';
ALTER TABLE users MODIFY COLUMN zg_id VARCHAR(64) NULL COMMENT '知光ID';
ALTER TABLE users MODIFY COLUMN gender VARCHAR(16) NULL COMMENT '性别';
ALTER TABLE users MODIFY COLUMN birthday DATE NULL COMMENT '生日';
ALTER TABLE users MODIFY COLUMN school VARCHAR(128) NULL COMMENT '学校';
ALTER TABLE users MODIFY COLUMN tags_json JSON NULL COMMENT '标签JSON数组';
ALTER TABLE users MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE users MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 更新 login_logs 表字段注释
ALTER TABLE login_logs MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '登录日志ID';
ALTER TABLE login_logs MODIFY COLUMN user_id BIGINT UNSIGNED NULL COMMENT '用户ID';
ALTER TABLE login_logs MODIFY COLUMN identifier VARCHAR(128) NOT NULL COMMENT '登录标识（手机号/邮箱）';
ALTER TABLE login_logs MODIFY COLUMN channel VARCHAR(32) NOT NULL COMMENT '登录渠道';
ALTER TABLE login_logs MODIFY COLUMN ip VARCHAR(45) NULL COMMENT 'IP地址';
ALTER TABLE login_logs MODIFY COLUMN user_agent VARCHAR(512) NULL COMMENT '用户代理字符串';
ALTER TABLE login_logs MODIFY COLUMN status VARCHAR(16) NOT NULL COMMENT '登录状态';
ALTER TABLE login_logs MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 更新 know_posts 表字段注释
ALTER TABLE know_posts MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '知文ID（雪花算法生成）';
ALTER TABLE know_posts MODIFY COLUMN tag_id BIGINT UNSIGNED NULL COMMENT '主分类/内容分类ID';
ALTER TABLE know_posts MODIFY COLUMN tags JSON NULL COMMENT '标签名数组，例如 ["java","编程"]';
ALTER TABLE know_posts MODIFY COLUMN title VARCHAR(256) NULL COMMENT '标题';
ALTER TABLE know_posts MODIFY COLUMN description VARCHAR(50) NULL COMMENT '摘要/描述，最多50字';
ALTER TABLE know_posts MODIFY COLUMN content_url TEXT NULL COMMENT '正文存储于OSS的访问URL或签名URL';
ALTER TABLE know_posts MODIFY COLUMN content_object_key VARCHAR(512) NULL COMMENT 'OSS对象Key';
ALTER TABLE know_posts MODIFY COLUMN content_etag VARCHAR(128) NULL COMMENT 'OSS ETag（用于校验）';
ALTER TABLE know_posts MODIFY COLUMN content_size BIGINT UNSIGNED NULL COMMENT '正文字节大小';
ALTER TABLE know_posts MODIFY COLUMN content_sha256 CHAR(64) NULL COMMENT '正文SHA-256哈希（hex）';
ALTER TABLE know_posts MODIFY COLUMN creator_id BIGINT UNSIGNED NOT NULL COMMENT '创作者ID';
ALTER TABLE know_posts MODIFY COLUMN is_top TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶';
ALTER TABLE know_posts MODIFY COLUMN type VARCHAR(32) NOT NULL DEFAULT 'image_text' COMMENT '内容类型';
ALTER TABLE know_posts MODIFY COLUMN visible VARCHAR(32) NOT NULL DEFAULT 'public' COMMENT '可见性';
ALTER TABLE know_posts MODIFY COLUMN img_urls JSON NULL COMMENT '图片URL数组或对象数组';
ALTER TABLE know_posts MODIFY COLUMN video_url TEXT NULL COMMENT '视频URL（一期不使用）';
ALTER TABLE know_posts MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT '状态（draft:草稿/pending:审核中/published:已发布）';
ALTER TABLE know_posts MODIFY COLUMN create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE know_posts MODIFY COLUMN update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE know_posts MODIFY COLUMN publish_time TIMESTAMP NULL DEFAULT NULL COMMENT '发布时间';

-- 更新 outbox 表字段注释
ALTER TABLE outbox MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT 'Outbox事件ID';
ALTER TABLE outbox MODIFY COLUMN aggregate_type VARCHAR(64) NOT NULL COMMENT '聚合根类型';
ALTER TABLE outbox MODIFY COLUMN aggregate_id BIGINT UNSIGNED NULL COMMENT '聚合根ID';
ALTER TABLE outbox MODIFY COLUMN type VARCHAR(64) NOT NULL COMMENT '事件类型';
ALTER TABLE outbox MODIFY COLUMN payload JSON NOT NULL COMMENT '事件负载数据';
ALTER TABLE outbox MODIFY COLUMN created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间';

-- 更新 following 表字段注释
ALTER TABLE following MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '关注关系ID';
ALTER TABLE following MODIFY COLUMN from_user_id BIGINT UNSIGNED NOT NULL COMMENT '关注者用户ID';
ALTER TABLE following MODIFY COLUMN to_user_id BIGINT UNSIGNED NOT NULL COMMENT '被关注者用户ID';
ALTER TABLE following MODIFY COLUMN rel_status TINYINT NOT NULL DEFAULT 1 COMMENT '关系状态（1:正常 0:取消）';
ALTER TABLE following MODIFY COLUMN created_at DATETIME(3) NOT NULL COMMENT '创建时间';
ALTER TABLE following MODIFY COLUMN updated_at DATETIME(3) NOT NULL COMMENT '更新时间';

-- 更新 follower 表字段注释
ALTER TABLE follower MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '粉丝关系ID';
ALTER TABLE follower MODIFY COLUMN to_user_id BIGINT UNSIGNED NOT NULL COMMENT '被关注者用户ID';
ALTER TABLE follower MODIFY COLUMN from_user_id BIGINT UNSIGNED NOT NULL COMMENT '关注者用户ID';
ALTER TABLE follower MODIFY COLUMN rel_status TINYINT NOT NULL DEFAULT 1 COMMENT '关系状态（1:正常 0:取消）';
ALTER TABLE follower MODIFY COLUMN created_at DATETIME(3) NOT NULL COMMENT '创建时间';
ALTER TABLE follower MODIFY COLUMN updated_at DATETIME(3) NOT NULL COMMENT '更新时间';

CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                                     phone VARCHAR(32) NULL COMMENT '手机号',
                                     email VARCHAR(128) NULL COMMENT '邮箱',
                                     password_hash VARCHAR(128) NULL COMMENT '密码哈希值',
                                     nickname VARCHAR(64) NOT NULL COMMENT '昵称',
                                     avatar TEXT NULL COMMENT '头像URL',
                                     bio VARCHAR(512) NULL COMMENT '个人简介',
                                     zg_id VARCHAR(64) NULL COMMENT '知光ID',
                                     gender VARCHAR(16) NULL COMMENT '性别',
                                     birthday DATE NULL COMMENT '生日',
                                     school VARCHAR(128) NULL COMMENT '学校',
                                     tags_json JSON NULL COMMENT '标签JSON数组',
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     PRIMARY KEY (id),
                                     UNIQUE KEY uk_users_phone (phone),
                                     UNIQUE KEY uk_users_email (email),
                                     UNIQUE KEY uk_users_zg_id (zg_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS login_logs (
                                          id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '登录日志ID',
                                          user_id BIGINT UNSIGNED NULL COMMENT '用户ID',
                                          identifier VARCHAR(128) NOT NULL COMMENT '登录标识（手机号/邮箱）',
                                          channel VARCHAR(32) NOT NULL COMMENT '登录渠道',
                                          ip VARCHAR(45) NULL COMMENT 'IP地址',
                                          user_agent VARCHAR(512) NULL COMMENT '用户代理字符串',
                                          status VARCHAR(16) NOT NULL COMMENT '登录状态',
                                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           PRIMARY KEY (id),
                                           KEY ix_login_logs_user_created_at (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- 知文（KnowPost）主表
-- 说明：
-- - id 使用雪花算法在业务层生成（非自增）；
-- - tags、img_urls 使用 JSON 存储，兼容多标签/多图片；
-- - content 存储在 OSS，仅记录 URL 与校验信息；
-- - 一期类型仅 image_text，可扩展；
-- - 状态包含草稿/审核中/已发布，预留 rejected/deleted；
CREATE TABLE IF NOT EXISTS know_posts (
                                          id BIGINT UNSIGNED NOT NULL COMMENT '知文ID（雪花算法生成）',
                                          tag_id BIGINT UNSIGNED NULL COMMENT '主分类/内容分类ID',
                                          tags JSON NULL COMMENT '标签名数组，例如 ["java","编程"]',
                                          title VARCHAR(256) NULL COMMENT '标题',
                                          description VARCHAR(50) NULL COMMENT '摘要/描述，最多50字',
                                          content_url TEXT NULL COMMENT '正文存储于OSS的访问URL或签名URL',
                                          content_object_key VARCHAR(512) NULL COMMENT 'OSS对象Key',
                                          content_etag VARCHAR(128) NULL COMMENT 'OSS ETag（用于校验）',
                                          content_size BIGINT UNSIGNED NULL COMMENT '正文字节大小',
                                          content_sha256 CHAR(64) NULL COMMENT '正文SHA-256哈希（hex）',
                                          creator_id BIGINT UNSIGNED NOT NULL COMMENT '创作者ID',
                                          is_top TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
                                          type VARCHAR(32) NOT NULL DEFAULT 'image_text' COMMENT '内容类型',
                                          visible VARCHAR(32) NOT NULL DEFAULT 'public' COMMENT '可见性',
                                          img_urls JSON NULL COMMENT '图片URL数组或对象数组',
                                          video_url TEXT NULL COMMENT '视频URL（一期不使用）',
                                          status VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT '状态（draft:草稿/pending:审核中/published:已发布）',
                                          create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                          update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                          publish_time TIMESTAMP NULL DEFAULT NULL COMMENT '发布时间',
                                          PRIMARY KEY (id),
                                          KEY ix_know_posts_creator_ct (creator_id, create_time),
                                          KEY ix_know_posts_status_ct (status, create_time),
                                          KEY ix_know_posts_tag_ct (tag_id, create_time),
                                          KEY ix_know_posts_top_ct (is_top, create_time),
                                          KEY ix_know_posts_creator_status_pub (creator_id, status, publish_time),
                                           CONSTRAINT fk_know_posts_creator FOREIGN KEY (creator_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知文（知识文章）主表';

CREATE TABLE IF NOT EXISTS outbox (
                                      id BIGINT UNSIGNED NOT NULL COMMENT 'Outbox事件ID',
                                      aggregate_type VARCHAR(64) NOT NULL COMMENT '聚合根类型',
                                      aggregate_id BIGINT UNSIGNED NULL COMMENT '聚合根ID',
                                      type VARCHAR(64) NOT NULL COMMENT '事件类型',
                                      payload JSON NOT NULL COMMENT '事件负载数据',
                                      created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                                      PRIMARY KEY (id),
                                       KEY ix_outbox_agg (aggregate_type, aggregate_id),
                                       KEY ix_outbox_ct (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Outbox事件表（事务性事件存储）';

CREATE TABLE IF NOT EXISTS following (
                                         id BIGINT UNSIGNED NOT NULL COMMENT '关注关系ID',
                                         from_user_id BIGINT UNSIGNED NOT NULL COMMENT '关注者用户ID',
                                         to_user_id BIGINT UNSIGNED NOT NULL COMMENT '被关注者用户ID',
                                         rel_status TINYINT NOT NULL DEFAULT 1 COMMENT '关系状态（1:正常 0:取消）',
                                         created_at DATETIME(3) NOT NULL COMMENT '创建时间',
                                         updated_at DATETIME(3) NOT NULL COMMENT '更新时间',
                                         PRIMARY KEY (id),
                                         UNIQUE KEY uk_from_to (from_user_id, to_user_id),
                                         KEY idx_from_created (from_user_id, created_at, to_user_id, rel_status),
                                         KEY idx_to (to_user_id, from_user_id, rel_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注关系表（我关注的人）';
                                        id BIGINT UNSIGNED NOT NULL COMMENT '粉丝关系ID',
                                        to_user_id BIGINT UNSIGNED NOT NULL COMMENT '被关注者用户ID',
                                        from_user_id BIGINT UNSIGNED NOT NULL COMMENT '关注者用户ID',
                                        rel_status TINYINT NOT NULL DEFAULT 1 COMMENT '关系状态（1:正常 0:取消）',
                                        created_at DATETIME(3) NOT NULL COMMENT '创建时间',
                                        updated_at DATETIME(3) NOT NULL COMMENT '更新时间',
                                        PRIMARY KEY (id),
                                        UNIQUE KEY uk_to_from (to_user_id, from_user_id),
                                         KEY idx_to_created (to_user_id, created_at, from_user_id, rel_status),
                                         KEY idx_from (from_user_id, to_user_id, rel_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='粉丝关系表（关注我的人）';

-- 为 know_posts 表添加软删除字段（配合 @TableLogic 使用）
ALTER TABLE know_posts ADD COLUMN is_delete TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记（0:正常 1:已删除）' AFTER publish_time;

