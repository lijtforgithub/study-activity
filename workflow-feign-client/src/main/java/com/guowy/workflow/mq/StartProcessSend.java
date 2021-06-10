package com.guowy.workflow.mq;

import com.alibaba.fastjson.JSON;
import com.guowy.cloud.common.properties.InstanceProperties;
import com.guowy.workflow.dto.StartProcessDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.guowy.workflow.dto.StartProcessDTO.START_QUEUE_RULE;

/**
 * @author LiJingTang
 * @date 2020-06-03 09:12
 */
@Slf4j
public class StartProcessSend {

    @Autowired
    private InstanceProperties instanceProperties;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired(required = false)
    private Validator validator;

    /**
     * 发送启动流程消息
     *
     * @param startDTO 启动入参
     * @return 是否发送成功
     */
    public boolean send(StartProcessDTO startDTO) {
        Assert.notNull(startDTO, "启动参数为空");

        String env = instanceProperties.getEnv();
        String queue = START_QUEUE_RULE.srcEnv(env).destEnv(env).build();
        String msg = JSON.toJSONString(startDTO);

        if (Objects.nonNull(validator)) {
            Set<ConstraintViolation<StartProcessDTO>> validate = validator.validate(startDTO);
            Assert.isTrue(CollectionUtils.isEmpty(validate),
                    validate.stream().map(ConstraintViolation::getMessageTemplate).collect(Collectors.joining(";")));
        }

        try {
            rabbitTemplate.convertAndSend(DirectExchange.DEFAULT.getName(), queue, msg);
            log.warn("发送启动流程消息：{} = {}", queue, msg);
            return true;
        } catch (AmqpException e) {
            log.warn("发送启动流程消息异常：{} = {}", queue, msg);
            return false;
        }
    }

}
