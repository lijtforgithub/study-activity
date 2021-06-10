package com.guowy.workflow.client;

import com.guowy.workflow.api.WorkFlowApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author LiJingTang
 * @date 2020-06-09 09:38
 */
@FeignClient(value = "guowy-workflow-webapp", fallbackFactory = WorkflowFeignClientFallbackFactory.class)
public interface WorkflowFeignClient extends WorkFlowApi {
}
