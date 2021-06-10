package com.guowy.workflow.webapp.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.pagehelper.PageInfo;
import com.guowy.cloud.common.properties.InstanceProperties;
import com.guowy.cloud.common.util.JsonResult;
import com.guowy.workflow.webapp.biz.ModelBiz;
import com.guowy.workflow.webapp.dto.ModelCreateDTO;
import com.guowy.workflow.webapp.dto.ModelQueryDTO;
import com.guowy.workflow.webapp.dto.ModelUpdateDTO;
import com.guowy.workflow.webapp.vo.ModelVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.activiti.engine.repository.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

/**
 * @author LiJingTang
 * @date 2020-05-13 16:59
 */
@Api(value = "流程模型接口", tags = "流程模型接口")
@RestController
@RequestMapping("/model")
public class ModelController {

    @Autowired
    private InstanceProperties instanceProperties;
    @Autowired
    private ModelBiz modelBiz;

    @ApiOperation("分页查询流程模型")
    @GetMapping
    public JsonResult<PageInfo<ModelVO>> findByPage(ModelQueryDTO queryDTO) {
        return modelBiz.findByPage(queryDTO);
    }

    @ApiOperation("创建流程模型")
    @PostMapping("/create")
    public JsonResult<String> create(@Validated @RequestBody ModelCreateDTO createDTO) {
        return modelBiz.create(createDTO);
    }

    @ApiOperation("流程模型XML")
    @GetMapping("/{id}")
    public JsonResult<ObjectNode> getXml(@PathVariable String id) throws IOException {
        return modelBiz.get(id);
    }

    @ApiOperation("更新流程模型XML")
    @PutMapping("/{id}")
    public JsonResult<Boolean> update(@PathVariable String id, @Validated ModelUpdateDTO updateDTO) throws IOException {
        updateDTO.setId(id);
        return modelBiz.update(updateDTO);
    }

    @ApiOperation("删除流程模型")
    @DeleteMapping("/{id}")
    public JsonResult<Boolean> delete(@PathVariable String id) {
        return modelBiz.delete(id);
    }

    @ApiOperation("部署流程模型")
    @PatchMapping("/{id}/deploy")
    public JsonResult<String> deploy(@PathVariable String id) throws IOException {
        return modelBiz.deploy(id);
    }

    @ApiOperation("更新流程模型分类")
    @PatchMapping("/{id}/category")
    public JsonResult<Boolean> updateCategory(@PathVariable String id, @RequestParam("category") String category) throws IOException {
        return modelBiz.updateCategory(id, category);
    }

    @ApiOperation("下载流程模型")
    @GetMapping("/{id}/download")
    public JsonResult<Boolean> download(@PathVariable String id, HttpServletResponse response) throws IOException {
        Model model = modelBiz.checkAndGet(id);
        String name = model.getKey() + "(" + instanceProperties.getEnv() + ").zip";
        response.reset();
        response.setContentType(APPLICATION_OCTET_STREAM.getMimeType());
        response.setHeader(CONTENT_DISPOSITION, "attachment; filename=" + name);
        try (OutputStream out = response.getOutputStream()) {
            return modelBiz.download(model, out);
        }
    }

    @ApiOperation("导入流程模型")
    @PostMapping("/{id}/import")
    public JsonResult<Boolean> importData(@PathVariable String id, @RequestParam MultipartFile file) throws IOException {
        return modelBiz.importData(id, file.getInputStream());
    }

}
