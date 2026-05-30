package by.presassistant;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class LauncherApp {

    private static Process serverProcess;
    private static volatile boolean stoppingIntentionally = false;
    private static volatile boolean serverRunning = false;

    private static JLabel statusLabel;
    private static JButton startBtn;
    private static JButton stopBtn;
    private static JTextField tokenField;
    private static JTextField usernameField;

    private static JLabel certStatusLabel;
    private static JLabel addinStatusLabel;
    private static JButton certBtn;
    private static JButton addinBtn;
    private static JPanel setupPanel;
    private static JFrame mainFrame;

    private static JCheckBox cmdJoinCheckBox;
    private static JCheckBox cmdSlideCheckBox;
    private static JCheckBox cmdQuestionsCheckBox;

    private static JProgressBar progressBar;
    private static JLabel logLabel;

    private static final Path CONFIG_FILE =
            Path.of(System.getProperty("user.home"), ".presassistant", "config.properties");

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        SwingUtilities.invokeLater(LauncherApp::showWindow);
    }

    private static void showWindow() {
        Properties cfg = loadConfig();
        boolean certDone  = "true".equals(cfg.getProperty("setup.cert"));
        boolean addinDone = "true".equals(cfg.getProperty("setup.addin"));

        mainFrame = new JFrame("Ассистент лектора");
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (serverProcess != null && serverProcess.isAlive()) {
                    int choice = JOptionPane.showConfirmDialog(mainFrame,
                            "Сервер запущен. Остановить и выйти?",
                            "Выход", JOptionPane.YES_NO_OPTION);
                    if (choice != JOptionPane.YES_OPTION) return;
                    serverProcess.destroy();
                }
                System.exit(0);
            }
        });
        mainFrame.setResizable(true);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(16, 20, 16, 20));

        tokenField    = new JTextField(cfg.getProperty("TELEGRAM_BOT_TOKEN", ""));
        usernameField = new JTextField(cfg.getProperty("TELEGRAM_BOT_USERNAME", ""));

        root.add(labeledRow("Токен бота:", tokenField));
        root.add(Box.createVerticalStrut(8));
        root.add(labeledRow("Имя бота:", usernameField));
        root.add(Box.createVerticalStrut(8));
        root.add(buildCommandsPanel(cfg));

        // Setup section — shown only if setup is not yet complete
        if (!certDone || !addinDone) {
            root.add(Box.createVerticalStrut(12));
            setupPanel = buildSetupPanel(certDone, addinDone);
            root.add(setupPanel);
        }

        root.add(Box.createVerticalStrut(12));

        statusLabel = new JLabel("Сервер остановлен", SwingConstants.CENTER);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(statusLabel.getFont().deriveFont(12f));
        root.add(statusLabel);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        progressBar.setVisible(false);
        root.add(Box.createVerticalStrut(4));
        root.add(progressBar);

        logLabel = new JLabel(" ", SwingConstants.CENTER);
        logLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        logLabel.setFont(logLabel.getFont().deriveFont(10f));
        logLabel.setForeground(Color.GRAY);
        logLabel.setVisible(false);
        root.add(Box.createVerticalStrut(2));
        root.add(logLabel);

        root.add(Box.createVerticalStrut(8));

        startBtn = new JButton("▶  Запустить");
        stopBtn  = new JButton("■  Остановить");
        stopBtn.setEnabled(false);
        startBtn.setPreferredSize(new Dimension(145, 32));
        stopBtn.setPreferredSize(new Dimension(145, 32));
        startBtn.addActionListener(e -> onStart());
        stopBtn.addActionListener(e -> onStop());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.add(startBtn);
        btnRow.add(stopBtn);
        root.add(btnRow);

        mainFrame.add(root);
        mainFrame.pack();
        mainFrame.setMinimumSize(new Dimension(480, mainFrame.getHeight()));
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private static JPanel buildCommandsPanel(Properties cfg) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Команды бота"));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        cmdJoinCheckBox = new JCheckBox(
                "/join — подключение к лекции по названию",
                !"false".equals(cfg.getProperty("bot.cmd.join", "true")));
        cmdSlideCheckBox = new JCheckBox(
                "/slide — запрос текущего слайда",
                !"false".equals(cfg.getProperty("bot.cmd.slide", "true")));
        cmdQuestionsCheckBox = new JCheckBox(
                "Вопросы от студентов (текстовые сообщения)",
                !"false".equals(cfg.getProperty("bot.cmd.questions", "true")));

        panel.add(cmdJoinCheckBox);
        panel.add(cmdSlideCheckBox);
        panel.add(cmdQuestionsCheckBox);
        return panel;
    }

    private static JPanel buildSetupPanel(boolean certDone, boolean addinDone) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 150, 0)),
                "Первый запуск — требуется настройка"));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (!certDone) {
            certStatusLabel = new JLabel("Сертификат не установлен");
            certStatusLabel.setForeground(new Color(180, 0, 0));
            certBtn = new JButton("Установить сертификат...");
            certBtn.addActionListener(e -> onInstallCert());
            panel.add(setupRow(certStatusLabel, certBtn));
        }

        if (!addinDone) {
            addinStatusLabel = new JLabel("Надстройка не зарегистрирована  (запустите сервер сначала)");
            addinStatusLabel.setForeground(new Color(180, 0, 0));
            addinBtn = new JButton("Зарегистрировать надстройку...");
            addinBtn.addActionListener(e -> onRegisterAddin());
            panel.add(setupRow(addinStatusLabel, addinBtn));
        }

        return panel;
    }

    private static JPanel setupRow(JLabel label, JButton btn) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(label);
        row.add(btn);
        return row;
    }

    private static void onInstallCert() {
        certBtn.setEnabled(false);
        certBtn.setText("Устанавливается...");
        new Thread(() -> {
            try {
                Path script = getAppDir().resolve("install-cert.ps1");
                // Run as admin via UAC prompt
                Process p = new ProcessBuilder("powershell.exe", "-Command",
                        "Start-Process powershell -Verb RunAs -Wait " +
                        "-ArgumentList '-ExecutionPolicy Bypass -File \"" + script + "\"'")
                        .redirectErrorStream(true).start();
                p.waitFor(60, TimeUnit.SECONDS);
                boolean ok = isCertInstalled();
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        certStatusLabel.setText("Сертификат установлен");
                        certStatusLabel.setForeground(new Color(0, 130, 0));
                        certBtn.setVisible(false);
                        saveSetupFlag("setup.cert", "true");
                        checkHideSetup();
                    } else {
                        certStatusLabel.setText("Ошибка — попробуйте ещё раз");
                        certBtn.setText("Установить сертификат...");
                        certBtn.setEnabled(true);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    certStatusLabel.setText("Ошибка: " + ex.getMessage());
                    certBtn.setText("Установить сертификат...");
                    certBtn.setEnabled(true);
                });
            }
        }, "cert-install").start();
    }

    private static void onRegisterAddin() {
        if (!serverRunning) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Сначала запустите сервер, затем нажмите эту кнопку.",
                    "Сервер не запущен", JOptionPane.WARNING_MESSAGE);
            return;
        }
        addinBtn.setEnabled(false);
        addinBtn.setText("Регистрируется...");
        new Thread(() -> {
            try {
                Path script = getAppDir().resolve("register-addin.ps1");
                Process p = new ProcessBuilder("powershell.exe",
                        "-ExecutionPolicy", "Bypass", "-File", script.toString())
                        .redirectErrorStream(true).start();
                p.waitFor(30, TimeUnit.SECONDS);
                boolean ok = p.exitValue() == 0;
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        addinStatusLabel.setText("Надстройка зарегистрирована — перезапустите PowerPoint");
                        addinStatusLabel.setForeground(new Color(0, 130, 0));
                        addinBtn.setVisible(false);
                        saveSetupFlag("setup.addin", "true");
                        checkHideSetup();
                    } else {
                        addinStatusLabel.setText("Ошибка регистрации — попробуйте ещё раз");
                        addinBtn.setText("Зарегистрировать надстройку...");
                        addinBtn.setEnabled(true);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    addinStatusLabel.setText("Ошибка: " + ex.getMessage());
                    addinBtn.setText("Зарегистрировать надстройку...");
                    addinBtn.setEnabled(true);
                });
            }
        }, "addin-register").start();
    }

    private static void checkHideSetup() {
        if (setupPanel == null) return;
        boolean certOk  = certStatusLabel  == null || certStatusLabel.getText().startsWith("Сертификат установлен");
        boolean addinOk = addinStatusLabel == null || addinStatusLabel.getText().startsWith("Надстройка зарегистрирована");
        if (certOk && addinOk) {
            setupPanel.setVisible(false);
            mainFrame.pack();
        }
    }

    private static boolean isCertInstalled() {
        try {
            Process p = new ProcessBuilder("powershell.exe", "-NonInteractive", "-Command",
                    "[bool](Get-ChildItem Cert:\\LocalMachine\\Root | Where-Object { $_.Subject -like '*PresAssistant*' })")
                    .redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor(10, TimeUnit.SECONDS);
            return "True".equalsIgnoreCase(out);
        } catch (Exception e) {
            return false;
        }
    }

    // ── Server lifecycle ───────────────────────────────────────────────────

    private static void onStart() {
        String token    = tokenField.getText().trim();
        String username = usernameField.getText().trim();
        saveConfig(token, username);

        startBtn.setEnabled(false);
        tokenField.setEnabled(false);
        usernameField.setEnabled(false);
        cmdJoinCheckBox.setEnabled(false);
        cmdSlideCheckBox.setEnabled(false);
        cmdQuestionsCheckBox.setEnabled(false);
        progressBar.setVisible(true);
        logLabel.setText(" ");
        logLabel.setVisible(true);
        mainFrame.pack();
        setStatus(Color.GRAY, "Запускается...");

        new Thread(() -> {
            try {
                Path appDir    = getAppDir();
                Path javaExe   = Path.of(System.getProperty("java.home"), "bin",
                        isWindows() ? "java.exe" : "java");
                Path serverJar = appDir.resolve("presassistant.jar");

                ProcessBuilder pb = new ProcessBuilder(
                        javaExe.toString(), "-Djava.awt.headless=true",
                        "-jar", serverJar.toString());
                pb.environment().put("TELEGRAM_BOT_TOKEN", token);
                pb.environment().put("TELEGRAM_BOT_USERNAME", username);
                List<String> enabled = new ArrayList<>();
                if (cmdJoinCheckBox.isSelected()) enabled.add("join");
                if (cmdSlideCheckBox.isSelected()) enabled.add("slide");
                if (cmdQuestionsCheckBox.isSelected()) enabled.add("questions");
                pb.environment().put("BOT_ENABLED_COMMANDS", String.join(",", enabled));
                pb.directory(appDir.toFile());
                pb.redirectErrorStream(true);

                serverProcess = pb.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(serverProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String logLine = truncate(line);
                        SwingUtilities.invokeLater(() -> logLabel.setText(logLine));
                        if (line.contains("Started PresAssistantApplication")
                                || line.contains("Tomcat started on port")) {
                            serverRunning = true;
                            SwingUtilities.invokeLater(() -> {
                                setStatus(new Color(0, 140, 0), "Сервер работает — https://localhost:8082");
                                progressBar.setVisible(false);
                                logLabel.setVisible(false);
                                mainFrame.pack();
                                stopBtn.setEnabled(true);
                            });
                        }
                    }
                }

                serverRunning = false;
                int exitCode = serverProcess.waitFor();
                if (exitCode != 0 && !stoppingIntentionally) {
                    SwingUtilities.invokeLater(() -> {
                        setStatus(Color.RED, "Сервер завершился с ошибкой (код " + exitCode + ")");
                        progressBar.setVisible(false);
                        logLabel.setVisible(false);
                        mainFrame.pack();
                        resetControls();
                    });
                }
            } catch (Exception ex) {
                serverRunning = false;
                SwingUtilities.invokeLater(() -> {
                    setStatus(Color.RED, "Ошибка запуска: " + ex.getMessage());
                    progressBar.setVisible(false);
                    logLabel.setVisible(false);
                    mainFrame.pack();
                    resetControls();
                });
            }
        }, "server-monitor").start();
    }

    private static void onStop() {
        stoppingIntentionally = true;
        stopBtn.setEnabled(false);
        setStatus(Color.GRAY, "Останавливается...");

        new Thread(() -> {
            if (serverProcess != null && serverProcess.isAlive()) {
                serverProcess.destroy();
                try {
                    if (!serverProcess.waitFor(10, TimeUnit.SECONDS))
                        serverProcess.destroyForcibly();
                } catch (InterruptedException ignored) {}
            }
            serverRunning = false;
            stoppingIntentionally = false;
            SwingUtilities.invokeLater(() -> {
                setStatus(Color.BLACK, "Сервер остановлен");
                resetControls();
            });
        }, "server-stop").start();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static JPanel labeledRow(String label, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel lbl = new JLabel(label);
        lbl.setPreferredSize(new Dimension(110, 24));
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private static void resetControls() {
        startBtn.setEnabled(true);
        tokenField.setEnabled(true);
        usernameField.setEnabled(true);
        cmdJoinCheckBox.setEnabled(true);
        cmdSlideCheckBox.setEnabled(true);
        cmdQuestionsCheckBox.setEnabled(true);
        progressBar.setVisible(false);
        logLabel.setVisible(false);
        mainFrame.pack();
    }

    private static String truncate(String line) {
        String stripped = line.replaceAll("^.*\\]\\s*", "").trim();
        String text = stripped.isEmpty() ? line.trim() : stripped;
        return text.length() > 70 ? text.substring(0, 67) + "…" : text;
    }

    private static void setStatus(Color color, String text) {
        statusLabel.setForeground(color);
        statusLabel.setText(text);
    }

    private static Path getAppDir() {
        try {
            URI loc = LauncherApp.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            return Path.of(loc).getParent();
        } catch (Exception e) {
            return Path.of(System.getProperty("user.dir"));
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static Properties loadConfig() {
        Properties p = new Properties();
        Path appDefaults = getAppDir().resolve("launcher.properties");
        if (Files.exists(appDefaults)) {
            try (InputStream in = Files.newInputStream(appDefaults)) { p.load(in); }
            catch (IOException ignored) {}
        }
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) { p.load(in); }
            catch (IOException ignored) {}
        }
        return p;
    }

    private static void saveConfig(String token, String username) {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Properties p = loadConfig();
            p.setProperty("TELEGRAM_BOT_TOKEN", token);
            p.setProperty("TELEGRAM_BOT_USERNAME", username);
            p.setProperty("bot.cmd.join",      String.valueOf(cmdJoinCheckBox.isSelected()));
            p.setProperty("bot.cmd.slide",     String.valueOf(cmdSlideCheckBox.isSelected()));
            p.setProperty("bot.cmd.questions", String.valueOf(cmdQuestionsCheckBox.isSelected()));
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) { p.store(out, null); }
        } catch (IOException ignored) {}
    }

    private static void saveSetupFlag(String key, String value) {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Properties p = loadConfig();
            p.setProperty(key, value);
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) { p.store(out, null); }
        } catch (IOException ignored) {}
    }
}
