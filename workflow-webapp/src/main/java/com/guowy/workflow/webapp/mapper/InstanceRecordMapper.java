package com.guowy.workflow.webapp.mapper;

import com.guowy.workflow.webapp.dto.InstanceRecordDTO;

import java.util.List;
import java.util.Map;

/**
 * @author LiJingTang
 * @date 2020-05-15 11:05
 */
public interface InstanceRecordMapper {

    /**
     * 保存流程实例
     *
     * @param infoDTO 信息实体
     * @return 保存记录数
     */
    int insert(InstanceRecordDTO infoDTO);

    /**
     * 根据条件查询实例信息
     *
     * @param map 参数
     * @return 实例信息
     */
    List<InstanceRecordDTO> select(Map<String, Object> map);

    /**
     * 根据流程实例ID查询执行ID和状态
     *
     * @param instanceId 实例ID
     * @return 执行ID和状态
     */
    List<InstanceRecordDTO> selectStatus(String instanceId);

    /**
     * 根据流程实例ID删除
     *
     * @param instanceId 实例ID
     */
    void deleteByInstanceId(String instanceId);

}
