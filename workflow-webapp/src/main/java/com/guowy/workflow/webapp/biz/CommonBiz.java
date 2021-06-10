package com.guowy.workflow.webapp.biz;

import com.guowy.cloud.common.enums.StatusEnum;
import com.guowy.cloud.common.util.JsonResult;
import com.guowy.workflow.webapp.service.CustomOperateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author LiJingTang
 * @date 2020-05-14 10:11
 */
@Service
public class CommonBiz {

    @Autowired
    private DiscoveryClient discoveryClient;
    @Autowired
    private CustomOperateService customOperateService;

    /**
     * 查询注册中心所有应用
     */
    public JsonResult<List<String>> findAllApp() {
        List<String> services = discoveryClient.getServices();
        if (!CollectionUtils.isEmpty(services)) {
            services.sort(String::compareTo);
        }

        return new JsonResult<>(StatusEnum.OK.getValue(), null, services);
    }

    /**
     * 查询模型所有key
     */
    public JsonResult<List<String>> findAllKey() {
        List<String> keys = customOperateService.findAllModelKey();
        return new JsonResult<>(StatusEnum.OK.getValue(), null, keys);
    }

}
