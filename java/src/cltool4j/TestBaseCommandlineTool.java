package cltool4j;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.junit.Test;

import cltool4j.ConfigProperties.InvalidConfigurationException;
import cltool4j.args4j.Argument;
import cltool4j.args4j.EnumAliasMap;
import cltool4j.args4j.Option;

/**
 * Unit tests for {@link BaseCommandlineTool}.
 * 
 * @author Aaron Dunlop
 */
public class TestBaseCommandlineTool extends ToolTestCase {

    /**
     * Verifies that the {@link BaseCommandlineTool#setup()} method is executed properly
     */
    @Test
    public void testSetup() throws Exception {
        final Cat tool = new Cat();
        executeTool(tool, "", "");
        assertTrue("Expected setupFlag = 'true'", tool.setupFlag);
        assertEquals(2, tool.option);
    }

    @Test
    public void testCat() throws Exception {
        final String input = "This is a\nthree-line\ntest.\n";
        final String output = executeTool(new Cat(), "", input);
        assertEquals(input, output);
    }

    /**
     * Tests iterating over input using the {@link java.lang.Iterable} returned by
     * {@link BaseCommandlineTool#inputLines()}.
     */
    @Test
    public void testIterableCat() throws Exception {
        final String input = "This is a\nthree-line\ntest.\n";
        final String output = executeTool(new IterableCat(), "", input);
        assertEquals(input, output);
    }

    @Test
    public void testDebugOutput() throws Exception {
        final String input = "This is a\nthree-line\ntest.\n";

        StringBuilder sb = new StringBuilder();
        sb.append("MaxThreads: 1\n");
        sb.append(input);

        final String output = executeTool(new Cat(), "-v debug", input);
        // Just in case the actual run took a measurable amount of time

        assertEquals(sb.toString(), output);

        // Try a couple other ways of specifying verbosity
        sb = new StringBuilder();
        sb.append("MaxThreads: 1\n");
        sb.append(input);
        assertEquals(sb.toString(), executeTool(new Cat(), "-v 2", input));
        assertEquals(sb.toString(), executeTool(new Cat(), "-v +2", input));
        assertEquals(input, executeTool(new Cat(), "-v -1", input));
    }

    /**
     * Tests argument parsing. Pass three arguments to a class which declares one required and one optional
     * argument. We expect the declared arguments to be populated and the third to be considerd as an input
     * file.
     */
    @Test
    public void testArguments() throws Exception {
        final WithRequiredArguments tool = new WithRequiredArguments();
        executeTool(tool, "-option foo arg0 arg1 arg2", "");
        assertEquals("foo", tool.option);
        assertEquals("arg0", tool.arg0);
        assertEquals("arg1", tool.arg1);
        assertArrayEquals(new String[] { "arg2" }, tool.otherArgs);
    }

    /**
     * Tests parsing required multi-value arguments. Tests a class which declares two arguments and required
     * multi-value args.
     */
    @Test
    public void testRequiredMultivaluedArgument() throws Exception {
        final WithRequiredMultivaluedArgument tool = new WithRequiredMultivaluedArgument();

        // Verify that multi-valued arg is parsed
        executeTool(tool, "arg1 arg2 arg3", "");
        assertArrayEquals(new String[] { "arg1", "arg2", "arg3" }, tool.requiredArgs);

        // Verify usage output if multi-valued arg is not supplied
        final StringBuilder sb = new StringBuilder();
        sb.append("Argument <args> is required\n");
        sb.append("\n");
        sb.append("Usage: TestBaseCommandlineTool$WithRequiredMultivaluedArgument [-help] [-O option / file] [-v level] <args>\n");
        sb.append(" args                : [args]\n");
        sb.append(" -help (--help,-?)   : Print usage information\n");
        sb.append(" -O <option / file>  : Option or option file (file in Java properties format or option as key=value)\n");
        sb.append(" -v <level>          : Verbosity  (all,+5,5; finest,+4,4; finer,+3,3; fine,+2,2,debug; config,+1,1; info,0; warning,-1;\n");
        sb.append("                       severe,-2; off,-3)   Default = info\n");

        assertEquals(sb.toString(), executeTool(tool, "", ""));
    }

