package org.brylex.krekar;

public class QueueManagerImpl implements QueueManager {

    private final Queue queue;


    public QueueManagerImpl(final Queue queue) {
        this.queue = queue;
    }

    @Override
    public void process() {
        System.err.println("PROCESS[" + queue.name() + "] :: " + Thread.currentThread());
    }
}
