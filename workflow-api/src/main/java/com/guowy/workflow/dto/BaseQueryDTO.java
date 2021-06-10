package com.guowy.workflow.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author LiJingTang
 * @date 2020-05-14 09:12
 */
@Data
@ApiModel("分页查询属性")
public class BaseQueryDTO implements Serializable {

    private static final long serialVersionUID = -5310890178911484252L;

    /**
     * 页码
     */
    @ApiModelProperty(value = "页码", required = true)
    private Integer pageNum;
    /**
     * 每页大小
     */
    @ApiModelProperty(value = "每页大小", required = true)
    private Integer pageSize;

}
