package com.guowy.workflow.webapp.config.valid;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.*;
import org.activiti.validation.ValidationError;
import org.activiti.validation.validator.ProcessLevelValidator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.guowy.workflow.webapp.config.handler.CustomUserTaskParseHandler.AUTO_COMPLETE;

/**
 * 自定义校验规则
 *
 * @author LiJingTang
 * @date 2020-05-14 16:14
 */
@Slf4j
@Component
public class CustomProcessValidator extends ProcessLevelValidator {

    /**
     * 用户任务办理人配置正则
     */
    private static final Pattern ASSIGNEE = Pattern.compile("^\\$\\{\\w+}$|^[12]:\\d+$");
    /**
     * 用户任务候选人配置正则
     */
    private static final Pattern CANDIDATE = Pattern.compile("^\\$\\{\\w+}$|^[12]:\\d+(,[12]:\\d+)+$");
    /**
     * 变量正则
     */
    private static final Pattern VAR = Pattern.compile("^[a-zA-Z]\\w*$");

    private static final int INT2 = 2;
    private static final String INT2_MSG = "少于2个传出流";

    @Override
    protected void executeValidation(BpmnModel bpmnModel, Process process, List<ValidationError> list) {
        if (bpmnModel.getProcesses().size() > 1) {
            list.add(newError(null, null, "配置文件存在多个流程"));
        }

        boolean hasStart = false;
        for (FlowElement element : process.getFlowElements()) {
            if (element instanceof SequenceFlow) {
                SequenceFlow flow = (SequenceFlow) element;
                log.debug("{} = {}", flow.getId(), flow.getConditionExpression());
            } else if (element instanceof StartEvent) {
                validate((StartEvent) element, list);
                hasStart = true;
            } else if (element instanceof EndEvent) {
                validate((EndEvent) element, list);
            } else if (element instanceof UserTask) {
                validate((UserTask) element, list);
            } else if (element instanceof ReceiveTask) {
                validate((ReceiveTask) element, list);
            } else if (element instanceof ExclusiveGateway) {
                validate((ExclusiveGateway) element, list);
            } else if (element instanceof ParallelGateway) {
                validate((ParallelGateway) element, list);
            } else if (element instanceof InclusiveGateway) {
                validate((InclusiveGateway) element, list);
            } else if (element instanceof SubProcess) {
                SubProcess sub = (SubProcess) element;
                log.info("子流程ID：{}", sub.getId());
            } else {
                list.add(newError(element.getId(), element.getName(), "暂不支持元素类型：" + element.getClass().getSimpleName()));
            }
        }

        if (!hasStart) {
            list.add(newError(process.getId(), process.getName(), "流程未配置开始节点"));
        }
    }

    private static void validate(StartEvent startEvent, List<ValidationError> list) {
        String name = "开启节点" + StringUtils.trimToEmpty(startEvent.getName());
        if (StringUtils.isBlank(startEvent.getFormKey())) {
            list.add(newError(startEvent.getId(), name, "未配置审核URL属性"));
        }
    }

    private static void validate(EndEvent endEvent, List<ValidationError> list) {
        String name = "结束节点" + StringUtils.trimToEmpty(endEvent.getName());
        endEvent.getIncomingFlows().forEach(flow -> {
            if (StringUtils.isNotBlank(flow.getConditionExpression())) {
                list.add(newError(endEvent.getId(), name, String.format("的传入流【%s】不可配置条件【%s】",
                        flow.getId(), flow.getConditionExpression())));
            }
        });
    }

    private static void validate(UserTask userTask, List<ValidationError> list) {
        String name = "用户任务" + StringUtils.trimToEmpty(userTask.getName());
        if (StringUtils.isBlank(userTask.getName())) {
            list.add(newError(userTask.getId(), name, "未配置名称"));
        }
        if (StringUtils.isBlank(userTask.getAssignee()) && CollectionUtils.isEmpty(userTask.getCandidateUsers())) {
            list.add(newError(userTask.getId(), name, "未配置办理人或候选人"));
        }
        if (StringUtils.isNotBlank(userTask.getAssignee()) && !CollectionUtils.isEmpty(userTask.getCandidateUsers())) {
            list.add(newError(userTask.getId(), name, "同时配置了办理人和候选人"));
        }
        if (StringUtils.isNotBlank(userTask.getAssignee())) {
            userTask.setAssignee(userTask.getAssignee().replace(StringUtils.SPACE, StringUtils.EMPTY));
            if (!ASSIGNEE.matcher(userTask.getAssignee()).matches()) {
                list.add(newError(userTask.getId(), name, String.format("办理人配置【%s】不合法", userTask.getAssignee())));
            }
        }
        if (!CollectionUtils.isEmpty(userTask.getCandidateUsers())) {
            List<String> result = new ArrayList<>(userTask.getCandidateUsers().size());
            for (String id : userTask.getCandidateUsers()) {
                String userId = id.replace(StringUtils.SPACE, StringUtils.EMPTY);
                if (!CANDIDATE.matcher(userId).matches()) {
                    list.add(newError(userTask.getId(), name, String.format("候选人配置【%s】不合法", userId)));
                }
                result.add(userId);
            }
            userTask.setCandidateUsers(result);
        }
        validate(userTask, list, name);
    }

