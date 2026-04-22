package me.hypinohaizin.loader.security;

import me.hypinohaizin.loader.security.antidump.AntiClassSave;
import me.hypinohaizin.loader.security.antidump.AntiDebug;
import me.hypinohaizin.loader.security.antidump.Inspection;
import me.hypinohaizin.loader.security.antidump.LaunchArgs;
import me.hypinohaizin.loader.security.antidump.Presence;
import me.hypinohaizin.loader.updater.UpdateCheck;

/**
 * @author h_ypi
 * @since 2025/08/05 19:35
 */
public class Init {
    public static void init() throws Throwable {
        AntiDebug.check();
        AntiClassSave.check();
        Inspection.check();
        LaunchArgs.check();
        Presence.check();

        Auth.auth();

        UpdateCheck.check();
    }
}