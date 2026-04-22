package me.hypinohaizin.loader.security.antidump;

/**
 * @author h_ypi
 * @since 2025/08/16 6:43
 */
public class Presence {
    public static void check() throws Throwable {
        try {
            Class.forName("me.hypinohaizin.candyplusrewrite.CandyPlusRewrite");
        } catch (Throwable t) {
        }
    }
}