package org.brylex.krekar;


import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

public class JallaTest {

    @Test
    public void testName() throws Exception {

        final ApplicationContext applicationContext = new AnnotationConfigApplicationContext(TestConfiguration.class);

        final DoStuffService service = applicationContext.getBean(DoStuffService.class);

        for (int i = 0; i < 500; i++) {
            service.doIt();
        }

        Thread.sleep(10000);
    }

    @Component
    public static class AsynchronousBean implements EntityListener<String> {
        @Override
        public void onEntity(String entity) {
            System.err.println("[" + entity + " :: " + Thread.currentThread() + "]");
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
            asynchronousBean.onEntity("JALLA(" + System.currentTimeMillis() + ")");
        }
    }

    @Configuration
    @ComponentScan({"org.brylex.krekar"})
    public static class TestConfiguration {

    }

}
