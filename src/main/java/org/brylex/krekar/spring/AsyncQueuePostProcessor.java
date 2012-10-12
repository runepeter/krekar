package org.brylex.krekar.spring;

import org.brylex.krekar.Broker;
import org.brylex.krekar.AsyncQueue;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class AsyncQueuePostProcessor implements BeanPostProcessor {

    private final Broker broker;

    @Autowired
    public AsyncQueuePostProcessor(final Broker broker) {
        this.broker = broker;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        if (AsyncQueue.class.isAssignableFrom(bean.getClass())) {
            return broker.registerQueue((AsyncQueue) bean);
        }

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
