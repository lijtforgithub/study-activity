package com.guowy.workflow.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author LiJingTang
 * @date 2020-05-19 20:20
 */
@Data
@ApiModel("办理用户任务返回值")
public class UserTaskResponseDTO implements Serializable {

    private static final long serialVersionUID = 6622366862631257328L;

    /**
     * 流程实例是否结束
     */
    @ApiModelProperty(value = "流程实例是否结束")
    private Boolean instanceEnd;
    /**
     * 办理的任务节点KEY
     */
    @ApiModelProperty(value = "办理的任务节点KEY")
    private String taskKey;
    /**
     * 办理的任务节点是否结束 多实例场景
     */
    @ApiModelProperty(value = "办理的任务节点是否结束")
    private Boolean taskEnd;

}