    /**
     * Tests parsing required multi-value arguments. Tests a class which declares two arguments and required
     * multi-value args.
     */
    @Test
    public void testRequiredMultiValuedArgumentWithOtherArguments() throws Exception {
        WithRequiredArgumentsAndMultivaluedArgument tool = new WithRequiredArgumentsAndMultivaluedArgument();

        // Verify that multi-valued parameters are parsed
        executeTool(tool, "arg0 arg1 arg2 arg3 arg4", "");
        assertEquals("arg0", tool.arg0);
        assertEquals("arg1", tool.arg1);
        assertArrayEquals(new String[] { "arg2", "arg3", "arg4" }, tool.otherArgs);

        // Verify usage output if multi-valued params are not supplied
        StringBuilder sb = new StringBuilder();
        sb.append("Argument <arg0> is required\n");
        sb.append("\n");
        sb.append("Usage: TestBaseCommandlineTool$WithRequiredArgumentsAndMultivaluedArgument [-help] [-O option / file] [-v level] <arg0> <arg1> <values>\n");
        sb.append(" arg0                : arg0\n");
        sb.append(" arg1                : arg1\n");
        sb.append(" values              : Other required arguments\n");
        sb.append(" -help (--help,-?)   : Print usage information\n");
        sb.append(" -O <option / file>  : Option or option file (file in Java properties format or option as key=value)\n");
        sb.append(" -v <level>          : Verbosity  (all,+5,5; finest,+4,4; finer,+3,3; fine,+2,2,debug; config,+1,1; info,0; warning,-1;\n");
        sb.append("                       severe,-2; off,-3)   Default = info\n");
        tool = new WithRequiredArgumentsAndMultivaluedArgument();
        assertEquals(sb.toString(), executeTool(tool, "", ""));

        sb = new StringBuilder();
        sb.append("Argument <values> is required\n");
        sb.append("\n");
        sb.append("Usage: TestBaseCommandlineTool$WithRequiredArgumentsAndMultivaluedArgument [-help] [-O option / file] [-v level] <arg0> <arg1> <values>\n");
        sb.append(" arg0                : arg0\n");
        sb.append(" arg1                : arg1\n");
        sb.append(" values              : Other required arguments\n");
        sb.append(" -help (--help,-?)   : Print usage information\n");
        sb.append(" -O <option / file>  : Option or option file (file in Java properties format or option as key=value)\n");
        sb.append(" -v <level>          : Verbosity  (all,+5,5; finest,+4,4; finer,+3,3; fine,+2,2,debug; config,+1,1; info,0; warning,-1;\n");
        sb.append("                       severe,-2; off,-3)   Default = info\n");

        tool = new WithRequiredArgumentsAndMultivaluedArgument();
        assertEquals(sb.toString(), executeTool(tool, "arg1 arg2", ""));
    }

    /**
     * Tests parsing required multi-value arguments. Tests a class which declares optional single- and
     * multi-valued arguments.
     */
    @Test
    public void testOptionalMultivaluedArgument() throws Exception {
        final WithOptionalMultivaluedArgument tool = new WithOptionalMultivaluedArgument();

        // Verify that both single- and multi-valued args is parsed
        executeTool(tool, "arg1", "");
        assertEquals("arg1", tool.optionalArg);
        assertNull(tool.optionalArgs);

        executeTool(tool, "arg1 arg2 arg3", "");
        assertEquals("arg1", tool.optionalArg);
        assertArrayEquals(new String[] { "arg2", "arg3" }, tool.optionalArgs);
    }

