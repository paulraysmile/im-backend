create table `im_user` (
  `id` bigint not null auto_increment primary key comment 'id',
  `user_name` varchar(255) not null comment '用户名',
  `nick_name` varchar(255) not null comment '用户昵称',
  `head_image` varchar(255) default '' comment '用户头像',
  `head_image_thumb` varchar(255) default '' comment '用户头像缩略图',
  `password` varchar(255) not null comment '密码',
  `sex` tinyint default 0 comment '性别 0:男 1:女',
  `phone` varchar(16) default null comment '手机号码',
  `email` varchar(32) default null comment '邮箱',
  `company_id` bigint comment '归属企业id',
  `company_name` varchar(128) comment '归属企业名称',
  `is_banned` tinyint(1) default 0 comment '是否被封禁 0:否 1:是',
	`unban_time` datetime default null comment '解除封禁时间',
  `reason` varchar(255) default '' comment '被封禁原因',
  `type` tinyint default 1 comment '用户类型 1:普通用户 2:公开测试账户 3:审核专用账户',
  `signature` varchar(1024) default '' comment '个性签名',
  `is_manual_approve` tinyint(1) default 0 comment '是否手动验证好友请求',
  `audio_tip` tinyint default 1 comment '新消息语音提醒 bit-0:web端 bit-1:app端',
  `cid` varchar(255) default '' comment '客户端id,用于uni-push推送',
  `status` tinyint(1) default 0 comment '状态  0:正常  1:已注销',
  `auth_status` tinyint DEFAULT 0 comment '实名认证状态: 0-未认证  1-审核中 2-已认证 3-认证失败',
  `last_login_time` datetime default null comment '最后登录时间',
  `last_login_ip` varchar(64) default null comment '最后登陆ip',
  `created_time` datetime default current_timestamp comment '创建时间',
  unique key `idx_user_name` (user_name),
  unique key `idx_phone` (phone),
  unique key `idx_email` (email),
  key `idx_nick_name` (nick_name),
  key `idx_last_login_ip` (last_login_ip)
) engine = innodb charset = utf8mb4 comment '用户';

create table `im_friend` (
  `id` bigint not null auto_increment primary key comment 'id',
  `user_id` bigint not null comment '用户id',
  `friend_id` bigint not null comment '好友id',
  `friend_nick_name` varchar(255) not null comment '好友昵称',
  `friend_head_image` varchar(255) default '' comment '好友头像',
  `friend_company_name` varchar(128) comment '企业名称',
  `remark_nick_name` varchar(255) default '' comment '备注昵称',
  `is_dnd` tinyint(1) default 0 comment '免打扰标识(do not disturb)  0:关闭   1:开启',
  `is_top` tinyint(1) default 0 comment '是否置顶会话',
  `deleted` tinyint(1) comment '删除标识  0：正常   1：已删除',
  `created_time` datetime default current_timestamp comment '创建时间',
  key `idx_user_id` (`user_id`),
  key `idx_friend_id` (`friend_id`)
) engine = innodb charset = utf8mb4 comment '好友';

create table `im_friend_request` (
  `id` bigint not null auto_increment primary key comment 'id',
  `send_id` bigint not null comment '发起方用户id',
  `send_nick_name` varchar(255) not null comment '发起方昵称，冗余字段',
  `send_head_image` varchar(255) default null comment '发起方头像，冗余字段',
  `recv_id` bigint not null comment '接收方用户id',
  `recv_nick_name` varchar(255) not null comment '接收方昵称，冗余字段',
  `recv_head_image` varchar(255) default null comment '接收方头像，冗余字段',
  `remark` varchar(255) default '' comment '申请备注',
  `status` tinyint default 1 comment '状态  1:未处理 2:同意 3:拒绝 4:过期',
  `apply_time` datetime default current_timestamp comment '申请时间',
  key `idx_send_id` (`send_id`),
  key `idx_recv_id` (`recv_id`),
  key `idx_apply_time` (`apply_time`)
) engine = innodb charset = utf8mb4 comment '好友申请列表';

