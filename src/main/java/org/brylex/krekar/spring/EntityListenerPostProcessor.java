package org.brylex.krekar.spring;

import org.brylex.krekar.Broker;
import org.brylex.krekar.EntityListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class EntityListenerPostProcessor implements BeanPostProcessor {

    private final Broker broker;

    @Autowired
    public EntityListenerPostProcessor(final Broker broker) {
        this.broker = broker;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        if (EntityListener.class.isAssignableFrom(bean.getClass())) {
            return broker.registerQueue((EntityListener) bean);
        }

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
