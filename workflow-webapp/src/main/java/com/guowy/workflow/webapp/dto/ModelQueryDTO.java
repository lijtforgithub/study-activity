package com.guowy.workflow.webapp.dto;

import com.guowy.workflow.dto.BaseQueryDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author LiJingTang
 * @date 2020-05-14 09:17
 */
@Data
@ApiModel("流程模型查询参数")
public class ModelQueryDTO extends BaseQueryDTO {

    private static final long serialVersionUID = 3705371915164181206L;

    /**
     * 流程KEY
     */
    @ApiModelProperty(value = "流程KEY")
    private String key;
    /**
     * 流程模型名称
     */
    @ApiModelProperty(value = "流程模型名称")
    private String name;
    /**
     * 分类
     */
    @ApiModelProperty(value = "分类")
    private String category;

}
