package com.guowy.workflow.webapp.service;

/**
 * @author LiJingTang
 * @date 2020-05-15 14:16
 */
public interface UserService {

    /**
     * 根据用户类型和用户ID查询用户名称
     *
     * @param userType 用户类型
     * @param userId 用户ID
     * @return 用户名称
     */
    String getName(int userType, long userId);

}
