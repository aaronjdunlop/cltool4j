package cltool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Processes input (from files or STDIN) line-by-line. Subclasses must implement a {@link Callable}
 * task to do the processing.
 *
 * @author Aaron Dunlop
 * @since Nov 5, 2008
 *
 * @version $Revision$ $Date$ $Author$
 */
@Threadable
public abstract class LinewiseCommandlineTool extends BaseCommandlineTool
{
    // A simple marker denoting the end of input lines.
    protected final static FutureTask<String> END_OF_INPUT_MARKER = new FutureTask<String>(new Callable<String>()
    {
        @Override
        public String call() throws Exception
        {
            return null;
        }
    });

    @Override
    public final void run() throws Exception
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        if (maxThreads == 1)
        {
            // Single-threaded version is simple...
            for (String line = br.readLine(); line != null; line = br.readLine())
            {
                Callable<String> lineTask = lineTask(line);
                System.out.println(lineTask.call());
            }
            br.close();
        }
        else
        {
            // For the multi-threaded version, we need to create a separate thread which will
            // collect the output and spit it out in-order
            BlockingQueue<FutureTask<String>> outputQueue = new LinkedBlockingQueue<FutureTask<String>>();
            OutputThread outputThread = new OutputThread(outputQueue);
            outputThread.start();

            ExecutorService executor = Executors.newFixedThreadPool(maxThreads);

            for (String line = br.readLine(); line != null; line = br.readLine())
            {
                Callable<String> lineTask = lineTask(line);
                FutureTask<String> futureTask = new FutureTask<String>(lineTask);
                outputQueue.add(futureTask);
                executor.execute(futureTask);
            }
            br.close();

            // Enqueue a marker
            outputQueue.add(END_OF_INPUT_MARKER);

            // The output thread will exit when it comes to the termination marker
            outputThread.join();
            executor.shutdown();
        }
    }

    /**
     * @return a {@link FutureTask} which will process an input line and return a String as output.
     */
    protected abstract Callable<String> lineTask(String line);

    private static class OutputThread extends Thread
    {
        private final BlockingQueue<FutureTask<String>> queue;

        public OutputThread(BlockingQueue<FutureTask<String>> queue)
        {
            this.queue = queue;
        }

        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    FutureTask<String> task = queue.take();
                    if (task == END_OF_INPUT_MARKER)
                    {
                        return;
                    }
                    String output = task.get();
                    System.out.println(output);
                    System.out.flush();
                }
                catch (InterruptedException ignore)
                {}
                catch (ExecutionException e)
                {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
}
