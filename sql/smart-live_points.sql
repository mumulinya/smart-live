/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : smart-live_points

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 13/03/2026 18:08:51
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for daily_sign_in
-- ----------------------------
DROP TABLE IF EXISTS `daily_sign_in`;
CREATE TABLE `daily_sign_in`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `sign_date` date NOT NULL COMMENT '签到日期',
  `points` int NOT NULL COMMENT '获得积分',
  `consecutive_days` int NOT NULL DEFAULT 1 COMMENT '当前连续签到天数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id` ASC, `sign_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '每日签到记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for points_lottery_prize
-- ----------------------------
DROP TABLE IF EXISTS `points_lottery_prize`;
CREATE TABLE `points_lottery_prize`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '奖品名称',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '奖品类型 (points:积分, coupon:优惠券, none:未中奖)',
  `value` int NOT NULL DEFAULT 0 COMMENT '奖品价值 (积分数或优惠券ID)',
  `probability` int NOT NULL DEFAULT 0 COMMENT '中奖概率权重',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 (1:启用, 0:禁用)',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '积分抽奖商品配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for points_record
-- ----------------------------
DROP TABLE IF EXISTS `points_record`;
CREATE TABLE `points_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `type` tinyint NOT NULL COMMENT '类型 1:收入 2:支出',
  `amount` int NOT NULL COMMENT '变动金额（绝对值）',
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '(1:签到, 2:消费, 4:人工调整, 5:抽奖',
  `biz_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联业务ID（如订单号）',
  `description` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '展示文案（如：签到奖励、兑换优惠券）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_time`(`user_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 41 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '积分流水记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_points_wallet
-- ----------------------------
DROP TABLE IF EXISTS `user_points_wallet`;
CREATE TABLE `user_points_wallet`  (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `balance` int NOT NULL DEFAULT 0 COMMENT '当前可用积分余额',
  `total_earned` int NOT NULL DEFAULT 0 COMMENT '历史累计获取积分（用于计算等级）',
  `consecutive_days` int NOT NULL DEFAULT 0 COMMENT '连续签到天数',
  `last_sign_date` date NULL DEFAULT NULL COMMENT '最后签到日期',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户积分钱包表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
