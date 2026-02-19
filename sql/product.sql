-- ----------------------------
-- Table structure for product
-- ----------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id` bigint(20) NULL DEFAULT NULL COMMENT '商店铺id',
  `type_id` bigint(20) NULL DEFAULT NULL COMMENT '商品类型id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品名称',
  `sub_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '副标题',
  `rules_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '规则配置(JSON)',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '实际售价',
  `original_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '原价',
  `category` int(11) NULL DEFAULT NULL COMMENT '商品类别 1:代金券, 2:团购套餐',
  `activity_type` int(11) NULL DEFAULT NULL COMMENT '活动类型 0:普通售卖, 1:秒杀活动',
  `status` int(11) NULL DEFAULT NULL COMMENT '状态 1:上架, 2:下架, 3:过期',
  `stock` int(11) NULL DEFAULT NULL COMMENT '库存',
  `sold` int(11) NULL DEFAULT NULL COMMENT '销量',
  `validity_type` int(11) NULL DEFAULT NULL COMMENT '有效期类型 1:固定日期, 2:动态有效期',
  `valid_days` int(11) NULL DEFAULT NULL COMMENT '动态有效期天数',
  `use_start_time` datetime(0) NULL DEFAULT NULL COMMENT '使用开始时间',
  `use_end_time` datetime(0) NULL DEFAULT NULL COMMENT '使用结束时间',
  `begin_time` datetime(0) NULL DEFAULT NULL COMMENT '秒杀开始时间',
  `end_time` datetime(0) NULL DEFAULT NULL COMMENT '秒杀结束时间',
  `reviews` int(11) NULL DEFAULT NULL COMMENT '评价数',
  `fans` int(11) NULL DEFAULT NULL COMMENT '粉丝数',
  `stars` int(11) NULL DEFAULT NULL COMMENT '收藏数',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
