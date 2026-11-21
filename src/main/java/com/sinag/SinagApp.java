package com.sinag;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main Swing GUI window for SINAG: El Niño Awareness and Preparedness System.
 */
public class SinagApp extends JFrame {

    // --- THEME COLORS ---
    private static final Color NAVY = new Color(17, 43, 66);
    private static final Color NAVY_LIGHT = new Color(29, 66, 91);
    private static final Color TEAL = new Color(0, 137, 123);
    private static final Color BACKGROUND = new Color(241, 246, 249);
    private static final Color TEXT_DARK = new Color(39, 57, 72);
    private static final Color TEXT_MUTED = new Color(111, 130, 145);
    private static final Color BORDER_COLOR = new Color(222, 231, 237);

    // --- CORE COMPONENTS ---
    private final Database db = new Database();
    private final JPanel mainContentArea = new JPanel(new BorderLayout());

    // --- QUIZ STATE VARIABLES (Stored as fields so they are easy to read and update) ---
    private List<QuizQuestion> quizQuestions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int quizScore = 0;
    private boolean hasAnswered = false;

    public SinagApp() {
        // Window setup
        setTitle("SINAG: El Niño Awareness and Preparedness System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(940, 640));
        setLocationRelativeTo(null); // Center window on screen
        getContentPane().setBackground(BACKGROUND);
        setLayout(new BorderLayout());

        // Add top header, left navigation menu, and center content area
        add(createHeader(), BorderLayout.NORTH);
        add(createSidebar(), BorderLayout.WEST);

        mainContentArea.setBackground(BACKGROUND);
        add(mainContentArea, BorderLayout.CENTER);

        // Load the Home page first
        showHome();
    }

    // =========================================================================
    // 1. HEADER & SIDEBAR NAVIGATION
    // =========================================================================

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(17, 25, 17, 25));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brandPanel.setOpaque(false);

        JLabel logo = new JLabel("☀  SINAG");
        logo.setFont(new Font("SansSerif", Font.BOLD, 25));
        logo.setForeground(Color.WHITE);

        JLabel tagline = new JLabel("   |   EL NIÑO AWARENESS & PREPAREDNESS");
        tagline.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tagline.setForeground(new Color(190, 218, 229));

        brandPanel.add(logo);
        brandPanel.add(tagline);

        JButton adminBtn = createStyledButton("ADMIN PORTAL", TEAL);
        adminBtn.addActionListener(e -> openAdminLogin());

