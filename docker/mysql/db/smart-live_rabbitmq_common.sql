SET NAMES utf8mb4;

SET @rabbitmq_common_content = 'spring:
  rabbitmq:
    host: 8.134.81.22
    port: 5672
    username: smart-live
    password: 123167
    virtual-host: smart-live';

INSERT INTO `config_info` (
  `data_id`,
  `group_id`,
  `content`,
  `md5`,
  `gmt_create`,
  `gmt_modified`,
  `src_user`,
  `src_ip`,
  `tenant_id`,
  `c_desc`,
  `c_use`,
  `effect`,
  `type`,
  `encrypted_data_key`
)
VALUES (
  'rabbitmq-common.yml',
  'DEFAULT_GROUP',
  @rabbitmq_common_content,
  MD5(@rabbitmq_common_content),
  NOW(),
  NOW(),
  'nacos',
  '127.0.0.1',
  '',
  'RabbitMQ 公共连接配置',
  '供所有依赖 RabbitMQ 的微服务统一引用',
  '发布后立即生效',
  'yaml',
  ''
)
ON DUPLICATE KEY UPDATE
  `content` = VALUES(`content`),
  `md5` = VALUES(`md5`),
  `gmt_modified` = NOW(),
  `src_user` = VALUES(`src_user`),
  `src_ip` = VALUES(`src_ip`),
  `c_desc` = VALUES(`c_desc`),
  `c_use` = VALUES(`c_use`),
  `effect` = VALUES(`effect`),
  `type` = VALUES(`type`);
