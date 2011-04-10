package cltool4j;

import java.util.logging.Logger;

/**
 * Globally-accessible {@link Logger} instance for a command-line tool. Calling {@link BaseLogger#singleton()}
 * is a convenience method equivalent to <code>Logger#getLogger("")</code>, but perhaps easier to remember.
 * 
 * @author Aaron Dunlop
 */
public class BaseLogger {

    private final static Logger singletonInstance = Logger.getLogger("");

    public static Logger singleton() {
        return singletonInstance;
    }
}
