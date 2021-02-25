package cn.tlh.admin.service.serviceImpl.rabbitmq;

import cn.tlh.admin.common.pojo.DlxMessage;
import cn.tlh.admin.common.util.constants.RabbitMqConstants;
import cn.tlh.admin.dao.BrokerMessageLogDao;
import cn.tlh.admin.service.rabbitmq.MqService;
import org.apache.dubbo.config.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * mq发消息
 *
 * @author TANG
 */
@Service(version = "${service.version}")
@Component
public class MqServiceImpl implements MqService {
    private static final Logger log = LoggerFactory.getLogger(MqServiceImpl.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Resource
    private BrokerMessageLogDao brokerMessageLogDao;


    @Override
    public void send(String messageId, String queueName, Object msg) {
        log.info("send msg，队列名：{}，消息： {}，CorrelationData：{}", queueName, msg, messageId);
        this.rabbitTemplate.convertAndSend(queueName, msg, new CorrelationData(messageId));
    }

    @Override
    public void sendDelayOrder(String messageId, String queueName, String msg, Integer times) {
        DlxMessage dlxMessage = new DlxMessage(messageId, queueName, msg, times);
        MessagePostProcessor processor = (message) -> {
//            message.getMessageProperties().setDelay(times);
            message.getMessageProperties().setExpiration(String.valueOf(times));
            return message;
        };

        // 当指定一个不存在的交换机，这样可以触发confirmCallback失败回调，进行重发尝试
        // 发送消息的时候将消息的deliveryMode设置为2，在Spring Boot中消息默认就是持久化的。
        rabbitTemplate.convertAndSend(
                RabbitMqConstants.ORDER_EXCHANGE,
                RabbitMqConstants.ORDER_DLK_KEY,
                dlxMessage,
                processor,
                new CorrelationData(messageId));
        log.info("订单延时队列发送：消息id: {} 队列名：{}，消息：{}，延时时间（毫秒）：{}", messageId, queueName, msg, times);
    }

}
