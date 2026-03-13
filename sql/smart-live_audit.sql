/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : smart-live_audit

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 13/03/2026 18:07:50
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for audit_task
-- ----------------------------
DROP TABLE IF EXISTS `audit_task`;
CREATE TABLE `audit_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_id` bigint NOT NULL COMMENT '业务ID',
  `biz_type` int NOT NULL COMMENT '业务类型(1-用户,2-店铺,3-博客,4-代金券,5-评论,6-团购)',
  `submitter_id` bigint NOT NULL COMMENT '提交人ID',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态(0-待审, 1-通过, 2-驳回)',
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '驳回原因',
  `audit_content` json NULL COMMENT '审核内容快照(JSON)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE COMMENT '状态索引'
) ENGINE = InnoDB AUTO_INCREMENT = 2032381507070418946 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '审核任务表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
