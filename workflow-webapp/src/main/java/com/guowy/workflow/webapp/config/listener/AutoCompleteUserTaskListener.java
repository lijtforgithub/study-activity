package com.guowy.workflow.webapp.config.listener;

import com.boot.guowy.cloud.ApplicationContextUtils;
import com.guowy.workflow.webapp.config.cmd.CompleteUserTaskCmd;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.ManagementService;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import static com.guowy.workflow.webapp.config.MainConfig.MANAGEMENT;

/**
 * 用户任务自动办理监听器
 *
 * @author LiJingTang
 * @date 2020-06-04 10:17
 */
@Slf4j
public class AutoCompleteUserTaskListener implements TaskListener {

    private static final long serialVersionUID = -7777241569150989180L;

    private static final ManagementService managementService;

    static {
        managementService = ApplicationContextUtils.getBean(MANAGEMENT, ManagementService.class);
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        String assignee = delegateTask.getAssignee();
        Assert.isTrue(StringUtils.isNotBlank(assignee), String.format("自动办理任务【%s（%s）】办理人为空",
                delegateTask.getName(), delegateTask.getTaskDefinitionKey()));
        managementService.executeCommand(new CompleteUserTaskCmd(delegateTask.getId(), null));
    }

}
