-- wallet module schema

DROP TABLE IF EXISTS `wallet_transaction`;
DROP TABLE IF EXISTS `user_wallet`;

CREATE TABLE `user_wallet` (
  `user_id` bigint(20) NOT NULL COMMENT 'user id',
  `balance` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT 'available balance',
  `frozen_balance` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT 'frozen balance',
  `pay_password` varchar(128) DEFAULT NULL COMMENT 'encrypted pay password',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '1-normal 0-frozen',
  `version` int(11) NOT NULL DEFAULT '0' COMMENT 'optimistic lock version',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user wallet';

CREATE TABLE `wallet_transaction` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT 'user id',
  `type` tinyint(2) NOT NULL COMMENT '1-recharge 3-consume(order) 4-refund 5-rebate',
  `amount` decimal(10,2) NOT NULL COMMENT 'absolute amount',
  `direction` tinyint(1) NOT NULL COMMENT '1-in 2-out',
  `balance_after` decimal(10,2) NOT NULL COMMENT 'balance after transaction',
  `biz_type` varchar(32) NOT NULL COMMENT 'recharge,order_pay,refund,rebate',
  `biz_id` varchar(64) DEFAULT NULL COMMENT 'business id',
  `title` varchar(64) NOT NULL COMMENT 'display title',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0-pending 1-success 2-failed',
  `remark` varchar(255) DEFAULT NULL COMMENT 'remark',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='wallet transaction log';
