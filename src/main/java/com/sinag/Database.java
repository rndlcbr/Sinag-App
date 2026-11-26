package com.sinag;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Handles all Firebase Realtime Database REST API operations
 * (seeding initial data and performing Create, Read, Update, Delete actions).
 */
public class Database {
    private static final String FIREBASE_URL = "https://sinag-system-1533e-default-rtdb.asia-southeast1.firebasedatabase.app";
    private final HttpClient httpClient;

    public Database() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        try {
            insertStarterData();
        } catch (SQLException e) {
            throw new RuntimeException("Firebase initialization failed: " + e.getMessage(), e);
        }
    }

    // --- HTTP REST HELPERS ---

    private String sendRequest(String path, String method, String jsonBody) throws SQLException {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(FIREBASE_URL + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json");

            switch (method) {
                case "GET"    -> builder.GET();
                case "PUT"    -> builder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
                case "DELETE" -> builder.DELETE();
                default       -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SQLException("Firebase HTTP " + response.statusCode() + ": " + response.body());
            }
            return response.body();
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Firebase connection error (check internet/rules): " + e.getMessage(), e);
        }
    }

    /**
     * Parses Firebase's JSON response (which may be a JSONObject map or a JSONArray
     * when keys are sequential integers like 1, 2, 3) into a list of JSONObjects.
     */
    private List<JSONObject> fetchNodeRecords(String nodePath) throws SQLException {
        String raw = sendRequest(nodePath + ".json", "GET", null);
        List<JSONObject> list = new ArrayList<>();
        if (raw == null || raw.isBlank() || raw.trim().equals("null")) {
            return list;
        }

        String trimmed = raw.trim();
        if (trimmed.startsWith("{")) {
            JSONObject map = new JSONObject(trimmed);
            for (String key : map.keySet()) {
                if (!map.isNull(key)) {
                    list.add(map.getJSONObject(key));
                }
            }
        } else if (trimmed.startsWith("[")) {
            JSONArray arr = new JSONArray(trimmed);
            for (int i = 0; i < arr.length(); i++) {
                if (!arr.isNull(i)) {
                    list.add(arr.getJSONObject(i));
                }
            }
        }
        return list;
    }

    private int getNextId(String nodePath) throws SQLException {
        int maxId = 0;
        for (JSONObject obj : fetchNodeRecords(nodePath)) {
            int id = obj.optInt("id", 0);
            if (id > maxId) {
                maxId = id;
            }
        }
        return maxId + 1;
    }

    // --- STARTER SEED DATA (Runs only if Firebase nodes are empty) ---

    private void insertStarterData() throws SQLException {
        if (fetchNodeRecords("/content").isEmpty()) {
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

        if (fetchNodeRecords("/questions").isEmpty()) {
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
        int newId = getNextId("/content");
        JSONObject obj = new JSONObject();
        obj.put("id", newId);
        obj.put("type", type);
        obj.put("title", title);
        obj.put("body", body);
        obj.put("category", category);
        obj.put("source", source);
        obj.put("published_date", date);
        obj.put("image_path", imagePath == null ? "" : imagePath);

        sendRequest("/content/item_" + newId + ".json", "PUT", obj.toString());
    }

    public void updateContent(int id, String type, String title, String body, String category, String source, String date, String imagePath) throws SQLException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("type", type);
        obj.put("title", title);
        obj.put("body", body);
        obj.put("category", category);
        obj.put("source", source);
        obj.put("published_date", date);
        obj.put("image_path", imagePath == null ? "" : imagePath);

        sendRequest("/content/item_" + id + ".json", "PUT", obj.toString());
    }

    public void deleteContent(int id) throws SQLException {
        sendRequest("/content/item_" + id + ".json", "DELETE", null);
    }

    public List<Content> getContent(String type, String searchKeyword, String category) throws SQLException {
        List<Content> results = new ArrayList<>();
        String keywordLower = searchKeyword == null ? "" : searchKeyword.toLowerCase();

        for (JSONObject obj : fetchNodeRecords("/content")) {
            int id = obj.optInt("id", 0);
            String contentType = obj.optString("type", "Guide");
            String title = obj.optString("title", "");
            String body = obj.optString("body", "");
            String cat = obj.optString("category", "Basics");
            String source = obj.optString("source", "");
            String date = obj.optString("published_date", "");
            String img = obj.optString("image_path", "");

            boolean matchesType = type.equals("All") || contentType.equals(type);
            boolean matchesKeyword = keywordLower.isEmpty()
                    || title.toLowerCase().contains(keywordLower)
                    || body.toLowerCase().contains(keywordLower);
            boolean matchesCategory = category.equals("All") || cat.equals(category);

            if (matchesType && matchesKeyword && matchesCategory) {
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

        // Order newest first (ORDER BY id DESC)
        results.sort(Comparator.comparingInt(Content::getId).reversed());
        return results;
    }

    // --- QUIZ QUESTION CRUD METHODS ---

    public void addQuestion(String category, String question, String a, String b, String c, String d, String correct, String explanation) throws SQLException {
        int newId = getNextId("/questions");
        JSONObject obj = new JSONObject();
        obj.put("id", newId);
        obj.put("category", category);
        obj.put("question", question);
        obj.put("a", a);
        obj.put("b", b);
        obj.put("c", c);
        obj.put("d", d);
        obj.put("correct", correct);
        obj.put("explanation", explanation);

        sendRequest("/questions/q_" + newId + ".json", "PUT", obj.toString());
    }

    public void updateQuestion(int id, String category, String question, String a, String b, String c, String d, String correct, String explanation) throws SQLException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("category", category);
        obj.put("question", question);
        obj.put("a", a);
        obj.put("b", b);
        obj.put("c", c);
        obj.put("d", d);
        obj.put("correct", correct);
        obj.put("explanation", explanation);

        sendRequest("/questions/q_" + id + ".json", "PUT", obj.toString());
    }

    public List<QuizQuestion> getQuestions(String category) throws SQLException {
        List<QuizQuestion> questions = new ArrayList<>();

        for (JSONObject obj : fetchNodeRecords("/questions")) {
            String cat = obj.optString("category", "Basics");
            if (category.equals("All") || cat.equals(category)) {
                questions.add(new QuizQuestion(
                    obj.optInt("id", 0),
                    cat,
                    obj.optString("question", ""),
                    obj.optString("a", ""),
                    obj.optString("b", ""),
                    obj.optString("c", ""),
                    obj.optString("d", ""),
                    obj.optString("correct", "A"),
                    obj.optString("explanation", "")
                ));
            }
        }

        // Randomize question order (ORDER BY RANDOM())
        Collections.shuffle(questions);
        return questions;
    }

    public void deleteQuestion(int id) throws SQLException {
        sendRequest("/questions/q_" + id + ".json", "DELETE", null);
    }
}