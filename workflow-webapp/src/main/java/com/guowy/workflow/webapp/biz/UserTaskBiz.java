package com.guowy.workflow.webapp.biz;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.guowy.cloud.common.enums.StatusEnum;
import com.guowy.cloud.common.util.JsonResult;
import com.guowy.cloud.security.context.UserContextHolder;
import com.guowy.cloud.security.enums.UserTypeEnum;
import com.guowy.workflow.dto.UserTaskCompleteDTO;
import com.guowy.workflow.dto.UserTaskDTO;
import com.guowy.workflow.dto.UserTaskQueryDTO;
import com.guowy.workflow.dto.UserTaskResponseDTO;
import com.guowy.workflow.webapp.config.cmd.AddMultiInstanceCmd;
import com.guowy.workflow.webapp.dto.HistoryUserTaskQueryDTO;
import com.guowy.workflow.webapp.dto.InstanceRecordDTO;
import com.guowy.workflow.webapp.dto.UserTaskSignDTO;
import com.guowy.workflow.webapp.enums.RecordStatusEnum;
import com.guowy.workflow.webapp.enums.RecordTypeEnum;
import com.guowy.workflow.webapp.service.CustomOperateService;
import com.guowy.workflow.webapp.service.InstanceRecordService;
import com.guowy.workflow.webapp.service.UserService;
import com.guowy.workflow.webapp.util.PageUtils;
import com.guowy.workflow.webapp.util.ParamUtils;
import com.guowy.workflow.webapp.vo.HistoryUserTaskVO;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.el.ExpressionFactory;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.guowy.workflow.webapp.constant.Constant.*;
import static com.guowy.workflow.webapp.util.WorkFlowUtils.*;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author LiJingTang
 * @date 2020-05-17 20:24
 */
@Slf4j
@Service
public class UserTaskBiz {

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private ManagementService managementService;
    @Autowired
    private InstanceRecordService recordService;
    @Autowired
    private UserService userService;
    @Autowired
    private ExpressionFactory expressionFactory;
    @Autowired
    private InstanceBiz instanceBiz;
    @Autowired
    private CustomOperateService customOperateService;

    /**
     * ????????????
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<UserTaskResponseDTO> complete(UserTaskCompleteDTO completeDTO) {
        Task task = checkAndGet(completeDTO.getTaskId());
        Assert.isTrue(!task.isSuspended(), "ID???" + completeDTO.getTaskId() + "??????????????????");
        String taskUser = joinTaskUser(completeDTO.getUserType(), completeDTO.getUserId());
        Assert.isTrue(getTaskUserId(task).contains(taskUser), "??????????????????????????????");

        HistoricProcessInstance instance = completeTask(task, completeDTO, taskUser);
        return new JsonResult<>(StatusEnum.OK.getValue(), null, assembleResponse(task, instance));
    }

    /**
     * ????????????
     */
    private HistoricProcessInstance completeTask(Task task, UserTaskCompleteDTO completeDTO, String assignee) {
        boolean noOutGoing = isNoOutGoing(task, completeDTO);
        Map<String, Object> map = Maps.newHashMapWithExpectedSize(1);
        map.put(PASS, noOutGoing ? RecordStatusEnum.PASS.getValue() : getPassValue(completeDTO.getPass()));
        // ?????????????????????
        if (StringUtils.isBlank(task.getAssignee())) {
            taskService.claim(task.getId(), assignee);
        }
        // ??????????????????
        taskService.complete(task.getId(), CollectionUtils.isEmpty(completeDTO.getVarMap()) ? null : completeDTO.getVarMap(), map);
        if (noOutGoing) {
            long count = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).count();
            if (count > 0) {
                runtimeService.deleteProcessInstance(task.getProcessInstanceId(), RecordStatusEnum.REJECT.name());
                customOperateService.terminateInstance(task.getProcessInstanceId(), RecordStatusEnum.REJECT.name());
            }
        }
        // ??????????????????
        HistoricProcessInstance instance = instanceBiz.getHistoryInstance(task.getProcessInstanceId());
        saveRecord(completeDTO, instance, task);

