package cltool4j.args4j;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

/**
 * Unit tests for Args4J. Most test scenarios require their own bean class, but the tests are generally fairly
 * simple, so all bean classes are implemented as inner classes of {@link TestArgs4J}.
 * 
 * @author Aaron Dunlop
 */
@SuppressWarnings("unused")
public class TestArgs4J {

    private <T extends Object> T parseArgs(final T bean, final String... args) throws CmdLineException {
        final CmdLineParser parser = new CmdLineParser(bean);
        parser.setUsageWidth(120);
        parser.parseArguments(args);
        return bean;
    }

    @Test
    public void testNativeTypeOptions() throws CmdLineException {
        // boolean (true)
        assertTrue(parseArgs(new NativeTypeOptions(), "-boolean")._boolean);

        // boolean (false)
        assertFalse(parseArgs(new NativeTypeOptions())._boolean);

        // byte
        assertEquals(42, parseArgs(new NativeTypeOptions(), "-byte", "42")._byte);

        // char
        assertEquals('a', parseArgs(new NativeTypeOptions(), "-char", "a")._char);

        // double
        assertEquals(42, parseArgs(new NativeTypeOptions(), "-double", "42")._double, 0.01);

        // float
        assertEquals(42, parseArgs(new NativeTypeOptions(), "-float", "42")._float, 0.01f);

        // int
        assertEquals(42, parseArgs(new NativeTypeOptions(), "-int", "42")._int);

        // long
        assertEquals(42, parseArgs(new NativeTypeOptions(), "-long", "42")._long);

        // short
        assertEquals(42, parseArgs(new NativeTypeOptions(), "-short", "42")._short);

        // String
        assertEquals("42", parseArgs(new NativeTypeOptions(), "-string", "42")._string);
    }

    @Test
    public void testMissingParameter() {
        try {
            parseArgs(new NativeTypeOptions(), "-int");
            fail("Expected CmdLineException");
        } catch (final CmdLineException e) {
            final String expectedError = "Option <-int> takes an operand";
            final String errorMessage = e.getMessage();
            assertTrue("Wrong error message: " + errorMessage, errorMessage.startsWith(expectedError));
        }
    }

    @Test
    public void testExtraParameter() throws Exception {
        try {
            parseArgs(new NativeTypeOptions(), "-boolean", "foo");
            fail("Expected CmdLineException");
        } catch (final CmdLineException e) {
            assertEquals("No arguments allowed", e.getMessage());
            return;
        }
    }

    @Test
    public void testParseExceptions() throws Exception {
        try {
            parseArgs(new NativeTypeOptions(), "-int", "foo");
            fail("Expected CmdLineException");
        } catch (final CmdLineException e) {
            assertEquals("\"foo\" is not a valid argument for -int", e.getMessage());
            return;
        }
    }

    @Test
    public void testNativeTypeArguments() throws CmdLineException {
        // Omitting all arguments
        NativeTypeArguments nta = new NativeTypeArguments();
        parseArgs(nta);
        assertFalse(nta._boolean);
        assertEquals(10, nta._int);
        assertEquals(10, nta._int2);
        assertEquals(10f, nta._float, 0.01f);
        assertEquals("default1", nta._string);
        assertEquals("default2", nta._string2);

        // boolean, int, int, float, String, String
        nta = new NativeTypeArguments();
        parseArgs(nta, "true", "20", "30", "1.1", "value1", "value2");
        assertTrue(nta._boolean);
        assertEquals(20, nta._int);
        assertEquals(30, nta._int2);
        assertEquals(1.1f, nta._float, 0.01f);
        assertEquals("value1", nta._string);
        assertEquals("value2", nta._string2);
    }

    @Test
    public void testMissingRequiredOption() throws Exception {

        try {
            parseArgs(new WithRequiredOptions());
            fail("Expected CmdLineException");
        } catch (final CmdLineException e) {
            assertEquals("Option <-o1> is required", e.getMessage());
        }
    }

    @Test
    public void testMissingRequiredArgument() throws Exception {

        try {
            parseArgs(new WithRequiredArguments());
            fail("Expected CmdLineException");
        } catch (final CmdLineException e) {
            assertEquals("Argument <arg1> is required", e.getMessage());
        }

        try {
            parseArgs(new WithRequiredArguments(), "1");
            fail("Expected CmdLineException");
        } catch (final CmdLineException e) {
            assertEquals("Argument <arg2> is required", e.getMessage());
        }
    }

    @Test
    public void testDefaultValues() throws CmdLineException {
        assertEquals("string", parseArgs(new NativeTypeOptions())._string);
        assertEquals(10, parseArgs(new NativeTypeOptions())._int);
    }

    @Test
    public void testLongOption() throws Exception {
        assertEquals(42, parseArgs(new NativeTypeOptions(), "--int-value", "42")._int);
    }

