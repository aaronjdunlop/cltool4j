package cltool4j.args4j;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Represents an argument on the command-line. Processing of {@link Argument}s is very similar to that of
 * {@link Option}s, although not all parameters of {@link Option} are supported. The one additional parameter,
 * {@link Argument#index()}, denotes the argument's position in the command-line.
 * 
 * @author Kohsuke Kawaguchi
 * @author Mark Sinke
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER })
public @interface Argument {

    /** See {@link Option#usage()}. */
    String usage() default "";

    /** See {@link Option#metaVar()}. */
    String metaVar() default "";

    /** See {@link Option#required()}. */
    boolean required() default false;

    /** See {@link Option#parser()}. */
    @SuppressWarnings("rawtypes")
    Class<? extends ArgumentParser> parser() default ArgumentParser.class;

    /**
     * Position of the argument.<br/>
     * <br/>
     * 
     * If you define multiple single value properties to bind to arguments, they should have index=0, index=1,
     * index=2, ... and so on.<br/>
     * <br/>
     * 
     * If a multi-valued property is annotated with {@link Argument}, its index must be the highest.
     * 
     * @return Position of the argument
     */
    int index() default 0;

    /** See {@link Option#multiValued()}. */
    boolean multiValued() default false;

    /** See {@link Option#separator()}. */
    String separator() default "";
}
