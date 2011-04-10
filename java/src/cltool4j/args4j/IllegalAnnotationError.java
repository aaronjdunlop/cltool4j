package cltool4j.args4j;

/**
 * Signals an incorrect use of args4j annotations. Examples include repeated usage of the same option name or
 * alias, a required {@link Argument} following an optional {@link Argument}, annotating a method which takes
 * multiple parameters, etc.<br/>
 * <br/>
 * 
 * These conditions indicate misuse of annotations, rather than incorrect command-line arguments, so this
 * class extends {@link Error}.
 * 
 * @author Kohsuke Kawaguchi
 */
public class IllegalAnnotationError extends Error {
    private static final long serialVersionUID = 2397757838147693218L;

    public IllegalAnnotationError(final String message) {
        super(message);
    }

    public IllegalAnnotationError(final String message, final Throwable cause) {
        super(message, cause);
    }

    public IllegalAnnotationError(final Throwable cause) {
        super(cause);
    }
}
