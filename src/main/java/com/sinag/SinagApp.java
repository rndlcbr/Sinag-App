package com.sinag;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * Modern, interactive Swing UI for SINAG: El Niño Awareness and Preparedness System.
 * Theme: Solar Amber (heat/sunlight awareness) + Deep Ocean & Teal (water conservation & readiness).
 */
public class SinagApp extends JFrame {

    // --- EL NIÑO CLIMATE & SOLAR THEME PALETTE ---
    private static final Color OCEAN_DARK   = new Color(15, 41, 58);     // Header & primary headings
    private static final Color OCEAN_MID    = new Color(26, 58, 79);     // Secondary dark accents
    private static final Color TEAL_PRIMARY = new Color(13, 148, 136);   // Action buttons & water theme
    private static final Color TEAL_SOFT    = new Color(204, 244, 237);  // Soft highlight tint
    private static final Color SOLAR_AMBER  = new Color(245, 158, 11);   // "Sinag" sunlight accent
    private static final Color SOLAR_SOFT   = new Color(254, 243, 199);  // Warm alert banner background
    private static final Color SOLAR_TEXT   = new Color(146, 64, 14);    // Warm dark amber text
    private static final Color BG_CANVAS    = new Color(244, 247, 250);  // Clean off-white workspace
    private static final Color SIDEBAR_BG   = new Color(235, 241, 246);  // Soft slate sidebar
    private static final Color TEXT_MAIN    = new Color(30, 41, 59);     // Readable dark slate body
    private static final Color TEXT_MUTED   = new Color(100, 116, 139);  // Subtitles & metadata
    private static final Color BORDER_LIGHT = new Color(218, 227, 235);  // Subtle card borders

    // --- CORE SERVICES & STATE ---
    private final Database db = new Database();
    private final JPanel mainContentArea = new JPanel(new BorderLayout());
    private final List<JButton> navButtons = new ArrayList<>();
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
        setTitle("SINAG: El Niño Awareness and Preparedness System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 780);
        setMinimumSize(new Dimension(960, 650));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_CANVAS);
        setLayout(new BorderLayout());

        add(createHeader(), BorderLayout.NORTH);
        add(createSidebar(), BorderLayout.WEST);

        mainContentArea.setBackground(BG_CANVAS);
        add(mainContentArea, BorderLayout.CENTER);

