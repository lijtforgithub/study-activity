package com.guowy.workflow.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * @author LiJingTang
 * @date 2020-05-18 13:24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("取消流程实例入参")
public class CancelInstanceDTO implements Serializable {

    private static final long serialVersionUID = -1063904656317255209L;

    /**
     * 流程KEY
     */
    @ApiModelProperty(value = "流程KEY", required = true)
    @NotBlank(message = "流程KEY为空")
    private String processKey;
    /**
     * 原因
     */
    @ApiModelProperty(value = "原因", required = true)
    @NotBlank(message = "原因为空")
    private String reason;
    /**
     * 操作人
     */
    @ApiModelProperty(value = "操作人", required = true)
    @NotBlank(message = "操作人为空")
    private String operator;
    /**
     * 业务ID
     */
    @ApiModelProperty(value = "业务ID", required = true)
    @NotNull(message = "业务ID为空")
    @Size(min = 1, message = "业务ID为空")
    private String[] bizIds;

}
