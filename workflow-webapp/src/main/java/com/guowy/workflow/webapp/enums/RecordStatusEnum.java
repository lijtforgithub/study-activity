package com.guowy.workflow.webapp.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author LiJingTang
 * @date 2020-05-15 11:43
 */
@Getter
@AllArgsConstructor
public enum RecordStatusEnum {

    /**
     * 驳回
     */
    REJECT(0),
    /**
     * 通过
     */
    PASS(1);

    /**
     * 值
     */
    private final int value;

}
