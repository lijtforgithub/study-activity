package com.guowy.workflow.webapp.dto;

import lombok.Data;

/**
 * @author LiJingTang
 * @date 2020-05-15 11:08
 */
@Data
public class InstanceRecordDTO {
    /**
     * 主键
     */
    private Long id;
    /**
     * 流程KEY
     */
    private String processKey;
    /**
     * 流程实例ID
     */
    private String instanceId;
    /**
     * 任务KEY
     */
    private String taskKey;
    /**
     * 执行ID
     */
    private String executionId;
    /**
     * 业务ID
     */
    private String bizId;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 用户名
     */
    private String userName;
    /**
     * 用户类型（0-未知/1-内部员工/2-商家账号）
     */
    private Integer userType;
    /**
     * 类型（0-开始/1-用户任务/2-接收任务）
     */
    private Integer type;
    /**
     * 状态（0-驳回/1-通过）
     */
    private Integer status;
    /**
     * 内容
     */
    private String content;
    /**
     * 创建时间
     */
    private Long createTime;

}
