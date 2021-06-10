package com.guowy.workflow.webapp.service.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.guowy.cloud.common.util.JsonResult;
import com.guowy.cloud.security.enums.UserTypeEnum;
import com.guowy.cloud.security.model.UserDetail;
import com.guowy.security.client.UserDetailClient;
import com.guowy.workflow.webapp.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.guowy.cloud.common.util.DateUtils.HOUR_TIME;
import static com.guowy.workflow.webapp.util.WorkFlowUtils.joinTaskUser;

/**
 * @author LiJingTang
 * @date 2020-05-15 14:20
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final Cache<String, UserDetail> USER_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(HOUR_TIME * 2, TimeUnit.MILLISECONDS)
            .maximumSize(200).build();

    @Autowired
    private UserDetailClient userDetailClient;

    @Override
    public String getName(int userType, long userId) {
        if (UserTypeEnum.UNKNOWN.getValue() != userType) {
            UserDetail user = getUser(userType, userId);
            if (Objects.nonNull(user)) {
                return user.getName();
            }
        }

        return StringUtils.EMPTY;
    }

    private UserDetail getUser(int userType, long userId) {
        try {
            String key = joinTaskUser(userType, userId);
            return USER_CACHE.get(key, () -> {
                JsonResult<UserDetail> result = UserTypeEnum.EMP.getValue() == userType ?
                        userDetailClient.getUserDetailByEmp(userId) : userDetailClient.getUserDetailByAccount(userId);
                log.debug(JSON.toJSONString(result));
                return ObjectUtils.defaultIfNull(result.getData(), null);
            });
        } catch (CacheLoader.InvalidCacheLoadException | ExecutionException e) {
            log.warn("查询用户：{}", e.getMessage());
        } catch (Exception e) {
            log.error("查询用户失败：", e);
        }

        return null;
    }

}
