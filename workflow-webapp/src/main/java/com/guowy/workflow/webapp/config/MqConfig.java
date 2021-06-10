package com.guowy.workflow.webapp.config;

import com.guowy.cloud.common.properties.InstanceProperties;
import com.guowy.workflow.webapp.mq.recv.StartProcessRecv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.guowy.workflow.dto.StartProcessDTO.START_QUEUE_RULE;

/**
 * @author LiJingTang
 * @date 2020-05-15 13:53
 */
@Slf4j
@Configuration
public class MqConfig {

    /**
     * 启动流程消息队列
     */
    @Bean
    public Queue startQueue(InstanceProperties instanceProperties) {
        String env = instanceProperties.getEnv();
        String startQueue = START_QUEUE_RULE.srcEnv(env).destEnv(env).build();
        Queue queue = new Queue(startQueue);
        log.info("创建接收启动流程消息队列：{}", queue.getName());
        return queue;
    }

    /**
     * 启动流程消息监听器容器
     */
    @Bean
    public SimpleMessageListenerContainer startContainer(CachingConnectionFactory connectionFactory, Queue startQueue,
                                                         StartProcessRecv startProcessRecv) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(startQueue);
        container.setMessageListener(startProcessRecv);
        container.setConcurrentConsumers(1);
        return container;
    }

}
