package com.guowy.workflow.webapp.service.impl;

import com.google.common.collect.Lists;
import com.guowy.cloud.crud.utils.CommonUtils;
import com.guowy.workflow.webapp.dto.InstanceRecordDTO;
import com.guowy.workflow.webapp.enums.RecordTypeEnum;
import com.guowy.workflow.webapp.mapper.InstanceRecordMapper;
import com.guowy.workflow.webapp.service.InstanceRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author LiJingTang
 * @date 2020-05-15 11:46
 */
@Slf4j
@Service
public class InstanceRecordServiceImpl implements InstanceRecordService {

    @Autowired
    private InstanceRecordMapper recordMapper;

    @Override
    public void save(InstanceRecordDTO infoDTO) {
        recordMapper.insert(infoDTO);
    }

    @Override
    public List<InstanceRecordDTO> findByInstance(String instanceId, boolean containsStart) {
        if (StringUtils.isBlank(instanceId)) {
            log.info("instanceId={} 为空", instanceId);
            return new ArrayList<>();
        }

        InstanceRecordDTO infoDTO = new InstanceRecordDTO();
        infoDTO.setInstanceId(instanceId);

        return find(infoDTO, getTypes(containsStart));
    }

    @Override
    public List<InstanceRecordDTO> findByBiz(String processKey, String bizId, boolean containsStart) {
        if (StringUtils.isAnyBlank(processKey, bizId)) {
            log.info("processKey={} 或 bizId={} 为空", processKey, bizId);
            return new ArrayList<>();
        }

        InstanceRecordDTO infoDTO = new InstanceRecordDTO();
        infoDTO.setProcessKey(processKey);
        infoDTO.setBizId(bizId);

        return find(infoDTO, getTypes(containsStart));
    }

    @Override
    public Map<String, Integer> findStatusByInstance(String instanceId) {
        if (StringUtils.isBlank(instanceId)) {
            log.info("instanceId={} 为空", instanceId);
            return Collections.emptyMap();
        }

        return recordMapper.selectStatus(instanceId).stream().collect(Collectors
                .toMap(r -> r.getExecutionId() + r.getTaskKey(), InstanceRecordDTO::getStatus, (v1, v2) -> v2));
    }

    @Override
    public void deleteByInstanceId(String instanceId) {
        if (StringUtils.isBlank(instanceId)) {
            log.warn("流程实例ID={}为空", instanceId);
            return;
        }

        recordMapper.deleteByInstanceId(instanceId);
    }

    private static List<RecordTypeEnum> getTypes(boolean containsStart) {
        List<RecordTypeEnum> typeEnums = Lists.newArrayList(RecordTypeEnum.USER_TASK);
        if (containsStart) {
            typeEnums.add(RecordTypeEnum.START);
        }
        return typeEnums;
    }

    private List<InstanceRecordDTO> find(InstanceRecordDTO infoDTO, List<RecordTypeEnum> typeEnums) {
        Map<String, Object> map = CommonUtils.getFieldVals(infoDTO);
        if (!CollectionUtils.isEmpty(typeEnums)) {
            map.put("types", typeEnums.stream().map(RecordTypeEnum::getValue).collect(Collectors.toList()));
        }

        return recordMapper.select(map);
    }

}
