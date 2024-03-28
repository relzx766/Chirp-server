/*
 Navicat Premium Data Transfer

 Source Server         : mysql-docker
 Source Server Type    : MySQL
 Source Server Version : 80027
 Source Host           : 192.168.31.14:3306
 Source Schema         : db_chirp

 Target Server Type    : MySQL
 Target Server Version : 80027
 File Encoding         : 65001

 Date: 28/03/2024 16:23:59
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for role_permission
-- ----------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission`
(
    `role_id`       int NULL DEFAULT NULL,
    `permission_id` int NULL DEFAULT NULL
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_unicode_ci
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for site_message_of_user
-- ----------------------------
DROP TABLE IF EXISTS `site_message_of_user`;
CREATE TABLE `site_message_of_user`
(
    `user_id`         bigint   NOT NULL,
    `last_read_time`  datetime NULL DEFAULT NULL,
    `final_read_time` datetime NULL DEFAULT NULL,
    UNIQUE INDEX `unique_user_id` (`user_id` ASC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_chirper
-- ----------------------------
DROP TABLE IF EXISTS `tb_chirper`;
CREATE TABLE `tb_chirper`
(
    `id`                     bigint                                                        NOT NULL,
    `author_id`              bigint                                                        NULL DEFAULT NULL,
    `community_id`           bigint                                                        NULL DEFAULT NULL,
    `conversation_id`        bigint                                                        NULL DEFAULT NULL COMMENT '对话由原创chirper及其所有回复组成，该值为原创chirper的id',
    `in_reply_to_chirper_id` bigint                                                        NULL DEFAULT NULL COMMENT '父推文标识',
    `in_reply_to_user_id`    bigint                                                        NULL DEFAULT NULL,
    `active_time`            datetime                                                      NULL DEFAULT NULL,
    `create_time`            datetime                                                      NULL DEFAULT NULL,
    `text`                   varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `type`                   varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NULL DEFAULT NULL COMMENT '原创、回复、转发、引用',
    `referenced_chirper_id`  bigint                                                        NULL DEFAULT NULL COMMENT '引用或者转发时不为空',
    `media_keys`             json                                                          NULL,
    `view_count`             int                                                           NULL DEFAULT NULL,
    `reply_count`            int                                                           NULL DEFAULT NULL,
    `like_count`             int                                                           NULL DEFAULT NULL,
    `quote_count`            int                                                           NULL DEFAULT NULL,
    `forward_count`          int                                                           NULL DEFAULT NULL,
    `status`                 tinyint                                                       NULL DEFAULT NULL,
    `reply_range`            tinyint(1)                                                    NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `time_index` (`create_time` ASC) USING BTREE,
    INDEX `status_index` (`status` ASC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_chirper_like
-- ----------------------------
DROP TABLE IF EXISTS `tb_chirper_like`;
CREATE TABLE `tb_chirper_like`
(
    `chirper_id`  bigint   NULL DEFAULT NULL,
    `user_id`     bigint   NULL DEFAULT NULL,
    `create_time` datetime NULL DEFAULT NULL,
    UNIQUE INDEX `like_unique` (`chirper_id` ASC, `user_id` ASC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_unicode_ci
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_community
-- ----------------------------
DROP TABLE IF EXISTS `tb_community`;
CREATE TABLE `tb_community`
(
    `id`          bigint                                                         NOT NULL,
    `user_id`     bigint                                                         NULL DEFAULT NULL,
    `name`        varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NULL DEFAULT NULL,
    `tags`        varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
    `cover`       varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
    `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
    `join_range`  tinyint(1)                                                     NULL DEFAULT NULL COMMENT '谁可以加入（范围）',
    `post_range`  tinyint(1)                                                     NULL DEFAULT NULL COMMENT '谁可以发布（范围）',
    `create_time` datetime                                                       NULL DEFAULT NULL,
    `rules`       text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci          NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tb_community_apply
-- ----------------------------
DROP TABLE IF EXISTS `tb_community_apply`;
CREATE TABLE `tb_community_apply`
(
    `id`           bigint     NOT NULL,
    `user_id`      bigint     NULL DEFAULT NULL,
    `approver_id`  bigint     NULL DEFAULT NULL,
    `community_id` bigint     NULL DEFAULT NULL,
    `type`         tinyint(1) NULL DEFAULT NULL,
    `create_time`  datetime   NULL DEFAULT NULL,
    `update_time`  datetime   NULL DEFAULT NULL,
    `status`       tinyint(1) NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tb_community_invitation
-- ----------------------------
DROP TABLE IF EXISTS `tb_community_invitation`;
CREATE TABLE `tb_community_invitation`
(
    `id`           bigint     NOT NULL,
    `fromId_id`    bigint     NULL DEFAULT NULL,
    `to_id`        bigint     NULL DEFAULT NULL,
    `community_id` bigint     NULL DEFAULT NULL,
    `create_time`  datetime   NULL DEFAULT NULL,
    `update_time`  datetime   NULL DEFAULT NULL,
    `status`       tinyint(1) NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tb_media_file
-- ----------------------------
DROP TABLE IF EXISTS `tb_media_file`;
CREATE TABLE `tb_media_file`
(
    `id`          int UNSIGNED                                                  NOT NULL AUTO_INCREMENT,
    `name`        varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `size`        bigint                                                        NULL DEFAULT NULL,
    `extension`   varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `type`        varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `category`    varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `md5`         char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci     NULL DEFAULT NULL,
    `create_time` datetime                                                      NULL DEFAULT NULL,
    `url`         varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `md5_unique` (`md5` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 57
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_permission
-- ----------------------------
DROP TABLE IF EXISTS `tb_permission`;
CREATE TABLE `tb_permission`
(
    `id`          int                                                     NOT NULL AUTO_INCREMENT,
    `path`        varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
    `description` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  CHARACTER SET = utf8
  COLLATE = utf8_unicode_ci
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_private_message
-- ----------------------------
DROP TABLE IF EXISTS `tb_private_message`;
CREATE TABLE `tb_private_message`
(
    `id`              bigint                                                         NOT NULL,
    `sender_id`       bigint                                                         NULL DEFAULT NULL,
    `receiver_id`     bigint                                                         NULL DEFAULT NULL,
    `conversation_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NULL DEFAULT NULL,
    `content`         text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci          NULL,
    `iv`              varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `type`            varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci   NULL DEFAULT NULL COMMENT 'text or media',
    `reference_id`    bigint                                                         NULL DEFAULT NULL,
    `create_time`     datetime                                                       NULL DEFAULT NULL,
    `status`          varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_relation
-- ----------------------------
DROP TABLE IF EXISTS `tb_relation`;
CREATE TABLE `tb_relation`
(
    `from_id`     bigint   NULL DEFAULT NULL,
    `to_id`       bigint   NULL DEFAULT NULL,
    `create_time` datetime NULL DEFAULT NULL,
    `status`      tinyint  NULL DEFAULT NULL,
    UNIQUE INDEX `relation_unique` (`from_id` ASC, `to_id` ASC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_unicode_ci
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_role
-- ----------------------------
DROP TABLE IF EXISTS `tb_role`;
CREATE TABLE `tb_role`
(
    `id`          int                                                     NOT NULL AUTO_INCREMENT,
    `name`        varchar(16) CHARACTER SET utf8 COLLATE utf8_unicode_ci  NULL DEFAULT NULL,
    `description` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_unicode_ci
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_site_message
-- ----------------------------
DROP TABLE IF EXISTS `tb_site_message`;
CREATE TABLE `tb_site_message`
(
    `id`          bigint                                                        NOT NULL,
    `sender_id`   bigint                                                        NULL DEFAULT NULL,
    `receiver_id` bigint                                                        NULL DEFAULT NULL,
    `son_entity`  varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '消息子实体',
    `entity`      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '消息实体',
    `entity_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `event`       varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci       NULL DEFAULT NULL COMMENT '事件类型',
    `notice_type` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci        NULL DEFAULT NULL COMMENT '通知类型（系统通知、互动通知)',
    `create_time` datetime                                                      NULL DEFAULT NULL,
    `status`      tinyint(1)                                                    NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `index_receiver` (`receiver_id` ASC) USING BTREE,
    INDEX `index_status` (`status` ASC) USING BTREE,
    INDEX `index_createtime` (`create_time` ASC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_user
-- ----------------------------
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user`
(
    `id`                bigint                                                        NOT NULL AUTO_INCREMENT,
    `username`          char(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci     NULL DEFAULT NULL,
    `password`          char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci     NULL DEFAULT NULL,
    `nickname`          varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NULL DEFAULT NULL,
    `email`             varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `birthday`          date                                                          NULL DEFAULT NULL,
    `gender`            varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NULL DEFAULT NULL COMMENT '设置大点容许更多的可能性',
    `create_time`       datetime                                                      NULL DEFAULT NULL,
    `description`       varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '主页简介',
    `profile_back_url`  varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '主页背景',
    `small_avatar_url`  varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `medium_avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `large_avatar_url`  varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `status`            tinyint                                                       NULL DEFAULT NULL,
    `last_login_time`   datetime                                                      NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `unique_username` (`username` ASC) USING BTREE,
    UNIQUE INDEX `unique_email` (`email` ASC) USING BTREE,
    INDEX `index_nickname` (`nickname` ASC) USING BTREE,
    INDEX `index_status` (`status` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 18
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_of_chat_public
-- ----------------------------
DROP TABLE IF EXISTS `user_of_chat_public`;
CREATE TABLE `user_of_chat_public`
(
    `id`                  bigint                                                        NOT NULL,
    `user_id`             bigint                                                        NULL DEFAULT NULL,
    `chat_allow`          tinyint(1)                                                    NULL DEFAULT NULL,
    `pinned_conversation` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_of_community
-- ----------------------------
DROP TABLE IF EXISTS `user_of_community`;
CREATE TABLE `user_of_community`
(
    `id`           bigint     NOT NULL,
    `community_id` bigint     NOT NULL,
    `user_id`      bigint     NULL DEFAULT NULL,
    `role`         tinyint(1) NULL DEFAULT NULL,
    `create_Time`  datetime   NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role`
(
    `user_id` bigint NOT NULL,
    `role_id` int    NULL DEFAULT NULL
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_unicode_ci
  ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
