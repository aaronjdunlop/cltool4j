package cltool4j;

import java.util.logging.Logger;

/**
 * Globally-accessible {@link Logger} instance for a command-line tool.
 * 
 * @author aarond
 */
public class BaseLogger extends Logger {

    private final static BaseLogger singletonInstance = new BaseLogger();

    private BaseLogger() {
        super("", null);
    }

    public static BaseLogger singleton() {
        return singletonInstance;
    }
}
