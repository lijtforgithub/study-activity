package com.guowy.workflow.webapp.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.activiti.bpmn.model.GraphicInfo;

import java.io.Serializable;

/**
 * @author LiJingTang
 * @date 2020-05-15 16:11
 */
@Data
@ApiModel("节点坐标")
public class GraphicVO implements Serializable {

    private static final long serialVersionUID = 5650367338149865604L;

    /**
     * 左边距
     */
    @ApiModelProperty(value = "左边距")
    private Double left;
    /**
     * 上边距
     */
    @ApiModelProperty(value = "上边距")
    private Double top;
    /**
     * 宽度
     */
    @ApiModelProperty(value = "宽度")
    private Double width;
    /**
     * 高度
     */
    @ApiModelProperty(value = "高度")
    private Double height;

    public static GraphicVO build(GraphicInfo graphic ) {
        GraphicVO vo = new GraphicVO();
        vo.setTop(graphic.getY());
        vo.setLeft(graphic.getX());
        vo.setWidth(graphic.getWidth());
        vo.setHeight(graphic.getHeight());

        return vo;
    }

}
