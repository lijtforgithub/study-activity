package com.guowy.workflow.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author LiJingTang
 * @date 2020-05-19 20:47
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("办理接收任务返回值")
public class ReceiveTaskResponseDTO implements Serializable {

    private static final long serialVersionUID = -7702640596071444154L;

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

}
