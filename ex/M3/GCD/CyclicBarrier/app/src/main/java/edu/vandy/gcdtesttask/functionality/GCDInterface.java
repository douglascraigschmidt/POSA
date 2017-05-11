package edu.vandy.gcdtesttask.functionality;

/**
 * This functional interface matches the signature of all the
 * GCDInterface methods.
 */
@FunctionalInterface
public interface GCDInterface {
    /**
     * Compute and return the GCD for parameters @a a and @a b.
     */
    int compute(int a,
                int b);
}
