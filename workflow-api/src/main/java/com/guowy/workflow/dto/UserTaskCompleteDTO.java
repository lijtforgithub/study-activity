package com.guowy.workflow.dto;

import com.guowy.cloud.common.validator.FlagValidator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;

/**
 * 办理用户任务入参
 *
 * @author LiJingTang
 * @date 2020-05-17 20:26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("办理用户任务入参")
public class UserTaskCompleteDTO implements Serializable {

    private static final long serialVersionUID = 7428160183113710367L;

    /**
     * 任务ID
     */
    @ApiModelProperty(value = "任务ID", required = true)
    @NotBlank(message = "任务ID为空")
    private String taskId;
    /**
     * 审核通过
     */
    @ApiModelProperty(value = "审核通过", required = true)
    @NotNull(message = "审核状态为空")
    private Boolean pass;
    /**
     * 审核意见
     */
    @ApiModelProperty(value = "审核意见", required = true)
    @NotBlank(message = "审核意见为空")
    private String content;
    /**
     * 操作人
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
    /**
     * 任务变量
     */
    @ApiModelProperty(value = "流程变量")
    @SuppressWarnings("squid:S1948")
    private Map<String, Object> varMap;

}
