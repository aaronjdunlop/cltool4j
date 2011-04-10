package cltool4j.args4j;

import java.lang.annotation.Annotation;

/**
 * Base 'setter' class. A {@link Setter} implementation assigns values to either an object's member
 * {@link java.lang.reflect.Field}s or {@link java.lang.reflect.Method}s.
 * 
 * @author Kohsuke Kawaguchi
 * @author Aaron Dunlop
 */
public abstract class Setter<T> {

    /** {@link Option} annotation (one of {@link #argument} or {@link #option} must be set). */
    public final Option option;

    /** {@link Option} annotation (one of {@link #argument} or {@link #option} must be set). */
    public final Argument argument;

    public final ArgumentParser<T> argumentParser;

    protected Setter(final Annotation[] annotations, final ArgumentParser<T> argumentParser) {
        if (annotations[0] instanceof Option) {
            this.option = (Option) annotations[0];
            this.argument = null;
        } else {
            this.argument = (Argument) annotations[0];
            this.option = null;
        }
        this.argumentParser = argumentParser;
    }

    /**
     * Parse the operand of an option and set the bean field or method.
     * 
     * @param parameters Command-line parameters
     * @throws IllegalArgumentException if parsing is not possible
     * @throws CmdLineException
     */
    public void parseNextOperand(final Parameters parameters) throws IllegalArgumentException,
            CmdLineException {
        addValue(argumentParser.parseNextOperand(parameters));
    }

    /**
     * Parse an argument and set the bean field or method.
     * 
     * @param parameters Command-line parameters
     * @throws IllegalArgumentException if parsing is not possible
     * @throws CmdLineException
     */
    public void parseNextArgument(final Parameters parameters) throws IllegalArgumentException,
            CmdLineException {
        addValue(argumentParser.parseNextArgument(parameters));
    }

    /**
     * Adds or sets a value which will be applied to the bean after all arguments have been parsed.
     */
    protected abstract void addValue(T value) throws CmdLineException;

    /**
     * Applies discovered value(s) to the bean.
     */
    protected abstract void setValues() throws CmdLineException;

    /**
     * @return true if this setter handles arrays or collections
     */
    public abstract boolean isMultiValued();

    /**
     * @return true if the type handles by this setter is an {@link Enum} or an array or collection of
     *         {@link Enum}s
     */
    public abstract boolean isEnum();

    public final String parameterName() {

        if (argument != null) {
            return argument.metaVar() != null && argument.metaVar().length() > 0 ? argument.metaVar() : "arg"
                    + (argument.index() + 1);
        }

        // Option name
        final StringBuilder sb = new StringBuilder();
        sb.append(option.name());

        // Include any aliases (e.g. --long-usage) in parentheses
        if (option.aliases().length > 0) {
            sb.append(" (");
            for (final String alias : option.aliases()) {
                sb.append(alias);
                sb.append(',');
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(')');
        }
        return sb.toString();
    }

    public final int prefixLen() {
        final String metaVar = metaVar();
        return parameterName().length() + (metaVar != null ? (metaVar.length() + 1) + 1 : 0);
    }

    public String metaVar() {
        if (option != null && option.metaVar().length() > 0) {
            return option.metaVar();
        } else if (argument != null && argument.metaVar().length() > 0) {
            return argument.metaVar();
        }
        return argumentParser.defaultMetaVar();
    }

    public String nameAndMeta() {
        if (option != null) {
            return option.metaVar().length() > 0 ? parameterName() + " " + option.metaVar() : parameterName();
        }
        return metaVar();
    }

    public boolean required() {
        return option != null ? option.required() : argument.required();
    }

    public String usage() {
        return option != null ? option.usage() : argument.usage();
    }
}