        header.add(brandPanel, BorderLayout.WEST);
        header.add(adminBtn, BorderLayout.EAST);
        return header;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(215, 0));
        sidebar.setBackground(new Color(229, 238, 243));
        sidebar.setBorder(new EmptyBorder(22, 13, 18, 13));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JLabel menuLabel = new JLabel("  MAIN MENU");
        menuLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        menuLabel.setForeground(TEXT_MUTED);
        menuLabel.setBorder(new EmptyBorder(0, 8, 12, 0));
        sidebar.add(menuLabel);

        // Menu items: {Icon, Display Label, Page Key}
        String[][] menuItems = {
            {"⌂", "Home", "Home"},
            {"▤", "Articles", "Article"},
            {"⚑", "Advisories", "Advisory"},
            {"?", "FAQs", "FAQ"},
            {"＋", "Preparedness Guides", "Guide"},
            {"★", "Preparedness Quiz", "Quiz"},
            {"ⓘ", "About SINAG", "About"}
        };

        for (String[] item : menuItems) {
            String icon = item[0];
            String label = item[1];
            String pageKey = item[2];

            JButton navButton = new JButton(icon + "   " + label);
            navButton.setHorizontalAlignment(SwingConstants.LEFT);
            navButton.setFont(new Font("SansSerif", Font.BOLD, 12));
            navButton.setForeground(NAVY);
            navButton.setBackground(new Color(229, 238, 243));
            navButton.setBorder(new EmptyBorder(12, 12, 12, 8));
            navButton.setFocusPainted(false);
            navButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            navButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

            // Hover highlight effect
            navButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    navButton.setBackground(new Color(211, 231, 235));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    navButton.setBackground(new Color(229, 238, 243));
                }
            });

            // Switch page on click
            navButton.addActionListener(e -> {
                if (pageKey.equals("Home")) {
                    showHome();
                } else if (pageKey.equals("Quiz")) {
                    showQuiz();
                } else if (pageKey.equals("About")) {
                    showAbout();
                } else {
                    showContentPage(pageKey);
                }
            });

            sidebar.add(navButton);
            sidebar.add(Box.createVerticalStrut(5));
        }

        sidebar.add(Box.createVerticalGlue());

        JLabel infoBox = new JLabel("<html><div style='width:155px'><b>STAY INFORMED</b><br><br>"
                + "Preparedness starts with reliable information and small daily actions.</div></html>");
        infoBox.setFont(new Font("SansSerif", Font.PLAIN, 11));
        infoBox.setForeground(new Color(77, 105, 121));
        infoBox.setBorder(new EmptyBorder(12, 9, 12, 9));
        infoBox.setBackground(new Color(215, 234, 237));
        infoBox.setOpaque(true);
        sidebar.add(infoBox);
        sidebar.add(Box.createVerticalStrut(12));

        JButton exitBtn = createStyledButton("  Exit application", NAVY_LIGHT);
        exitBtn.setHorizontalAlignment(SwingConstants.LEFT);
        exitBtn.addActionListener(e -> dispose());
        sidebar.add(exitBtn);

        return sidebar;
    }

    // =========================================================================
    // 2. HOME PAGE
    // =========================================================================

    private void showHome() {
        JPanel homePanel = new JPanel(new BorderLayout(0, 18));
        homePanel.setBackground(BACKGROUND);
        homePanel.setBorder(new EmptyBorder(25, 28, 25, 28));

        // Top gradient hero banner
        GradientPanel heroBanner = new GradientPanel();
        heroBanner.setLayout(new BorderLayout());
        heroBanner.setBorder(new EmptyBorder(27, 30, 27, 30));

        // Left side of hero banner: Title and subtitle
        JPanel heroText = new JPanel();
        heroText.setOpaque(false);
        heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("EL NIÑO  •  CLIMATE AWARENESS  •  COMMUNITY READINESS");
        eyebrow.setForeground(new Color(204, 244, 237));
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 10));

        JLabel title = new JLabel("Be informed. Be prepared. Stay safe.");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 25));

        JLabel subtitle = new JLabel("Practical knowledge and preparedness resources for every household.");
        subtitle.setForeground(new Color(225, 242, 246));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));

        heroText.add(eyebrow);
        heroText.add(Box.createVerticalStrut(12));
        heroText.add(title);
        heroText.add(Box.createVerticalStrut(8));
        heroText.add(subtitle);
        heroBanner.add(heroText, BorderLayout.CENTER);

        // Right side of hero banner: Live Weather widget
        JPanel weatherBox = new JPanel();
        weatherBox.setOpaque(false);
        weatherBox.setLayout(new BoxLayout(weatherBox, BoxLayout.Y_AXIS));
        weatherBox.setBorder(new EmptyBorder(4, 16, 4, 4));

        JLabel weatherTitle = new JLabel("☀ LIVE WEATHER");
        weatherTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        weatherTitle.setForeground(new Color(204, 244, 237));
        weatherTitle.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel weatherTemp = new JLabel("Loading...");
        weatherTemp.setFont(new Font("SansSerif", Font.BOLD, 24));
        weatherTemp.setForeground(new Color(255, 231, 157));
        weatherTemp.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel weatherDetails = new JLabel("Batangas City");
        weatherDetails.setFont(new Font("SansSerif", Font.PLAIN, 12));
        weatherDetails.setForeground(new Color(225, 242, 246));
        weatherDetails.setAlignmentX(Component.RIGHT_ALIGNMENT);

        weatherBox.add(weatherTitle);
        weatherBox.add(Box.createVerticalStrut(4));
        weatherBox.add(weatherTemp);
        weatherBox.add(Box.createVerticalStrut(4));
        weatherBox.add(weatherDetails);
        heroBanner.add(weatherBox, BorderLayout.EAST);

        // Fetch live weather in the background so the UI never freezes
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return WeatherService.fetchLiveWeather();
            }

            @Override
            protected void done() {
                try {
                    // Expected format: "Batangas City: 31.2°C (Feels like 35.0°C)  •  Humidity: 74%"
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

        // Center 2x2 Feature Cards
        JPanel centerSection = new JPanel(new BorderLayout(0, 15));
        centerSection.setOpaque(false);

        JPanel sectionHeading = new JPanel();
        sectionHeading.setOpaque(false);
        sectionHeading.setLayout(new BoxLayout(sectionHeading, BoxLayout.Y_AXIS));

        JLabel exploreTitle = new JLabel("Explore SINAG");
        exploreTitle.setFont(new Font("SansSerif", Font.BOLD, 21));
        exploreTitle.setForeground(NAVY);

        JLabel exploreSub = new JLabel("Learn about climate conditions and everyday preparedness.");
        exploreSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        exploreSub.setForeground(TEXT_MUTED);

        sectionHeading.add(exploreTitle);
        sectionHeading.add(Box.createVerticalStrut(5));
        sectionHeading.add(exploreSub);
        centerSection.add(sectionHeading, BorderLayout.NORTH);

        JPanel cardGrid = new JPanel(new GridLayout(2, 2, 14, 14));
        cardGrid.setOpaque(false);

        addHomeCard(cardGrid, "▤", "EDUCATIONAL ARTICLES",
            "Understand El Niño, its causes, and possible effects.",
            "Explore articles", () -> showContentPage("Article"));

        addHomeCard(cardGrid, "⚑", "PUBLIC ADVISORIES",
            "Read reminders about water conservation and heat safety.",
            "View advisories", () -> showContentPage("Advisory"));

        addHomeCard(cardGrid, "＋", "PREPAREDNESS CENTER",
            "Find household readiness tips, FAQs, and practical guides.",
            "Open resources", () -> showContentPage("Guide"));

        addHomeCard(cardGrid, "★", "PREPAREDNESS QUIZ",
            "Test your knowledge and learn through instant feedback.",
            "Start quiz", this::showQuiz);

        centerSection.add(cardGrid, BorderLayout.CENTER);

        // Bottom reminder bar
        JLabel reminderBar = new JLabel("☀  PREPAREDNESS REMINDER   Keep drinking water available and follow verified local announcements.");
        reminderBar.setForeground(new Color(47, 102, 105));
        reminderBar.setFont(new Font("SansSerif", Font.BOLD, 11));
        reminderBar.setBorder(new EmptyBorder(13, 15, 13, 15));
        reminderBar.setBackground(new Color(222, 242, 237));
        reminderBar.setOpaque(true);

        homePanel.add(heroBanner, BorderLayout.NORTH);
        homePanel.add(centerSection, BorderLayout.CENTER);
        homePanel.add(reminderBar, BorderLayout.SOUTH);

        switchMainPage(homePanel);
    }

    private void addHomeCard(JPanel parentGrid, String icon, String title, String desc, String buttonText, Runnable onClickAction) {
        RoundedPanel card = new RoundedPanel(18, Color.WHITE);
        card.setLayout(new BorderLayout(8, 8));
        card.setBorder(new EmptyBorder(17, 18, 16, 18));

        JPanel cardHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cardHeader.setOpaque(false);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        iconLabel.setForeground(TEAL);

        JLabel titleLabel = new JLabel("  " + title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleLabel.setForeground(NAVY);

        cardHeader.add(iconLabel);
        cardHeader.add(titleLabel);

        JTextArea descArea = new JTextArea(desc);
        descArea.setWrapStyleWord(true);
        descArea.setLineWrap(true);
        descArea.setEditable(false);
        descArea.setOpaque(false);
        descArea.setForeground(TEXT_MUTED);
        descArea.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JButton openBtn = createStyledButton(buttonText + "  →", new Color(229, 244, 241));
        openBtn.setForeground(TEAL);
        openBtn.addActionListener(e -> onClickAction.run());

        card.add(cardHeader, BorderLayout.NORTH);
        card.add(descArea, BorderLayout.CENTER);
        card.add(openBtn, BorderLayout.SOUTH);

        parentGrid.add(card);
    }

    // =========================================================================
    // 3. EDUCATIONAL CONTENT PAGE (Articles, Advisories, FAQs, Guides)
    // =========================================================================

    private void showContentPage(String contentType) {
        String pageTitle;
        if (contentType.equals("Article")) {
            pageTitle = "Educational Articles";
        } else if (contentType.equals("Advisory")) {
            pageTitle = "Public Advisories";
        } else if (contentType.equals("FAQ")) {
            pageTitle = "Frequently Asked Questions";
        } else {
            pageTitle = "Preparedness Guides";
        }

        JPanel page = createPageContainer(pageTitle, "Browse learning resources and practical preparedness information.");
        JPanel body = new JPanel(new BorderLayout(12, 12));
        body.setOpaque(false);

        // Search & Category filter bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.setOpaque(false);

        JTextField searchField = new JTextField(19);
        JComboBox<String> categoryBox = new JComboBox<>(new String[]{
            "All", "Basics", "Effects", "Water Conservation", "Heat Safety", "Preparedness", "Other"
        });
        JButton searchBtn = createStyledButton("Search", TEAL);

        filterBar.add(createFieldLabel("Search"));
        filterBar.add(searchField);
        filterBar.add(createFieldLabel("Category"));
        filterBar.add(categoryBox);
        filterBar.add(searchBtn);

        // Left list of items
        DefaultListModel<Content> listModel = new DefaultListModel<>();
        JList<Content> itemList = new JList<>(listModel);
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setFixedCellHeight(48);
        itemList.setBackground(Color.WHITE);
        itemList.setFont(new Font("SansSerif", Font.PLAIN, 12));
        itemList.setBorder(new EmptyBorder(5, 5, 5, 5));

        itemList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel row = new JLabel("<html><b>" + escapeHtml(value.getTitle()) + "</b><br>"
                    + "<span style='color:#718292'>" + escapeHtml(value.getCategory()) + "</span></html>");
            row.setOpaque(true);
            row.setBorder(new EmptyBorder(8, 10, 8, 10));
            row.setBackground(isSelected ? new Color(218, 241, 236) : Color.WHITE);
            row.setForeground(NAVY);
            return row;
        });

        // Right reading pane
        JTextArea readingPane = new JTextArea("Select an item from the list to read the complete information.");
        readingPane.setLineWrap(true);
        readingPane.setWrapStyleWord(true);
        readingPane.setEditable(false);
        readingPane.setFont(new Font("SansSerif", Font.PLAIN, 13));
        readingPane.setForeground(TEXT_DARK);
        readingPane.setBackground(Color.WHITE);
        readingPane.setBorder(new EmptyBorder(18, 18, 18, 18));

        // Update reading pane when a user clicks an item
        itemList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && itemList.getSelectedValue() != null) {
                Content selected = itemList.getSelectedValue();
                readingPane.setText(selected.displayContent()
                        + "\n\nCATEGORY: " + selected.getCategory()
                        + "\nPUBLISHED: " + selected.getPublishedDate()
                        + "\nSOURCE: " + selected.getSource());
                readingPane.setCaretPosition(0);
            }
        });

        // Helper to load filtered items from SQLite
        Runnable reloadItems = () -> {
            listModel.clear();
            try {
                String keyword = searchField.getText().trim();
                String selectedCategory = (String) categoryBox.getSelectedItem();
                List<Content> records = db.getContent(contentType, keyword, selectedCategory);
                for (Content c : records) {
                    listModel.addElement(c);
                }
            } catch (SQLException ex) {
                showError(ex);
            }
            readingPane.setText("Select an item from the list to read the complete information.");
        };

        searchBtn.addActionListener(e -> reloadItems.run());
        searchField.addActionListener(e -> reloadItems.run());
        categoryBox.addActionListener(e -> reloadItems.run());

        JScrollPane leftScroll = new JScrollPane(itemList);
        JScrollPane rightScroll = new JScrollPane(readingPane);
        leftScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        rightScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll);
        splitPane.setResizeWeight(0.38);
        splitPane.setBorder(null);

        body.add(filterBar, BorderLayout.NORTH);
        body.add(splitPane, BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);

        reloadItems.run();
        switchMainPage(page);
    }

    // =========================================================================
    // 4. SINGLE-PLAYER QUIZ PAGE
    // =========================================================================

    private void showQuiz() {
        JPanel page = createPageContainer("Preparedness Quiz", "Test your knowledge of El Niño awareness and household readiness.");
        JPanel wrapper = new JPanel(new BorderLayout(12, 15));
        wrapper.setOpaque(false);

        // Category selector + Start button
        JPanel topControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topControls.setOpaque(false);

        JComboBox<String> categoryBox = new JComboBox<>(new String[]{
            "All", "Basics", "Effects", "Water Conservation", "Heat Safety", "Preparedness"
        });
        JButton startBtn = createStyledButton("▶  Start Quiz", TEAL);

        topControls.add(createFieldLabel("Quiz category"));
        topControls.add(categoryBox);
        topControls.add(startBtn);

        // Quiz card area
        RoundedPanel quizCard = new RoundedPanel(18, Color.WHITE);
        quizCard.setLayout(new BorderLayout(12, 15));
        quizCard.setBorder(new EmptyBorder(22, 24, 22, 24));

        JLabel progressLabel = new JLabel("READY WHEN YOU ARE");
        progressLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        progressLabel.setForeground(TEAL);

        JTextArea questionArea = new JTextArea("Choose a category, then press Start Quiz to begin.");
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setEditable(false);
        questionArea.setOpaque(false);
        questionArea.setForeground(NAVY);
        questionArea.setFont(new Font("SansSerif", Font.BOLD, 18));

        JPanel choicesPanel = new JPanel(new GridLayout(4, 1, 8, 8));
        choicesPanel.setOpaque(false);
        ButtonGroup choiceGroup = new ButtonGroup();
        JRadioButton[] optionButtons = new JRadioButton[4];

        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JRadioButton();
            optionButtons[i].setFont(new Font("SansSerif", Font.PLAIN, 13));
            optionButtons[i].setForeground(TEXT_DARK);
            optionButtons[i].setBackground(new Color(246, 249, 251));
            optionButtons[i].setBorder(new EmptyBorder(10, 12, 10, 12));
            optionButtons[i].setOpaque(true);
            optionButtons[i].setVisible(false);
            choiceGroup.add(optionButtons[i]);
            choicesPanel.add(optionButtons[i]);
        }

        JTextArea feedbackArea = new JTextArea();
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        feedbackArea.setEditable(false);
        feedbackArea.setOpaque(false);
        feedbackArea.setForeground(TEAL);
        feedbackArea.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JButton submitOrNextBtn = createStyledButton("Submit answer", NAVY_LIGHT);
        submitOrNextBtn.setEnabled(false);

        JPanel questionBody = new JPanel(new BorderLayout(10, 14));
        questionBody.setOpaque(false);
        questionBody.add(questionArea, BorderLayout.NORTH);
        questionBody.add(choicesPanel, BorderLayout.CENTER);
        questionBody.add(feedbackArea, BorderLayout.SOUTH);

        quizCard.add(progressLabel, BorderLayout.NORTH);
        quizCard.add(questionBody, BorderLayout.CENTER);
        quizCard.add(submitOrNextBtn, BorderLayout.SOUTH);

        wrapper.add(topControls, BorderLayout.NORTH);
        wrapper.add(quizCard, BorderLayout.CENTER);
        page.add(wrapper, BorderLayout.CENTER);

        // Start Quiz button logic
        startBtn.addActionListener(e -> {
            try {
                String selectedCategory = (String) categoryBox.getSelectedItem();
                quizQuestions = db.getQuestions(selectedCategory);
                currentQuestionIndex = 0;
                quizScore = 0;

                if (quizQuestions.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No questions available for this category.");
                    return;
                }
                displayCurrentQuestion(progressLabel, questionArea, optionButtons, choiceGroup, feedbackArea, submitOrNextBtn);
            } catch (SQLException ex) {
                showError(ex);
            }
        });

        // Submit / Next Question button logic
        submitOrNextBtn.addActionListener(e -> {
            if (quizQuestions.isEmpty()) return;

            QuizQuestion currentQ = quizQuestions.get(currentQuestionIndex);

            if (!hasAnswered) {
                // Step 1: Check selected radio button
                String userAnswer = null;
                for (int i = 0; i < 4; i++) {
                    if (optionButtons[i].isSelected()) {
                        userAnswer = String.valueOf((char) ('A' + i));
                    }
                }

                if (userAnswer == null) {
                    JOptionPane.showMessageDialog(this, "Please select an answer.");
                    return;
                }

                boolean isCorrect = currentQ.checkAnswer(userAnswer);
                if (isCorrect) {
                    quizScore++;
                    feedbackArea.setText("Correct! " + currentQ.getExplanation());
                } else {
                    feedbackArea.setText("Not quite. Correct answer: " + currentQ.getCorrect()
                            + ". " + currentQ.getChoice(currentQ.getCorrect()) + ". " + currentQ.getExplanation());
                }

                hasAnswered = true;
                if (currentQuestionIndex == quizQuestions.size() - 1) {
                    submitOrNextBtn.setText("View result");
                } else {
                    submitOrNextBtn.setText("Next question");
                }
            } else {
                // Step 2: Advance to next question or show final score
                currentQuestionIndex++;
                if (currentQuestionIndex >= quizQuestions.size()) {
                    questionArea.setText("Quiz completed! Great work.");
                    progressLabel.setText("FINAL SCORE: " + quizScore + " / " + quizQuestions.size());
                    for (JRadioButton btn : optionButtons) {
                        btn.setVisible(false);
                    }
                    feedbackArea.setText("Review the learning materials and try again to improve your score.");
                    submitOrNextBtn.setEnabled(false);
                } else {
                    displayCurrentQuestion(progressLabel, questionArea, optionButtons, choiceGroup, feedbackArea, submitOrNextBtn);
                }
            }
        });

        switchMainPage(page);
    }

    private void displayCurrentQuestion(JLabel progressLabel, JTextArea questionArea, JRadioButton[] optionButtons,
                                        ButtonGroup choiceGroup, JTextArea feedbackArea, JButton actionBtn) {
        QuizQuestion q = quizQuestions.get(currentQuestionIndex);
        progressLabel.setText("QUESTION " + (currentQuestionIndex + 1) + " OF " + quizQuestions.size() + "  •  SCORE: " + quizScore);
        questionArea.setText(q.getQuestion());

        String[] choices = q.getChoices();
        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText((char) ('A' + i) + ".  " + choices[i]);
            optionButtons[i].setVisible(true);
        }

        choiceGroup.clearSelection();
        feedbackArea.setText("");
        hasAnswered = false;
        actionBtn.setText("Submit answer");
        actionBtn.setEnabled(true);
    }

    // =========================================================================
    // 5. ABOUT PAGE & ADMIN PORTAL
    // =========================================================================

    private void showAbout() {
        JPanel page = createPageContainer("About SINAG", "Awareness • Readiness • Safety");
        JTextArea aboutText = new JTextArea(
            "SINAG is an educational application designed to help users learn about El Niño, "
            + "understand possible impacts, review preparedness guidance, and test their knowledge.\n\n"
            + "The application provides educational content and sample advisories. It does not generate "
            + "live weather forecasts and does not replace official government advisories, emergency services, "
            + "or instructions from local authorities.\n\n"
            + "Always verify time-sensitive weather information and public advisories with PAGASA and your LGU."
        );
        aboutText.setWrapStyleWord(true);
        aboutText.setLineWrap(true);
        aboutText.setEditable(false);
        aboutText.setFont(new Font("SansSerif", Font.PLAIN, 14));
        aboutText.setForeground(TEXT_DARK);
        aboutText.setBackground(Color.WHITE);
        aboutText.setBorder(new EmptyBorder(22, 22, 22, 22));

        page.add(new JScrollPane(aboutText), BorderLayout.CENTER);
        switchMainPage(page);
    }

    private void openAdminLogin() {
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();

        JPanel loginForm = new JPanel(new GridLayout(2, 2, 8, 8));
        loginForm.add(new JLabel("Username:"));
        loginForm.add(userField);
        loginForm.add(new JLabel("Password:"));
        loginForm.add(passField);

        int choice = JOptionPane.showConfirmDialog(this, loginForm, "SINAG Administrator Login",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (choice == JOptionPane.OK_OPTION) {
            String username = userField.getText();
            String password = new String(passField.getPassword());

            if (username.equals("admin") && password.equals("sinag123")) {
                openAdminMenu();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openAdminMenu() {
        String[] options = {"Manage Content", "Manage Quiz Questions", "Close"};
        int choice = JOptionPane.showOptionDialog(this, "Choose an administrator task.", "SINAG Admin Portal",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (choice == 0) {
            manageContent();
        } else if (choice == 1) {
            manageQuestions();
        }
    }

    private void manageContent() {
        try {
            List<Content> items = db.getContent("All", "", "All");
            String[] choices = new String[items.size() + 1];
            for (int i = 0; i < items.size(); i++) {
                Content c = items.get(i);
                choices[i] = c.getId() + " | " + c.getType() + " | " + c.getTitle();
            }
            choices[items.size()] = "Add New";

            String selected = (String) JOptionPane.showInputDialog(this,
                    "Select a record to edit/delete, or choose Add New:", "Content Management",
                    JOptionPane.PLAIN_MESSAGE, null, choices, "Add New");

            if (selected == null) return;
            if (selected.equals("Add New")) {
                openContentEditor(null);
                return;
            }

            int id = Integer.parseInt(selected.substring(0, selected.indexOf(" |")));
            Content chosen = null;
            for (Content c : items) {
                if (c.getId() == id) {
                    chosen = c;
                    break;
                }
            }

            Object[] actions = {"Edit", "Delete", "Cancel"};
            int action = JOptionPane.showOptionDialog(this, "Record: " + selected, "Manage Record",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, actions, actions[0]);

            if (action == 0) {
                openContentEditor(chosen);
            } else if (action == 1) {
                int confirm = JOptionPane.showConfirmDialog(this, "Delete this record?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    db.deleteContent(id);
                    JOptionPane.showMessageDialog(this, "Deleted.");
                }
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void openContentEditor(Content existing) {
        JTextField titleField = new JTextField(existing == null ? "" : existing.getTitle());
        JTextArea bodyArea = new JTextArea(existing == null ? "" : existing.getBody(), 7, 28);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);

        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Article", "Advisory", "FAQ", "Guide"});
        JComboBox<String> categoryBox = new JComboBox<>(new String[]{
            "Basics", "Effects", "Water Conservation", "Heat Safety", "Preparedness", "Other"
        });
        JTextField sourceField = new JTextField(existing == null ? "" : existing.getSource());
        JTextField dateField = new JTextField(existing == null ? LocalDate.now().toString() : existing.getPublishedDate());

        if (existing != null) {
            typeBox.setSelectedItem(existing.getType());
            categoryBox.setSelectedItem(existing.getCategory());
        }

        JPanel form = new JPanel(new GridLayout(0, 1, 5, 5));
        form.add(new JLabel("Content type"));
        form.add(typeBox);
        form.add(new JLabel("Title"));
        form.add(titleField);
        form.add(new JLabel("Body"));
        form.add(new JScrollPane(bodyArea));
        form.add(new JLabel("Category"));
        form.add(categoryBox);
        form.add(new JLabel("Source"));
        form.add(sourceField);
        form.add(new JLabel("Publication date (YYYY-MM-DD)"));
        form.add(dateField);

        int result = JOptionPane.showConfirmDialog(this, form, existing == null ? "Add Content" : "Edit Content",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            if (titleField.getText().isBlank() || bodyArea.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Title and body are required.");
                return;
            }
            try {
                if (existing == null) {
                    db.addContent((String) typeBox.getSelectedItem(), titleField.getText().trim(),
                            bodyArea.getText().trim(), (String) categoryBox.getSelectedItem(),
                            sourceField.getText().trim(), dateField.getText().trim());
                } else {
                    db.updateContent(existing.getId(), (String) typeBox.getSelectedItem(), titleField.getText().trim(),
                            bodyArea.getText().trim(), (String) categoryBox.getSelectedItem(),
                            sourceField.getText().trim(), dateField.getText().trim());
                }
                JOptionPane.showMessageDialog(this, "Content saved successfully.");
            } catch (SQLException ex) {
                showError(ex);
            }
        }
    }

    private void manageQuestions() {
        try {
            List<QuizQuestion> questions = db.getQuestions("All");
            String[] choices = new String[questions.size() + 1];
            for (int i = 0; i < questions.size(); i++) {
                QuizQuestion q = questions.get(i);
                choices[i] = q.getId() + " | " + q.getCategory() + " | " + q.getQuestion();
            }
            choices[questions.size()] = "Add New";

            String selected = (String) JOptionPane.showInputDialog(this,
                    "Select a question to delete, or choose Add New:", "Quiz Management",
                    JOptionPane.PLAIN_MESSAGE, null, choices, "Add New");

            if (selected == null) return;
            if (selected.equals("Add New")) {
                openQuestionEditor();
                return;
            }

            int id = Integer.parseInt(selected.substring(0, selected.indexOf(" |")));
            int confirm = JOptionPane.showConfirmDialog(this, "Delete selected question?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                db.deleteQuestion(id);
                JOptionPane.showMessageDialog(this, "Question deleted.");
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void openQuestionEditor() {
        JTextField categoryField = new JTextField("Basics");
        JTextField questionField = new JTextField();
        JTextField choiceA = new JTextField();
        JTextField choiceB = new JTextField();
        JTextField choiceC = new JTextField();
        JTextField choiceD = new JTextField();
        JTextField correctField = new JTextField("A");
        JTextField explanationField = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Category")); form.add(categoryField);
        form.add(new JLabel("Question")); form.add(questionField);
        form.add(new JLabel("Choice A")); form.add(choiceA);
        form.add(new JLabel("Choice B")); form.add(choiceB);
        form.add(new JLabel("Choice C")); form.add(choiceC);
        form.add(new JLabel("Choice D")); form.add(choiceD);
        form.add(new JLabel("Correct answer (A/B/C/D)")); form.add(correctField);
        form.add(new JLabel("Explanation")); form.add(explanationField);

        int result = JOptionPane.showConfirmDialog(this, form, "Add Quiz Question", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String correctLetter = correctField.getText().trim().toUpperCase();
            boolean validAnswer = Arrays.asList("A", "B", "C", "D").contains(correctLetter);

            if (questionField.getText().isBlank() || choiceA.getText().isBlank() || choiceB.getText().isBlank()
                    || choiceC.getText().isBlank() || choiceD.getText().isBlank() || explanationField.getText().isBlank()
                    || !validAnswer) {
                JOptionPane.showMessageDialog(this, "Complete all fields and enter A, B, C, or D as the answer.");
                return;
            }

            try {
                db.addQuestion(categoryField.getText().trim(), questionField.getText().trim(),
                        choiceA.getText().trim(), choiceB.getText().trim(),
                        choiceC.getText().trim(), choiceD.getText().trim(),
                        correctLetter, explanationField.getText().trim());
                JOptionPane.showMessageDialog(this, "Question added.");
            } catch (SQLException ex) {
                showError(ex);
            }
        }
    }

    // =========================================================================
    // 6. HELPER UI METHODS & CUSTOM PANELS
    // =========================================================================

    private JPanel createPageContainer(String title, String subtitle) {
        JPanel page = new JPanel(new BorderLayout(12, 18));
        page.setBackground(BACKGROUND);
        page.setBorder(new EmptyBorder(26, 28, 25, 28));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 23));
        titleLabel.setForeground(NAVY);

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subLabel.setForeground(TEXT_MUTED);

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(6));
        header.add(subLabel);
        page.add(header, BorderLayout.NORTH);

        return page;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text + ":");
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setForeground(NAVY);
        return label;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(10, 15, 10, 15));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void switchMainPage(JPanel newPage) {
        mainContentArea.removeAll();
        mainContentArea.add(newPage, BorderLayout.CENTER);
        mainContentArea.revalidate();
        mainContentArea.repaint();
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "SINAG Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }

    // Draws a white card with smooth rounded corners
    private static class RoundedPanel extends JPanel {
        private final int cornerRadius;
        private final Color fillColor;

        RoundedPanel(int cornerRadius, Color fillColor) {
            this.cornerRadius = cornerRadius;
            this.fillColor = fillColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fillColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Draws the teal-to-navy gradient banner on the Home screen
    private static class GradientPanel extends JPanel {
        GradientPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, new Color(0, 119, 116), getWidth(), getHeight(), new Color(25, 73, 103)));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
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