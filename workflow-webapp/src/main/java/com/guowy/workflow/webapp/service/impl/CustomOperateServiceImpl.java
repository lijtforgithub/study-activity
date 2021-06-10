package com.guowy.workflow.webapp.service.impl;

import com.guowy.workflow.webapp.mapper.HistoryActinstMapper;
import com.guowy.workflow.webapp.mapper.HistoryTaskMapper;
import com.guowy.workflow.webapp.mapper.ModelMapper;
import com.guowy.workflow.webapp.service.CustomOperateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author LiJingTang
 * @date 2020-05-14 11:09
 */
@Service
public class CustomOperateServiceImpl implements CustomOperateService {

    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private HistoryTaskMapper historyTaskMapper;
    @Autowired
    private HistoryActinstMapper historyActinstMapper;

    @Override
    public List<String> findAllModelKey() {
        return modelMapper.selectAllKey();
    }

    @Override
    public void terminateInstance(String instanceId, String reason) {
        // activiti 升级后流程线不满足报错 不结束流程 手动删除流程实例 删除多余创建的表
        historyTaskMapper.delete(instanceId, reason);
        historyActinstMapper.delete(instanceId, reason);
    }

}
