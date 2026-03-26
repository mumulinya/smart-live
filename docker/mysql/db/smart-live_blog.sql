/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : smart-live_blog

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 13/03/2026 18:08:05
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for blog
-- ----------------------------
DROP TABLE IF EXISTS `blog`;
CREATE TABLE `blog`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id` bigint NULL DEFAULT NULL COMMENT '商户id',
  `type_id` bigint NULL DEFAULT NULL COMMENT '类型id',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `images` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '探店的照片，最多9张，多张以\",\"隔开',
  `content` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '探店的文字描述',
  `liked` int UNSIGNED NULL DEFAULT 0 COMMENT '点赞数量',
  `stared` int NULL DEFAULT 0 COMMENT '收藏数',
  `comments` int UNSIGNED NULL DEFAULT 0 COMMENT '评论数量',
  `pin` smallint NULL DEFAULT 0 COMMENT '置顶  1置顶 0未置顶',
  `status` smallint UNSIGNED NULL DEFAULT 1 COMMENT '状态 0草稿 1发布 2下架',
  `audit_status` smallint NULL DEFAULT 0 COMMENT '审核状态：0未审核 1审核通过 2人工审核驳回 3自动审核驳回',
  `reject_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '拒绝原因',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_typeId_shopId_userId`(`type_id` ASC, `shop_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_blog_user_status_pin_ctime`(`user_id` ASC, `status` ASC, `pin` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 68 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = COMPACT;

SET FOREIGN_KEY_CHECKS = 1;
