package cltool4j.args4j;

/**
 * Code that parses arguments or operands of an option and populates member fields or methods.
 * 
 * This class can be extended by application to support additional Java data types in option operands.
 * 
 * Implementations of this class must be registered using
 * {@link CmdLineParser#registerParser(ArgumentParser, Class...)}
 * 
 * @param <T> The type of the field that this {@link ArgumentParser} works with.
 * 
 * @author Kohsuke Kawaguchi
 * @author Aaron Dunlop
 */
public abstract class ArgumentParser<T> {

    /**
     * @return the default meta variable name for a usage summary, or null to hide a meta variable.
     */
    public String defaultMetaVar() {
        return "arg";
    }

    /**
     * Parse the operand of an option to an object of Type <code>T<code>.
     * 
     * Note: The default implementations of {@link #parseNextOperand(Parameters)} and 
     * {@link #parseNextArgument(Parameters)} are identical, but subclasses occasionally need 
     * to implement different behavior. e.g. {@link BooleanParser}.
     * 
     * @param parameters Command-line parameters
     * @return the parsed value
     * 
     * @throws IllegalArgumentException if parsing is not possible
     */
    public T parseNextOperand(final Parameters parameters) throws IllegalArgumentException {
        return parse(parameters.next());
    }

    /**
     * Parse an argument to an object of type <code>T<code>.
     * 
     * @param parameters Command-line parameters
     * @return the parsed value
     * 
     * @throws IllegalArgumentException if parsing is not possible
     */
    public T parseNextArgument(final Parameters parameters) throws IllegalArgumentException {
        return parse(parameters.next());
    }

    /**
     * Parses a single argument to an object of type <code>T<code>.
     * 
     * @param arg String value to parse
     * @return the parsed value
     * 
     * @throws IllegalArgumentException if parsing is not possible
     */
    public abstract T parse(String arg) throws IllegalArgumentException;

}
