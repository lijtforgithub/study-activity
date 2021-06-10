package com.guowy.workflow.webapp.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * @author LiJingTang
 * @date 2020-05-21 17:40
 */
public interface HistoryActinstMapper {

    /**
     * 手动删除历史节点
     *
     * @param instanceId 实例ID
     * @param reason 删除原因
     */
    void delete(@Param("instanceId") String instanceId, @Param("reason") String reason);

}
