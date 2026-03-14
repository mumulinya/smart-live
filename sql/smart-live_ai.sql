SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `ai_merchant_session`;
CREATE TABLE `ai_merchant_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `user_id` bigint NOT NULL COMMENT 'merchant user id',
  `shop_id` bigint NOT NULL COMMENT 'shop id',
  `type` varchar(32) NOT NULL COMMENT 'session type: reply/analysis/copywrite/suggest',
  `title` varchar(100) DEFAULT 'new session' COMMENT 'session title',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_shop_type_update` (`user_id`, `shop_id`, `type`, `update_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='merchant ai session';

DROP TABLE IF EXISTS `ai_merchant_message`;
CREATE TABLE `ai_merchant_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `session_id` bigint NOT NULL COMMENT 'session id',
  `role` varchar(10) NOT NULL COMMENT 'user/assistant',
  `content` text NOT NULL COMMENT 'message content',
  `review_id` bigint DEFAULT NULL COMMENT 'related review id',
  `product_id` bigint DEFAULT NULL COMMENT 'related product id',
  `time_range` varchar(10) DEFAULT NULL COMMENT 'analysis time range: week/month/quarter',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_session_create` (`session_id`, `create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='merchant ai message';

SET FOREIGN_KEY_CHECKS = 1;