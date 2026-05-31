package by.presassistant;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class LauncherApp {

    private static final Color ACCENT      = new Color(0x63, 0x2e, 0x9e);
    private static final Color ACCENT_DARK = new Color(0x50, 0x18, 0x80);
    private static final Color GREEN       = new Color(0x2e, 0x7d, 0x32);
    private static final Color RED         = new Color(0xc6, 0x28, 0x28);
    private static final Color GRAY_TEXT   = new Color(0x66, 0x66, 0x66);

    private static final String DEV_REG_KEY  = "HKCU\\SOFTWARE\\Microsoft\\Office\\16.0\\WEF\\Developer";
    private static final String DEV_REG_NAME = "8c75a7c2-47aa-4b16-b0d8-be3e0c982485";

    private static Process          serverProcess         = null;
    private static Process          devServerProcess      = null;
    private static volatile boolean stoppingIntentionally = false;
    private static volatile boolean serverRunning         = false;

    private static JFrame       mainFrame;
    private static JLabel       statusDot;
    private static JLabel       statusLabel;
    private static JProgressBar progressBar;
    private static JLabel       logLabel;
    private static JButton      startBtn;
    private static JButton      stopBtn;
    private static JButton      openFileBtn;

    private static JPasswordField tokenField;
    private static JButton        tokenToggleBtn;
    private static JTextField     usernameField;
    private static JCheckBox      cmdJoinBox;
    private static JCheckBox      cmdSlideBox;
    private static JCheckBox      cmdQuestionsBox;

    private static JCheckBox  devModeBox;
    private static JTextField addinDirField;
    private static JLabel     devStatusLabel;

    private static JLabel  certStatusLabel;
    private static JButton certBtn;
    private static JLabel  addinStatusLabel;
    private static JButton addinBtn;

    private static final Path CONFIG_FILE =
            Path.of(System.getProperty("user.home"), ".presassistant", "config.properties");

    // ── Entry point ───────────────────────────────────────────────────────

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        setupLaf();
        SwingUtilities.invokeLater(LauncherApp::buildWindow);
    }

    private static void setupLaf() {
        try {
            FlatLightLaf.setup();
            UIManager.put("Component.focusWidth",           1);
            UIManager.put("Component.arc",                  6);
            UIManager.put("Button.arc",                     6);
            UIManager.put("TextComponent.arc",              6);
            UIManager.put("TabbedPane.selectedBackground",  ACCENT);
            UIManager.put("TabbedPane.selectedForeground",  Color.WHITE);
            UIManager.put("TabbedPane.underlineColor",      ACCENT);
            UIManager.put("TabbedPane.hoverColor",          new Color(0xf3, 0xea, 0xff));
            UIManager.put("TabbedPane.focusColor",          new Color(0xf3, 0xea, 0xff));
            UIManager.put("CheckBox.icon.selectedColor",    ACCENT);
            UIManager.put("CheckBox.icon.checkmarkColor",   Color.WHITE);
            UIManager.put("ProgressBar.foreground",         ACCENT);
            FlatLaf.updateUI();
        } catch (Exception ignored) {}
    }

    // ── Window ────────────────────────────────────────────────────────────

    private static void buildWindow() {
        Properties cfg = loadConfig();

        mainFrame = new JFrame("Ассистент лектора");
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                if (serverProcess != null && serverProcess.isAlive()) {
                    int r = JOptionPane.showConfirmDialog(mainFrame,
                            "Сервер запущен. Остановить и выйти?",
                            "Выход", JOptionPane.YES_NO_OPTION);
                    if (r != JOptionPane.YES_OPTION) return;
                    stopAll();
                }
                System.exit(0);
            }
        });

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(tabs.getFont().deriveFont(12f));
        tabs.addTab("  Управление  ", buildServerTab());
        tabs.addTab("  Бот  ",        buildBotTab(cfg));
        tabs.addTab("  Установка  ",  buildSetupTab(cfg));

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);

        mainFrame.add(root);
        mainFrame.setMinimumSize(new Dimension(500, 440));
        mainFrame.pack();
        mainFrame.setSize(520, 490);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private static JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ACCENT);
        header.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel title = new JLabel("Ассистент лектора");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        JLabel subtitle = new JLabel("Управление сервером и ботом");
        subtitle.setForeground(new Color(255, 255, 255, 180));
        subtitle.setFont(subtitle.getFont().deriveFont(11f));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(title);
        text.add(Box.createVerticalStrut(2));
        text.add(subtitle);
        header.add(text, BorderLayout.WEST);
        return header;
    }

    // ── Tab: Управление ───────────────────────────────────────────────────

    private static JPanel buildServerTab() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statusRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusDot = new JLabel("●");
        statusDot.setFont(statusDot.getFont().deriveFont(16f));
        statusDot.setForeground(Color.LIGHT_GRAY);
        statusLabel = new JLabel("Сервер остановлен");
        statusLabel.setFont(statusLabel.getFont().deriveFont(13f));
        statusRow.add(statusDot);
        statusRow.add(statusLabel);
        p.add(statusRow);
        p.add(Box.createVerticalStrut(6));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        progressBar.setVisible(false);
        p.add(progressBar);

        logLabel = new JLabel(" ");
        logLabel.setFont(logLabel.getFont().deriveFont(10f));
        logLabel.setForeground(GRAY_TEXT);
        logLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        logLabel.setVisible(false);
        p.add(Box.createVerticalStrut(2));
        p.add(logLabel);
        p.add(Box.createVerticalStrut(16));

        startBtn = accentButton("▶  Запустить");
        stopBtn  = outlineButton("■  Остановить");
        stopBtn.setEnabled(false);
        startBtn.setPreferredSize(new Dimension(150, 34));
        stopBtn.setPreferredSize(new Dimension(150, 34));
        startBtn.addActionListener(e -> onStart());
        stopBtn.addActionListener(e -> onStop());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.add(startBtn);
        btnRow.add(stopBtn);
        p.add(btnRow);
        p.add(Box.createVerticalStrut(20));

        JSeparator sep = new JSeparator();
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        p.add(sep);
        p.add(Box.createVerticalStrut(20));

        JLabel openLabel = new JLabel("Открыть презентацию в PowerPoint");
        openLabel.setFont(openLabel.getFont().deriveFont(Font.BOLD, 12f));
        openLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(openLabel);
        p.add(Box.createVerticalStrut(4));

        JLabel openHint = new JLabel("<html>Сервер должен быть запущен.<br>" +
                "Файл откроется в PowerPoint — надстройка доступна через Insert → My Add-ins.</html>");
        openHint.setFont(openHint.getFont().deriveFont(11f));
        openHint.setForeground(GRAY_TEXT);
        openHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(openHint);
        p.add(Box.createVerticalStrut(10));

        openFileBtn = outlineButton("📂  Выбрать файл...");
        openFileBtn.setPreferredSize(new Dimension(180, 34));
        openFileBtn.setEnabled(false);
        openFileBtn.addActionListener(e -> onOpenFile());
        JPanel openRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        openRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        openRow.add(openFileBtn);
        p.add(openRow);

        return p;
    }

    // ── Tab: Бот ──────────────────────────────────────────────────────────

    private static JPanel buildBotTab(Properties cfg) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(20, 24, 20, 24));

        sectionLabel(p, "Токен бота");
        p.add(Box.createVerticalStrut(6));

        tokenField = new JPasswordField(cfg.getProperty("TELEGRAM_BOT_TOKEN", ""));
        tokenField.setEchoChar('●');
        tokenField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tokenField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        tokenField.setAlignmentX(Component.LEFT_ALIGNMENT);

        tokenToggleBtn = new JButton("👁");
        tokenToggleBtn.setPreferredSize(new Dimension(36, 30));
        tokenToggleBtn.addActionListener(e -> {
            if (tokenField.getEchoChar() == 0) { tokenField.setEchoChar('●'); tokenToggleBtn.setText("👁"); }
            else { tokenField.setEchoChar((char) 0); tokenToggleBtn.setText("🙈"); }
        });

        JPanel tokenRow = new JPanel(new BorderLayout(6, 0));
        tokenRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        tokenRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        tokenRow.add(tokenField, BorderLayout.CENTER);
        tokenRow.add(tokenToggleBtn, BorderLayout.EAST);
        p.add(tokenRow);
        p.add(Box.createVerticalStrut(14));

        sectionLabel(p, "Имя бота (@username)");
        p.add(Box.createVerticalStrut(6));
        usernameField = new JTextField(cfg.getProperty("TELEGRAM_BOT_USERNAME", ""));
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(usernameField);
        p.add(Box.createVerticalStrut(18));

        sectionLabel(p, "Доступные команды");
        p.add(Box.createVerticalStrut(8));
        cmdJoinBox = styledCheckbox("/join — подключение к лекции по названию",
                !"false".equals(cfg.getProperty("bot.cmd.join", "true")));
        cmdSlideBox = styledCheckbox("/slide — запрос текущего слайда",
                !"false".equals(cfg.getProperty("bot.cmd.slide", "true")));
        cmdQuestionsBox = styledCheckbox("Вопросы от студентов (текстовые сообщения)",
                !"false".equals(cfg.getProperty("bot.cmd.questions", "true")));
        p.add(cmdJoinBox);
        p.add(Box.createVerticalStrut(4));
        p.add(cmdSlideBox);
        p.add(Box.createVerticalStrut(4));
        p.add(cmdQuestionsBox);
        p.add(Box.createVerticalStrut(18));

        // ── Dev mode ──
        JSeparator sep = new JSeparator();
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        p.add(sep);
        p.add(Box.createVerticalStrut(14));

        sectionLabel(p, "Режим разработки");
        p.add(Box.createVerticalStrut(6));

        devModeBox = styledCheckbox("Запускать webpack dev-server (надстройка на порту 3000)",
                "true".equals(cfg.getProperty("dev.mode", "false")));
        p.add(devModeBox);
        p.add(Box.createVerticalStrut(8));

        JLabel dirLabel = new JLabel("Папка pres-assistant-addin:");
        dirLabel.setFont(dirLabel.getFont().deriveFont(11f));
        dirLabel.setForeground(GRAY_TEXT);
        dirLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(dirLabel);
        p.add(Box.createVerticalStrut(4));

        String defaultAddinDir = cfg.getProperty("dev.addin.dir", detectAddinDir());
        addinDirField = new JTextField(defaultAddinDir);
        addinDirField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        addinDirField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton browseBtn = outlineButton("...");
        browseBtn.setPreferredSize(new Dimension(36, 30));
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(addinDirField.getText());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setDialogTitle("Выберите папку pres-assistant-addin");
            if (fc.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION)
                addinDirField.setText(fc.getSelectedFile().getAbsolutePath());
        });

        JPanel dirRow = new JPanel(new BorderLayout(6, 0));
        dirRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        dirRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        dirRow.add(addinDirField, BorderLayout.CENTER);
        dirRow.add(browseBtn, BorderLayout.EAST);
        p.add(dirRow);
        p.add(Box.createVerticalStrut(8));

        devStatusLabel = new JLabel(" ");
        devStatusLabel.setFont(devStatusLabel.getFont().deriveFont(11f));
        devStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(devStatusLabel);

        p.add(Box.createVerticalGlue());
        p.add(Box.createVerticalStrut(10));
        JLabel saveHint = new JLabel("Настройки сохраняются при запуске сервера.");
        saveHint.setFont(saveHint.getFont().deriveFont(10f));
        saveHint.setForeground(GRAY_TEXT);
        saveHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(saveHint);

        return p;
    }

    // ── Tab: Установка ────────────────────────────────────────────────────

    private static JPanel buildSetupTab(Properties cfg) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(20, 24, 20, 24));

        boolean certDone  = "true".equals(cfg.getProperty("setup.cert"));
        boolean addinDone = "true".equals(cfg.getProperty("setup.addin"));

        sectionLabel(p, "Шаг 1 — SSL-сертификат");
        p.add(Box.createVerticalStrut(4));
        JLabel certHint = new JLabel("Требуется для HTTPS-соединения надстройки с сервером.");
        certHint.setFont(certHint.getFont().deriveFont(11f));
        certHint.setForeground(GRAY_TEXT);
        certHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(certHint);
        p.add(Box.createVerticalStrut(8));
        certStatusLabel = statusIcon(certDone ? "Сертификат установлен" : "Не установлен", certDone);
        p.add(certStatusRow(certStatusLabel, certDone));
        p.add(Box.createVerticalStrut(20));

        sectionLabel(p, "Шаг 2 — Регистрация надстройки");
        p.add(Box.createVerticalStrut(4));
        JLabel addinHint = new JLabel("<html>Регистрирует надстройку для всех версий PowerPoint.<br>" +
                "Требует запущенного сервера. После регистрации перезапустите PowerPoint.</html>");
        addinHint.setFont(addinHint.getFont().deriveFont(11f));
        addinHint.setForeground(GRAY_TEXT);
        addinHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(addinHint);
        p.add(Box.createVerticalStrut(8));
        addinStatusLabel = statusIcon(addinDone ? "Надстройка зарегистрирована" : "Не зарегистрирована", addinDone);
        p.add(addinStatusRow(addinStatusLabel, addinDone));

        p.add(Box.createVerticalGlue());
        return p;
    }

    private static JPanel certStatusRow(JLabel lbl, boolean done) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(lbl);
        if (!done) {
            certBtn = outlineButton("Установить...");
            certBtn.addActionListener(e -> onInstallCert());
            row.add(certBtn);
        }
        return row;
    }

    private static JPanel addinStatusRow(JLabel lbl, boolean done) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(lbl);
        addinBtn = outlineButton(done ? "Перерегистрировать" : "Зарегистрировать...");
        addinBtn.addActionListener(e -> onRegisterAddin());
        row.add(addinBtn);
        return row;
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private static void onStart() {
        String token    = new String(tokenField.getPassword()).trim();
        String username = usernameField.getText().trim();
        if (token.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Введите токен бота на вкладке «Бот».",
                    "Токен не задан", JOptionPane.WARNING_MESSAGE);
            return;
        }
        saveConfig(token, username);

        startBtn.setEnabled(false);
        tokenField.setEnabled(false);
        usernameField.setEnabled(false);
        cmdJoinBox.setEnabled(false);
        cmdSlideBox.setEnabled(false);
        cmdQuestionsBox.setEnabled(false);
        devModeBox.setEnabled(false);
        addinDirField.setEnabled(false);
        progressBar.setVisible(true);
        logLabel.setText(" ");
        logLabel.setVisible(true);
        setStatus(Color.LIGHT_GRAY, "Запускается...");

        new Thread(() -> {
            try {
                Path appDir    = getAppDir();
                Path javaExe   = Path.of(System.getProperty("java.home"), "bin",
                        isWindows() ? "java.exe" : "java");
                Path serverJar = appDir.resolve("presassistant.jar");

                List<String> enabled = new ArrayList<>();
                if (cmdJoinBox.isSelected())      enabled.add("join");
                if (cmdSlideBox.isSelected())     enabled.add("slide");
                if (cmdQuestionsBox.isSelected()) enabled.add("questions");

                ProcessBuilder pb = new ProcessBuilder(
                        javaExe.toString(), "-Djava.awt.headless=true",
                        "-jar", serverJar.toString());
                pb.environment().put("TELEGRAM_BOT_TOKEN",    token);
                pb.environment().put("TELEGRAM_BOT_USERNAME", username);
                pb.environment().put("BOT_ENABLED_COMMANDS",  String.join(",", enabled));
                pb.directory(appDir.toFile());
                pb.redirectErrorStream(true);

                serverProcess = pb.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(serverProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String log = truncate(line);
                        SwingUtilities.invokeLater(() -> logLabel.setText(log));
                        if (line.contains("Started PresAssistantApplication")
                                || line.contains("Tomcat started on port")) {
                            serverRunning = true;
                            SwingUtilities.invokeLater(() -> {
                                setStatus(GREEN, "Сервер работает — https://localhost:8082");
                                progressBar.setVisible(false);
                                logLabel.setVisible(false);
                                stopBtn.setEnabled(true);
                                openFileBtn.setEnabled(true);
                                if (devModeBox.isSelected()) startDevServer();
                            });
                        }
                    }
                }

                serverRunning = false;
                int code = serverProcess.waitFor();
                if (code != 0 && !stoppingIntentionally) {
                    SwingUtilities.invokeLater(() -> {
                        setStatus(RED, "Сервер завершился с ошибкой (код " + code + ")");
                        progressBar.setVisible(false);
                        logLabel.setVisible(false);
                        resetControls();
                    });
                }
            } catch (Exception ex) {
                serverRunning = false;
                SwingUtilities.invokeLater(() -> {
                    setStatus(RED, "Ошибка запуска: " + ex.getMessage());
                    progressBar.setVisible(false);
                    logLabel.setVisible(false);
                    resetControls();
                });
            }
        }, "server-monitor").start();
    }

    private static void startDevServer() {
        String addinDir = addinDirField.getText().trim();
        if (addinDir.isEmpty() || !Files.exists(Path.of(addinDir))) {
            devStatusLabel.setText("⚠ Папка надстройки не найдена: " + addinDir);
            devStatusLabel.setForeground(RED);
            return;
        }
        Path manifestPath = Path.of(addinDir, "manifest.xml");
        if (!Files.exists(manifestPath)) {
            devStatusLabel.setText("⚠ manifest.xml не найден в " + addinDir);
            devStatusLabel.setForeground(RED);
            return;
        }

        new Thread(() -> {
            try {
                // Start webpack dev-server
                String npm = isWindows() ? "npm.cmd" : "npm";
                ProcessBuilder pb = new ProcessBuilder(npm, "run", "dev-server");
                pb.directory(Path.of(addinDir).toFile());
                pb.redirectErrorStream(true);
                devServerProcess = pb.start();

                // Register manifest in WEF\Developer (ribbon button)
                registerDevManifest(manifestPath.toString());

                SwingUtilities.invokeLater(() -> {
                    devStatusLabel.setText("✔ Dev-server запущен (порт 3000) — перезапустите PowerPoint");
                    devStatusLabel.setForeground(GREEN);
                });

                // Drain output silently
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(devServerProcess.getInputStream()))) {
                    while (r.readLine() != null) {}
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    devStatusLabel.setText("⚠ Ошибка dev-server: " + ex.getMessage());
                    devStatusLabel.setForeground(RED);
                });
            }
        }, "dev-server").start();
    }

    private static void registerDevManifest(String manifestPath) {
        try {
            new ProcessBuilder("reg", "add",
                    "HKCU\\SOFTWARE\\Microsoft\\Office\\16.0\\WEF\\Developer",
                    "/v", DEV_REG_NAME,
                    "/t", "REG_SZ",
                    "/d", manifestPath,
                    "/f")
                    .redirectErrorStream(true).start().waitFor(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
    }

    private static void unregisterDevManifest() {
        try {
            new ProcessBuilder("reg", "delete",
                    "HKCU\\SOFTWARE\\Microsoft\\Office\\16.0\\WEF\\Developer",
                    "/v", DEV_REG_NAME, "/f")
                    .redirectErrorStream(true).start().waitFor(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
    }

    private static void onStop() {
        stoppingIntentionally = true;
        stopBtn.setEnabled(false);
        openFileBtn.setEnabled(false);
        setStatus(Color.LIGHT_GRAY, "Останавливается...");
        new Thread(() -> {
            stopAll();
            stoppingIntentionally = false;
            SwingUtilities.invokeLater(() -> {
                setStatus(Color.DARK_GRAY, "Сервер остановлен");
                if (devModeBox.isSelected()) {
                    devStatusLabel.setText(" ");
                }
                resetControls();
            });
        }, "server-stop").start();
    }

    private static void stopAll() {
        if (devServerProcess != null && devServerProcess.isAlive()) {
            devServerProcess.destroy();
            try { devServerProcess.waitFor(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            devServerProcess = null;
            unregisterDevManifest();
        }
        if (serverProcess != null && serverProcess.isAlive()) {
            serverProcess.destroy();
            try {
                if (!serverProcess.waitFor(10, TimeUnit.SECONDS))
                    serverProcess.destroyForcibly();
            } catch (InterruptedException ignored) {}
        }
        serverRunning = false;
    }

    private static void onOpenFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Выберите презентацию");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Презентации PowerPoint (*.pptx, *.ppt)", "pptx", "ppt"));
        chooser.setAcceptAllFileFilterUsed(false);
        String lastDir = loadConfig().getProperty("last.open.dir");
        if (lastDir != null) chooser.setCurrentDirectory(new java.io.File(lastDir));
        if (chooser.showOpenDialog(mainFrame) != JFileChooser.APPROVE_OPTION) return;

        java.io.File file = chooser.getSelectedFile();
        Properties p = loadConfig();
        p.setProperty("last.open.dir", file.getParent());
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) { p.store(out, null); }
        } catch (IOException ignored) {}

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
                Desktop.getDesktop().open(file);
            else
                new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath()).start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Не удалось открыть файл:\n" + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void onInstallCert() {
        certBtn.setEnabled(false);
        certBtn.setText("Устанавливается...");
        new Thread(() -> {
            try {
                Path script = getAppDir().resolve("install-cert.ps1");
                new ProcessBuilder("powershell.exe", "-Command",
                        "Start-Process powershell -Verb RunAs -Wait " +
                        "-ArgumentList '-ExecutionPolicy Bypass -File \"" + script + "\"'")
                        .redirectErrorStream(true).start().waitFor(60, TimeUnit.SECONDS);
                boolean ok = isCertInstalled();
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        certStatusLabel.setText("✔  Сертификат установлен");
                        certStatusLabel.setForeground(GREEN);
                        certBtn.setVisible(false);
                        saveSetupFlag("setup.cert", "true");
                    } else {
                        certStatusLabel.setText("✘  Ошибка — попробуйте ещё раз");
                        certStatusLabel.setForeground(RED);
                        certBtn.setText("Установить...");
                        certBtn.setEnabled(true);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    certStatusLabel.setText("✘  " + ex.getMessage());
                    certStatusLabel.setForeground(RED);
                    certBtn.setText("Установить...");
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
                Process proc = new ProcessBuilder("powershell.exe",
                        "-ExecutionPolicy", "Bypass", "-File", script.toString())
                        .redirectErrorStream(true).start();
                proc.waitFor(30, TimeUnit.SECONDS);
                boolean ok = proc.exitValue() == 0;
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        addinStatusLabel.setText("✔  Надстройка зарегистрирована — перезапустите PowerPoint");
                        addinStatusLabel.setForeground(GREEN);
                        addinBtn.setText("Перерегистрировать");
                        addinBtn.setEnabled(true);
                        saveSetupFlag("setup.addin", "true");
                    } else {
                        addinStatusLabel.setText("✘  Ошибка регистрации — попробуйте ещё раз");
                        addinStatusLabel.setForeground(RED);
                        addinBtn.setText("Зарегистрировать...");
                        addinBtn.setEnabled(true);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    addinStatusLabel.setText("✘  " + ex.getMessage());
                    addinStatusLabel.setForeground(RED);
                    addinBtn.setText("Зарегистрировать...");
                    addinBtn.setEnabled(true);
                });
            }
        }, "addin-register").start();
    }

    // ── UI helpers ────────────────────────────────────────────────────────

    private static JButton accentButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(ACCENT_DARK);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(ACCENT);
            }
        });
        return b;
    }

    private static JButton outlineButton(String text) {
        JButton b = new JButton(text);
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setFocusPainted(false);
        return b;
    }

    private static JCheckBox styledCheckbox(String text, boolean selected) {
        JCheckBox cb = new JCheckBox(text, selected);
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        cb.setFont(cb.getFont().deriveFont(12f));
        return cb;
    }

    private static void sectionLabel(JPanel parent, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
        lbl.setForeground(ACCENT);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(lbl);
    }

    private static JLabel statusIcon(String text, boolean ok) {
        JLabel lbl = new JLabel((ok ? "✔  " : "✘  ") + text);
        lbl.setForeground(ok ? GREEN : RED);
        lbl.setFont(lbl.getFont().deriveFont(12f));
        return lbl;
    }

    private static void setStatus(Color color, String text) {
        statusDot.setForeground(color);
        statusLabel.setText(text);
        statusLabel.setForeground(color.equals(GREEN) ? GREEN : Color.DARK_GRAY);
    }

    private static void resetControls() {
        startBtn.setEnabled(true);
        tokenField.setEnabled(true);
        usernameField.setEnabled(true);
        cmdJoinBox.setEnabled(true);
        cmdSlideBox.setEnabled(true);
        cmdQuestionsBox.setEnabled(true);
        devModeBox.setEnabled(true);
        addinDirField.setEnabled(true);
        progressBar.setVisible(false);
        logLabel.setVisible(false);
        openFileBtn.setEnabled(false);
    }

    private static String truncate(String line) {
        String s = line.replaceAll("^.*\\]\\s*", "").trim();
        String t = s.isEmpty() ? line.trim() : s;
        return t.length() > 72 ? t.substring(0, 69) + "…" : t;
    }

    // ── Config ────────────────────────────────────────────────────────────

    private static Properties loadConfig() {
        Properties p = new Properties();
        Path defaults = getAppDir().resolve("launcher.properties");
        if (Files.exists(defaults)) {
            try (InputStream in = Files.newInputStream(defaults)) { p.load(in); }
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
            p.setProperty("TELEGRAM_BOT_TOKEN",    token);
            p.setProperty("TELEGRAM_BOT_USERNAME", username);
            p.setProperty("bot.cmd.join",      String.valueOf(cmdJoinBox.isSelected()));
            p.setProperty("bot.cmd.slide",     String.valueOf(cmdSlideBox.isSelected()));
            p.setProperty("bot.cmd.questions", String.valueOf(cmdQuestionsBox.isSelected()));
            p.setProperty("dev.mode",          String.valueOf(devModeBox.isSelected()));
            p.setProperty("dev.addin.dir",     addinDirField.getText().trim());
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

    // ── System ────────────────────────────────────────────────────────────

    private static String detectAddinDir() {
        // Try common locations relative to project structure
        Path[] candidates = {
            Path.of("D:\\pres_assistant_v2\\pres-assistant-addin"),
            getAppDir().resolve("../../../../pres-assistant-addin").normalize(),
            getAppDir().resolve("../pres-assistant-addin").normalize(),
        };
        for (Path c : candidates) {
            if (Files.exists(c.resolve("manifest.xml"))) return c.toString();
        }
        return "";
    }

    private static boolean isCertInstalled() {
        try {
            Process p = new ProcessBuilder("powershell.exe", "-NonInteractive", "-Command",
                    "[bool](Get-ChildItem Cert:\\LocalMachine\\Root | Where-Object { $_.Subject -like '*PresAssistant*' })")
                    .redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor(10, TimeUnit.SECONDS);
            return "True".equalsIgnoreCase(out);
        } catch (Exception e) { return false; }
    }

    private static Path getAppDir() {
        try {
            URI loc = LauncherApp.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            return Path.of(loc).getParent();
        } catch (Exception e) {
            return Path.of(System.getProperty("user.dir"));
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
