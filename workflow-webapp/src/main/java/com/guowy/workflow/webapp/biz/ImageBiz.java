package com.guowy.workflow.webapp.biz;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.guowy.cloud.common.enums.StatusEnum;
import com.guowy.cloud.common.util.JsonResult;
import com.guowy.workflow.dto.UserTaskDTO;
import com.guowy.workflow.webapp.config.image.CustomProcessDiagramGenerator;
import com.guowy.workflow.webapp.dto.InstanceRecordDTO;
import com.guowy.workflow.webapp.service.InstanceRecordService;
import com.guowy.workflow.webapp.service.UserService;
import com.guowy.workflow.webapp.vo.GraphicVO;
import com.guowy.workflow.webapp.vo.ImageUserTaskVO;
import com.guowy.workflow.webapp.vo.RecordVO;
import de.odysseus.el.util.SimpleContext;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.*;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.persistence.entity.HistoricVariableInstanceEntityImpl;
import org.activiti.engine.runtime.Execution;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.guowy.workflow.webapp.constant.Constant.PASS;
import static com.guowy.workflow.webapp.constant.Constant.VAR;
import static com.guowy.workflow.webapp.util.WorkFlowUtils.getSelectOutFlow;
import static com.guowy.workflow.webapp.util.WorkFlowUtils.sepTaskUser;

/**
 * @author LiJingTang
 * @date 2020-05-27 10:20
 */
@Slf4j
@Service
public class ImageBiz {

    @Autowired
    private CustomProcessDiagramGenerator diagramGenerator;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private InstanceBiz instanceBiz;
    @Autowired
    private ExpressionFactory factory;
    @Autowired
    private InstanceRecordService recordService;
    @Autowired
    private UserService userService;

    /**
     * ??????????????????????????????
     */
    public JsonResult<Map<String, ImageUserTaskVO>> getImageInfo(String instanceId) {
        HistoricProcessInstance instance = instanceBiz.getHistoryInstance(instanceId);
        Assert.notNull(instance, "ID[" + instanceId + "]?????????????????????");
        List<InstanceRecordDTO> records = recordService.findByInstance(instance.getId(), false);
        return getImageInfo(instance, records);
    }

    /**
     * ??????????????????????????????
     */
    public JsonResult<Map<String, ImageUserTaskVO>> getImageInfo(String processKey, String bizId) {
        HistoricProcessInstance instance = checkAndGet(processKey, bizId);
        List<InstanceRecordDTO> records = recordService.findByBiz(processKey, bizId, false);
        return getImageInfo(instance, records);
    }

    private JsonResult<Map<String, ImageUserTaskVO>> getImageInfo(HistoricProcessInstance instance, List<InstanceRecordDTO> recordList) {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(instance.getProcessDefinitionId());
        // ????????????????????????
        List<UserTask> userTasks = bpmnModel.getProcesses().stream()
                .flatMap(p -> p.findFlowElementsOfType(UserTask.class).stream()).collect(Collectors.toList());
        // ??????????????????
        Map<String, List<InstanceRecordDTO>> recordMap = recordList.stream().collect(Collectors.groupingBy(InstanceRecordDTO::getTaskKey));
        // ??????????????????????????????
        List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(instance.getId()).activityType("userTask").list()
                .stream().filter(n -> StringUtils.isBlank(n.getDeleteReason())).collect(Collectors.toList());
        // ???????????????
        Map<String, Set<String>> userIdMap = Maps.newHashMapWithExpectedSize(userTasks.size());
        list.stream().collect(Collectors.groupingBy(HistoricActivityInstance::getActivityId))
                .forEach((k, v) -> userIdMap.put(k, v.stream().map(HistoricActivityInstance::getAssignee)
                        .filter(StringUtils::isNotBlank).collect(Collectors.toSet())));
        // ?????????????????????????????????????????????????????????
        Set<String> unCompleteIds = list.stream().filter(n -> Objects.isNull(n.getEndTime())).collect(Collectors.toSet())
                .stream().map(HistoricActivityInstance::getActivityId).collect(Collectors.toSet());
        Set<String> completeIds = list.stream().filter(n -> Objects.nonNull(n.getEndTime()) && !unCompleteIds.contains(n.getActivityId()))
                .map(HistoricActivityInstance::getActivityId).collect(Collectors.toSet());
        Map<String, Object> varMap = getInstanceVar(instance);

        Map<String, ImageUserTaskVO> map = userTasks.stream().map(userTask -> {
            ImageUserTaskVO vo = new ImageUserTaskVO();
            vo.setTaskKey(userTask.getId());
            vo.setTaskName(userTask.getName());
            vo.setGraphic(GraphicVO.build(bpmnModel.getGraphicInfo(vo.getTaskKey())));
            List<InstanceRecordDTO> records = recordMap.get(vo.getTaskKey());
            if (!CollectionUtils.isEmpty(records)) {
                vo.setRecords(records.stream().map(RecordVO::build).collect(Collectors.toList()));
            }
            Set<String> userIds = userIdMap.getOrDefault(vo.getTaskKey(), new HashSet<>());
            // ?????????????????????????????????
            if (!completeIds.contains(vo.getTaskKey())) {
                userIds.addAll(getUser(userTask, varMap));
            }
            vo.setUsers(assembleTaskUsers(userIds));
            return vo;
        }).collect(Collectors.toMap(ImageUserTaskVO::getTaskKey, Function.identity()));

        return new JsonResult<>(StatusEnum.OK.getValue(), null, map);
    }

