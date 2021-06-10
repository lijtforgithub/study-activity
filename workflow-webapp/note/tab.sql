CREATE TABLE `gwy_instance_record`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `process_key`  varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '流程定义KEY',
    `biz_id`       varchar(50)                                      NOT NULL DEFAULT '' COMMENT '业务ID',
    `instance_id`  varchar(64) CHARACTER SET utf8 COLLATE utf8_bin  NOT NULL COMMENT '流程实例ID',
    `execution_id` varchar(64)                                               DEFAULT '' COMMENT '执行ID',
    `task_key`     varchar(100) CHARACTER SET utf8 COLLATE utf8_bin          DEFAULT NULL COMMENT '任务KEY',
    `user_id`      bigint(20) unsigned DEFAULT NULL COMMENT '用户ID',
    `user_name`    varchar(20) CHARACTER SET utf8 COLLATE utf8_bin           DEFAULT NULL COMMENT '用户名',
    `user_type`    tinyint(1) NOT NULL COMMENT '用户类型（0-未知/1-内部员工/2-商家账号）',
    `type`         tinyint(1) NOT NULL COMMENT '类型（0-开始/1-用户任务/2-接收任务）',
    `status`       tinyint(1) NOT NULL COMMENT '状态（0-驳回/1-通过）',
    `content`      varchar(200) CHARACTER SET utf8 COLLATE utf8_bin          DEFAULT '' COMMENT '内容',
    `create_time`  bigint(20) unsigned NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY            `idx_process` (`process_key`) USING BTREE,
    KEY            `idx_instance` (`instance_id`) USING BTREE,
    KEY            `idx_biz` (`biz_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;