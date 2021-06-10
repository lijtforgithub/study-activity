package com.guowy.workflow.webapp.config.cmd;

import com.guowy.workflow.dto.UserTaskDTO;
import com.guowy.workflow.webapp.dto.InstanceRecordDTO;
import com.guowy.workflow.webapp.enums.RecordStatusEnum;
import com.guowy.workflow.webapp.enums.RecordTypeEnum;
import com.guowy.workflow.webapp.service.InstanceRecordService;
import com.guowy.workflow.webapp.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.impl.cmd.CompleteTaskCmd;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static com.guowy.workflow.webapp.util.WorkFlowUtils.sepTaskUser;

/**
 * 自动办理用户任务命令
 *
 * @author LiJingTang
 * @date 2020-06-04 13:41
 */
@Slf4j
public class CompleteUserTaskCmd extends CompleteTaskCmd {

    private static final long serialVersionUID = 7874977832297386269L;

    public CompleteUserTaskCmd(String taskId, Map<String, Object> variables) {
        super(taskId, variables);
    }

    @Override
    protected Void execute(CommandContext commandContext, TaskEntity task) {
        super.execute(commandContext, task);
        log.info("自动完成任务：{}", task.getId());

        SpringProcessEngineConfiguration config = (SpringProcessEngineConfiguration) commandContext.getProcessEngineConfiguration();
        ApplicationContext applicationContext = config.getApplicationContext();
        UserService userService = applicationContext.getBean(UserService.class);
        InstanceRecordService recordService = applicationContext.getBean(InstanceRecordService.class);
        String processKey = config.getRepositoryService().getProcessDefinition(task.getProcessDefinitionId()).getKey();

        // 保存审核日志
        recordService.save(assembleRecord(task, userService, processKey));

        return null;
    }

    private InstanceRecordDTO assembleRecord(TaskEntity task, UserService userService, String processKey) {
        UserTaskDTO.TaskUser taskUser = sepTaskUser(task.getAssignee());
        ExecutionEntity execution = task.getExecution();
        InstanceRecordDTO recordDTO = new InstanceRecordDTO();
        recordDTO.setType(RecordTypeEnum.USER_TASK.getValue());
        recordDTO.setStatus(RecordStatusEnum.PASS.getValue());
        recordDTO.setProcessKey(processKey);
        recordDTO.setBizId(execution.getProcessInstanceBusinessKey());
        recordDTO.setInstanceId(execution.getProcessInstanceId());
        recordDTO.setExecutionId(execution.getId());
        recordDTO.setTaskKey(task.getTaskDefinitionKey());
        recordDTO.setUserType(taskUser.getUserType());
        recordDTO.setUserId(taskUser.getUserId());
        recordDTO.setUserName(userService.getName(recordDTO.getUserType(), recordDTO.getUserId()));
        recordDTO.setContent("通过");
        recordDTO.setCreateTime(System.currentTimeMillis());
        return recordDTO;
    }

}
