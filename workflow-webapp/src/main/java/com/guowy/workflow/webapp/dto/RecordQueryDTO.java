package com.guowy.workflow.webapp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @author LiJingTang
 * @date 2020-05-25 09:57
 */
@Data
@ApiModel("流程审批记录查询参数")
public class RecordQueryDTO implements Serializable {

    private static final long serialVersionUID = -6107035995851564554L;

    public RecordQueryDTO(String processKey, String bizId) {
        this.processKey = processKey;
        this.bizId = bizId;
    }

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
     * 是否包含启动类型
     */
    @ApiModelProperty(value = "是否包含启动类型")
    private Boolean containsStart;

}
