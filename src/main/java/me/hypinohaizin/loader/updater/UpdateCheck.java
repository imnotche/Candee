package me.hypinohaizin.loader.updater;

import me.hypinohaizin.candyplusrewrite.CandyPlusRewrite;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UpdateCheck {
    private static final String BASE = "http://taruki.f5.si:64911";
    private static final String VERSION_URL = BASE + "/update";
    private static final String INSTALLER_TOKEN = "8a2d3f105e334f2c9c8b1f82f26af9b1";

    public static void check() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(VERSION_URL).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (!INSTALLER_TOKEN.isEmpty()) conn.setRequestProperty("x-cpr-token", INSTALLER_TOKEN);
            if (conn.getResponseCode() != 200) return;
            String json;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                json = sb.toString();
            }
            String latestVersion = extract(json, "version");
            String downloadUrl   = extract(json, "url");
            if (latestVersion != null && isNewer(latestVersion, CandyPlusRewrite.VERSION)) {
                showUpdateDialog(latestVersion, downloadUrl);
            }
        } catch (Exception ignored) {}
    }

    private static String extract(String json, String key) {
        String pat = "\"" + key + "\":\"";
        int s = json.indexOf(pat);
        if (s < 0) return null;
        s += pat.length();
        int e = json.indexOf('"', s);
        if (e < 0) return null;
        return json.substring(s, e);
    }

    private static boolean isNewer(String server, String client) {
        try {
            String[] a = server.split("\\.");
            String[] b = client.split("\\.");
            int n = Math.max(a.length, b.length);
            for (int i = 0; i < n; i++) {
                int x = (i < a.length ? Integer.parseInt(a[i]) : 0);
                int y = (i < b.length ? Integer.parseInt(b[i]) : 0);
                if (x != y) return x > y;
            }
        } catch (NumberFormatException ignored) {}
        return false;
    }

    private static void showUpdateDialog(String newVersion, String downloadUrl) {
        SwingUtilities.invokeLater(() -> {
            String msg = String.format(
                    "<html>新しいバージョン <b>%s</b> が利用可能です。<br>現在のバージョン: %s<br><br>今すぐダウンロードしますか？</html>",
                    newVersion, CandyPlusRewrite.VERSION
            );
            String[] opts = {"はい、ダウンロード", "いいえ"};
            int r = JOptionPane.showOptionDialog(null, msg, CandyPlusRewrite.NAME2 + " - アップデート",
                    JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, opts, opts[0]);
            if (r == JOptionPane.YES_OPTION && downloadUrl != null) new Thread(() -> downloadFile(newVersion, downloadUrl)).start();
        });
    }

    private static void downloadFile(String newVersion, String downloadUrl) {
        JDialog dlg = new JDialog((Frame) null, "ダウンロード中...", false);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setIndeterminate(true);
        bar.setStringPainted(true);
        bar.setString("接続中...");
        dlg.add(BorderLayout.CENTER, bar);
        dlg.add(BorderLayout.NORTH, new JLabel("Candy+R " + newVersion + " をダウンロードしています..."));
        dlg.setSize(300, 75);
        dlg.setLocationRelativeTo(null);
        dlg.setAlwaysOnTop(true);
        SwingUtilities.invokeLater(() -> dlg.setVisible(true));
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
            if (!INSTALLER_TOKEN.isEmpty()) conn.setRequestProperty("x-cpr-token", INSTALLER_TOKEN);
            int len = conn.getContentLength();
            if (len > 0) bar.setIndeterminate(false);
            File modsDir = new File("mods");
            if (!modsDir.exists()) modsDir.mkdirs();
            File outFile = new File(modsDir, "candyplusrewrite-" + newVersion + ".jar.pending");
            try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(outFile)) {
                byte[] buf = new byte[8192];
                int r;
                long read = 0;
                while ((r = in.read(buf)) != -1) {
                    out.write(buf, 0, r);
                    read += r;
                    if (len > 0) {
                        int p = (int) ((read * 100) / len);
                        bar.setValue(p);
                        bar.setString(p + "%");
                    }
                }
            }
            SwingUtilities.invokeLater(() -> {
                dlg.dispose();
                JOptionPane.showMessageDialog(null,
                        "<html>ダウンロードが完了しました。<br><br><b>" + outFile.getName() + "</b><br><br>"
                                + "mods フォルダに保存しました。<br>"
                                + "<b>次回起動前に旧jarを削除し、新ファイルの「.pending」を外してください。</b></html>",
                        "ダウンロード完了", JOptionPane.INFORMATION_MESSAGE);
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                dlg.dispose();
                JOptionPane.showMessageDialog(null, "ダウンロード中にエラー: " + e.getMessage(),
                        "ダウンロードエラー", JOptionPane.ERROR_MESSAGE);
            });
        }
    }
}