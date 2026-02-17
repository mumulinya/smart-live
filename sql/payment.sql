-- =============================================
-- 支付模块数据库脚本
-- =============================================

-- 支付流水表
CREATE TABLE `payment_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `pay_sn` varchar(64) NOT NULL COMMENT '支付流水号(传给微信的out_trade_no)',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型: recharge(充值), order(订单)',
  `biz_id` varchar(64) NOT NULL COMMENT '业务ID(充值单号/订单号)',
  `amount` decimal(10,2) NOT NULL COMMENT '支付金额(元)',
  `pay_method` varchar(16) NOT NULL DEFAULT 'wechat' COMMENT '支付方式: wechat/alipay',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0:待支付 1:支付成功 2:支付失败 3:已关闭',
  `transaction_id` varchar(64) DEFAULT NULL COMMENT '第三方支付单号(微信返回的transaction_id)',
  `pay_time` datetime DEFAULT NULL COMMENT '支付成功时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_sn` (`pay_sn`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';
