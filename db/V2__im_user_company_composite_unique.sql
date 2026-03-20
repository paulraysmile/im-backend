-- 租户内唯一：用户名 / 手机 / 邮箱在同一企业下唯一，不同企业可重复
-- 执行前请确保：
-- 1) 已为 company_id 为 NULL 的历史用户补全企业（示例，按实际企业 id 修改）：
--    UPDATE im_user SET company_id = (SELECT MIN(id) FROM im_company LIMIT 1) WHERE company_id IS NULL;
-- 2) im_company 至少有一条记录，否则无法补全 company_id

ALTER TABLE `im_user` DROP INDEX `idx_user_name`;
ALTER TABLE `im_user` DROP INDEX `idx_phone`;
ALTER TABLE `im_user` DROP INDEX `idx_email`;

ALTER TABLE `im_user`
    ADD UNIQUE KEY `uk_company_user_name` (`company_id`, `user_name`),
    ADD UNIQUE KEY `uk_company_phone` (`company_id`, `phone`),
    ADD UNIQUE KEY `uk_company_email` (`company_id`, `email`);

-- 可选：强制企业非空（仅在完成上面 UPDATE 后执行）
-- ALTER TABLE `im_user` MODIFY COLUMN `company_id` bigint NOT NULL COMMENT '归属企业id';
