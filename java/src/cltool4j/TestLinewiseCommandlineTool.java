package cltool4j;

import static junit.framework.Assert.assertEquals;

import java.util.concurrent.FutureTask;

import org.junit.Test;

public class TestLinewiseCommandlineTool extends ToolTestCase {
    /**
     * Tests ordering of multithreaded output, verifying that the lines are returned in the order read even
     * when 2 concurrent threads are processing the file.
     * 
     * @throws Exception
     */
    @Test
    public void testLinewiseCat() throws Exception {
        String filename = "simple.txt";
        String expectedOutput = ToolTestCase.unitTestFileAsString(filename);
        // First, single-threaded
        assertEquals(expectedOutput, executeToolFromFile(new LinewiseCat(), "-xt 1", filename));

        // And now a 2-thread run
        assertEquals(expectedOutput, executeToolFromFile(new LinewiseCat(), "-xt 2", filename));
    }

    /**
     * Outputs each line as-is.
     */
    private static class LinewiseCat extends LinewiseCommandlineTool<String> {
        @Override
        protected FutureTask<String> lineTask(final String line) {
            return new FutureTask<String>(new Runnable() {
                public void run() {
                }
            }, line);
        }
    }
}
