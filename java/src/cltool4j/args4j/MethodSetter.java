package cltool4j.args4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * {@link Setter} that sets to a {@link Method}.
 * 
 * TODO Support multi-valued method setters
 * 
 * @author Kohsuke Kawaguchi
 */
public final class MethodSetter<T> extends Setter<T> {

    private final Object bean;
    private final Method m;
    private T value;

    @SuppressWarnings("unchecked")
    public MethodSetter(final CmdLineParser parser, final Object bean, final Method m) {
        super(m.getAnnotations(), (ArgumentParser<T>) parser.argumentParser(m.getParameterTypes()[0]));

        this.bean = bean;
        this.m = m;
        if (m.getParameterTypes().length != 1) {
            throw new IllegalAnnotationError("Method " + m.getName() + " takes more than one parameter");
        }
    }

    @Override
    public void addValue(final T v) {
        this.value = v;
    }

    @Override
    protected void setValues() throws CmdLineException {
        if (value != null) {
            try {
                try {
                    m.invoke(bean, value);
                } catch (final IllegalAccessException _) {
                    // try again
                    m.setAccessible(true);
                    try {
                        m.invoke(bean, value);
                    } catch (final IllegalAccessException e) {
                        throw new IllegalAccessError(e.getMessage());
                    }
                }
            } catch (final InvocationTargetException e) {

                final Throwable t = e.getTargetException();
                if (t != null) {
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    } else if (t instanceof Error) {
                        throw (Error) t;
                    }
                    // otherwise wrap
                    throw new CmdLineException(t);
                }

                throw new CmdLineException(e);
            }

        }
    }

    @Override
    public boolean isMultiValued() {
        return false;
    }

    @Override
    public boolean isEnum() {
        return m.getParameterTypes()[0].isEnum();
    }

}
