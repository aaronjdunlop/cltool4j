package cltool4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.SequenceInputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import cltool4j.args4j.Argument;
import cltool4j.args4j.ArgumentParser;
import cltool4j.args4j.CalendarParser;
import cltool4j.args4j.CmdLineException;
import cltool4j.args4j.CmdLineParser;
import cltool4j.args4j.EnumAliasMap;
import cltool4j.args4j.Option;

/**
 * Base class for any tools which should be executable from the command-line. This class implements the
 * majority of the functionality needed to execute java code as a 'standard' command-line tool, including
 * parsing command-line options and reading input from either STDIN or from multiple files specified on the
 * command-line.
 * 
 * Unfortunately, it doesn't appear possible to determine the actual class being executed within
 * <code>main(String[])</code>, so each subclass must implement a <code>main(String[])</code> method and call
 * {@link BaseCommandlineTool#run(String[])} from within it.
 * 
 * In addition, subclasses should include a no-argument constructor and the abstract methods declared here in
 * the superclass.
 * 
 * 
 * @author Aaron Dunlop
 * @since Aug 14, 2008
 * 
 *        $Id$
 */
public abstract class BaseCommandlineTool {

    @Option(name = "-help", aliases = { "--help", "-?" }, ignoreRequired = true, usage = "Print detailed usage information")
    protected boolean printHelp = false;

    @Option(name = "-readme", aliases = { "--readme" }, hidden = true, ignoreRequired = true, usage = "Print full documentation", requiredResource = "META-INF/README.txt")
    protected boolean printReadme = false;

    @Option(name = "-license", aliases = { "--license" }, hidden = true, ignoreRequired = true, usage = "Print license", requiredResource = "META-INF/LICENSE.txt")
    protected boolean printLicense = false;

    @Option(name = "-O", metaVar = "option / file", multiValued = true, usage = "Option or option file (file in Java properties format or option as key=value)")
    protected String[] options = new String[0];

    @Option(name = "-v", metaVar = "level", usage = "Verbosity")
    protected LogLevel verbosityLevel = LogLevel.info;

    @Option(name = "-version", aliases = { "--version" }, hidden = true, ignoreRequired = true, usage = "Print version information")
    protected boolean printVersion = false;

    /**
     * Non-threadable tools use a single thread; {@link Threadable} tools default to either the optional
     * 'defaultThreads' parameter or the number of CPUs
     */
    @Option(name = "-xt", metaVar = "threads", usage = "Maximum threads", requiredAnnotations = { Threadable.class })
    protected int maxThreads = getClass().getAnnotation(Threadable.class) != null ? (getClass()
            .getAnnotation(Threadable.class).defaultThreads() != 0 ? getClass().getAnnotation(
            Threadable.class).defaultThreads() : Runtime.getRuntime().availableProcessors()) : 1;

    protected final static Logger baseLogger = Logger.getLogger("");

    @Argument(multiValued = true, metaVar = "files")
    protected String[] inputFiles = new String[0];

    protected Exception exception;

    protected String currentInputFile;

    /**
     * Default constructor
     */
    protected BaseCommandlineTool() {
    }

    /**
     * Perform any tool-specific setup. This method will only be called once, even if the tool is threadable
     * and {@link #run()} is called by multiple threads.
     */
    protected void setup() throws Exception {
    }

    /**
     * Perform any tool-specific cleanup. This method will only be called once, even if the tool is threadable
     * and {@link #run()} is called by multiple threads.
     */
    protected void cleanup() {
    }

    /**
     * Execute the tool's core functionality. If the tool is threadable, this method must be thread-safe and
     * reentrant.
     * 
     * @throws Exception
     */
    protected abstract void run() throws Exception;

    /**
     * Callback executed when starting to process a new input file. Subclasses may override
     * {@link #beginFile(String)} if they wish to be notified when the input source changes.
     * 
     * @param filename
     */
    protected void beginFile(final String filename) {
    }

