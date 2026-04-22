package me.hypinohaizin.loader.security;

import java.security.MessageDigest;

public class Hwid {
    public static String getHwid() {
        try {
            String toEncrypt = System.getenv("COMPUTERNAME") + System.getProperty("user.name") + System.getenv("PROCESSOR_IDENTIFIER") + System.getenv("PROCESSOR_LEVEL") + System.getenv("PROCESSOR_REVISION");
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(toEncrypt.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte aByteData : md.digest()) {
                String hex = Integer.toHexString(0xFF & aByteData);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            return "Error";
        }
    }
}
