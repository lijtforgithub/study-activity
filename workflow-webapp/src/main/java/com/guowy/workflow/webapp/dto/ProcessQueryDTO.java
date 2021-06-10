package com.guowy.workflow.webapp.dto;

import com.guowy.workflow.dto.BaseQueryDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author LiJingTang
 * @date 2020-05-14 18:09
 */
@Data
@ApiModel("流程定义查询参数")
public class ProcessQueryDTO extends BaseQueryDTO {

    private static final long serialVersionUID = 3458932775984902423L;

    /**
     * 流程KEY
     */
    @ApiModelProperty("流程KEY")
    private String key;
    /**
     * 流程名称
     */
    @ApiModelProperty("流程名称")
    private String name;
    /**
     * 分类
     */
    @ApiModelProperty("分类")
    private String category;
    /**
     * 是否启用
     */
    @ApiModelProperty("是否启用")
    private Boolean enable;

}
