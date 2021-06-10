package com.guowy.workflow.webapp.biz;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.guowy.cloud.common.enums.StatusEnum;
import com.guowy.cloud.common.util.JsonResult;
import com.guowy.cloud.security.context.UserContextHolder;
import com.guowy.workflow.webapp.dto.ModelCreateDTO;
import com.guowy.workflow.webapp.dto.ModelQueryDTO;
import com.guowy.workflow.webapp.dto.ModelUpdateDTO;
import com.guowy.workflow.webapp.util.PageUtils;
import com.guowy.workflow.webapp.util.ParamUtils;
import com.guowy.workflow.webapp.vo.ModelVO;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ModelQuery;
import org.activiti.validation.ValidationError;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.guowy.workflow.webapp.constant.Constant.CREATOR;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.activiti.editor.constants.EditorJsonConstants.EDITOR_SHAPE_ID;
import static org.activiti.editor.constants.EditorJsonConstants.EDITOR_SHAPE_PROPERTIES;
import static org.activiti.editor.constants.ModelDataJsonConstants.*;
import static org.activiti.editor.constants.StencilConstants.*;
import static org.apache.http.HttpStatus.SC_CREATED;

/**
 * @author LiJingTang
 * @date 2020-05-13 15:32
 */
@Slf4j
@Service
public class ModelBiz {

    private static final String BPMN = ".bpmn";
    private static final String PNG = ".png";
    private static final String STENCIL_SET = "stencilset";

    private ObjectNode stencilSetNode;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RepositoryService repositoryService;

    @PostConstruct
    public void init() {
        stencilSetNode = objectMapper.createObjectNode();
        stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
    }

    /**
     * 创建模型
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<String> create(ModelCreateDTO createDTO) {
        Model origin = getByKey(createDTO.getKey());
        if (Objects.nonNull(origin)) {
            log.info("模型【{}】已存在", createDTO.getKey());
            return new JsonResult<>(SC_CREATED, "KEY【" + createDTO.getKey() + "】模型已存在", origin.getId());
        }
        // 初始化一个模型
        Model model = repositoryService.newModel();
        model.setKey(createDTO.getKey());
        model.setName(createDTO.getName());
        model.setCategory(createDTO.getCategory());
        model.setMetaInfo(assembleMetaInfo(model.getName(), 1, createDTO.getDescription()).toString());
        repositoryService.saveModel(model);
        // 保存模型的xml
        ObjectNode modelNode = objectMapper.createObjectNode();
        modelNode.put(EDITOR_SHAPE_ID, model.getId());
        modelNode.set(STENCIL_SET, stencilSetNode);
        modelNode.set(EDITOR_SHAPE_PROPERTIES, assemblePropNode(createDTO));
        repositoryService.addModelEditorSource(model.getId(), modelNode.toString().getBytes(UTF_8));

        return new JsonResult<>(StatusEnum.OK.getValue(), null, model.getId());
    }

    /**
     * 封装properties节点
     */
    private ObjectNode assemblePropNode(ModelCreateDTO createDTO) {
        ObjectNode propNode = objectMapper.createObjectNode();
        propNode.put(PROPERTY_PROCESS_ID, createDTO.getKey());
        propNode.put(PROPERTY_NAME, createDTO.getName());
        propNode.put(PROPERTY_PROCESS_NAMESPACE, createDTO.getCategory());
        propNode.put(PROPERTY_DOCUMENTATION, createDTO.getDescription());
        propNode.put(PROPERTY_PROCESS_AUTHOR, UserContextHolder.get().getName());
        propNode.put(PROPERTY_PROCESS_VERSION, NumberUtils.INTEGER_ONE);
        return propNode;
    }

