package cltool4j.args4j;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a field or setter that receives a command line switch value.
 * 
 * <p>
 * This annotation can be placed on a field of type T or the method of the form
 * <tt>void <i>methodName</i>(T value)</tt>.
 * 
 * <p>
 * The behavior of the annotation differs depending on the type of the field or the parameter of the method.
 * All Java intrinsics and many standard classes are supported by default (see subclasses of
 * {@link ArgumentParser}). Other types can be supported with a custom {@link ArgumentParser} implementation.
 * 
 * @author Kohsuke Kawaguchi
 * @author Aaron Dunlop
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER })
public @interface Option {

    /**
     * The name of the option; e.g. "-foo" or "-bar".
     * 
     * @return The name of the option.
     */
    String name();

    /**
     * Aliases for the options, such as "--long-option-name". Multiple aliases can be specified as:
     * <code>aliases={"--alias-1", "--alias-2", "--alias-3"}.
     * 
     * @return Aliases for the options.
     */
    String[] aliases() default {};

    /**
     * Help string used to display the usage screen.
     * 
     * @return Human-readable usage description
     */
    String usage() default "";

    /**
     * The name of the operand (if any) to be displayed in usage information. e.g.,
     * 
     * <pre>
     * -x FOO  : Usage
     * </pre>
     * 
     * This parameter replaces the token 'FOO'.
     * 
     * @return A human-readable label for the option's parameter
     */
    String metaVar() default "";

    /**
     * Indicates that the option is mandatory. {@link CmdLineParser#parseArguments(String...)} will throw a
     * {@link CmdLineException} if a required option is not present.
     * 
     * See {@link Option#ignoreRequired()}
     * 
     * @return True if the option is required
     */
    boolean required() default false;

    /**
     * Specifies the {@link ArgumentParser} that processes the command line arguments. By default, the
     * {@link ArgumentParser} class will be inferred from the type of field or method annotated. If this
     * annotation element is included, it overrides the default parser inference.
     * 
     * @return {@link ArgumentParser} class
     */
    @SuppressWarnings("rawtypes")
    Class<? extends ArgumentParser> parser() default ArgumentParser.class;

    /**
     * True if the option is multi-valued. If the annotated type is an array, {@link java.util.List},
     * {@link java.util.Set}, or other implicitly multi-valued type, this defaults to true; otherwise, false.
     * 
     * TODO Remove - this is implicit in the annotated type
     * 
     * @return True if the option is multi-valued.
     * 
     */
    boolean multiValued() default false;

    /**
     * Separator to use for multi-valued parameters (e.g. -f 1,2,3). Note that multi-valued options can also
     * be specified by invoking the option multiple times (e.g. -f 1 -f 2 -f 3).
     * 
     * @return Separator to use for multi-valued parameters.
     */
    String separator() default "";

    /**
     * Annotations which must be present on the class to 'activate' this option. e.g. an option declared in a
     * superclass which only applies to subclasses with a particular annotation.
     * 
     * @return Annotations which must be present on the class to 'activate' this option.
     */
    Class<? extends Annotation>[] requiredAnnotations() default {};

    /**
     * True if this option should be hidden in the 'standard' usage display. Hidden options are generally not
     * included in the error message usage display, but are included with the detailed usage display invoked
     * with '-?' or '-help'.
     * 
     * @return True if this option should be hidden in the 'standard' usage display.
     */
    boolean hidden() default false;

    /**
     * True if this option should suppress required-option checks. Generally used on options which indicate an
     * information request from the user (e.g. -help, -readme, -version, -license) to avoid printing out an
     * error instead of the desired usage information.
     * 
     * @return True if this option should suppress required-option checks.
     */
    boolean ignoreRequired() default false;

    /**
     * The name of a resource which must be present in classpath for this option to be valid. If not present,
     * the option will be ignored and its description will be omitted from help information. Generally used on
     * options which indicate an informational request from the user which depend on a particular resource
     * (e.g. -readme, -license).
     * 
     * @return The name of a resource which must be present in classpath for this option to be valid.
     */
    String requiredResource() default "";

    /**
     * Defines a 'group' of options, one of which is required. If specified, the user must provide one (and
     * only one) of the options specifying the same group.
     * 
     * @return A 'group' of mutually exclusive options, one of which is required.
     */
    String choiceGroup() default "";

    /**
     * Defines a 'group' of options which are mutually exclusive. Similar to {@link #choiceGroup}, in that
     * only one of the options in a group is permitted, but this choice is optional (the user has the option
     * of omitting all group members).
     * 
     * @return A 'group' of mutually exclusive options.
     */
    String optionalChoiceGroup() default "";

    /**
     * @return The name of another {@link Option} which is required in conjunction with this {@link Option}.
     */
    String requires() default "";
}
