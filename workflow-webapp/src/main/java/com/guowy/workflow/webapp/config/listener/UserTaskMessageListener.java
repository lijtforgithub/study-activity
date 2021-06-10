package com.guowy.workflow.webapp.config.listener;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

/**
 * 用户任务创建提醒办理监听器
 *
 * @author LiJingTang
 * @date 2020-05-17 14:10
 */
@Slf4j
public class UserTaskMessageListener implements TaskListener {

    private static final long serialVersionUID = -8589905261464042982L;

    @Override
    public void notify(DelegateTask delegateTask) {
        log.info("实例【{}】任务【{}】监听器", delegateTask.getProcessInstanceId(),
                delegateTask.getName());
        /**
         是否自动删除流程实例
         Object value = delegateTask.getVariable(BIZ_MSG);
         String assignee = delegateTask.getAssignee();
         List<String> userIds = StringUtils.isNotBlank(assignee) ? Lists.newArrayList(assignee) :
         delegateTask.getCandidates().stream().map(IdentityLink::getUserId).collect(Collectors.toList());
         **/
    }

}