    /**
     * Parses command-line arguments and executes the tool. This method should be called from within the
     * main() methods of all subclasses.
     * 
     * @param args
     */
    @SuppressWarnings("unchecked")
    public final static void run(final String[] args) {
        try {
            final Class<? extends BaseCommandlineTool> c = (Class<? extends BaseCommandlineTool>) Class
                    .forName(Thread.currentThread().getStackTrace()[2].getClassName());

            // For Scala objects
            try {
                final BaseCommandlineTool tool = (BaseCommandlineTool) c.getField("MODULE$").get(null);
                tool.runInternal(args);
            } catch (final Exception e) {
                // For Java
                final BaseCommandlineTool tool = c.getConstructor(new Class[] {})
                        .newInstance(new Object[] {});
                tool.runInternal(args);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    protected final void runInternal(final String[] args) throws Exception {
        final CmdLineParser parser = new CmdLineParser(this);
        parser.setUsageWidth(120);

        try {
            parser.parseArguments(args);

            // If the user specified -help, print extended usage information and exit
            if (printHelp) {
                // Don't print out default value for help flag
                printHelp = false;

                printUsage(parser, true);
                return;
            } else if (printReadme) {
                printToStdout(getClass().getClassLoader().getResourceAsStream("META-INF/README.txt"));
                return;
            } else if (printLicense) {
                printToStdout(getClass().getClassLoader().getResourceAsStream("META-INF/LICENSE.txt"));
                return;
            } else if (printVersion) {
                try {
                    final Class<? extends BaseCommandlineTool> c = getClass();
                    final String classFileName = c.getSimpleName() + ".class";
                    final String pathToThisClass = c.getResource(classFileName).toString();

                    final String pathToManifest = pathToThisClass.toString().substring(0,
                            pathToThisClass.indexOf("!") + 1)
                            + "/META-INF/MANIFEST.MF";

                    final Manifest manifest = new Manifest(new URL(pathToManifest).openStream());

                    if (manifest.getMainAttributes().getValue("Version") != null
                            && manifest.getMainAttributes().getValue("Version").length() > 0) {
                        System.out.println("Version: " + manifest.getMainAttributes().getValue("Version"));
                    }
                    System.out.println("Built at: " + manifest.getMainAttributes().getValue("Build-Time")
                            + " from source revision "
                            + manifest.getMainAttributes().getValue("Source-Revision"));
                } catch (final Exception e) {
                    System.out.println("Version information unavailable");
                }
                return;
            }

            // Configure GlobalProperties from property files or command-line options (-O)

            // First, iterate through any property files specified, 'merging' the file contents together (in
            // case of a duplicate key, the last one found wins)
            for (final String o : options) {
                final String[] keyValue = o.split("=");
                if (keyValue.length != 2) {
                    // Treat it as a property file name
                    final Properties p = new Properties();
                    p.load(new FileReader(o));
                    GlobalConfigProperties.singleton().mergeOver(p);
                }
            }

            // Now iterate though any key-value pairs specified directly on the command-line; those override
            // properties loaded from files, and again, the last one found wins.
            for (final String o : options) {
                final String[] keyValue = o.split("=");
                if (keyValue.length == 2) {
                    GlobalConfigProperties.singleton().setProperty(keyValue[0], keyValue[1]);
                }
            }

            // Configure java.util.logging to log to the console, and only the message actually
            // logged, without any header or formatting.
            for (final Handler h : baseLogger.getHandlers()) {
                baseLogger.removeHandler(h);
            }
            baseLogger.setUseParentHandlers(false);
            final Level l = verbosityLevel.toLevel();
            baseLogger.addHandler(new SystemOutHandler(l));
            baseLogger.setLevel(l);

            // If input files were specified on the command-line, check for the first one before running
            // setup()
            // If it cannot be found, we'd prefer to fail here than after a potentially expensive setup() call
            if (inputFiles.length > 0 && inputFiles[0].length() > 0) {
                if (!new File(inputFiles[0]).exists()) {
                    throw new CmdLineException("Unable to find file: " + inputFiles[0]);
                }
            }

            setup();
        } catch (final CmdLineException e) {
            System.err.println(e.getMessage() + '\n');
            printUsage(parser, false);
            return;
        }

        // Handle arguments
        if (inputFiles.length > 0 && inputFiles[0].length() > 0) {
            // Handle one or more input files from the command-line, translating gzipped
            // files as appropriate. Re-route multiple files into a single InputStream so we can execute the
            // tool a single time.
            // Open all files prior to processing, so we can fail early if one or more files cannot be opened
            final LinkedList<InputStream> inputList = new LinkedList<InputStream>();
            for (final String filename : inputFiles) {
                inputList.add(fileAsInputStream(filename));
            }

            final InputStream is = new MultiInputStream(inputList);
            System.setIn(is);
            run();
            is.close();

        } else {
            // Handle input on STDIN
            run();
        }

        if (exception != null) {
            throw exception;
        }

        cleanup();
        System.out.flush();
        System.out.close();
    }

    private void printUsage(final CmdLineParser parser, final boolean includeHiddenOptions) {
        String classname = getClass().getName();
        classname = classname.substring(classname.lastIndexOf('.') + 1);
        if (classname.endsWith("$")) {
            classname = classname.substring(0, classname.length() - 1);
        }
        System.err.print("Usage: " + classname);
        parser.printOneLineUsage(new OutputStreamWriter(System.err), includeHiddenOptions);
        parser.printUsage(new OutputStreamWriter(System.err), includeHiddenOptions);
    }

    /**
     * Convenience method; opens the specified file, uncompressing GZIP'd files as appropriate.
     * 
     * @param filename
     * @return InputStream
     * @throws IOException
     */
    protected InputStream fileAsInputStream(final String filename) throws IOException {
        return fileAsInputStream(new File(filename));
    }

    /**
     * Convenience method; opens the specified file, uncompressing GZIP'd files as appropriate.
     * 
     * @param f File
     * @return InputStream
     * @throws IOException
     */
    protected InputStream fileAsInputStream(final File f) throws IOException {
        if (!f.exists()) {
            System.err.println("Unable to find file: " + f.getName());
            System.err.flush();
            System.exit(-1);
        }

        InputStream is = new FileInputStream(f);
        if (f.getName().endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }
        return is;
    }

    /**
     * Convenience method; opens the specified file, uncompressing GZIP'd files as appropriate.
     * 
     * @param f File
     * @return BufferedReader
     * @throws IOException
     */
    protected BufferedReader fileAsBufferedReader(final File f) throws IOException {
        return new BufferedReader(new InputStreamReader(fileAsInputStream(f)));
    }

    /**
     * Convenience method; opens the specified file, uncompressing GZIP'd files as appropriate.
     * 
     * @param filename
     * @return BufferedReader
     * @throws IOException
     */
    protected BufferedReader fileAsBufferedReader(final String filename) throws IOException {
        return fileAsBufferedReader(new File(filename));
    }

    /**
     * Convenience method; reads the file in its entirety, uncompressing GZIP'd files as appropriate. Warning:
     * This method is not particularly efficient, and may consume large amounts of CPU and memory if executed
     * on a large file.
     * 
     * @param filename
     * @return InputStream
     * @throws IOException
     */
    protected String fileAsString(final String filename) throws IOException {
        final StringBuilder sb = new StringBuilder(10240);
        final BufferedReader r = fileAsBufferedReader(filename);
        for (int c = r.read(); c != 0; c = r.read()) {
            sb.append((char) c);
        }
        return sb.toString();
    }

    /**
     * Prints the entire content of the {@link InputStream} to STDOUT.
     * 
     * @param input
     * @throws IOException
     */
    private void printToStdout(final InputStream input) throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(input));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            System.out.println(line);
        }
    }

