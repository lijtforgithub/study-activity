package com.guowy.workflow.webapp.config.handler;

import com.google.common.collect.Maps;
import com.guowy.workflow.webapp.config.listener.AutoCompleteUserTaskListener;
import com.guowy.workflow.webapp.config.listener.UserTaskMessageListener;
import org.activiti.bpmn.model.ActivitiListener;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.bpmn.parser.handler.UserTaskParseHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.activiti.bpmn.model.ImplementationType.IMPLEMENTATION_TYPE_CLASS;
import static org.activiti.engine.delegate.BaseTaskListener.EVENTNAME_CREATE;

/**
 * 自定义用户任务解析器
 *
 * @author LiJingTang
 * @date 2020-06-04 11:00
 */
@Component
public class CustomUserTaskParseHandler extends UserTaskParseHandler {

    /**
     * 自动办理任务配置
     */
    public static final String AUTO_COMPLETE = "AutoComplete";
    private static final Map<String, String> LISTENER_MAP;

    static {
        LISTENER_MAP = Maps.newHashMapWithExpectedSize(1);
        LISTENER_MAP.put(AUTO_COMPLETE, AutoCompleteUserTaskListener.class.getName());
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, UserTask userTask) {
        super.executeParse(bpmnParse, userTask);

        boolean isNotice = true;
        for (ActivitiListener listener : userTask.getTaskListeners()) {
            if (IMPLEMENTATION_TYPE_CLASS.equals(listener.getImplementationType())
                    && LISTENER_MAP.containsKey(listener.getImplementation())) {
                // 自动办理任务监听器
                listener.setImplementation(LISTENER_MAP.get(listener.getImplementation()));
                isNotice = false;
            }
        }
        // 自动办理不需要提醒
        if (isNotice) {
            userTask.getTaskListeners().add(getUserTaskMessageListener());
        }
    }

    private static ActivitiListener getUserTaskMessageListener() {
        ActivitiListener listener = new ActivitiListener();
        listener.setEvent(EVENTNAME_CREATE);
        listener.setImplementationType(IMPLEMENTATION_TYPE_CLASS);
        listener.setImplementation(UserTaskMessageListener.class.getName());

        return listener;
    }

}
