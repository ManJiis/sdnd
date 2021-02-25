package cn.tlh.admin.service.serviceImpl.jobhandler;

import cn.tlh.admin.common.pojo.BrokerMessageLog;
import cn.tlh.admin.common.pojo.Order;
import cn.tlh.admin.common.util.constants.RabbitMqConstants;
import cn.tlh.admin.common.util.json.JackJsonUtils;
import cn.tlh.admin.dao.BrokerMessageLogDao;
import cn.tlh.admin.service.rabbitmq.MqService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.tlh.admin.common.util.constants.RabbitMqConstants.ORDER_DELAY_QUEUE;

/**
 * XxlJob开发示例（Bean模式）
 * <p>
 * 开发步骤：
 * 1、任务开发：在Spring Bean实例中，开发Job方法；
 * 2、注解配置：为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 * 3、执行日志：需要通过 "XxlJobHelper.log" 打印执行日志；
 * 4、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "XxlJobHelper.handleFail/handleSuccess" 自主设置任务结果；
 *
 * @author xuxueli 2019-12-11 21:52:51
 */
@Component
public class SampleXxlJob {
    private static final Logger log = LoggerFactory.getLogger(SampleXxlJob.class);

    @Resource
    MqService mqService;
    @Resource
    private BrokerMessageLogDao brokerMessageLogDao;

    /**
     * 1、简单任务示例（Bean模式）
     */
    @XxlJob("demoJob")
    public void demoJobHandler() throws Exception {
        XxlJobHelper.log("XXL-JOB, Hello World.");
        log.info("XXL-JOB, Hello World.");
        XxlJobHelper.log("demoJob2 running -----> 当前时间: {}", LocalDateTime.now());
        log.info("demoJob2 running -----> 当前时间: {}", LocalDateTime.now());
        XxlJobHelper.handleSuccess();
        // default success
    }

    @XxlJob("timeOutOrderReSendJob")
    public void reSend() {
        // pull status = 0 and timeout message
        List<BrokerMessageLog> list = brokerMessageLogDao.query4StatusAndTimeoutMessage();
        list.forEach(brokerMessageLog -> {
            if (brokerMessageLog.getTryCount() >= 3) {
                brokerMessageLogDao.changeBrokerMessageLogStatus(brokerMessageLog.getMessageId(), RabbitMqConstants.MSG_SEND_FAILURE, LocalDateTime.now());
            } else {
                // resend
                brokerMessageLogDao.update4ReSend(brokerMessageLog.getMessageId(), LocalDateTime.now());
                Order reSendOrder = JackJsonUtils.toObject(brokerMessageLog.getMessage(), Order.class);
                try {
                    mqService.sendDelayOrder(Objects.requireNonNull(reSendOrder).getId(), ORDER_DELAY_QUEUE, JackJsonUtils.toJsonString(reSendOrder), 1000 * 60);
                } catch (Exception e) {
                    System.err.println("-----------异常处理-----------");
                }
            }
        });
    }

}
