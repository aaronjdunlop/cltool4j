package cltool4j;

import java.util.logging.Logger;

/**
 * Globally-accessible {@link Logger} instance for a command-line tool.
 * 
 * @author aarond
 */
public class BaseLogger {

    private final static Logger singletonInstance = Logger.getLogger("");

    public static Logger singleton() {
        return singletonInstance;
    }
}
