package cltool4j;

import static junit.framework.Assert.assertEquals;

import java.util.Random;
import java.util.concurrent.FutureTask;

import org.junit.Test;

/**
 * Unit tests for {@link LinewiseCommandlineTool}. Note that this tests the threading functionality in
 * {@link ThreadableCommandlineTool}.
 */
public class TestLinewiseCommandlineTool extends ToolTestCase {

    /**
     * Tests ordering of multithreaded output, verifying that the lines are returned in the order read even
     * when multiple concurrent threads are processing the file (and sleeping for random intervals).
     * 
     * @throws Exception if an error occurs while executing the tool
     */
    @Test
    public void testLinewiseCat() throws Exception {
        final String filename = "simple.txt";
        final String expectedOutput = ToolTestCase.unitTestFileAsString(filename);
        // First, single-threaded
        assertEquals(expectedOutput, executeToolFromFile(new LinewiseCat(), "-xt 1", filename));

        // And now a 2-thread run
        assertEquals(expectedOutput, executeToolFromFile(new LinewiseCat(), "-xt 2", filename));

        // Finally, 8 threads
        assertEquals(expectedOutput, executeToolFromFile(new LinewiseCat(), "-xt 8", filename));
    }

    /**
     * Outputs each line as-is.
     */
    private static class LinewiseCat extends LinewiseCommandlineTool<String> {
        private final static Random random = new Random();

        @Override
        protected FutureTask<String> lineTask(final String line) {
            return new FutureTask<String>(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(random.nextInt(25));
                    } catch (final InterruptedException ignore) {
                    }
                }
            }, line);
        }
    }
}
