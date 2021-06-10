package com.guowy.workflow.webapp.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.activiti.engine.history.HistoricProcessInstance;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * @author LiJingTang
 * @date 2020-05-15 14:33
 */
@Data
@ApiModel("流程实例")
public class InstanceVO implements Serializable {

    private static final long serialVersionUID = -4033563706475793419L;

    /**
     * 实例ID
     */
    @ApiModelProperty(value = "实例ID")
    private String id;
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
     * 提醒消息
     */
    @ApiModelProperty(value = "提醒消息")
    private String message;
    /**
     * 状态
     */
    @ApiModelProperty(value = "状态 1-执行中/2-已挂起/9-已作废/10-已结束")
    private Integer status;
    /**
     * 作废原因
     */
    @ApiModelProperty(value = "作废原因")
    private String reason;
    /**
     * 实例变量
     */
    @ApiModelProperty(value = "实例变量")
    private Map<String, String> varMap;
    /**
     * 流程定义ID
     */
    @ApiModelProperty(value = "流程定义ID")
    private String processId;
    /**
     * 流程定义KEY
     */
    @ApiModelProperty(value = "流程定义KEY")
    private String processKey;
    /**
     * 流程定义名称
     */
    @ApiModelProperty(value = "流程定义名称")
    private String processName;
    /**
     * 开始时间
     */
    @ApiModelProperty(value = "开始时间")
    private Long startTime;
    /**
     * 结束时间
     */
    @ApiModelProperty(value = "结束时间")
    private Long endTime;

    public static InstanceVO build(HistoricProcessInstance instance) {
        if (Objects.isNull(instance)) {
            return null;
        }

        InstanceVO vo = new InstanceVO();
        vo.setId(instance.getId());
        vo.setBizId(instance.getBusinessKey());
        vo.setBizKey(instance.getName());
        vo.setReason(instance.getDeleteReason());
        vo.setProcessId(instance.getProcessDefinitionId());
        vo.setProcessKey(instance.getProcessDefinitionKey());
        vo.setProcessName(instance.getProcessDefinitionName());
        vo.setStartTime(instance.getStartTime().getTime());

        return vo;
    }

}
