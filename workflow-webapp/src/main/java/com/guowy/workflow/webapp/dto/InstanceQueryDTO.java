package com.guowy.workflow.webapp.dto;

import com.guowy.workflow.dto.BaseQueryDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author LiJingTang
 * @date 2020-05-15 14:35
 */
@Data
@ApiModel("流程实例查询参数")
public class InstanceQueryDTO extends BaseQueryDTO {

    private static final long serialVersionUID = 2387161375184538196L;

    /**
     * 流程定义KEY
     */
    @ApiModelProperty(value = "流程定义KEY", required = true)
    @NotBlank(message = "流程定义KEY为空")
    private String processKey;
    /**
     * 业务ID
     */
    @ApiModelProperty(value = "业务ID")
    private String bizId;
    /**
     * 业务KEY
     */
    @ApiModelProperty(value = "业务KEY")
    private String bizKey;
    /**
     * 是否已完成
     */
    @ApiModelProperty(value = "是否已完成")
    private Boolean finished;

}
