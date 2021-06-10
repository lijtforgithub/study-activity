package com.guowy.workflow.webapp.vo;

import com.guowy.workflow.webapp.dto.InstanceRecordDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author LiJingTang
 * @date 2020-05-25 09:53
 */
@Data
@ApiModel("审批记录")
public class RecordVO implements Serializable {

    private static final long serialVersionUID = 612940607419086008L;

    /**
     * 用户ID
     */
    @ApiModelProperty(value = "用户ID")
    private Long userId;
    /**
     * 用户名
     */
    @ApiModelProperty(value = "用户名")
    private String userName;
    /**
     * 用户类型（0-未知/1-内部员工/2-商家账号）
     */
    @ApiModelProperty(value = "用户类型")
    private Integer userType;
    /**
     * 类型（0-开始/1-用户任务）
     */
    @ApiModelProperty(value = "类型（0-开始/1-用户任务）")
    private Integer type;
    /**
     * 状态（0-驳回/1-通过）
     */
    @ApiModelProperty(value = "状态（0-驳回/1-通过）")
    private Integer status;
    /**
     * 内容
     */
    @ApiModelProperty(value = "内容")
    private String content;
    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    private Long createTime;

    public static RecordVO build(InstanceRecordDTO recordDTO) {
        if (Objects.isNull(recordDTO)) {
            return null;
        }

        RecordVO vo = new RecordVO();
        BeanUtils.copyProperties(recordDTO, vo);
        return vo;
    }

}
