package cltool4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cltool4j.args4j.CmdLineParser;
import cltool4j.args4j.EnumAliasMap;
import cltool4j.args4j.Option;
import cltool4j.args4j.Setter;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;

/**
 * JavaDoc Doclet which processes JavaDoc-format comments as well as annotations to produce detailed usage
 * documentation (usually packaged into META-INF/HELP.txt by genjar2 and accessed with '-long-help')
 * 
 * @author aarond
 */
public class UsageDoclet {

    //
    // Names of elements in @Option
    //
    private static final String METAVAR_ELEMENT = "metaVar";
    private static final String NAME_ELEMENT = "name";
    private static final String USAGE_ELEMENT = "usage";
    private static final String CHOICE_GROUP_ELEMENT = "choiceGroup";
    private static final String OPTIONAL_CHOICE_GROUP_ELEMENT = "optionalChoiceGroup";
    private static final String REQUIRED_RESOURCE_ELEMENT = "requiredResource";
    private static final String REQUIRED_ANNOTATIONS_ELEMENT = "requiredAnnotations";

    // Keys for other information we may store in a map along with the elements
    private static final String JAVADOC_DOCUMENTATION = "_javadocDocumentation";
    private static final String FIELDNAME = "_fieldname";

    // Maximum line width
    private final static int USAGE_WIDTH = 80;

