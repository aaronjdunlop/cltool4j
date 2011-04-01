package cltool4j;


public abstract class ThreadLocalLinewiseClTool<T,R> extends LinewiseCommandlineTool<R>
{
    ThreadLocal<T> threadLocal = new ThreadLocal<T>();

    /**
     * Creates an instance of a thread-local data structure (generally a structure that is
     * expensive to create and initialize). This method will be called by the first task invoked
     * on a thread; subsequent tasks executed on the same thread will reuse the existing data
     * structure.
     *
     * @return An instance of a thread-local data structure.
     */
    public abstract T createLocal();

    /**
     * @return Thread-local data structure.
     */
    protected final T getLocal()
    {
        T local = threadLocal.get();
        if (local == null)
        {
            local = createLocal();
            threadLocal.set(local);
        }
        return local;
    }
}
