package com.guowy.workflow.dto;

import com.guowy.cloud.common.validator.FlagValidator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author LiJingTang
 * @date 2020-05-18 08:42
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("查询待办用户任务入参")
public class UserTaskQueryDTO extends BaseQueryDTO {

    private static final long serialVersionUID = 5316404426250280264L;

    /**
     * 流程定义KEY
     */
    @ApiModelProperty(value = "流程定义KEY")
    private List<String> processKeys;
    /**
     * 业务ID 用作关联
     */
    @ApiModelProperty(value = "业务ID")
    private String bizId;
    /**
     * 业务关键字 可用作查询
     */
    @ApiModelProperty(value = "业务关键字")
    private String bizKey;
    /**
     * 提交人
     */
    @ApiModelProperty(value = "办理人ID")
    private Long userId;
    /**
     * 用户类型
     *
     * @see com.guowy.cloud.security.enums.UserTypeEnum
     */
    @ApiModelProperty(value = "用户类型")
    @FlagValidator(value = {"0", "1", "2"}, message = "用户类型不合法")
    private Integer userType;

}
