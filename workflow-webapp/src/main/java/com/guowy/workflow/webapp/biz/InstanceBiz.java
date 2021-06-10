package com.guowy.workflow.webapp.biz;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.guowy.cloud.common.enums.StatusEnum;
import com.guowy.cloud.common.util.JsonResult;
import com.guowy.cloud.security.context.UserContextHolder;
import com.guowy.cloud.security.enums.UserTypeEnum;
import com.guowy.workflow.dto.CancelInstanceDTO;
import com.guowy.workflow.dto.StartProcessDTO;
import com.guowy.workflow.webapp.dto.InstanceQueryDTO;
import com.guowy.workflow.webapp.dto.InstanceRecordDTO;
import com.guowy.workflow.webapp.dto.RecordQueryDTO;
import com.guowy.workflow.webapp.enums.InstanceStatusEnum;
import com.guowy.workflow.webapp.enums.RecordStatusEnum;
import com.guowy.workflow.webapp.enums.RecordTypeEnum;
import com.guowy.workflow.webapp.enums.ResponseTypeEnum;
import com.guowy.workflow.webapp.mq.send.ResponseSend;
import com.guowy.workflow.webapp.service.InstanceRecordService;
import com.guowy.workflow.webapp.util.PageUtils;
import com.guowy.workflow.webapp.util.ParamUtils;
import com.guowy.workflow.webapp.vo.InstanceVO;
import com.guowy.workflow.webapp.vo.RecordVO;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.guowy.workflow.webapp.constant.Constant.*;
import static com.guowy.workflow.webapp.util.WorkFlowUtils.joinTaskUser;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author LiJingTang
 * @date 2020-05-15 09:47
 */
@Slf4j
@Service
public class InstanceBiz {

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private InstanceRecordService recordService;
    @Autowired
    private ResponseSend responseSend;

    /**
     * 创建一个流程实例 返回流程实例ID
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<String> create(StartProcessDTO startDTO) {
        String processId = checkAndGetProcess(startDTO);
        Map<String, Object> map = setStartParam(startDTO);
        Authentication.setAuthenticatedUserId(joinTaskUser(startDTO.getUserType(), startDTO.getUserId()));
        ProcessInstance instance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(processId).businessKey(startDTO.getBizId())
                .name(startDTO.getBizKey()).variables(map).start();
        // ThreadLocal 处理
        Authentication.setAuthenticatedUserId(null);
        afterStart(startDTO, instance.getId());

        return new JsonResult<>(StatusEnum.OK.getValue(), null, instance.getId());
    }

    /**
     * 分页查询流程实例
     */
    public JsonResult<PageInfo<InstanceVO>> findByPage(InstanceQueryDTO queryDTO) {
        HistoricProcessInstanceQuery query = getInstanceQuery(queryDTO);
        Page<InstanceVO> page = PageUtils.newPage(queryDTO, query.count());

        if (page.getTotal() > 0) {
            List<HistoricProcessInstance> list = query.orderByProcessInstanceStartTime().desc()
                    .listPage(page.getStartRow(), page.getPageSize());
            page.addAll(list.stream().map(hpi -> {
                InstanceVO vo = InstanceVO.build(hpi);
                if (Objects.nonNull(vo)) {
                    setStatus(hpi, vo);
                }
                return vo;
            }).collect(Collectors.toList()));
        }

        return new JsonResult<>(StatusEnum.OK.getValue(), null, new PageInfo<>(page));
    }

    /**
     * 设置实例状态
     */
    private void setStatus(HistoricProcessInstance hpi, InstanceVO vo) {
        // 未结束
        if (Objects.isNull(hpi.getEndTime())) {
            long count = runtimeService.createProcessInstanceQuery().processInstanceId(hpi.getId()).active().count();
            vo.setStatus(count > 0 ? InstanceStatusEnum.RUN.getValue() : InstanceStatusEnum.SUSPENDED.getValue());
        } else {
            vo.setEndTime(hpi.getEndTime().getTime());
            vo.setStatus(StringUtils.isNotBlank(hpi.getEndActivityId()) || RecordStatusEnum.REJECT.name().equals(hpi.getDeleteReason()) ?
                    InstanceStatusEnum.END.getValue() : InstanceStatusEnum.CANCELED.getValue());
        }
    }

