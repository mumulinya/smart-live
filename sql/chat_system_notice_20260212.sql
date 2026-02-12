-- chat system notice table
DROP TABLE IF EXISTS `chat_system_notice`;
CREATE TABLE `chat_system_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'pk',
  `notice_id` varchar(64) NOT NULL COMMENT 'business notice id',
  `user_id` bigint NOT NULL COMMENT 'receiver user id',
  `source_type` int NOT NULL COMMENT 'source type',
  `source_id` bigint DEFAULT NULL COMMENT 'source id',
  `action` varchar(64) DEFAULT NULL COMMENT 'action code',
  `title` varchar(200) NOT NULL COMMENT 'title',
  `content` varchar(1000) DEFAULT NULL COMMENT 'content',
  `reject_reason` varchar(1000) DEFAULT NULL COMMENT 'reject reason',
  `link` varchar(512) DEFAULT NULL COMMENT 'jump link',
  `payload` text COMMENT 'extra payload json',
  `read_status` tinyint NOT NULL DEFAULT '0' COMMENT '0 unread 1 read',
  `read_at` datetime DEFAULT NULL COMMENT 'read time',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_id` (`notice_id`),
  KEY `idx_user_create_time` (`user_id`,`create_time`),
  KEY `idx_user_read_status` (`user_id`,`read_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='chat system notice';

-- sample data
INSERT INTO `chat_system_notice`
(`notice_id`,`user_id`,`source_type`,`source_id`,`action`,`title`,`content`,`reject_reason`,`link`,`payload`,`read_status`,`read_at`,`create_time`,`update_time`)
VALUES
('n_20260212_0001',1,1,123,'AUDIT_REJECT','User info audit rejected','User info audit rejected','Avatar contains contact details, please update and resubmit','/user/info','{}',0,NULL,'2026-02-12 12:00:00','2026-02-12 12:00:00');
