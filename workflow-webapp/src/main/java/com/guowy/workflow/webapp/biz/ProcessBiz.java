package com.guowy.workflow.webapp.biz;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.guowy.cloud.common.enums.StatusEnum;
import com.guowy.cloud.common.util.JsonResult;
import com.guowy.cloud.security.context.UserContextHolder;
import com.guowy.workflow.webapp.dto.ProcessQueryDTO;
import com.guowy.workflow.webapp.util.PageUtils;
import com.guowy.workflow.webapp.util.ParamUtils;
import com.guowy.workflow.webapp.vo.ProcessVO;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentQuery;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author LiJingTang
 * @date 2020-05-14 18:08
 */
@Slf4j
@Service
public class ProcessBiz {

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private HistoryService historyService;

    /**
     * 分页查询流程定义
     */
    public JsonResult<PageInfo<ProcessVO>> findByPage(ProcessQueryDTO queryDTO) {
        ProcessDefinitionQuery query = getProcessQuery(queryDTO);
        Page<ProcessVO> page = PageUtils.newPage(queryDTO, query.count());

        if (page.getTotal() > 0) {
            DeploymentQuery deployQuery = repositoryService.createDeploymentQuery();
            Map<String, Deployment> deploymentMap = Maps.newHashMapWithExpectedSize(page.getPageSize());
            Function<String, Deployment> fun = id -> deployQuery.deploymentId(id).singleResult();
            List<ProcessDefinition> list = query.orderByProcessDefinitionKey().asc()
                    .orderByProcessDefinitionVersion().desc()
                    .listPage(page.getStartRow(), page.getPageSize());
            page.addAll(list.stream().map(p -> ProcessVO.build(p, deploymentMap
                    .computeIfAbsent(p.getDeploymentId(), fun),
                    repositoryService.getBpmnModel(p.getId()).getStartFormKey(p.getKey()))).collect(Collectors.toList()));
        }

        return new JsonResult<>(StatusEnum.OK.getValue(), null, new PageInfo<>(page));

    }

    /**
     * 封装查询对象
     */
    private ProcessDefinitionQuery getProcessQuery(ProcessQueryDTO queryDTO) {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

        if (StringUtils.isNotBlank(queryDTO.getKey())) {
            query.processDefinitionKey(queryDTO.getKey());
        }
        if (StringUtils.isNotBlank(queryDTO.getName())) {
            query.processDefinitionNameLike(ParamUtils.bothLike(queryDTO.getName()));
        }
        if (StringUtils.isNotBlank(queryDTO.getCategory())) {
            query.processDefinitionCategory(queryDTO.getCategory());
        }
        if (Objects.nonNull(queryDTO.getEnable())) {
            if (Boolean.TRUE.equals(queryDTO.getEnable())) {
                query.active();
            } else {
                query.suspended();
            }
        }

        return query;
    }

    /**
     * 根据ID查询流程定义
     */
    public JsonResult<ProcessVO> get(String id) {
        ProcessDefinition process = checkAndGet(id);
        String auditUrl = repositoryService.getBpmnModel(process.getId()).getStartFormKey(process.getKey());
        Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(process.getDeploymentId()).singleResult();
        ProcessVO vo = ProcessVO.build(process, deployment, auditUrl);
        if (Objects.nonNull(vo)) {
            vo.setAuditUrl(repositoryService.getBpmnModel(id).getStartFormKey(process.getKey()));
        }
        return new JsonResult<>(StatusEnum.OK.getValue(), null, vo);
    }

    /**
     * 根据部署ID查询流程定义
     */
    public JsonResult<ProcessVO> getByDeployId(String deployId) {
        ProcessDefinition process = repositoryService.createProcessDefinitionQuery().deploymentId(deployId).singleResult();
        Assert.notNull(process, "部署ID【" + deployId + "】流程定义不存在");
        String auditUrl = repositoryService.getBpmnModel(process.getId()).getStartFormKey(process.getKey());
        Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deployId).singleResult();
        return new JsonResult<>(StatusEnum.OK.getValue(), null, ProcessVO.build(process, deployment, auditUrl));
    }

    /**
     * 查询流程定义XML
     */
    public void getXml(String id, OutputStream output) throws IOException {
        Assert.notNull(output, "输出流为空");
        checkAndGet(id);

        try (InputStream input = repositoryService.getProcessModel(id)) {
            IOUtils.copy(input, output);
            output.close();
        }
    }

    /**
     * 查询流程定义图片
     */
    public void getImage(String id, OutputStream output) throws IOException {
        Assert.notNull(output, "输出流为空");
        ProcessDefinition process = checkAndGet(id);

        try (InputStream input = repositoryService.getResourceAsStream(process.getDeploymentId(), process.getDiagramResourceName())) {
            IOUtils.copy(input, output);
            output.close();
        }
    }

    /**
     * 启用
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<Boolean> enable(String id) {
        ProcessDefinition process = checkAndGet(id);
        Assert.isTrue(process.isSuspended(), "流程定义已经是启用状态");
        repositoryService.activateProcessDefinitionById(id);
        log.info("{} 启用流程定义 {}", UserContextHolder.get().getName(), id);
        return new JsonResult<>(StatusEnum.OK.getValue(), null, Boolean.TRUE);
    }

    /**
     * 禁用
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<Boolean> disable(String id) {
        ProcessDefinition process = checkAndGet(id);
        Assert.isTrue(!process.isSuspended(), "流程定义已经是禁用状态");
        repositoryService.suspendProcessDefinitionById(id);
        log.info("{} 禁用流程定义 {}", UserContextHolder.get().getName(), id);
        return new JsonResult<>(StatusEnum.OK.getValue(), null, Boolean.TRUE);
    }

    /**
     * 删除
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<Boolean> delete(String id) {
        ProcessDefinition process = checkAndGet(id);
        long count = historyService.createHistoricProcessInstanceQuery().processDefinitionId(id).count();
        Assert.isTrue(count == 0, "该流程定义已经生成实例，请先去删除流程实例");
        repositoryService.deleteDeployment(process.getDeploymentId(), true);
        log.info("{} 删除流程定义 {}", UserContextHolder.get().getName(), id);
        return new JsonResult<>(StatusEnum.OK.getValue(), null, Boolean.TRUE);
    }

    /**
     * 根据ID查询流程定义校验并返回
     */
    private ProcessDefinition checkAndGet(String id) {
        ProcessDefinition process = repositoryService.createProcessDefinitionQuery().processDefinitionId(id).singleResult();
        Assert.notNull(process, "ID【" + id + "】流程定义不存在");
        return process;
    }

}
