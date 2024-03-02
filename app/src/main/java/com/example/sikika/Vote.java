package com.example.sikika;

public class Vote {
    private String userId;
    private String pollId;
    private String candidateId;

    public Vote(String userId, String pollId, String candidateId) {
        this.userId = userId;
        this.pollId = pollId;
        this.candidateId = candidateId;
    }

    // Getters and setters if needed
}