        showHome();
    }

    // =========================================================================
    // 1. TOP HEADER & INTERACTIVE SIDEBAR
    // =========================================================================

    /** Loads an image from the project images folder or Maven resources. */
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
                System.err.println("SINAG image not found: " + relativePath);
                return new ImageIcon();
            }
            Image scaled = original.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception ex) {
            System.err.println("Could not load " + relativePath + ": " + ex.getMessage());
            return new ImageIcon();
        }
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(OCEAN_DARK);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 3, 0, SOLAR_AMBER),
            new EmptyBorder(12, 26, 12, 26)
        ));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brandPanel.setOpaque(false);

        // Official SINAG logo image (save the new image as images/sinag-logo.jpg).
        JLabel logo = new JLabel(loadImageIcon("images/sinag-logo.jpg", 76, 88));
        logo.setBorder(new EmptyBorder(0, 0, 0, 12));

        JLabel tagline = new JLabel("   |   EL NIÑO AWARENESS & COMMUNITY READINESS");
        tagline.setFont(new Font("SansSerif", Font.BOLD, 11));
        tagline.setForeground(new Color(186, 214, 226));

        brandPanel.add(logo);
        brandPanel.add(tagline);

        JButton adminBtn = createStyledButton("⚙  ADMIN PORTAL", TEAL_PRIMARY);
        adminBtn.addActionListener(e -> openAdminLogin());

        header.add(brandPanel, BorderLayout.WEST);
        header.add(adminBtn, BorderLayout.EAST);
        return header;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_LIGHT),
            new EmptyBorder(20, 14, 18, 14)
        ));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JLabel menuLabel = new JLabel("NAVIGATION");
        menuLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        menuLabel.setForeground(TEXT_MUTED);
        menuLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuLabel.setBorder(new EmptyBorder(0, 10, 10, 0));
        sidebar.add(menuLabel);

        String[][] menuItems = {
            {"⌂", "Home", "Home"},
            {"▤", "Articles", "Article"},
            {"⚑", "Advisories", "Advisory"},
            {"?", "FAQs", "FAQ"},
            {"＋", "Preparedness Guides", "Guide"},
            {"★", "Preparedness Quiz", "Quiz"},
            {"ⓘ", "About SINAG", "About"}
        };

        navButtons.clear();
        for (String[] item : menuItems) {
            String icon = item[0];
            String label = item[1];
            String pageKey = item[2];

            JButton btn = new JButton(icon + "   " + label);
            btn.setActionCommand(pageKey);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setFont(new Font("SansSerif", Font.BOLD, 12));
            btn.setFocusPainted(false);
            btn.setOpaque(true);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!pageKey.equals(activeMenuKey)) {
                        btn.setBackground(new Color(222, 232, 240));
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (!pageKey.equals(activeMenuKey)) {
                        btn.setBackground(SIDEBAR_BG);
                    }
                }
            });

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
            sidebar.add(Box.createVerticalStrut(5));
        }

        setActiveNav(activeMenuKey);
        sidebar.add(Box.createVerticalGlue());

        JButton exitBtn = createStyledButton("⏻  Exit Application", OCEAN_MID);
        exitBtn.setHorizontalAlignment(SwingConstants.LEFT);
        exitBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        exitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        exitBtn.addActionListener(e -> dispose());
        sidebar.add(exitBtn);

        return sidebar;
    }

    private void setActiveNav(String key) {
        activeMenuKey = key;
        for (JButton btn : navButtons) {
            boolean isActive = btn.getActionCommand().equals(key);
            if (isActive) {
                btn.setBackground(Color.WHITE);
                btn.setForeground(TEAL_PRIMARY);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, SOLAR_AMBER),
                    new EmptyBorder(10, 10, 10, 8)
                ));
            } else {
                btn.setBackground(SIDEBAR_BG);
                btn.setForeground(OCEAN_DARK);
                btn.setBorder(new EmptyBorder(10, 14, 10, 8));
            }
        }
    }

    // =========================================================================
    // 2. HOME DASHBOARD (Hero + Live Weather Widget + Interactive Cards)
    // =========================================================================

    private void showHome() {
        setActiveNav("Home");
        JPanel homePanel = new JPanel(new BorderLayout(0, 18));
        homePanel.setBackground(BG_CANVAS);
        homePanel.setBorder(new EmptyBorder(24, 28, 22, 28));

        // Hero Banner with Solar-to-Ocean Gradient
        GradientPanel heroBanner = new GradientPanel("images/hero-drought.png");
        heroBanner.setLayout(new BorderLayout(20, 0));
        heroBanner.setBorder(new EmptyBorder(26, 30, 26, 30));
        heroBanner.setPreferredSize(new Dimension(900, 205));

        JPanel heroText = new JPanel();
        heroText.setOpaque(false);
        heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("EL NIÑO  •  CLIMATE AWARENESS  •  HOUSEHOLD READINESS");
        eyebrow.setForeground(new Color(253, 230, 138));
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 10));

        JLabel title = new JLabel("Be informed. Be prepared. Stay safe.");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));

        JLabel subtitle = new JLabel("Actionable climate knowledge, water-saving advisories, and readiness guides.");
        subtitle.setForeground(new Color(224, 242, 254));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));

        heroText.add(eyebrow);
        heroText.add(Box.createVerticalStrut(10));
        heroText.add(title);
        heroText.add(Box.createVerticalStrut(8));
        heroText.add(subtitle);
        heroBanner.add(heroText, BorderLayout.CENTER);

        // Right-side Live Weather Card inside Hero
        RoundedPanel weatherCard = new RoundedPanel(16, new Color(15, 41, 58, 170), new Color(255, 255, 255, 60));
        weatherCard.setLayout(new BoxLayout(weatherCard, BoxLayout.Y_AXIS));
        weatherCard.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel weatherBadge = new JLabel("☀ LIVE WEATHER");
        weatherBadge.setFont(new Font("SansSerif", Font.BOLD, 10));
        weatherBadge.setForeground(SOLAR_AMBER);
        weatherBadge.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel weatherTemp = new JLabel("Loading...");
        weatherTemp.setFont(new Font("SansSerif", Font.BOLD, 25));
        weatherTemp.setForeground(Color.WHITE);
        weatherTemp.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel weatherDetails = new JLabel("Batangas City");
        weatherDetails.setFont(new Font("SansSerif", Font.PLAIN, 11));
        weatherDetails.setForeground(new Color(204, 244, 237));
        weatherDetails.setAlignmentX(Component.RIGHT_ALIGNMENT);

        weatherCard.add(weatherBadge);
        weatherCard.add(Box.createVerticalStrut(4));
        weatherCard.add(weatherTemp);
        weatherCard.add(Box.createVerticalStrut(4));
        weatherCard.add(weatherDetails);
        heroBanner.add(weatherCard, BorderLayout.EAST);

        // Fetch live weather asynchronously
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
                        String extraPart = afterCity.substring(afterCity.indexOf("°C") + 2).trim();
                        weatherTemp.setText(tempPart);
                        weatherDetails.setText("Batangas City " + extraPart);
                    } else {
                        weatherTemp.setText("--°C");
                        weatherDetails.setText(fullText);
                    }
                } catch (Exception ex) {
                    weatherTemp.setText("--°C");
                    weatherDetails.setText("Weather unavailable");
                }
            }
        }.execute();

        // Center Section: 2x2 Interactive Feature Cards
        JPanel centerSection = new JPanel(new BorderLayout(0, 14));
        centerSection.setOpaque(false);

        JPanel sectionHeading = new JPanel();
        sectionHeading.setOpaque(false);
        sectionHeading.setLayout(new BoxLayout(sectionHeading, BoxLayout.Y_AXIS));

        JLabel exploreTitle = new JLabel("Explore SINAG Modules");
        exploreTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        exploreTitle.setForeground(OCEAN_DARK);

        JLabel exploreSub = new JLabel("Click any module below to read climate guides or test your preparedness.");
        exploreSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        exploreSub.setForeground(TEXT_MUTED);

        sectionHeading.add(exploreTitle);
        sectionHeading.add(Box.createVerticalStrut(4));
        sectionHeading.add(exploreSub);
        centerSection.add(sectionHeading, BorderLayout.NORTH);

        JPanel cardGrid = new JPanel(new GridLayout(2, 2, 16, 16));
        cardGrid.setOpaque(false);

        addInteractiveHomeCard(cardGrid, "▤", "EDUCATIONAL ARTICLES", TEAL_PRIMARY,
            "Understand what El Niño is, its global climate drivers, and local impacts.",
            "Explore Articles", () -> showContentPage("Article"));

        addInteractiveHomeCard(cardGrid, "⚑", "PUBLIC ADVISORIES", SOLAR_AMBER,
            "Stay updated on water conservation alerts and extreme heat safety reminders.",
            "View Advisories", () -> showContentPage("Advisory"));

        addInteractiveHomeCard(cardGrid, "＋", "PREPAREDNESS GUIDES", new Color(2, 132, 199),
            "Practical household checklists, emergency water storage, and cooling tips.",
            "Open Guides", () -> showContentPage("Guide"));

        addInteractiveHomeCard(cardGrid, "★", "PREPAREDNESS QUIZ", new Color(217, 119, 6),
            "Challenge yourself with interactive questions and instant explanations.",
            "Start Quiz", this::showQuiz);

        centerSection.add(cardGrid, BorderLayout.CENTER);

        RoundedPanel reminderBar = new RoundedPanel(12, SOLAR_SOFT, new Color(253, 230, 138));
        reminderBar.setLayout(new BorderLayout(8, 4));
        reminderBar.setBorder(new EmptyBorder(14, 18, 14, 18));
        reminderBar.setPreferredSize(new Dimension(900, 105));

        JLabel reminderTitle = new JLabel("☀  PREPAREDNESS TIP OF THE DAY");
        reminderTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        reminderTitle.setForeground(SOLAR_TEXT);

        JLabel reminderText = new JLabel(
            "Conserve water, stay hydrated, and follow verified climate advisories from PAGASA and your local government."
        );
        reminderText.setFont(new Font("SansSerif", Font.PLAIN, 12));
        reminderText.setForeground(OCEAN_DARK);

        JPanel reminderContent = new JPanel();
        reminderContent.setOpaque(false);
        reminderContent.setLayout(new BoxLayout(reminderContent, BoxLayout.Y_AXIS));
        reminderContent.add(reminderTitle);
        reminderContent.add(Box.createVerticalStrut(7));
        reminderContent.add(reminderText);
        reminderBar.add(reminderContent, BorderLayout.CENTER);

        homePanel.add(heroBanner, BorderLayout.NORTH);
        homePanel.add(centerSection, BorderLayout.CENTER);
        homePanel.add(reminderBar, BorderLayout.SOUTH);

        switchMainPage(homePanel);
    }

    private void addInteractiveHomeCard(JPanel parentGrid, String icon, String title, Color accentColor,
                                        String desc, String buttonText, Runnable onClickAction) {
        RoundedPanel card = new RoundedPanel(14, Color.WHITE, BORDER_LIGHT);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(20, 22, 18, 22));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel cardHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cardHeader.setOpaque(false);

        JLabel iconBadge = new JLabel(icon);
        iconBadge.setFont(new Font("SansSerif", Font.BOLD, 14));
        iconBadge.setForeground(accentColor);

        JLabel titleLabel = new JLabel("   " + title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLabel.setForeground(OCEAN_DARK);

        cardHeader.add(iconBadge);
        cardHeader.add(titleLabel);

        JTextArea descArea = new JTextArea(desc);
        descArea.setWrapStyleWord(true);
        descArea.setLineWrap(true);
        descArea.setEditable(false);
        descArea.setOpaque(false);
        descArea.setForeground(TEXT_MUTED);
        descArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        descArea.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JButton openBtn = createStyledButton(buttonText + "  →", TEAL_SOFT);
        openBtn.setForeground(new Color(15, 118, 110));
        openBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        openBtn.setBorder(new EmptyBorder(10, 16, 10, 16));
        openBtn.addActionListener(e -> onClickAction.run());

        MouseAdapter hoverEffect = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorderColor(accentColor);
                card.setFillColor(new Color(250, 253, 255));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorderColor(BORDER_LIGHT);
                card.setFillColor(Color.WHITE);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                onClickAction.run();
            }
        };
        card.addMouseListener(hoverEffect);
        descArea.addMouseListener(hoverEffect);

        card.add(cardHeader, BorderLayout.NORTH);
        card.add(descArea, BorderLayout.CENTER);
        card.add(openBtn, BorderLayout.SOUTH);

        parentGrid.add(card);
    }

    // =========================================================================
    // 3. EDUCATIONAL CONTENT READER (Search + Topic Filter + Image + Bold HTML)
    // =========================================================================

    private void showContentPage(String contentType) {
        setActiveNav(contentType);
        String pageTitle = switch (contentType) {
            case "Article"  -> "Educational Articles";
            case "Advisory" -> "Public Advisories";
            case "FAQ"      -> "Frequently Asked Questions";
            default         -> "Preparedness Guides";
        };

        JPanel page = createPageContainer(pageTitle, "Search by keyword or filter by topic to explore climate preparedness resources.");
        JPanel body = new JPanel(new BorderLayout(12, 14));
        body.setOpaque(false);

        // Search & Filter Bar
        RoundedPanel filterCard = new RoundedPanel(14, Color.WHITE, BORDER_LIGHT);
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterCard.setBorder(new EmptyBorder(6, 12, 6, 12));

        JTextField searchField = new JTextField(20);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT),
            new EmptyBorder(6, 8, 6, 8)
        ));

        JComboBox<String> categoryBox = new JComboBox<>(new String[]{
            "All", "Basics", "Effects", "Water Conservation", "Heat Safety", "Preparedness", "Other"
        });
        categoryBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        categoryBox.setBackground(Color.WHITE);

        JButton searchBtn = createStyledButton("Search", TEAL_PRIMARY);
        JButton resetBtn = createStyledButton("Reset", TEXT_MUTED);

        filterCard.add(createFieldLabel("Keyword"));
        filterCard.add(searchField);
        filterCard.add(createFieldLabel("Topic"));
        filterCard.add(categoryBox);
        filterCard.add(searchBtn);
        filterCard.add(resetBtn);

        // Left List of Records
        DefaultListModel<Content> listModel = new DefaultListModel<>();
        JList<Content> itemList = new JList<>(listModel);
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setFixedCellHeight(74);
        itemList.setBackground(Color.WHITE);

        itemList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel row = new JLabel("<html><div style='width:195px; padding:2px;'>"
                    + "<b style='font-size:11px; color:#0F293A;'>" + escapeHtml(value.getTitle()) + "</b><br>"
                    + "<span style='font-size:9px; color:#0D9488;'>● " + escapeHtml(value.getCategory()) + "</span>"
                    + "</div></html>");
            row.setOpaque(true);
            row.setVerticalAlignment(SwingConstants.CENTER);
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, isSelected ? 4 : 0, 1, 0, isSelected ? SOLAR_AMBER : BORDER_LIGHT),
                new EmptyBorder(6, 10, 6, 10)
            ));
            row.setBackground(isSelected ? new Color(240, 253, 250) : Color.WHITE);
            return row;
        });

        // Right Reader Panel
        JPanel readerCard = new JPanel(new BorderLayout(0, 12));
        readerCard.setBackground(Color.WHITE);
        readerCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT),
            new EmptyBorder(20, 22, 20, 22)
        ));

        JLabel readerTitle = new JLabel("Select an item on the left to read");
        readerTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        readerTitle.setForeground(OCEAN_DARK);

        JLabel readerMeta = new JLabel("Category  •  Published Date  •  Source");
        readerMeta.setFont(new Font("SansSerif", Font.BOLD, 11));
        readerMeta.setForeground(TEAL_PRIMARY);

        JPanel readerHeader = new JPanel();
        readerHeader.setOpaque(false);
        readerHeader.setLayout(new BoxLayout(readerHeader, BoxLayout.Y_AXIS));
        readerHeader.add(readerTitle);
        readerHeader.add(Box.createVerticalStrut(4));
        readerHeader.add(readerMeta);
        readerHeader.add(Box.createVerticalStrut(10));
        readerHeader.add(new JSeparator());

        // Scrollable container locked to viewport width with clean white background
        ScrollablePanel scrollableContent = new ScrollablePanel();
        scrollableContent.setLayout(new BoxLayout(scrollableContent, BoxLayout.Y_AXIS));
        scrollableContent.setBackground(Color.WHITE);

        JLabel readerImageLabel = new JLabel();
        readerImageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        readerImageLabel.setBorder(new EmptyBorder(4, 0, 14, 0));
        readerImageLabel.setVisible(false);

        JEditorPane readingPane = new JEditorPane("text/html", "");
        readingPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        readingPane.setFont(new Font("SansSerif", Font.PLAIN, 14));
        readingPane.setForeground(TEXT_MAIN);
        readingPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        readingPane.setEditable(false);
        readingPane.setOpaque(true);
        readingPane.setBackground(Color.WHITE);
        readingPane.setText(formatBodyAsHtml("Choose any article, advisory, FAQ, or preparedness guide from the list to view its full details here."));

        scrollableContent.add(readerImageLabel);
        scrollableContent.add(readingPane);

        JScrollPane contentScroll = new JScrollPane(scrollableContent);
        contentScroll.setBorder(null);
        contentScroll.setBackground(Color.WHITE);
        contentScroll.getViewport().setBackground(Color.WHITE);
        contentScroll.getVerticalScrollBar().setUnitIncrement(16);
        contentScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        readerCard.add(readerHeader, BorderLayout.NORTH);
        readerCard.add(contentScroll, BorderLayout.CENTER);

        itemList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && itemList.getSelectedValue() != null) {
                Content selected = itemList.getSelectedValue();
                readerTitle.setText(selected.getTitle());
                readerMeta.setText(selected.getCategory().toUpperCase()
                        + "   |   Published: " + selected.getPublishedDate()
                        + "   |   Source: " + selected.getSource());
                readingPane.setText(formatBodyAsHtml(selected.getBody()));

                // Only display image label when a valid image exists; otherwise collapse it completely
                String imgPath = selected.getImagePath();
                boolean hasValidImage = false;
                if (imgPath != null && !imgPath.isBlank()) {
                    try {
                        if (imgPath.startsWith("data:image")) {
                            String base64Data = imgPath.substring(imgPath.indexOf(",") + 1);
                            byte[] imgBytes = Base64.getDecoder().decode(base64Data);
                            readerImageLabel.setIcon(new ImageIcon(imgBytes));
                            hasValidImage = true;
                        } else if (new File(imgPath).exists()) {
                            ImageIcon rawIcon = new ImageIcon(imgPath);
                            Image scaled = rawIcon.getImage().getScaledInstance(480, 230, Image.SCALE_SMOOTH);
                            readerImageLabel.setIcon(new ImageIcon(scaled));
                            hasValidImage = true;
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (hasValidImage) {
                    readerImageLabel.setVisible(true);
                } else {
                    readerImageLabel.setIcon(null);
                    readerImageLabel.setVisible(false);
                }

                scrollableContent.revalidate();
                scrollableContent.repaint();
                SwingUtilities.invokeLater(() -> contentScroll.getVerticalScrollBar().setValue(0));
            }
        });

        Runnable reloadItems = () -> {
            listModel.clear();
            readerImageLabel.setIcon(null);
            readerImageLabel.setVisible(false);
            try {
                List<Content> records = db.getContent(contentType, searchField.getText().trim(), (String) categoryBox.getSelectedItem());
                for (Content c : records) {
                    listModel.addElement(c);
                }
                if (!records.isEmpty()) {
                    itemList.setSelectedIndex(0);
                } else {
                    readerTitle.setText("No matching records found");
                    readerMeta.setText("Try clearing your search keyword or selecting 'All' topics");
                    readingPane.setText(formatBodyAsHtml(""));
                }
            } catch (SQLException ex) {
                showError(ex);
            }
        };

        searchBtn.addActionListener(e -> reloadItems.run());
        searchField.addActionListener(e -> reloadItems.run());
        categoryBox.addActionListener(e -> reloadItems.run());
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            categoryBox.setSelectedIndex(0);
            reloadItems.run();
        });

        JScrollPane leftScroll = new JScrollPane(itemList);
        leftScroll.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT));
        leftScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setMinimumSize(new Dimension(265, 0));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, readerCard);
        splitPane.setDividerLocation(275);
        splitPane.setResizeWeight(0.30);
        splitPane.setDividerSize(8);
        splitPane.setBorder(null);

        body.add(filterCard, BorderLayout.NORTH);
        body.add(splitPane, BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);

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

        // Top Game Bar (Topic Selector + Start + 50/50 Lifeline + Streak Badge)
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
        JButton lifelineBtn = createStyledButton("⚡ 50/50 Lifeline (1)", SOLAR_AMBER);
        lifelineBtn.setEnabled(false);

        leftControls.add(createFieldLabel("Topic"));
        leftControls.add(categoryBox);
        leftControls.add(startBtn);
        leftControls.add(lifelineBtn);

        JLabel streakLabel = new JLabel("🔥 STREAK: 0x   |   SCORE: 0");
        streakLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        streakLabel.setForeground(SOLAR_TEXT);

        topControls.add(leftControls, BorderLayout.WEST);
        topControls.add(streakLabel, BorderLayout.EAST);

        // Main Quiz Card
        RoundedPanel quizCard = new RoundedPanel(18, Color.WHITE, BORDER_LIGHT);
        quizCard.setLayout(new BorderLayout(12, 16));
        quizCard.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Progress + Live Countdown Header
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
        progressBar.setForeground(SOLAR_AMBER);
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
        questionArea.setForeground(OCEAN_DARK);
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

        JButton submitOrNextBtn = createStyledButton("Lock In Answer", OCEAN_DARK);
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

        // Helper to evaluate and color-code options on submit or timeout
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

            // Highlight correct & wrong cards visually
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

        // 20-Second Per-Question Countdown Timer
        if (questionTimer != null) questionTimer.stop();
        questionTimer = new Timer(1000, e -> {
            if (hasAnswered) return;
            secondsLeft--;
            timerLabel.setText("⏱ " + secondsLeft + "s");
            if (secondsLeft <= 5) {
                timerLabel.setForeground(new Color(220, 38, 38));
            } else if (secondsLeft <= 10) {
                timerLabel.setForeground(SOLAR_TEXT);
            } else {
                timerLabel.setForeground(TEAL_PRIMARY);
            }
            if (secondsLeft <= 0) {
                evaluateAnswer.run();
            }
        });

        // 50/50 Lifeline: Disables 2 incorrect options once per game
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
                    feedbackArea.setBackground(SOLAR_SOFT);
                    feedbackArea.setForeground(SOLAR_TEXT);
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
        actionBtn.setBackground(OCEAN_DARK);
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
        missionHeader.setForeground(OCEAN_DARK);

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
        JButton logoutBtn = createStyledButton("←  Exit Admin Portal", OCEAN_MID);
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
        JButton editBtn = createStyledButton("✎ Edit Selected", OCEAN_MID);
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

        // Native Windows File Explorer picker + Base64 Cloud Encoder
        JPanel imagePickerRow = new JPanel(new BorderLayout(6, 0));
        imagePickerRow.setOpaque(false);
        String initialStatus = chosenImagePath[0].isBlank()
                ? "No image"
                : (chosenImagePath[0].startsWith("data:image") ? "Cloud image attached" : new File(chosenImagePath[0]).getName());
        JLabel fileStatusLabel = new JLabel(initialStatus);
        fileStatusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        fileStatusLabel.setForeground(TEXT_MUTED);

        JButton uploadBtn = createStyledButton("Browse...", OCEAN_MID);
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

        // Body Editor with Bold (B) formatting button
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
        boldBtn.setForeground(OCEAN_DARK);
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
        JButton editBtn = createStyledButton("✎ Edit Selected", OCEAN_MID);
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
    // 6. UI HELPERS & CUSTOM GRAPHICS COMPONENTS
    // =========================================================================

    private JTable styleModernTable(JTable table) {
        table.setRowHeight(30);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setGridColor(BORDER_LIGHT);
        table.setSelectionBackground(new Color(204, 244, 237));
        table.setSelectionForeground(OCEAN_DARK);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(SIDEBAR_BG);
        table.getTableHeader().setForeground(OCEAN_DARK);

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
        page.setBackground(BG_CANVAS);
        page.setBorder(new EmptyBorder(24, 28, 22, 28));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 23));
        titleLabel.setForeground(OCEAN_DARK);

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
        label.setForeground(OCEAN_DARK);
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
        return "<html><body style='margin:0; padding:0; width:100%; text-align:justify;'>" + safe + "</body></html>";
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

        public void setFillColor(Color fillColor) {
            this.fillColor = fillColor;
            repaint();
        }

        public void setBorderColor(Color borderColor) {
            this.borderColor = borderColor;
            repaint();
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

    private class GradientPanel extends JPanel {
        private BufferedImage backgroundImage;

        GradientPanel(String imagePath) {
            setOpaque(false);
            try {
                File imageFile = new File(imagePath);
                if (imageFile.isFile()) backgroundImage = ImageIO.read(imageFile);
                if (backgroundImage == null) {
                    java.net.URL resource = getClass().getResource("/images/hero-drought.png");
                    if (resource != null) backgroundImage = ImageIO.read(resource);
                }
                if (backgroundImage == null) {
                    System.err.println("SINAG hero image not found: " + imagePath);
                }
            } catch (Exception ex) {
                System.err.println("Could not load hero image: " + ex.getMessage());
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (backgroundImage != null) {
                g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                // Overlay keeps the white hero text readable over the photo.
                g2.setColor(new Color(8, 35, 48, 125));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            } else {
                g2.setPaint(new GradientPaint(0, 0, new Color(13, 115, 119),
                        getWidth(), getHeight(), new Color(15, 41, 58)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            }
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