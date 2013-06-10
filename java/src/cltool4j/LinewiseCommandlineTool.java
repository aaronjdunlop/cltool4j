package cltool4j;

import java.io.BufferedReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Processes input (from files or STDIN) line-by-line (optionally using multiple threads). Subclasses must
 * implement a {@link Callable} task to do the processing.
 * 
 * @author Aaron Dunlop
 * @since Nov 5, 2008
 * 
 * @param <R> Type produced by processing of each input line
 */
@Threadable
public abstract class LinewiseCommandlineTool<R> extends BaseCommandlineTool {

    // A simple marker denoting the end of input lines.
    protected final FutureTask<R> END_OF_INPUT_MARKER = new FutureTask<R>(new Callable<R>() {
        @Override
        public R call() throws Exception {
            return null;
        }
    });

    @Override
    public final void run() throws Exception {
        final BufferedReader br = inputAsBufferedReader();

        if (maxThreads == 1) {
            // Single-threaded version is simple...
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                final FutureTask<R> lineTask = lineTask(line);
                lineTask.run();
                output(lineTask.get());
            }
            br.close();
        } else {
            // For the multi-threaded version, we need to create a separate thread which will
            // collect the output and spit it out in-order

            // Allocate a queue large enough to contain several pending tasks for each thread, but small
            // enough to avoid attempting to schedule all future jobs at once.
            final BlockingQueue<FutureTask<R>> outputQueue = new LinkedBlockingQueue<FutureTask<R>>(
                    maxThreads * 4);
            final OutputThread outputThread = new OutputThread(outputQueue);
            outputThread.start();

            final ExecutorService executor = Executors.newFixedThreadPool(maxThreads);

            for (String line = br.readLine(); line != null; line = br.readLine()) {
                final FutureTask<R> futureTask = lineTask(line);
                outputQueue.put(futureTask);
                executor.execute(futureTask);
            }
            br.close();

            // Enqueue a marker
            outputQueue.put(END_OF_INPUT_MARKER);

            // The output thread will exit when it comes to the termination marker
            outputThread.join();
            executor.shutdown();
        }
    }

    /**
     * @return a {@link FutureTask} which will process an input line and return a String as output.
     */
    protected abstract FutureTask<R> lineTask(String line);

    /**
     * Outputs the result to STDOUT
     * 
     * @param result
     */
    protected void output(final R result) {
        final String s = result.toString();
        if (s.length() > 0) {
            System.out.println(s);
            System.out.flush();
        }
    }

    private class OutputThread extends Thread {

        private final BlockingQueue<FutureTask<R>> queue;

        public OutputThread(final BlockingQueue<FutureTask<R>> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    final FutureTask<R> task = queue.take();
                    if (task == END_OF_INPUT_MARKER) {
                        return;
                    }
                    output(task.get());
                } catch (final InterruptedException ignore) {
                } catch (final ExecutionException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
}
