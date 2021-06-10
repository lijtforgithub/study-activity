package com.guowy.workflow.webapp.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author LiJingTang
 * @date 2020-05-15 14:00
 */
@Getter
@AllArgsConstructor
public enum ResponseTypeEnum {

    /**
     * 流程实例 创建即结束
     */
    INSTANCE_END(1, "流程实例结束");

    /**
     * 值
     */
    private final int value;
    /**
     * 描述
     */
    private final String desc;

}
