package com.guowy.workflow.webapp.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author LiJingTang
 * @date 2020-05-26 10:52
 */
@Data
@ApiModel("已办理任务")
public class HistoryUserTaskVO implements Serializable {

    private static final long serialVersionUID = 5816721035098910224L;

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
     * 办理人
     */
    @ApiModelProperty(value = "办理人")
    private String assignee;
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
     * 开始时间
     */
    @ApiModelProperty(value = "开始时间")
    private Long createTime;
    /**
     * 结束时间
     */
    @ApiModelProperty(value = "结束时间")
    private Long endTime;


    public static HistoryUserTaskVO build(HistoricTaskInstance task, HistoricProcessInstance instance) {
        if (Objects.isNull(task)) {
            return null;
        }

        HistoryUserTaskVO vo = new HistoryUserTaskVO();
        vo.setTaskId(task.getId());
        vo.setTaskName(task.getName());
        vo.setAssignee(task.getAssignee());
        vo.setInstanceId(task.getProcessInstanceId());
        vo.setProcessId(task.getProcessDefinitionId());
        vo.setCreateTime(task.getStartTime().getTime());
        if (Objects.nonNull(task.getEndTime())) {
            vo.setEndTime(task.getEndTime().getTime());
        }
        if (Objects.nonNull(instance)) {
            vo.setProcessName(instance.getProcessDefinitionName());
            vo.setBizId(instance.getBusinessKey());
            vo.setBizKey(instance.getName());
        }

        return vo;
    }

}
