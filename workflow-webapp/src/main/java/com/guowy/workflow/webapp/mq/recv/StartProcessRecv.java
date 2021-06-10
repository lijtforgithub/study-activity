package com.guowy.workflow.webapp.mq.recv;

import com.alibaba.fastjson.JSON;
import com.guowy.cloud.common.enums.StatusEnum;
import com.guowy.cloud.common.util.JsonResult;
import com.guowy.workflow.dto.StartProcessDTO;
import com.guowy.workflow.webapp.biz.InstanceBiz;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author LiJingTang
 * @date 2020-05-25 16:42
 */
@Slf4j
@Component
public class StartProcessRecv implements MessageListener {

    @Autowired
    private InstanceBiz instanceBiz;

    @Override
    public void onMessage(Message message) {
        try {
            String msg = new String(message.getBody());
            log.info("启动流程实例消息：{}", msg);

            StartProcessDTO startDTO = JSON.parseObject(msg, StartProcessDTO.class);
            JsonResult<String> result = instanceBiz.create(startDTO);
            if (result.getCode() != StatusEnum.OK.getValue()) {
                log.error("启动流程实例异常：{}", JSON.toJSONString(result));
            }
        } catch (Exception e) {
            log.error("启动流程实例失败", e);
        }
    }

}