    /**
     * ????????????????????????
     */
    private Map<String, Object> getInstanceVar(HistoricProcessInstance instance) {
        if (Objects.isNull(instance.getEndTime())) {
            return runtimeService.getVariables(instance.getId());
        } else {
            return historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(instance.getId()).list().stream()
                    .collect(Collectors.toMap(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue, (v1, v2) -> v2));
        }
    }

    /**
     * ???????????????????????????
     */
    private List<UserTaskDTO.TaskUser> assembleTaskUsers(Set<String> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        return userIds.stream().map(id -> {
            UserTaskDTO.TaskUser taskUser = sepTaskUser(id);
            taskUser.setUserName(userService.getName(taskUser.getUserType(), taskUser.getUserId()));
            return taskUser;
        }).collect(Collectors.toList());
    }

    /**
     * ???????????????
     */
    private Set<String> getUser(UserTask userTask, Map<String, Object> varMap) {
        MultiInstanceLoopCharacteristics characteristics = userTask.getLoopCharacteristics();
        // ???????????????
        if (Objects.nonNull(characteristics) && StringUtils.isNotBlank(characteristics.getElementVariable())) {
            String s = "${" + characteristics.getElementVariable().trim() + "}";
            if (s.equals(userTask.getAssignee()) || userTask.getCandidateUsers().stream().anyMatch(s::equals)) {
                String collection = userTask.getLoopCharacteristics().getInputDataItem();
                Object value = varMap.get(collection);
                if (Objects.nonNull(value)) {
                    return new HashSet<>((List<String>) value);
                }
            }
        }

        return getLoopUser(userTask, varMap);
    }

    /**
     * ?????????????????????
     */
    private Set<String> getLoopUser(UserTask userTask, Map<String, Object> varMap) {
        if (StringUtils.isNotBlank(userTask.getAssignee())) {
            String assignee = userTask.getAssignee();
            if (assignee.startsWith(VAR)) {
                assignee = getUserId(assignee, varMap);
            }
            if (StringUtils.isNotBlank(assignee)) {
                return Sets.newHashSet(assignee);
            }
        } else {
            Set<String> tempIds = new HashSet<>();
            userTask.getCandidateUsers().forEach(user -> {
                if (user.startsWith(VAR)) {
                    user = getUserId(user, varMap);
                }
                if (StringUtils.isNotBlank(user)) {
                    tempIds.addAll(Arrays.asList(user.split(",")));
                }
            });

            return tempIds;
        }

        return Collections.emptySet();
    }

    /**
     * ?????????????????????
     */
    private String getUserId(String expression, Map<String, Object> varMap) {
        try {
            SimpleContext context = new SimpleContext();
            varMap.forEach((k, v) -> context.setVariable(k, factory.createValueExpression(v, v.getClass())));
            ValueExpression valueExpression = factory.createValueExpression(context, expression, String.class);
            return valueExpression.getValue(context).toString();
        } catch (ELException e) {
            log.info("??????????????????{}???????????? {}", expression, JSON.toJSONString(varMap));
            return null;
        }
    }

    /**
     * ????????????????????????
     */
    public void generate(String processKey, String bizId, OutputStream output) throws IOException {
        HistoricProcessInstance instance = checkAndGet(processKey, bizId);
        generate(instance, output);
    }

    /**
     * ????????????????????????
     */
    public void generate(String instanceId, OutputStream output) throws IOException {
        HistoricProcessInstance instance = instanceBiz.getHistoryInstance(instanceId);
        Assert.notNull(instance, "ID[" + instanceId + "]?????????????????????");
        generate(instance, output);
    }

    private void generate(HistoricProcessInstance instance, OutputStream output) throws IOException {
        Assert.notNull(output, "???????????????");
        String instanceId = instance.getId();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(instance.getProcessDefinitionId());
        // ???????????????
        Set<String> completeNodes = new HashSet<>();
        // ???????????????????????????
        Set<String> currentNodes = new HashSet<>();
        // ?????????????????? ?????????
        Set<String> runNodes = new HashSet<>();
        // ???????????????
        List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery().processInstanceId(instanceId)
                .orderByHistoricActivityInstanceEndTime().desc().list().stream()
                .filter(n -> StringUtils.isBlank(n.getDeleteReason())).collect(Collectors.toList());
        String endNode = null;
        if (Objects.isNull(instance.getEndTime())) {
            initNodes(instanceId, bpmnModel, list, completeNodes, currentNodes, runNodes);
        } else {
            completeNodes.addAll(list.stream().map(HistoricActivityInstance::getActivityId).collect(Collectors.toSet()));
            // ???????????????????????????
            if (StringUtils.isBlank(instance.getEndActivityId())) {
                endNode = list.get(0).getActivityId();
            }
        }
        Set<String> flows = getHighFlows(instanceId, bpmnModel, list, completeNodes, currentNodes, runNodes);
        InputStream inputStream = diagramGenerator.generate(bpmnModel, completeNodes, currentNodes, runNodes, flows, endNode);
        IOUtils.copy(inputStream, output);
    }

