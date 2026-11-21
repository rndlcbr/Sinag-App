package com.sinag;

public class Advisory extends Content {
    public Advisory(int id, String title, String body, String category, String source, String publishedDate, String imagePath) {
        super(id, title, body, category, source, publishedDate, imagePath);
    }

    @Override
    public String getType() {
        return "Advisory";
    }
}