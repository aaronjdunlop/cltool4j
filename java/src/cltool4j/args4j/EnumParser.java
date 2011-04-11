package cltool4j.args4j;

import java.lang.reflect.Method;

/**
 * {@link ArgumentParser} for enumerated types. For ease of use on the command-line, enumerations are parsed
 * case-insensitively. This could cause conflicts if an <code>enum</code> class declares enumerations
 * differing only in case. In this case, the exact (case-sensitive) comparison will be performed first. If no
 * case-sensitive match is found, the first matching enumeration (lowest ordinal value) will be returned.
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

    /**
     * For ease of use on the command-line, enumerations are parsed case-insensitively. This could cause
     * conflicts if an <code>enum</code> class declares enumerations differing only in case. In this case, the
     * exact (case-sensitive) comparison will be performed first. If no case-sensitive match is found, the
     * first matching enumeration (lowest ordinal value) will be returned.
     * 
     * If no match is found, we then check the {@link EnumAliasMap} for a matching alias, and finally attempt
     * the enum's <code>forString()</code> method (if present).
     */
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