    /**
     * 根据ID查询详情
     */
    public JsonResult<InstanceVO> get(String id) {
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery().processInstanceId(id).singleResult();
        InstanceVO vo = InstanceVO.build(instance);
        if (Objects.nonNull(vo)) {
            setStatus(instance, vo);
            List<HistoricVariableInstance> list = historyService.createHistoricVariableInstanceQuery().processInstanceId(id).list();
            Map<String, String> varMap = Maps.newHashMapWithExpectedSize(list.size());
            list.forEach(var -> {
                switch (var.getVariableName()) {
                    case BIZ_KEY:
                    case LOOP_COUNTER:
                    case NUMBER_OF_INSTANCES:
                    case NUMBER_OF_ACTIVE_INSTANCES:
                    case NUMBER_OF_COMPLETED_INSTANCES:
                        break;
                    case BIZ_MSG:
                        vo.setMessage(var.getValue().toString());
                        break;
                    default:
                        varMap.put(var.getVariableName(), var.getValue().toString());
                }
            });
            vo.setVarMap(varMap);
        }

        return new JsonResult<>(StatusEnum.OK.getValue(), null, vo);
    }

    /**
     * 封装查询对象
     */
    private HistoricProcessInstanceQuery getInstanceQuery(InstanceQueryDTO queryDTO) {
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(trimToNull(queryDTO.getProcessKey()))
                .processInstanceBusinessKey(trimToNull(queryDTO.getBizId()))
                .processInstanceNameLike(ParamUtils.bothLike(queryDTO.getBizKey()));
        if (Objects.nonNull(queryDTO.getFinished())) {
            if (Boolean.TRUE.equals(queryDTO.getFinished())) {
                query.finished();
            } else {
                query.unfinished();
            }
        }

        return query;
    }

    /**
     * 激活
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<Boolean> activate(String id) {
        ProcessInstance instance = checkAndGetRun(id);
        Assert.isTrue(instance.isSuspended(), "流程实例已经是激活状态");
        runtimeService.activateProcessInstanceById(id);
        log.info("{} 激活流程实例 {}", UserContextHolder.get().getName(), id);
        return new JsonResult<>(StatusEnum.OK.getValue(), null, Boolean.TRUE);
    }

    /**
     * 挂起
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<Boolean> suspend(String id) {
        ProcessInstance instance = checkAndGetRun(id);
        Assert.isTrue(!instance.isSuspended(), "流程实例已经是挂起状态");
        runtimeService.suspendProcessInstanceById(id);
        log.info("{} 挂起流程实例 {}", UserContextHolder.get().getName(), id);
        return new JsonResult<>(StatusEnum.OK.getValue(), null, Boolean.TRUE);
    }

    /**
     * 作废
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<Boolean> cancel(String id, String reason) {
        checkAndGetRun(id);
        runtimeService.deleteProcessInstance(id, reason);
        log.info("{} 作废流程实例 {}", UserContextHolder.get().getName(), id);
        return new JsonResult<>(StatusEnum.OK.getValue(), null, Boolean.TRUE);
    }

    @Transactional(rollbackFor = Exception.class)
    public JsonResult<Boolean> cancel(CancelInstanceDTO cancelDTO) {
        for (int i = 0; i < cancelDTO.getBizIds().length; i++) {
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceBusinessKey(cancelDTO.getBizIds()[i], cancelDTO.getProcessKey()).singleResult();
            if (Objects.nonNull(instance)) {
                runtimeService.deleteProcessInstance(instance.getId(), cancelDTO.getReason());
                log.info("{} 作废流程实例 {}", cancelDTO.getOperator(), instance.getId());
            }
        }
        return new JsonResult<>(StatusEnum.OK.getValue(), null, Boolean.TRUE);
    }

    /**
     * 删除
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<Boolean> delete(String id) {
        HistoricProcessInstance instance = getHistoryInstance(id);
        Assert.notNull(instance, "ID【" + id + "】历史流程实例不存在");

        if (Objects.isNull(instance.getEndTime())) {
            // 删除正在运行的
            runtimeService.deleteProcessInstance(id, StringUtils.EMPTY);
        }
        recordService.deleteByInstanceId(id);
        historyService.deleteHistoricProcessInstance(id);

        log.info("{} 删除流程实例 key={} bizId={}", UserContextHolder.get().getName(),
                instance.getProcessDefinitionKey(), instance.getBusinessKey());
        return new JsonResult<>(StatusEnum.OK.getValue(), null, Boolean.TRUE);
    }

    /**
     * 流程启动后事宜
     */
    private void afterStart(StartProcessDTO startDTO, String instanceId) {
        if (StringUtils.isNotBlank(startDTO.getRemark())) {
            saveRecord(startDTO, instanceId);
        }

        if (isInstanceEnd(instanceId)) {
            responseSend.send(startDTO.getProcessKey(), ResponseTypeEnum.INSTANCE_END, startDTO.getBizId());
        }
    }

