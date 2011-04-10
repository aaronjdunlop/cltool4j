package cltool4j;

/**
 * Singleton instance of {@link ConfigProperties} used by {@link BaseCommandlineTool} and subclasses thereof.
 * Populated via command-line arguments, a configuration property file, or properties from a
 * application-specific model file.
 * 
 * @author Aaron Dunlop
 * @since Oct 2010
 */
public class GlobalConfigProperties extends ConfigProperties {

    private final static GlobalConfigProperties singletonInstance = new GlobalConfigProperties();

    private GlobalConfigProperties() {
    }

    /**
     * @return The singleton instance of {@link GlobalConfigProperties} used by {@link BaseCommandlineTool}
     */
    public static GlobalConfigProperties singleton() {
        return singletonInstance;
    }

}
