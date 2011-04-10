package cltool4j.args4j;

import java.lang.reflect.Method;

/**
 * {@link Enum} {@link ArgumentParser}.
 * 
 * @author Kohsuke Kawaguchi
 * @author Aaron Dunlop
 */
public class EnumParser<T extends Enum<T>> extends ArgumentParser<T> {

    private final Class<T> enumType;

    public EnumParser(final CmdLineParser parser, final Class<T> enumType) {
        super();
        this.enumType = enumType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T parse(final String arg) throws IllegalArgumentException {

        try {
            if (Enum.valueOf(enumType, arg) != null) {
                return Enum.valueOf(enumType, arg);
            }
        } catch (final IllegalArgumentException e) {
        }

        // Try case-insensitive exact match
        final String lowercase = arg.toLowerCase();
        for (final T o : enumType.getEnumConstants()) {
            if (o.name().toLowerCase().equals(lowercase)) {
                return o;
            }
        }

        // Try EnumAliasMap
        if (EnumAliasMap.singleton().forString(enumType, arg) != null) {
            return (T) EnumAliasMap.singleton().forString(enumType, arg);
        }

        // Now try the enum's own 'forString()' method, if it has one
        try {
            final Method forStringMethod = enumType.getMethod("forString", new Class[] { String.class });
            return (T) forStringMethod.invoke(enumType, new Object[] { arg });
        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
