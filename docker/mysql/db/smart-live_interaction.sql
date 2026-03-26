/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : smart-live_interaction

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 13/03/2026 18:07:08
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for comment
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户id',
  `source_type` int NULL DEFAULT NULL COMMENT '来源类型  2（店铺）, 1（文章）, 3（团购）等。',
  `source_id` bigint UNSIGNED NOT NULL COMMENT '来源id  对应来源类型表的主键ID。例如：如果 source_type=2，则此字段存 shop_id；如果 source_type=1，则此字段存 article_id。',
  `parent_id` bigint UNSIGNED NOT NULL COMMENT '关联的1级评论id，如果是一级评论，则值为0',
  `answer_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '回复的评论id',
  `images` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '评论的图片',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '回复的内容',
  `liked` int NULL DEFAULT 0 COMMENT '点赞数',
  `reply_count` int NULL DEFAULT 0 COMMENT '回复评论数',
  `status` tinyint NULL DEFAULT NULL COMMENT '状态 0正常 1不显示',
  `audit_status` tinyint NULL DEFAULT 0 COMMENT '审核状态：0未审核 1审核通过 2人工审核驳回 3自动审核驳回',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 513 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Table structure for follow
-- ----------------------------
DROP TABLE IF EXISTS `follow`;
CREATE TABLE `follow`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户id',
  `source_id` int NULL DEFAULT NULL COMMENT '关注类型(1用户  2店铺)',
  `source_type` bigint UNSIGNED NOT NULL COMMENT '关注目标id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 103 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Table structure for like_record
-- ----------------------------
DROP TABLE IF EXISTS `like_record`;
CREATE TABLE `like_record`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `source_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '来源类型(1 店铺 2博客  3评价 ）',
  `source_id` bigint NOT NULL COMMENT '来源id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 118 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for review
-- ----------------------------
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户id',
  `shop_id` bigint NULL DEFAULT NULL COMMENT '店铺id',
  `order_id` bigint NULL DEFAULT NULL COMMENT '订单id',
  `source_type` int NULL DEFAULT NULL COMMENT '来源类型  2（店铺）, 1（文章）, 3（团购）等。',
  `source_id` bigint UNSIGNED NOT NULL COMMENT '来源id  对应来源类型表的主键ID。例如：如果 source_type=2，则此字段存 shop_id；如果 source_type=1，则此字段存 article_id。',
  `images` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '评论的图片',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '评价的内容',
  `liked` int NULL DEFAULT 0 COMMENT '点赞数',
  `stared` int NULL DEFAULT 0 COMMENT '收藏数量',
  `reply_count` int NULL DEFAULT 0 COMMENT '回复评论数',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '0草稿 1上架  2下架',
  `audit_status` tinyint NULL DEFAULT 0 COMMENT '审核状态：0未审核 1审核通过 2人工审核驳回 3自动审核驳回',
  `score` tinyint NULL DEFAULT NULL COMMENT '总评分',
  `taste_score` tinyint NULL DEFAULT NULL COMMENT '口味评分',
  `env_score` tinyint NULL DEFAULT NULL COMMENT '环境评分',
  `service_score` tinyint NULL DEFAULT NULL COMMENT '服务评分',
  `is_anonymous` tinyint NULL DEFAULT 0 COMMENT '是否匿名 (0:否 1:是)',
  `reject_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '拒绝原因',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 483 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Table structure for star
-- ----------------------------
DROP TABLE IF EXISTS `star`;
CREATE TABLE `star`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `source_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '来源类型(1 店铺 2博客 ）',
  `source_id` bigint NOT NULL COMMENT '来源id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 88 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
