package me.hypinohaizin.loader.security.antidump;

import java.lang.management.ManagementFactory;
import java.util.List;

public class AntiDebug {

    public static void check() {
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : jvmArgs) {
            if (arg.toLowerCase().contains("jdwp") || arg.toLowerCase().contains("debug")) {
            }
        }
    }
}
