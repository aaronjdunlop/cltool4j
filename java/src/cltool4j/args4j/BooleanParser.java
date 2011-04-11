package cltool4j.args4j;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link ArgumentParser} for Boolean types. Handles true/false, t/f, on/off, yes/no, 1/0. All comparisons are
 * insensitive to case.
 * 
 * @author Kohsuke Kawaguchi
 */
public class BooleanParser extends ArgumentParser<Boolean> {
    private static final Map<String, Boolean> ACCEPTABLE_VALUES = new HashMap<String, Boolean>();

    static {
        ACCEPTABLE_VALUES.put("true", Boolean.TRUE);
        ACCEPTABLE_VALUES.put("false", Boolean.FALSE);
        ACCEPTABLE_VALUES.put("t", Boolean.TRUE);
        ACCEPTABLE_VALUES.put("f", Boolean.FALSE);
        ACCEPTABLE_VALUES.put("on", Boolean.TRUE);
        ACCEPTABLE_VALUES.put("off", Boolean.FALSE);
        ACCEPTABLE_VALUES.put("yes", Boolean.TRUE);
        ACCEPTABLE_VALUES.put("no", Boolean.FALSE);
        ACCEPTABLE_VALUES.put("1", Boolean.TRUE);
        ACCEPTABLE_VALUES.put("0", Boolean.FALSE);
    }

    public BooleanParser(final CmdLineParser parser) {
        super();
    }

    @Override
    public Boolean parseNextOperand(final Parameters parameters) throws IllegalArgumentException {
        // If we're 'parsing' a boolean option, don't consume another argument
        return true;
    }

    @Override
    public Boolean parse(final String arg) throws IllegalArgumentException {

        if (!ACCEPTABLE_VALUES.containsKey(arg.toLowerCase())) {
            throw new IllegalArgumentException();
        }
        return ACCEPTABLE_VALUES.get(arg.toLowerCase());
    }

    @Override
    public String defaultMetaVar() {
        return null;
    }
}
