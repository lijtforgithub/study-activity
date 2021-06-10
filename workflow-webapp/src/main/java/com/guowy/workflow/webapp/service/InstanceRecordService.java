package com.guowy.workflow.webapp.service;

import com.guowy.workflow.webapp.dto.InstanceRecordDTO;

import java.util.List;
import java.util.Map;

/**
 * @author LiJingTang
 * @date 2020-05-15 11:32
 */
public interface InstanceRecordService {

    /**
     * 保存实例相关信息
     *
     * @param infoDTO 实体
     */
    void save(InstanceRecordDTO infoDTO);

    /**
     * 根据实例ID查询
     *
     * @param instanceId 实例ID
     * @param containsStart 是否包含启动类型
     * @return 实例信息
     */
    List<InstanceRecordDTO> findByInstance(String instanceId, boolean containsStart);

    /**
     * 根据流程key和业务ID查询
     *
     * @param processKey 流程key
     * @param bizId 业务ID
     * @param containsStart 是否包含启动类型
     * @return 实例信息
     */
    List<InstanceRecordDTO> findByBiz(String processKey, String bizId, boolean containsStart);

    /**
     * 根据业务ID查询执行Id状态
     *
     * @param instanceId 流程实例ID
     * @return 状态
     */
    Map<String, Integer> findStatusByInstance(String instanceId);

    /**
     * 根据流程实例ID删除
     *
     * @param instanceId 实例ID
     */
    void deleteByInstanceId(String instanceId);

}
