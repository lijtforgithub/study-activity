package com.guowy.workflow.webapp.web;

import com.github.pagehelper.PageInfo;
import com.guowy.cloud.common.util.JsonResult;
import com.guowy.workflow.webapp.biz.ProcessBiz;
import com.guowy.workflow.webapp.dto.ProcessQueryDTO;
import com.guowy.workflow.webapp.vo.ProcessVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.http.entity.ContentType.*;

/**
 * @author LiJingTang
 * @date 2020-05-15 09:34
 */
@Api(value = "流程定义接口", tags = "流程定义接口")
@RestController
@RequestMapping("/process")
public class ProcessController {

    @Autowired
    private ProcessBiz processBiz;

    @ApiOperation("分页查询流程定义")
    @GetMapping
    public JsonResult<PageInfo<ProcessVO>> findByPage(ProcessQueryDTO queryDTO) {
        return processBiz.findByPage(queryDTO);
    }

    @ApiOperation("根据ID查询流程定义")
    @GetMapping("/{id}")
    public JsonResult<ProcessVO> get(@PathVariable String id) {
        return processBiz.get(id);
    }

    @ApiOperation("根据部署ID查询流程定义")
    @GetMapping("/deploy/{deployId}")
    public JsonResult<ProcessVO> getByDeployId(@PathVariable String deployId) {
        return processBiz.getByDeployId(deployId);
    }

    @ApiOperation("根据ID查询流程定义XML")
    @GetMapping("/xml/{id}")
    public void getXml(@PathVariable String id, HttpServletResponse response) throws IOException {
        response.reset();
        response.setContentType(TEXT_XML.getMimeType());
        processBiz.getXml(id, response.getOutputStream());
    }

    @ApiOperation("根据ID查询流程定义图片")
    @GetMapping("/image/{id}")
    public void getImage(@PathVariable String id, HttpServletResponse response) throws IOException {
        response.reset();
        response.setContentType(IMAGE_PNG.getMimeType());
        processBiz.getImage(id, response.getOutputStream());
    }

    @ApiOperation("删除流程定义")
    @DeleteMapping("/{id}")
    public JsonResult<Boolean> delete(@PathVariable String id) {
        return processBiz.delete(id);
    }

    @ApiOperation("启用流程定义")
    @PatchMapping("/{id}/enable")
    public JsonResult<Boolean> enable(@PathVariable String id) {
        return processBiz.enable(id);
    }

    @ApiOperation("禁用流程定义")
    @PatchMapping("/{id}/disable")
    public JsonResult<Boolean> disable(@PathVariable String id) {
        return processBiz.disable(id);
    }

}
