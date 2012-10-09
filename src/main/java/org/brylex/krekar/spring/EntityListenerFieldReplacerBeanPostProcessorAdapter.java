package org.brylex.krekar.spring;

import java.lang.reflect.Field;

import org.brylex.krekar.Broker;
import org.brylex.krekar.EntityListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.stereotype.Component;

@Component
public class EntityListenerFieldReplacerBeanPostProcessorAdapter extends InstantiationAwareBeanPostProcessorAdapter {

    private final Broker broker;

    @Autowired
    public EntityListenerFieldReplacerBeanPostProcessorAdapter(final Broker broker) {
        this.broker = broker;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {

        for (Field f : bean.getClass().getDeclaredFields()) {
            if (EntityListener.class.isAssignableFrom(f.getType())) {
                EntityListener entityListener = broker.getEntityListener((Class<EntityListener>) f.getType());

                try {
                    f.setAccessible(true);
                    f.set(bean, entityListener);
                    return true;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Unable to replace field [" + f + "]'s value with asynchronous proxy.");
                }
            }
        }

        return super.postProcessAfterInstantiation(bean, beanName);
    }
}
