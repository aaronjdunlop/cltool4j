package cltool4j.args4j;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Command line argument owner.
 * 
 * <p>
 * For a typical usage, see <a
 * href="https://args4j.dev.java.net/source/browse/args4j/args4j/examples/SampleMain.java?view=markup">this
 * example</a>.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class CmdLineParser {

    /**
     * Known {@link ArgumentParser}s, mapped by class name.
     */
    private final Map<String, ArgumentParser<?>> argumentParsers = new HashMap<String, ArgumentParser<?>>();

    /**
     * Discovered {@link Setter}s for options.
     */
    private final ArrayList<Setter<?>> optionSetters = new ArrayList<Setter<?>>();
    private final Map<String, Setter<?>> argumentSettersByArgumentName = new HashMap<String, Setter<?>>();

    /**
     * Discovered {@link Setter}s for arguments.
     */
    private final List<Setter<?>> argumentSetters = new ArrayList<Setter<?>>();

    /**
     * The length of a usage line. If the usage message is longer than this value, the parser wraps the line.
     * Defaults to 80.
     */
    private int usageWidth = 80;

    /**
     * Creates a new command line owner that parses arguments/options and set them into the given object.
     * 
     * @param bean instance of a class annotated by {@link Option} and {@link Argument}. this object will
     *            receive values. If this is null, the processing will be skipped, which is useful if you'd
     *            like to feed metadata from other sources.
     * @throws CmdLineException
     * 
     * @throws IllegalAnnotationError if the option bean class is using args4j annotations incorrectly.
     */
    public CmdLineParser(final Object bean, Map<Class<?>, Class<ArgumentParser<?>>> customArgumentParsers)
            throws CmdLineException {
        // A 'return' in the constructor just skips the rest of the implementation
        // and returns the new object directly.
        if (bean == null)
            return;

        registerDefaultParsers();

        if (customArgumentParsers != null) {
            registerParsers(customArgumentParsers);
        }

        // Parse the metadata and create the setters
        new ClassParser().parse(bean, this);

    }

    public CmdLineParser(final Object bean) throws CmdLineException {
        this(bean, null);
    }

    /**
     * Programmatically defines an argument (instead of reading it from annotations like you normally do.)
     * 
     * @param setter the setter for the type
     * @param a the Argument
     */
    public <T> void addArgument(final Setter<T> setter, final Argument a) {

        final int argumentIndex = a.index();
        if (setter.isMultiValued()) {
            // A single multi-valued argument setter always goes at the end of the list,
            // and there should only be one. If multiple multi-valued setters are found, the
            // first one will win, allowing a subclass to override a String[] declared in
            // a superclass
            // TODO Do we need the 'isMultiValued()' check?
            if (argumentSetters.size() == 0
                    || !argumentSetters.get(argumentSetters.size() - 1).isMultiValued()) {
                argumentSetters.add(setter);
            }
        } else {
            if (argumentIndex < argumentSetters.size() && argumentSetters.get(argumentIndex) != null) {
                throw new IllegalAnnotationError("Argument index " + argumentIndex + " used multiple times");
            }

            // Ensure no multi-valued arguments precede the new argument
            for (int i = 0; i < argumentIndex && i < argumentSetters.size(); i++) {
                Setter<?> previousSetter = argumentSetters.get(i);
                if (previousSetter != null && previousSetter.isMultiValued()) {
                    throw new IllegalAnnotationError("Argument follows multivalued argument");
                }
            }

            if (argumentIndex >= argumentSetters.size()) {
                // make sure the argument will fit in the list
                for (int i = argumentSetters.size(); i <= argumentIndex; i++) {
                    argumentSetters.add(i, null);
                }
            }
            argumentSetters.set(argumentIndex, setter);
        }
    }

    /**
     * Programmatically defines an option (instead of reading it from annotations like you normally do.)
     * 
     * @param setter the setter for the type
     * @param o the Option
     */
    public <T> void addOption(final Setter<T> setter, final Option o) {
        checkOptionNotInMap(o.name());
        for (final String alias : o.aliases()) {
            checkOptionNotInMap(alias);
        }
        optionSetters.add(setter);
        argumentSettersByArgumentName.put(o.name(), setter);
    }

    public List<Setter<?>> argumentSetters() {
        return argumentSetters;
    }

    private void checkOptionNotInMap(final String name) throws IllegalAnnotationError {
        if (argumentSettersByArgumentName.get(name) != null) {
            throw new IllegalAnnotationError("Option name <" + name + "> is used more than once");
        }
    }

    /**
     * Parses the command line arguments and populates the bean.
     * 
     * @param args arguments to parse
     * 
     * @throws CmdLineException if there's any error parsing arguments, or if a required option is missing.
     */
    @SuppressWarnings("unchecked")
    public <T> void parseArguments(final String... args) throws CmdLineException {
        final Parameters parameters = new Parameters(args);

        final Set<Setter<?>> observedSetters = new HashSet<Setter<?>>();
        int argIndex = 0;

        while (parameters.hasNext()) {
            Setter<T> setter;
            if (parameters.peek().charAt(0) == '-') {
                final String optionName = parameters.next();
                // parse this as an option.
                setter = (Setter<T>) findOptionByName(optionName);

                if (setter == null) {
                    // TODO: insert dynamic setter processing
                    throw new CmdLineException(this, "<" + optionName + "> is not a valid option");
                }

                try {
                    setter.parseNextOperand(parameters);
                } catch (NoSuchElementException e) {
                    throw new CmdLineException(this, "Option <" + optionName + "> takes an operand");
                } catch (IllegalArgumentException e) {
                    throw new CmdLineException(this, "\"" + parameters.current()
                            + "\" is not a valid argument for " + optionName);
                }

            } else {
                if (argIndex >= argumentSetters.size()) {
                    throw new CmdLineException(this, argumentSetters.size() == 0 ? "No arguments allowed"
                            : "Too many arguments");
                }

                // known argument
                setter = (Setter<T>) argumentSetters.get(argIndex);

                // TODO This check probably interacts poorly with arguments specifying separator chars
                if (!setter.isMultiValued()) {
                    argIndex++;
                }
                try {
                    setter.parseNextArgument(parameters);
                } catch (IllegalArgumentException e) {
                    throw new CmdLineException(this, "\"" + parameters.current()
                            + "\" is not valid for argument <" + setter.parameterName() + ">");
                }
            }
            observedSetters.add(setter);
        }

        boolean suppressUsageErrors = false;
        for (final Setter<?> setter : optionSetters) {
            if (setter.option.ignoreRequired() && observedSetters.contains(setter)) {
                suppressUsageErrors = true;
            }
        }

        if (!suppressUsageErrors) {
            // make sure that all mandatory options are present
            for (final Setter<?> setter : optionSetters) {
                if (setter.option.required() && !observedSetters.contains(setter)) {
                    throw new CmdLineException(this, "Option <" + setter.parameterName() + "> is required");
                }
            }

            // make sure that all mandatory arguments are present
            for (final Setter<?> setter : argumentSetters) {
                if (setter.argument.required() && !observedSetters.contains(setter)) {
                    throw new CmdLineException(this, "Argument <" + setter.parameterName() + "> is required");
                }
            }
        }

        // Set all the discovered values onto the bean
        for (final Setter<?> setter : optionSetters) {
            setter.setValues();
        }
        for (final Setter<?> setter : argumentSetters) {
            setter.setValues();
        }
    }

    /**
     * Finds a registered {@link Setter} by its name or alias.
     * 
     * @param name name
     * @return the {@link Setter} instance or <code>null</code> if none is found
     */
    private Setter<?> findOptionByName(final String name) {
        // TODO Add set of option names
        for (final Setter<?> h : optionSetters) {
            if (name.equals(h.option.name())) {
                return h;
            }
            for (final String alias : h.option.aliases()) {
                if (name.equals(alias)) {
                    return h;
                }
            }
        }
        return null;
    }

    /**
     * Registers a user-defined {@link Parser} class with args4j. This method allows users to extend the
     * behavior of args4j by writing their own {@link Parser} implementations.
     * 
     * @param parser A {@link Parser} instance capable of parsing the specified types
     * @param argumentClasses The specified parser is used when the field/method annotated by {@link Option}
     *            is of this type.
     */
    public void registerParser(final ArgumentParser<?> parser, final Class<?>... argumentClasses) {
        for (Class<?> argumentClass : argumentClasses) {
            argumentParsers.put(argumentClass.getName(), parser);
        }
    }

    private void registerParsers(final Map<Class<?>, Class<ArgumentParser<?>>> customArgumentParsers)
            throws CmdLineException {
        for (Class<?> argumentClass : customArgumentParsers.keySet()) {
            Class<?> parserClass = customArgumentParsers.get(argumentClass);
            try {
                ArgumentParser<?> p = (ArgumentParser<?>) parserClass.getConstructor(CmdLineParser.class)
                        .newInstance(this);
                argumentParsers.put(argumentClass.getName(), p);
            } catch (Exception e) {
                throw new CmdLineException(this, "Illegal parser class: " + e.getMessage());
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> ArgumentParser<T> argumentParser(Class<T> argumentClass) {

        // enum is a special case
        if (Enum.class.isAssignableFrom(argumentClass)) {
            return new EnumParser(this, argumentClass);
        }

        if (!argumentParsers.containsKey(argumentClass.getName())) {
            throw new IllegalAnnotationError("No parser registered for type " + argumentClass.getName());
        }
        return (ArgumentParser<T>) argumentParsers.get(argumentClass.getName());
    }

    public void setUsageWidth(final int usageWidth) {
        this.usageWidth = usageWidth;
    }

    /**
     * Prints one-line usage summary.
     * 
     * @param out
     * @param includeHiddenOptions Include options marked as 'hidden'
     */
    public void printOneLineUsage(final Writer out, final boolean includeHiddenOptions) {
        PrintWriter w = new PrintWriter(out);
        for (final Setter<?> s : optionSetters) {
            if (!s.option.hidden() || includeHiddenOptions) {
                final String metaVar = s.option.metaVar().length() > 0 ? " " + s.option.metaVar() : "";
                if (s.option.required()) {
                    w.print(" <" + s.option.name() + metaVar + ">");
                } else {
                    w.print(" [" + s.option.name() + metaVar + "]");
                }
            }
        }
        for (final Setter<?> s : argumentSetters) {
            if (s.argument.required()) {
                w.print(" <" + s.argument.metaVar() + ">");
            } else {
                w.print(" [" + s.argument.metaVar() + "]");
            }
        }
        w.write('\n');
        w.flush();
    }

    /**
     * Prints the list of options and their usages to the specified {@link Writer}, optionally including any
     * hidden options.
     * 
     * @param out Writer to write to
     * @param includeHidden Include options annotated as 'hidden'
     */
    public void printUsage(final Writer out, final boolean includeHidden) {
        final PrintWriter w = new PrintWriter(out);
        // determine the length of the option + metavar first
        int len = 0;
        for (final Setter<?> s : argumentSetters) {
            final int curLen = s.prefixLen();
            len = Math.max(len, curLen);
        }
        for (final Setter<?> s : optionSetters) {
            if (!s.option.hidden() || includeHidden) {
                final int curLen = s.prefixLen();
                len = Math.max(len, curLen);
            }
        }

        // then print
        for (final Setter<?> h : argumentSetters) {
            printOption(w, h, len);
        }
        for (final Setter<?> h : optionSetters) {
            if (!h.option.hidden() || includeHidden) {
                printOption(w, h, len);
            }
        }

        w.flush();
    }

    /**
     * Prints the usage information for a single option.
     * 
     * @param out Writer to write into
     * @param setter {@link Setter} for the option
     * @param len Maximum length of metadata column
     */
    private void printOption(final PrintWriter out, final Setter<?> setter, final int len) {
        // Skip output for arguments without usage information
        if (setter.argument != null && setter.usage().length() == 0) {
            return;
        }

        // Find the width of the two columns
        final int widthMetadata = Math.min(len, (usageWidth - 4) / 2);
        final int widthUsage = usageWidth - 4 - widthMetadata;

        // Line wrapping
        final List<String> namesAndMetas = wrapLines(setter.nameAndMeta(), widthMetadata);
        final List<String> usages = wrapLines(setter.usage(), widthUsage);

        // Output
        final int outputLines = Math.max(namesAndMetas.size(), usages.size());
        for (int i = 0; i < outputLines; i++) {
            final String nameAndMeta = (i >= namesAndMetas.size()) ? "" : namesAndMetas.get(i);
            final String usage = (i >= usages.size()) ? "" : usages.get(i);
            final String format = ((nameAndMeta.length() > 0) ? " %1$-" + widthMetadata + "s : %2$-1s"
                    : " %1$-" + widthMetadata + "s   %2$-1s");

            final String output = String.format(format, nameAndMeta, usage);
            out.println(output);
        }
    }

    /**
     * Wraps a line so that the resulting parts are not longer than a given maximum length.
     * 
     * @param line Line to wrap
     * @param maxLength maximum length for the resulting parts
     * @return list of all wrapped parts
     */
    private List<String> wrapLines(final String line, final int maxLength) {
        final List<String> rv = new ArrayList<String>();
        for (String restOfLine : line.split("\\n")) {
            while (restOfLine.length() > maxLength) {
                // Wrap at space
                int lineLength;
                final String candidate = restOfLine.substring(0, maxLength);
                final int sp = candidate.lastIndexOf(' ');
                if (sp > maxLength * 3 / 4) {
                    lineLength = sp;
                } else {
                    lineLength = maxLength;
                }
                rv.add(restOfLine.substring(0, lineLength));
                restOfLine = restOfLine.substring(lineLength).trim();
            }
            rv.add(restOfLine);
        }
        return rv;
    }

    private void registerDefaultParsers() {
        registerParser(new BooleanParser(this), Boolean.class, boolean.class);

        registerParser(new ArgumentParser<Byte>() {
            @Override
            public Byte parse(String arg) throws NumberFormatException {
                return Byte.parseByte(arg);
            }
        }, Byte.class, byte.class);

        registerParser(new ArgumentParser<Character>() {
            @Override
            public Character parse(String arg) {
                if (arg.length() != 1) {
                    throw new IllegalArgumentException();
                }
                return arg.charAt(0);
            }
        }, Character.class, char.class);

        registerParser(new ArgumentParser<Double>() {
            @Override
            public Double parse(String arg) throws NumberFormatException {
                return Double.parseDouble(arg);
            }
        }, Double.class, double.class);

        registerParser(new ArgumentParser<File>() {
            @Override
            public File parse(String arg) throws NumberFormatException {
                return new File(arg);
            }
        }, File.class);

        registerParser(new ArgumentParser<Float>() {
            @Override
            public Float parse(String arg) throws NumberFormatException {
                return Float.parseFloat(arg);
            }
        }, Float.class, float.class);

        registerParser(new IntParser(), Integer.class, int.class);

        registerParser(new ArgumentParser<Long>() {
            @Override
            public Long parse(String arg) throws NumberFormatException {
                return Long.parseLong(arg);
            }
        }, Long.class, long.class);

        registerParser(new ArgumentParser<Short>() {
            @Override
            public Short parse(String arg) throws NumberFormatException {
                return Short.parseShort(arg);
            }
        }, Short.class, short.class);

        registerParser(new ArgumentParser<String>() {
            @Override
            public String parse(String s) {
                return s;
            }
        }, String.class);

        registerParser(new ArgumentParser<URI>() {
            @Override
            public URI parse(String arg) {
                try {
                    return new URI(arg);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException();
                }
            }
        }, URI.class);

        registerParser(new ArgumentParser<URL>() {
            @Override
            public URL parse(String arg) {
                try {
                    return new URL(arg);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException();
                }
            }
        }, URL.class);

        // Calendar and Date
        registerParser(new CalendarParser(), Calendar.class);
        registerParser(new ArgumentParser<Date>() {
            @Override
            public Date parse(String arg) throws IllegalArgumentException {
                return CalendarParser.parseDate(arg).getTime();
            }
        }, Date.class);
    }

}
