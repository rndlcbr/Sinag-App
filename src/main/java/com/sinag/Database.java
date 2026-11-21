package com.sinag;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all SQLite database operations (creating tables, seeding sample data,
 * and performing Create, Read, Update, Delete actions).
 */
public class Database {
    // Store database in user's home directory so Windows never blocks file creation
    private static final String DB_FOLDER = System.getProperty("user.home") + File.separator + "SINAG_Data";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FOLDER + File.separator + "sinag.db";

    public Database() {
        try {
            // Explicitly load the SQLite JDBC driver so Java recognizes "jdbc:sqlite:"
            Class.forName("org.sqlite.JDBC");

            try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
                // Create the educational content table
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS content (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        body TEXT NOT NULL,
                        category TEXT,
                        source TEXT,
                        published_date TEXT,
                        image_path TEXT DEFAULT ''
                    )
                """);

                // Safely add image_path column if upgrading an older database file
                try {
                    stmt.executeUpdate("ALTER TABLE content ADD COLUMN image_path TEXT DEFAULT ''");
                } catch (SQLException ignored) {
                    // Column already exists
                }

                // Create the quiz questions table
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS questions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        category TEXT NOT NULL,
                        question TEXT NOT NULL,
                        a TEXT NOT NULL,
                        b TEXT NOT NULL,
                        c TEXT NOT NULL,
                        d TEXT NOT NULL,
                        correct TEXT NOT NULL,
                        explanation TEXT NOT NULL
                    )
                """);

                // Insert starter records if the database is empty
                insertStarterData(conn);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "SQLite Driver missing! Make sure you opened the 'Sinag-App' folder (with pom.xml) in VS Code, " +
                "or added sqlite-jdbc.jar to Referenced Libraries.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    // Ensures SINAG_Data folder exists and opens a connection to sinag.db
    private Connection connect() throws SQLException {
        File folder = new File(DB_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA busy_timeout = 5000");
        }
        return conn;
    }

    // Adds initial articles, advisories, FAQs, guides, and quiz questions on first run
    private void insertStarterData(Connection conn) throws SQLException {
        boolean isContentEmpty = false;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM content")) {
            if (rs.next() && rs.getInt(1) == 0) {
                isContentEmpty = true;
            }
        }

        if (isContentEmpty) {
            addContent("Article", "What is El Niño?",
                "El Niño is a climate pattern involving unusually warm ocean surface temperatures in the central and eastern tropical Pacific. It can influence rainfall and temperature patterns in many regions.",
                "Basics", "PAGASA / Educational reference", "2026-11-06", "");

            addContent("Article", "Possible effects of El Niño",
                "Possible effects include below-normal rainfall, dry conditions, water shortages, increased heat, and impacts on agriculture. Effects vary by location and event.",
                "Effects", "Educational reference", "2026-11-06", "");

            addContent("Advisory", "Practice responsible water use",
                "Repair leaking taps, reuse suitable household water, turn off taps when not needed, and prioritize safe drinking water. Follow announcements from your local water provider.",
                "Water Conservation", "Sample educational advisory — verify with your LGU", "2026-11-06", "");

            addContent("Guide", "Household heat-safety guide",
                "Drink water regularly, limit strenuous outdoor activity during the hottest hours, stay in shaded or cool places, and check on children, older adults, and vulnerable neighbors.",
                "Heat Safety", "Sample preparedness guide", "2026-11-06", "");

            addContent("FAQ", "Does El Niño mean it will not rain at all?",
                "No. El Niño can increase the likelihood of below-normal rainfall in some places, but it does not mean every location will have no rain.",
                "Basics", "Educational reference", "2026-11-06", "");
        }

        boolean isQuestionsEmpty = false;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM questions")) {
            if (rs.next() && rs.getInt(1) == 0) {
                isQuestionsEmpty = true;
            }
        }