    @Test
    public void testStandardUsageOutput() throws Exception {
        // Test with an invalid option
        StringBuilder sb = new StringBuilder();
        sb.append("<-badarg> is not a valid option\n");
        sb.append("\n");
        sb.append("Usage: TestBaseCommandlineTool$Cat [-help] [-O option / file] [-v level] [-option opt] [files]\n");
        sb.append(" -help (--help,-?)   : Print usage information\n");
        sb.append(" -O <option / file>  : Option or option file (file in Java properties format or option as key=value)\n");
        sb.append(" -v <level>          : Verbosity  (all,+5,5; finest,+4,4; finer,+3,3; fine,+2,2,debug; config,+1,1; info,0; warning,-1;\n");
        sb.append("                       severe,-2; off,-3)   Default = info\n");
        sb.append(" -option <opt>       : Integer option;   Default = 2\n");

        assertEquals(sb.toString(), executeTool(new Cat(), "-badarg", ""));

        // With a missing option
        sb = new StringBuilder();
        sb.append("Option <-option> is required\n");
        sb.append("\n");
        sb.append("Usage: TestBaseCommandlineTool$WithRequiredArguments [-help] [-O option / file] [-v level] [-xt threads] <-option value> <arg0> [arg1] [values]\n");
        sb.append(" arg0                : arg0\n");
        sb.append(" arg1                : arg1\n");
        sb.append(" values              : [other args]\n");
        sb.append(" -help (--help,-?)   : Print usage information\n");
        sb.append(" -O <option / file>  : Option or option file (file in Java properties format or option as key=value)\n");
        sb.append(" -v <level>          : Verbosity  (all,+5,5; finest,+4,4; finer,+3,3; fine,+2,2,debug; config,+1,1; info,0; warning,-1;\n");
        sb.append("                       severe,-2; off,-3)   Default = info\n");
        sb.append(" -xt <threads>       : Maximum threads;   Default = "
                + Runtime.getRuntime().availableProcessors() + "\n");
        sb.append(" -option <value>     : o\n");

        WithRequiredArguments tool = new WithRequiredArguments();
        assertEquals(sb.toString(), executeTool(tool, "argument", ""));

        // With a missing argument
        sb = new StringBuilder();
        sb.append("Argument <arg0> is required\n");
        sb.append("\n");
        sb.append("Usage: TestBaseCommandlineTool$WithRequiredArguments [-help] [-O option / file] [-v level] [-xt threads] <-option value> <arg0> [arg1] [values]\n");
        sb.append(" arg0                : arg0\n");
        sb.append(" arg1                : arg1\n");
        sb.append(" values              : [other args]\n");
        sb.append(" -help (--help,-?)   : Print usage information\n");
        sb.append(" -O <option / file>  : Option or option file (file in Java properties format or option as key=value)\n");
        sb.append(" -v <level>          : Verbosity  (all,+5,5; finest,+4,4; finer,+3,3; fine,+2,2,debug; config,+1,1; info,0; warning,-1;\n");
        sb.append("                       severe,-2; off,-3)   Default = info\n");
        sb.append(" -xt <threads>       : Maximum threads;   Default = "
                + Runtime.getRuntime().availableProcessors() + "\n");
        sb.append(" -option <value>     : o\n");

        tool = new WithRequiredArguments();
        assertEquals(sb.toString(), executeTool(tool, "-option foo", ""));
    }

    @Test
    public void testExtendedUsageOutput() throws Exception {
        // Test with an invalid option
        final StringBuilder sb = new StringBuilder();
        sb.append("Usage: TestBaseCommandlineTool$Cat [-help] [-O option / file] [-v level] [-version] [-charset] [-pause] [-option opt] [-hidden] [files]\n");
        sb.append(" -help (--help,-?)    : Print usage information\n");
        sb.append(" -O <option / file>   : Option or option file (file in Java properties format or option as key=value)\n");
        sb.append(" -v <level>           : Verbosity  (all,+5,5; finest,+4,4; finer,+3,3; fine,+2,2,debug; config,+1,1; info,0;\n");
        sb.append("                        warning,-1; severe,-2; off,-3)   Default = info\n");
        sb.append(" -version (--version) : Print version information\n");
        sb.append(" -charset             : Charset of all input (STDIN and files)\n");
        sb.append(" -pause               : Pause for a single carriage-return after setup\n");
        sb.append(" -option <opt>        : Integer option;   Default = 2\n");
        sb.append(" -hidden              : Hidden option\n");

        // Basic check of -help output
        assertEquals(sb.toString(), executeTool(new Cat(), "-help", ""));

        // Verify that if we run with -? and -option, the default reported for -option is still correct (i.e.,
        // that we print help info prior to setting any values on the tool instance)
        assertEquals(sb.toString(), executeTool(new Cat(), "-? -option 3", ""));
    }

