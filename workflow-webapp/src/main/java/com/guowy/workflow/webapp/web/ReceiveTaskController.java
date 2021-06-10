package com.guowy.workflow.webapp.web;

import com.guowy.cloud.common.util.JsonResult;
import com.guowy.workflow.dto.ReceiveTaskCompleteDTO;
import com.guowy.workflow.dto.ReceiveTaskResponseDTO;
import com.guowy.workflow.webapp.biz.ReceiveTaskBiz;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author LiJingTang
 * @date 2020-05-25 09:44
 */
@Api(value = "接收任务接口", tags = "接收任务接口")
@RestController
@RequestMapping("/receivetask")
public class ReceiveTaskController {

    @Autowired
    private ReceiveTaskBiz receiveTaskBiz;

    @ApiOperation("后台办理")
    @PatchMapping("/complete")
    public JsonResult<ReceiveTaskResponseDTO> complete(@Validated @RequestBody ReceiveTaskCompleteDTO completeDTO) {
        return receiveTaskBiz.complete(completeDTO);
    }

}