    /**
     * 更新模型
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<Boolean> update(ModelUpdateDTO updateDTO) throws IOException {
        Model model = checkAndGet(updateDTO.getId());
        JsonNode node = objectMapper.readTree(updateDTO.getJson_xml()).get(EDITOR_SHAPE_PROPERTIES);
        String key = node.get(PROPERTY_PROCESS_ID).textValue();
        if (!model.getKey().equalsIgnoreCase(key)) {
            Assert.isNull(getByKey(key), "模型【" + key + "】已存在");
        }

        ObjectNode metaNode = (ObjectNode) objectMapper.readTree(model.getMetaInfo());
        metaNode = assembleMetaInfo(updateDTO.getName(), metaNode.get(MODEL_REVISION).asInt() + 1, updateDTO.getDescription());
        model.setMetaInfo(metaNode.toString());
        model.setKey(key);
        model.setName(updateDTO.getName());
        model.setVersion(model.getVersion() + 1);
        // 更新模型
        repositoryService.saveModel(model);
        // 更新xml
        repositoryService.addModelEditorSource(model.getId(), updateDTO.getJson_xml().getBytes(UTF_8));
        // 更新图片
        try (InputStream svgStream = new ByteArrayInputStream(updateDTO.getSvg_xml().getBytes(UTF_8));
             ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            TranscoderInput input = new TranscoderInput(svgStream);
            PNGTranscoder transcoder = new PNGTranscoder();
            TranscoderOutput output = new TranscoderOutput(outStream);
            transcoder.transcode(input, output);
            byte[] png = outStream.toByteArray();
            repositoryService.addModelEditorSourceExtra(model.getId(), png);
        } catch (TranscoderException e) {
            throw new IOException(e);
        }

        return new JsonResult<>(StatusEnum.OK.getValue(), null, Boolean.TRUE);
    }

    /**
     * 根据ID查询模型
     */
    public JsonResult<ObjectNode> get(String id) throws IOException {
        Model model = checkAndGet(id);
        ObjectNode modelNode = (ObjectNode) objectMapper.readTree(model.getMetaInfo());
        byte[] editorSource = repositoryService.getModelEditorSource(model.getId());
        modelNode.put(MODEL_ID, id);
        modelNode.set("model", objectMapper.readTree(new String(editorSource, UTF_8)));

        return new JsonResult<>(StatusEnum.OK.getValue(), null, modelNode);
    }

    /**
     * 分页查询模型
     */
    public JsonResult<PageInfo<ModelVO>> findByPage(ModelQueryDTO queryDTO) {
        ModelQuery query = getModelQuery(queryDTO);
        Page<ModelVO> page = PageUtils.newPage(queryDTO, query.count());

        if (page.getTotal() > 0) {
            List<Model> list = query.orderByModelKey().asc().listPage(page.getStartRow(), page.getPageSize());
            page.addAll(list.stream().map(ModelVO::build).collect(Collectors.toList()));
        }

        return new JsonResult<>(StatusEnum.OK.getValue(), null, new PageInfo<>(page));
    }

    /**
     * 封装查询对象
     */
    private ModelQuery getModelQuery(ModelQueryDTO queryDTO) {
        ModelQuery query = repositoryService.createModelQuery();

        if (StringUtils.isNotBlank(queryDTO.getKey())) {
            query.modelKey(queryDTO.getKey().trim());
        }
        if (StringUtils.isNotBlank(queryDTO.getName())) {
            query.modelNameLike(ParamUtils.bothLike(queryDTO.getName()));
        }
        if (StringUtils.isNotBlank(queryDTO.getCategory())) {
            query.modelCategory(queryDTO.getCategory());
        }

        return query;
    }

    /**
     * 部署模型
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<String> deploy(String id) throws IOException {
        Model model = checkAndGet(id);
        byte[] bytes = repositoryService.getModelEditorSource(model.getId());
        JsonNode modelNode = objectMapper.readTree(bytes);
        BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        Assert.notEmpty(bpmnModel.getProcesses(), "流程定义为空，请先绘制流程图");
        // 校验xml 方便返回异常信息
        List<ValidationError> errorList = repositoryService.validateProcess(bpmnModel);
        if (!CollectionUtils.isEmpty(errorList)) {
            log.warn("Bpmn文件【{}】校验不通过：{}", model.getKey(), JSON.toJSONString(errorList));
            return new JsonResult<>(StatusEnum.FAIL.getValue(), errorList.stream().map(this::getErrorMsg)
                    .distinct().collect(Collectors.joining(" ")), null);
        }
        // 设置分类
        bpmnModel.setTargetNamespace(model.getCategory());
        byte[] png = repositoryService.getModelEditorSourceExtra(model.getId());
        // 部署
        Deployment deploy = repositoryService.createDeployment()
                // 上面已经校验 此处关闭
                .disableBpmnValidation()
                .key(model.getKey())
                .name(UserContextHolder.get().getName())
                .category(model.getCategory())
                .addBpmnModel(model.getKey() + BPMN, bpmnModel)
                .addBytes(model.getKey() + PNG, png)
                .deploy();
        model.setDeploymentId(deploy.getId());
        setUpdater(model);
        repositoryService.saveModel(model);

        return new JsonResult<>(StatusEnum.OK.getValue(), null, deploy.getId());
    }

    private String getErrorMsg(ValidationError error) {
        return error.getActivityName() + "[" + error.getActivityId() + "] " + error.getDefaultDescription();
    }

    /**
     * 更新模型分类
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<Boolean> updateCategory(String id, String category) throws IOException {
        Model model = checkAndGet(id);
        model.setCategory(category);
        setUpdater(model);
        repositoryService.saveModel(model);
        return new JsonResult<>(StatusEnum.OK.getValue(), null, Boolean.TRUE);
    }

    /**
     * 删除模型
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<Boolean> delete(String id) {
        checkAndGet(id);
        repositoryService.deleteModel(id);
        return new JsonResult<>(StatusEnum.OK.getValue(), null, Boolean.TRUE);
    }

    /**
     * 修改最后操作人
     */
    private void setUpdater(Model model) throws IOException {
        ObjectNode metaNode = (ObjectNode) objectMapper.readTree(model.getMetaInfo());
        metaNode.put(MODEL_REVISION, metaNode.get(MODEL_REVISION).asInt() + 1);
        metaNode.put(CREATOR, UserContextHolder.get().getName());
        model.setMetaInfo(metaNode.toString());
    }

