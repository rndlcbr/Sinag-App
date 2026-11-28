package com.sinag;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * Modern, interactive Swing UI for SINAG: El Niño Awareness and Preparedness System.
 * Styled with deep navy sidebar, hero banner, gradient dashboard cards, and unified content reader.
 */
public class SinagApp extends JFrame {

    // --- THEME PALETTE ---
    private static final Color NAVY_TOP      = new Color(8, 43, 82);
    private static final Color NAVY_BOTTOM   = new Color(4, 24, 52);
    private static final Color TEAL_PRIMARY  = new Color(13, 148, 136);
    private static final Color SOLAR_GOLD    = new Color(247, 194, 50);
    private static final Color SOLAR_GOLD_DK = new Color(224, 162, 22);
    private static final Color BG_CANVAS     = new Color(241, 246, 252);
    private static final Color SIDEBAR_BG    = new Color(235, 241, 246);
    private static final Color TEXT_MAIN     = new Color(24, 39, 63);
    private static final Color TEXT_MUTED    = new Color(92, 108, 132);
    private static final Color BORDER_LIGHT  = new Color(214, 224, 236);

    // --- CORE SERVICES & STATE ---
    private final Database db = new Database();
    private final JPanel mainContentArea = new JPanel(new BorderLayout());
    private final List<NavPillButton> navButtons = new ArrayList<>();
    private String activeMenuKey = "Home";

    // --- QUIZ STATE ---
    private List<QuizQuestion> quizQuestions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int quizScore = 0;
    private boolean hasAnswered = false;
    private Timer questionTimer;
    private int secondsLeft = 20;
    private int currentStreak = 0;
    private int bestStreak = 0;
    private boolean fiftyFiftyUsed = false;

    public SinagApp() {
        setTitle("SINAG – El Niño Awareness and Preparedness System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1240, 800);
        setMinimumSize(new Dimension(1020, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(NAVY_TOP);
        setLayout(new BorderLayout());

        add(createSidebar(), BorderLayout.WEST);

        JPanel rightShell = new JPanel(new BorderLayout());
        rightShell.setBackground(NAVY_TOP);
        rightShell.add(createHeader(), BorderLayout.NORTH);

        RoundedWorkspacePanel workspaceWrapper = new RoundedWorkspacePanel();
        workspaceWrapper.setLayout(new BorderLayout());
        mainContentArea.setOpaque(false);
        workspaceWrapper.add(mainContentArea, BorderLayout.CENTER);

        rightShell.add(workspaceWrapper, BorderLayout.CENTER);
        add(rightShell, BorderLayout.CENTER);

        showHome();
    }

    // =========================================================================
    // 1. TOP UTILITY HEADER & FULL-HEIGHT NAVY SIDEBAR
    // =========================================================================

    private ImageIcon loadImageIcon(String relativePath, int width, int height) {
        try {
            String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
            BufferedImage original = null;
            String[] candidates = {
                relativePath,
                "src/main/resources/" + relativePath,
                "src/main/resources/images/" + fileName
            };
            for (String candidatePath : candidates) {
                File candidate = new File(candidatePath);
                if (candidate.isFile()) {
                    original = ImageIO.read(candidate);
                    if (original != null) break;
                }
            }
            if (original == null) {
                java.net.URL resource = getClass().getResource("/images/" + fileName);
                if (resource != null) original = ImageIO.read(resource);
            }
            if (original == null) {
                return null;
            }
            Image scaled = original.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception ex) {
            return null;
        }
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, NAVY_TOP, getWidth(), 0, new Color(14, 52, 96)));
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(new Color(255, 255, 255, 10));
                Path2D wave = new Path2D.Double();
                wave.moveTo(0, getHeight() * 0.7);
                wave.curveTo(getWidth() * 0.35, getHeight() * 0.2, getWidth() * 0.65, getHeight() * 0.95, getWidth(), getHeight() * 0.35);
                wave.lineTo(getWidth(), getHeight());
                wave.lineTo(0, getHeight());
                wave.closePath();
                g2.fill(wave);
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, 66));
        header.setBorder(new EmptyBorder(0, 28, 0, 28));

