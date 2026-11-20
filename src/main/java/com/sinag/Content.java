package com.sinag;

/** Abstraction: common fields and behavior for all information content. */
public abstract class Content {
    private int id;
    private String title, body, category, source, publishedDate;

    public Content(int id, String title, String body, String category,
                   String source, String publishedDate) {
        this.id = id; this.title = title; this.body = body;
        this.category = category; this.source = source; this.publishedDate = publishedDate;
    }
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getCategory() { return category; }
    public String getSource() { return source; }
    public String getPublishedDate() { return publishedDate; }
    public void setTitle(String v) { title = v; }
    public void setBody(String v) { body = v; }
    public void setCategory(String v) { category = v; }
    public void setSource(String v) { source = v; }
    public void setPublishedDate(String v) { publishedDate = v; }
    public abstract String getType();
    public String displayContent() { return title + "\n\n" + body; }
}
