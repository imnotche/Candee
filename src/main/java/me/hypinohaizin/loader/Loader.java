package me.hypinohaizin.loader;

import me.hypinohaizin.loader.security.Init;

public final class Loader {

    private static boolean loaded = false;

    public static void load() {
        if (loaded) {
            return;
        }
        try {
            System.out.println("Starting CandyPlusRewrite security check...");
            Init.init();
            System.out.println("Security check passed.");
            loaded = true;
        } catch (Throwable t) {
            System.err.println("Security check failed for CandyPlusRewrite.");
            throw new RuntimeException("Security check failed", t);
        }
    }
}