    /**
     * Tests line wrapping for long enum options
     */
    @Test
    public void testWrappedEnumUsageOutput() throws Exception {
        final StringBuilder sb = new StringBuilder();
        sb.append("<-option> is not a valid option\n");
        sb.append("\n");
        sb.append("Usage: TestBaseCommandlineTool$WithEnumField [-help] [-O option / file] [-v level] [enum] [files]\n");
        sb.append(" enum                : Enum value;   Default = VeryLongOptionNameB\n");
        sb.append("                         VeryLongOptionNameA,a\n");
        sb.append("                         VeryLongOptionNameB,b\n");
        sb.append("                         VeryLongOptionNameC,c\n");
        sb.append("                         VeryLongOptionNameD,d\n");
        sb.append("                         VeryLongOptionNameE,e\n");
        sb.append("                         VeryLongOptionNameF,f\n");
        sb.append("                         VeryLongOptionNameG,g\n");
        sb.append("                         VeryLongOptionNameH,h\n");
        sb.append(" -help (--help,-?)   : Print usage information\n");
        sb.append(" -O <option / file>  : Option or option file (file in Java properties format or option as key=value)\n");
        sb.append(" -v <level>          : Verbosity  (all,+5,5; finest,+4,4; finer,+3,3; fine,+2,2,debug; config,+1,1; info,0; warning,-1;\n");
        sb.append("                       severe,-2; off,-3)   Default = info\n");

        assertEquals(sb.toString(), executeTool(new WithEnumField(), "-option foo", ""));
    }

    @Test
    public void testMultivaluedOption() throws Exception {
        final WithMultivaluedOption tool = new WithMultivaluedOption();
        executeTool(tool, "", "");
        assertArrayEquals(new int[] { 1 }, tool.intOpts);

        executeTool(tool, "-i 2,3", "");
        assertArrayEquals(new int[] { 2, 3 }, tool.intOpts);
    }

    @Test
    public void testDateOptionHandler() throws Exception {
        final WithDateField tool = new WithDateField();
        executeTool(tool, "2008.12.01", "");
        assertEquals(1228118400000L, tool.date.getTime());

        executeTool(tool, "12.01.2008", "");
        assertEquals(1228118400000L, tool.date.getTime());

        executeTool(tool, "2008/12/01", "");
        assertEquals(1228118400000L, tool.date.getTime());

        executeTool(tool, "12/01/2008", "");
        assertEquals(1228118400000L, tool.date.getTime());

        // And test a parse failure
        final String output = executeTool(tool, "123456", "");
        assertTrue(output.startsWith("\"123456\" is not valid for argument <date>"));
    }

    /**
     * Verifies that one and only one option from a 'choice group' is required
     */
    @Test
    public void testChoiceGroup() throws Exception {
        final WithChoiceGroups tool = new WithChoiceGroups();
        executeTool(tool, "-o1 1 -o3 3", "");
        executeTool(tool, "-o2 2 -o3 3", "");

        // Test a multi-valued option in a choice group
        executeTool(tool, "-o2 2 -o6 3 -o6 4", "");

        String output = executeTool(tool, "-o3 3", "");
        assertTrue(output.startsWith("One of <-o1> or <-o2> is required"));

        output = executeTool(tool, "-o1 1 -o2 1", "");
        assertTrue(output.startsWith("Only one of <-o1> or <-o2> is allowed"));

        output = executeTool(tool, "-o1 1", "");
        assertTrue(output.startsWith("One of <-o3>, <-o4>, <-o5>, or <-o6> is required"));

        output = executeTool(tool, "-o1 1 -o3 3 -o4 4", "");
        assertTrue(output.startsWith("Only one of <-o3>, <-o4>, <-o5>, or <-o6> is allowed"));
    }