    @Test
    public void testSetterMethods() throws CmdLineException {
        assertEquals("42", parseArgs(new NativeTypeOptions(), "-stringmethod", "42")._string);
        assertEquals(42, parseArgs(new NativeTypeOptions(), "-intmethod", "42")._int);
    }

    @Test
    public void testInheritance() throws CmdLineException {
        final Subclass s = new Subclass();
        parseArgs(s, "-string", "string1", "-string2", "string2");
        assertEquals("string1", s._string);
        assertEquals("string2", s._string2);
    }

    @Test
    public void testEnum() throws CmdLineException {
        assertEquals(Enum.Animal.HORSE, parseArgs(new Enum(), "-animal", "HORSE").myAnimal);

        // Lowercase
        assertEquals(Enum.Animal.HORSE, parseArgs(new Enum(), "-animal", "horse").myAnimal);

        // Aliased
        assertEquals(Enum.Animal.HORSE, parseArgs(new Enum(), "-animal", "h").myAnimal);

        // A bad argument
        try {
            parseArgs(new Enum(), "-animal", "BAD_ANIMAL");
            fail("Expected parse exception");
        } catch (final CmdLineException expected) {
            // expected
        }

        // And a malformed annotation (multiple uses of the same enum alias)
        try {
            new CmdLineParser(new WithMalformedEnum());
            fail("Expected IllegalAnnotationError");
        } catch (final IllegalAnnotationError expected) {
            assertEquals(
                    "Alias 'h' used more than once for enum class "
                            + WithMalformedEnum.Animal.class.getName(), expected.getMessage());
        }

    }

    @Test
    public void testMultivaluedOptions() throws Exception {

        // List
        WithMultivaluedOptions mv = parseArgs(new WithMultivaluedOptions(), "-list", "one", "-list", "two",
                "-list", "three");
        assertEquals("Expected three values", 3, mv.list.size());
        assertTrue(mv.list.contains("one"));
        assertTrue(mv.list.contains("two"));
        assertTrue(mv.list.contains("three"));

        // Array
        mv = parseArgs(new WithMultivaluedOptions(), "-array", "one", "-array", "two", "-array", "three");
        assertEquals("Should got three values", 3, mv.array.length);
        assertEquals("one", mv.array[0]);
        assertEquals("two", mv.array[1]);
        assertEquals("three", mv.array[2]);

        // Un-initialized HashSet field
        mv = parseArgs(new WithMultivaluedOptions(), "-hashset", "two", "-hashset", "one", "-hashset",
                "three");
        assertEquals("Expected three values", 3, mv.set.size());
        assertTrue("Expected 'one'", mv.set.contains("one"));
        assertTrue("Expected 'two'", mv.set.contains("two"));
        assertTrue("Expected 'three'", mv.set.contains("three"));

        // Pre-initialized TreeSet field
        mv = parseArgs(new WithMultivaluedOptions(), "-treeset", "two", "-treeset", "one", "-treeset",
                "three");
        assertEquals("Expected three values", 3, mv.treeset.size());
        assertTrue("Expected 'one'", mv.treeset.contains("one"));
        assertTrue("Expected 'two'", mv.treeset.contains("two"));
        assertTrue("Expected 'three'", mv.treeset.contains("three"));

        // String field with defaults
        mv = parseArgs(new WithMultivaluedOptions());
        assertEquals("Expected two values", 2, mv.stringsWithDefault.length);
        assertEquals("default1", mv.stringsWithDefault[0]);
        assertEquals("default2", mv.stringsWithDefault[1]);

        // Using separator
        mv = parseArgs(new WithMultivaluedOptions(), "-array2", "one,two");
        assertEquals("Expected two values", 2, mv.stringsWithDefault.length);
        assertEquals("one", mv.stringsWithDefault[0]);
        assertEquals("two", mv.stringsWithDefault[1]);

        // Using multiple options
        mv = parseArgs(new WithMultivaluedOptions(), "-array2", "one", "-array2", "two");
        assertEquals("Expected two values", 2, mv.stringsWithDefault.length);
        assertEquals("one", mv.stringsWithDefault[0]);
        assertEquals("two", mv.stringsWithDefault[1]);

        /*
         * Test multivalued parsing with separators
         */

        mv = parseArgs(new WithMultivaluedOptions(), "-strings", "one,two,three");
        assertEquals("Expected three values", 3, mv.strings.length);
        assertEquals("one", mv.strings[0]);
        assertEquals("two", mv.strings[1]);
        assertEquals("three", mv.strings[2]);

        mv = parseArgs(new WithMultivaluedOptions(), "-ints", "1:2:3");
        assertEquals("Expected three values", 3, mv.ints.length);
        assertEquals(1, mv.ints[0]);
        assertEquals(2, mv.ints[1]);
        assertEquals(3, mv.ints[2]);

        mv = parseArgs(new WithMultivaluedOptions(), "-treeset", "two:one:three");
        assertEquals("Expected three values", 3, mv.treeset.size());
        assertTrue("Expected 'one'", mv.treeset.contains("one"));
        assertTrue("Expected 'two'", mv.treeset.contains("two"));
        assertTrue("Expected 'three'", mv.treeset.contains("three"));
    }

