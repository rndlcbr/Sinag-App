package com.sinag;

public abstract class Content {
    private final int id;
    private final String title;
    private final String body;
    private final String category;
    private final String source;
    private final String publishedDate;
    private final String imagePath;

    public Content(int id, String title, String body, String category, String source, String publishedDate, String imagePath) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.category = category;
        this.source = source;
        this.publishedDate = publishedDate;
        this.imagePath = imagePath == null ? "" : imagePath;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getCategory() { return category; }
    public String getSource() { return source; }
    public String getPublishedDate() { return publishedDate; }
    public String getImagePath() { return imagePath; }

    public abstract String getType();

    public String displayContent() {
        return title + "\n\n" + body;
    }
}