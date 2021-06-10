package com.guowy.workflow.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;

/**
 * @author LiJingTang
 * @date 2020-05-19 18:22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("办理接收任务入参")
public class ReceiveTaskCompleteDTO implements Serializable {

    private static final long serialVersionUID = 2852617457662853197L;

    /**
     * 流程定义KEY
     */
    @ApiModelProperty(value = "流程定义KEY", required = true)
    @NotBlank(message = "流程定义KEY为空")
    private String processKey;
    /**
     * 业务ID
     */
    @ApiModelProperty(value = "业务ID", required = true)
    @NotBlank(message = "业务ID为空")
    private String bizId;
    /**
     * 任务定义KEY
     */
    @ApiModelProperty(value = "任务定义KEY")
    private String taskKey;
    /**
     * 审核通过
     */
    @ApiModelProperty(value = "审核通过", required = true)
    @NotNull(message = "审核状态为空")
    private Boolean pass;
    /**
     * 任务变量
     */
    @ApiModelProperty(value = "任务变量")
    @SuppressWarnings("squid:S1948")
    private Map<String, Object> varMap;

}
