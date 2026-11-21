package com.sinag;

public class Guide extends Content {
    public Guide(int id, String title, String body, String category, String source, String publishedDate, String imagePath) {
        super(id, title, body, category, source, publishedDate, imagePath);
    }

    @Override
    public String getType() {
        return "Guide";
    }
}