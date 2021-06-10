package com.guowy.workflow.webapp.mq.send;

import com.alibaba.fastjson.JSONObject;
import com.guowy.cloud.common.properties.InstanceProperties;
import com.guowy.workflow.webapp.enums.ResponseTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author LiJingTang
 * @date 2020-05-15 13:54
 */
@Slf4j
@Component
public class ResponseSend {

    private static final String PREFIX = "gwy.workflow.{env}.resp.";
    private static final String PATTERN = "[^a-zA-Z0-9]";

    @Autowired
    private InstanceProperties instanceProperties;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(String processKey, ResponseTypeEnum typeEnum, String bizId) {
        if (StringUtils.isAnyBlank(processKey, bizId) || Objects.isNull(typeEnum)) {
            log.warn("入参为空：processKey={} bizId={} typeEnum={}", processKey, bizId, typeEnum);
            return;
        }

        String queue = PREFIX.replace("{env}", instanceProperties.getEnv()) + processKey.replaceAll(PATTERN, ".");
        JSONObject object = new JSONObject();
        object.put("type", typeEnum.getValue());
        object.put("desc", typeEnum.getDesc());
        object.put("bizId", bizId);
        String msg = object.toJSONString();

        try {
            rabbitTemplate.convertAndSend(DirectExchange.DEFAULT.getName(), queue, msg);
        } catch (AmqpException e) {
            log.error(String.format("发送消息失败：%s | %s ", queue, msg), e);
        }
    }

}
