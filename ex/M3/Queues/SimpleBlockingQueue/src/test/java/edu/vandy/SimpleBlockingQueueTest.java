package edu.vandy;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * Test program for the SimpleBlockingQueue that fixes race conditions
 * by having proper synchronization (i.e., mutual exclusion and
 * coordination).
 */
public class SimpleBlockingQueueTest { 
    /**
     * Maximum number of iterations.
     */
    private final static int mMaxIterations = 100000;

    /**
     * Maximum size of the queue.
     */
    private final static int sQUEUE_SIZE = 10;

    /**
     * Count the number of iterations.
     */
    private final static AtomicInteger mCount =
        new AtomicInteger(0);

    /**
     * This producer runs in a separate Java thread and passes integers
     * to a consumer thread via a shared BlockingQueue.
     */
    private static class Producer<BQ extends BlockingQueue<Integer>>
           implements Runnable {
        /**
         * This queue is shared with the consumer.
         */
        private final BQ mQueue;
        
        /**
         * Constructor initializes the BlockingQueue data
         * member.
         */
        Producer(BQ blockingQueue) {
            mQueue = blockingQueue;
        }

        /**
         * This method runs in a separate Java thread and passes
         * integers to a consumer thread via a shared BlockingQueue.
         */
        public void run(){ 
            try {
                for (int i = 0; i < mMaxIterations; i++) {
                    mCount.incrementAndGet();

                    // Call the put() method.
                    mQueue.put(Integer.valueOf(i));
                }
            } catch (InterruptedException e) {
                System.out.println("InterruptedException caught");
            }
        }
    }

    /**
     * This consumer runs in a separate Java thread and receives
     * integers from a producer thread via a shared BlockingQueue.
     */
    private static class Consumer<BQ extends BlockingQueue<Integer>>
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
         * integers from a producer thread[q via a shared BlockingQueue.
         */
        public void run(){
            Integer integer = null;
            int nullCount = 0;

            try {
                // Get the first item from the queue.
                Integer previous = mQueue.take();
                mCount.decrementAndGet();

                for (int i = 1; i < mMaxIterations; ) {
                    // Calls the take() method.
                    integer = mQueue.take();

                    // Make sure the entries are ordered.
                    assertEquals(previous + 1, integer.intValue());
                    previous = integer;
                        
                    // Only update the state if we get a non-null
                    // value from take().
                    if (integer != null) {
                        mCount.decrementAndGet();
                        i++;
                    } else
                        nullCount++;

                    if((i % (mMaxIterations / 10)) == 0)
                        System.out.println(integer);
                }
                } catch (InterruptedException e) {
                    System.out.println("InterruptedException caught");
                }
                assertEquals(mCount.get(), 0);

                System.out.println("Final size of the queue is " 
                                   + mQueue.size()
                                   + "\nmCount is "
                                   + mCount.get()
                                   + "\nFinal value is "
                                   + integer
                                   + "\nnumber of null returns from take() is "
                                   + nullCount
                                   + "\nmCount + nullCount is "
                                   + (mCount.get() + nullCount));
            }
    }

    /**
     * Main entry point that tests the SimpleBlockingQueue class.
     */
    @Test
    public void testSimpleBlockingQueue() {
        final SimpleBlockingQueue<Integer> simpleQueue =
            new SimpleBlockingQueue<>(sQUEUE_SIZE);

        try {
            // Create producer and consumer threads.
            Thread[] threads = new Thread[] {
                new Thread(new Producer<>(simpleQueue)),
                new Thread(new Consumer<>(simpleQueue))
            };

            // Start both threads.
            for (Thread thread : threads)
                thread.start();

            // Wait for both threads to stop.
            for (Thread thread : threads)
                thread.join();
        } catch (Exception e) {
            System.out.println("caught exception");
        }
    }
}
