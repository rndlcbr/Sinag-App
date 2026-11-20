package com.sinag;
public class Guide extends Content {
    public Guide(int id, String title, String body, String category, String source, String date) {
        super(id, title, body, category, source, date);
    }
    @Override public String getType() { return "Guide"; }
}
