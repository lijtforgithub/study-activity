package com.guowy.workflow.webapp.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author LiJingTang
 * @date 2020-05-14 18:10
 */
@Data
@ApiModel("流程定义")
public class ProcessVO implements Serializable {

    private static final long serialVersionUID = 3253415612894603710L;

    /**
     * 流程定义ID
     */
    @ApiModelProperty(value = "流程定义ID")
    private String id;
    /**
     * 流程 KEY
     */
    @ApiModelProperty(value = "流程KEY")
    private String key;
    /**
     * 流程模型名称
     */
    @ApiModelProperty(value = "流程名称")
    private String name;
    /**
     * 分类
     */
    @ApiModelProperty(value = "分类")
    private String category;
    /**
     * 是否挂起
     */
    @ApiModelProperty(value = "是否挂起")
    private Boolean suspended;
    /**
     * 创建人
     */
    @ApiModelProperty(value = "创建人")
    private String creator;
    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    private Long createTime;
    /**
     * 版本号
     */
    @ApiModelProperty(value = "版本号")
    private Integer version;
    /**
     * 描述
     */
    @ApiModelProperty(value = "描述")
    private String description;
    /**
     * 业务审核URL
     */
    @ApiModelProperty(value = "业务审核URL")
    private String auditUrl;

    public static ProcessVO build(ProcessDefinition process, Deployment deployment, String auditUrl) {
        if (Objects.isNull(process)) {
            return null;
        }

        ProcessVO vo = new ProcessVO();
        BeanUtils.copyProperties(process, vo);
        vo.setAuditUrl(auditUrl);
        if (Objects.nonNull(deployment)) {
            vo.setCreator(deployment.getName());
            vo.setCreateTime(deployment.getDeploymentTime().getTime());
        }
        return vo;
    }

}
