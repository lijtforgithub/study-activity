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
     * 流程实例图片节点信息
     */
    public JsonResult<Map<String, ImageUserTaskVO>> getImageInfo(String instanceId) {
        HistoricProcessInstance instance = instanceBiz.getHistoryInstance(instanceId);
        Assert.notNull(instance, "ID[" + instanceId + "]流程实例不存在");
        List<InstanceRecordDTO> records = recordService.findByInstance(instance.getId(), false);
        return getImageInfo(instance, records);
    }

    /**
     * 流程实例图片节点信息
     */
    public JsonResult<Map<String, ImageUserTaskVO>> getImageInfo(String processKey, String bizId) {
        HistoricProcessInstance instance = checkAndGet(processKey, bizId);
        List<InstanceRecordDTO> records = recordService.findByBiz(processKey, bizId, false);
        return getImageInfo(instance, records);
    }

    private JsonResult<Map<String, ImageUserTaskVO>> getImageInfo(HistoricProcessInstance instance, List<InstanceRecordDTO> recordList) {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(instance.getProcessDefinitionId());
        // 筛选用户任务元素
        List<UserTask> userTasks = bpmnModel.getProcesses().stream()
                .flatMap(p -> p.findFlowElementsOfType(UserTask.class).stream()).collect(Collectors.toList());
        // 查询审批记录
        Map<String, List<InstanceRecordDTO>> recordMap = recordList.stream().collect(Collectors.groupingBy(InstanceRecordDTO::getTaskKey));
        // 查询历史用户任务节点
        List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(instance.getId()).activityType("userTask").list()
                .stream().filter(n -> StringUtils.isBlank(n.getDeleteReason())).collect(Collectors.toList());
        // 任务办理人
        Map<String, Set<String>> userIdMap = Maps.newHashMapWithExpectedSize(userTasks.size());
        list.stream().collect(Collectors.groupingBy(HistoricActivityInstance::getActivityId))
                .forEach((k, v) -> userIdMap.put(k, v.stream().map(HistoricActivityInstance::getAssignee)
                        .filter(StringUtils::isNotBlank).collect(Collectors.toSet())));
        // 流程未结束就要获取全部任务的审批人信息
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
            // 已完成的节点办理人固定
            if (!completeIds.contains(vo.getTaskKey())) {
                userIds.addAll(getUser(userTask, varMap));
            }
            vo.setUsers(assembleTaskUsers(userIds));
            return vo;
        }).collect(Collectors.toMap(ImageUserTaskVO::getTaskKey, Function.identity()));

        return new JsonResult<>(StatusEnum.OK.getValue(), null, map);
    }

    /**
     * 查询流程实例变量
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
     * 封装任务办理人对象
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
     * 任务办理人
     */
    private Set<String> getUser(UserTask userTask, Map<String, Object> varMap) {
        MultiInstanceLoopCharacteristics characteristics = userTask.getLoopCharacteristics();
        // 多实例配置
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
     * 办理人或候选人
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
     * 获取表达式的值
     */
    private String getUserId(String expression, Map<String, Object> varMap) {
        try {
            SimpleContext context = new SimpleContext();
            varMap.forEach((k, v) -> context.setVariable(k, factory.createValueExpression(v, v.getClass())));
            ValueExpression valueExpression = factory.createValueExpression(context, expression, String.class);
            return valueExpression.getValue(context).toString();
        } catch (ELException e) {
            log.info("获取表达式【{}】值失败 {}", expression, JSON.toJSONString(varMap));
            return null;
        }
    }

    /**
     * 生成流程实例图片
     */
    public void generate(String processKey, String bizId, OutputStream output) throws IOException {
        HistoricProcessInstance instance = checkAndGet(processKey, bizId);
        generate(instance, output);
    }

    /**
     * 生成流程实例图片
     */
    public void generate(String instanceId, OutputStream output) throws IOException {
        HistoricProcessInstance instance = instanceBiz.getHistoryInstance(instanceId);
        Assert.notNull(instance, "ID[" + instanceId + "]流程实例不存在");
        generate(instance, output);
    }

    private void generate(HistoricProcessInstance instance, OutputStream output) throws IOException {
        Assert.notNull(output, "输出流为空");
        String instanceId = instance.getId();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(instance.getProcessDefinitionId());
        // 完成的节点
        Set<String> completeNodes = new HashSet<>();
        // 即将可以执行的节点
        Set<String> currentNodes = new HashSet<>();
        // 执行中的节点 多实例
        Set<String> runNodes = new HashSet<>();
        // 非删除节点
        List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery().processInstanceId(instanceId)
                .orderByHistoricActivityInstanceEndTime().desc().list().stream()
                .filter(n -> StringUtils.isBlank(n.getDeleteReason())).collect(Collectors.toList());
        String endNode = null;
        if (Objects.isNull(instance.getEndTime())) {
            initNodes(instanceId, bpmnModel, list, completeNodes, currentNodes, runNodes);
        } else {
            completeNodes.addAll(list.stream().map(HistoricActivityInstance::getActivityId).collect(Collectors.toSet()));
            // 为空说明是异常终止
            if (StringUtils.isBlank(instance.getEndActivityId())) {
                endNode = list.get(0).getActivityId();
            }
        }
        Set<String> flows = getHighFlows(instanceId, bpmnModel, list, completeNodes, currentNodes, runNodes);
        InputStream inputStream = diagramGenerator.generate(bpmnModel, completeNodes, currentNodes, runNodes, flows, endNode);
        IOUtils.copy(inputStream, output);
    }

    /**
     * 获取需要高亮展示的流程线
     */
    private Set<String> getHighFlows(String instanceId, BpmnModel bpmnModel, List<HistoricActivityInstance> list,
                                     Set<String> completeNodes, Set<String> currentNodes, Set<String> runNodes) {
        // 流程实例变量
        Map<String, Object> varMap = Maps.newHashMap();
        historyService.createHistoricVariableInstanceQuery().processInstanceId(instanceId).list().forEach(var -> {
            HistoricVariableInstanceEntityImpl varEntity = (HistoricVariableInstanceEntityImpl) var;
            if (instanceId.equals(varEntity.getExecutionId())) {
                varMap.put(var.getVariableName(), var.getValue());
            }
        });
        // 所有审核状态
        Map<String, Integer> passMap = recordService.findStatusByInstance(instanceId);

        return CustomProcessDiagramGenerator.getSequenceFlows(bpmnModel)
                .stream().filter(f -> {
                    String src = f.getSourceRef();
                    String target = f.getTargetRef();
                    // 源头是完成节点 目标是完成或部分或将要完成的节点
                    boolean flag = completeNodes.contains(src) &&
                            (completeNodes.contains(target) || currentNodes.contains(target) || runNodes.contains(target));
                    // 判断条件是否满足
                    if (flag && StringUtils.isNotBlank(f.getConditionExpression())) {
                        Map<String, Object> tempMap = new HashMap<>(varMap);
                        String id = bpmnModel.getFlowElement(src).getId();
                        // 源节点执行ID
                        Optional<HistoricActivityInstance> optional = list.stream().filter(n -> id.equals(n.getActivityId())).findFirst();
                        if (optional.isPresent()) {
                            Integer pass = passMap.get(optional.get().getExecutionId() + src);
                            if (Objects.nonNull(pass)) {
                                tempMap.put(PASS, pass);
                            }
                        }
                        // 不为空说明满足条件 经过
                        return !CollectionUtils.isEmpty(getSelectOutFlow(Lists.newArrayList(f), factory, tempMap));
                    }

                    return flag;
                }).map(SequenceFlow::getId).collect(Collectors.toSet());
    }

    /**
     * 分类不同状态的节点
     */
    private void initNodes(String instanceId, BpmnModel bpmnModel, List<HistoricActivityInstance> list,
                           Set<String> completeNodes, Set<String> currentNodes, Set<String> runNodes) {
        // 未完成的节点
        Set<HistoricActivityInstance> unCompleteNodes = list.stream().filter(n -> Objects.isNull(n.getEndTime())).collect(Collectors.toSet());
        Set<String> unCompleteIds = unCompleteNodes.stream().map(HistoricActivityInstance::getActivityId).collect(Collectors.toSet());
        completeNodes.addAll(list.stream().filter(n -> Objects.nonNull(n.getEndTime()) && !unCompleteIds.contains(n.getActivityId()))
                .map(HistoricActivityInstance::getActivityId).collect(Collectors.toSet()));
        // 正在执行节点
        Set<String> runIds = runtimeService.createExecutionQuery().processInstanceId(instanceId).onlyChildExecutions().list()
                .stream().map(Execution::getId).collect(Collectors.toSet());
        // 完成的节点和部分完成的节点
        Set<String> exeIds = list.stream().filter(n -> Objects.nonNull(n.getEndTime()))
                .map(HistoricActivityInstance::getActivityId).collect(Collectors.toSet());

        unCompleteNodes.forEach(node -> {
            // 执行过
            if (exeIds.contains(node.getActivityId())) {
                FlowElement element = bpmnModel.getFlowElement(node.getActivityId());
                // 只有用户任务才可以多实例
                if (element instanceof UserTask && Objects.nonNull(((UserTask) element).getLoopCharacteristics())) {
                    // 多实例 判断是是不是执行中 如果正在执行的ID包含完成的执行ID 说明是执行中
                    boolean isRun = list.stream().filter(n -> node.getActivityId().equals(n.getActivityId()) && Objects.nonNull(n.getEndTime()))
                            .anyMatch(n -> runIds.contains(n.getExecutionId()));
                    if (isRun) {
                        runNodes.add(node.getActivityId());
                    } else {
                        // 多实例节点未执行过
                        currentNodes.add(node.getActivityId());
                    }
                } else {
                    // 不是多实例 回退 当作第一次执行
                    currentNodes.add(node.getActivityId());
                }
            } else {
                // 即将可以执行且一次未执行过的节点
                currentNodes.add(node.getActivityId());
            }
        });
    }

    private HistoricProcessInstance checkAndGet(String processKey, String bizId) {
        List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processKey).processInstanceBusinessKey(bizId)
                .orderByProcessInstanceStartTime().desc().listPage(0, 1);
        Assert.notEmpty(instances, String.format("流程定义KEY【%s】业务ID【%s】流程实例不存在", processKey, bizId));
        return instances.get(0);
    }

}
