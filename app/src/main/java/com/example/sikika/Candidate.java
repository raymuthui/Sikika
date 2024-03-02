package com.example.sikika;


public class Candidate {
    private String candidateName;
    private String pictureUrl;

    public Candidate(String candidateName, String pictureUrl) {
        this.candidateName = candidateName;
        this.pictureUrl = pictureUrl;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }
}

