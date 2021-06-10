package com.guowy.workflow.webapp.web;

import com.github.pagehelper.PageInfo;
import com.guowy.cloud.common.util.JsonResult;
import com.guowy.workflow.dto.UserTaskCompleteDTO;
import com.guowy.workflow.dto.UserTaskDTO;
import com.guowy.workflow.dto.UserTaskQueryDTO;
import com.guowy.workflow.webapp.biz.UserTaskBiz;
import com.guowy.workflow.webapp.dto.HistoryUserTaskQueryDTO;
import com.guowy.workflow.webapp.dto.UserTaskSignDTO;
import com.guowy.workflow.webapp.vo.HistoryUserTaskVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author LiJingTang
 * @date 2020-05-25 09:16
 */
@Api(value = "用户任务接口", tags = "用户任务接口")
@RestController
@RequestMapping("/usertask")
public class UserTaskController {

    @Autowired
    private UserTaskBiz userTaskBiz;


    @ApiOperation("任务加签")
    @PostMapping("/signAdd")
    public JsonResult<Boolean> signAdd(@Validated @RequestBody UserTaskSignDTO signDTO) {
        return userTaskBiz.signAdd(signDTO);
    }

    @ApiOperation("指定办理人")
    @PatchMapping("/assign")
    public JsonResult<Boolean> assign(@Validated @RequestBody UserTaskSignDTO signDTO) {
        return userTaskBiz.assign(signDTO);
    }

    @ApiOperation("后台办理")
    @PatchMapping("/complete")
    public JsonResult<Boolean> complete(@Validated @RequestBody UserTaskCompleteDTO completeDTO) {
        return userTaskBiz.systemComplete(completeDTO);
    }

    @ApiOperation("分页查询待办任务")
    @GetMapping()
    public JsonResult<PageInfo<UserTaskDTO>> findTodoByPage(UserTaskQueryDTO queryDTO) {
        return userTaskBiz.findTodoByPage(queryDTO);
    }

    @ApiOperation("分页查询已办理任务")
    @GetMapping("/completed")
    public JsonResult<PageInfo<HistoryUserTaskVO>> findHistoryByPage(HistoryUserTaskQueryDTO queryDTO) {
        return userTaskBiz.findHistoryByPage(queryDTO);
    }

}