    /**
     * ????????????????????????????????????
     */
    private Set<String> getHighFlows(String instanceId, BpmnModel bpmnModel, List<HistoricActivityInstance> list,
                                     Set<String> completeNodes, Set<String> currentNodes, Set<String> runNodes) {
        // ??????????????????
        Map<String, Object> varMap = Maps.newHashMap();
        historyService.createHistoricVariableInstanceQuery().processInstanceId(instanceId).list().forEach(var -> {
            HistoricVariableInstanceEntityImpl varEntity = (HistoricVariableInstanceEntityImpl) var;
            if (instanceId.equals(varEntity.getExecutionId())) {
                varMap.put(var.getVariableName(), var.getValue());
            }
        });
        // ??????????????????
        Map<String, Integer> passMap = recordService.findStatusByInstance(instanceId);

        return CustomProcessDiagramGenerator.getSequenceFlows(bpmnModel)
                .stream().filter(f -> {
                    String src = f.getSourceRef();
                    String target = f.getTargetRef();
                    // ????????????????????? ????????????????????????????????????????????????
                    boolean flag = completeNodes.contains(src) &&
                            (completeNodes.contains(target) || currentNodes.contains(target) || runNodes.contains(target));
                    // ????????????????????????
                    if (flag && StringUtils.isNotBlank(f.getConditionExpression())) {
                        Map<String, Object> tempMap = new HashMap<>(varMap);
                        String id = bpmnModel.getFlowElement(src).getId();
                        // ???????????????ID
                        Optional<HistoricActivityInstance> optional = list.stream().filter(n -> id.equals(n.getActivityId())).findFirst();
                        if (optional.isPresent()) {
                            Integer pass = passMap.get(optional.get().getExecutionId() + src);
                            if (Objects.nonNull(pass)) {
                                tempMap.put(PASS, pass);
                            }
                        }
                        // ??????????????????????????? ??????
                        return !CollectionUtils.isEmpty(getSelectOutFlow(Lists.newArrayList(f), factory, tempMap));
                    }

                    return flag;
                }).map(SequenceFlow::getId).collect(Collectors.toSet());
    }

    /**
     * ???????????????????????????
     */
    private void initNodes(String instanceId, BpmnModel bpmnModel, List<HistoricActivityInstance> list,
                           Set<String> completeNodes, Set<String> currentNodes, Set<String> runNodes) {
        // ??????????????????
        Set<HistoricActivityInstance> unCompleteNodes = list.stream().filter(n -> Objects.isNull(n.getEndTime())).collect(Collectors.toSet());
        Set<String> unCompleteIds = unCompleteNodes.stream().map(HistoricActivityInstance::getActivityId).collect(Collectors.toSet());
        completeNodes.addAll(list.stream().filter(n -> Objects.nonNull(n.getEndTime()) && !unCompleteIds.contains(n.getActivityId()))
                .map(HistoricActivityInstance::getActivityId).collect(Collectors.toSet()));
        // ??????????????????
        Set<String> runIds = runtimeService.createExecutionQuery().processInstanceId(instanceId).onlyChildExecutions().list()
                .stream().map(Execution::getId).collect(Collectors.toSet());
        // ???????????????????????????????????????
        Set<String> exeIds = list.stream().filter(n -> Objects.nonNull(n.getEndTime()))
                .map(HistoricActivityInstance::getActivityId).collect(Collectors.toSet());

        unCompleteNodes.forEach(node -> {
            // ?????????
            if (exeIds.contains(node.getActivityId())) {
                FlowElement element = bpmnModel.getFlowElement(node.getActivityId());
                // ????????????????????????????????????
                if (element instanceof UserTask && Objects.nonNull(((UserTask) element).getLoopCharacteristics())) {
                    // ????????? ??????????????????????????? ?????????????????????ID?????????????????????ID ??????????????????
                    boolean isRun = list.stream().filter(n -> node.getActivityId().equals(n.getActivityId()) && Objects.nonNull(n.getEndTime()))
                            .anyMatch(n -> runIds.contains(n.getExecutionId()));
                    if (isRun) {
                        runNodes.add(node.getActivityId());
                    } else {
                        // ???????????????????????????
                        currentNodes.add(node.getActivityId());
                    }
                } else {
                    // ??????????????? ?????? ?????????????????????
                    currentNodes.add(node.getActivityId());
                }
            } else {
                // ????????????????????????????????????????????????
                currentNodes.add(node.getActivityId());
            }
        });
    }

    private HistoricProcessInstance checkAndGet(String processKey, String bizId) {
        List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processKey).processInstanceBusinessKey(bizId)
                .orderByProcessInstanceStartTime().desc().listPage(0, 1);
        Assert.notEmpty(instances, String.format("????????????KEY???%s?????????ID???%s????????????????????????", processKey, bizId));
        return instances.get(0);
    }

}
