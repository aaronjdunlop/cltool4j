package cltool4j;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
        return executeTool(tool, args, new ByteArrayInputStream(input.getBytes()), false);
    }

    /**
     * Executes the tool with the given arguments, returning the tool output as a String. Output combines
     * STDOUT and STDERR into a single String.
     * 
     * @param tool Tool to be tested
     * @param args Command-line
     * @param input Standard Input
     * @param teeToStdout If true, output is printed to STDOUT as well as being captured and returned
     * 
     * @return Tool output (STDOUT and STDERR)
     * @throws Exception if something bad happens
     */
    protected String executeTool(final BaseCommandlineTool tool, final String args, final String input,
            final boolean teeToStdout) throws Exception {
        return executeTool(tool, args, new ByteArrayInputStream(input.getBytes()), teeToStdout);
    }

    /**
     * Executes the tool with the given arguments, returning the tool output as a String. Output combines
     * STDOUT and STDERR into a single String.
     * 
     * @param tool Tool to be tested
     * @param args Command-line
     * @param inputFilename File from unit-test-data directory to use as tool input.
     * 
     * @return Tool output (STDOUT and STDERR)
     * @throws Exception if something bad happens
     */
    protected String executeToolFromFile(final BaseCommandlineTool tool, final String args,
            final String inputFilename) throws Exception {
        return executeTool(tool, args, BaseCommandlineTool.fileAsInputStream(UNIT_TEST_DIR + inputFilename),
                false);
    }

    /**
     * Executes the tool with the given arguments, returning the tool output as a String. Output combines
     * STDOUT and STDERR into a single String.
     * 
     * @param tool Tool to be tested
     * @param args Command-line
     * @param inputFilename File from unit-test-data directory to use as tool input.
     * 
     * @return Tool output (STDOUT and STDERR)
     * @throws Exception if something bad happens
     */
    protected String executeToolFromFile(final BaseCommandlineTool tool, final String args,
            final String inputFilename, final boolean teeToStdout) throws Exception {
        return executeTool(tool, args, BaseCommandlineTool.fileAsInputStream(UNIT_TEST_DIR + inputFilename),
                teeToStdout);
    }

    /**
     * Executes the tool with the given arguments, using the specified InputStream as input. Output combines
     * STDOUT and STDERR into a single String.
     * 
     * @param tool Tool to be tested
     * @param args Command-line
     * @param input Source of tool input (simulating STDIN or input files)
     * @param teeToStdout If true, output is printed to STDOUT as well as being captured and returned
     * @return Tool output (STDOUT and STDERR)
     * 
     * @throws Exception if something bad happens
     */
    protected String executeTool(final BaseCommandlineTool tool, final String args, final InputStream input,
            final boolean teeToStdout) throws Exception {
        // Clear out any global properties left over from a previous run
        GlobalConfigProperties.singleton().clear();

        // Store STDIN, STDOUT, and STDERR so we can restore them after the test run
        final InputStream systemIn = System.in;
        final PrintStream systemOut = System.out;
        final PrintStream systemErr = System.err;

        final ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
        PrintStream stdoutPs;
        PrintStream stderrPs;
        if (teeToStdout) {
            stdoutPs = new TeePrintStream(bos, systemOut, true);
            stderrPs = new TeePrintStream(bos, systemErr, true);
        } else {
            stdoutPs = new PrintStream(bos);
            stderrPs = new PrintStream(bos);
        }
        try {
            // Redirect STDIN, STDOUT, and STDERR for testing
            if (input != null) {
                System.setIn(input);
            }
            System.setOut(stdoutPs);
            System.setErr(stderrPs);

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
     * @param filename Unit-test input file
     * @return Contents of the specified file
     * @throws IOException If an error occurs while reading from <code>filename</code>
     */
    protected static String unitTestFileAsString(final String filename) throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(UNIT_TEST_DIR
                + filename)));
        final StringBuilder sb = new StringBuilder(1024);
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            sb.append(line);
            sb.append('\n');
        }
        br.close();
        return sb.toString();
    }

    /**
     * Classic splitter of {@link PrintStream}. Named after the unix 'tee' command. It allows a stream to be
     * branched off so there are now two streams. Adapted from the Apache Commons-IO TeeOutputStream (licensed
     * under Apache License 2.0 - see http://www.apache.org/licenses/LICENSE-2.0)
     * 
     * Original license statement:
     * 
     * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See
     * the NOTICE file distributed with this work for additional information regarding copyright ownership.
     * The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not
     * use this file except in compliance with the License. You may obtain a copy of the License at
     * 
     * Unless required by applicable law or agreed to in writing, software distributed under the License is
     * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
     * implied. See the License for the specific language governing permissions and limitations under the
     * License.
     */
    public class TeePrintStream extends PrintStream {

        /** the second {@link PrintStream} to write to */
        private final PrintStream branch;
        /**
         * If true, {@link #close()} will <em>not</em> close the main {@link PrintStream}. E.g., to leave
         * {@link System#out} open when teeing it to another stream.
         */
        private final boolean dontCloseMainStream;

        /**
         * Constructs a {@link TeePrintStream}.
         * 
         * @param out The underlying {@link OutputStream}
         * @param branch the second {@link PrintStream}
         * @param dontCloseMainStream If true, {@link #close()} will close <code>branch</code>, and leave the
         *            main {@link PrintStream} open
         */
        public TeePrintStream(final OutputStream out, final PrintStream branch,
                final boolean dontCloseMainStream) {
            super(out);
            this.branch = branch;
            this.dontCloseMainStream = dontCloseMainStream;
        }

        /**
         * Write the bytes to both streams.
         * 
         * @param b the bytes to write
         * @throws IOException if an I/O error occurs
         */
        @Override
        public synchronized void write(final byte[] b) throws IOException {
            super.write(b);
            this.branch.write(b);
        }

        /**
         * Write the specified bytes to both streams.
         * 
         * @param b the bytes to write
         * @param off The start offset
         * @param len The number of bytes to write
         */
        @Override
        public synchronized void write(final byte[] b, final int off, final int len) {
            super.write(b, off, len);
            this.branch.write(b, off, len);
        }

        /**
         * Write a character to both streams.
         * 
         * @param b the character to write
         */
        @Override
        public synchronized void write(final int b) {
            super.write(b);
            this.branch.write(b);
        }

        /**
         * Flushes both streams.
         */
        @Override
        public void flush() {
            super.flush();
            this.branch.flush();
        }

        /**
         * Closes both output streams.
         */
        @Override
        public void close() {
            try {
                super.close();
            } finally {
                if (!dontCloseMainStream) {
                    this.branch.close();
                }
            }
        }

    }
}
