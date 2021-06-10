package com.guowy.workflow.webapp.mapper;

import java.util.List;

/**
 * @author LiJingTang
 * @date 2020-05-14 11:05
 */
public interface ModelMapper {

    /**
     * 查询流程模型所有key
     * @return key
     */
    List<String> selectAllKey();

}
