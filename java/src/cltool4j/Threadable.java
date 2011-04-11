package cltool4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a command-line tool is threadable.
 * 
 * @author Aaron Dunlop
 * @since Jan 10, 2009
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Inherited
public @interface Threadable {

    /**
     * The number of threads started by default. If unspecified, the thread count will default to the number
     * of CPUs on the machine. This option is generally used to limit the default to a lower number.
     * 
     * The default thread count can be overridden on the command-line with the -xt option.
     * 
     * @return The number of threads to start by default for this class.
     */
    public int defaultThreads() default 0;
}
