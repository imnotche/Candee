package me.hypinohaizin.loader.security.antidump;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Inspection {
    public static void check() throws Throwable {
        String line;
        ProcessBuilder processBuilder = new ProcessBuilder();
        Process process = processBuilder.command("tasklist.exe").start();
        BufferedReader resultReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while ((line = resultReader.readLine()) != null) {
            if (!(line = line.toLowerCase()).contains("wireshark") && !line.contains("httpdebugger") && !line.contains("smartsniff") && !line.contains("everything") && !line.contains("recaf")) continue;
        }
    }
}

