package com.guowy.workflow.dto;

import com.guowy.cloud.common.rule.MQNamedRule;
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
 * 启动流程入参
 *
 * @author LiJingTang
 * @date 2020-05-15 09:50
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("启动流程入参")
public class StartProcessDTO implements Serializable {

    public static final MQNamedRule START_QUEUE_RULE = MQNamedRule.builder().namespace("workflow")
            .srcApp("app").sendClass("StartProcessSend")
            .destApp("wfWeb").recvClass("StartProcessRecv");

    private static final long serialVersionUID = 5377859992937565763L;

    /**
     * 流程定义KEY
     */
    @ApiModelProperty(value = "流程定义KEY", required = true)
    @NotBlank(message = "流程定义KEY为空")
    private String processKey;
    /**
     * 业务ID 用作关联
     */
    @ApiModelProperty(value = "业务ID", required = true)
    @NotBlank(message = "业务ID为空")
    private String bizId;
    /**
     * 业务关键字 可用作查询
     */
    @ApiModelProperty(value = "业务关键字")
    private String bizKey;
    /**
     * 提醒审批人消息
     */
    @ApiModelProperty(value = "提醒审批人消息")
    private String message;
    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;
    /**
     * 提交人
     */
    @ApiModelProperty(value = "提交人ID")
    private Long userId;
    @ApiModelProperty(value = "提交人名称")
    private String userName;
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
     * 流程变量
     */
    @ApiModelProperty(value = "流程变量")
    @SuppressWarnings("squid:S1948")
    private Map<String, Object> varMap;

}
