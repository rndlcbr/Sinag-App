package com.sinag;
public class Article extends Content {
    public Article(int id, String title, String body, String category, String source, String date) {
        super(id, title, body, category, source, date);
    }
    @Override public String getType() { return "Article"; }
}
