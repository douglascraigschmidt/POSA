package vandy.mooc.prime.utils;

/**
 *
 */
public class ThresholdCrosser {
    private int mCount;

    /**
     *
     */
    void setInitialCount(int initialCount) {
        mCount = initialCount;
    }

    /**
     *
     */
    public ThresholdCrosser(int initialCount) {
        mCount = initialCount;
    }
    
    /**
     *
     */
    public void incrementAndCallAtN(int n,
                                    Runnable runnable) {
        synchronized(this) {
            if (++mCount == n)
                runnable.run();
        }
    }

    /**
     *
     */
    public void decrementAndCallAtN(int n,
                                    Runnable runnable) {
        synchronized(this) {
            if (--mCount == n)
                runnable.run();
        }
    }
}
