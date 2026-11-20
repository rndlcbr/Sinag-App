package com.sinag;
public class QuizQuestion {
    private final int id;
    private final String category, question, a, b, c, d, correct, explanation;
    public QuizQuestion(int id, String category, String question, String a, String b,
                        String c, String d, String correct, String explanation) {
        this.id=id; this.category=category; this.question=question; this.a=a; this.b=b;
        this.c=c; this.d=d; this.correct=correct; this.explanation=explanation;
    }
    public int getId(){return id;} public String getCategory(){return category;}
    public String getQuestion(){return question;} public String getExplanation(){return explanation;}
    public String getCorrect(){return correct;}
    public String[] getChoices(){return new String[]{a,b,c,d};}
    public String getChoice(String letter) {
        return switch(letter) { case "A" -> a; case "B" -> b; case "C" -> c; default -> d; };
    }
    public boolean checkAnswer(String answer){return correct.equalsIgnoreCase(answer);}
}
