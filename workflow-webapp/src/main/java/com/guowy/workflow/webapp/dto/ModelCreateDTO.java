package com.guowy.workflow.webapp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * @author LiJingTang
 * @date 2020-05-13 17:01
 */
@Data
@ApiModel("流程模型创建参数")
public class ModelCreateDTO implements Serializable {

    private static final long serialVersionUID = 8617103741002312304L;

    /**
     * 流程KEY
     */
    @ApiModelProperty(value = "流程KEY", required = true)
    @NotBlank(message = "流程KEY为空")
    @Size(max = 100, message = "流程KEY太长")
    private String key;
    /**
     * 流程名称
     */
    @ApiModelProperty(value = "流程名称", required = true)
    @NotBlank(message = "流程名称为空")
    @Size(max = 100, message = "流程名称太长")
    private String name;
    /**
     * 分类
     */
    @ApiModelProperty(value = "流程分类", required = true)
    @NotBlank(message = "流程分类为空")
    @Size(max = 100, message = "流程分类太长")
    private String category;
    /**
     * 流程描述
     */
    @ApiModelProperty("流程模型描述")
    private String description;

}
