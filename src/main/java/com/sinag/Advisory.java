package com.sinag;
public class Advisory extends Content {
    public Advisory(int id, String title, String body, String category, String source, String date) {
        super(id, title, body, category, source, date);
    }
    @Override public String getType() { return "Advisory"; }
}