    /**
     * Verifies that only one option from an optional 'choice group' is allowed
     */
    @Test
    public void testOptionalChoiceGroup() throws Exception {
        final WithOptionalChoiceGroups tool = new WithOptionalChoiceGroups();

        // We should be able to run without either option
        executeTool(tool, "", "");
        // Or with either one
        executeTool(tool, "-o1 1", "");
        executeTool(tool, "-o2 2", "");

        // But not with both
        final String output = executeTool(tool, "-o1 1 -o2 2", "");
        assertTrue(output.startsWith("Only one of <-o1> or <-o2> is allowed"));
    }

    /**
     * Tests the {@link Option#requires()} parameter.
     */
    @Test
    public void testRequires() throws Exception {
        final WithRequires tool = new WithRequires();

        // We should be able to run without either option
        String output = executeTool(tool, "", "");
        assertEquals("", output);
        // Or with both
        output = executeTool(tool, "-o1 1 -o2 2", "");
        assertEquals("", output);

        // But -o1 depends on -o2
        output = executeTool(tool, "-o1 1", "");
        assertTrue(output.startsWith("Option <-o2> is required for <-o1>"));

        // And -o2 depends on -o1
        output = executeTool(tool, "-o2 2", "");
        assertTrue(output.startsWith("Option <-o1> is required for <-o2>"));
    }

    @Test
    public void testFileAlerts() throws Exception {
        final Wc tool = new Wc();
        final String output = executeTool(tool, "unit-test-data/file1.txt unit-test-data/file2.txt",
                (InputStream) null, false);
        assertEquals("unit-test-data/file1.txt : 1\nunit-test-data/file2.txt : 2\n", output);
    }

    @Test
    public void testCalendarOptionHandler() throws Exception {
        final WithCalendarField tool = new WithCalendarField();
        executeTool(tool, "2008.12.01", "");
        assertEquals(2008, tool.cal.get(Calendar.YEAR));
        assertEquals(11, tool.cal.get(Calendar.MONTH));
        assertEquals(1, tool.cal.get(Calendar.DATE));

        // And test a parse failure
        final String output = executeTool(tool, "123456", "");
        assertTrue(output.startsWith("\"123456\" is not valid for argument <date>"));
    }

    @Test
    public void testMemoryOptionHandler() throws Exception {
        final WithMemoryField tool = new WithMemoryField();
        executeTool(tool, "1500", "");
        assertEquals(1500, tool.memory);
        executeTool(tool, "1500m", "");
        assertEquals(1500 * 1024 * 1024, tool.memory);
        executeTool(tool, "2k", "");
        assertEquals(2048, tool.memory);
        executeTool(tool, "1g", "");
        assertEquals(1024 * 1024 * 1024, tool.memory);

        // And test a parse failure
        final String output = executeTool(tool, "2z", "");
        assertTrue(output.startsWith("\"2z\" is not valid for argument <value>"));
    }

    /**
     * Tests enumeration handling - both through case-insensitive lexical matching and through the forString()
     * method.
     */
    @Test
    public void testEnumOptionHandler() throws Exception {
        final WithEnumField tool = new WithEnumField();
        executeTool(tool, "", "");
        assertEquals(Enum.VeryLongOptionNameB, tool.e);
        executeTool(tool, "VeryLongOptionNameC", "");
        assertEquals(Enum.VeryLongOptionNameC, tool.e);
        executeTool(tool, "verylongoptionnamea", "");
        assertEquals(Enum.VeryLongOptionNameA, tool.e);
        executeTool(tool, "b", "");
        assertEquals(Enum.VeryLongOptionNameB, tool.e);

        // Test a parse failure and usage info
        final String output = executeTool(tool, "2z", "");
        assertTrue("Wrong error output: " + output,
                output.startsWith("\"2z\" is not valid for argument <enum>"));
    }