    /**
     * Tests multivalued options separated by commas, colons, etc.
     */
    @Test
    public void testMultivaluedArguments() throws Exception {
        // Using multiple options
        final WithMultivaluedListArgument mvl = parseArgs(new WithMultivaluedListArgument(), "one", "two");
        assertEquals("Expected two values", 2, mvl.list.size());
        assertEquals("one", mvl.list.get(0));
        assertEquals("two", mvl.list.get(1));

        /*
         * Test multivalued parsing with separators
         */

        final WithMultivaluedArrayArgument mva = parseArgs(new WithMultivaluedArrayArgument(), "1", "2,3,4");
        assertEquals("Expected four values", 4, mva.array.length);
        assertEquals(1, mva.array[0]);
        assertEquals(2, mva.array[1]);
        assertEquals(3, mva.array[2]);
        assertEquals(4, mva.array[3]);
    }

    // TODO Implement multivalued setter handling and tests

    @Test
    public void testInvalidArgumentOrdering() throws Exception {

        try {
            new CmdLineParser(new InvalidMultivaluedArgumentClass1());
            fail("Expected IllegalAnnotationError");
        } catch (final IllegalAnnotationError expected) {
            assertEquals("Argument follows multivalued argument", expected.getMessage());
        }

        try {
            new CmdLineParser(new InvalidMultivaluedArgumentClass2());
            fail("Expected IllegalAnnotationError");
        } catch (final IllegalAnnotationError expected) {
            assertEquals("Required argument follows optional argument", expected.getMessage());
        }
    }

    @Test
    public void testUsageMessage() {

        final StringBuilder sb = new StringBuilder();
        sb.append(" arg1       : Integer argument 1\n");
        sb.append(" arg2       : Integer argument 2\n");

        try {
            parseArgs(new WithRequiredArguments());
            fail("Expected CmdLineException");
        } catch (final CmdLineException expected) {
            assertEquals("Argument <arg1> is required", expected.getMessage());
        }

        // Verify usage message for invalid input displays argument metaVar or number if no metaVar
        try {
            parseArgs(new WithRequiredArguments(), "foo");
            fail("Expected CmdLineException");
        } catch (final CmdLineException expected) {
            assertEquals("\"foo\" is not valid for argument <arg1>", expected.getMessage());
        }

        try {
            parseArgs(new WithRequiredArguments(), "1", "foo");
            fail("Expected CmdLineException");
        } catch (final CmdLineException expected) {
            assertEquals("\"foo\" is not valid for argument <arg2>", expected.getMessage());
        }
    }

    @Test
    public void testRequiredAnnotations() throws CmdLineException {
        // Test with -o1
        assertEquals("one", parseArgs(new WithRequiredAnnotation(), "-o1", "one").o1);

        // Test with -o2 - this should fail, since the class doesn't have the @ClassAnnotation2 annotation
        try {
            parseArgs(new WithRequiredAnnotation(), "-o2", "two");
            fail("Expected CmdLineException");
        } catch (final CmdLineException expected) {
            assertEquals("<-o2> is not a valid option", expected.getMessage());
        }
    }

    @Test
    public void testRequiredResource() throws CmdLineException {

        // Test with -o1
        parseArgs(new WithRequiredResource(), "-o1");

        try {
            // Test with -o2 - this should fail, since the required resource isn't in CLASSPATH
            parseArgs(new WithRequiredResource(), "-o2");
            fail("Expected CmdLineException");
        } catch (final CmdLineException expected) {
            assertEquals("<-o2> is not a valid option", expected.getMessage());
        }
    }

    /*
     * ----------------------- Supporting bean classes -----------------------
     */

    private class NativeTypeOptions {

        @Option(name = "-boolean")
        public boolean _boolean;

        @Option(name = "-byte")
        public byte _byte;

        @Option(name = "-char")
        public char _char;

        @Option(name = "-double")
        public double _double;

        @Option(name = "-float")
        public float _float;

        @Option(name = "-int", aliases = { "--int-value" })
        public int _int = 10;

        @Option(name = "-long")
        public long _long;

        @Option(name = "-short")
        public short _short;

        @Option(name = "-string")
        public String _string = "string";

        @Option(name = "-stringmethod")
        public void setString(final String newString) {
            _string = newString;
        }

        @Option(name = "-intmethod")
        public void setInt(final int newInt) {
            _int = newInt;
        }
    }

