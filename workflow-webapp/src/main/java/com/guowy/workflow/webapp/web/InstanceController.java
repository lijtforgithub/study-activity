package com.guowy.workflow.webapp.web;

import com.github.pagehelper.PageInfo;
import com.guowy.cloud.common.util.JsonResult;
import com.guowy.workflow.webapp.biz.ImageBiz;
import com.guowy.workflow.webapp.biz.InstanceBiz;
import com.guowy.workflow.webapp.dto.InstanceQueryDTO;
import com.guowy.workflow.webapp.vo.ImageUserTaskVO;
import com.guowy.workflow.webapp.vo.InstanceVO;
import com.guowy.workflow.webapp.vo.RecordVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.apache.http.entity.ContentType.IMAGE_SVG;

/**
 * @author LiJingTang
 * @date 2020-05-15 16:35
 */
@Api(value = "流程实例接口", tags = "流程实例接口")
@RestController
@RequestMapping("/instance")
public class InstanceController {

    @Autowired
    private InstanceBiz instanceBiz;
    @Autowired
    private ImageBiz imageBiz;

    @ApiOperation("分页查询流程实例")
    @GetMapping
    public JsonResult<PageInfo<InstanceVO>> findByPage(@Validated InstanceQueryDTO queryDTO) {
        return instanceBiz.findByPage(queryDTO);
    }

    @ApiOperation("根据ID查询流程实例")
    @GetMapping("/{id}")
    public JsonResult<InstanceVO> get(@PathVariable String id) {
        return instanceBiz.get(id);
    }

    @ApiOperation("删除流程实例")
    @DeleteMapping("/{id}")
    public JsonResult<Boolean> delete(@PathVariable String id) {
        return instanceBiz.delete(id);
    }

    @ApiOperation("激活流程实例")
    @PatchMapping("/{id}/activate")
    public JsonResult<Boolean> activate(@PathVariable String id) {
        return instanceBiz.activate(id);
    }

    @ApiOperation("挂起流程实例")
    @PatchMapping("/{id}/suspend")
    public JsonResult<Boolean> suspend(@PathVariable String id) {
        return instanceBiz.suspend(id);
    }

    @ApiOperation("取消流程实例")
    @PatchMapping("/{id}/cancel")
    public JsonResult<Boolean> cancel(@PathVariable String id, @RequestParam("reason") String reason) {
        return instanceBiz.cancel(id, reason);
    }

    @ApiOperation("查询审核记录")
    @GetMapping("/record/{id}")
    public JsonResult<List<RecordVO>> findRecords(@PathVariable String id) {
        return instanceBiz.findRecords(id);
    }

    @ApiOperation("生成图片")
    @GetMapping("/image/{id}")
    public void image(@PathVariable String id, HttpServletResponse response) throws IOException {
        response.reset();
        response.setContentType(IMAGE_SVG.getMimeType());
        imageBiz.generate(id, response.getOutputStream());
    }

    @ApiOperation("图片坐标信息")
    @GetMapping("/imageInfo/{id}")
    public JsonResult<Map<String, ImageUserTaskVO>> imageInfo(@PathVariable String id) {
        return imageBiz.getImageInfo(id);
    }

}
