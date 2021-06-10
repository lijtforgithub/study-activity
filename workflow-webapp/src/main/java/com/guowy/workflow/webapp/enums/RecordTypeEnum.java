package com.guowy.workflow.webapp.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author LiJingTang
 * @date 2020-05-15 11:43
 */
@Getter
@AllArgsConstructor
public enum RecordTypeEnum {

    /**
     * 创建
     */
    START(0),
    /**
     * 用户任务
     */
    USER_TASK(1),
    /**
     * 接收任务
     */
    RECEIVE_TASK(2);

    /**
     * 值
     */
    private final int value;

}