    /**
     * Tests handling of global properties - specified on the command-line, in a properties file, and
     * overriding a properties file from the command-line.
     */
    @Test
    public void testGlobalProperties() throws Exception {
        final PrintGlobalProperties tool = new PrintGlobalProperties();
        // Specify some properties on the command-line
        assertEquals("test.keyA=test.valueA\ntest.keyB=test.valueB\n",
                executeTool(tool, "-O test.keyA=test.valueA -O test.keyB=test.valueB", ""));

        // And in a property file
        assertEquals("test.keyA=test.valueA\ntest.keyB=test.valueB\ntest.keyC=test.valueC\n",
                executeTool(tool, "-O " + UNIT_TEST_DIR + "/test.properties", ""));

        // Now, both (and override one from the property file)
        assertEquals("test.keyA=test.valueA\ntest.keyB=test.valueB\ntest.keyC=test.valueC2\n",
                executeTool(tool, "-O test.keyC=test.valueC2 -O " + UNIT_TEST_DIR + "/test.properties", ""));

        // We expect GlobalProperties to parse integer and floating-point values sensibly and to throw an
        // exception if we ask for an unset configuration parameter
        executeTool(tool, "-O test.int=2 -O test.float=0.3", "");
        assertEquals(2, GlobalConfigProperties.singleton().getIntProperty("test.int"));
        assertEquals(0.3f, GlobalConfigProperties.singleton().getFloatProperty("test.float"), .01f);
        try {
            assertEquals(2, GlobalConfigProperties.singleton().getProperty("test.unset"));
        } catch (final InvalidConfigurationException expected) {
        }
    }

    @SuppressWarnings("unused")
    private static class Cat extends BaseCommandlineTool {
        private boolean setupFlag = false;

        @Option(name = "-option", metaVar = "opt", usage = "Integer option")
        public int option = 2;

        @Option(name = "-hidden", hidden = true, usage = "Hidden option")
        public boolean hidden = false;

        public Cat() {
        }

        @Override
        public void setup() throws Exception {
            setupFlag = true;
        }

        @Override
        public void run() throws Exception {
            baseLogger.fine("MaxThreads: " + maxThreads);
            final StringBuffer sb = new StringBuffer();
            for (final String arg : inputFiles) {
                sb.append(arg);
                sb.append(' ');
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }

            final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            for (String s = br.readLine(); s != null; s = br.readLine()) {
                System.out.println(s);
            }
        }
    }

    /**
     * Uses the {@link BaseCommandlineTool#inputLines()} iteration method.
     */
    private static class IterableCat extends BaseCommandlineTool {

        @Override
        public void run() throws Exception {
            for (final String line : inputLines()) {
                System.out.println(line);
            }
        }
    }

    /**
     * Tests usage of {@link BaseCommandlineTool#currentInputFile}
     */
    private static class Wc extends BaseCommandlineTool {

        private HashMap<String, Integer> lines = new HashMap<String, Integer>();

        @Override
        public void run() throws Exception {
            final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                final int count = lines.containsKey(currentInputFile) ? lines.get(currentInputFile) : 0;
                lines.put(currentInputFile, count + 1);
            }

