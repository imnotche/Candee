package me.hypinohaizin.loader.security;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.datatransfer.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Auth {
    private static final String BASE_URL = "http://taruki.f5.si:64911";
    private static final File APP_DIR = new File(System.getProperty("user.home"), "CandyPlusRewrite");
    private static final File DATA_FILE = new File(APP_DIR, "auth.dat");

    public static void auth() {
        String hwid = Hwid.getHwid();
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        try {
            Saved saved = loadLocal(hwid);
            String presetName = saved != null ? saved.name : null;
            CheckResult r = checkAllowJson(hwid);
            if (r.allow) {
                if (presetName == null) {
                    String nameFromServer = tryFetchNameByLogin(hwid);
                    if (nameFromServer != null) saveLocal(nameFromServer, hwid);
                }
                return;
            }
            if ("not_registered".equals(r.reason)) {
                String name = presetName != null ? presetName : promptName();
                if (name == null || name.trim().isEmpty()) {
                    showHwidCopyWindow(hwid);
                    throw new SecurityException("認証に失敗しました。強制終了します。");
                }
                if (!clientRegister(name.trim(), hwid)) {
                    String license = promptLicense(name.trim());
                    if (license == null || license.trim().isEmpty()) {
                        showHwidCopyWindow(hwid);
                        throw new SecurityException("認証に失敗しました。強制終了します。");
                    }
                    boolean pre = preregister(name.trim(), license.trim());
                    if (!pre) {
                        showHwidCopyWindow(hwid);
                        throw new SecurityException("認証に失敗しました。強制終了します。");
                    }
                    boolean reg = clientRegister(name.trim(), hwid);
                    if (!reg) {
                        showHwidCopyWindow(hwid);
                        throw new SecurityException("認証に失敗しました。強制終了します。");
                    }
                }
                saveLocal(name.trim(), hwid);
                CheckResult r2 = checkAllowJson(hwid);
                if (r2.allow) return;
                showHwidCopyWindow(hwid);
                throw new SecurityException("認証に失敗しました。強制終了します。");
            } else {
                boolean allowTxt = checkAllowTxt(hwid);
                if (allowTxt) {
                    if (presetName == null) {
                        String nameFromServer = tryFetchNameByLogin(hwid);
                        if (nameFromServer != null) saveLocal(nameFromServer, hwid);
                    }
                    return;
                }
                showHwidCopyWindow(hwid);
                throw new SecurityException("認証に失敗しました。強制終了します。");
            }
        } catch (IOException e) {
            throw new RuntimeException("認証サーバーに接続できませんでした。強制終了します。", e);
        }
    }

    private static String promptName() {
        JPanel panel = new JPanel(new BorderLayout(8,8));
        JLabel label = new JLabel("初回起動です。名前を入力してください。");
        JTextField tf = new JTextField();
        panel.add(label, BorderLayout.NORTH);
        panel.add(tf, BorderLayout.CENTER);
        int res = JOptionPane.showConfirmDialog(null, panel, "Candy+R 初回登録", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) return tf.getText();
        return null;
    }

    private static String promptLicense(String name) {
        JPanel panel = new JPanel(new BorderLayout(8,8));
        JLabel label = new JLabel("<html>事前登録が必要です。<br>受け取ったライセンスキーを入力してください。<br>名前: " + escapeHtml(name) + "</html>");
        JTextField tf = new JTextField();
        panel.add(label, BorderLayout.NORTH);
        panel.add(tf, BorderLayout.CENTER);
        int res = JOptionPane.showConfirmDialog(null, panel, "Candy+R ライセンス登録", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) return tf.getText();
        return null;
    }

    private static boolean checkAllowTxt(String hwid) throws IOException {
        URL url = new URL(BASE_URL + "/client/allow.txt?hwid=" + encode(hwid));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod("GET");
        int code = conn.getResponseCode();
        if (code != 200) return false;
        String body = readAll(conn.getInputStream()).trim();
        return "ALLOW".equals(body);
    }

    private static CheckResult checkAllowJson(String hwid) throws IOException {
        URL url = new URL(BASE_URL + "/client/allow?hwid=" + encode(hwid));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        int code = conn.getResponseCode();
        if (code != 200) return new CheckResult(false, "network");
        String body = readAll(conn.getInputStream()).trim();
        boolean allow = body.contains("\"allow\":true");
        String reason = null;
        if (!allow) {
            int i = body.indexOf("\"reason\"");
            if (i >= 0) {
                int s = body.indexOf(':', i);
                if (s >= 0) {
                    String sub = body.substring(s + 1).trim();
                    if (sub.startsWith("\"")) {
                        int e = sub.indexOf('"', 1);
                        if (e > 1) reason = sub.substring(1, e);
                    }
                }
            }
        }
        return new CheckResult(allow, reason);
    }

    private static boolean clientRegister(String name, String hwid) throws IOException {
        URL url = new URL(BASE_URL + "/client/register");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(7000);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        String json = "{\"name\":\"" + escape(name) + "\",\"hwid\":\"" + escape(hwid) + "\"}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        String body = null;
        if (code == 200) {
            body = readAll(conn.getInputStream());
            return body.contains("\"ok\":true");
        } else {
            InputStream es = conn.getErrorStream();
            if (es != null) body = readAll(es);
            if (body != null && body.contains("未事前登録")) return false;
            return false;
        }
    }

    private static boolean preregister(String name, String license) throws IOException {
        URL url = new URL(BASE_URL + "/register");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(7000);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        String json = "{\"name\":\"" + escape(name) + "\",\"license\":\"" + escape(license) + "\"}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        String body = null;
        if (code == 201 || code == 200) {
            body = readAll(conn.getInputStream());
            return body.contains("\"ok\":true");
        } else {
            InputStream es = conn.getErrorStream();
            if (es != null) body = readAll(es);
            return false;
        }
    }

    private static String tryFetchNameByLogin(String hwid) {
        try {
            URL url = new URL(BASE_URL + "/client/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(7000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            String json = "{\"hwid\":\"" + escape(hwid) + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            if (code != 200) return null;
            String body = readAll(conn.getInputStream());
            int i = body.indexOf("\"name\":\"");
            if (i < 0) return null;
            int s = i + 8;
            int e = body.indexOf('"', s);
            if (e <= s) return null;
            return body.substring(s, e);
        } catch (Exception ex) {
            return null;
        }
    }

    private static Saved loadLocal(String hwid) {
        try {
            if (!DATA_FILE.exists()) return null;
            byte[] raw = readAllBytes(DATA_FILE);
            byte[] plain = decrypt(hwid, raw);
            String text = new String(plain, StandardCharsets.UTF_8);
            String name = extractJson(text, "name");
            String fileHwid = extractJson(text, "hwid");
            if (name == null || fileHwid == null) return null;
            if (!hwid.equals(fileHwid)) return null;
            return new Saved(name, fileHwid);
        } catch (Exception e) {
            return null;
        }
    }

    private static void saveLocal(String name, String hwid) {
        try {
            if (!APP_DIR.exists()) APP_DIR.mkdirs();
            String json = "{\"name\":\"" + escape(name) + "\",\"hwid\":\"" + escape(hwid) + "\",\"ts\":" + System.currentTimeMillis() + "}";
            byte[] enc = encrypt(hwid, json.getBytes(StandardCharsets.UTF_8));
            writeAllBytes(DATA_FILE, enc);
        } catch (Exception ignored) {
        }
    }

    private static byte[] encrypt(String hwid, byte[] plain) throws Exception {
        byte[] key = deriveKey(hwid);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        byte[] ct = c.doFinal(plain);
        byte[] all = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, all, 0, iv.length);
        System.arraycopy(ct, 0, all, iv.length, ct.length);
        return Base64.getEncoder().encode(all);
    }

    private static byte[] decrypt(String hwid, byte[] encB64) throws Exception {
        byte[] all = Base64.getDecoder().decode(encB64);
        byte[] iv = new byte[12];
        byte[] ct = new byte[all.length - 12];
        System.arraycopy(all, 0, iv, 0, 12);
        System.arraycopy(all, 12, ct, 0, ct.length);
        byte[] key = deriveKey(hwid);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        return c.doFinal(ct);
    }

    private static byte[] deriveKey(String hwid) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest((hwid + ":CPR-Local-1").getBytes(StandardCharsets.UTF_8));
        byte[] k = new byte[16];
        System.arraycopy(d, 0, k, 0, 16);
        return k;
    }

    private static byte[] readAllBytes(File f) throws IOException {
        try (FileInputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int r;
            while ((r = in.read(buf)) != -1) bos.write(buf, 0, r);
            return bos.toByteArray();
        }
    }

    private static void writeAllBytes(File f, byte[] b) throws IOException {
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(b);
        }
    }

    private static String extractJson(String body, String key) {
        String pat = "\"" + key + "\":\"";
        int i = body.indexOf(pat);
        if (i < 0) return null;
        int s = i + pat.length();
        int e = body.indexOf('"', s);
        if (e <= s) return null;
        return body.substring(s, e);
    }

    private static String readAll(InputStream in) throws IOException {
        try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(r)) {
            return br.lines().collect(Collectors.joining("\n"));
        }
    }

    private static String encode(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.' || c == '~') sb.append(c);
            else {
                sb.append('%');
                sb.append(String.format("%02X", (int) c));
            }
        }
        return sb.toString();
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\\' || c == '\"') sb.append('\\');
            sb.append(c);
        }
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '<') sb.append("&lt;");
            else if (c == '>') sb.append("&gt;");
            else if (c == '&') sb.append("&amp;");
            else if (c == '"') sb.append("&quot;");
            else sb.append(c);
        }
        return sb.toString();
    }

    private static void showHwidCopyWindow(String hwid) {
        JDialog dialog = new JDialog((Frame) null, "Candy+R 認証エラー", true);
        dialog.setUndecorated(true);
        dialog.setSize(500, 260);
        dialog.setLocationRelativeTo(null);
        dialog.setAlwaysOnTop(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(180, 20, 80));
        headerPanel.setPreferredSize(new Dimension(500, 50));

        AnimatedGradientLabel titleLabel = new AnimatedGradientLabel("Candy+R 認証システム");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setVerticalAlignment(SwingConstants.CENTER);
        titleLabel.setOpaque(false);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        JButton closeButton = new JButton("X");
        closeButton.setFocusPainted(false);
        closeButton.setBorderPainted(false);
        closeButton.setBackground(new Color(180, 30, 30));
        closeButton.setForeground(Color.WHITE);
        closeButton.setPreferredSize(new Dimension(45, 45));
        closeButton.addActionListener(e -> dialog.dispose());
        headerPanel.add(closeButton, BorderLayout.EAST);

        final Point[] initialClick = {null};
        headerPanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { initialClick[0] = e.getPoint(); }
        });
        headerPanel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (initialClick[0] != null) {
                    int thisX = dialog.getLocation().x;
                    int thisY = dialog.getLocation().y;
                    int xMoved = e.getX() - initialClick[0].x;
                    int yMoved = e.getY() - initialClick[0].y;
                    dialog.setLocation(thisX + xMoved, thisY + yMoved);
                }
            }
        });

        GradientPanel contentPanel = new GradientPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel messageLabel = new JLabel("<html><div style='text-align:center;'>認証に失敗しました。<br>以下のHWIDをコピーしてDiscordのサポートまでお送りください。</div></html>");
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField hwidField = new JTextField(hwid);
        hwidField.setEditable(false);
        hwidField.setFont(new Font("Monospaced", Font.BOLD, 18));
        hwidField.setBackground(new Color(30, 30, 30));
        hwidField.setForeground(Color.WHITE);
        hwidField.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        hwidField.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel hwidPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        hwidPanel.setOpaque(false);
        hwidPanel.add(hwidField);

        JButton copyButton = new JButton("コピー");
        copyButton.setFocusPainted(false);
        copyButton.setBackground(new Color(70, 120, 200));
        copyButton.setForeground(Color.WHITE);
        copyButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        copyButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(hwid);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            JOptionPane.showMessageDialog(dialog, "HWIDをクリップボードにコピーしました。", "コピー完了", JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(copyButton);

        contentPanel.add(messageLabel, BorderLayout.NORTH);
        contentPanel.add(hwidPanel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setLayout(new BorderLayout());
        dialog.add(headerPanel, BorderLayout.NORTH);
        dialog.add(contentPanel, BorderLayout.CENTER);

        dialog.setVisible(true);
    }

    private static class GradientPanel extends JPanel {
        private float animationOffset = 0f;
        private final Timer timer;
        public GradientPanel() {
            timer = new Timer(10, e -> {
                animationOffset += 0.004f;
                if (animationOffset > 1f) animationOffset -= 1f;
                repaint();
            });
            timer.start();
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            int width = getWidth();
            int height = getHeight();
            int maxShift = 40;
            int shiftY = (int) (maxShift * Math.sin(animationOffset * 2 * Math.PI));
            Color colorStart = new Color(255, 182, 193);
            Color colorEnd = new Color(219, 112, 147);
            GradientPaint gp = new GradientPaint(0, shiftY, colorStart, 0, height - shiftY, colorEnd);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, width, height);
            g2d.dispose();
        }
    }

    private static class AnimatedGradientLabel extends JLabel {
        private float linearOffset = 0f;
        private final Timer timer;
        public AnimatedGradientLabel(String text) {
            super(text);
            setOpaque(false);
            setForeground(new Color(219, 112, 147));
            setFont(new Font("SansSerif", Font.BOLD, 28));
            timer = new Timer(10, e -> {
                linearOffset += 0.003f;
                if (linearOffset > 1f) linearOffset -= 1f;
                repaint();
            });
            timer.start();
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(getText());
            int labelWidth = getWidth();
            int gradientWidth = textWidth * 6;
            int startX = -textWidth * 3;
            int shiftX = startX + (int) (gradientWidth * linearOffset);
            float[] fractions = {0f, 0.3f, 0.5f, 0.7f, 1f};
            Color[] colors = {new Color(255, 182, 193, 0), new Color(255, 182, 193, 150), new Color(255, 182, 193, 255), new Color(219, 112, 147, 150), new Color(219, 112, 147, 0)};
            LinearGradientPaint lgp = new LinearGradientPaint(shiftX, 0, shiftX + gradientWidth, 0, fractions, colors, CycleMethod.REPEAT);
            g2d.setPaint(lgp);
            g2d.setFont(getFont());
            int x = (labelWidth - textWidth) / 2;
            int y = fm.getAscent() + 6;
            g2d.drawString(getText(), x, y);
            g2d.dispose();
        }
    }

    private static class CheckResult {
        final boolean allow;
        final String reason;
        CheckResult(boolean allow, String reason) { this.allow = allow; this.reason = reason; }
    }

    private static class Saved {
        final String name;
        final String hwid;
        Saved(String name, String hwid) { this.name = name; this.hwid = hwid; }
    }
}