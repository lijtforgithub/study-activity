package com.guowy.workflow.webapp.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @author LiJingTang
 * @date 2020-05-13 19:30
 */
@Data
@ApiModel("修改模型入参")
public class ModelUpdateDTO implements Serializable {

    private static final long serialVersionUID = 2489707572382284841L;

    /**
     * 流程模型ID
     */
    private String id;
    /**
     * 流程模型名称
     */
    @NotBlank(message = "流程模型名称为空")
    private String name;
    /**
     * 流程模型描述
     */
    private String description;
    /**
     * 详细信息
     */
    @NotBlank(message = "流程模型详细信息为空")
    @SuppressWarnings("squid:S00116")
    private String json_xml;
    /**
     * 图片信息
     */
    @NotBlank(message = "流程模型图片信息为空")
    @SuppressWarnings("squid:S00116")
    private String svg_xml;

}
