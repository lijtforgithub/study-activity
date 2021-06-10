package com.guowy.workflow.webapp.dto;

import com.guowy.cloud.common.validator.FlagValidator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 加签/指定办理人 入参
 *
 * @author LiJingTang
 * @date 2020-05-21 19:40
 */
@Data
@ApiModel("加签/指定办理人 入参")
public class UserTaskSignDTO implements Serializable {

    private static final long serialVersionUID = 4127607924683750973L;

    /**
     * 任务ID
     */
    @ApiModelProperty(value = "任务ID", required = true)
    @NotBlank(message = "任务ID为空")
    private String taskId;
    /**
     * 审核人ID
     */
    @ApiModelProperty(value = "审核人ID", required = true)
    @NotNull(message = "审核人ID为空")
    private Long userId;
    /**
     * 用户类型
     *
     * @see com.guowy.cloud.security.enums.UserTypeEnum
     */
    @ApiModelProperty(value = "用户类型", required = true)
    @NotNull(message = "用户类型为空")
    @FlagValidator(value = {"0", "1", "2"}, message = "用户类型不合法")
    private Integer userType;

}
