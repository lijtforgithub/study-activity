package com.guowy.workflow.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author LiJingTang
 * @date 2020-05-18 08:38
 */
@Data
@ApiModel("待办用户任务返回参数")
public class UserTaskDTO implements Serializable {

    private static final long serialVersionUID = -6661282878549084277L;

    /**
     * 任务ID
     */
    @ApiModelProperty(value = "任务ID")
    private String taskId;
    /**
     * 任务名称
     */
    @ApiModelProperty(value = "任务名称")
    private String taskName;
    /**
     * 流程实例ID
     */
    @ApiModelProperty(value = "流程实例ID")
    private String instanceId;
    /**
     * 流程定义ID
     */
    @ApiModelProperty(value = "流程定义ID")
    private String processId;
    /**
     * 流程名称
     */
    @ApiModelProperty(value = "流程名称")
    private String processName;
    /**
     * 业务ID
     */
    @ApiModelProperty(value = "业务ID")
    private String bizId;
    /**
     * 业务关键字
     */
    @ApiModelProperty(value = "业务关键字")
    private String bizKey;
    /**
     * 审核URL
     */
    @ApiModelProperty(value = "审核URL")
    private String auditUrl;
    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    private Long createTime;
    /**
     * 任务办理人 如果查询指定了办理人 则不返回此字段
     */
    @ApiModelProperty(value = "任务办理人")
    private List<TaskUser> users;

    @Data
    @ApiModel("任务办理人")
    public static class TaskUser implements Serializable {

        private static final long serialVersionUID = 5568042767447636616L;

        /**
         * 用户类型
         */
        @ApiModelProperty(value = "用户类型")
        private Integer userType;
        /**
         * 用户ID
         */
        @ApiModelProperty(value = "用户ID")
        private Long userId;
        /**
         * 用户名称
         */
        @ApiModelProperty(value = "用户名称")
        private String userName;

    }

}
