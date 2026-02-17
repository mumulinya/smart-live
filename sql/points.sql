-- =============================================
-- 积分模块数据库脚本
-- =============================================

-- 1. 用户积分钱包表
CREATE TABLE `user_points_wallet` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `balance` int(11) NOT NULL DEFAULT '0' COMMENT '当前可用积分余额',
  `total_earned` int(11) NOT NULL DEFAULT '0' COMMENT '历史累计获取积分（用于计算等级）',
  `consecutive_days` int(11) NOT NULL DEFAULT '0' COMMENT '连续签到天数',
  `last_sign_date` date DEFAULT NULL COMMENT '最后签到日期',
  `version` int(11) NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分钱包表';

-- 2. 积分流水记录表
CREATE TABLE `points_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `type` tinyint(4) NOT NULL COMMENT '类型 1:收入 2:支出',
  `amount` int(11) NOT NULL COMMENT '变动金额（绝对值）',
  `biz_type` tinyint(4) NOT NULL COMMENT '业务类型: 1:签到, 2:消费, 4:人工调整, 5:抽奖',
  `biz_id` varchar(64) DEFAULT NULL COMMENT '关联业务ID（如订单号）',
  `description` varchar(128) DEFAULT NULL COMMENT '展示文案（如：签到奖励、兑换优惠券）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水记录表';

-- 3. 每日签到记录表
CREATE TABLE `daily_sign_in` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `sign_date` date NOT NULL COMMENT '签到日期',
  `points` int(11) NOT NULL COMMENT '获得积分',
  `consecutive_days` int(11) NOT NULL DEFAULT '1' COMMENT '当前连续签到天数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `sign_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日签到记录表';

-- =============================================
-- 默认测试数据
-- =============================================

-- 用户积分钱包（假设用户ID为 1 和 2）
INSERT INTO `user_points_wallet` (`user_id`, `balance`, `total_earned`, `consecutive_days`, `last_sign_date`, `version`) VALUES
(1, 2458, 5200, 5, '2026-02-16', 0),
(2, 680, 1350, 2, '2026-02-17', 0);

-- 积分流水记录
INSERT INTO `points_record` (`user_id`, `type`, `amount`, `biz_type`, `biz_id`, `description`, `create_time`) VALUES
-- 用户1的流水
(1, 1, 10, 1, NULL, '每日签到奖励', '2026-02-12 08:30:00'),
(1, 1, 10, 1, NULL, '每日签到奖励', '2026-02-13 09:15:00'),
(1, 1, 10, 1, NULL, '每日签到奖励', '2026-02-14 07:45:00'),
(1, 1, 20, 1, NULL, '连续签到7天翻倍奖励', '2026-02-15 08:00:00'),
(1, 1, 10, 1, NULL, '每日签到奖励', '2026-02-16 08:20:00'),
(1, 2, 50, 5, NULL, '积分抽奖消耗', '2026-02-14 10:30:00'),
(1, 1, 88, 5, NULL, '抽奖获得：积分+88', '2026-02-14 10:30:01'),
(1, 2, 50, 5, NULL, '积分抽奖消耗', '2026-02-15 14:00:00'),
(1, 1, 5, 5, NULL, '抽奖获得：积分+5', '2026-02-15 14:00:01'),
(1, 1, 100, 2, '20260210001', '订单完成奖励', '2026-02-10 16:00:00'),
(1, 1, 200, 2, '20260211002', '订单完成奖励', '2026-02-11 12:30:00'),
(1, 2, 500, 2, NULL, '兑换95折优惠券', '2026-02-13 20:00:00'),
-- 用户2的流水
(2, 1, 10, 1, NULL, '每日签到奖励', '2026-02-16 09:00:00'),
(2, 1, 10, 1, NULL, '每日签到奖励', '2026-02-17 08:30:00'),
(2, 1, 150, 2, '20260215003', '订单完成奖励', '2026-02-15 18:00:00'),
(2, 2, 50, 5, NULL, '积分抽奖消耗', '2026-02-16 11:00:00'),
(2, 1, 10, 5, NULL, '抽奖获得：积分+10', '2026-02-16 11:00:01');

-- 每日签到记录
INSERT INTO `daily_sign_in` (`user_id`, `sign_date`, `points`, `consecutive_days`) VALUES
-- 用户1连续5天签到
(1, '2026-02-12', 10, 1),
(1, '2026-02-13', 10, 2),
(1, '2026-02-14', 10, 3),
(1, '2026-02-15', 20, 4),
(1, '2026-02-16', 10, 5),
-- 用户2连续2天签到
(2, '2026-02-16', 10, 1),
-- =============================================
-- 积分抽奖商品配置表
-- =============================================
DROP TABLE IF EXISTS `points_lottery_prize`;
CREATE TABLE `points_lottery_prize` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) NOT NULL COMMENT '奖品名称',
  `type` varchar(32) NOT NULL COMMENT '奖品类型 (points:积分, coupon:优惠券, none:未中奖)',
  `value` int(11) NOT NULL DEFAULT '0' COMMENT '奖品价值 (积分数或优惠券ID)',
  `probability` int(11) NOT NULL DEFAULT '0' COMMENT '中奖概率权重',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 (1:启用, 0:禁用)',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分抽奖商品配置表';

-- 初始化抽奖商品数据
INSERT INTO `points_lottery_prize` (`id`, `name`, `type`, `value`, `probability`, `description`, `sort`, `create_time`) VALUES
(1, '积分+88',  'points', 88,  10, '积分返还', 10, NOW()),
(2, '95折券',   'coupon', 1,   5,  '全场可用', 20, NOW()),
(3, '积分+10',  'points', 10,  25, '安慰奖',   30, NOW()),
(4, '积分+58',  'points', 58,  8,  '积分返还', 40, NOW()),
(5, '9折券',    'coupon', 1,   3,  '限品类',   50, NOW()),
(6, '积分+5',   'points', 5,   30, '参与奖',   60, NOW()),
(7, '积分+168', 'points', 168, 2,  '大奖',     70, NOW()),
(8, '谢谢参与', 'none',   0,   17, '再接再厉', 80, NOW());
