package com.guowy.workflow.webapp.biz;

import com.google.common.collect.Maps;
import com.guowy.cloud.common.enums.StatusEnum;
import com.guowy.cloud.common.util.JsonResult;
import com.guowy.cloud.security.enums.UserTypeEnum;
import com.guowy.workflow.dto.ReceiveTaskCompleteDTO;
import com.guowy.workflow.dto.ReceiveTaskResponseDTO;
import com.guowy.workflow.webapp.dto.InstanceRecordDTO;
import com.guowy.workflow.webapp.enums.RecordStatusEnum;
import com.guowy.workflow.webapp.enums.RecordTypeEnum;
import com.guowy.workflow.webapp.service.CustomOperateService;
import com.guowy.workflow.webapp.service.InstanceRecordService;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.ReceiveTask;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.el.ExpressionFactory;
import java.util.List;
import java.util.Map;

import static com.guowy.workflow.webapp.constant.Constant.PASS;
import static com.guowy.workflow.webapp.util.WorkFlowUtils.getPassValue;
import static com.guowy.workflow.webapp.util.WorkFlowUtils.getSelectOutFlow;

/**
 * @author LiJingTang
 * @date 2020-05-19 18:18
 */
@Slf4j
@Service
public class ReceiveTaskBiz {

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private ExpressionFactory expressionFactory;
    @Autowired
    private CustomOperateService customOperateService;
    @Autowired
    private InstanceRecordService recordService;

    /**
     * 办理接收任务
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<ReceiveTaskResponseDTO> complete(ReceiveTaskCompleteDTO completeDTO) {
        ProcessInstance instance = checkAndGetInstance(completeDTO);
        BpmnModel bpmnModel = repositoryService.getBpmnModel(instance.getProcessDefinitionId());
        Execution execution = checkAndGetExecution(completeDTO, instance, bpmnModel);
        // 办理接收任务
        boolean noOutGoing = completeTask(completeDTO, bpmnModel, execution);
        saveRecord(completeDTO, instance, execution);
        boolean isEnd = false;
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processInstanceId(instance.getId());
        if (noOutGoing) {
            isEnd = true;
            long count = query.count();
            if (count > 0) {
                runtimeService.deleteProcessInstance(instance.getId(), RecordStatusEnum.REJECT.name());
                customOperateService.terminateInstance(instance.getId(), RecordStatusEnum.REJECT.name());
            }
        }

        isEnd = isEnd || query.count() == 0;
        return new JsonResult<>(StatusEnum.OK.getValue(), null, new ReceiveTaskResponseDTO(isEnd, execution.getActivityId()));
    }

    private boolean completeTask(ReceiveTaskCompleteDTO completeDTO, BpmnModel bpmnModel, Execution execution) {
        boolean noOutGoing = false;
        if (Boolean.FALSE.equals(completeDTO.getPass())) {
            log.info("接收任务节点ID【{}】", execution.getId());
            noOutGoing = isNoOutGoing(execution, bpmnModel, completeDTO.getVarMap());
        }
        Map<String, Object> map = Maps.newHashMapWithExpectedSize(1);
        map.put(PASS, noOutGoing ? RecordStatusEnum.PASS.getValue() : getPassValue(completeDTO.getPass()));
        runtimeService.trigger(execution.getId(), CollectionUtils.isEmpty(completeDTO.getVarMap()) ? null : completeDTO.getVarMap(), map);
        return noOutGoing;
    }

    /**
     * 任务审批记录
     */
    private void saveRecord(ReceiveTaskCompleteDTO completeDTO, ProcessInstance instance, Execution execution) {
        InstanceRecordDTO recordDTO = new InstanceRecordDTO();
        recordDTO.setType(RecordTypeEnum.RECEIVE_TASK.getValue());
        recordDTO.setStatus(getPassValue(completeDTO.getPass()));
        recordDTO.setProcessKey(instance.getProcessDefinitionKey());
        recordDTO.setBizId(instance.getBusinessKey());
        recordDTO.setInstanceId(instance.getId());
        recordDTO.setExecutionId(execution.getId());
        recordDTO.setTaskKey(execution.getActivityId());
        recordDTO.setUserType(UserTypeEnum.UNKNOWN.getValue());
        recordDTO.setCreateTime(System.currentTimeMillis());

        recordService.save(recordDTO);
    }

    private Execution checkAndGetExecution(ReceiveTaskCompleteDTO completeDTO, ProcessInstance instance, BpmnModel bpmnModel) {
        List<Execution> list = runtimeService.createExecutionQuery()
                .processInstanceId(instance.getId()).onlyChildExecutions()
                .activityId(StringUtils.trimToNull(completeDTO.getTaskKey())).list();
        if (!CollectionUtils.isEmpty(list)) {
            list.removeIf(e -> !(bpmnModel.getFlowElement(e.getActivityId()) instanceof ReceiveTask));
        }
        Assert.notEmpty(list, String.format("符合条件的接收任务【%s:%s:%s】不存在",
                completeDTO.getProcessKey(), completeDTO.getBizId(), completeDTO.getTaskKey()));
        // 接收任务不允许多实例
        Assert.isTrue(list.size() == 1, String.format("符合条件的接收任务【%s:%s:%s】存在多条",
                completeDTO.getProcessKey(), completeDTO.getBizId(), completeDTO.getTaskKey()));
        return list.get(0);
    }

    private ProcessInstance checkAndGetInstance(ReceiveTaskCompleteDTO completeDTO) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(completeDTO.getProcessKey())
                .processInstanceBusinessKey(completeDTO.getBizId(), completeDTO.getProcessKey())
                .singleResult();
        Assert.notNull(instance, String.format("正在执行的流程实例【%s:%s】不存在",
                completeDTO.getProcessKey(), completeDTO.getBizId()));
        return instance;
    }

    /**
     * 驳回状态判断是否有可以选择的下一步流程线
     */
    private boolean isNoOutGoing(Execution execution, BpmnModel bpmnModel, Map<String, Object> taskVarMap) {
        FlowElement element = bpmnModel.getFlowElement(execution.getActivityId());
        Map<String, Object> varMap = runtimeService.getVariables(execution.getProcessInstanceId());
        if (!CollectionUtils.isEmpty(taskVarMap)) {
            varMap.putAll(taskVarMap);
        }
        varMap.put(PASS, RecordStatusEnum.REJECT.getValue());
        List<SequenceFlow> outFlows = getSelectOutFlow(((ReceiveTask) element).getOutgoingFlows(), expressionFactory, varMap);
        return CollectionUtils.isEmpty(outFlows);
    }

}
