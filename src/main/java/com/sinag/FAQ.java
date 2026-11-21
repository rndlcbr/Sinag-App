package com.sinag;

public class FAQ extends Content {
    public FAQ(int id, String title, String body, String category, String source, String publishedDate, String imagePath) {
        super(id, title, body, category, source, publishedDate, imagePath);
    }

    @Override
    public String getType() {
        return "FAQ";
    }
}