    /**
     * @return This morning at 00:00:00 local time as a {@link Date}
     */
    protected Date todayMidnight() {
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTime();
    }

    /**
     * @return Yesterday at 00:00:00 local time as a {@link Date}
     */
    protected Date yesterdayMidnight() {
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DATE, cal.get(Calendar.DATE) - 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTime();
    }

    /**
     * @return Yesterday at 23:59:59 local time as a {@link Date}
     */
    protected Date yesterday235959() {
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DATE, cal.get(Calendar.DATE) - 1);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        return cal.getTime();
    }

    /**
     * Parses a date into a long (seconds since the epoch)
     */
    public static class TimestampParser extends ArgumentParser<Long> {

        @Override
        public Long parse(final String s) {
            return CalendarParser.parseDate(s.toLowerCase()).getTime().getTime();
        }

        @Override
        public String defaultMetaVar() {
            return "date";
        }
    }

    public static enum LogLevel {
        all("+5", "5"), finest("+4", "4"), finer("+3", "3"), fine("+2", "2", "debug"), config("+1", "1"), info(
                "0"), warning("-1"), severe("-2"), off("-3");

        private LogLevel(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }

        public Level toLevel() {
            switch (this) {
            case all:
                return Level.ALL;
            case finest:
                return Level.FINEST;
            case finer:
                return Level.FINER;
            case fine:
                return Level.FINE;
            case config:
                return Level.CONFIG;
            case info:
                return Level.INFO;
            case warning:
                return Level.WARNING;
            case severe:
                return Level.SEVERE;
            case off:
                return Level.OFF;
            default:
                return null;
            }
        }
    }

    private static class SystemOutHandler extends Handler {

        public SystemOutHandler(final Level level) {
            setLevel(level);
        }

        @Override
        public void close() throws SecurityException {
            flush();
        }

        @Override
        public void flush() {
            System.out.flush();
        }

        @Override
        public void publish(final LogRecord record) {
            System.out.println(record.getMessage());
        }
    }

    /**
     * Combines multiple {@link InputStream}s into a single stream. Adapted from {@link SequenceInputStream}
     * to alert {@link BaseCommandlineTool} when beginning a new file.
     */
    private class MultiInputStream extends InputStream {
        Iterator<? extends InputStream> streamIterator;
        InputStream currentStream;
        int currentFileIndex = -1;

        public MultiInputStream(List<? extends InputStream> inputStreams) {
            this.streamIterator = inputStreams.iterator();
            try {
                next();
            } catch (IOException ex) {
                // This should never happen
                throw new Error("panic");
            }
        }

        /**
         * Proceed on to the next input file
         */
        final void next() throws IOException {
            if (currentStream != null) {
                currentStream.close();
            }

            if (streamIterator.hasNext()) {
                currentStream = streamIterator.next();
                currentInputFile = inputFiles[++currentFileIndex];
                beginFile(currentInputFile);
                if (currentStream == null) {
                    throw new NullPointerException();
                }
            } else {
                currentStream = null;
            }
        }

        /**
         * @return an estimate of the number of bytes that can be read (or skipped over) from the current
         *         underlying input stream without blocking or {@code 0} if this input stream has been closed
         *         by invoking its {@link #close()} method
         * @exception IOException if an I/O error occurs.
         */
        public int available() throws IOException {
            if (currentStream == null) {
                return 0; // no way to signal EOF from available()
            }
            return currentStream.available();
        }

        /**
         * @return the next byte of data, or <code>-1</code> if the end of the stream is reached.
         * @exception IOException if an I/O error occurs.
         */
        public int read() throws IOException {
            if (currentStream == null) {
                return -1;
            }
            int c = currentStream.read();
            if (c == -1) {
                next();
                return read();
            }
            return c;
        }

        /**
         * Reads up to <code>len</code> bytes of data from this input stream into an array of bytes. If
         * <code>len</code> is not zero, the method blocks until at least 1 byte of input is available;
         * otherwise, no bytes are read and <code>0</code> is returned.
         * <p>
         * The <code>read</code> method of <code>SequenceInputStream</code> tries to read the data from the
         * current substream. If it fails to read any characters because the substream has reached the end of
         * the stream, it calls the <code>close</code> method of the current substream and begins reading from
         * the next substream.
         * 
         * @param b the buffer into which the data is read.
         * @param off the start offset in array <code>b</code> at which the data is written.
         * @param len the maximum number of bytes read.
         * @return int the number of bytes read.
         * @exception NullPointerException If <code>b</code> is <code>null</code>.
         * @exception IndexOutOfBoundsException If <code>off</code> is negative, <code>len</code> is negative,
         *                or <code>len</code> is greater than <code>b.length - off</code>
         * @exception IOException if an I/O error occurs.
         */
        public int read(byte b[], int off, int len) throws IOException {
            if (currentStream == null) {
                return -1;
            } else if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }

            int n = currentStream.read(b, off, len);
            if (n <= 0) {
                // TODO Insert a line-feed at the end of a file?
                next();
                return read(b, off, len);
            }
            return n;
        }

        /**
         * Closes this input stream and releases any system resources associated with the stream. A closed
         * <code>SequenceInputStream</code> cannot perform input operations and cannot be reopened.
         * 
         * @exception IOException if an I/O error occurs.
         */
        public void close() throws IOException {
            do {
                next();
            } while (currentStream != null);
        }
    }
}
