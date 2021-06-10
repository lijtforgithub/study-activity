package com.guowy.workflow.api;

import com.github.pagehelper.PageInfo;
import com.guowy.cloud.common.util.JsonResult;
import com.guowy.workflow.dto.*;
import org.springframework.web.bind.annotation.*;

/**
 * @author LiJingTang
 * @date 2020-05-15 16:56
 */
@RequestMapping("/wf/client")
public interface WorkFlowApi {

    /**
     * 启动流程实例
     *
     * @param startDTO 启动入参
     * @return 实例ID
     */
    @PostMapping("/instance/create")
    JsonResult<String> startProcess(@RequestBody StartProcessDTO startDTO);

    /**
     * 取消正在执行的流程实例
     *
     * @param cancelDTO 取消入参
     * @return 是否成功
     */
    @PutMapping("/instance/cancel")
    JsonResult<Boolean> cancelInstance(@RequestBody CancelInstanceDTO cancelDTO);


    /**
     * 办理用户任务
     *
     * @param completeDTO 办理入参
     * @return 流程实例是否已结束
     */
    @PutMapping("/userTask/complete")
    JsonResult<UserTaskResponseDTO> completeUserTask(@RequestBody UserTaskCompleteDTO completeDTO);

    /**
     * 查询待办用户任务
     *
     * @param queryDTO 查询条件
     * @return 待办任务
     */
    @PostMapping("/userTask/todo")
    JsonResult<PageInfo<UserTaskDTO>> findTodoUserTask(@RequestBody UserTaskQueryDTO queryDTO);


    /**
     * 办理接收任务
     *
     * @param completeDTO 办理入参
     * @return 流程实例是否已结束
     */
    @PutMapping("/receiveTask/complete")
    JsonResult<ReceiveTaskResponseDTO> completeReceiveTask(@RequestBody ReceiveTaskCompleteDTO completeDTO);

}