        return instance;
    }

    /**
     * ???????????????????????????????????????
     */
    private Task checkAndGet(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        Assert.notNull(task, "ID???" + taskId + "??????????????????");
        return task;
    }

    /**
     * ???????????????????????????????????????????????????
     */
    private boolean isNoOutGoing(Task task, UserTaskCompleteDTO completeDTO) {
        if (Boolean.TRUE.equals(completeDTO.getPass())) {
            return false;
        }

        Map<String, Object> varMap = runtimeService.getVariables(task.getProcessInstanceId());
        if (!CollectionUtils.isEmpty(completeDTO.getVarMap())) {
            varMap.putAll(completeDTO.getVarMap());
        }
        varMap.put(PASS, RecordStatusEnum.REJECT.getValue());
        FlowElement element = repositoryService.getBpmnModel(task.getProcessDefinitionId())
                .getFlowElement(task.getTaskDefinitionKey());
        return CollectionUtils.isEmpty(getSelectOutFlow(((UserTask) element).getOutgoingFlows(), expressionFactory, varMap));
    }

    /**
     * ???????????????
     */
    private UserTaskResponseDTO assembleResponse(Task task, HistoricProcessInstance instance) {
        long count = taskService.createTaskQuery().processInstanceId(instance.getId())
                .taskDefinitionKey(task.getTaskDefinitionKey()).count();
        UserTaskResponseDTO dto = new UserTaskResponseDTO();
        dto.setTaskKey(task.getTaskDefinitionKey());
        dto.setInstanceEnd(Objects.nonNull(instance.getEndTime()));
        dto.setTaskEnd(count == 0L);
        return dto;
    }

    /**
     * ????????????????????????
     */
    public JsonResult<PageInfo<UserTaskDTO>> findTodoByPage(UserTaskQueryDTO queryDTO) {
        TaskQuery query = getTaskQuery(queryDTO);
        Page<UserTaskDTO> page = PageUtils.newPage(queryDTO, query.count());

        if (page.getTotal() > 0) {
            List<Task> list = query.orderByTaskCreateTime().asc().listPage(page.getStartRow(), page.getPageSize());
            // ??????????????????URL
            Map<String, String> urlMap = Maps.newHashMapWithExpectedSize(list.size());
            // ??????????????????
            Map<String, Map<String, Object>> varMap = Maps.newHashMapWithExpectedSize(list.size());
            // ????????????
            Map<String, ProcessInstance> instanceMap = Maps.newHashMapWithExpectedSize(list.size());
            Function<String, Map<String, Object>> varFun = id -> runtimeService.getVariables(id);
            Function<String, ProcessInstance> instanceFun = id -> runtimeService.createProcessInstanceQuery().processInstanceId(id).singleResult();

            page.addAll(list.stream().map(task -> {
                UserTaskDTO dto = getUserTaskDTO(task, instanceMap.computeIfAbsent(task.getProcessInstanceId(), instanceFun));
                // ????????????????????????url ??????????????????????????????
                String url = task.getFormKey();
                if (StringUtils.isBlank(url)) {
                    if (!urlMap.containsKey(dto.getProcessId())) {
                        String processKey = instanceMap.get(dto.getInstanceId()).getProcessDefinitionKey();
                        urlMap.put(dto.getProcessId(), repositoryService.getBpmnModel(dto.getProcessId()).getStartFormKey(processKey));
                    }
                    url = urlMap.get(dto.getProcessId());
                }
                dto.setAuditUrl(getAuditUrl(url, task.getId(), dto.getBizId(), varMap.computeIfAbsent(dto.getInstanceId(), varFun)));
                // ?????????????????????????????? ?????????????????????
                if (!ObjectUtils.allNotNull(queryDTO.getUserType(), queryDTO.getUserId())) {
                    dto.setUsers(getTaskUsers(task));
                }
                return dto;
            }).collect(Collectors.toList()));
        }

        return new JsonResult<>(StatusEnum.OK.getValue(), null, new PageInfo<>(page));
    }

    /**
     * ????????????????????????
     */
    private UserTaskDTO getUserTaskDTO(Task task, ProcessInstance instance) {
        UserTaskDTO dto = new UserTaskDTO();
        dto.setTaskId(task.getId());
        dto.setTaskName(task.getName());
        dto.setCreateTime(task.getCreateTime().getTime());
        dto.setInstanceId(task.getProcessInstanceId());
        dto.setProcessId(task.getProcessDefinitionId());
        dto.setProcessName(instance.getProcessDefinitionName());
        dto.setBizId(instance.getBusinessKey());
        dto.setBizKey(instance.getName());
        return dto;
    }

    /**
     * ????????????????????????????????????
     */
    private List<UserTaskDTO.TaskUser> getTaskUsers(Task task) {
        Set<String> userIds = getTaskUserId(task);
        return userIds.stream().map(userId -> {
            UserTaskDTO.TaskUser user = sepTaskUser(userId);
            user.setUserName(userService.getName(user.getUserType(), user.getUserId()));
            return user;
        }).collect(Collectors.toList());
    }

    /**
     * ??????????????????????????????
     */
    private Set<String> getTaskUserId(Task task) {
        return StringUtils.isNotBlank(task.getAssignee()) ? Sets.newHashSet(task.getAssignee()) :
                taskService.getIdentityLinksForTask(task.getId()).stream().map(IdentityLink::getUserId).collect(Collectors.toSet());
    }

    /**
     * ????????????????????????
     */
    private TaskQuery getTaskQuery(UserTaskQueryDTO queryDTO) {
        // ??????????????????????????????????????????
        TaskQuery query = taskService.createTaskQuery().active()
                .processInstanceBusinessKey(trimToNull(queryDTO.getBizId()));

        if (!CollectionUtils.isEmpty(queryDTO.getProcessKeys())) {
            query.processDefinitionKeyIn(queryDTO.getProcessKeys());
        }
        if (StringUtils.isNotBlank(queryDTO.getBizKey())) {
            query.processVariableValueLikeIgnoreCase(BIZ_KEY, ParamUtils.suffixLike(queryDTO.getBizKey()));
        }
        if (ObjectUtils.allNotNull(queryDTO.getUserType(), queryDTO.getUserId())) {
            query.taskCandidateOrAssigned(joinTaskUser(queryDTO.getUserType(), queryDTO.getUserId()));
        }

        return query;
    }

    /**
     * ??????????????????
     */
    private void saveRecord(UserTaskCompleteDTO completeDTO, HistoricProcessInstance instance, Task task) {
        InstanceRecordDTO recordDTO = new InstanceRecordDTO();
        recordDTO.setType(RecordTypeEnum.USER_TASK.getValue());
        recordDTO.setStatus(getPassValue(completeDTO.getPass()));
        recordDTO.setProcessKey(instance.getProcessDefinitionKey());
        recordDTO.setBizId(instance.getBusinessKey());
        recordDTO.setInstanceId(instance.getId());
        recordDTO.setExecutionId(task.getExecutionId());
        recordDTO.setTaskKey(task.getTaskDefinitionKey());
        recordDTO.setUserType(completeDTO.getUserType());
        recordDTO.setUserId(completeDTO.getUserId());
        recordDTO.setUserName(userService.getName(completeDTO.getUserType(), completeDTO.getUserId()));
        recordDTO.setContent(StringUtils.left(completeDTO.getContent(), CONTENT_LEN));
        recordDTO.setCreateTime(System.currentTimeMillis());
        recordService.save(recordDTO);
    }

    /**
     * ?????????????????????
     */
    public JsonResult<Boolean> signAdd(UserTaskSignDTO signDTO) {
        Task task = checkAndGet(signDTO.getTaskId());
        managementService.executeCommand(new AddMultiInstanceCmd(task,
                joinTaskUser(signDTO.getUserType(), signDTO.getUserId())));
        return new JsonResult<>(StatusEnum.OK.getValue(), null, Boolean.TRUE);
    }

    /**
     * ???????????????
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<Boolean> assign(UserTaskSignDTO signDTO) {
        Task task = checkAndGet(signDTO.getTaskId());
        String taskUser = joinTaskUser(signDTO.getUserType(), signDTO.getUserId());

        if (StringUtils.isNotBlank(task.getAssignee())) {
            Assert.isTrue(!task.getAssignee().equalsIgnoreCase(taskUser), "????????????????????????????????????");
            taskService.setAssignee(signDTO.getTaskId(), taskUser);
            taskService.setOwner(signDTO.getTaskId(), task.getAssignee());
            log.info("{} ???????????????{}???????????????{}???", UserContextHolder.get().getName(), signDTO.getTaskId(), taskUser);
        } else {
            taskService.claim(signDTO.getTaskId(), taskUser);
            log.info("{} ???????????????{}???????????????{}?????????", UserContextHolder.get().getName(), signDTO.getTaskId(), taskUser);
        }

        return new JsonResult<>(StatusEnum.OK.getValue(), null, Boolean.TRUE);
    }

    /**
     * ??????????????????
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<Boolean> systemComplete(UserTaskCompleteDTO completeDTO) {
        Task task = checkAndGet(completeDTO.getTaskId());
        String assignee;
        if (UserTypeEnum.UNKNOWN.getValue().equals(completeDTO.getUserType()) || 0 == completeDTO.getUserId()) {
            assignee = new ArrayList<>(getTaskUserId(task)).get(0);
            UserTaskDTO.TaskUser taskUser = sepTaskUser(assignee);
            completeDTO.setUserType(taskUser.getUserType());
            completeDTO.setUserId(taskUser.getUserId());
        } else {
            assignee = joinTaskUser(completeDTO.getUserType(), completeDTO.getUserId());
        }
        completeTask(task, completeDTO, assignee);

        return new JsonResult<>(StatusEnum.OK.getValue(), null, Boolean.TRUE);
    }

    /**
     * ?????????????????????
     */
    public JsonResult<PageInfo<HistoryUserTaskVO>> findHistoryByPage(HistoryUserTaskQueryDTO queryDTO) {
        HistoricTaskInstanceQuery query = getHistoryQuery(queryDTO);
        Page<HistoryUserTaskVO> page = PageUtils.newPage(queryDTO, query.count());

        if (page.getTotal() > 0) {
            List<HistoricTaskInstance> list = query.orderByTaskCreateTime().asc().listPage(page.getStartRow(), page.getEndRow());
            Map<String, HistoricProcessInstance> instanceMap = Maps.newHashMapWithExpectedSize(list.size());
            Function<String, HistoricProcessInstance> instanceFun =
                    id -> historyService.createHistoricProcessInstanceQuery().processInstanceId(id).singleResult();
            page.addAll(list.stream().map(task -> {
                HistoryUserTaskVO vo = HistoryUserTaskVO.build(task, instanceMap.computeIfAbsent(task.getProcessInstanceId(), instanceFun));
                if (Objects.nonNull(vo)) {
                    UserTaskDTO.TaskUser taskUser = sepTaskUser(vo.getAssignee());
                    vo.setAssignee(userService.getName(taskUser.getUserType(), taskUser.getUserId()));
                }
                return vo;
            }).collect(Collectors.toList()));
        }

        return new JsonResult<>(StatusEnum.OK.getValue(), null, new PageInfo<>(page));
    }

    /**
     * ???????????????????????????
     */
    private HistoricTaskInstanceQuery getHistoryQuery(HistoryUserTaskQueryDTO queryDTO) {
        return historyService.createHistoricTaskInstanceQuery().finished()
                .taskId(trimToNull(queryDTO.getTaskId()))
                .processInstanceId(trimToNull(queryDTO.getInstanceId()))
                .processDefinitionKey(trimToNull(queryDTO.getProcessKey()))
                .processInstanceBusinessKey(trimToNull(queryDTO.getBizId()))
                .taskName(trimToNull(queryDTO.getTaskName()));
    }

}
