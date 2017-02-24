package edu.vandy;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.Test;

/**
 * Test program for the BuggyQueue that induces race conditions due to
 * lack of synchronization.
 */
public class BuggyQueueTest {
    /**
     * Maximum number of iterations.
     */
    private final static int mMaxIterations = 100000;

    /**
     * Count the number of iterations.
     */
    private final static AtomicInteger mCount =
        new AtomicInteger(0);

    /**
     * This producer runs in a separate Java thread and passes strings
     * to a consumer thread via a shared BlockingQueue.
     */
    private static class Producer<BQ extends BlockingQueue<String>>
                   implements Runnable {
        /**
         * This queue is shared with the consumer.
         */
        private final BQ mQueue;
        
        /**
         * Constructor initializes the BlockingQueue data member.
         */
        Producer(BQ blockingQueue) {
            mQueue = blockingQueue;
        }

        /**
         * This method runs in a separate Java thread and passes
         * strings to a consumer thread via a shared BlockingQueue.
         */
        public void run(){ 
            try {
                for(int i = 0; i < mMaxIterations; i++) {
                    mCount.incrementAndGet();

                    // Calls the put() method.
                    mQueue.put(Integer.toString(i));
                }
            } catch (InterruptedException e) {
                System.out.println("InterruptedException caught");
            }
        }
    }

    /**
     * This consumer runs in a separate Java thread and receives
     * strings from a producer thread via a shared BlockingQueue.
     */
    private static class Consumer<BQ extends BlockingQueue<String>>
                   implements Runnable {
        /**
         * This queue is shared with the producer.
         */
        private final BQ mQueue;
        
        /**
         * Constructor initializes the BlockingQueue data member.
         */
        Consumer(BQ blockingQueue) {
            mQueue = blockingQueue;
        }

        /**
         * This method runs in a separate Java thread and receives
         * strings from a producer thread via a shared BlockingQueue.
         */
        public void run(){ 
            Object s = null;
            int nullCount = 0;
            try {
                for (int i = 0; i < mMaxIterations; i++) {
                    // Calls the take() method.
                    s = mQueue.take();
                    
                    // Only update the state if we get a non-null
                    // value from take().
                    if (s != null) {
                        mCount.decrementAndGet();
                    } else
                        nullCount++;

                    /*
                    if ((i % (mMaxIterations / 10)) == 0)
                        System.out.println(s == null ? "<null>" : s);
                    */
                }
            } catch (InterruptedException e) {
                System.out.println("InterruptedException caught");
            }
            // assertEquals(mCount.get(), 0);

            System.out.println("Final size of the queue is " 
                               + mQueue.size()
                               + "\nmCount is "
                               + mCount.get()
                               + "\nFinal value is "
                               + s
                               + "\nnumber of null returns from take() is "
                               + nullCount
                               + "\nmCount + nullCount is "
                               + (mCount.get() + nullCount));
        }
    }

    /**
     * Main entry point that tests the SimpleQueue class.
     */
    @Test
    public void testBuggyQueue() {
        final BuggyQueue<String> buggyQueue =
            new BuggyQueue<>();

        try {
            // Create producer and consumer threads.
            Thread[] threads = new Thread[] {
                new Thread(new Producer<>(buggyQueue)),
                new Thread(new Consumer<>(buggyQueue))
            };

            // Start all the threads.
            for (Thread thread : threads)
                thread.start();

            // Wait for all threads to stop.
            for (Thread thread : threads)
                thread.join();
        } catch (Exception e) {
            System.out.println("caught exception");
        }
    }
}
