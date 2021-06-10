package com.guowy.workflow.webapp.util;

import com.guowy.workflow.dto.UserTaskDTO;
import com.guowy.workflow.webapp.enums.RecordStatusEnum;
import de.odysseus.el.util.SimpleContext;
import org.activiti.bpmn.model.SequenceFlow;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author LiJingTang
 * @date 2020-05-21 19:17
 */
public class WorkFlowUtils {

    private WorkFlowUtils() {}

    /**
     * 关键字pass存入数值
     */
    public static int getPassValue(Boolean pass) {
        return Boolean.TRUE.equals(pass) ? RecordStatusEnum.PASS.getValue() : RecordStatusEnum.REJECT.getValue();
    }


    /**
     * 用户类型用户ID间隔符
     */
    public static final String SEP = ":";

    /**
     * 连接用户类型和用户ID
     */
    public static String joinTaskUser(Integer userType, Long userId) {
        return userType + SEP + userId;
    }


    /**
     * 拆分用户类型和用户ID
     */
    public static UserTaskDTO.TaskUser sepTaskUser(String userId) {
        String[] array = userId.split(SEP);
        UserTaskDTO.TaskUser taskUser = new UserTaskDTO.TaskUser();
        taskUser.setUserType(Integer.valueOf(array[0]));
        taskUser.setUserId(Long.valueOf(array[1]));
        return taskUser;
    }


    /**
     * url固定占位符任务ID
     */
    private static final String URL_TASK = "{taskId}";
    /**
     * url固定占位符业务ID
     */
    private static final String URL_BIZ = "{bizId}";

    /**
     * 替换审核url变量占位符
     */
    public static String getAuditUrl(String url, String taskId, String bizId, Map<String, Object> varMap) {
        if (StringUtils.isBlank(url)) {
            return null;
        }

        url = url.replace(URL_TASK, taskId).replace(URL_BIZ, bizId);

        if (!CollectionUtils.isEmpty(varMap)) {
            for (Map.Entry<String, Object> entry : varMap.entrySet()) {
                url = url.replace("{" + entry.getKey() + "}", entry.getValue().toString());
            }
        }

        return url;
    }


    /**
     * 计算表达式值
     */
    private static boolean calcExpression(ExpressionFactory factory, String expression, Map<String, Object> varMap) {
        SimpleContext context = new SimpleContext();
        varMap.forEach((k, v) -> context.setVariable(k, factory.createValueExpression(v, v.getClass())));
        ValueExpression valueExpression = factory.createValueExpression(context, expression, Boolean.class);
        return Boolean.parseBoolean(valueExpression.getValue(context).toString());
    }

    /**
     * 计算传出线的表达式 返回满足条件的
     */
    public static List<SequenceFlow> getSelectOutFlow(List<SequenceFlow> sequenceFlows, ExpressionFactory factory, Map<String, Object> varMap) {
        if (CollectionUtils.isEmpty(sequenceFlows)) {
            return sequenceFlows;
        }

        return sequenceFlows.stream().filter(seq -> StringUtils.isBlank(seq.getConditionExpression()) || calcExpression(factory,
                seq.getConditionExpression(), varMap)).collect(Collectors.toList());
    }

}
