package cltool4j.args4j;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Maps aliases to {@link java.lang.Enum} values. This allows use of long (and descriptive) enumeration names
 * in code and shorter versions for use on the command-line.<br/>
 * <br/>
 * 
 * If aliases are mapped for an enum, they will be considered equivalent to the full enum name on the
 * command-line and will be displayed along with alias names in usage information.<br/>
 * <br/>
 * 
 * Note: Enumeration and alias parsing is always case-insensitive.<br/>
 * <br/>
 * 
 * Example usage:<br/>
 * 
 * <pre>
 * public enum E {
 *     // Accept 'a', 'b', and 'optionb' as well as the enumerations themselves
 *     VeryLongOptionNameA(&quot;a&quot;), VeryLongOptionNameB(&quot;b&quot;, &quot;optionb&quot;);
 * 
 *     private E(final String... aliases) {
 *         EnumAliasMap.singleton().addAliases(this, aliases);
 *     }
 * }
 * </pre>
 * 
 * @author Aaron Dunlop
 */
public final class EnumAliasMap {

    private final static EnumAliasMap singletonInstance = new EnumAliasMap();

    private final HashMap<Class<? extends Enum<?>>, HashMap<String, Enum<?>>> classToAliasMaps = new HashMap<Class<? extends Enum<?>>, HashMap<String, Enum<?>>>();

    private final HashMap<Class<? extends Enum<?>>, LinkedList<String>> classToAliasLists = new HashMap<Class<? extends Enum<?>>, LinkedList<String>>();

    private EnumAliasMap() {
    }

    public static EnumAliasMap singleton() {
        return singletonInstance;
    }

    @SuppressWarnings("unchecked")
    public void addAliases(final Enum<?> e, final String... aliases) {
        HashMap<String, Enum<?>> aliasMap = classToAliasMaps.get(e.getClass());
        LinkedList<String> aliasList = classToAliasLists.get(e.getClass());

        if (aliasMap == null) {
            aliasMap = new HashMap<String, Enum<?>>();
            aliasList = new LinkedList<String>();
            classToAliasMaps.put((Class<? extends Enum<?>>) e.getClass(), aliasMap);
            classToAliasLists.put((Class<? extends Enum<?>>) e.getClass(), aliasList);
        }

        // Map the lowercased version of the enum's value
        aliasMap.put(e.toString().toLowerCase(), e);
        aliasList.add(e.toString());

        // And all the aliases
        for (final String alias : aliases) {
            // Skip 'aliases' which are the same as the enum name
            if (alias.equals(e.toString().toLowerCase())) {
                continue;
            }

            // Map the alias
            if (aliasMap.put(alias, e) != null) {
                throw new IllegalAnnotationError("Alias '" + alias + "' used more than once for enum "
                        + e.getClass());
            }
            aliasList.add(alias);
        }
        // Add null delimiter
        aliasList.add(null);
    }

    /**
     * @param enumClass
     * @return A string representation of the legal command-line parameters for the specified {@link Enum}
     */
    public String usage(final Class<? extends Enum<?>> enumClass) {
        final LinkedList<String> aliasList = classToAliasLists.get(enumClass);
        if (aliasList == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(128);
        for (final Iterator<String> i = aliasList.iterator(); i.hasNext();) {
            final String alias = i.next();
            if (alias == null) {
                sb.setCharAt(sb.length() - 1, ';');
                sb.append(' ');
            } else {
                sb.append(alias);
                if (i.hasNext()) {
                    sb.append(',');
                }
            }
        }
        sb.delete(sb.length() - 2, sb.length());

        // Print very long lists of enum options as one-per-line
        if (sb.length() > 150) {
            sb = new StringBuilder(512);
            sb.append("  ");
            for (final String alias : aliasList) {
                if (alias == null) {
                    sb.setCharAt(sb.length() - 1, '\n');
                    sb.append("  ");
                } else {
                    sb.append(alias);
                    sb.append(',');
                }
            }
            sb.delete(sb.length() - 2, sb.length());
        }

        return sb.toString();
    }

    public Enum<?> forString(final Class<? extends Enum<?>> enumClass, final String alias) {
        final HashMap<String, Enum<?>> aliasMap = classToAliasMaps.get(enumClass);
        if (aliasMap == null) {
            return null;
        }
        return aliasMap.get(alias.toLowerCase());
    }
}
