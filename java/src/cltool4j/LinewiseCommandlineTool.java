package cltool4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.FutureTask;

/**
 * Processes input (from files or STDIN) line-by-line (optionally using multiple threads). Subclasses must
 * implement a {@link FutureTask} task to do the processing.
 * 
 * @author Aaron Dunlop
 * @since Nov 5, 2008
 * 
 * @param <R> Type produced by processing of each input line
 */
public abstract class LinewiseCommandlineTool<R> extends ThreadableCommandlineTool<String, R> {

    /**
     * @return a {@link FutureTask} which will process an input line and return a String as output.
     */
    protected abstract FutureTask<R> lineTask(String line);

    private BufferedReader inputReader;

    @Override
    public final String nextInput() throws IOException {
        synchronized (this) {
            if (inputReader == null) {
                inputReader = inputAsBufferedReader();
            }
        }
        synchronized (inputReader) {
            return inputReader.readLine();
        }
    }

    // Delegates to lineTask(), since previous subclasses use that name
    @Override
    protected final FutureTask<R> task(final String line) {
        return lineTask(line);
    }
}
