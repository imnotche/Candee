package me.hypinohaizin.loader.security.antidump;

import java.lang.management.ManagementFactory;
import java.util.List;

public class AntiClassSave {
    public static void check() throws Throwable {
        boolean debugClassLoadingSave;
        List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        boolean debugClassLoading = Boolean.parseBoolean(System.getProperty("legacy.debugClassLoading", "false")) || inputArguments.contains("-Dlegacy.debugClassLoading");
        boolean debugClassLoadingFiner = Boolean.parseBoolean(System.getProperty("legacy.debugClassLoadingFiner", "false")) || inputArguments.contains("-Dlegacy.debugClassLoadingFiner");
        debugClassLoadingSave = Boolean.parseBoolean(System.getProperty("legacy.debugClassLoadingSave", "false")) || inputArguments.contains("-Dlegacy.debugClassLoadingSave");
        if (debugClassLoading || debugClassLoadingFiner || debugClassLoadingSave) {
        }
    }
}

