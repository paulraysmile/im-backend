-- 邀请码表：用于注册时绑定企业，实现企业维度分表
-- 执行前请确保 im_company 表已存在
CREATE TABLE IF NOT EXISTS `im_invite_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `code` varchar(16) NOT NULL COMMENT '邀请码，6位数字或字母',
  `company_id` bigint NOT NULL COMMENT '关联企业id',
  `enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用 0:否 1:是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `remark` varchar(255) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请码';