    private static void validate(UserTask userTask, List<ValidationError> list, String name) {
        if (Objects.nonNull(userTask.getLoopCharacteristics())) {
            MultiInstanceLoopCharacteristics characteristics = userTask.getLoopCharacteristics();
            if (!VAR.matcher(characteristics.getInputDataItem()).matches()) {
                list.add(newError(userTask.getId(), name,
                        String.format("多实例集合配置【%s】不合法", characteristics.getInputDataItem())));
            }
        }
        if (!CollectionUtils.isEmpty(userTask.getTaskListeners())) {
            userTask.getTaskListeners().forEach(listener -> {
                if (AUTO_COMPLETE.equals(listener.getImplementation()) && StringUtils.isBlank(userTask.getAssignee())) {
                    list.add(newError(userTask.getId(), name, "自动办理任务必须配置办理人"));
                }
            });
        }
    }

    private static void validate(ReceiveTask receiveTask, List<ValidationError> list) {
        String name = "接收任务" + StringUtils.trimToEmpty(receiveTask.getName());
        if (StringUtils.isBlank(receiveTask.getName())) {
            list.add(newError(receiveTask.getId(), name, "未配置名称"));
        }
        if (Objects.nonNull(receiveTask.getLoopCharacteristics())) {
            list.add(newError(receiveTask.getId(), name, "配置了多实例"));
        }
    }

    /**
     * 排他网关选择第一个满足条件的传出流执行
     */
    private static void validate(ExclusiveGateway gateway, List<ValidationError> list) {
        String name = "排他网关" + StringUtils.trimToEmpty(gateway.getName());
        if (gateway.getOutgoingFlows().size() < INT2) {
            list.add(newError(gateway.getId(), name, INT2_MSG));
        }
        Map<String, String> map = Maps.newHashMapWithExpectedSize(gateway.getOutgoingFlows().size());
        gateway.getOutgoingFlows().forEach(flow -> {
            if (!flow.getId().equals(gateway.getDefaultFlow())) {
                if (StringUtils.isBlank(flow.getConditionExpression())) {
                    list.add(newError(gateway.getId(), name, String.format("的传出流【%s】未配置条件", flow.getId())));
                } else {
                    String expression = flow.getConditionExpression().replace(StringUtils.SPACE, StringUtils.EMPTY);
                    String id = map.get(expression);
                    if (StringUtils.isNotBlank(id)) {
                        list.add(newError(gateway.getId(), name, String.format("存在相同条件【%s】的传出流【%s和%s】",
                                expression, id, flow.getId())));
                    }
                    map.put(expression, flow.getId());
                }
            }
        });
    }

    /**
     * 并行网关不会判断传出流条件全部执行
     */
    private static void validate(ParallelGateway gateway, List<ValidationError> list) {
        String name = "并行网关" + StringUtils.trimToEmpty(gateway.getName());
        if (gateway.getIncomingFlows().size() == 1 && gateway.getOutgoingFlows().size() < INT2) {
            list.add(newError(gateway.getId(), name, INT2_MSG));
        }
        if (StringUtils.isNotBlank(gateway.getDefaultFlow())) {
            list.add(newError(gateway.getId(), name, "配置了多余的默认传出流线"));
        }
        gateway.getOutgoingFlows().forEach(flow -> {
            if (StringUtils.isNotBlank(flow.getConditionExpression())) {
                list.add(newError(gateway.getId(), name, String.format("的传出流【%s】配置了多余的条件【%s】",
                        flow.getId(), flow.getConditionExpression())));
            }
        });
    }

    /**
     * 包容性网关执行所有满足条件的传出流 没有条件就是一直为true
     * 其他传出流条件都不满足的情况执行默认流程 默认流程线上的条件表达式无用
     */
    private static void validate(InclusiveGateway gateway, List<ValidationError> list) {
        String name = "包容性网关" + StringUtils.trimToEmpty(gateway.getName());
        if (gateway.getOutgoingFlows().size() < INT2) {
            list.add(newError(gateway.getId(), name, INT2_MSG));
        }
        if (StringUtils.isNotBlank(gateway.getDefaultFlow())) {
            SequenceFlow flow = gateway.getOutgoingFlows().stream().filter(seq -> gateway.getDefaultFlow()
                    .equals(seq.getConditionExpression())).collect(Collectors.toList()).get(0);
            if (StringUtils.isNotBlank(flow.getConditionExpression())) {
                list.add(newError(gateway.getId(), name, String.format("默认传出流配置了多余的条件【%s】", flow.getConditionExpression())));
            }
        }
    }

    private static ValidationError newError(String id, String name, String desc) {
        ValidationError error = new ValidationError();
        error.setActivityId(id);
        error.setActivityName(name);
        error.setDefaultDescription(desc);
        return error;
    }

}