        JLabel tagline = new JLabel("EL NIÑO AWARENESS   •   COMMUNITY READINESS");
        tagline.setFont(new Font("SansSerif", Font.BOLD, 11));
        tagline.setForeground(new Color(214, 228, 245));
        header.add(tagline, BorderLayout.WEST);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 13));
        rightControls.setOpaque(false);

        PillButton adminBtn = new PillButton("🛡  Admin Portal", SOLAR_GOLD, SOLAR_GOLD_DK, NAVY_BOTTOM);
        adminBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        adminBtn.setBorder(new EmptyBorder(9, 18, 9, 18));
        adminBtn.addActionListener(e -> openAdminLogin());

        JLabel profileBadge = new JLabel("👤", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 215));
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        profileBadge.setPreferredSize(new Dimension(34, 34));
        profileBadge.setFont(new Font("SansSerif", Font.PLAIN, 15));
        profileBadge.setForeground(NAVY_TOP);
        profileBadge.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profileBadge.setToolTipText("Administrator Login");
        profileBadge.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openAdminLogin();
            }
        });

        rightControls.add(adminBtn);
        rightControls.add(profileBadge);
        header.add(rightControls, BorderLayout.EAST);

        return header;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, NAVY_TOP, 0, getHeight(), NAVY_BOTTOM));
                g2.fillRect(0, 0, getWidth(), getHeight());

                int h = getHeight();
                int w = getWidth();
                Path2D goldWave = new Path2D.Double();
                goldWave.moveTo(0, h - 108);
                goldWave.curveTo(w * 0.35, h - 85, w * 0.65, h - 140, w, h - 122);
                goldWave.lineTo(w, h - 98);
                goldWave.curveTo(w * 0.65, h - 118, w * 0.35, h - 68, 0, h - 94);
                goldWave.closePath();
                g2.setPaint(new GradientPaint(0, h - 130, new Color(245, 158, 11, 180), w, h - 90, SOLAR_GOLD));
                g2.fill(goldWave);

                g2.setColor(new Color(56, 189, 248, 70));
                g2.setStroke(new BasicStroke(1.5f));
                Path2D pulse = new Path2D.Double();
                int py = h - 78;
                pulse.moveTo(0, py);
                pulse.lineTo(28, py);
                pulse.lineTo(35, py - 12);
                pulse.lineTo(42, py + 10);
                pulse.lineTo(49, py - 6);
                pulse.lineTo(55, py);
                pulse.lineTo(w, py);
                g2.draw(pulse);

                g2.dispose();
            }
        };
        sidebar.setPreferredSize(new Dimension(235, 0));
        sidebar.setBorder(new EmptyBorder(22, 16, 20, 16));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JPanel brandBox = new JPanel();
        brandBox.setOpaque(false);
        brandBox.setLayout(new BoxLayout(brandBox, BoxLayout.Y_AXIS));
        brandBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        ImageIcon customLogo = loadImageIcon("images/sinag-logo.jpg", 94, 94);
        if (customLogo != null && customLogo.getIconWidth() > 0) {
            JLabel logoImg = new JLabel(customLogo);
            logoImg.setAlignmentX(Component.CENTER_ALIGNMENT);
            brandBox.add(logoImg);
        } else {
            JLabel sunIcon = new JLabel("☀");
            sunIcon.setFont(new Font("SansSerif", Font.BOLD, 38));
            sunIcon.setForeground(SOLAR_GOLD);
            sunIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel brandName = new JLabel("SINAG");
            brandName.setFont(new Font("SansSerif", Font.BOLD, 28));
            brandName.setForeground(Color.WHITE);
            brandName.setAlignmentX(Component.CENTER_ALIGNMENT);

            brandBox.add(sunIcon);
            brandBox.add(brandName);
        }

        JLabel brandTag = new JLabel("Be Ready. Stay Safe.");
        brandTag.setFont(new Font("SansSerif", Font.BOLD, 11));
        brandTag.setForeground(SOLAR_GOLD);
        brandTag.setAlignmentX(Component.CENTER_ALIGNMENT);
        brandBox.add(Box.createVerticalStrut(4));
        brandBox.add(brandTag);

        sidebar.add(brandBox);
        sidebar.add(Box.createVerticalStrut(24));

        String[][] menuItems = {
            {"⌂", "Home", "Home"},
            {"▤", "Articles", "Article"},
            {"⚠", "Advisories", "Advisory"},
            {"?", "FAQs", "FAQ"},
            {"☰", "Preparedness Guide", "Guide"},
            {"★", "Preparedness Quiz", "Quiz"},
            {"ⓘ", "About SINAG", "About"}
        };

        navButtons.clear();
        for (String[] item : menuItems) {
            String icon = item[0];
            String label = item[1];
            String pageKey = item[2];

            NavPillButton btn = new NavPillButton(icon, label, pageKey);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

            btn.addActionListener(e -> {
                setActiveNav(pageKey);
                switch (pageKey) {
                    case "Home"  -> showHome();
                    case "Quiz"  -> showQuiz();
                    case "About" -> showAbout();
                    default      -> showContentPage(pageKey);
                }
            });

            navButtons.add(btn);
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(6));
        }

        setActiveNav(activeMenuKey);
        sidebar.add(Box.createVerticalGlue());

        JButton exitBtn = new JButton("⚙   Exit Application") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(255, 255, 255, 25) : new Color(6, 28, 58));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.setColor(new Color(148, 178, 214, 130));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        exitBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        exitBtn.setForeground(Color.WHITE);
        exitBtn.setContentAreaFilled(false);
        exitBtn.setBorderPainted(false);
        exitBtn.setFocusPainted(false);
        exitBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        exitBtn.addActionListener(e -> dispose());
        sidebar.add(exitBtn);

        return sidebar;
    }

    private void setActiveNav(String key) {
        activeMenuKey = key;
        for (NavPillButton btn : navButtons) {
            btn.setActive(btn.getPageKey().equals(key));
        }
    }

    // =========================================================================
    // 2. HOME DASHBOARD (Hero Banner + Glass Weather Card + Gradient Modules)
    // =========================================================================

    private void showHome() {
        setActiveNav("Home");
        JPanel homePanel = new JPanel(new BorderLayout(0, 16));
        homePanel.setOpaque(false);
        homePanel.setBorder(new EmptyBorder(20, 26, 18, 26));

        HeroBannerPanel heroBanner = new HeroBannerPanel("images/hero-drought.png");
        heroBanner.setLayout(new BorderLayout(20, 0));
        heroBanner.setBorder(new EmptyBorder(22, 28, 22, 24));

        JPanel heroLeft = new JPanel();
        heroLeft.setOpaque(false);
        heroLeft.setLayout(new BoxLayout(heroLeft, BoxLayout.Y_AXIS));

        JLabel sinagPill = new JLabel("SINAG", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SOLAR_GOLD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        sinagPill.setFont(new Font("SansSerif", Font.BOLD, 10));
        sinagPill.setForeground(NAVY_BOTTOM);
        sinagPill.setBorder(new EmptyBorder(4, 14, 4, 14));
        sinagPill.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heroHeadline = new JLabel("<html><span style='color:#FFFFFF;'>Be informed. Be prepared. </span><span style='color:#F7C232;'>Stay safe.</span></html>");
        heroHeadline.setFont(new Font("SansSerif", Font.BOLD, 27));
        heroHeadline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heroSub = new JLabel("<html><div style='width:460px; color:#E2EEFA; line-height:1.35;'>"
                + "Access reliable information, helpful guides, and real-time advisories<br>"
                + "to keep you and your community safe during El Niño conditions.</div></html>");
        heroSub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        heroSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        heroLeft.add(sinagPill);
        heroLeft.add(Box.createVerticalStrut(12));
        heroLeft.add(heroHeadline);
        heroLeft.add(Box.createVerticalStrut(8));
        heroLeft.add(heroSub);
        heroBanner.add(heroLeft, BorderLayout.CENTER);

        GlassWeatherPanel weatherCard = new GlassWeatherPanel();
        weatherCard.setPreferredSize(new Dimension(235, 145));
        weatherCard.setLayout(new BorderLayout(0, 8));
        weatherCard.setBorder(new EmptyBorder(14, 18, 12, 18));

        JPanel weatherTop = new JPanel(new BorderLayout(10, 0));
        weatherTop.setOpaque(false);

        JLabel sunWeatherGraphic = new SunCloudGraphic();
        sunWeatherGraphic.setPreferredSize(new Dimension(54, 46));

        JPanel tempStack = new JPanel();
        tempStack.setOpaque(false);
        tempStack.setLayout(new BoxLayout(tempStack, BoxLayout.Y_AXIS));

        JLabel weatherTemp = new JLabel("--.-°C");
        weatherTemp.setFont(new Font("SansSerif", Font.BOLD, 28));
        weatherTemp.setForeground(Color.WHITE);

        JLabel weatherCondition = new JLabel("Partly Cloudy");
        weatherCondition.setFont(new Font("SansSerif", Font.PLAIN, 12));
        weatherCondition.setForeground(new Color(230, 240, 252));

        tempStack.add(weatherTemp);
        tempStack.add(weatherCondition);

        weatherTop.add(sunWeatherGraphic, BorderLayout.WEST);
        weatherTop.add(tempStack, BorderLayout.CENTER);

        JPanel weatherBottom = new JPanel();
        weatherBottom.setOpaque(false);
        weatherBottom.setLayout(new BoxLayout(weatherBottom, BoxLayout.Y_AXIS));
        weatherBottom.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 65)),
            new EmptyBorder(8, 0, 0, 0)
        ));

        JLabel locationLabel = new JLabel("📍  Batangas City, PH", SwingConstants.CENTER);
        locationLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        locationLabel.setForeground(Color.WHITE);
        locationLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String nowStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a"));
        JLabel updatedLabel = new JLabel("Last updated: " + nowStamp, SwingConstants.CENTER);
        updatedLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        updatedLabel.setForeground(new Color(218, 232, 248));
        updatedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        weatherBottom.add(locationLabel);
        weatherBottom.add(Box.createVerticalStrut(3));
        weatherBottom.add(updatedLabel);

        weatherCard.add(weatherTop, BorderLayout.CENTER);
        weatherCard.add(weatherBottom, BorderLayout.SOUTH);
        heroBanner.add(weatherCard, BorderLayout.EAST);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return WeatherService.fetchLiveWeather();
            }

            @Override
            protected void done() {
                try {
                    String fullText = get();
                    if (fullText.contains(": ") && fullText.contains("°C")) {
                        String afterCity = fullText.substring(fullText.indexOf(": ") + 2);
                        String tempPart = afterCity.substring(0, afterCity.indexOf("°C") + 2);
                        String extraPart = afterCity.substring(afterCity.indexOf("°C") + 2).replace("(", "").replace(")", "").trim();
                        weatherTemp.setText(tempPart);
                        if (!extraPart.isBlank()) {
                            weatherCondition.setText(extraPart);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();

        JPanel centerSection = new JPanel(new BorderLayout(0, 12));
        centerSection.setOpaque(false);

        JPanel sectionHeading = new JPanel(new BorderLayout(10, 0));
        sectionHeading.setOpaque(false);

        JPanel accentBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SOLAR_GOLD);
                g2.fillRoundRect(0, 2, 4, getHeight() - 4, 4, 4);
                g2.dispose();
            }
        };
        accentBar.setOpaque(false);
        accentBar.setPreferredSize(new Dimension(6, 40));

        JPanel headingTexts = new JPanel();
        headingTexts.setOpaque(false);
        headingTexts.setLayout(new BoxLayout(headingTexts, BoxLayout.Y_AXIS));

        JLabel exploreTitle = new JLabel("Explore SINAG Modules");
        exploreTitle.setFont(new Font("SansSerif", Font.BOLD, 21));
        exploreTitle.setForeground(NAVY_BOTTOM);

        JLabel exploreSub = new JLabel("Click any module below to find useful guides and tools for your preparedness.");
        exploreSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        exploreSub.setForeground(TEXT_MUTED);

        headingTexts.add(exploreTitle);
        headingTexts.add(Box.createVerticalStrut(2));
        headingTexts.add(exploreSub);

        sectionHeading.add(accentBar, BorderLayout.WEST);
        sectionHeading.add(headingTexts, BorderLayout.CENTER);
        centerSection.add(sectionHeading, BorderLayout.NORTH);

        JPanel cardGrid = new JPanel(new GridLayout(2, 2, 18, 16));
        cardGrid.setOpaque(false);

        addDashboardModuleCard(
            cardGrid,
            ModuleCardStyle.BLUE,
            "Educational Articles",
            "Learn about El Niño risks, safety tips, and preparedness measures.",
            "Explore Articles →",
            () -> showContentPage("Article")
        );

        addDashboardModuleCard(
            cardGrid,
            ModuleCardStyle.GREEN,
            "Public Advisories",
            "Stay updated on water conservation alerts and extreme weather reminders.",
            "View Advisories →",
            () -> showContentPage("Advisory")
        );

        addDashboardModuleCard(
            cardGrid,
            ModuleCardStyle.PURPLE,
            "Preparedness Guides",
            "Practical household checklists, emergency water storage, and cooling tips.",
            "Open Guides →",
            () -> showContentPage("Guide")
        );

        addDashboardModuleCard(
            cardGrid,
            ModuleCardStyle.ORANGE,
            "Preparedness Quiz",
            "Challenge yourself with timed questions about climate preparedness.",
            "Start Quiz →",
            this::showQuiz
        );

        centerSection.add(cardGrid, BorderLayout.CENTER);

        ReminderBannerPanel reminderBar = new ReminderBannerPanel();
        reminderBar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));

        JLabel warnBadge = new JLabel("▲", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(146, 64, 14));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        warnBadge.setPreferredSize(new Dimension(24, 24));
        warnBadge.setFont(new Font("SansSerif", Font.BOLD, 12));
        warnBadge.setForeground(SOLAR_GOLD);

        JLabel reminderBold = new JLabel("PREPARE TODAY, BE SAFER TOMORROW.");
        reminderBold.setFont(new Font("SansSerif", Font.BOLD, 12));
        reminderBold.setForeground(new Color(69, 39, 9));

        JLabel divider = new JLabel("|");
        divider.setForeground(new Color(180, 130, 40));

        JLabel reminderDesc = new JLabel("Consult official PAGASA and local government updates for the latest information.");
        reminderDesc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        reminderDesc.setForeground(new Color(69, 39, 9));

        reminderBar.add(warnBadge);
        reminderBar.add(reminderBold);
        reminderBar.add(divider);
        reminderBar.add(reminderDesc);

        homePanel.add(heroBanner, BorderLayout.NORTH);
        homePanel.add(centerSection, BorderLayout.CENTER);
        homePanel.add(reminderBar, BorderLayout.SOUTH);

        switchMainPage(homePanel);
    }

    private enum ModuleCardStyle {
        BLUE(
            new Color(224, 241, 255), new Color(156, 206, 252), new Color(165, 208, 248),
            new Color(10, 88, 184), new Color(10, 62, 126), "📖"
        ),
        GREEN(
            new Color(225, 250, 232), new Color(164, 232, 184), new Color(168, 228, 186),
            new Color(14, 138, 66), new Color(12, 92, 46), "📢"
        ),
        PURPLE(
            new Color(236, 228, 255), new Color(196, 180, 252), new Color(198, 184, 248),
            new Color(91, 68, 204), new Color(34, 28, 108), "📋"
        ),
        ORANGE(
            new Color(255, 240, 222), new Color(253, 204, 152), new Color(248, 204, 158),
            new Color(234, 112, 16), new Color(186, 78, 8), "🎮"
        );

        final Color gradStart;
        final Color gradEnd;
        final Color borderColor;
        final Color circleColor;
        final Color buttonColor;
        final String symbol;

        ModuleCardStyle(Color gradStart, Color gradEnd, Color borderColor,
                        Color circleColor, Color buttonColor, String symbol) {
            this.gradStart = gradStart;
            this.gradEnd = gradEnd;
            this.borderColor = borderColor;
            this.circleColor = circleColor;
            this.buttonColor = buttonColor;
            this.symbol = symbol;
        }
    }

    private void addDashboardModuleCard(JPanel parentGrid, ModuleCardStyle style, String title,
                                        String desc, String buttonText, Runnable onClickAction) {
        GradientModuleCard card = new GradientModuleCard(style);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(new EmptyBorder(16, 20, 16, 18));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel badgeWrapper = new JPanel(new GridBagLayout());
        badgeWrapper.setOpaque(false);

        JLabel circleIcon = new JLabel(style.symbol, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 110));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setPaint(new GradientPaint(0, 0, style.circleColor.brighter(), getWidth(), getHeight(), style.circleColor.darker()));
                g2.fillOval(5, 5, getWidth() - 10, getHeight() - 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        circleIcon.setPreferredSize(new Dimension(74, 74));
        circleIcon.setFont(new Font("SansSerif", Font.PLAIN, 28));
        circleIcon.setForeground(Color.WHITE);
        badgeWrapper.add(circleIcon);

        JPanel textColumn = new JPanel();
        textColumn.setOpaque(false);
        textColumn.setLayout(new BoxLayout(textColumn, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
        titleLabel.setForeground(NAVY_BOTTOM);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea descArea = new JTextArea(desc);
        descArea.setWrapStyleWord(true);
        descArea.setLineWrap(true);
        descArea.setEditable(false);
        descArea.setOpaque(false);
        descArea.setForeground(new Color(44, 62, 86));
        descArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        descArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        textColumn.add(Box.createVerticalGlue());
        textColumn.add(titleLabel);
        textColumn.add(Box.createVerticalStrut(5));
        textColumn.add(descArea);
        textColumn.add(Box.createVerticalGlue());

        JPanel rightActionCol = new JPanel(new BorderLayout());
        rightActionCol.setOpaque(false);

        PillButton actionBtn = new PillButton(buttonText, style.buttonColor, style.buttonColor.darker(), Color.WHITE);
        actionBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
        actionBtn.setBorder(new EmptyBorder(9, 16, 9, 16));
        actionBtn.addActionListener(e -> onClickAction.run());

        rightActionCol.add(actionBtn, BorderLayout.SOUTH);

        MouseAdapter clickHandler = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setHovered(true);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                card.setHovered(false);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                onClickAction.run();
            }
        };
        card.addMouseListener(clickHandler);
        descArea.addMouseListener(clickHandler);

        card.add(badgeWrapper, BorderLayout.WEST);
        card.add(textColumn, BorderLayout.CENTER);
        card.add(rightActionCol, BorderLayout.EAST);

        parentGrid.add(card);
    }

    // =========================================================================
    // 3. UNIFIED CONTENT READER (Articles, Advisories, FAQs, Preparedness Guides)
    // =========================================================================

    private void showContentPage(String contentType) {
        setActiveNav(contentType);

        String pageTitle;
        String pageSubtitle;
        String headerSymbol;
        String searchPlaceholder;
        String listHeaderTitle;
        String actionPillLabel;

        switch (contentType) {
            case "Article" -> {
                pageTitle = "Educational Articles";
                pageSubtitle = "Search for keyword or filter by topic to explore climate preparedness resources.";
                headerSymbol = "📖";
                searchPlaceholder = "Search articles...";
                listHeaderTitle = "Articles List";
                actionPillLabel = "📖  Full Article View";
            }
            case "Advisory" -> {
                pageTitle = "Public Advisories";
                pageSubtitle = "Stay updated on water conservation alerts and extreme heat safety reminders.";
                headerSymbol = "⚠";
                searchPlaceholder = "Search advisories...";
                listHeaderTitle = "Advisories List";
                actionPillLabel = "⚠  Official Advisory";
            }
            case "FAQ" -> {
                pageTitle = "Frequently Asked Questions";
                pageSubtitle = "Quick, verified answers to common El Niño and household readiness questions.";
                headerSymbol = "?";
                searchPlaceholder = "Search FAQs...";
                listHeaderTitle = "Questions List";
                actionPillLabel = "?  Verified Answer";
            }
            default -> {
                pageTitle = "Preparedness Guides";
                pageSubtitle = "Practical household checklists, emergency water storage, and cooling tips.";
                headerSymbol = "📋";
                searchPlaceholder = "Search guides...";
                listHeaderTitle = "Guides List";
                actionPillLabel = "📋  Preparedness Guide";
            }
        }

        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(18, 26, 18, 26));

        // 1. TOP GRADIENT HEADER BANNER CARD
        ContentPageBanner banner = new ContentPageBanner(headerSymbol, pageTitle, pageSubtitle);

        // 2. PILL SEARCH & FILTER TOOLBAR
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        RoundedPanel searchBarBox = new RoundedPanel(22, Color.WHITE, BORDER_LIGHT);
        searchBarBox.setLayout(new BorderLayout(10, 0));
        searchBarBox.setBorder(new EmptyBorder(6, 16, 6, 14));

        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchIcon.setForeground(NAVY_TOP);

        JTextField searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setBorder(null);
        searchField.setOpaque(false);
        searchField.setForeground(TEXT_MUTED);
        searchField.setText(searchPlaceholder);

        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals(searchPlaceholder)) {
                    searchField.setText("");
                    searchField.setForeground(TEXT_MAIN);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isBlank()) {
                    searchField.setForeground(TEXT_MUTED);
                    searchField.setText(searchPlaceholder);
                }
            }
        });

        searchBarBox.add(searchIcon, BorderLayout.WEST);
        searchBarBox.add(searchField, BorderLayout.CENTER);

        JPanel rightFilterControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightFilterControls.setOpaque(false);

        JLabel topicLabel = new JLabel("Topic:");
        topicLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        topicLabel.setForeground(NAVY_BOTTOM);

        RoundedPanel comboWrapper = new RoundedPanel(20, Color.WHITE, BORDER_LIGHT);
        comboWrapper.setLayout(new BorderLayout());
        comboWrapper.setBorder(new EmptyBorder(4, 12, 4, 8));

        JComboBox<String> categoryBox = new JComboBox<>(new String[]{
            "All", "Basics", "Effects", "Water Conservation", "Heat Safety", "Preparedness", "Other"
        });
        categoryBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        categoryBox.setBackground(Color.WHITE);
        categoryBox.setForeground(TEXT_MAIN);
        categoryBox.setBorder(null);
        categoryBox.setPreferredSize(new Dimension(150, 28));
        comboWrapper.add(categoryBox, BorderLayout.CENTER);

        PillButton searchBtn = new PillButton("🔍  Search", NAVY_TOP, NAVY_BOTTOM, Color.WHITE);
        searchBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        searchBtn.setBorder(new EmptyBorder(9, 18, 9, 18));

        PillButton resetBtn = new PillButton("↻  Reset", new Color(216, 224, 234), new Color(196, 208, 222), NAVY_BOTTOM);
        resetBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        resetBtn.setBorder(new EmptyBorder(9, 16, 9, 16));

        rightFilterControls.add(topicLabel);
        rightFilterControls.add(comboWrapper);
        rightFilterControls.add(searchBtn);
        rightFilterControls.add(resetBtn);

        toolbar.add(searchBarBox, BorderLayout.CENTER);
        toolbar.add(rightFilterControls, BorderLayout.EAST);

        JPanel topStack = new JPanel(new BorderLayout(0, 12));
        topStack.setOpaque(false);
        topStack.add(banner, BorderLayout.NORTH);
        topStack.add(toolbar, BorderLayout.SOUTH);

        // 3. LEFT MASTER LIST CARD + RIGHT READER CARD
        JPanel splitContainer = new JPanel(new BorderLayout(16, 0));
        splitContainer.setOpaque(false);

        // Left Card ("Categories / Records")
        RoundedPanel leftCard = new RoundedPanel(22, Color.WHITE, BORDER_LIGHT);
        leftCard.setPreferredSize(new Dimension(285, 0));
        leftCard.setLayout(new BorderLayout(0, 8));
        leftCard.setBorder(new EmptyBorder(14, 12, 12, 12));

        JPanel leftCardHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        leftCardHeader.setOpaque(false);
        leftCardHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(232, 238, 246)),
            new EmptyBorder(0, 4, 8, 4)
        ));

        JLabel stackIcon = new JLabel("❖");
        stackIcon.setFont(new Font("SansSerif", Font.BOLD, 16));
        stackIcon.setForeground(NAVY_TOP);

        JLabel leftHeading = new JLabel(listHeaderTitle);
        leftHeading.setFont(new Font("SansSerif", Font.BOLD, 15));
        leftHeading.setForeground(NAVY_BOTTOM);

        leftCardHeader.add(stackIcon);
        leftCardHeader.add(leftHeading);

        DefaultListModel<Content> listModel = new DefaultListModel<>();
        JList<Content> itemList = new JList<>(listModel);
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setOpaque(false);
        itemList.setBackground(Color.WHITE);

        // Custom Rounded Card Cell Renderer matching screenshot
        itemList.setCellRenderer((list, value, index, isSelected, cellHasFocus) ->
            new ContentListItemCard(value, index + 1, isSelected)
        );

        JScrollPane leftScroll = new JScrollPane(itemList);
        leftScroll.setBorder(null);
        leftScroll.setOpaque(false);
        leftScroll.getViewport().setOpaque(false);
        leftScroll.getVerticalScrollBar().setUnitIncrement(14);
        leftScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Scenic mini-illustration footer at bottom of left card
        JPanel leftFooterArt = new CommunityIllustrationFooter();
        leftFooterArt.setPreferredSize(new Dimension(0, 92));

        leftCard.add(leftCardHeader, BorderLayout.NORTH);
        leftCard.add(leftScroll, BorderLayout.CENTER);
        leftCard.add(leftFooterArt, BorderLayout.SOUTH);

        // Right Detail Reader Card
        RoundedPanel readerCard = new RoundedPanel(22, Color.WHITE, BORDER_LIGHT);
        readerCard.setLayout(new BorderLayout(0, 12));
        readerCard.setBorder(new EmptyBorder(20, 24, 18, 24));

        // Reader Header with vertical gold bar + title + metadata icons
        JPanel readerTopHeader = new JPanel(new BorderLayout(12, 0));
        readerTopHeader.setOpaque(false);

        JPanel goldVertBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SOLAR_GOLD);
                g2.fillRoundRect(0, 2, 4, getHeight() - 4, 4, 4);
                g2.dispose();
            }
        };
        goldVertBar.setOpaque(false);
        goldVertBar.setPreferredSize(new Dimension(5, 48));

        JPanel titleMetaStack = new JPanel();
        titleMetaStack.setOpaque(false);
        titleMetaStack.setLayout(new BoxLayout(titleMetaStack, BoxLayout.Y_AXIS));

        JLabel readerTitle = new JLabel("Select an item on the left to read");
        readerTitle.setFont(new Font("SansSerif", Font.BOLD, 19));
        readerTitle.setForeground(NAVY_BOTTOM);

        JLabel readerMeta = new JLabel("📅  Date   |   🏷  Category   |   🛡  Source");
        readerMeta.setFont(new Font("SansSerif", Font.PLAIN, 12));
        readerMeta.setForeground(TEXT_MUTED);

        titleMetaStack.add(readerTitle);
        titleMetaStack.add(Box.createVerticalStrut(5));
        titleMetaStack.add(readerMeta);

        readerTopHeader.add(goldVertBar, BorderLayout.WEST);
        readerTopHeader.add(titleMetaStack, BorderLayout.CENTER);

        // Scrollable Body Area with Rounded Image Preview + Formatted HTML Text
        ScrollablePanel scrollableContent = new ScrollablePanel();
        scrollableContent.setLayout(new BoxLayout(scrollableContent, BoxLayout.Y_AXIS));
        scrollableContent.setBackground(Color.WHITE);

        RoundedImagePreview readerImagePreview = new RoundedImagePreview();
        readerImagePreview.setAlignmentX(Component.LEFT_ALIGNMENT);
        readerImagePreview.setVisible(false);

        JEditorPane readingPane = new JEditorPane("text/html", "");
        readingPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        readingPane.setFont(new Font("SansSerif", Font.PLAIN, 14));
        readingPane.setForeground(TEXT_MAIN);
        readingPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        readingPane.setEditable(false);
        readingPane.setOpaque(true);
        readingPane.setBackground(Color.WHITE);
        readingPane.setText(formatBodyAsHtml("Choose any item from the list on the left to view its full details here."));

        scrollableContent.add(readerImagePreview);
        scrollableContent.add(readingPane);

        JScrollPane contentScroll = new JScrollPane(scrollableContent);
        contentScroll.setBorder(null);
        contentScroll.setBackground(Color.WHITE);
        contentScroll.getViewport().setBackground(Color.WHITE);
        contentScroll.getVerticalScrollBar().setUnitIncrement(16);
        contentScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Bottom-Right Pill Badge / Scroll-to-Top Action matching "Read Full Article →"
        JPanel readerFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        readerFooter.setOpaque(false);

        PillButton statusPill = new PillButton(actionPillLabel, SOLAR_GOLD, SOLAR_GOLD_DK, NAVY_BOTTOM);
        statusPill.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusPill.setBorder(new EmptyBorder(9, 18, 9, 18));
        statusPill.addActionListener(e -> SwingUtilities.invokeLater(() -> contentScroll.getVerticalScrollBar().setValue(0)));
        readerFooter.add(statusPill);

        readerCard.add(readerTopHeader, BorderLayout.NORTH);
        readerCard.add(contentScroll, BorderLayout.CENTER);
        readerCard.add(readerFooter, BorderLayout.SOUTH);

        itemList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && itemList.getSelectedValue() != null) {
                Content selected = itemList.getSelectedValue();
                readerTitle.setText(selected.getTitle());
                readerMeta.setText("📅  " + selected.getPublishedDate()
                        + "    |    🏷  " + selected.getCategory()
                        + "    |    🛡  Source: " + selected.getSource());
                readingPane.setText(formatBodyAsHtml(selected.getBody()));

                String imgPath = selected.getImagePath();
                BufferedImage loadedImg = null;
                if (imgPath != null && !imgPath.isBlank()) {
                    try {
                        if (imgPath.startsWith("data:image")) {
                            String base64Data = imgPath.substring(imgPath.indexOf(",") + 1);
                            byte[] imgBytes = Base64.getDecoder().decode(base64Data);
                            loadedImg = ImageIO.read(new java.io.ByteArrayInputStream(imgBytes));
                        } else if (new File(imgPath).exists()) {
                            loadedImg = ImageIO.read(new File(imgPath));
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (loadedImg != null) {
                    readerImagePreview.setImage(loadedImg);
                    readerImagePreview.setVisible(true);
                } else {
                    readerImagePreview.setImage(null);
                    readerImagePreview.setVisible(false);
                }

                scrollableContent.revalidate();
                scrollableContent.repaint();
                SwingUtilities.invokeLater(() -> contentScroll.getVerticalScrollBar().setValue(0));
            }
        });

        Runnable reloadItems = () -> {
            listModel.clear();
            readerImagePreview.setImage(null);
            readerImagePreview.setVisible(false);
            String queryText = searchField.getText().trim();
            if (queryText.equals(searchPlaceholder)) {
                queryText = "";
            }
            try {
                List<Content> records = db.getContent(contentType, queryText, (String) categoryBox.getSelectedItem());
                for (Content c : records) {
                    listModel.addElement(c);
                }
                if (!records.isEmpty()) {
                    itemList.setSelectedIndex(0);
                } else {
                    readerTitle.setText("No matching records found");
                    readerMeta.setText("Try clearing your search keyword or selecting 'All' topics");
                    readingPane.setText(formatBodyAsHtml("No records matched your current filter criteria."));
                }
            } catch (SQLException ex) {
                showError(ex);
            }
        };

        searchBtn.addActionListener(e -> reloadItems.run());
        searchField.addActionListener(e -> reloadItems.run());
        categoryBox.addActionListener(e -> reloadItems.run());
        resetBtn.addActionListener(e -> {
            searchField.setForeground(TEXT_MUTED);
            searchField.setText(searchPlaceholder);
            categoryBox.setSelectedIndex(0);
            reloadItems.run();
        });

        splitContainer.add(leftCard, BorderLayout.WEST);
        splitContainer.add(readerCard, BorderLayout.CENTER);

        page.add(topStack, BorderLayout.NORTH);
        page.add(splitContainer, BorderLayout.CENTER);

        reloadItems.run();
        switchMainPage(page);
    }

    // =========================================================================
    // 4. INTERACTIVE GAMIFIED QUIZ (Timer + Streak + 50/50 Lifeline + Rank)
    // =========================================================================

    private void showQuiz() {
        setActiveNav("Quiz");
        JPanel page = createPageContainer("Preparedness Quiz Challenge", "Beat the 20-second clock, build an answer streak, and test your El Niño readiness.");
        JPanel wrapper = new JPanel(new BorderLayout(12, 14));
        wrapper.setOpaque(false);

        RoundedPanel topControls = new RoundedPanel(14, Color.WHITE, BORDER_LIGHT);
        topControls.setLayout(new BorderLayout(10, 0));
        topControls.setBorder(new EmptyBorder(8, 14, 8, 14));

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftControls.setOpaque(false);

        JComboBox<String> categoryBox = new JComboBox<>(new String[]{
            "All", "Basics", "Effects", "Water Conservation", "Heat Safety", "Preparedness"
        });
        categoryBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        categoryBox.setBackground(Color.WHITE);

        JButton startBtn = createStyledButton("▶  Start Challenge", TEAL_PRIMARY);
        JButton lifelineBtn = createStyledButton("⚡ 50/50 Lifeline (1)", SOLAR_GOLD_DK);
        lifelineBtn.setEnabled(false);

        leftControls.add(createFieldLabel("Topic"));
        leftControls.add(categoryBox);
        leftControls.add(startBtn);
        leftControls.add(lifelineBtn);

        JLabel streakLabel = new JLabel("🔥 STREAK: 0x   |   SCORE: 0");
        streakLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        streakLabel.setForeground(new Color(146, 64, 14));

        topControls.add(leftControls, BorderLayout.WEST);
        topControls.add(streakLabel, BorderLayout.EAST);

        RoundedPanel quizCard = new RoundedPanel(18, Color.WHITE, BORDER_LIGHT);
        quizCard.setLayout(new BorderLayout(12, 16));
        quizCard.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel progressHeader = new JPanel(new BorderLayout(0, 8));
        progressHeader.setOpaque(false);

        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setOpaque(false);

        JLabel progressLabel = new JLabel("SELECT A TOPIC & PRESS START CHALLENGE");
        progressLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        progressLabel.setForeground(TEAL_PRIMARY);

        JLabel timerLabel = new JLabel("⏱ 20s");
        timerLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        timerLabel.setForeground(TEAL_PRIMARY);

        statusRow.add(progressLabel, BorderLayout.WEST);
        statusRow.add(timerLabel, BorderLayout.EAST);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setForeground(SOLAR_GOLD);
        progressBar.setBackground(new Color(226, 232, 240));
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 8));

        progressHeader.add(statusRow, BorderLayout.NORTH);
        progressHeader.add(progressBar, BorderLayout.SOUTH);

        JTextArea questionArea = new JTextArea("Ready to test your El Niño preparedness? Choose a topic above and click Start Challenge!");
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setEditable(false);
        questionArea.setOpaque(false);
        questionArea.setForeground(NAVY_BOTTOM);
        questionArea.setFont(new Font("SansSerif", Font.BOLD, 18));

        JPanel choicesPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        choicesPanel.setOpaque(false);
        ButtonGroup choiceGroup = new ButtonGroup();
        JRadioButton[] optionButtons = new JRadioButton[4];

        for (int i = 0; i < 4; i++) {
            JRadioButton radio = new JRadioButton();
            radio.setFont(new Font("SansSerif", Font.PLAIN, 13));
            radio.setForeground(TEXT_MAIN);
            radio.setBackground(new Color(248, 250, 252));
            radio.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_LIGHT),
                new EmptyBorder(10, 14, 10, 14)
            ));
            radio.setOpaque(true);
            radio.setFocusPainted(false);
            radio.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            radio.setVisible(false);

            radio.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!hasAnswered && radio.isEnabled() && !radio.isSelected()) {
                        radio.setBackground(new Color(241, 245, 249));
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (!hasAnswered && radio.isEnabled() && !radio.isSelected()) {
                        radio.setBackground(new Color(248, 250, 252));
                    }
                }
            });

            radio.addActionListener(e -> {
                if (hasAnswered) return;
                for (JRadioButton rb : optionButtons) {
                    if (!rb.isEnabled()) continue;
                    if (rb.isSelected()) {
                        rb.setBackground(new Color(240, 253, 250));
                        rb.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(TEAL_PRIMARY, 2),
                            new EmptyBorder(9, 13, 9, 13)
                        ));
                    } else {
                        rb.setBackground(new Color(248, 250, 252));
                        rb.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(BORDER_LIGHT),
                            new EmptyBorder(10, 14, 10, 14)
                        ));
                    }
                }
            });

            optionButtons[i] = radio;
            choiceGroup.add(radio);
            choicesPanel.add(radio);
        }

        JTextArea feedbackArea = new JTextArea();
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        feedbackArea.setEditable(false);
        feedbackArea.setOpaque(true);
        feedbackArea.setBackground(Color.WHITE);
        feedbackArea.setFont(new Font("SansSerif", Font.BOLD, 12));
        feedbackArea.setBorder(new EmptyBorder(10, 14, 10, 14));

        JButton submitOrNextBtn = createStyledButton("Lock In Answer", NAVY_TOP);
        submitOrNextBtn.setEnabled(false);

        JPanel questionBody = new JPanel(new BorderLayout(10, 14));
        questionBody.setOpaque(false);
        questionBody.add(questionArea, BorderLayout.NORTH);
        questionBody.add(choicesPanel, BorderLayout.CENTER);
        questionBody.add(feedbackArea, BorderLayout.SOUTH);

        quizCard.add(progressHeader, BorderLayout.NORTH);
        quizCard.add(questionBody, BorderLayout.CENTER);
        quizCard.add(submitOrNextBtn, BorderLayout.SOUTH);

        wrapper.add(topControls, BorderLayout.NORTH);
        wrapper.add(quizCard, BorderLayout.CENTER);
        page.add(wrapper, BorderLayout.CENTER);

        Runnable evaluateAnswer = () -> {
            if (hasAnswered || quizQuestions.isEmpty()) return;
            if (questionTimer != null) questionTimer.stop();

            QuizQuestion currentQ = quizQuestions.get(currentQuestionIndex);
            String userAnswer = null;
            for (int i = 0; i < 4; i++) {
                if (optionButtons[i].isSelected()) {
                    userAnswer = String.valueOf((char) ('A' + i));
                }
            }

            boolean isTimeout = (userAnswer == null);
            boolean isCorrect = !isTimeout && currentQ.checkAnswer(userAnswer);

            int correctIdx = currentQ.getCorrect().charAt(0) - 'A';
            for (int i = 0; i < 4; i++) {
                optionButtons[i].setEnabled(false);
                if (i == correctIdx) {
                    optionButtons[i].setBackground(new Color(220, 252, 231));
                    optionButtons[i].setForeground(new Color(22, 101, 52));
                    optionButtons[i].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(22, 163, 74), 2),
                        new EmptyBorder(9, 13, 9, 13)
                    ));
                } else if (optionButtons[i].isSelected()) {
                    optionButtons[i].setBackground(new Color(254, 226, 226));
                    optionButtons[i].setForeground(new Color(153, 27, 27));
                    optionButtons[i].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(220, 38, 38), 2),
                        new EmptyBorder(9, 13, 9, 13)
                    ));
                }
            }

            if (isCorrect) {
                quizScore++;
                currentStreak++;
                bestStreak = Math.max(bestStreak, currentStreak);
                feedbackArea.setBackground(new Color(220, 252, 231));
                feedbackArea.setForeground(new Color(22, 101, 52));
                feedbackArea.setText("✓ Spot on! " + (currentStreak >= 2 ? "(" + currentStreak + "x Streak!) " : "") + currentQ.getExplanation());
            } else {
                currentStreak = 0;
                feedbackArea.setBackground(new Color(254, 226, 226));
                feedbackArea.setForeground(new Color(153, 27, 27));
                String prefix = isTimeout ? "⏰ Time's up! " : "✕ Not quite. ";
                feedbackArea.setText(prefix + "Correct answer: " + currentQ.getCorrect()
                        + ". " + currentQ.getChoice(currentQ.getCorrect()) + " — " + currentQ.getExplanation());
            }

            streakLabel.setText("🔥 STREAK: " + currentStreak + "x   |   SCORE: " + quizScore);
            lifelineBtn.setEnabled(false);
            hasAnswered = true;
            submitOrNextBtn.setBackground(TEAL_PRIMARY);
            submitOrNextBtn.setText(currentQuestionIndex == quizQuestions.size() - 1 ? "View Final Readiness Rank  ★" : "Next Question  →");
        };

        if (questionTimer != null) questionTimer.stop();
        questionTimer = new Timer(1000, e -> {
            if (hasAnswered) return;
            secondsLeft--;
            timerLabel.setText("⏱ " + secondsLeft + "s");
            if (secondsLeft <= 5) {
                timerLabel.setForeground(new Color(220, 38, 38));
            } else if (secondsLeft <= 10) {
                timerLabel.setForeground(new Color(146, 64, 14));
            } else {
                timerLabel.setForeground(TEAL_PRIMARY);
            }
            if (secondsLeft <= 0) {
                evaluateAnswer.run();
            }
        });

        lifelineBtn.addActionListener(e -> {
            if (fiftyFiftyUsed || hasAnswered || quizQuestions.isEmpty()) return;
            QuizQuestion currentQ = quizQuestions.get(currentQuestionIndex);
            int correctIdx = currentQ.getCorrect().charAt(0) - 'A';

            List<Integer> wrongIndices = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                if (i != correctIdx) wrongIndices.add(i);
            }
            Collections.shuffle(wrongIndices);

            for (int k = 0; k < 2; k++) {
                int hideIdx = wrongIndices.get(k);
                optionButtons[hideIdx].setEnabled(false);
                optionButtons[hideIdx].setText("—  Eliminated by 50/50");
                optionButtons[hideIdx].setForeground(TEXT_MUTED);
            }
            fiftyFiftyUsed = true;
            lifelineBtn.setEnabled(false);
            lifelineBtn.setText("⚡ 50/50 Used");
        });

        startBtn.addActionListener(e -> {
            try {
                quizQuestions = db.getQuestions((String) categoryBox.getSelectedItem());
                currentQuestionIndex = 0;
                quizScore = 0;
                currentStreak = 0;
                bestStreak = 0;
                fiftyFiftyUsed = false;

                if (quizQuestions.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No questions available for this topic.");
                    return;
                }
                streakLabel.setText("🔥 STREAK: 0x   |   SCORE: 0");
                lifelineBtn.setText("⚡ 50/50 Lifeline (1)");
                lifelineBtn.setEnabled(true);
                displayCurrentQuestion(progressLabel, progressBar, timerLabel, questionArea,
                        optionButtons, choiceGroup, feedbackArea, submitOrNextBtn, lifelineBtn);
            } catch (SQLException ex) {
                showError(ex);
            }
        });

        submitOrNextBtn.addActionListener(e -> {
            if (quizQuestions.isEmpty()) return;

            if (!hasAnswered) {
                boolean anySelected = false;
                for (JRadioButton rb : optionButtons) {
                    if (rb.isSelected()) anySelected = true;
                }
                if (!anySelected) {
                    JOptionPane.showMessageDialog(this, "Pick an option before locking in your answer!");
                    return;
                }
                evaluateAnswer.run();
            } else {
                currentQuestionIndex++;
                if (currentQuestionIndex >= quizQuestions.size()) {
                    if (questionTimer != null) questionTimer.stop();
                    progressBar.setValue(100);
                    timerLabel.setText("DONE");

                    double pct = ((double) quizScore / quizQuestions.size()) * 100.0;
                    String rankTitle = pct >= 80 ? "🏆 RANK: CLIMATE CHAMPION"
                                     : pct >= 50 ? "🛡 RANK: PREPARED CITIZEN"
                                                 : "🌱 RANK: HEAT & WATER TRAINEE";

                    progressLabel.setText("CHALLENGE COMPLETE  •  " + rankTitle);
                    questionArea.setText("Final Score: " + quizScore + " / " + quizQuestions.size()
                            + " (" + Math.round(pct) + "%)   •   Best Streak: " + bestStreak + "x");
                    for (JRadioButton btn : optionButtons) {
                        btn.setVisible(false);
                    }
                    feedbackArea.setBackground(new Color(254, 243, 199));
                    feedbackArea.setForeground(new Color(146, 64, 14));
                    feedbackArea.setText("☀ Tip: Review the Educational Articles and Preparedness Guides to boost your rank, then click Start Challenge to play again!");
                    submitOrNextBtn.setEnabled(false);
                    lifelineBtn.setEnabled(false);
                } else {
                    displayCurrentQuestion(progressLabel, progressBar, timerLabel, questionArea,
                            optionButtons, choiceGroup, feedbackArea, submitOrNextBtn, lifelineBtn);
                }
            }
        });

        switchMainPage(page);
    }

    private void displayCurrentQuestion(JLabel progressLabel, JProgressBar progressBar, JLabel timerLabel,
                                        JTextArea questionArea, JRadioButton[] optionButtons, ButtonGroup choiceGroup,
                                        JTextArea feedbackArea, JButton actionBtn, JButton lifelineBtn) {
        QuizQuestion q = quizQuestions.get(currentQuestionIndex);
        int percent = (int) (((double) currentQuestionIndex / quizQuestions.size()) * 100);
        progressBar.setValue(percent);
        progressLabel.setText("QUESTION " + (currentQuestionIndex + 1) + " OF " + quizQuestions.size()
                + "   •   TOPIC: " + q.getCategory().toUpperCase());
        questionArea.setText(q.getQuestion());

        String[] choices = q.getChoices();
        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText((char) ('A' + i) + ".   " + choices[i]);
            optionButtons[i].setEnabled(true);
            optionButtons[i].setForeground(TEXT_MAIN);
            optionButtons[i].setBackground(new Color(248, 250, 252));
            optionButtons[i].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_LIGHT),
                new EmptyBorder(10, 14, 10, 14)
            ));
            optionButtons[i].setVisible(true);
        }

        choiceGroup.clearSelection();
        feedbackArea.setBackground(Color.WHITE);
        feedbackArea.setText("");
        hasAnswered = false;
        lifelineBtn.setEnabled(!fiftyFiftyUsed);
        actionBtn.setBackground(NAVY_TOP);
        actionBtn.setText("Lock In Answer");
        actionBtn.setEnabled(true);

        secondsLeft = 20;
        timerLabel.setText("⏱ 20s");
        timerLabel.setForeground(TEAL_PRIMARY);
        if (questionTimer != null) questionTimer.restart();
    }

    // =========================================================================
    // 5. ABOUT PAGE & FULL-PAGE ADMIN PORTAL
    // =========================================================================

    private void showAbout() {
        setActiveNav("About");
        JPanel page = createPageContainer("About SINAG", "El Niño Awareness and Preparedness System");

        RoundedPanel card = new RoundedPanel(18, Color.WHITE, BORDER_LIGHT);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(new EmptyBorder(26, 28, 26, 28));

        JLabel missionHeader = new JLabel("☀  System Objectives & Scope");
        missionHeader.setFont(new Font("SansSerif", Font.BOLD, 17));
        missionHeader.setForeground(NAVY_BOTTOM);

        JTextArea aboutText = new JTextArea(
            "SINAG is an educational desktop system designed to promote climate awareness, water conservation, "
            + "and household preparedness during El Niño conditions.\n\n"
            + "Core Capabilities:\n"
            + "• Educational Content Library: Structured Articles, Public Advisories, FAQs, and Preparedness Guides with cloud image support.\n"
            + "• Keyword & Topic Search: Instant filtering to locate relevant heat-safety and water-saving guidance.\n"
            + "• Interactive Single-Player Quiz: Timed readiness challenge with streaks, lifelines, live scoring, and explanations.\n"
            + "• Administrator Management: Secure portal for updating educational materials, images, and quiz questions.\n\n"
            + "Disclaimer: Always verify official weather warnings and community announcements with PAGASA and your Local Government Unit (LGU)."
        );
        aboutText.setWrapStyleWord(true);
        aboutText.setLineWrap(true);
        aboutText.setEditable(false);
        aboutText.setFont(new Font("SansSerif", Font.PLAIN, 14));
        aboutText.setForeground(TEXT_MAIN);
        aboutText.setBackground(Color.WHITE);

        card.add(missionHeader, BorderLayout.NORTH);
        card.add(new JScrollPane(aboutText), BorderLayout.CENTER);

        page.add(card, BorderLayout.CENTER);
        switchMainPage(page);
    }

    private void openAdminLogin() {
        JDialog loginDialog = new JDialog(this, "SINAG Administrator Login", true);
        loginDialog.setSize(360, 220);
        loginDialog.setLocationRelativeTo(this);
        loginDialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel fields = new JPanel(new GridLayout(2, 2, 8, 10));
        fields.setOpaque(false);
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        fields.add(new JLabel("Username:"));
        fields.add(userField);
        fields.add(new JLabel("Password:"));
        fields.add(passField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton cancelBtn = createStyledButton("Cancel", TEXT_MUTED);
        JButton loginBtn = createStyledButton("Login", TEAL_PRIMARY);

        Runnable attemptLogin = () -> {
            if (userField.getText().trim().equals("admin") && new String(passField.getPassword()).equals("sinag123")) {
                loginDialog.dispose();
                showAdminDashboard();
            } else {
                JOptionPane.showMessageDialog(loginDialog, "Invalid username or password.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            }
        };

        cancelBtn.addActionListener(e -> loginDialog.dispose());
        loginBtn.addActionListener(e -> attemptLogin.run());
        passField.addActionListener(e -> attemptLogin.run());

        buttons.add(cancelBtn);
        buttons.add(loginBtn);

        panel.add(fields, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        loginDialog.add(panel);
        loginDialog.setVisible(true);
    }

    private void showAdminDashboard() {
        setActiveNav("");
        JPanel page = createPageContainer("Administrator Portal", "Add, update, or remove educational content, images, and quiz questions.");

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topBar.setOpaque(false);
        JButton logoutBtn = createStyledButton("←  Exit Admin Portal", NAVY_TOP);
        logoutBtn.addActionListener(e -> showHome());
        topBar.add(logoutBtn);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 12));
        tabs.addTab("▤  Educational Content", createContentAdminTab());
        tabs.addTab("★  Quiz Questions", createQuizAdminTab());

        JPanel centerWrapper = new JPanel(new BorderLayout(0, 10));
        centerWrapper.setOpaque(false);
        centerWrapper.add(topBar, BorderLayout.NORTH);
        centerWrapper.add(tabs, BorderLayout.CENTER);

        page.add(centerWrapper, BorderLayout.CENTER);
        switchMainPage(page);
    }

    private JPanel createContentAdminTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] columns = {"ID", "Type", "Category", "Title", "Published Date", "Image"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = styleModernTable(new JTable(tableModel));
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        List<Content> cachedList = new ArrayList<>();
        Runnable refreshTable = () -> {
            tableModel.setRowCount(0);
            cachedList.clear();
            try {
                cachedList.addAll(db.getContent("All", "", "All"));
                for (Content c : cachedList) {
                    String imgStatus = (c.getImagePath() == null || c.getImagePath().isBlank()) ? "None" : "Attached";
                    tableModel.addRow(new Object[]{
                        c.getId(), c.getType(), c.getCategory(), c.getTitle(), c.getPublishedDate(), imgStatus
                    });
                }
            } catch (SQLException ex) {
                showError(ex);
            }
        };

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);

        JButton addBtn = createStyledButton("+ Add Content", TEAL_PRIMARY);
        JButton editBtn = createStyledButton("✎ Edit Selected", NAVY_TOP);
        JButton deleteBtn = createStyledButton("✕ Delete Selected", new Color(220, 38, 38));

        addBtn.addActionListener(e -> {
            if (openContentEditor(null)) refreshTable.run();
        });

        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a content row from the table first.");
                return;
            }
            if (openContentEditor(cachedList.get(row))) refreshTable.run();
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a content row to delete.");
                return;
            }
            Content selected = cachedList.get(row);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete \"" + selected.getTitle() + "\"?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    db.deleteContent(selected.getId());
                    refreshTable.run();
                } catch (SQLException ex) {
                    showError(ex);
                }
            }
        });

        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(deleteBtn);

        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        refreshTable.run();
        return panel;
    }

    private boolean openContentEditor(Content existing) {
        final boolean[] isSaved = {false};
        final String[] chosenImagePath = {existing == null ? "" : existing.getImagePath()};

        JDialog dialog = new JDialog(this, existing == null ? "Add New Content" : "Edit Content", true);
        dialog.setSize(540, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel container = new JPanel(new BorderLayout(10, 12));
        container.setBorder(new EmptyBorder(18, 20, 18, 20));
        container.setBackground(Color.WHITE);

        JPanel topFields = new JPanel(new GridLayout(0, 2, 10, 8));
        topFields.setOpaque(false);

        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Article", "Advisory", "FAQ", "Guide"});
        JComboBox<String> categoryBox = new JComboBox<>(new String[]{
            "Basics", "Effects", "Water Conservation", "Heat Safety", "Preparedness", "Other"
        });
        JTextField titleField = new JTextField(existing == null ? "" : existing.getTitle());
        JTextField sourceField = new JTextField(existing == null ? "" : existing.getSource());
        JTextField dateField = new JTextField(existing == null ? LocalDate.now().toString() : existing.getPublishedDate());

        if (existing != null) {
            typeBox.setSelectedItem(existing.getType());
            categoryBox.setSelectedItem(existing.getCategory());
        }

        JPanel imagePickerRow = new JPanel(new BorderLayout(6, 0));
        imagePickerRow.setOpaque(false);
        String initialStatus = chosenImagePath[0].isBlank()
                ? "No image"
                : (chosenImagePath[0].startsWith("data:image") ? "Cloud image attached" : new File(chosenImagePath[0]).getName());
        JLabel fileStatusLabel = new JLabel(initialStatus);
        fileStatusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        fileStatusLabel.setForeground(TEXT_MUTED);

        JButton uploadBtn = createStyledButton("Browse...", NAVY_TOP);
        uploadBtn.addActionListener(e -> {
            FileDialog fileDialog = new FileDialog(dialog, "Select Image", FileDialog.LOAD);
            fileDialog.setFile("*.jpg;*.jpeg;*.png;*.gif");
            fileDialog.setVisible(true);

            String dir = fileDialog.getDirectory();
            String file = fileDialog.getFile();

            if (dir != null && file != null) {
                File sourceFile = new File(dir, file);
                try {
                    BufferedImage original = ImageIO.read(sourceFile);
                    if (original != null) {
                        Image scaled = original.getScaledInstance(480, 230, Image.SCALE_SMOOTH);
                        BufferedImage resized = new BufferedImage(480, 230, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2 = resized.createGraphics();
                        g2.drawImage(scaled, 0, 0, null);
                        g2.dispose();

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(resized, "jpg", baos);
                        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                        chosenImagePath[0] = "data:image/jpeg;base64," + base64;
                        fileStatusLabel.setText(sourceFile.getName() + " (Cloud Ready)");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Could not process image: " + ex.getMessage());
                }
            }
        });

        imagePickerRow.add(uploadBtn, BorderLayout.WEST);
        imagePickerRow.add(fileStatusLabel, BorderLayout.CENTER);

        topFields.add(new JLabel("Content Type:")); topFields.add(typeBox);
        topFields.add(new JLabel("Category:")); topFields.add(categoryBox);
        topFields.add(new JLabel("Title:")); topFields.add(titleField);
        topFields.add(new JLabel("Source:")); topFields.add(sourceField);
        topFields.add(new JLabel("Date (YYYY-MM-DD):")); topFields.add(dateField);
        topFields.add(new JLabel("Image (Optional):")); topFields.add(imagePickerRow);

        JPanel bodyWrapper = new JPanel(new BorderLayout(0, 6));
        bodyWrapper.setOpaque(false);

        JPanel bodyHeader = new JPanel(new BorderLayout());
        bodyHeader.setOpaque(false);
        bodyHeader.add(new JLabel("Body / Full Text:"), BorderLayout.WEST);

        JButton boldBtn = new JButton("B");
        boldBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        boldBtn.setToolTipText("Highlight text and click B to bold it (**text**)");
        boldBtn.setFocusPainted(false);
        boldBtn.setBackground(SIDEBAR_BG);
        boldBtn.setForeground(NAVY_BOTTOM);
        boldBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT),
            new EmptyBorder(3, 10, 3, 10)
        ));
        boldBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bodyHeader.add(boldBtn, BorderLayout.EAST);

        bodyWrapper.add(bodyHeader, BorderLayout.NORTH);

        JTextArea bodyArea = new JTextArea(existing == null ? "" : existing.getBody());
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        bodyArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        bodyArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        boldBtn.addActionListener(e -> {
            String selectedText = bodyArea.getSelectedText();
            if (selectedText != null && !selectedText.isEmpty()) {
                bodyArea.replaceSelection("**" + selectedText + "**");
            } else {
                int pos = bodyArea.getCaretPosition();
                bodyArea.insert("****", pos);
                bodyArea.setCaretPosition(pos + 2);
            }
            bodyArea.requestFocusInWindow();
        });

        JScrollPane bodyScroll = new JScrollPane(bodyArea);
        bodyScroll.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT));
        bodyWrapper.add(bodyScroll, BorderLayout.CENTER);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonBar.setOpaque(false);
        JButton cancelBtn = createStyledButton("Cancel", TEXT_MUTED);
        JButton saveBtn = createStyledButton("Save Content", TEAL_PRIMARY);

        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            if (titleField.getText().isBlank() || bodyArea.getText().isBlank()) {
                JOptionPane.showMessageDialog(dialog, "Title and body are required.");
                return;
            }
            try {
                if (existing == null) {
                    db.addContent((String) typeBox.getSelectedItem(), titleField.getText().trim(),
                            bodyArea.getText().trim(), (String) categoryBox.getSelectedItem(),
                            sourceField.getText().trim(), dateField.getText().trim(), chosenImagePath[0]);
                } else {
                    db.updateContent(existing.getId(), (String) typeBox.getSelectedItem(), titleField.getText().trim(),
                            bodyArea.getText().trim(), (String) categoryBox.getSelectedItem(),
                            sourceField.getText().trim(), dateField.getText().trim(), chosenImagePath[0]);
                }
                isSaved[0] = true;
                dialog.dispose();
            } catch (SQLException ex) {
                showError(ex);
            }
        });

        buttonBar.add(cancelBtn);
        buttonBar.add(saveBtn);

        container.add(topFields, BorderLayout.NORTH);
        container.add(bodyWrapper, BorderLayout.CENTER);
        container.add(buttonBar, BorderLayout.SOUTH);

        dialog.add(container);
        dialog.setVisible(true);
        return isSaved[0];
    }

    private JPanel createQuizAdminTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] columns = {"ID", "Category", "Question", "Correct Answer"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = styleModernTable(new JTable(tableModel));
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(3).setMaxWidth(120);

        List<QuizQuestion> cachedQuestions = new ArrayList<>();
        Runnable refreshTable = () -> {
            tableModel.setRowCount(0);
            cachedQuestions.clear();
            try {
                cachedQuestions.addAll(db.getQuestions("All"));
                for (QuizQuestion q : cachedQuestions) {
                    tableModel.addRow(new Object[]{
                        q.getId(), q.getCategory(), q.getQuestion(), q.getCorrect()
                    });
                }
            } catch (SQLException ex) {
                showError(ex);
            }
        };

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);

        JButton addBtn = createStyledButton("+ Add Question", TEAL_PRIMARY);
        JButton editBtn = createStyledButton("✎ Edit Selected", NAVY_TOP);
        JButton deleteBtn = createStyledButton("✕ Delete Selected", new Color(220, 38, 38));

        addBtn.addActionListener(e -> {
            if (openQuestionEditor(null)) refreshTable.run();
        });

        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a question row from the table first.");
                return;
            }
            if (openQuestionEditor(cachedQuestions.get(row))) refreshTable.run();
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a question row to delete.");
                return;
            }
            QuizQuestion selected = cachedQuestions.get(row);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete this question?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    db.deleteQuestion(selected.getId());
                    refreshTable.run();
                } catch (SQLException ex) {
                    showError(ex);
                }
            }
        });

        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(deleteBtn);

        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        refreshTable.run();
        return panel;
    }

    private boolean openQuestionEditor(QuizQuestion existing) {
        final boolean[] isSaved = {false};
        String[] choices = existing == null ? new String[]{"", "", "", ""} : existing.getChoices();

        JDialog dialog = new JDialog(this, existing == null ? "Add Quiz Question" : "Edit Quiz Question", true);
        dialog.setSize(520, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel container = new JPanel(new BorderLayout(10, 12));
        container.setBorder(new EmptyBorder(18, 20, 18, 20));
        container.setBackground(Color.WHITE);

        JPanel fields = new JPanel(new GridLayout(0, 2, 10, 8));
        fields.setOpaque(false);

        JComboBox<String> categoryBox = new JComboBox<>(new String[]{
            "Basics", "Effects", "Water Conservation", "Heat Safety", "Preparedness"
        });
        if (existing != null) categoryBox.setSelectedItem(existing.getCategory());

        JTextField questionField = new JTextField(existing == null ? "" : existing.getQuestion());
        JTextField choiceA = new JTextField(choices[0]);
        JTextField choiceB = new JTextField(choices[1]);
        JTextField choiceC = new JTextField(choices[2]);
        JTextField choiceD = new JTextField(choices[3]);
        JComboBox<String> correctBox = new JComboBox<>(new String[]{"A", "B", "C", "D"});
        if (existing != null) correctBox.setSelectedItem(existing.getCorrect());
        JTextField explanationField = new JTextField(existing == null ? "" : existing.getExplanation());

        fields.add(new JLabel("Category:")); fields.add(categoryBox);
        fields.add(new JLabel("Question:")); fields.add(questionField);
        fields.add(new JLabel("Choice A:")); fields.add(choiceA);
        fields.add(new JLabel("Choice B:")); fields.add(choiceB);
        fields.add(new JLabel("Choice C:")); fields.add(choiceC);
        fields.add(new JLabel("Choice D:")); fields.add(choiceD);
        fields.add(new JLabel("Correct Answer:")); fields.add(correctBox);
        fields.add(new JLabel("Explanation:")); fields.add(explanationField);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonBar.setOpaque(false);
        JButton cancelBtn = createStyledButton("Cancel", TEXT_MUTED);
        JButton saveBtn = createStyledButton("Save Question", TEAL_PRIMARY);

        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            if (questionField.getText().isBlank() || choiceA.getText().isBlank() || choiceB.getText().isBlank()
                    || choiceC.getText().isBlank() || choiceD.getText().isBlank() || explanationField.getText().isBlank()) {
                JOptionPane.showMessageDialog(dialog, "Please complete all fields.");
                return;
            }
            try {
                if (existing == null) {
                    db.addQuestion((String) categoryBox.getSelectedItem(), questionField.getText().trim(),
                            choiceA.getText().trim(), choiceB.getText().trim(),
                            choiceC.getText().trim(), choiceD.getText().trim(),
                            (String) correctBox.getSelectedItem(), explanationField.getText().trim());
                } else {
                    db.updateQuestion(existing.getId(), (String) categoryBox.getSelectedItem(), questionField.getText().trim(),
                            choiceA.getText().trim(), choiceB.getText().trim(),
                            choiceC.getText().trim(), choiceD.getText().trim(),
                            (String) correctBox.getSelectedItem(), explanationField.getText().trim());
                }
                isSaved[0] = true;
                dialog.dispose();
            } catch (SQLException ex) {
                showError(ex);
            }
        });

        buttonBar.add(cancelBtn);
        buttonBar.add(saveBtn);

        container.add(fields, BorderLayout.CENTER);
        container.add(buttonBar, BorderLayout.SOUTH);

        dialog.add(container);
        dialog.setVisible(true);
        return isSaved[0];
    }

    // =========================================================================
    // 6. CUSTOM GRAPHICS COMPONENTS & UI HELPERS
    // =========================================================================

    private JTable styleModernTable(JTable table) {
        table.setRowHeight(30);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setGridColor(BORDER_LIGHT);
        table.setSelectionBackground(new Color(204, 244, 237));
        table.setSelectionForeground(NAVY_BOTTOM);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(SIDEBAR_BG);
        table.getTableHeader().setForeground(NAVY_BOTTOM);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return c;
            }
        });
        return table;
    }

    private JPanel createPageContainer(String title, String subtitle) {
        JPanel page = new JPanel(new BorderLayout(12, 16));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(22, 28, 20, 28));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(NAVY_BOTTOM);

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subLabel.setForeground(TEXT_MUTED);

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(subLabel);
        page.add(header, BorderLayout.NORTH);

        return page;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text + ":");
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setForeground(NAVY_BOTTOM);
        return label;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(9, 15, 9, 15));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String formatBodyAsHtml(String rawBody) {
        String safe = escapeHtml(rawBody == null ? "" : rawBody);
        safe = safe.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        safe = safe.replace("\n", "<br>");
        return "<html><body style='margin:0; padding:0; width:100%; line-height:1.45; text-align:justify;'>" + safe + "</body></html>";
    }

    private void switchMainPage(JPanel newPage) {
        if (questionTimer != null && questionTimer.isRunning()) {
            questionTimer.stop();
        }
        mainContentArea.removeAll();
        mainContentArea.add(newPage, BorderLayout.CENTER);
        mainContentArea.revalidate();
        mainContentArea.repaint();
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "SINAG Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }

    // --- CUSTOM PAINTED UI CLASSES ---

    private static class ContentPageBanner extends JPanel {
        ContentPageBanner(String symbol, String title, String subtitle) {
            setOpaque(false);
            setLayout(new BorderLayout(16, 0));
            setBorder(new EmptyBorder(14, 20, 14, 24));

            JLabel iconTile = new JLabel(symbol, SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setPaint(new GradientPaint(0, 0, new Color(253, 224, 71), getWidth(), getHeight(), SOLAR_GOLD_DK));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            iconTile.setPreferredSize(new Dimension(54, 54));
            iconTile.setFont(new Font("SansSerif", Font.BOLD, 24));
            iconTile.setForeground(NAVY_TOP);

            JPanel textStack = new JPanel();
            textStack.setOpaque(false);
            textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));

            JLabel titleLbl = new JLabel(title);
            titleLbl.setFont(new Font("SansSerif", Font.BOLD, 24));
            titleLbl.setForeground(NAVY_BOTTOM);

            JLabel subLbl = new JLabel(subtitle);
            subLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
            subLbl.setForeground(new Color(62, 82, 112));

            textStack.add(titleLbl);
            textStack.add(Box.createVerticalStrut(3));
            textStack.add(subLbl);

            // Translucent Shield Badge on Right
            JLabel shieldMark = new JLabel("🛡", SwingConstants.CENTER);
            shieldMark.setFont(new Font("SansSerif", Font.PLAIN, 44));
            shieldMark.setForeground(new Color(255, 255, 255, 190));

            add(iconTile, BorderLayout.WEST);
            add(textStack, BorderLayout.CENTER);
            add(shieldMark, BorderLayout.EAST);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            g2.setPaint(new GradientPaint(0, 0, new Color(238, 246, 255), w, 0, new Color(172, 212, 250)));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 20, 20);

            // Diagonal light swoosh
            g2.setColor(new Color(255, 255, 255, 115));
            Path2D swoosh = new Path2D.Double();
            swoosh.moveTo(w * 0.45, h);
            swoosh.curveTo(w * 0.65, h * 0.35, w * 0.85, h * 0.25, w, 0);
            swoosh.lineTo(w, h);
            swoosh.closePath();
            g2.fill(swoosh);

            g2.setColor(new Color(196, 218, 242));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class ContentListItemCard extends JPanel {
        private final boolean selected;

        ContentListItemCard(Content item, int number, boolean selected) {
            this.selected = selected;
            setOpaque(false);
            setLayout(new BorderLayout(10, 0));
            setBorder(new EmptyBorder(8, 10, 8, 10));

            // Left Category/Type Symbol
            JLabel leadIcon = new JLabel("✦", SwingConstants.CENTER);
            leadIcon.setFont(new Font("SansSerif", Font.BOLD, 14));
            leadIcon.setForeground(selected ? NAVY_BOTTOM : NAVY_TOP);
            leadIcon.setPreferredSize(new Dimension(24, 24));

            // Middle Title + Category Subtitle
            JLabel textLbl = new JLabel("<html><div style='width:162px;'>"
                    + "<b style='font-size:11px; color:#041834;'>" + escape(item.getTitle()) + "</b><br>"
                    + "<span style='font-size:9px; color:#5C6C84;'>" + escape(item.getCategory()) + "</span>"
                    + "</div></html>");

            // Right Circular Number Pill Badge
            JPanel badgeWrap = new JPanel(new GridBagLayout());
            badgeWrap.setOpaque(false);

            JLabel countBadge = new JLabel(String.valueOf(number), SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(selected ? SOLAR_GOLD : new Color(224, 236, 250));
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            countBadge.setPreferredSize(new Dimension(24, 24));
            countBadge.setFont(new Font("SansSerif", Font.BOLD, 10));
            countBadge.setForeground(NAVY_BOTTOM);
            badgeWrap.add(countBadge);

            add(leadIcon, BorderLayout.WEST);
            add(textLbl, BorderLayout.CENTER);
            add(badgeWrap, BorderLayout.EAST);
        }

        private static String escape(String s) {
            return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int padX = 2;
            int padY = 3;
            int w = getWidth() - padX * 2;
            int h = getHeight() - padY * 2;

            if (selected) {
                g2.setPaint(new GradientPaint(padX, padY, new Color(254, 238, 178), padX + w, padY + h, new Color(249, 212, 92)));
                g2.fillRoundRect(padX, padY, w, h, 14, 14);
                g2.setColor(SOLAR_GOLD_DK);
            } else {
                g2.setColor(new Color(246, 250, 255));
                g2.fillRoundRect(padX, padY, w, h, 14, 14);
                g2.setColor(new Color(222, 232, 244));
            }
            g2.drawRoundRect(padX, padY, w - 1, h - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class CommunityIllustrationFooter extends JPanel {
        CommunityIllustrationFooter() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            Shape clip = new RoundRectangle2D.Double(0, 4, w, h - 4, 16, 16);
            g2.setClip(clip);

            // Sky gradient
            g2.setPaint(new GradientPaint(0, 4, new Color(166, 212, 248), 0, h, new Color(214, 236, 252)));
            g2.fillRect(0, 4, w, h);

            // Distant blue hills
            g2.setColor(new Color(110, 166, 218));
            Path2D hills = new Path2D.Double();
            hills.moveTo(0, h * 0.62);
            hills.curveTo(w * 0.28, h * 0.35, w * 0.65, h * 0.52, w, h * 0.42);
            hills.lineTo(w, h);
            hills.lineTo(0, h);
            hills.closePath();
            g2.fill(hills);

            // Green foreground lawn
            g2.setColor(new Color(44, 138, 86));
            g2.fillRoundRect(-10, (int) (h * 0.72), w + 20, h, 24, 24);

            // Stylized community house
            int hx = (int) (w * 0.28);
            int hy = (int) (h * 0.44);
            g2.setColor(new Color(252, 243, 218));
            g2.fillRect(hx, hy + 14, 62, 30);
            g2.setColor(new Color(28, 72, 128));
            Polygon roof = new Polygon(
                new int[]{hx - 6, hx + 31, hx + 68},
                new int[]{hy + 15, hy, hy + 15},
                3
            );
            g2.fillPolygon(roof);

            // Windows
            g2.setColor(SOLAR_GOLD);
            g2.fillRect(hx + 12, hy + 21, 10, 10);
            g2.fillRect(hx + 40, hy + 21, 10, 10);

            g2.setClip(null);
            g2.dispose();
        }
    }

    private static class RoundedImagePreview extends JPanel {
        private BufferedImage image;

        RoundedImagePreview() {
            setOpaque(false);
            setBorder(new EmptyBorder(4, 0, 14, 0));
        }

        public void setImage(BufferedImage img) {
            this.image = img;
            if (img != null) {
                setPreferredSize(new Dimension(540, 225));
                setMaximumSize(new Dimension(680, 235));
            } else {
                setPreferredSize(new Dimension(0, 0));
                setMaximumSize(new Dimension(0, 0));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int w = Math.min(getWidth(), 640);
            int h = getHeight() - 14;
            if (w <= 0 || h <= 0) {
                g2.dispose();
                return;
            }

            Shape clip = new RoundRectangle2D.Double(0, 2, w, h, 20, 20);
            g2.setClip(clip);
            g2.drawImage(image, 0, 2, w, h, this);
            g2.setClip(null);
            g2.setColor(BORDER_LIGHT);
            g2.drawRoundRect(0, 2, w - 1, h - 1, 20, 20);
            g2.dispose();
        }
    }

    private static class RoundedWorkspacePanel extends JPanel {
        RoundedWorkspacePanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BG_CANVAS);
            g2.fillRoundRect(0, 0, getWidth() + 24, getHeight() + 24, 36, 36);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class NavPillButton extends JButton {
        private final String pageKey;
        private boolean active = false;

        NavPillButton(String icon, String text, String pageKey) {
            super(icon + "   " + text);
            this.pageKey = pageKey;
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 16, 10, 14));
            setForeground(Color.WHITE);
        }

        public String getPageKey() {
            return pageKey;
        }

        public void setActive(boolean active) {
            this.active = active;
            setForeground(active ? NAVY_BOTTOM : new Color(230, 240, 252));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) {
                g2.setPaint(new GradientPaint(0, 0, SOLAR_GOLD, getWidth(), 0, new Color(252, 211, 77)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(255, 255, 255, 22));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class PillButton extends JButton {
        private final Color baseColor;
        private final Color hoverColor;

        PillButton(String text, Color baseColor, Color hoverColor, Color textColor) {
            super(text);
            this.baseColor = baseColor;
            this.hoverColor = hoverColor;
            setForeground(textColor);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? hoverColor : baseColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private class HeroBannerPanel extends JPanel {
        private BufferedImage backgroundImage;

        HeroBannerPanel(String imagePath) {
            setOpaque(false);
            try {
                File imageFile = new File(imagePath);
                if (imageFile.isFile()) backgroundImage = ImageIO.read(imageFile);
                if (backgroundImage == null) {
                    java.net.URL resource = getClass().getResource("/images/hero-drought.png");
                    if (resource != null) backgroundImage = ImageIO.read(resource);
                }
            } catch (Exception ignored) {
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            Shape clip = new RoundRectangle2D.Double(0, 0, w, h, 24, 24);
            g2.setClip(clip);

            if (backgroundImage != null) {
                g2.drawImage(backgroundImage, 0, 0, w, h, this);
                g2.setPaint(new GradientPaint(
                    0, 0, new Color(10, 42, 82, 235),
                    w * 0.72f, 0, new Color(14, 72, 128, 75)
                ));
                g2.fillRect(0, 0, w, h);
            } else {
                g2.setPaint(new GradientPaint(0, 0, new Color(11, 52, 102), w, h, new Color(24, 118, 178)));
                g2.fillRect(0, 0, w, h);
            }

            g2.setClip(null);
            g2.setColor(new Color(180, 206, 234));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 24, 24);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class GlassWeatherPanel extends JPanel {
        GlassWeatherPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, new Color(36, 78, 122, 165), 0, h, new Color(22, 54, 92, 195)));
            g2.fillRoundRect(0, 0, w, h, 20, 20);
            g2.setColor(new Color(255, 255, 255, 95));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class SunCloudGraphic extends JLabel {
        SunCloudGraphic() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(253, 224, 71, 80));
            g2.fillOval(2, 2, 32, 32);
            g2.setPaint(new GradientPaint(5, 5, new Color(254, 240, 138), 28, 28, SOLAR_GOLD));
            g2.fillOval(6, 6, 24, 24);

            g2.setColor(new Color(248, 250, 252, 240));
            g2.fillOval(18, 18, 18, 16);
            g2.fillOval(28, 20, 16, 14);
            g2.fillRoundRect(12, 24, 36, 14, 14, 14);

            g2.dispose();
        }
    }

    private static class GradientModuleCard extends JPanel {
        private final ModuleCardStyle style;
        private boolean hovered = false;

        GradientModuleCard(ModuleCardStyle style) {
            this.style = style;
            setOpaque(false);
        }

        public void setHovered(boolean hovered) {
            this.hovered = hovered;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(new Color(15, 41, 78, hovered ? 28 : 14));
            g2.fillRoundRect(2, 4, w - 4, h - 4, 22, 22);

            g2.setPaint(new GradientPaint(0, 0, style.gradStart, w, h, style.gradEnd));
            g2.fillRoundRect(0, 0, w - 2, h - 3, 20, 20);

            g2.setColor(new Color(255, 255, 255, 55));
            g2.fillOval(w - 115, -20, 130, 130);

            g2.setColor(hovered ? style.circleColor : style.borderColor);
            g2.setStroke(new BasicStroke(hovered ? 1.8f : 1.2f));
            g2.drawRoundRect(0, 0, w - 3, h - 4, 20, 20);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class ReminderBannerPanel extends JPanel {
        ReminderBannerPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            g2.setPaint(new GradientPaint(0, 0, new Color(254, 243, 199), w, 0, new Color(252, 211, 77)));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);

            g2.setColor(new Color(217, 155, 28, 55));
            int[] bx = {w - 160, w - 138, w - 118, w - 95, w - 72, w - 50, w - 30};
            int[] bw = {18, 16, 20, 18, 18, 16, 22};
            int[] bh = {22, 32, 18, 28, 20, 30, 16};
            for (int i = 0; i < bx.length; i++) {
                g2.fillRect(bx[i], h - bh[i] - 2, bw[i], bh[i]);
            }

            g2.setColor(new Color(245, 190, 66));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class ScrollablePanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }
        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }
        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 64;
        }
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static class RoundedPanel extends JPanel {
        private final int cornerRadius;
        private Color fillColor;
        private Color borderColor;

        RoundedPanel(int cornerRadius, Color fillColor, Color borderColor) {
            this.cornerRadius = cornerRadius;
            this.fillColor = fillColor;
            this.borderColor = borderColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fillColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            try {
                new SinagApp().setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "SINAG Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}