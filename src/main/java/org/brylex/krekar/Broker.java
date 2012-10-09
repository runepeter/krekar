package org.brylex.krekar;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.typesafe.config.ConfigFactory;

import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.TypedActor;
import akka.actor.TypedProps;
import akka.japi.Creator;
import akka.routing.RoundRobinRouter;
import akka.util.Duration;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

@Component
public class Broker {

    private final Map<Class<? extends EntityListener>, EntityListener> map = Maps.newHashMap();
    private final ActorSystem system;

    public Broker() {

        this.system = ActorSystem.create("KrekarSystem", ConfigFactory.load());

        /*system.scheduler().schedule(Duration.create(2L, TimeUnit.SECONDS), Duration.create(2L, TimeUnit.SECONDS), new Runnable() {
            @Override
            public void run() {

                System.err.println("RUNE-" + System.currentTimeMillis());

            }
        });*/
    }

    public <T extends EntityListener> T getEntityListener(Class<T> listenerClass) {
        return (T) map.get(listenerClass);
    }

    public <T extends EntityListener> T registerQueue(final T entityListener) {

        int numWorkersForQueue = 10;

        final List<ActorRef> actorRefs = Lists.newArrayListWithCapacity(numWorkersForQueue);

        for (int i = 0; i < numWorkersForQueue; i++) {
            EntityListener listener = TypedActor.get(system).typedActorOf(new TypedProps<EntityListener>(EntityListener.class, new Creator<EntityListener>() {
                @Override
                public EntityListener create() throws Exception {
                    return entityListener;
                }
            }).withDispatcher("krekar-dispatcher"));
            actorRefs.add(TypedActor.get(system).getActorRefFor(listener));
        }

        ActorRef actorRef = system.actorOf(new Props().withDispatcher("krekar-dispatcher").withRouter(RoundRobinRouter.create(actorRefs)));
        final EntityListener delegate = TypedActor.get(system).typedActorOf(new TypedProps<EntityListener>(EntityListener.class), actorRef);

        EntityListener proxy = (EntityListener) Enhancer.create(entityListener.getClass(), new MethodInterceptor() {
            @Override
            public Object intercept(final Object o, final Method method, final Object[] objects, final MethodProxy methodProxy) throws Throwable {

                if ("onEntity".equals(method.getName())) {
                    delegate.onEntity(objects[0]);
                } else {
                    return method.invoke(entityListener, objects);
                }

                return null;
            }
        });

        map.put(entityListener.getClass(), proxy);

        return (T) proxy;
    }
}