    /**
     * 封装模型元数据
     */
    private ObjectNode assembleMetaInfo(String name, int revision, String description) {
        ObjectNode metaNode = objectMapper.createObjectNode();
        metaNode.put(MODEL_NAME, name);
        metaNode.put(MODEL_DESCRIPTION, description);
        metaNode.put(MODEL_REVISION, revision);
        metaNode.put(CREATOR, UserContextHolder.get().getName());
        return metaNode;
    }

    /**
     * 根据key查询模型
     */
    private Model getByKey(String key) {
        return repositoryService.createModelQuery().modelKey(key.trim()).singleResult();
    }

    /**
     * 根据ID查询模型校验并返回
     */
    public Model checkAndGet(String id) {
        Model model = repositoryService.createModelQuery().modelId(id).singleResult();
        Assert.notNull(model, "ID【" + id + "】模型不存在");
        return model;
    }

    /**
     * 下载模型的bpmn文件和图片
     */
    public JsonResult<Boolean> download(Model model, OutputStream out) throws IOException {
        Assert.notNull(out, "输出流为空");
        byte[] bpmn = repositoryService.getModelEditorSource(model.getId());
        byte[] png = repositoryService.getModelEditorSourceExtra(model.getId());

        try (ZipOutputStream zipOut = new ZipOutputStream(out);
             ByteArrayInputStream bpmnInput = new ByteArrayInputStream(bpmn);
             ByteArrayInputStream pngInput = new ByteArrayInputStream(png)) {
            zipOut.putNextEntry(new ZipEntry(model.getKey() + BPMN));
            IOUtils.copyLarge(bpmnInput, zipOut);
            zipOut.putNextEntry(new ZipEntry(model.getKey() + PNG));
            IOUtils.copyLarge(pngInput, zipOut);
            zipOut.flush();
            out.flush();
        }

        return new JsonResult<>(StatusEnum.OK.getValue(), null, Boolean.TRUE);
    }

    /**
     * 导入模型的bpmn文件和图片
     */
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<Boolean> importData(String id, InputStream in) throws IOException {
        Assert.notNull(in, "输入流为空");
        Model model = checkAndGet(id);

        try (ZipInputStream zipIn = new ZipInputStream(in)) {
            ZipEntry zipEntry;
            byte[] bpmn = null;
            byte[] png = null;

            while (Objects.nonNull(zipEntry = zipIn.getNextEntry())) {
                if ((model.getKey() + BPMN).equals(zipEntry.getName())) {
                    bpmn = IoUtil.readInputStream(zipIn, zipEntry.getName());
                } else if ((model.getKey() + PNG).equals(zipEntry.getName())) {
                    png = IoUtil.readInputStream(zipIn, zipEntry.getName());
                } else {
                    log.info("未知文件：{}", zipEntry.getName());
                }
            }

            Assert.isTrue(ArrayUtils.isNotEmpty(bpmn), (model.getKey() + BPMN) + "文件为空");
            Assert.isTrue(ArrayUtils.isNotEmpty(png), (model.getKey() + PNG) + "文件为空");
            repositoryService.addModelEditorSource(id, bpmn);
            repositoryService.addModelEditorSourceExtra(id, png);
            in.close();
        }

        log.info("{} 导入 {}", UserContextHolder.get().getName(), model.getKey());
        return new JsonResult<>(StatusEnum.OK.getValue(), null, Boolean.TRUE);
    }

}
