package com.guowy.workflow.webapp.service;

import java.util.List;

/**
 * @author LiJingTang
 * @date 2020-05-14 11:09
 */
public interface CustomOperateService {

    /**
     * 查询流程模型所有key
     * @return key
     */
    List<String> findAllModelKey();

    /**
     * 终止流程实例
     *
     * @param instanceId 实例ID
     * @param reason 原因
     */
    void terminateInstance(String instanceId, String reason);

}