    private class NativeTypeArguments {

        @Argument(index = 0)
        public boolean _boolean;

        @Argument(index = 1)
        public int _int = 10;
        public int _int2 = 10;

        @Argument(index = 2)
        public void setInt2(final int newInt2) {
            _int2 = newInt2;
        }

        @Argument(index = 3)
        public float _float = 10f;

        @Argument(index = 4)
        public String _string = "default1";
        public String _string2 = "default2";

        @Argument(index = 5)
        public void setString2(final String newString2) {
            _string2 = newString2;
        }

    }

    private class Subclass extends NativeTypeOptions {
        @Option(name = "-string2")
        public String _string2;
    }

    private static class Enum {

        enum Animal {
            HORSE("h"), DUCK("d");
            private Animal(final String... aliases) {
                EnumAliasMap.singleton().addAliases(this, aliases);
            }
        }

        @Option(name = "-animal", usage = "Give your favorite animal.")
        Animal myAnimal;

    }

    private static class WithMalformedEnum {

        enum Animal {
            HORSE("h"), DUCK("h");
            private Animal(final String... aliases) {
                EnumAliasMap.singleton().addAliases(this, aliases);
            }
        }

        @Option(name = "-animal", usage = "Give your favorite animal.")
        Animal myAnimal = Animal.HORSE;
    }

    private class LongUsage {

        @Option(name = "-LongNamedStringOption", usage = "String option", metaVar = "USE_A_NICE_STRING")
        private String s;

        @Option(name = "-i", metaVar = "arg", usage = "Integer option")
        private int i;

    }

    @Retention(RUNTIME)
    @Target({ TYPE })
    private @interface ClassAnnotation1 {
    }

    @Retention(RUNTIME)
    @Target({ TYPE })
    private @interface ClassAnnotation2 {
    }

    @ClassAnnotation1
    private class WithRequiredAnnotation {

        @Option(name = "-o1", requiredAnnotations = { ClassAnnotation1.class })
        String o1;

        @Option(name = "-o2", requiredAnnotations = { ClassAnnotation2.class })
        String o2;
    }

    private class WithRequiredResource {

        @Option(name = "-o1", requiredResource = "cltool4j/args4j/TestArgs4J.class")
        boolean o1;

        @Option(name = "-o2", requiredResource = "Missing.class")
        boolean o2;
    }

    private class WithMultivaluedOptions {

        // On Lists, the multiValued parameter defaults to 'true'
        @Option(name = "-list")
        List<String> list;

        @Option(name = "-strings", separator = ",")
        String[] strings;

        @Option(name = "-ints", separator = ":")
        int[] ints;

        @Option(name = "-array", multiValued = true)
        String[] array;

        @Option(name = "-hashset")
        Set<String> set;

        @Option(name = "-treeset", separator = ":")
        Set<String> treeset = new TreeSet<String>();

        @Option(name = "-array2", separator = ",")
        String[] stringsWithDefault = { "default1", "default2" };
    }

    private class WithMultivaluedListArgument {

        // On Lists, the multiValued parameter defaults to 'true'
        @Argument(index = 0, metaVar = "values", multiValued = true, separator = ",")
        List<String> list;
    }

    private class WithMultivaluedArrayArgument {

        // On Lists, the multiValued parameter defaults to 'true'
        @Argument(index = 0, metaVar = "values", multiValued = true, separator = ",")
        int[] array;
    }

    private class WithProperties {

        @Option(name = "-O")
        private Properties properties;
    }

    protected class WithRequiredOptions {
        @Option(name = "-o1", required = true)
        public String o1;

        @Option(name = "-o2", required = true)
        public String o2;
    }

    protected class WithRequiredArguments {
        @Argument(index = 0, metaVar = "arg1", usage = "Integer argument 1", required = true)
        public int arg1;

        @Argument(index = 1, metaVar = "arg2", usage = "Integer argument 2", required = true)
        public int arg2;
    }

    /**
     * Tests an invalid ordering of arguments. A multivalued argument with 'uses up' all arguments provided,
     * so {@link #arg2} can never be populated.
     * 
     */
    private class InvalidMultivaluedArgumentClass1 {

        @Argument(index = 0, multiValued = true)
        private String args1[];

        @Argument(index = 1)
        private String arg2;
    }

    /**
     * Tests another invalid ordering of arguments. Since {@link #optionalArg} is followed by a multivalued
     * argument, we can't tell if the first argument provided is intended to populate {@link #optionalArg} or
     * {@link #requiredArgs}.
     */
    private class InvalidMultivaluedArgumentClass2 {

        @Argument(required = false, index = 0)
        private String optionalArg;

        @Argument(required = true, index = 1, multiValued = true)
        private String requiredArgs[];
    }

}