create table `im_private_message` (
  `id` bigint not null auto_increment primary key comment 'id',
  `tmp_id` varchar(32) comment '临时id,由前端生成',
  `send_id` bigint not null comment '发送用户id',
  `recv_id` bigint not null comment '接收用户id',
  `content` text character set utf8mb4 comment '发送内容',
  `type` tinyint not null comment '消息类型 0:文字 1:图片 2:文件 3:语音 4:视频 21:提示',
  `quote_message_id` bigint default null comment '引用消息id',
  `status` tinyint not null comment '状态 0:未读 1:已发送 2:撤回 3:已读',
  `send_time` datetime default current_timestamp comment '发送时间',
  key `idx_send_recv_id` (`send_id`, recv_id),
  key `idx_recv_id` (`recv_id`)
) engine = innodb charset = utf8mb4 comment '私聊消息';

create table `im_group` (
  `id` bigint not null auto_increment primary key comment 'id',
  `name` varchar(255) not null comment '群名字',
  `owner_id` bigint not null comment '群主id',
  `head_image` varchar(255) default '' comment '群头像',
  `head_image_thumb` varchar(255) default '' comment '群头像缩略图',
  `notice` varchar(1024) default '' comment '群公告',
  `top_message_id` bigint default null comment '置顶消息id',
  `is_all_muted` tinyint(1) default 0 comment '是否开启全体禁言 0:否 1:是',
  `is_allow_invite` tinyint(1) default 1 comment '是否允许普通成员邀请好友 0:否 1:是',
  `is_allow_share_card` tinyint(1) default 1 comment '是否允许普通成员分享名片 0:否 1:是',
  `is_banned` tinyint(1) default 0 comment '是否被封禁 0:否 1:是',
	`unban_time` datetime default null comment '解除封禁时间',
  `reason` varchar(255) default '' comment '被封禁原因',
  `dissolve` tinyint(1) default 0 comment '是否已解散',
  `created_time` datetime default current_timestamp comment '创建时间'
) engine = innodb charset = utf8mb4 comment '群';

create table `im_group_member` (
  `id` bigint not null auto_increment primary key comment 'id',
  `group_id` bigint not null comment '群id',
  `user_id` bigint not null comment '用户id',
  `user_nick_name` varchar(255) default '' comment '用户昵称',
  `remark_nick_name` varchar(255) default '' comment '显示昵称备注',
  `head_image` varchar(255) default '' comment '用户头像',
  `company_name` varchar(128) comment '企业名称',
  `remark_group_name` varchar(255) default '' comment '显示群名备注',
  `is_manager` tinyint(1) default 0 comment '是否管理员 0:否 1:是',
  `is_muted` tinyint(1) default 0 comment '是否被禁言 0:否 1:是',
  `is_dnd` tinyint(1) comment '免打扰标识(do not disturb)  0:关闭   1:开启',
  `is_top_message` tinyint(1) default 0 comment '是否显示置顶消息',
  `is_top` tinyint(1) default 0 comment '是否置顶会话',
  `quit` tinyint(1) default 0 comment '是否已退出',
  `quit_time` datetime default null comment '退出时间',
  `created_time` datetime default current_timestamp comment '创建时间',
  `version` int default 0 comment '版本号',
  key `idx_group_id` (`group_id`),
  key `idx_user_id` (`user_id`)
) engine = innodb charset = utf8mb4 comment '群成员';

create table `im_group_message` (
  `id` bigint not null auto_increment primary key comment 'id',
  `tmp_id` varchar(32) comment '临时id,由前端生成',
  `group_id` bigint not null comment '群id',
  `send_id` bigint not null comment '发送用户id',
  `send_nick_name` varchar(255) default '' comment '发送用户昵称',
  `recv_ids` varchar(1024) default '' comment '接收用户id,逗号分隔，为空表示发给所有成员',
  `content` text character set utf8mb4 comment '发送内容',
  `at_user_ids` varchar(1024) comment '被@的用户id列表，逗号分隔',
  `receipt` tinyint(1) default 0 comment '是否回执消息',
  `receipt_ok` tinyint(1) default 0 comment '回执消息是否完成',
  `type` tinyint not null comment '消息类型 0:文字 1:图片 2:文件 3:语音 4:视频 21:提示',
  `quote_message_id` bigint default null comment '引用消息id',
  `status` tinyint default 0 comment '状态 0:未发出  2:撤回 ',
  `send_time` datetime default current_timestamp comment '发送时间',
  key `idx_group_id` (group_id)
) engine = innodb charset = utf8mb4 comment '群消息';

