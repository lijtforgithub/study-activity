package com.guowy.workflow.client;

import com.github.pagehelper.PageInfo;
import com.guowy.cloud.common.enums.StatusEnum;
import com.guowy.cloud.common.util.JsonResult;
import com.guowy.workflow.dto.*;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author LiJingTang
 * @date 2020-06-09 09:38
 */
@Slf4j
@Component
public class WorkflowFeignClientFallbackFactory implements FallbackFactory<WorkflowFeignClient> {

    private static final String MSG = "调用接口失败";

    @Override
    public WorkflowFeignClient create(Throwable e) {

        return new WorkflowFeignClient() {
            @Override
            public JsonResult<String> startProcess(StartProcessDTO startDTO) {
                return result(null, "startProcess", e);
            }

            @Override
            public JsonResult<Boolean> cancelInstance(CancelInstanceDTO cancelDTO) {
                return result(null, "cancelInstance", e);
            }

            @Override
            public JsonResult<UserTaskResponseDTO> completeUserTask(UserTaskCompleteDTO completeDTO) {
                return result(null, "completeUserTask", e);
            }

            @Override
            public JsonResult<PageInfo<UserTaskDTO>> findTodoUserTask(UserTaskQueryDTO queryDTO) {
                return result(null, "findTodoUserTask", e);
            }

            @Override
            public JsonResult<ReceiveTaskResponseDTO> completeReceiveTask(ReceiveTaskCompleteDTO completeDTO) {
                return result(null, "completeReceiveTask", e);
            }
        };
    }

    private static <T> JsonResult<T> result(T t, String method, Throwable e) {
        log.error(MSG + method, e);
        return new JsonResult<>(StatusEnum.FAIL.getValue(), MSG, t);
    }

}
