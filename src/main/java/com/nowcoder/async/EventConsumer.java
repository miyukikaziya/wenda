package com.nowcoder.async;

import com.alibaba.fastjson.JSON;
import com.nowcoder.util.JedisAdapter;
import com.nowcoder.util.RedisKeyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//分发事件给各个handler list
@Service
public class EventConsumer implements InitializingBean, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    JedisAdapter jedisAdapter;
    //获取EventType及其对应的EventHandler List
    private Map<EventType, List<EventHandler>> config = new HashMap<>();
    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        //找到所有的EventHandler
        Map<String, EventHandler> beans = applicationContext.getBeansOfType(EventHandler.class);
        if (beans != null) {
            for (Map.Entry<String, EventHandler> entry : beans.entrySet()) {
                List<EventType> eventTypes = entry.getValue().getSupportEventTypes();

                for (EventType type : eventTypes) {
                    if (!config.containsKey(type)) {
                        config.put(type, new ArrayList<EventHandler>());
                    }
                    config.get(type).add(entry.getValue());//和自己关联起来
                }
            }
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String key = RedisKeyUtil.getEventQueueKey();
                List<String> events = jedisAdapter.brpop(0, key);
                for (String message : events) {
                    if (message.equals(key)) {
                        continue;
                    }
                    /*
                    System.out.println("45Key-------------"+key);
                    User user = new User();
                    user.setName("xx");
                    user.setPassword("ppp");
                    user.setHeadUrl("a.png");
                    user.setSalt("salt");
                    user.setId(1);
                    System.out.println("46-------------" + JSONObject.toJSONString(user));//user序列化
                    EventModel event = new EventModel();
                    event.setActorId(1);
                    event.setEntityId(2);
                    event.setEntityOwnerId(3);
                    event.setEntityType(6);
                    event.setType(COMMENT);
                    System.out.println("47-------------" + JSONObject.toJSONString(event));//user序列化
                    */
                    //System.out.println("48message-------------"+message);
                    EventModel eventModel = JSON.parseObject(message, EventModel.class);//反序列化
                    if (!config.containsKey(eventModel.getType())) {
                        logger.error("不能识别的事件");
                        continue;
                    }
                    for (EventHandler handler : config.get(eventModel.getType())) {
                        handler.doHandler(eventModel);
                    }

                }
            }
        });
        thread.start();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