create table `im_system_message` (
  `id` bigint not null auto_increment primary key comment 'id',
  `title` varchar(64) not null comment '标题',
  `cover_url` varchar(255) comment '封面图片',
  `intro` varchar(1024) not null comment '简介',
  `content_type` tinyint default 0 comment '内容类型 0:富文本  1:外部链接',
  `rich_text` text comment '富文本内容，base64编码',
  `extern_link` varchar(255) comment '外部链接',
  `deleted` tinyint(1) default 0 comment '删除标识  0：正常   1：已删除',
  `creator` bigint comment '创建者',
  `create_time` datetime comment '创建时间'
) engine = innodb charset = utf8mb4 comment '系统消息';

create table `im_sm_push_task` (
  `id` bigint not null auto_increment primary key comment 'id',
  `message_id` bigint not null comment '系统消息id',
  `seq_no` bigint comment '发送序列号',
  `send_time` datetime comment '推送时间',
  `status` tinyint default 1 comment '状态 1:待发送 2:发送中 3:已发送 4:已取消',
  `send_to_all` tinyint(1) default 1 comment '是否发送给全体用户',
  `recv_ids` varchar(1024) comment '接收用户id,逗号分隔,send_to_all为false时有效',
  `deleted` tinyint(1) comment '删除标识  0：正常   1：已删除',
  `creator` bigint comment '创建者',
  `create_time` datetime comment '创建时间',
  unique key `idx_seq_no` (seq_no)
) engine = innodb charset = utf8mb4 comment '系统消息推送任务';

create table `im_sensitive_word` (
  `id` bigint not null auto_increment primary key comment 'id',
  `content` varchar(64) not null comment '敏感词内容',
  `enabled` tinyint(1) default 0 comment '是否启用 0:未启用 1:启用',
  `creator` bigint default null comment '创建者',
  `create_time` datetime default current_timestamp comment '创建时间'
) engine = innodb charset = utf8mb4 comment '敏感词';

create table `im_user_blacklist` (
  `id` bigint not null auto_increment primary key comment 'id',
  `from_user_id` bigint not null comment '拉黑用户id',
  `to_user_id` bigint not null comment '被拉黑用户id',
  `create_time` datetime default current_timestamp comment '创建时间',
  key `idx_from_user_id` (from_user_id)
) engine = innodb charset = utf8mb4 comment '用户黑名单';

create table `im_file_info` (
  `id` bigint not null auto_increment primary key comment 'id',
  `file_name` varchar(255) not null comment '文件名',
  `file_path` varchar(255) not null comment '文件地址',
  `file_size` integer not null comment '文件大小',
  `file_type` tinyint not null comment '0:普通文件 1:图片 2:视频',
  `compressed_path` varchar(255) default null comment '压缩文件路径',
  `cover_path` varchar(255) default null comment '封面文件路径，仅视频文件有效',
  `upload_time` datetime default current_timestamp comment '上传时间',
  `is_permanent` tinyint(1) default 0 comment '是否永久文件',
  `md5` varchar(64) not null comment '文件md5',
  key `idx_md5` (md5)
) engine = innodb charset = utf8mb4 comment '文件';

create table `im_user_complaint` (
  `id` bigint not null auto_increment primary key comment 'id',
  `user_id` bigint not null comment '用户id',
  `target_type` tinyint not null comment '投诉对象类型 1:用户 2:群聊',
  `target_id` bigint not null comment '投诉对象id',
  `target_name` varchar(255) not null comment '投诉对象名称',
  `type` tinyint not null comment '投诉原因类型 1:对我造成骚扰 2:疑似诈骗 3:传播不良内容 99:其他',
  `images` varchar(4096) default '' comment '图片列表,最多9张',
  `content` varchar(1024) default '' comment '投诉内容',
  `status` tinyint default 1 comment '状态 1:未处理 2:已处理',
  `resolved_admin_id` bigint default null comment '处理投诉的管理员id',
  `resolved_type` varchar(255) default null comment '处理结果类型 1:已处理 2:不予处理 3:未涉及不正规行为 4:其他',
  `resolved_summary` varchar(255) default null comment '处理结果摘要',
  `resolved_time` varchar(255) default null comment '处理时间',
  `create_time` datetime default current_timestamp comment '创建时间',
  key `idx_user_id` (user_id)
) engine = innodb charset = utf8mb4 comment '用户投诉';

