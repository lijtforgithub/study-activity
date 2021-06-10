package com.guowy.workflow.webapp.config.cmd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.MultiInstanceLoopCharacteristics;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.util.ProcessDefinitionUtil;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

import static com.guowy.workflow.webapp.constant.Constant.*;

/**
 * 用户任务加签命令
 *
 * @author LiJingTang
 * @date 2020-05-22 16:40
 */
@Slf4j
@Data
@AllArgsConstructor
public class AddMultiInstanceCmd implements Command<Void> {
    /**
     * 加签任务
     */
    private Task task;
    /**
     * 办理人ID
     */
    private String assignee;

    @Override
    public Void execute(CommandContext context) {
        Assert.notNull(task, "加签任务为空");
        Assert.isTrue(StringUtils.isNotBlank(assignee), "加签办理人为空");
        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(task.getProcessDefinitionId());
        FlowElement element = bpmnModel.getFlowElement(task.getTaskDefinitionKey());
        Assert.isInstanceOf(UserTask.class, element, "节点【" + task.getTaskDefinitionKey() + "】不是用户任务节点");
        UserTask userTask = (UserTask) element;
        MultiInstanceLoopCharacteristics characteristics = userTask.getLoopCharacteristics();
        Assert.notNull(characteristics, String.format("用户任务%s【%s】未配置多实例，不能加签", userTask.getName(), userTask.getId()));
        ExecutionEntity entity = context.getExecutionEntityManager().findById(task.getExecutionId());
        ExecutionEntity parent = entity.getParent();
        Integer nrOfInstances = (Integer) parent.getVariableLocal(NUMBER_OF_INSTANCES);
        Integer nrOfActiveInstances = (Integer) parent.getVariableLocal(NUMBER_OF_ACTIVE_INSTANCES);
        parent.setVariableLocal(NUMBER_OF_INSTANCES, nrOfInstances + 1);
        parent.setVariableLocal(NUMBER_OF_ACTIVE_INSTANCES, nrOfActiveInstances + 1);

        if (characteristics.isSequential()) {
            String varName = characteristics.getInputDataItem();
            Assert.isTrue(StringUtils.isNotBlank(varName) && !varName.contains(VAR),
                    String.format("用户任务%s【%s】多实例集合配置方式【%s】不能加签", userTask.getName(), userTask.getId(), varName));
            Object value = parent.getVariableLocal(varName);
            while (Objects.isNull(value)) {
                parent = parent.getParent();
                if (Objects.isNull(parent)) {
                    break;
                }
                value = parent.getVariableLocal(varName);
            }
            Assert.notNull(value, varName + "值为空");
            List<String> userIds = (List<String>) value;
            int index = userIds.indexOf(task.getAssignee());
            userIds.add(index + 1, assignee);
            parent.setVariableLocal(varName, userIds);
        } else {
            ExecutionEntity newExecution = context.getExecutionEntityManager().createChildExecution(parent);
            newExecution.setCurrentFlowElement(userTask);
            newExecution.setVariableLocal(characteristics.getElementVariable(), assignee);
            context.getHistoryManager().recordActivityStart(newExecution);
            ParallelMultiInstanceBehavior behavior = (ParallelMultiInstanceBehavior) userTask.getBehavior();
            AbstractBpmnActivityBehavior innerBehavior = behavior.getInnerActivityBehavior();
            innerBehavior.execute(newExecution);
            TaskEntity newTask = newExecution.getTasks().get(0);
            log.info("用户任务{}-{}加签{}", entity.getProcessDefinitionKey(), newTask.getTaskDefinitionKey(), newTask.getId());
        }

        return null;
    }

}
