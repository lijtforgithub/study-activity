package com.guowy.workflow.webapp.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author LiJingTang
 * @date 2020-05-15 14:48
 */
@Getter
@AllArgsConstructor
public enum InstanceStatusEnum {

    /**
     * 实例状态
     */
    RUN(1, "执行中"),
    SUSPENDED(2, "已挂起"),
    CANCELED(9, "已取消"),
    END(10, "已结束");

    private final int value;
    private final String desc;

}