create table im_company (
  `id` bigint not null auto_increment primary key comment 'id',
  `name` varchar(128) not null comment '企业名称',
  `code` varchar(64) not null comment '统一社会信用代码',
  `license` varchar(256) comment '营业执照',
  `biz_scope` varchar(256) comment '业务范围',
  `contact_person` varchar(32) comment '联系人姓名',
  `contact_phone` varchar(20) comment '联系电话',
  `deleted` tinyint(1) default 0 comment '删除标识  0：正常   1：已删除',
  `creator` bigint comment '创建者',
  `create_time` datetime comment '创建时间'
) engine = innodb charset = utf8mb4 comment '企业信息';

create table `im_sticker_album` (
  `id` bigint(20) not null auto_increment comment '专辑id',
  `name` varchar(100) not null comment '专辑名称',
  `logo_url` varchar(128) not null comment '专辑logo',
  `sort` int default 0 comment '排序权重',
  `status` tinyint(1) default 1 comment '状态 0:下架 1:上架',
  `description` varchar(1024) comment '专辑描述',
  `deleted` tinyint(1) default 0 comment '删除标识  0：正常   1：已删除',
  `creator` bigint comment '创建者',
  `create_time` datetime default current_timestamp comment '创建时间',
  primary key (`id`)
) engine = innodb default charset = utf8mb4 comment = '表情包专辑';

create table `im_sticker` (
  `id` bigint(20) not null auto_increment comment '表情id',
  `album_id` bigint(20) not null comment '专辑id',
  `name` varchar(64) not null comment '表情名称',
  `image_url` varchar(128) not null comment '表情图片url',
  `thumb_url` varchar(128) not null comment '缩略图url',
  `width` int default null comment '图片宽度',
  `height` int default null comment '图片高度',
  `sort` int default 0 comment '排序权重',
  `deleted` tinyint(1) default 0 comment '删除标识  0：正常   1：已删除',
  `creator` bigint comment '创建者',
  `create_time` datetime default current_timestamp comment '创建时间',
  primary key (`id`),
  key `idx_album_id` (`album_id`)
) engine = innodb default charset = utf8mb4 comment = '表情包';

create table `im_sticker_custom` (
  `id` bigint(20) not null auto_increment comment '表情id',
  `user_id` bigint(20) not null comment '用户id',
  `album_id` bigint(20) default null comment '专辑id',
  `sticker_id` bigint(20) default null comment '表情id',
  `name` varchar(64) default '' comment '表情名称',
  `image_url` varchar(128) not null comment '表情图片url',
  `thumb_url` varchar(128) not null comment '缩略图url',
  `width` int default null comment '图片宽度',
  `height` int default null comment '图片高度',
  `sort` int default 10000 comment '排序权重,序号小的展示在前面',
  `create_time` datetime default current_timestamp comment '创建时间',
  primary key (`id`),
  key `idx_user_id` (`user_id`)
) engine = innodb default charset = utf8mb4 comment = '用户自定义表情';

create table `im_realname_auth` (
  `id` bigint not null auto_increment comment '主键id',
  `user_id` bigint not null comment '用户id',
  `real_name` varchar(50) not null comment '真实姓名',
  `id_card_number` varchar(30) not null comment '证件号码',
  `id_card_front` varchar(255) comment '身份证正面照片url',
  `id_card_back` varchar(255) comment '身份证反面照片url',
  `auth_status` tinyint default 1 comment '认证状态：1-待审核，2-已认证，3-认证失败',
  `audit_admin_id` bigint comment '审核管理员id',
  `audit_time` datetime comment '审核时间',
  `fail_reason` varchar(500) comment '审核未通过原因',
  `auth_time` datetime default current_timestamp comment '发起认证时间',
  `create_time` datetime default current_timestamp comment '创建时间',
  primary key (`id`),
  key `idx_user_id` (`user_id`),
  key `idx_auth_time` (`auth_time`)
) engine = innodb default charset = utf8mb4 comment = '用户实名认证';

create table `im_message_deletion` (
  `id` bigint not null auto_increment primary key comment 'id',
  `user_id` bigint not null comment '用户id',
  `chat_type` tinyint not null comment '会话类型 1:私聊 2:群聊',
  `chat_id` bigint not null comment '好友id、群聊id',
  `message_id` bigint(20) comment '消息id',
  `delete_type` tinyint not null comment '删除类型 1:按消息删除 2:按会话删除',
  `delete_time` datetime default current_timestamp comment '消息删除时间',
  key `idx_user_id` (`user_id`)
) engine = innodb charset = utf8mb4 comment '消息删除记录';