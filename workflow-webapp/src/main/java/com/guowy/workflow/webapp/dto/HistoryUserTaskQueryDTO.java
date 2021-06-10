package com.guowy.workflow.webapp.dto;

import com.guowy.workflow.dto.BaseQueryDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author LiJingTang
 * @date 2020-05-26 10:57
 */
@Data
@ApiModel("已办理任务查询参数")
public class HistoryUserTaskQueryDTO extends BaseQueryDTO {

    private static final long serialVersionUID = 7191123949301573525L;

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
     * 流程定义KEY
     */
    @ApiModelProperty(value = "流程定义KEY")
    private String processKey;
    /**
     * 业务ID
     */
    @ApiModelProperty(value = "业务ID")
    private String bizId;

}
