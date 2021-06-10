package com.guowy.workflow.webapp.vo;

import com.guowy.workflow.dto.UserTaskDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.List;

/**
 * @author LiJingTang
 * @date 2020-05-29 13:48
 */
@Data
@ApiModel("流程实例坐标及审核记录")
public class ImageUserTaskVO implements Serializable {

    private static final long serialVersionUID = 4533656838255907815L;

    /**
     * 任务KEY
     */
    @ApiModelProperty(value = "任务KEY")
    private String taskKey;
    /**
     * 任务名称
     */
    @ApiModelProperty(value = "任务名称")
    private String taskName;
    /**
     * 坐标
     */
    @ApiModelProperty(value = "节点坐标")
    private GraphicVO graphic;
    /**
     * 审批记录
     */
    @ApiModelProperty(value = "审批记录")
    private List<RecordVO> records;
    /**
     * 审批次数
     */
    @ApiModelProperty(value = "审批次数")
    private Integer count;
    /**
     * 审批人
     */
    @ApiModelProperty(value = "审批人")
    private List<UserTaskDTO.TaskUser> users;

    public Integer getCount() {
        return CollectionUtils.isEmpty(records) ? 0 : records.size();
    }

}
