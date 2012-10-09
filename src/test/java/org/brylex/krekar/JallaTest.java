package org.brylex.krekar;


import akka.actor.*;
import akka.japi.Creator;
import akka.routing.RoundRobinRouter;
import akka.util.Duration;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class JallaTest {

    @Test
    public void testName() throws Exception {

        final ApplicationContext applicationContext = new AnnotationConfigApplicationContext(TestConfiguration.class);

        Map<String, BeanPostProcessor> map = applicationContext.getBeansOfType(BeanPostProcessor.class);
        for (String s : map.keySet()) {
            System.err.println(s + " = [" + map.get(s) + "]");
        }

        final DoStuffService service = applicationContext.getBean(DoStuffService.class);
        service.doIt();

        /*final EntityListener listener1 = new EntityListener() {
            @Override
            public void onEntity(Object entity) {
                System.err.println("[" + entity + "]");
            }
        };

        final EntityListener listener2 = new EntityListener() {
            @Override
            public void onEntity(Object entity) {
                listener1.onEntity("<wrapped>" + entity + "</wrapped>");
            }
        };

        Broker broker = new Broker();
        EntityListener entityListener1 = broker.registerQueue(listener1);
        EntityListener entityListener2 = broker.registerQueue(listener2);*/

        /*for (int i = 0; i < 500; i++) {
            entityListener2.onEntity("RUNE-" + i);
        }*/


        Thread.sleep(10000);
    }

    @Component
    public static class Broker {

        private final ActorSystem system;

        public Broker() {

            this.system = ActorSystem.create("KrekarSystem", ConfigFactory.load());

            system.scheduler().schedule(Duration.create(2L, TimeUnit.SECONDS), Duration.create(2L, TimeUnit.SECONDS), new Runnable() {
                @Override
                public void run() {

                    System.err.println("RUNE-" + System.currentTimeMillis());

                }
            });
        }

        public <T> T registerQueue(final T entityListener, final Class<T> queueType) {

            int numWorkersForQueue = 10;

            final List<ActorRef> actorRefs = Lists.newArrayListWithCapacity(numWorkersForQueue);

            for (int i = 0; i < numWorkersForQueue; i++) {
                T listener = TypedActor.get(system).typedActorOf(new TypedProps<T>(queueType, new Creator<T>() {
                    @Override
                    public T create() throws Exception {
                        return entityListener;
                    }
                }).withDispatcher("krekar-dispatcher"));
                actorRefs.add(TypedActor.get(system).getActorRefFor(listener));
            }

            ActorRef actorRef = system.actorOf(new Props().withDispatcher("krekar-dispatcher").withRouter(RoundRobinRouter.create(actorRefs)));
            return TypedActor.get(system).typedActorOf(new TypedProps<T>((Class<T>) queueType), actorRef);
        }
    }

    public static interface EntityListener<E> {

        void onEntity(E entity);

    }

    @Component
    public static class AsynchronousBean implements EntityListener<String> {
        @Override
        public void onEntity(String entity) {
            System.err.println("[" + entity + "]");
        }
    }

    @Service
    public static class DoStuffService {

        private final AsynchronousBean asynchronousBean;

        @Autowired
        public DoStuffService(AsynchronousBean entityListener) {
            this.asynchronousBean = entityListener;
        }

        public void doIt() {
            asynchronousBean.onEntity("JALLA :: " + System.currentTimeMillis());
        }
    }

    public static class AsynchronousBeanPostProcessorAdapter extends InstantiationAwareBeanPostProcessorAdapter {

        @Override
        public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {

            if (EntityListener.class.isAssignableFrom(bean.getClass())) {

            }

            return super.postProcessAfterInstantiation(bean, beanName);
        }
    }

    public static class AsynchronousBeanPostProcessor implements BeanPostProcessor {

        private final Broker broker;

        @Autowired
        public AsynchronousBeanPostProcessor(final Broker broker) {
            this.broker = broker;
        }

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

            if (EntityListener.class.isAssignableFrom(bean.getClass())) {

                System.out.printf("[" + beanName + "]::{" + bean + "}");

                return  broker.registerQueue(bean, (Class<Object>) bean.getClass());
            }

            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            return bean;
        }
    }

    @Configuration
    @ComponentScan({"org.brylex.krekar"})
    public static class TestConfiguration {

        @Bean
        public AsynchronousBeanPostProcessor asynchronousBeanPostProcessor(Broker broker) {
            return new AsynchronousBeanPostProcessor(broker);
        }

    }

}
