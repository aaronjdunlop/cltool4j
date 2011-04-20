package cltool4j.args4j;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Analyzes {@link Option} and {@link Argument} annotations in the class hierarchy. Called from
 * {@link CmdLineParser}.
 * 
 * @author Jan Materne
 */
class ClassParser {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void parse(final Object bean, final CmdLineParser parser) {

        final LinkedList<Class<?>> classHierarchy = new LinkedList<Class<?>>();

        // Process arguments working up the class hierarchy
        for (Class<?> c = bean.getClass(); c != null; c = c.getSuperclass()) {
            Argument argument;
            for (final Method m : c.getDeclaredMethods()) {
                // Skip Scala's auto-generated methods
                if (m.getName().endsWith("$eq") || m.getParameterTypes().length == 0) {
                    continue;
                }

                if ((argument = m.getAnnotation(Argument.class)) != null) {
                    // Protect against ambiguous argument ordering
                    parser.addArgument(new MethodSetter(parser, bean, m), argument);
                }
            }

            for (final Field f : c.getDeclaredFields()) {
                if ((argument = f.getAnnotation(Argument.class)) != null) {
                    parser.addArgument(createSetter(bean, parser, f), argument);
                }
            }

            // Add class to a reverse-order list so we can process options moving down the class hierarchy
            classHierarchy.addFirst(c);
        }

        // Process options working down from the top of the class hierarchy.
        for (final Class<?> c : classHierarchy) {
            Option option;
            for (final Method m : c.getDeclaredMethods()) {
                // Skip Scala's auto-generated methods
                if (m.getName().endsWith("$eq") || m.getParameterTypes().length == 0) {
                    continue;
                }

                if ((option = getOption(bean, parser, m)) != null) {
                    parser.addOption(new MethodSetter(parser, bean, m), option);
                }
            }

            for (final Field f : c.getDeclaredFields()) {
                if ((option = getOption(bean, parser, f)) != null) {
                    parser.addOption(createSetter(bean, parser, f), option);
                }
            }
        }

        checkArgumentOrdering(parser);
    }

    @SuppressWarnings("rawtypes")
    private Setter<?> createSetter(final Object bean, final CmdLineParser parser, final Field f) {

        if (List.class.isAssignableFrom(f.getType()) || Set.class.isAssignableFrom(f.getType())
                || f.getType().isArray()) {
            return new MultiValueFieldSetter(bean, parser, f);
        }

        return new FieldSetter(bean, parser, f);
    }

    private Option getOption(final Object bean, final CmdLineParser parser,
            final AccessibleObject fieldOrMethod) {
        final Option option = fieldOrMethod.getAnnotation(Option.class);
        if (option != null) {
            // Skip options which require missing class annotations
            for (int i = 0; i < option.requiredAnnotations().length; i++) {
                if (bean.getClass().getAnnotation(option.requiredAnnotations()[i]) == null) {
                    return null;
                }
            }

            // Skip options which require resources not present on CLASSPATH
            if (option.requiredResource() != null && option.requiredResource().length() > 0) {
                if (getClass().getClassLoader().getResource(option.requiredResource()) == null) {
                    return null;
                }
            }

            // Skip options which require resources not present on CLASSPATH
            if (option.requiredResource() != null && option.requiredResource().length() > 0) {
                if (getClass().getClassLoader().getResource(option.requiredResource()) == null) {
                    return null;
                }
            }
        }
        return option;
    }

    /**
     * Protects against ambiguous argument ordering (e.g., required arguments which follow optional args).
     * 
     * @param argument
     * @param parser
     * @param foundOptionalArgument
     * @return true if we have encountered an optional argument
     * @throws CmdLineException
     */
    private void checkArgumentOrdering(final CmdLineParser parser) {

        boolean foundOptionalArgument = false;
        // Argument setters are stored in an ArrayList by index, so we can iterate over them in order
        for (final Setter<?> argumentSetter : parser.argumentSetters()) {
            if (!argumentSetter.argument.required()) {
                foundOptionalArgument = true;
            } else if (foundOptionalArgument) {
                throw new IllegalAnnotationError("Required argument follows optional argument");
            }
        }
    }
}