            for (final String filename : inputFiles) {
                System.out.println(filename + " : " + lines.get(filename));
            }
        }
    }

    private static class WithMultivaluedOption extends BaseCommandlineTool {
        @Option(name = "-i", separator = ",", usage = "[args]", metaVar = "args")
        private int[] intOpts = { 1 };

        @Override
        protected void run() throws Exception {
        }
    }

    @Threadable
    private static class WithRequiredArguments extends BaseCommandlineTool {
        @Argument(index = 0, required = true, usage = "arg0", metaVar = "arg0")
        private String arg0;

        @Argument(index = 1, usage = "arg1", metaVar = "arg1")
        private String arg1;

        @Argument(usage = "[other args]", metaVar = "values")
        private String[] otherArgs;

        @Option(name = "-option", usage = "o", required = true, metaVar = "value")
        private String option;

        @Override
        protected void run() throws Exception {
        }
    }

    private static class WithRequiredMultivaluedArgument extends BaseCommandlineTool {
        @Argument(required = true, multiValued = true, usage = "[args]", metaVar = "args")
        private String[] requiredArgs;

        @Override
        protected void run() throws Exception {
        }
    }

    private static class WithOptionalMultivaluedArgument extends BaseCommandlineTool {
        @Argument(required = false, usage = "[arg]", metaVar = "arg")
        private String optionalArg;

        @Argument(required = false, multiValued = true, usage = "[args]", metaVar = "args")
        private String[] optionalArgs;

        @Override
        protected void run() throws Exception {
        }
    }

    private static class WithRequiredArgumentsAndMultivaluedArgument extends BaseCommandlineTool {
        // arg0 and arg1 are declared in reverse order, just to confirm that 'index' is used and not
        // declaration ordering.
        @Argument(index = 1, required = true, usage = "arg1", metaVar = "arg1")
        private String arg1;

        @Argument(index = 0, required = true, usage = "arg0", metaVar = "arg0")
        private String arg0;

        @Argument(required = true, usage = "Other required arguments", metaVar = "values")
        private String[] otherArgs;

        @Override
        protected void run() throws Exception {
            final StringBuilder sb = new StringBuilder();
            for (final String arg : otherArgs) {
                sb.append(arg);
                sb.append(' ');
            }
            System.out.println(sb.substring(0, sb.length() - 2));
        }
    }

    @SuppressWarnings("unused")
    private static class WithChoiceGroups extends BaseCommandlineTool {
        @Option(name = "-o1", usage = "o", choiceGroup = "A", metaVar = "value")
        private String o1;

        @Option(name = "-o2", usage = "o", choiceGroup = "A", metaVar = "value")
        private String o2;

        @Option(name = "-o3", usage = "o", choiceGroup = "B", metaVar = "value")
        private String o3;

        @Option(name = "-o4", usage = "o", choiceGroup = "B", metaVar = "value")
        private String o4;

        @Option(name = "-o5", usage = "o", choiceGroup = "B", metaVar = "value")
        private String o5;

        @Option(name = "-o6", usage = "o", choiceGroup = "B", metaVar = "value")
        private String[] o6;

        @Override
        protected void run() throws Exception {
        }
    }

    @SuppressWarnings("unused")
    private static class WithOptionalChoiceGroups extends BaseCommandlineTool {
        @Option(name = "-o1", usage = "o", optionalChoiceGroup = "A", metaVar = "value")
        private String o1;

        @Option(name = "-o2", usage = "o", optionalChoiceGroup = "A", metaVar = "value")
        private String o2;

        @Override
        protected void run() throws Exception {
        }
    }

    @SuppressWarnings("unused")
    private static class WithRequires extends BaseCommandlineTool {
        @Option(name = "-o1", usage = "o", requires = "-o2", metaVar = "value")
        private String o1;

        @Option(name = "-o2", usage = "o", requires = "-o1", metaVar = "value")
        private String o2;

        @Override
        protected void run() throws Exception {
        }
    }

    private static class WithCalendarField extends BaseCommandlineTool {
        @Argument(index = 0, required = true, usage = "date", metaVar = "date")
        protected Calendar cal;

        @Override
        protected void run() throws Exception {
        }
    }

    private static class WithDateField extends BaseCommandlineTool {
        @Argument(index = 0, required = true, usage = "date", metaVar = "date")
        protected Date date;

        @Override
        protected void run() throws Exception {
        }
    }

    private static class WithMemoryField extends BaseCommandlineTool {
        @Argument(index = 0, required = true, usage = "Memory value", metaVar = "value")
        protected int memory;

        @Override
        protected void run() throws Exception {
        }
    }

    private static class WithEnumField extends BaseCommandlineTool {
        @Argument(index = 0, usage = "Enum value", metaVar = "enum")
        protected Enum e = Enum.VeryLongOptionNameB;

        @Override
        protected void run() throws Exception {
        }
    }

    public static enum Enum {
        VeryLongOptionNameA("a"), VeryLongOptionNameB("b"), VeryLongOptionNameC("c"), VeryLongOptionNameD("d"), VeryLongOptionNameE(
                "e"), VeryLongOptionNameF("f"), VeryLongOptionNameG("g"), VeryLongOptionNameH("h");

        private Enum(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }

    private static class PrintGlobalProperties extends BaseCommandlineTool {
        @Override
        protected void run() throws Exception {
            System.out.println(GlobalConfigProperties.singleton().toString());
        }
    }
}
