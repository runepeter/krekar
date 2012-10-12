package org.brylex.krekar;

import akka.actor.*;
import akka.japi.Creator;
import akka.routing.RoundRobinRouter;
import com.google.common.collect.Maps;
import com.typesafe.config.ConfigFactory;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.Map;

public class Broker {

    private final Map<Class<? extends AsyncQueue>, AsyncQueue> map = Maps.newHashMap();
    private final ActorSystem system;

    public Broker() {
        this.system = ActorSystem.create("KrekarSystem", ConfigFactory.load());
    }

    public <T extends AsyncQueue> T getEntityListener(Class<T> listenerClass) {
        return (T) map.get(listenerClass);
    }

    public <T extends AsyncQueue> T registerQueue(final T entityListener) {

        int numWorkersForQueue = 10;

        final ActorRef ref = system.actorOf(
                new Props()
                        .withRouter(new RoundRobinRouter(numWorkersForQueue))
                        .withDispatcher("krekar-dispatcher")
                        .withCreator(new Creator<Actor>() {
                            @Override
                            public Actor create() throws Exception {
                                return new EntityListenerActor(entityListener);
                            }
                        })
        );

        final Class<? extends AsyncQueue> aClass = entityListener.getClass();
        System.out.println(aClass);

        AsyncQueue proxy = (AsyncQueue) Enhancer.create(aClass, new MethodInterceptor() {
            @Override
            public Object intercept(final Object o, final Method method, final Object[] objects, final MethodProxy methodProxy) throws Throwable {

                if ("onEntity".equals(method.getName())) {
                    ref.tell(objects[0]);
                } else {
                    return method.invoke(entityListener, objects);
                }

                return null;
            }
        });

        map.put(aClass, proxy);

        return (T) proxy;
    }

    private static class EntityListenerActor extends UntypedActor {

        private AsyncQueue asyncQueue;
        private Method onEntityMethod;

        private EntityListenerActor(final AsyncQueue asyncQueue) {
            this.asyncQueue = asyncQueue;

            try {
                this.onEntityMethod = AsyncQueue.class.getDeclaredMethod("onEntity", Object.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Unable to get onEntity method using reflection.", e);
            }
        }

        @Override
        public void onReceive(Object o) throws Exception {
            onEntityMethod.invoke(asyncQueue, o);
        }
    }

}
