-- AI Session Table
CREATE TABLE `ai_session` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Session ID',
  `user_id` bigint(20) NOT NULL COMMENT 'User ID',
  `title` varchar(50) DEFAULT NULL COMMENT 'Session Title',
  `create_by` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime DEFAULT NULL COMMENT 'Create Time',
  `update_by` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime DEFAULT NULL COMMENT 'Update Time',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Chat Session';

-- AI Message Table
CREATE TABLE `ai_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Message ID',
  `session_id` bigint(20) NOT NULL COMMENT 'Session ID',
  `role` varchar(20) NOT NULL COMMENT 'Role (user/assistant/system)',
  `content` text COMMENT 'Message Content',
  `type` varchar(20) DEFAULT 'text' COMMENT 'Message Type (text/image/tool)',
  `create_by` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime DEFAULT NULL COMMENT 'Create Time',
  `update_by` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime DEFAULT NULL COMMENT 'Update Time',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Chat Message';
