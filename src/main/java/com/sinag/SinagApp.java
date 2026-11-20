package com.sinag;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

public class SinagApp extends JFrame {
    private static final Color NAVY = new Color(17, 43, 66);
    private static final Color NAVY2 = new Color(29, 66, 91);
    private static final Color TEAL = new Color(0, 137, 123);
    private static final Color AQUA = new Color(91, 203, 190);
    private static final Color BG = new Color(241, 246, 249);
    private static final Color TEXT = new Color(39, 57, 72);
    private static final Color MUTED = new Color(111, 130, 145);
    private static final Color BORDER = new Color(222, 231, 237);

    private final Database db = new Database();
    private final JPanel contentPanel = new JPanel(new BorderLayout());

    public SinagApp() {
        setTitle("SINAG | Awareness • Readiness • Safety");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(940, 640));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        contentPanel.setBackground(BG);
        add(contentPanel, BorderLayout.CENTER);
        showHome();
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(NAVY);
        h.setBorder(new EmptyBorder(17, 25, 17, 25));
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setOpaque(false);
        JLabel logo = new JLabel("☀  SINAG");
        logo.setFont(new Font("SansSerif", Font.BOLD, 25));
        logo.setForeground(Color.WHITE);
        JLabel tagline = new JLabel("   |   EL NIÑO AWARENESS & PREPAREDNESS");
        tagline.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tagline.setForeground(new Color(190, 218, 229));
        brand.add(logo); brand.add(tagline);
        JButton admin = button("ADMIN PORTAL", TEAL);
        admin.addActionListener(e -> adminLogin());
        h.add(brand, BorderLayout.WEST); h.add(admin, BorderLayout.EAST);
        return h;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setPreferredSize(new Dimension(215, 0));
        side.setBackground(new Color(229, 238, 243));
        side.setBorder(new EmptyBorder(22, 13, 18, 13));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        JLabel menu = new JLabel("  MAIN MENU");
        menu.setFont(new Font("SansSerif", Font.BOLD, 10)); menu.setForeground(MUTED);
        menu.setBorder(new EmptyBorder(0, 8, 12, 0)); side.add(menu);
        String[][] items = {{"⌂", "Home", "Home"}, {"▤", "Articles", "Article"},
                {"⚑", "Advisories", "Advisory"}, {"?", "FAQs", "FAQ"},
                {"＋", "Preparedness Guides", "Guide"}, {"★", "Quiz Battle", "Quiz"},
                {"ⓘ", "About SINAG", "About"}};
        for (String[] item : items) {
            JButton b = new JButton(item[0] + "   " + item[1]);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setFont(new Font("SansSerif", Font.BOLD, 12)); b.setForeground(NAVY);
            b.setBackground(new Color(229, 238, 243)); b.setBorder(new EmptyBorder(12, 12, 12, 8));
            b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
            b.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { b.setBackground(new Color(211, 231, 235)); }
                public void mouseExited(MouseEvent e) { b.setBackground(new Color(229, 238, 243)); }
            });
            b.addActionListener(e -> {
                switch (item[2]) {
                    case "Home" -> showHome(); case "Quiz" -> showQuiz(); case "About" -> showAbout();
                    default -> showContent(item[2]);
                }
            });
            side.add(b); side.add(Box.createVerticalStrut(5));
        }
        side.add(Box.createVerticalGlue());
        JLabel tip = new JLabel("<html><div style='width:155px'><b>STAY INFORMED</b><br><br>Preparedness starts with reliable information and small daily actions.</div></html>");
        tip.setFont(new Font("SansSerif", Font.PLAIN, 11)); tip.setForeground(new Color(77, 105, 121));
        tip.setBorder(new EmptyBorder(12, 9, 12, 9)); tip.setBackground(new Color(215, 234, 237)); tip.setOpaque(true);
        side.add(tip); side.add(Box.createVerticalStrut(12));
        JButton exit = button("  Exit application", NAVY2); exit.setHorizontalAlignment(SwingConstants.LEFT);
        exit.addActionListener(e -> dispose()); side.add(exit);
        return side;
    }

    private void showHome() {
        JPanel p = new JPanel(new BorderLayout(0, 18)); p.setBackground(BG);
        p.setBorder(new EmptyBorder(25, 28, 25, 28));
        GradientPanel hero = new GradientPanel(); hero.setLayout(new BorderLayout());
        hero.setBorder(new EmptyBorder(27, 30, 27, 30));
        JPanel words = new JPanel(); words.setOpaque(false); words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("EL NIÑO  •  CLIMATE AWARENESS  •  COMMUNITY READINESS");
        eyebrow.setForeground(new Color(204, 244, 237)); eyebrow.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel title = new JLabel("Be informed. Be prepared. Stay safe.");
        title.setForeground(Color.WHITE); title.setFont(new Font("SansSerif", Font.BOLD, 25));
        JLabel sub = new JLabel("Practical knowledge and preparedness resources for every household.");
        sub.setForeground(new Color(225, 242, 246)); sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        words.add(eyebrow); words.add(Box.createVerticalStrut(12)); words.add(title);
        words.add(Box.createVerticalStrut(8)); words.add(sub); hero.add(words, BorderLayout.CENTER);
        JLabel sun = new JLabel("☀"); sun.setFont(new Font("SansSerif", Font.PLAIN, 72));
        sun.setForeground(new Color(255, 231, 157)); hero.add(sun, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout(0, 15)); center.setOpaque(false);
        JPanel heading = new JPanel(); heading.setOpaque(false); heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel h = new JLabel("Explore SINAG"); h.setFont(new Font("SansSerif", Font.BOLD, 21)); h.setForeground(NAVY);
        JLabel s = new JLabel("Learn about climate conditions and everyday preparedness."); s.setFont(new Font("SansSerif", Font.PLAIN, 12)); s.setForeground(MUTED);
        heading.add(h); heading.add(Box.createVerticalStrut(5)); heading.add(s); center.add(heading, BorderLayout.NORTH);
        JPanel cards = new JPanel(new GridLayout(2, 2, 14, 14)); cards.setOpaque(false);
        addHomeCard(cards, "▤", "EDUCATIONAL ARTICLES", "Understand El Niño, its causes, and possible effects.", "Explore articles", () -> showContent("Article"));
        addHomeCard(cards, "⚑", "PUBLIC ADVISORIES", "Read reminders about water conservation and heat safety.", "View advisories", () -> showContent("Advisory"));
        addHomeCard(cards, "＋", "PREPAREDNESS CENTER", "Find household readiness tips, FAQs, and practical guides.", "Open resources", () -> showContent("Guide"));
        addHomeCard(cards, "★", "INTERACTIVE QUIZ", "Test your knowledge and learn through instant feedback.", "Start quiz battle", this::showQuiz);
        center.add(cards, BorderLayout.CENTER);
        JLabel reminder = new JLabel("☀  PREPAREDNESS REMINDER   Keep drinking water available and follow verified local announcements.");
        reminder.setForeground(new Color(47, 102, 105)); reminder.setFont(new Font("SansSerif", Font.BOLD, 11));
        reminder.setBorder(new EmptyBorder(13, 15, 13, 15)); reminder.setBackground(new Color(222, 242, 237)); reminder.setOpaque(true);
        p.add(hero, BorderLayout.NORTH); p.add(center, BorderLayout.CENTER); p.add(reminder, BorderLayout.SOUTH);
        setPage(p);
    }

    private void addHomeCard(JPanel parent, String icon, String title, String desc, String actionText, Runnable action) {
        RoundedPanel card = new RoundedPanel(18, Color.WHITE);
        card.setLayout(new BorderLayout(8, 8)); card.setBorder(new EmptyBorder(17, 18, 16, 18));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); top.setOpaque(false);
        JLabel symbol = new JLabel(icon); symbol.setFont(new Font("SansSerif", Font.BOLD, 22)); symbol.setForeground(TEAL);
        JLabel heading = new JLabel("  " + title); heading.setFont(new Font("SansSerif", Font.BOLD, 13)); heading.setForeground(NAVY);
        top.add(symbol); top.add(heading);
        JTextArea description = new JTextArea(desc); description.setWrapStyleWord(true); description.setLineWrap(true);
        description.setEditable(false); description.setOpaque(false); description.setForeground(MUTED);
        description.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JButton open = button(actionText + "  →", new Color(229, 244, 241)); open.setForeground(TEAL);
        open.addActionListener(e -> action.run());
        card.add(top, BorderLayout.NORTH); card.add(description, BorderLayout.CENTER); card.add(open, BorderLayout.SOUTH);
        parent.add(card);
    }

    private JPanel page(String title, String subtitle) {
        JPanel p = new JPanel(new BorderLayout(12, 18)); p.setBackground(BG); p.setBorder(new EmptyBorder(26, 28, 25, 28));
        JPanel heading = new JPanel(); heading.setOpaque(false); heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel h = new JLabel(title); h.setFont(new Font("SansSerif", Font.BOLD, 23)); h.setForeground(NAVY);
        JLabel s = new JLabel(subtitle); s.setFont(new Font("SansSerif", Font.PLAIN, 12)); s.setForeground(MUTED);
        heading.add(h); heading.add(Box.createVerticalStrut(6)); heading.add(s); p.add(heading, BorderLayout.NORTH);
        return p;
    }

    private void showContent(String type) {
        String title = switch (type) { case "Article" -> "Educational Articles"; case "Advisory" -> "Public Advisories"; case "FAQ" -> "Frequently Asked Questions"; default -> "Preparedness Guides"; };
        JPanel p = page(title, "Browse learning resources and practical preparedness information.");
        JPanel body = new JPanel(new BorderLayout(12, 12)); body.setOpaque(false);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4)); filters.setOpaque(false);
        JTextField search = new JTextField(19);
        JComboBox<String> category = new JComboBox<>(new String[]{"All", "Basics", "Effects", "Water Conservation", "Heat Safety", "Preparedness", "Other"});
        JButton find = button("Search", TEAL);
        filters.add(fieldLabel("Search")); filters.add(search); filters.add(fieldLabel("Category")); filters.add(category); filters.add(find);
        DefaultListModel<Content> model = new DefaultListModel<>(); JList<Content> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); list.setFixedCellHeight(48); list.setBackground(Color.WHITE);
        list.setFont(new Font("SansSerif", Font.PLAIN, 12)); list.setBorder(new EmptyBorder(5, 5, 5, 5));
        list.setCellRenderer((l, value, index, selected, focus) -> {
            JLabel label = new JLabel("<html><b>" + escape(value.getTitle()) + "</b><br><span style='color:#718292'>" + escape(value.getCategory()) + "</span></html>");
            label.setOpaque(true); label.setBorder(new EmptyBorder(8, 10, 8, 10));
            label.setBackground(selected ? new Color(218, 241, 236) : Color.WHITE); label.setForeground(NAVY); return label;
        });
        JTextArea details = new JTextArea("Select an item from the list to read the complete information.");
        details.setLineWrap(true); details.setWrapStyleWord(true); details.setEditable(false); details.setFont(new Font("SansSerif", Font.PLAIN, 13));
        details.setForeground(TEXT); details.setBackground(Color.WHITE); details.setBorder(new EmptyBorder(18, 18, 18, 18));
        list.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
            Content c = list.getSelectedValue(); details.setText(c.displayContent() + "\n\nCATEGORY: " + c.getCategory() + "\nPUBLISHED: " + c.getPublishedDate() + "\nSOURCE: " + c.getSource()); details.setCaretPosition(0);
        }});
        Runnable load = () -> { model.clear(); try { for (Content c : db.getContent(type, search.getText().trim(), (String) category.getSelectedItem())) model.addElement(c); } catch (SQLException ex) { error(ex); } details.setText("Select an item from the list to read the complete information."); };
        find.addActionListener(e -> load.run()); search.addActionListener(e -> load.run()); category.addActionListener(e -> load.run());
        JScrollPane left = new JScrollPane(list), right = new JScrollPane(details); left.setBorder(BorderFactory.createLineBorder(BORDER)); right.setBorder(BorderFactory.createLineBorder(BORDER));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right); split.setResizeWeight(.38); split.setBorder(null);
        body.add(filters, BorderLayout.NORTH); body.add(split, BorderLayout.CENTER); p.add(body, BorderLayout.CENTER); load.run(); setPage(p);
    }

    private void showQuiz() {
        JPanel p = page("Quiz Battle", "Challenge yourself and strengthen your knowledge of El Niño preparedness.");
        JPanel wrapper = new JPanel(new BorderLayout(12, 15)); wrapper.setOpaque(false);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4)); controls.setOpaque(false);
        JComboBox<String> category = new JComboBox<>(new String[]{"All", "Basics", "Effects", "Water Conservation", "Heat Safety", "Preparedness"});
        JButton start = button("▶  Start Quiz", TEAL); controls.add(fieldLabel("Quiz category")); controls.add(category); controls.add(start);
        RoundedPanel game = new RoundedPanel(18, Color.WHITE); game.setLayout(new BorderLayout(12, 15)); game.setBorder(new EmptyBorder(22, 24, 22, 24));
        JLabel counter = new JLabel("READY WHEN YOU ARE"); counter.setFont(new Font("SansSerif", Font.BOLD, 12)); counter.setForeground(TEAL);
        JTextArea questionText = new JTextArea("Choose a category, then press Start Quiz to begin."); questionText.setLineWrap(true); questionText.setWrapStyleWord(true); questionText.setEditable(false); questionText.setOpaque(false); questionText.setForeground(NAVY); questionText.setFont(new Font("SansSerif", Font.BOLD, 18));
        JPanel choices = new JPanel(new GridLayout(4, 1, 8, 8)); choices.setOpaque(false); ButtonGroup group = new ButtonGroup(); JRadioButton[] radios = new JRadioButton[4];
        for (int i = 0; i < 4; i++) { radios[i] = new JRadioButton(); radios[i].setFont(new Font("SansSerif", Font.PLAIN, 13)); radios[i].setForeground(TEXT); radios[i].setBackground(new Color(246, 249, 251)); radios[i].setBorder(new EmptyBorder(10, 12, 10, 12)); radios[i].setOpaque(true); radios[i].setVisible(false); group.add(radios[i]); choices.add(radios[i]); }
        JTextArea feedback = new JTextArea(); feedback.setLineWrap(true); feedback.setWrapStyleWord(true); feedback.setEditable(false); feedback.setOpaque(false); feedback.setForeground(TEAL); feedback.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JButton next = button("Submit answer", NAVY2); next.setEnabled(false);
        JPanel center = new JPanel(new BorderLayout(10, 14)); center.setOpaque(false); center.add(questionText, BorderLayout.NORTH); center.add(choices, BorderLayout.CENTER); center.add(feedback, BorderLayout.SOUTH);
        game.add(counter, BorderLayout.NORTH); game.add(center, BorderLayout.CENTER); game.add(next, BorderLayout.SOUTH); wrapper.add(controls, BorderLayout.NORTH); wrapper.add(game, BorderLayout.CENTER); p.add(wrapper, BorderLayout.CENTER);
        final List<QuizQuestion>[] questions = new List[]{new ArrayList<>()}; final int[] index = {0}, score = {0}; final boolean[] answered = {false};
        Runnable display = () -> { QuizQuestion q = questions[0].get(index[0]); counter.setText("QUESTION " + (index[0] + 1) + " OF " + questions[0].size() + "  •  SCORE: " + score[0]); questionText.setText(q.getQuestion()); String[] opts = q.getChoices(); for (int i = 0; i < 4; i++) { radios[i].setText((char) ('A' + i) + ".  " + opts[i]); radios[i].setVisible(true); } group.clearSelection(); feedback.setText(""); answered[0] = false; next.setText("Submit answer"); next.setEnabled(true); };
        start.addActionListener(e -> { try { questions[0] = db.getQuestions((String) category.getSelectedItem()); index[0] = 0; score[0] = 0; if (questions[0].isEmpty()) { JOptionPane.showMessageDialog(this, "No questions available for this category."); return; } display.run(); } catch (SQLException ex) { error(ex); } });
        next.addActionListener(e -> { if (questions[0].isEmpty()) return; QuizQuestion q = questions[0].get(index[0]); if (!answered[0]) { String answer = null; for (int i = 0; i < 4; i++) if (radios[i].isSelected()) answer = String.valueOf((char) ('A' + i)); if (answer == null) { JOptionPane.showMessageDialog(this, "Please select an answer."); return; } boolean correct = q.checkAnswer(answer); if (correct) score[0]++; feedback.setText((correct ? "Correct! " : "Not quite. Correct answer: " + q.getCorrect() + ". " + q.getChoice(q.getCorrect()) + ". ") + q.getExplanation()); answered[0] = true; next.setText(index[0] == questions[0].size() - 1 ? "View result" : "Next question"); } else { index[0]++; if (index[0] >= questions[0].size()) { questionText.setText("Quiz completed! Great work."); counter.setText("FINAL SCORE: " + score[0] + " / " + questions[0].size()); for (JRadioButton r : radios) r.setVisible(false); feedback.setText("Review the learning materials and try again to improve your score."); next.setEnabled(false); } else display.run(); } });
        setPage(p);
    }

    private void showAbout() {
        JPanel p = page("About SINAG", "Awareness • Readiness • Safety");
        JTextArea about = new JTextArea("SINAG is an educational application designed to help users learn about El Niño, understand possible impacts, review preparedness guidance, and test their knowledge.\n\nThe application provides educational content and sample advisories. It does not generate live weather forecasts and does not replace official government advisories, emergency services, or instructions from local authorities.\n\nAlways verify time-sensitive weather information and public advisories with PAGASA and your LGU.");
        about.setWrapStyleWord(true); about.setLineWrap(true); about.setEditable(false); about.setFont(new Font("SansSerif", Font.PLAIN, 14)); about.setForeground(TEXT); about.setBackground(Color.WHITE); about.setBorder(new EmptyBorder(22, 22, 22, 22));
        p.add(new JScrollPane(about), BorderLayout.CENTER); setPage(p);
    }

    private void adminLogin() {
        JTextField username = new JTextField(); JPasswordField password = new JPasswordField(); JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("Username:")); form.add(username); form.add(new JLabel("Password:")); form.add(password);
        int result = JOptionPane.showConfirmDialog(this, form, "SINAG Administrator Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) { if (username.getText().equals("admin") && new String(password.getPassword()).equals("sinag123")) adminDashboard(); else JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login failed", JOptionPane.ERROR_MESSAGE); }
    }

    private void adminDashboard() {
        String[] options = {"Manage Content", "Manage Quiz Questions", "Close"};
        int choice = JOptionPane.showOptionDialog(this, "Choose an administrator task.", "SINAG Admin Portal", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice == 0) manageContent(); if (choice == 1) manageQuestions();
    }

    private void manageContent() {
        try {
            List<Content> items = db.getContent("All", "", "All");
            String[] labels = items.stream().map(c -> c.getId() + " | " + c.getType() + " | " + c.getTitle()).toArray(String[]::new);
            String selected = (String) JOptionPane.showInputDialog(this, "Select a record to edit/delete, or choose Add New:", "Content Management", JOptionPane.PLAIN_MESSAGE, null, items.isEmpty() ? new String[]{"Add New"} : append(labels, "Add New"), "Add New");
            if (selected == null) return; if (selected.equals("Add New")) { contentEditor(null); return; }
            int id = Integer.parseInt(selected.substring(0, selected.indexOf(" |"))); Content chosen = items.stream().filter(c -> c.getId() == id).findFirst().orElse(null);
            Object[] actions = {"Edit", "Delete", "Cancel"}; int action = JOptionPane.showOptionDialog(this, "Record: " + selected, "Manage Record", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, actions, actions[0]);
            if (action == 0) contentEditor(chosen); else if (action == 1 && JOptionPane.showConfirmDialog(this, "Delete this record?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) { db.deleteContent(id); JOptionPane.showMessageDialog(this, "Deleted."); }
        } catch (Exception ex) { error(ex); }
    }

    private void contentEditor(Content c) {
        JTextField title = new JTextField(c == null ? "" : c.getTitle()); JTextArea body = new JTextArea(c == null ? "" : c.getBody(), 7, 28); body.setLineWrap(true); body.setWrapStyleWord(true);
        JComboBox<String> type = new JComboBox<>(new String[]{"Article", "Advisory", "FAQ", "Guide"}); JComboBox<String> category = new JComboBox<>(new String[]{"Basics", "Effects", "Water Conservation", "Heat Safety", "Preparedness", "Other"});
        JTextField source = new JTextField(c == null ? "" : c.getSource()); JTextField date = new JTextField(c == null ? java.time.LocalDate.now().toString() : c.getPublishedDate());
        if (c != null) { type.setSelectedItem(c.getType()); category.setSelectedItem(c.getCategory()); }
        JPanel f = new JPanel(new GridLayout(0, 1, 5, 5)); f.add(new JLabel("Content type")); f.add(type); f.add(new JLabel("Title")); f.add(title); f.add(new JLabel("Body")); f.add(new JScrollPane(body)); f.add(new JLabel("Category")); f.add(category); f.add(new JLabel("Source")); f.add(source); f.add(new JLabel("Publication date (YYYY-MM-DD)")); f.add(date);
        int result = JOptionPane.showConfirmDialog(this, f, c == null ? "Add Content" : "Edit Content", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) { if (title.getText().isBlank() || body.getText().isBlank()) { JOptionPane.showMessageDialog(this, "Title and body are required."); return; } try { if (c == null) db.addContent((String) type.getSelectedItem(), title.getText().trim(), body.getText().trim(), (String) category.getSelectedItem(), source.getText().trim(), date.getText().trim()); else db.updateContent(c.getId(), (String) type.getSelectedItem(), title.getText().trim(), body.getText().trim(), (String) category.getSelectedItem(), source.getText().trim(), date.getText().trim()); JOptionPane.showMessageDialog(this, "Content saved successfully."); } catch (SQLException ex) { error(ex); } }
    }

    private void manageQuestions() {
        try {
            List<QuizQuestion> qs = db.getQuestions("All"); String[] labels = qs.stream().map(q -> q.getId() + " | " + q.getCategory() + " | " + q.getQuestion()).toArray(String[]::new);
            String selected = (String) JOptionPane.showInputDialog(this, "Select a question to delete, or choose Add New:", "Quiz Management", JOptionPane.PLAIN_MESSAGE, null, append(labels, "Add New"), "Add New");
            if (selected == null) return; if (selected.equals("Add New")) { questionEditor(); return; }
            int id = Integer.parseInt(selected.substring(0, selected.indexOf(" |"))); if (JOptionPane.showConfirmDialog(this, "Delete selected question?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) { db.deleteQuestion(id); JOptionPane.showMessageDialog(this, "Question deleted."); }
        } catch (Exception ex) { error(ex); }
    }

    private void questionEditor() {
        JTextField category = new JTextField("Basics"), question = new JTextField(), a = new JTextField(), b = new JTextField(), c = new JTextField(), d = new JTextField(), correct = new JTextField("A"), explanation = new JTextField();
        JPanel f = new JPanel(new GridLayout(0, 1, 4, 4)); Object[][] fields = {{"Category", category}, {"Question", question}, {"Choice A", a}, {"Choice B", b}, {"Choice C", c}, {"Choice D", d}, {"Correct answer (A/B/C/D)", correct}, {"Explanation", explanation}};
        for (Object[] pair : fields) { f.add(new JLabel((String) pair[0])); f.add((JComponent) pair[1]); }
        int result = JOptionPane.showConfirmDialog(this, f, "Add Quiz Question", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) { if (question.getText().isBlank() || a.getText().isBlank() || b.getText().isBlank() || c.getText().isBlank() || d.getText().isBlank() || explanation.getText().isBlank() || !Arrays.asList("A", "B", "C", "D").contains(correct.getText().trim().toUpperCase())) { JOptionPane.showMessageDialog(this, "Complete all fields and enter A, B, C, or D as the answer."); return; } try { db.addQuestion(category.getText().trim(), question.getText().trim(), a.getText().trim(), b.getText().trim(), c.getText().trim(), d.getText().trim(), correct.getText().trim().toUpperCase(), explanation.getText().trim()); JOptionPane.showMessageDialog(this, "Question added."); } catch (SQLException ex) { error(ex); } }
    }

    private JLabel fieldLabel(String text) { JLabel l = new JLabel(text + ":"); l.setFont(new Font("SansSerif", Font.BOLD, 11)); l.setForeground(NAVY); return l; }
    private JButton button(String text, Color color) { JButton b = new JButton(text); b.setFont(new Font("SansSerif", Font.BOLD, 11)); b.setForeground(Color.WHITE); b.setBackground(color); b.setFocusPainted(false); b.setBorder(new EmptyBorder(10, 15, 10, 15)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b; }
    private String[] append(String[] values, String extra) { String[] out = Arrays.copyOf(values, values.length + 1); out[values.length] = extra; return out; }
    private String escape(String s) { if (s == null) return ""; return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private void setPage(JPanel p) { contentPanel.removeAll(); contentPanel.add(p, BorderLayout.CENTER); contentPanel.revalidate(); contentPanel.repaint(); }
    private void error(Exception e) { JOptionPane.showMessageDialog(this, e.getMessage(), "SINAG Error", JOptionPane.ERROR_MESSAGE); e.printStackTrace(); }

    private static class RoundedPanel extends JPanel {
        private final int radius; private final Color fill;
        RoundedPanel(int radius, Color fill) { this.radius = radius; this.fill = fill; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(fill); g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius); g2.dispose(); super.paintComponent(g); }
    }
    private static class GradientPanel extends JPanel {
        GradientPanel() { setOpaque(false); }
        @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setPaint(new GradientPaint(0, 0, new Color(0, 119, 116), getWidth(), getHeight(), new Color(25, 73, 103))); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22); g2.dispose(); super.paintComponent(g); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
            try { new SinagApp().setVisible(true); } catch (Exception ex) { JOptionPane.showMessageDialog(null, ex.getMessage(), "SINAG Startup Error", JOptionPane.ERROR_MESSAGE); }
        });
    }
}
