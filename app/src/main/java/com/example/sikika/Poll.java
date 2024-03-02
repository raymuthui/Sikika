package com.example.sikika;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

public class Poll implements Serializable{
    private String pollId;
    private String pollName;
    private ArrayList<Candidate> candidates;
    private Map<String, Integer> pollStatistics; // Store candidate votes
    private String creatorId;
    private List<String> votedUsers;
    private String timeline;

    public Poll() {

    }

    public Poll(String pollId, String pollName, ArrayList<Poll.Candidate> candidates, String creatorId, String timeline) {
        // Default constructor required for Firebase Realtime Database
        this.pollId = pollId;
        this.pollName = pollName;
        this.candidates = candidates;
        this.pollStatistics = new HashMap<>();
        this.creatorId = creatorId;
        this.timeline = timeline;
        // Initialize poll statistics for each candidate
        for (Candidate candidate : candidates) {
            pollStatistics.put(candidate.getName(), 0);
        }

    }

    public String getPollId() {
        return pollId;
    }

    public void setPollId(String pollId) {
        this.pollId = pollId;
    }

    public String getPollName() {
        return pollName;
    }

    public void setPollName(String pollName) {
        this.pollName = pollName;
    }

    public ArrayList<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(ArrayList<Candidate> candidates) {
        this.candidates = candidates;
    }

    public Map<String, Integer> getPollStatistics() {
        return pollStatistics;
    }

    public void setPollStatistics(Map<String, Integer> pollStatistics) {
        this.pollStatistics = pollStatistics;
    }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

    public List<String> getVotedUsers() {
        return votedUsers;
    }
    public void setVotedUsers(List<String> votedUsers) {
        this.votedUsers = votedUsers;
    }
    public void setTimeline(String timeline) { this.timeline = timeline;}
    public String getTimeline() {
        return timeline;
    }
    // Define a Candidate class to hold candidate details including the picture URL
    public static class Candidate implements Serializable {
        private String name;
        private String pictureUrl; // URL or unique identifier for candidate picture

        public Candidate() {
            // Default constructor required for Firebase Realtime Database
        }

        public Candidate(String name, String pictureUrl) {
            this.name = name;
            this.pictureUrl = pictureUrl;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPictureUrl() {
            return pictureUrl; }

        public void setPictureUrl(String pictureUrl) {
            this.pictureUrl = pictureUrl;
        }
    }
}
