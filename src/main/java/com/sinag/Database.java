package com.sinag;

import java.sql.*;
import java.util.*;

/** SQLite persistence layer. All SQL values are passed using PreparedStatement. */
public class Database {
    private static final String URL = "jdbc:sqlite:sinag.db";

    public Database() {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS content(
                  id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL,
                  title TEXT NOT NULL, body TEXT NOT NULL, category TEXT,
                  source TEXT, published_date TEXT)
                """);
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS questions(
                  id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT NOT NULL,
                  question TEXT NOT NULL, a TEXT NOT NULL, b TEXT NOT NULL,
                  c TEXT NOT NULL, d TEXT NOT NULL, correct TEXT NOT NULL,
                  explanation TEXT NOT NULL)
                """);
            seed(c);
        } catch (SQLException e) { throw new RuntimeException("Database initialization failed: " + e.getMessage(), e); }
    }

    private Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA busy_timeout = 5000");
        }
        return c;
    }

    private void seed(Connection c) throws SQLException {
        // Close each COUNT query before opening separate connections for inserts.
        boolean seedContent;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM content")) {
            seedContent = rs.next() && rs.getInt(1) == 0;
        }

        if (seedContent) {
            addContent("Article", "What is El Niño?",
                "El Niño is a climate pattern involving unusually warm ocean surface temperatures in the central and eastern tropical Pacific. It can influence rainfall and temperature patterns in many regions.",
                "Basics", "PAGASA / Educational reference", "2026-11-06");
            addContent("Article", "Possible effects of El Niño",
                "Possible effects include below-normal rainfall, dry conditions, water shortages, increased heat, and impacts on agriculture. Effects vary by location and event.",
                "Effects", "Educational reference", "2026-11-06");
            addContent("Advisory", "Practice responsible water use",
                "Repair leaking taps, reuse suitable household water, turn off taps when not needed, and prioritize safe drinking water. Follow announcements from your local water provider.",
                "Water Conservation", "Sample educational advisory — verify with your LGU", "2026-11-06");
            addContent("Guide", "Household heat-safety guide",
                "Drink water regularly, limit strenuous outdoor activity during the hottest hours, stay in shaded or cool places, and check on children, older adults, and vulnerable neighbors.",
                "Heat Safety", "Sample preparedness guide", "2026-11-06");
            addContent("FAQ", "Does El Niño mean it will not rain at all?",
                "No. El Niño can increase the likelihood of below-normal rainfall in some places, but it does not mean every location will have no rain.",
                "Basics", "Educational reference", "2026-11-06");
        }

        boolean seedQuestions;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM questions")) {
            seedQuestions = rs.next() && rs.getInt(1) == 0;
        }

        if (seedQuestions) {
            addQuestion("Basics", "What does El Niño primarily involve?",
                "Unusually warm Pacific Ocean surface waters", "A volcanic eruption",
                "A strong earthquake", "A tropical cyclone", "A",
                "El Niño is associated with unusually warm sea-surface temperatures in the central and eastern tropical Pacific.");
            addQuestion("Water Conservation", "Which action helps conserve water?",
                "Leave taps running", "Repair leaking faucets", "Wash one item at a time",
                "Water roads daily", "B", "Repairing leaks prevents unnecessary water loss.");
            addQuestion("Heat Safety", "What is a sensible action during extreme heat?",
                "Avoid drinking water", "Wear heavy dark clothing",
                "Stay in a cool or shaded place", "Do intense outdoor exercise at noon",
                "C", "A cool or shaded place helps reduce heat exposure.");
            addQuestion("Preparedness", "Where should you check for local official advisories?",
                "Unverified social media posts", "Official government and local authority channels",
                "Anonymous messages", "Old forwarded posts", "B",
                "Use official government agencies and local authorities for current guidance.");
        }
    }

    public void addContent(String type, String title, String body, String category, String source, String date) throws SQLException {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO content(type,title,body,category,source,published_date) VALUES(?,?,?,?,?,?)")) {
            p.setString(1,type); p.setString(2,title); p.setString(3,body);
            p.setString(4,category); p.setString(5,source); p.setString(6,date); p.executeUpdate();
        }
    }

    public void updateContent(int id, String type, String title, String body, String category, String source, String date) throws SQLException {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "UPDATE content SET type=?,title=?,body=?,category=?,source=?,published_date=? WHERE id=?")) {
            p.setString(1,type); p.setString(2,title); p.setString(3,body);
            p.setString(4,category); p.setString(5,source); p.setString(6,date); p.setInt(7,id); p.executeUpdate();
        }
    }

    public void deleteContent(int id) throws SQLException {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("DELETE FROM content WHERE id=?")) {
            p.setInt(1,id); p.executeUpdate();
        }
    }

    public List<Content> getContent(String type, String search, String category) throws SQLException {
        List<Content> result = new ArrayList<>();
        String sql = "SELECT * FROM content WHERE (?='All' OR type=?) AND (?='' OR lower(title) LIKE ? OR lower(body) LIKE ?) AND (?='All' OR category=?) ORDER BY id DESC";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            String q = "%" + search.toLowerCase() + "%";
            p.setString(1,type); p.setString(2,type); p.setString(3,search);
            p.setString(4,q); p.setString(5,q); p.setString(6,category); p.setString(7,category);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    String t = r.getString("type");
                    Content item;
                    switch (t) {
                        case "Article" -> item = new Article(r.getInt("id"),r.getString("title"),r.getString("body"),r.getString("category"),r.getString("source"),r.getString("published_date"));
                        case "Advisory" -> item = new Advisory(r.getInt("id"),r.getString("title"),r.getString("body"),r.getString("category"),r.getString("source"),r.getString("published_date"));
                        case "FAQ" -> item = new FAQ(r.getInt("id"),r.getString("title"),r.getString("body"),r.getString("category"),r.getString("source"),r.getString("published_date"));
                        default -> item = new Guide(r.getInt("id"),r.getString("title"),r.getString("body"),r.getString("category"),r.getString("source"),r.getString("published_date"));
                    }
                    result.add(item);
                }
            }
        }
        return result;
    }

    public void addQuestion(String category, String question, String a, String b, String c, String d, String correct, String explanation) throws SQLException {
        try (Connection conn = connect(); PreparedStatement p = conn.prepareStatement(
            "INSERT INTO questions(category,question,a,b,c,d,correct,explanation) VALUES(?,?,?,?,?,?,?,?)")) {
            p.setString(1,category); p.setString(2,question); p.setString(3,a); p.setString(4,b);
            p.setString(5,c); p.setString(6,d); p.setString(7,correct); p.setString(8,explanation); p.executeUpdate();
        }
    }

    public List<QuizQuestion> getQuestions(String category) throws SQLException {
        List<QuizQuestion> list = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE (?='All' OR category=?) ORDER BY RANDOM()";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1,category); p.setString(2,category);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) list.add(new QuizQuestion(r.getInt("id"),r.getString("category"),
                    r.getString("question"),r.getString("a"),r.getString("b"),r.getString("c"),
                    r.getString("d"),r.getString("correct"),r.getString("explanation")));
            }
        }
        return list;
    }

    public void deleteQuestion(int id) throws SQLException {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("DELETE FROM questions WHERE id=?")) {
            p.setInt(1,id); p.executeUpdate();
        }
    }
}
