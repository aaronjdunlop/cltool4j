package cltool4j;

import java.util.concurrent.Callable;

/**
 * A {@link LinewiseCommandlineTool} which maintains a single data structure for each thread. Generally used
 * when threading a tool which requires a non-thread-safe data structure too expensive to create in each call
 * to {@link Callable#call()}. That local object is created once and maintained using a {@link ThreadLocal}.
 * 
 * @author Aaron Dunlop
 * 
 * @param <L> Type of the local data structure.
 * @param <R> Type produced by processing of each input line
 */
public abstract class ThreadLocalLinewiseClTool<L, R> extends LinewiseCommandlineTool<R> {
    ThreadLocal<L> threadLocal = new ThreadLocal<L>();

    /**
     * Creates an instance of a thread-local data structure (generally a structure that is expensive to create
     * and initialize). This method will be called by the first task invoked on a thread; subsequent tasks
     * executed on the same thread will reuse the existing data structure.
     * 
     * @return An instance of a thread-local data structure.
     */
    public abstract L createLocal();

    /**
     * @return Thread-local data structure.
     */
    protected final L getLocal() {
        L local = threadLocal.get();
        if (local == null) {
            local = createLocal();
            threadLocal.set(local);
        }
        return local;
    }
}
