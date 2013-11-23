package cltool4j;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.security.Permission;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Provides commonly-used functionality for testing command-line tools.
 * 
 * @author Aaron Dunlop
 * @since Oct 21, 2009
 */
public abstract class ToolTestCase {
    protected final static String UNIT_TEST_DIR = "unit-test-data/";

    /**
     * Trap {@link System#exit(int)} calls from {@link BaseCommandlineTool}
     */
    @BeforeClass
    public static void suiteSetup() {
        final SecurityManager securityManager = new SecurityManager() {
            @Override
            public void checkPermission(final Permission permission) {
                if (permission.getName().startsWith("exitVM")) {
                    throw new SecurityException("System.exit trapped");
                }
            }
        };
        System.setSecurityManager(securityManager);
    }

    @AfterClass
    public static void suiteTearDown() {
        System.setSecurityManager(null);
    }

    /**
     * Executes the tool with the given arguments, returning the tool output as a String. Output combines
     * STDOUT and STDERR into a single String.
     * 
     * @param tool Tool to be tested
     * @param args Command-line
     * @param input Standard Input
     * @return Tool output (STDOUT and STDERR)
     * @throws Exception if something bad happens
     */
    protected String executeTool(final BaseCommandlineTool tool, final String args, final String input)
            throws Exception {
        return executeTool(tool, args, new ByteArrayInputStream(input.getBytes()));
    }

    /**
     * Executes the tool with the given arguments, returning the tool output as a String. Output combines
     * STDOUT and STDERR into a single String.
     * 
     * @param tool Tool to be tested
     * @param args Command-line
     * @param inputFilename File from unit-test-data directory to use as tool input.
     * @return Tool output (STDOUT and STDERR)
     * @throws Exception if something bad happens
     */
    protected String executeToolFromFile(final BaseCommandlineTool tool, final String args,
            final String inputFilename) throws Exception {
        return executeTool(tool, args, BaseCommandlineTool.fileAsInputStream(UNIT_TEST_DIR + inputFilename));
    }

    /**
     * Executes the tool with the given arguments, using the specified InputStream as input. Output combines
     * STDOUT and STDERR into a single String.
     * 
     * @param tool Tool to be tested
     * @param args Command-line
     * @param inputFilename File from unit-test-data directory to use as tool input.
     * @return Tool output (STDOUT and STDERR)
     * @throws Exception if something bad happens
     */
    protected String executeTool(final BaseCommandlineTool tool, final String args, final InputStream input)
            throws Exception {
        // Clear out any global properties left over from a previous run
        GlobalConfigProperties.singleton().clear();

        // Store STDIN, STDOUT, and STDERR so we can restore them after the test run
        final InputStream systemIn = System.in;
        final PrintStream systemOut = System.out;
        final PrintStream systemErr = System.err;

        final ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
        try {
            // Redirect STDIN, STDOUT, and STDERR for testing
            if (input != null) {
                System.setIn(input);
            }
            final PrintStream ps = new PrintStream(bos);
            System.setOut(ps);
            System.setErr(ps);

            // Execute the tool
            final String[] argArray = args.length() == 0 ? new String[0] : args.split(" ");
            BaseCommandlineTool.initGlobalConfigProperties(tool.getClass(), argArray);
            try {
                tool.runInternal(argArray);
            } catch (final SecurityException e) {
                // This is expected when we trap System.exit()
            }
        } finally {
            // Restore STDIN, STDOUT, STDERR
            System.setIn(systemIn);
            System.setOut(systemOut);
            System.setErr(systemErr);
        }
        final String output = new String(bos.toByteArray());

        // Just to avoid cross-platform issues, we'll replace all forms of newline with '\n'
        return output.replaceAll("\r\n|\r", "\n");
    }

    /**
     * Returns the contents of the specified file
     * 
     * @param filename
     * @return Contents of the specified file
     * @throws IOException
     */
    protected static String unitTestFileAsString(final String filename) throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(UNIT_TEST_DIR
                + filename)));
        final StringBuilder sb = new StringBuilder(1024);
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            sb.append(line);
            sb.append('\n');
        }
        return sb.toString();
    }
}