    public static boolean start(final RootDoc root) {

        try {
            final String outputFile = outputFile(root.options());
            final String runtimeClasspath = runtimeClasspath(root.options());
            final PrintStream out = outputFile != null ? new PrintStream(new FileOutputStream(outputFile))
                    : System.out;
            // The main root class is first - any other classes are inner classes of that class
            final ClassDoc rootClass = root.classes()[0];

            //
            // Print class-level documentation for only the target class (not any of its superclasses)
            //
            final String commentText = rootClass.commentText();
            if (commentText != null) {
                for (final String line : commentText.split("\n ?\n ?")) {
                    if (line.contains("<pre>")) {
                        out.println(line.replaceAll("</?pre>", "").replaceAll("\n *", "\n").trim());
                    } else {
                        for (final String wrappedLine : CmdLineParser.wrapLines(escapeJavadoc(line
                                .replaceAll("\n ?", " ").trim()), USAGE_WIDTH, "")) {
                            out.println(wrappedLine);
                        }
                    }
                    out.println();
                }
            }

            // Instantiate the tool - among other things, this will force classloading any enumerations and
            // populating the EnumAliasMap
            final BaseCommandlineTool toolInstance = (BaseCommandlineTool) Class.forName(
                    rootClass.qualifiedTypeName()).newInstance();

            //
            // Work down from the top of the class hierarchy, accumulating fields and methods annotated with
            // '@Option' and class-level annotations (note - BaseCommandlineTool and its subclasses treat all
            // arguments as input files, so we ignore @Argument).
            //
            final List<Map<String, String>> annotationElements = new ArrayList<Map<String, String>>();
            final HashSet<String> classAnnotations = new HashSet<String>();
            final LinkedList<ClassDoc> classHierarchy = new LinkedList<ClassDoc>();

            for (ClassDoc classDoc = rootClass; classDoc != null; classDoc = classDoc.superclass()) {
                classHierarchy.addFirst(classDoc);

                for (final AnnotationDesc annotation : classDoc.annotations()) {
                    classAnnotations.add(annotation.annotationType().toString());
                }
            }

            // Maps choice group -> a list of the options in that group. E.g. "group" -> ["-a", "-b", "-c"]
            final Map<String, List<String>> choiceGroups = new HashMap<String, List<String>>();
            // Maps optional choice group -> a list of the options in that group. E.g. "group" -> ["-a", "-b",
            // "-c"]
            final Map<String, List<String>> optionalChoiceGroups = new HashMap<String, List<String>>();

            // Name and documentation of configuration options, as defined by OPT_x fields
            final List<String[]> configOptions = new ArrayList<String[]>();

            for (final ClassDoc classDoc : classHierarchy) {
                // First, handle fields annotated with '@Option'
                for (final FieldDoc field : classDoc.fields()) {
                    final Map<String, String> annotationMap = options(field.name(), field.annotations(),
                            field.commentText(), runtimeClasspath, classAnnotations, choiceGroups,
                            optionalChoiceGroups);

                    if (annotationMap != null) {
                        annotationElements.add(annotationMap);

                    } else if (field.name().startsWith("OPT_")) {
                        configOptions.add(new String[] {
                                (String) toolInstance.getClass().getField(field.name()).get(toolInstance),
                                field.commentText() });
                    }

                }

                // Now any methods similarly annotated with '@Option'
                for (final MethodDoc method : classDoc.methods()) {
                    final Map<String, String> annotationMap = options(method.name(), method.annotations(),
                            method.commentText(), runtimeClasspath, classAnnotations, choiceGroups,
                            optionalChoiceGroups);
                    if (annotationMap == null) {
                        continue;
                    }

                    annotationElements.add(annotationMap);
                }
            }

            out.println("===== Command-line Options =====\n");
            printUsage(out, toolInstance, annotationElements, choiceGroups, optionalChoiceGroups);

            if (!configOptions.isEmpty()) {
                out.println("\n===== Configuration Options (specified with '-O') =====\n");

                for (final String[] opt : configOptions) {
                    out.println(opt[0] + '\n');
                    for (final String wrappedLine : CmdLineParser.wrapLines(
                            escapeJavadoc(opt[1].replaceAll("\n ?", " ").trim()), USAGE_WIDTH, "  ")) {
                        out.println(wrappedLine);
                    }
                    out.println();
                }
            }

            // We were successful, so return true
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int optionLength(final String option) {
        if ("-out".equals(option)) {
            return 2;
        } else if ("-runtimecp".equals(option)) {
            return 2;
        }
        return 0;
    }

    private static String outputFile(final String[][] options) {
        for (final String[] option : options) {
            if (option[0].equals("-out")) {
                return option[1];
            }
        }
        return null;
    }

    private static String runtimeClasspath(final String[][] options) {
        for (final String[] option : options) {
            if (option[0].equals("-runtimecp")) {
                return option[1];
            }
        }
        return null;
    }

    private static Map<String, String> options(final String name, final AnnotationDesc[] annotations,
            final String documentation, final String runtimeClasspath,
            final HashSet<String> classAnnotations, final Map<String, List<String>> choiceGroups,
            final Map<String, List<String>> optionalChoiceGroups) {

        // Find the '@Option' annotation. Generally, it's the only annotation, but occasionally a field or
        // method will have others
        for (final AnnotationDesc a : annotations) {
            if (Option.class.getName().equals(a.annotationType().toString())) {

                final Map<String, String> annotationElements = keyValueMap(a);
                annotationElements.put(FIELDNAME, name);

                // Handle the 'requiredResource' element - return null if a required resource is not
                // present in CLASSPATH
                final String requiredResource = annotationElements.get(REQUIRED_RESOURCE_ELEMENT);
                if (requiredResource != null && !new File(runtimeClasspath + "/" + requiredResource).exists()) {
                    return null;
                }

                // Handle the 'requiredAnnotation' element - return null if a required annotation
                // is not present
                final String requiredAnnotations = annotationElements.get(REQUIRED_ANNOTATIONS_ELEMENT);
                if (requiredAnnotations != null) {
                    for (final String ra : requiredAnnotations.replaceAll("[{} ]", "")
                            .replaceAll("\\.class", "").split(",")) {
                        if (!classAnnotations.contains(ra)) {
                            return null;
                        }
                    }
                }

                // Handle the 'choiceGroup' element - if included, add this option's name to the choice-group
                // list
                final String choiceGroup = annotationElements.get(CHOICE_GROUP_ELEMENT);
                if (choiceGroup != null) {
                    List<String> choiceGroupList = choiceGroups.get(choiceGroup);
                    if (choiceGroupList == null) {
                        choiceGroupList = new ArrayList<String>();
                        choiceGroups.put(choiceGroup, choiceGroupList);
                    }
                    choiceGroupList.add(annotationElements.get(NAME_ELEMENT));
                }

                // Similarly, the 'optionalChoiceGroup' element
                final String optionalChoiceGroup = annotationElements.get(OPTIONAL_CHOICE_GROUP_ELEMENT);
                if (optionalChoiceGroup != null) {
                    List<String> optionalChoiceGroupList = optionalChoiceGroups.get(optionalChoiceGroup);
                    if (optionalChoiceGroupList == null) {
                        optionalChoiceGroupList = new ArrayList<String>();
                        optionalChoiceGroups.put(optionalChoiceGroup, optionalChoiceGroupList);
                    }
                    optionalChoiceGroupList.add(annotationElements.get(NAME_ELEMENT));
                }

                if (documentation != null && documentation.length() > 0) {
                    annotationElements.put(JAVADOC_DOCUMENTATION, escapeJavadoc(documentation));
                }
                return annotationElements;
            }
        }
        return null;
    }

    private static String escapeJavadoc(final String javadoc) {
        String escaped = javadoc.replaceAll(" *\n *", " ");
        escaped = escaped.replaceAll("\\{@link #?(.+?)\\}", "$1");
        return escaped;
    }

    private static Map<String, String> keyValueMap(final AnnotationDesc a) {
        final HashMap<String, String> map = new HashMap<String, String>();
        for (final ElementValuePair e : a.elementValues()) {
            final String value = e.value().toString();
            map.put(e.element().name(), value.replaceAll("\"", "").replaceAll("\\\\'", "'"));
        }
        return map;
    }

    /**
     * Prints the list of options and their usages to the specified {@link Writer}, optionally including any
     * hidden options.
     * 
     * @param out Writer to write to
     * @param includeHidden Include options annotated as 'hidden'
     */
    private static void printUsage(final PrintStream out, final BaseCommandlineTool toolInstance,
            final List<Map<String, String>> optionAnnotations, final Map<String, List<String>> choiceGroups,
            final Map<String, List<String>> optionalChoiceGroups) {

        // determine the length of the option + metavar first
        int len = 0;
        for (final Map<String, String> elements : optionAnnotations) {
            len = Math.max(len, prefixLen(elements));
        }

        // then print
        for (final Iterator<Map<String, String>> i = optionAnnotations.iterator(); i.hasNext();) {
            final Map<String, String> elements = i.next();
            printOption(new PrintWriter(out), toolInstance, elements, len, choiceGroups, optionalChoiceGroups);
            // 2 blank lines between options
            if (i.hasNext()) {
                out.println('\n');
            }
        }
    }

    private static final int prefixLen(final Map<String, String> annotationElements) {
        final String metaVar = annotationElements.get(METAVAR_ELEMENT);
        return parameterName(annotationElements).length()
                + (metaVar != null ? (metaVar.length() + 1) + 1 : 0);
    }

    private static final String parameterName(final Map<String, String> annotationElements) {

        final String name = annotationElements.get(NAME_ELEMENT);
        final String aliases = annotationElements.get("aliases");

        // Option name
        final StringBuilder sb = new StringBuilder();
        sb.append(name);

        // Include any aliases (e.g. --long-usage) in parentheses
        if (aliases != null) {
            sb.append(" (");
            sb.append(aliases);
            sb.append(')');
        }
        return sb.toString();
    }

    /**
     * Prints the usage information for a single option.
     * 
     * @param out Writer to write into
     * @param setter {@link Setter} for the option
     * @param len Maximum length of metadata column
     */
    private static void printOption(final PrintWriter out, final BaseCommandlineTool toolInstance,
            final Map<String, String> annotationElements, final int len,
            final Map<String, List<String>> choiceGroups, final Map<String, List<String>> optionalChoiceGroups) {

        // Find the width of the two columns
        final int widthMetadata = Math.min(len, (USAGE_WIDTH - 4) / 2);
        final int widthUsage = USAGE_WIDTH - 4 - widthMetadata;

        // Line wrapping
        final List<String> namesAndMetas = CmdLineParser.wrapLines(nameAndMeta(annotationElements),
                widthMetadata, null);

        String usage = annotationElements.get(USAGE_ELEMENT);

        // Add a note about required option(s) here
        if ("true".equals(annotationElements.get("required"))) {
            usage += " (required option)";
        }

        usage += defaultValueString(toolInstance, annotationElements.get(FIELDNAME));

        final List<String> usageLines = CmdLineParser.wrapLines(usage, widthUsage, null);

        // Output
        final int outputLines = Math.max(namesAndMetas.size(), usageLines.size());
        for (int i = 0; i < outputLines; i++) {
            final String nameAndMeta = (i >= namesAndMetas.size()) ? "" : namesAndMetas.get(i);
            final String usageLine = (i >= usageLines.size()) ? "" : usageLines.get(i);
            final String format = ((nameAndMeta.length() > 0) ? " %1$-" + widthMetadata + "s : %2$-1s"
                    : " %1$-" + widthMetadata + "s   %2$-1s");

            final String output = String.format(format, nameAndMeta, usageLine);
            out.println(output);
        }

        // Print the detailed JavaDoc documentation on the next line
        if (annotationElements.containsKey(JAVADOC_DOCUMENTATION)) {
            out.println();
            for (final String javadocLine : CmdLineParser.wrapLines(
                    annotationElements.get(JAVADOC_DOCUMENTATION), USAGE_WIDTH, "    ")) {
                out.println(javadocLine);
            }
        }

        if (annotationElements.containsKey(CHOICE_GROUP_ELEMENT)) {
            printChoiceGroup(out, "One of ", " is required",
                    choiceGroups.get(annotationElements.get(CHOICE_GROUP_ELEMENT)));

        } else if (annotationElements.containsKey(OPTIONAL_CHOICE_GROUP_ELEMENT)) {
            printChoiceGroup(out, "Only one of ", " is permitted",
                    optionalChoiceGroups.get(annotationElements.get(OPTIONAL_CHOICE_GROUP_ELEMENT)));
        }

        out.flush();
    }

    private static void printChoiceGroup(final PrintWriter out, final String prefix, final String suffix,
            final List<String> choiceGroupMembers) {
        final StringBuilder sb = new StringBuilder();
        sb.append('\n');
        sb.append(prefix);
        for (final String option : choiceGroupMembers) {
            sb.append("<" + option + ">, ");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append(suffix);

        for (final String javadocLine : CmdLineParser.wrapLines(sb.toString(), USAGE_WIDTH, "    ")) {
            out.println(javadocLine);
        }
    }

    private static String nameAndMeta(final Map<String, String> annotationElements) {
        final String metaVar = annotationElements.get(METAVAR_ELEMENT);
        return (metaVar != null && metaVar.length() > 0) ? nameAndAliases(annotationElements) + " <"
                + metaVar + ">" : nameAndAliases(annotationElements);
    }

    private static String nameAndAliases(final Map<String, String> annotationElements) {

        // Option name
        final StringBuilder sb = new StringBuilder();
        sb.append(annotationElements.get(NAME_ELEMENT));

        // Include any aliases (e.g. --long-usage) in parentheses
        final String aliases = annotationElements.get("aliases");
        if (aliases != null) {
            sb.append(" (");
            sb.append(aliases);
            sb.append(')');
        }
        return sb.toString();
    }

    private static String defaultValueString(final BaseCommandlineTool toolInstance, final String fieldname) {

        try {
            // getField() iterates through superclasses itself, but we want to include private fields, so we
            // have to use getDeclaredField() which only looks at the current class.
            Field f = null;

            for (Class<?> c = toolInstance.getClass(); c != null; c = c.getSuperclass()) {
                try {
                    f = c.getDeclaredField(fieldname);
                    f.setAccessible(true);
                    break;
                } catch (final NoSuchFieldException ignore) {
                }
            }
            if (f == null) {
                throw new NoSuchFieldException("Unknown field " + fieldname);
            }

            final Object defaultValue = f.get(toolInstance);

            // Special-case for enums
            if (f.getType().isEnum()) {

                @SuppressWarnings("unchecked")
                final String aliases = EnumAliasMap.singleton().usage((Class<? extends Enum<?>>) f.getType());
                if (aliases != null) {

                    // Print very long lists one-value per line (with the default, if any, on the first line)
                    if (aliases.length() > 150) {
                        if (defaultValue != null) {
                            return String.format("   Default = %s\n%s", defaultValue.toString(), aliases);
                        }
                        return '\n' + aliases;
                    }

                    // Collapse shorter lists and print the default at the end
                    if (defaultValue != null) {
                        return String.format("  (%s)   Default = %s", aliases, defaultValue.toString());
                    }
                    return String.format("  (%s)", aliases);
                }
            }

            // If there is no default value, print the regular usage
            if (defaultValue == null) {
                return "";
            }

            // Don't print 'Default = false' for booleans
            if (defaultValue instanceof Boolean) {
                return "";
            }

            // Don't print 'Default = 0' for ints, shorts, bytes, floats, doubles
            if (defaultValue instanceof Number && ((Number) defaultValue).doubleValue() == 0) {
                return "";
            }

            return String.format(";   Default = %s", defaultValue.toString());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
