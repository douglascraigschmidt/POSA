package vandy.mooc.prime.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Function;

import static vandy.mooc.prime.utils.LaunderThrowable.launderThrowable;

/**
 *
 */
public class Memoizer<A, V>
        implements Function<A, V> {
    private final ConcurrentMap<A, Future<V>> cache =
            new ConcurrentHashMap<>();

    private final Function<A, V> c;

    public Memoizer(Function<A, V> c) { this.c = c; }

    public V apply(final A arg) {
        while (true) {
            Future<V> f = cache.get(arg);
            if (f == null) {
                FutureTask<V> ft =
                        new FutureTask<>(() -> c.apply(arg));
                f = cache.putIfAbsent(arg, ft);
                if (f == null) {
                    f = ft;
                    ft.run();
                }
            } else {
                System.out.println("value " + arg + " was cached");
            }

            try {
                return f.get();
            } catch (CancellationException e) {
                cache.remove(arg, f);
            } catch (Exception e) {
                throw launderThrowable(e.getCause());
            }
        }
    }
}
