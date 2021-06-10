package com.guowy.workflow.webapp.client;

import com.github.pagehelper.PageInfo;
import com.guowy.cloud.common.util.JsonResult;
import com.guowy.workflow.api.WorkFlowApi;
import com.guowy.workflow.dto.*;
import com.guowy.workflow.webapp.biz.ImageBiz;
import com.guowy.workflow.webapp.biz.InstanceBiz;
import com.guowy.workflow.webapp.biz.ReceiveTaskBiz;
import com.guowy.workflow.webapp.biz.UserTaskBiz;
import com.guowy.workflow.webapp.dto.RecordQueryDTO;
import com.guowy.workflow.webapp.vo.ImageUserTaskVO;
import com.guowy.workflow.webapp.vo.RecordVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.apache.http.entity.ContentType.IMAGE_SVG;

/**
 * @author LiJingTang
 * @date 2020-05-15 16:59
 */
@Api(value = "工作流对外接口", tags = "工作流对外接口")
@RestController
public class WorkFlowClient implements WorkFlowApi {

    @Autowired
    private InstanceBiz instanceBiz;
    @Autowired
    private UserTaskBiz userTaskBiz;
    @Autowired
    private ReceiveTaskBiz receiveTaskBiz;
    @Autowired
    private ImageBiz imageBiz;

    @ApiOperation("启动流程实例")
    @Override
    public JsonResult<String> startProcess(@Validated StartProcessDTO startDTO) {
        return instanceBiz.create(startDTO);
    }

    @ApiOperation("取消流程实例")
    @Override
    public JsonResult<Boolean> cancelInstance(@Validated CancelInstanceDTO cancelDTO) {
        return instanceBiz.cancel(cancelDTO);
    }

    @ApiOperation("办理用户任务")
    @Override
    public JsonResult<UserTaskResponseDTO> completeUserTask(@Validated UserTaskCompleteDTO completeDTO) {
        return userTaskBiz.complete(completeDTO);
    }

    @ApiOperation("查询待办用户任务")
    @Override
    public JsonResult<PageInfo<UserTaskDTO>> findTodoUserTask(UserTaskQueryDTO queryDTO) {
        return userTaskBiz.findTodoByPage(queryDTO);
    }

    @ApiOperation("办理接收任务")
    @Override
    public JsonResult<ReceiveTaskResponseDTO> completeReceiveTask(@Validated ReceiveTaskCompleteDTO completeDTO) {
        return receiveTaskBiz.complete(completeDTO);
    }


    @ApiOperation("查询审核记录")
    @GetMapping("/record")
    public JsonResult<List<RecordVO>> findRecords(@Validated RecordQueryDTO queryDTO) {
        return instanceBiz.findRecords(queryDTO);
    }

    @ApiOperation("生成图片")
    @GetMapping("/image/{processKey}/{bizId}")
    public void image(@PathVariable String processKey, @PathVariable String bizId, HttpServletResponse response) throws IOException {
        response.reset();
        response.setContentType(IMAGE_SVG.getMimeType());
        imageBiz.generate(processKey, bizId, response.getOutputStream());
    }

    @ApiOperation("图片坐标信息")
    @GetMapping("/imageInfo/{processKey}/{bizId}")
    public JsonResult<Map<String, ImageUserTaskVO>> imageInfo(@PathVariable String processKey, @PathVariable String bizId) {
        return imageBiz.getImageInfo(processKey, bizId);
    }

}
