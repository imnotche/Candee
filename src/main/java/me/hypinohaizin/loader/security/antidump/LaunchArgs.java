package me.hypinohaizin.loader.security.antidump;

import java.lang.management.ManagementFactory;

public class LaunchArgs {
    public static void check() throws Throwable {
        for (String arg : new String[]{"-XBootclasspath", "-javaagent", "-Xdebug", "-agentlib", "-Xrunjdwp", "-Xnoagent", "-verbose", "-Xverify:none", "-DproxySet", "-DproxyHost", "-DproxyPort", "-Djavax.net.ssl.trustStore", "-Djavax.net.ssl.trustStorePassword"}) {
            for (String inArg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (!inArg.contains(arg)) continue;
                throw new Throwable("Candy+ Rewrite Security | これがエラーである場合は開発者に連絡を取ってください。");
            }
        }
    }
}

