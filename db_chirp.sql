/*
 Navicat Premium Data Transfer

 Source Server         : mysql
 Source Server Type    : MySQL
 Source Server Version : 80012
 Source Host           : localhost:3306
 Source Schema         : db_chirp

 Target Server Type    : MySQL
 Target Server Version : 80012
 File Encoding         : 65001

 Date: 29/10/2023 23:39:11
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for role_permission
-- ----------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission`
(
    `role_id`       int(4) NULL DEFAULT NULL,
    `permission_id` int(4) NULL DEFAULT NULL
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_unicode_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for site_message_of_user
-- ----------------------------
DROP TABLE IF EXISTS `site_message_of_user`;
CREATE TABLE `site_message_of_user`
(
    `user_id`         bigint(20)  NOT NULL,
    `last_read_time`  datetime(0) NULL DEFAULT NULL,
    `final_read_time` datetime(0) NULL DEFAULT NULL,
    UNIQUE INDEX `unique_user_id` (`user_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tb_chirper
-- ----------------------------
DROP TABLE IF EXISTS `tb_chirper`;
CREATE TABLE `tb_chirper`
(
    `id`                     bigint(64)                                                    NOT NULL,
    `author_id`              bigint(20)                                                    NULL DEFAULT NULL,
    `conversation_id`        bigint(64)                                                    NULL DEFAULT NULL COMMENT '对话由原创chirper及其所有回复组成，该值为原创chirper的id',
    `in_reply_to_chirper_id` bigint(64)                                                    NULL DEFAULT NULL COMMENT '父推文标识',
    `in_reply_to_user_id`    bigint(20)                                                    NULL DEFAULT NULL,
    `create_time`            datetime(0)                                                   NULL DEFAULT NULL,
    `text`                   varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `type`                   varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NULL DEFAULT NULL COMMENT '原创、回复、转发、引用',
    `referenced_chirper_id`  bigint(64)                                                    NULL DEFAULT NULL COMMENT '引用或者转发时不为空',
    `media_keys`             json                                                          NULL,
    `view_count`             int(11)                                                       NULL DEFAULT NULL,
    `reply_count`            int(11)                                                       NULL DEFAULT NULL,
    `like_count`             int(11)                                                       NULL DEFAULT NULL,
    `quote_count`            int(11)                                                       NULL DEFAULT NULL,
    `forward_count`          int(11)                                                       NULL DEFAULT NULL,
    `status`                 tinyint(2)                                                    NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `time_index` (`create_time`) USING BTREE,
    INDEX `status_index` (`status`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tb_chirper_like
-- ----------------------------
DROP TABLE IF EXISTS `tb_chirper_like`;
CREATE TABLE `tb_chirper_like`
(
    `chirper_id`  bigint(64)  NULL DEFAULT NULL,
    `user_id`     bigint(20)  NULL DEFAULT NULL,
    `create_time` datetime(0) NULL DEFAULT NULL,
    UNIQUE INDEX `like_unique` (`chirper_id`, `user_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_unicode_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tb_media_file
-- ----------------------------
DROP TABLE IF EXISTS `tb_media_file`;
CREATE TABLE `tb_media_file`
(
    `id`          int(10) UNSIGNED                                              NOT NULL AUTO_INCREMENT,
    `name`        varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `size`        bigint(20)                                                    NULL DEFAULT NULL,
    `extension`   varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `type`        varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `category`    varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `md5`         char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci     NULL DEFAULT NULL,
    `create_time` datetime(0)                                                   NULL DEFAULT NULL,
    `url`         varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `md5_unique` (`md5`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 26
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tb_permission
-- ----------------------------
DROP TABLE IF EXISTS `tb_permission`;
CREATE TABLE `tb_permission`
(
    `id`          int(4)                                                  NOT NULL AUTO_INCREMENT,
    `path`        varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
    `description` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  CHARACTER SET = utf8
  COLLATE = utf8_unicode_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tb_private_message
-- ----------------------------
DROP TABLE IF EXISTS `tb_private_message`;
CREATE TABLE `tb_private_message`
(
    `id`              bigint(64)                                                    NOT NULL,
    `sender_id`       bigint(20)                                                    NULL DEFAULT NULL,
    `receiver_id`     bigint(20)                                                    NULL DEFAULT NULL,
    `conversation_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `content`         varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `type`            varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NULL DEFAULT NULL COMMENT 'text or media',
    `create_time`     datetime(0)                                                   NULL DEFAULT NULL,
    `status`          tinyint(1)                                                    NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tb_relation
-- ----------------------------
DROP TABLE IF EXISTS `tb_relation`;
CREATE TABLE `tb_relation`
(
    `from_id`     bigint(20)  NULL DEFAULT NULL,
    `to_id`       bigint(20)  NULL DEFAULT NULL,
    `create_time` datetime(0) NULL DEFAULT NULL,
    `status`      tinyint(2)  NULL DEFAULT NULL,
    UNIQUE INDEX `relation_unique` (`from_id`, `to_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_unicode_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tb_role
-- ----------------------------
DROP TABLE IF EXISTS `tb_role`;
CREATE TABLE `tb_role`
(
    `id`          int(4)                                                  NOT NULL AUTO_INCREMENT,
    `name`        varchar(16) CHARACTER SET utf8 COLLATE utf8_unicode_ci  NULL DEFAULT NULL,
    `description` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_unicode_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tb_site_message
-- ----------------------------
DROP TABLE IF EXISTS `tb_site_message`;
CREATE TABLE `tb_site_message`
(
    `id`          bigint(64)                                                    NOT NULL,
    `sender_id`   bigint(20)                                                    NULL DEFAULT NULL,
    `receiver_id` bigint(20)                                                    NULL DEFAULT NULL,
    `son_entity`  varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '消息子实体',
    `entity`      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '消息实体',
    `entity_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `event`       varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci       NULL DEFAULT NULL COMMENT '事件类型',
    `notice_type` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci        NULL DEFAULT NULL COMMENT '通知类型（系统通知、互动通知)',
    `create_time` datetime(0)                                                   NULL DEFAULT NULL,
    `is_read`     tinyint(1)                                                    NULL DEFAULT NULL,
    `status`      tinyint(1)                                                    NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `index_receiver` (`receiver_id`) USING BTREE,
    INDEX `index_status` (`status`) USING BTREE,
    INDEX `index_read` (`is_read`) USING BTREE,
    INDEX `index_createtime` (`create_time`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tb_user
-- ----------------------------
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user`
(
    `id`                bigint(20)                                                    NOT NULL AUTO_INCREMENT,
    `username`          char(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci     NULL DEFAULT NULL,
    `password`          char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci     NULL DEFAULT NULL,
    `nickname`          varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NULL DEFAULT NULL,
    `email`             varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `birthday`          date                                                          NULL DEFAULT NULL,
    `gender`            varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NULL DEFAULT NULL COMMENT '设置大点容许更多的可能性',
    `create_time`       datetime(0)                                                   NULL DEFAULT NULL,
    `description`       varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '主页简介',
    `profile_back_url`  varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '主页背景',
    `small_avatar_url`  varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `medium_avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `large_avatar_url`  varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `status`            tinyint(2)                                                    NULL DEFAULT NULL,
    `last_login_time`   datetime(0)                                                   NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `unique_username` (`username`) USING BTREE,
    UNIQUE INDEX `unique_email` (`email`) USING BTREE,
    INDEX `index_nickname` (`nickname`) USING BTREE,
    INDEX `index_status` (`status`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 9
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role`
(
    `user_id` bigint(20) NOT NULL,
    `role_id` int(4)     NULL DEFAULT NULL
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_unicode_ci
  ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
