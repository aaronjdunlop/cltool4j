package cltool4j.args4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * {@link Setter} for member {@link Field}s.
 * 
 * @author Kohsuke Kawaguchi
 * @author Aaron Dunlop
 */
public final class FieldSetter<T> extends Setter<T> {
    private final Field f;
    private final Object bean;
    private T value;

    @SuppressWarnings("unchecked")
    public FieldSetter(final Object bean, final CmdLineParser parser, final Field f) {
        super(f.getAnnotations(), (ArgumentParser<T>) parser.argumentParser(f.getType()));
        this.bean = bean;
        this.f = f;
    }

    @Override
    public boolean isMultiValued() {
        return false;
    }

    @Override
    public boolean isEnum() {
        return f.getType().isEnum();
    }

    @Override
    public void addValue(final T v) {
        this.value = v;
    }

    @Override
    protected void setValues() {
        if (value != null) {
            if (Modifier.isFinal(f.getModifiers())) {
                throw new IllegalAccessError("Cannot set final field");
            }

            try {
                f.set(bean, value);
            } catch (final IllegalAccessException e) {
                // try again
                f.setAccessible(true);
                try {
                    f.set(bean, value);
                } catch (final IllegalAccessException e2) {
                    throw new IllegalAccessError(e2.getMessage());
                }
            }
        }
    }

    @Override
    public String usage() {

        final String defaultUsage = super.usage();

        // If there is no default value, print the regular usage
        final Object defaultValue = getValue();
        if (defaultValue == null) {
            return defaultUsage;
        }

        final String defaultValueString = defaultValue.toString();

        // Don't print 'Default = false' for booleans
        if (defaultValue instanceof Boolean) {
            return defaultUsage;
        }

        // Don't print 'Default = 0' for ints, shorts, bytes, floats, doubles
        if (defaultValue instanceof Number && ((Number) defaultValue).doubleValue() == 0) {
            return defaultUsage;
        }

        // Special-case for enums; if we find we need another special-case, we should probably push this back
        // into the parser hierarchy.
        if (isEnum()) {

            @SuppressWarnings("unchecked")
            final String aliases = EnumAliasMap.singleton().usage((Class<? extends Enum<?>>) f.getType());

            // If printing enum values one-per-line, print the default value on the first line; otherwise,
            // print it at the end of the list
            if (aliases != null) {
                if (defaultValueString != null && aliases.length() > 150) {
                    return String.format("%s;   Default = %s\n%s", defaultUsage, defaultValueString, aliases);
                }
                return String.format("%s  (%s)   Default = %s", defaultUsage, aliases, defaultValueString);
            }
        }

        return String.format("%s;   Default = %s", defaultUsage, defaultValueString);
    }

    @SuppressWarnings("unchecked")
    private T getValue() {
        try {
            return (T) f.get(bean);
        } catch (final IllegalAccessException _) {
            // try again
            f.setAccessible(true);
            try {
                return (T) f.get(bean);
            } catch (final IllegalAccessException e) {
                throw new IllegalAccessError(e.getMessage());
            }
        }
    }
}