        if (isQuestionsEmpty) {
            addQuestion("Basics", "What does El Niño primarily involve?",
                "Unusually warm Pacific Ocean surface waters", "A volcanic eruption",
                "A strong earthquake", "A tropical cyclone", "A",
                "El Niño is associated with unusually warm sea-surface temperatures in the central and eastern tropical Pacific.");

            addQuestion("Water Conservation", "Which action helps conserve water?",
                "Leave taps running", "Repair leaking faucets",
                "Wash one item at a time", "Water roads daily", "B",
                "Repairing leaks prevents unnecessary water loss.");

            addQuestion("Heat Safety", "What is a sensible action during extreme heat?",
                "Avoid drinking water", "Wear heavy dark clothing",
                "Stay in a cool or shaded place", "Do intense outdoor exercise at noon", "C",
                "A cool or shaded place helps reduce heat exposure.");

            addQuestion("Preparedness", "Where should you check for local official advisories?",
                "Unverified social media posts", "Official government and local authority channels",
                "Anonymous messages", "Old forwarded posts", "B",
                "Use official government agencies and local authorities for current guidance.");
        }
    }

    // --- CONTENT CRUD METHODS ---

    public void addContent(String type, String title, String body, String category, String source, String date, String imagePath) throws SQLException {
        String sql = "INSERT INTO content (type, title, body, category, source, published_date, image_path) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type);
            pstmt.setString(2, title);
            pstmt.setString(3, body);
            pstmt.setString(4, category);
            pstmt.setString(5, source);
            pstmt.setString(6, date);
            pstmt.setString(7, imagePath == null ? "" : imagePath);
            pstmt.executeUpdate();
        }
    }

    public void updateContent(int id, String type, String title, String body, String category, String source, String date, String imagePath) throws SQLException {
        String sql = "UPDATE content SET type=?, title=?, body=?, category=?, source=?, published_date=?, image_path=? WHERE id=?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type);
            pstmt.setString(2, title);
            pstmt.setString(3, body);
            pstmt.setString(4, category);
            pstmt.setString(5, source);
            pstmt.setString(6, date);
            pstmt.setString(7, imagePath == null ? "" : imagePath);
            pstmt.setInt(8, id);
            pstmt.executeUpdate();
        }
    }

    public void deleteContent(int id) throws SQLException {
        String sql = "DELETE FROM content WHERE id=?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public List<Content> getContent(String type, String searchKeyword, String category) throws SQLException {
        List<Content> results = new ArrayList<>();
        String sql = """
            SELECT * FROM content
            WHERE (? = 'All' OR type = ?)
              AND (? = '' OR lower(title) LIKE ? OR lower(body) LIKE ?)
              AND (? = 'All' OR category = ?)
            ORDER BY id DESC
        """;

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String pattern = "%" + searchKeyword.toLowerCase() + "%";
            pstmt.setString(1, type);
            pstmt.setString(2, type);
            pstmt.setString(3, searchKeyword);
            pstmt.setString(4, pattern);
            pstmt.setString(5, pattern);
            pstmt.setString(6, category);
            pstmt.setString(7, category);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String contentType = rs.getString("type");
                    String title = rs.getString("title");
                    String body = rs.getString("body");
                    String cat = rs.getString("category");
                    String source = rs.getString("source");
                    String date = rs.getString("published_date");

                    String img = "";
                    try {
                        img = rs.getString("image_path");
                    } catch (SQLException ignored) {
                        // Safe fallback if reading an older database
                    }

                    // Instantiate the matching subclass (Polymorphism)
                    Content item;
                    if (contentType.equals("Article")) {
                        item = new Article(id, title, body, cat, source, date, img);
                    } else if (contentType.equals("Advisory")) {
                        item = new Advisory(id, title, body, cat, source, date, img);
                    } else if (contentType.equals("FAQ")) {
                        item = new FAQ(id, title, body, cat, source, date, img);
                    } else {
                        item = new Guide(id, title, body, cat, source, date, img);
                    }
                    results.add(item);
                }
            }
        }
        return results;
    }

    // --- QUIZ QUESTION CRUD METHODS ---

    public void addQuestion(String category, String question, String a, String b, String c, String d, String correct, String explanation) throws SQLException {
        String sql = "INSERT INTO questions (category, question, a, b, c, d, correct, explanation) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            pstmt.setString(2, question);
            pstmt.setString(3, a);
            pstmt.setString(4, b);
            pstmt.setString(5, c);
            pstmt.setString(6, d);
            pstmt.setString(7, correct);
            pstmt.setString(8, explanation);
            pstmt.executeUpdate();
        }
    }

    public void updateQuestion(int id, String category, String question, String a, String b, String c, String d, String correct, String explanation) throws SQLException {
        String sql = "UPDATE questions SET category=?, question=?, a=?, b=?, c=?, d=?, correct=?, explanation=? WHERE id=?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            pstmt.setString(2, question);
            pstmt.setString(3, a);
            pstmt.setString(4, b);
            pstmt.setString(5, c);
            pstmt.setString(6, d);
            pstmt.setString(7, correct);
            pstmt.setString(8, explanation);
            pstmt.setInt(9, id);
            pstmt.executeUpdate();
        }
    }

    public List<QuizQuestion> getQuestions(String category) throws SQLException {
        List<QuizQuestion> questions = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE (? = 'All' OR category = ?) ORDER BY RANDOM()";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            pstmt.setString(2, category);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    questions.add(new QuizQuestion(
                        rs.getInt("id"),
                        rs.getString("category"),
                        rs.getString("question"),
                        rs.getString("a"),
                        rs.getString("b"),
                        rs.getString("c"),
                        rs.getString("d"),
                        rs.getString("correct"),
                        rs.getString("explanation")
                    ));
                }
            }
        }
        return questions;
    }

    public void deleteQuestion(int id) throws SQLException {
        String sql = "DELETE FROM questions WHERE id=?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
}