package com.guowy.workflow.webapp.vo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.guowy.workflow.webapp.dto.ModelCreateDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.activiti.engine.repository.Model;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.Objects;

import static com.guowy.workflow.webapp.constant.Constant.CREATOR;

/**
 * @author LiJingTang
 * @date 2020-05-13 16:03
 */
@Data
@ApiModel("流程模型")
public class ModelVO extends ModelCreateDTO {

    private static final long serialVersionUID = -3481767687572633303L;

    /**
     * 模型 ID
     */
    @ApiModelProperty(value = "模型ID")
    private String id;
    /**
     * 流程 KEY
     */
    @ApiModelProperty(value = "流程KEY")
    private String key;
    /**
     * 流程模型名称
     */
    @ApiModelProperty(value = "流程模型名称")
    private String name;
    /**
     * 分类
     */
    @ApiModelProperty(value = "流程分类")
    private String category;
    /**
     * 部署ID
     */
    @ApiModelProperty(value = "部署ID")
    private String deploymentId;
    /**
     * 最后操作人
     */
    @ApiModelProperty(value = "最后操作人")
    private String updater;
    /**
     * 最后更新时间
     */
    @ApiModelProperty(value = "最后更新时间")
    private Long updateTime;
    /**
     * 更新次数
     */
    @ApiModelProperty(value = "更新次数")
    private Integer version;

    public static ModelVO build(Model model) {
        if (Objects.isNull(model)) {
            return null;
        }

        ModelVO vo = new ModelVO();
        BeanUtils.copyProperties(model, vo);
        if (StringUtils.isNotBlank(model.getMetaInfo())) {
            JSONObject object = JSON.parseObject(model.getMetaInfo());
            vo.setUpdater(String.valueOf(object.get(CREATOR)));
            Date date = ObjectUtils.defaultIfNull(model.getLastUpdateTime(), model.getCreateTime());
            vo.setUpdateTime(date.getTime());
        }

        return vo;
    }

}
