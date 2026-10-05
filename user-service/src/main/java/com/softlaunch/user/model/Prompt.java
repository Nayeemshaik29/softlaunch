package com.softlaunch.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Prompt {

    @Column(nullable = false, length = 100)
    private String question;

    @Column(nullable = false, length = 300)
    private String answer;

    protected Prompt() {
    }

    public Prompt(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }
}