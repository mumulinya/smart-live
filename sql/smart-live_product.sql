/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : smart-live_product

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 13/03/2026 18:11:06
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for product
-- ----------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商店铺id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品名称',
  `sub_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '副标题',
  `rules_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '规则配置(JSON)',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '实际售价',
  `original_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '原价',
  `category` int NULL DEFAULT NULL COMMENT '商品类别 1:代金券, 2:团购套餐',
  `activity_type` int NULL DEFAULT NULL COMMENT '活动类型 0:普通售卖, 1:秒杀活动',
  `status` smallint NULL DEFAULT 0 COMMENT '状态 1:上架, 2:下架, 3:过期',
  `audit_status` smallint NULL DEFAULT 0 COMMENT '审核状态：0未审核 1审核通过 2人工审核驳回 3自动审核驳回',
  `stock` int NULL DEFAULT NULL COMMENT '库存',
  `sold` int UNSIGNED NULL DEFAULT 0 COMMENT '销量',
  `validity_type` int NULL DEFAULT NULL COMMENT '有效期类型 1:固定日期, 2:动态有效期',
  `valid_days` int NULL DEFAULT NULL COMMENT '动态有效期天数',
  `use_start_time` datetime NULL DEFAULT NULL COMMENT '使用开始时间',
  `use_end_time` datetime NULL DEFAULT NULL COMMENT '使用结束时间',
  `begin_time` datetime NULL DEFAULT NULL COMMENT '秒杀开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '秒杀结束时间',
  `reviews` int NULL DEFAULT NULL COMMENT '评价数',
  `fans` int NULL DEFAULT NULL COMMENT '粉丝数',
  `stars` int NULL DEFAULT NULL COMMENT '收藏数',
  `cover_img` char(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '商品图片',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `reject_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '拒绝原因',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