    /**
     * 处理启动入参
     */
    private static Map<String, Object> setStartParam(StartProcessDTO startDTO) {
        Map<String, Object> map = Maps.newHashMap();
        startDTO.setUserId(ObjectUtils.defaultIfNull(startDTO.getUserId(), 0L));
        startDTO.setUserName(StringUtils.defaultIfBlank(startDTO.getUserName(), UserTypeEnum.UNKNOWN.getDesc()));
        if (StringUtils.isNotBlank(startDTO.getBizKey())) {
            map.put(BIZ_KEY, startDTO.getBizKey());
        }
        if (StringUtils.isNotBlank(startDTO.getMessage())) {
            map.put(BIZ_MSG, startDTO.getMessage());
        }
        if (!CollectionUtils.isEmpty(startDTO.getVarMap())) {
            map.putAll(startDTO.getVarMap());
        }

        return map;
    }

    /**
     * 保存实例创建记录
     */
    private void saveRecord(StartProcessDTO startDTO, String instanceId) {
        InstanceRecordDTO recordDTO = new InstanceRecordDTO();
        recordDTO.setType(RecordTypeEnum.START.getValue());
        recordDTO.setStatus(RecordStatusEnum.PASS.getValue());
        recordDTO.setProcessKey(startDTO.getProcessKey());
        recordDTO.setBizId(startDTO.getBizId());
        recordDTO.setInstanceId(instanceId);
        recordDTO.setExecutionId(instanceId);
        recordDTO.setUserType(startDTO.getUserType());
        recordDTO.setUserId(startDTO.getUserId());
        recordDTO.setUserName(startDTO.getUserName());
        recordDTO.setContent(StringUtils.left(startDTO.getRemark(), CONTENT_LEN));
        recordDTO.setCreateTime(System.currentTimeMillis());
        recordService.save(recordDTO);
    }

    /**
     * 判断流程实例是否结束
     *
     * @param instanceId 实例ID
     * @return true-结束 false-未结束
     */
    private boolean isInstanceEnd(String instanceId) {
        return Objects.isNull(runtimeService.createProcessInstanceQuery()
                .processInstanceId(instanceId).singleResult());
    }

    /**
     * 检查是否存在正在执行的同一业务ID流程实例 并查询启动的流程定义ID
     */
    private String checkAndGetProcess(StartProcessDTO startDTO) {
        long count = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(startDTO.getBizId(), startDTO.getProcessKey()).count();
        Assert.isTrue(count == 0,
                String.format("存在正则执行的流程实例KEY【%s】 BizId【%s】", startDTO.getProcessKey(), startDTO.getBizId()));
        List<ProcessDefinition> processList = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(startDTO.getProcessKey()).active().orderByProcessDefinitionVersion().desc().listPage(0, 1);
        Assert.notEmpty(processList, "有效的流程定义KEY【" + startDTO.getProcessKey() + "】不存在");

        return processList.get(0).getId();
    }

    /**
     * 校验是否存在正在执行的流程实例
     */
    private ProcessInstance checkAndGetRun(String id) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(id).singleResult();
        Assert.notNull(instance, "ID【" + id + "】正在执行的流程实例不存在");
        return instance;
    }

    HistoricProcessInstance getHistoryInstance(String id) {
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(id).singleResult();
    }

    public JsonResult<List<RecordVO>> findRecords(RecordQueryDTO queryDTO) {
        List<RecordVO> dtoList = recordService.findByBiz(queryDTO.getProcessKey(), queryDTO.getBizId(),
                Boolean.TRUE.equals(queryDTO.getContainsStart()))
                .stream().map(RecordVO::build).collect(Collectors.toList());

        return new JsonResult<>(StatusEnum.OK.getValue(), null, dtoList);
    }

    public JsonResult<List<RecordVO>> findRecords(String instanceId) {
        List<RecordVO> dtoList = recordService.findByInstance(instanceId, true)
                .stream().map(RecordVO::build).collect(Collectors.toList());

        return new JsonResult<>(StatusEnum.OK.getValue(), null, dtoList);
    }

}
