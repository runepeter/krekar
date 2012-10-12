package org.brylex.krekar;

import org.junit.Test;

public class BrokerTest {

    @Test
    public void testName() throws Exception {

        final Broker broker = new Broker();

        TestAsyncQueue entityListener = broker.registerQueue(new TestAsyncQueue());

        entityListener.onEntity("TEST");

        Thread.sleep(5000);
    }

    public static class TestAsyncQueue implements AsyncQueue<String> {

        @Override
        public void onEntity(String entity) {
            System.out.println(entity);
        }
    }

}
