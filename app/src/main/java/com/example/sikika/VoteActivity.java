package com.example.sikika;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VoteActivity extends AppCompatActivity {

    private TextView pollNameTextView;
    private RecyclerView candidatesRecyclerView;
    private String pollUrl;
    private String userId; // You may need to pass the user ID from HomeActivity or get it in some other way

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote);

        // Get the user ID from the intent
        userId = getIntent().getStringExtra("userId");

        pollNameTextView = findViewById(R.id.pollNameTextView);
        candidatesRecyclerView = findViewById(R.id.candidatesRecyclerView);

        // Get the poll URL from the intent
        pollUrl = getIntent().getStringExtra("pollUrl");

        // Load the poll details and display them
        loadPollDetails();

    }

    private void loadPollDetails() {
        // Check if the user is authenticated
        if (userId != null && !userId.isEmpty()) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("polls");

        databaseReference.orderByChild("pollId").equalTo(pollUrl).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    if (dataSnapshot.exists()) {
                        // Get the poll details
                        DataSnapshot pollSnapshot = dataSnapshot.getChildren().iterator().next();

                        // Manually map the data to the Poll class
                        Poll poll = new Poll();
                        if (pollSnapshot.child("pollId").exists()) {
                            poll.setPollId(pollSnapshot.child("pollId").getValue(String.class));
                        }
                        if (pollSnapshot.child("pollName").exists()) {
                            poll.setPollName(pollSnapshot.child("pollName").getValue(String.class));
                        }
                        if (pollSnapshot.child("creatorId").exists()) {
                            poll.setCreatorId(pollSnapshot.child("creatorId").getValue(String.class));
                        }

                        // Map candidates manually
                        ArrayList<Poll.Candidate> candidates = new ArrayList<>();
                        for (DataSnapshot candidateSnapshot : pollSnapshot.child("candidates").getChildren()) {
                            Poll.Candidate candidate = new Poll.Candidate();
                            if (candidateSnapshot.child("name").exists()) {
                                candidate.setName(candidateSnapshot.child("name").getValue(String.class));
                            }
                            if (candidateSnapshot.child("pictureUrl").exists()) {
                                candidate.setPictureUrl(candidateSnapshot.child("pictureUrl").getValue(String.class));
                            }
                            candidates.add(candidate);
                        }
                        poll.setCandidates(candidates);

                        // Map poll statistics manually
                        if (pollSnapshot.child("pollStatistics").exists()) {
                            DataSnapshot statisticsSnapshot = pollSnapshot.child("pollStatistics");
                            // Convert the statistics to a Map
                            Map<String, Integer> pollStatistics = new HashMap<>();
                            for (DataSnapshot statSnapshot : statisticsSnapshot.getChildren()) {
                                String candidateName = statSnapshot.getKey();
                                int votes = statSnapshot.getValue(Integer.class);
                                pollStatistics.put(candidateName, votes);
                            }
                            poll.setPollStatistics(pollStatistics);
                        }
                        // Set the timeline
                        if (pollSnapshot.child("timeline").exists()) {
                            poll.setTimeline(pollSnapshot.child("timeline").getValue(String.class));
                        }

                        // Check if the user has already voted
                        if (pollHasUserVoted(poll, userId)) {
                            // User has already voted, show a message or take appropriate action
                            showAlreadyVotedDialog();
                        } else {
                            // Check if the current date and time are within the timeline
                            if (isWithinTimeline(poll.getTimeline())) {
                                // User has not voted, proceed to display the poll details
                                displayPollDetails(poll);
                            } else {
                                showOutsideTimelineDialog();
                            }
                        }
                    } else {
                        // Handle the case where the poll does not exist
                        Log.d("VoteActivity", "Poll not found");
                        Toast.makeText(VoteActivity.this, "Poll not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } catch (Exception e) {
                    Log.e("VoteActivity", "Error in onDataChange: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle the error
                Toast.makeText(VoteActivity.this, "Error loading poll details", Toast.LENGTH_SHORT).show();
            }
        });
    } else {
            // User is not authenticated, handle accordingly (e.g., redirect to sign-in)
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            // Add code to redirect the user to the sign-in activity or perform other actions
        }
    }

    // Helper method to check if the user has already voted
    private boolean pollHasUserVoted(Poll poll, String userId) {
        // Check if the user ID is present in the poll's votedUsers list
        return poll.getVotedUsers() != null && poll.getVotedUsers().contains(userId);
    }

    // Helper method to display poll details
    private void displayPollDetails(Poll poll) {
        // Display the poll details
        if (poll.getPollName() != null) {
            pollNameTextView.setText(poll.getPollName());
        }

        // Display the candidates using a RecyclerView
        if (!poll.getCandidates().isEmpty()) {
            setupRecyclerView(poll.getCandidates());
        }
    }

    // Helper method to show a dialog if the user has already voted
    private void showAlreadyVotedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Already Voted");
        builder.setMessage("You have already voted in this poll.");

        // Add a button to navigate back to the home page
        builder.setPositiveButton("Go Back to Home", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Implement the navigation logic to go back to the home page
                // For example, you can use Intent to navigate to the HomeActivity
                Intent intent = new Intent(VoteActivity.this, HomeActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish(); // Close the current activity
            }
        });

        // Show the dialog
        builder.show();
    }
    private boolean isWithinTimeline(String timeline) {
        Log.d("TimelineCheck", "isWithinTimeline method called");
        if (timeline == null || timeline.isEmpty()) {
            // Handle the case where the timeline is null or empty
            return false;
        }

        try {
            // Parse the timeline string to obtain the date and time
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date timelineDate = dateFormat.parse(timeline);

            if (timelineDate != null) {
                // Compare the current date and time with the poll timeline
                Date currentDate = new Date();
                if (currentDate != null && currentDate.before(timelineDate)) {
                    // Log the timeline and current date fot debugging
                    Log.d("TimelineCheck", "Timeline: " + timeline);
                    Log.d("TimelineCheck", "Current Date: " + currentDate);
                    return true;
                } else {
                    // Log if the current date is not before the timeline
                    Log.d("TimelineCheck", "Current Date is NOT before Timeline");
                    return false;
                }
            } else {
                // Handle case where parsing failed
                Log.d("TimelineCheck", "Failed to parse timeline");
                return false;
            }
        } catch (ParseException e) {
            e.printStackTrace();
            // Log the exception for debugging
            Log.d("TimelineCheck", "Exception" + e.getMessage());
            return false;
        }
    }
    private void showOutsideTimelineDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Voting Outside Timeline");
        builder.setMessage("This poll has expired!");

        // Add a button to navigate back to the home page
        builder.setPositiveButton("Go Back to Home", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(VoteActivity.this, HomeActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish();
            }
        });

        builder.show();
    }
    private void setupRecyclerView(ArrayList<Poll.Candidate> candidates) {
        // Set up the RecyclerView with the CandidateAdapter
        CandidateAdapter candidateAdapter = new CandidateAdapter(candidates);
        candidateAdapter.setOnVoteButtonClickListener(new CandidateAdapter.OnVoteButtonClickListener() {
            @Override
            public void onVoteButtonClick(String candidateId) {
                // Handle the vote button click here
                voteForCandidate(Integer.parseInt(candidateId));
            }
        });
        candidatesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        candidatesRecyclerView.setAdapter(candidateAdapter);
    }

    private void voteForCandidate(int position) {
      DatabaseReference pollsReference = FirebaseDatabase.getInstance().getReference().child("polls").child(pollUrl);

        pollsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    DataSnapshot pollSnapshot = dataSnapshot.getChildren().iterator().next();
                    Poll poll = dataSnapshot.getValue(Poll.class);

                    if (poll != null && !poll.getCandidates().isEmpty() && position < poll.getCandidates().size()) {
                        // Check if the user has already voted
                        if (!pollHasUserVoted(poll, userId)) {
                            // Check if the current date and time are within the set timeline
                            Log.d("VoteActivity", "Timeline: " + poll.getTimeline());
                            if (isWithinTimeline(poll.getTimeline())) {
                                // Update the vote count for the selected candidate
                                Poll.Candidate selectedCandidate = poll.getCandidates().get(position);
                                if (selectedCandidate != null) {
                                    // Increment the vote count
                                    int currentVotes = poll.getPollStatistics().get(selectedCandidate.getName());
                                    poll.getPollStatistics().put(selectedCandidate.getName(), currentVotes + 1);

                                    // Mark the user as voted
                                    markUserAsVotedInDatabase(poll, userId);

                                    // Save the updated poll object back to the database
                                    pollsReference.setValue(poll);

                                    // Display a success message using AlertDialog
                                    showSuccessDialog();

                                    // TODO: Implement any additional actions after voting (e.g., navigating to another activity)
                                }
                            } else {
                                // User is trying to vote after deadline has passed
                                showOutsideTimelineDialog();
                            }
                        } else {
                            // User has already voted
                            showAlreadyVotedDialog();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle the error
                Toast.makeText(VoteActivity.this, "Error updating vote count", Toast.LENGTH_SHORT).show();
            }
        });
    }
    // Helper method to mark the user as voted
    private void markUserAsVotedInDatabase(Poll poll, String userId) {
        // Add the user ID to the votedUsers list in the database
        DatabaseReference votedUsersReference = FirebaseDatabase.getInstance().getReference().child("votedUsers");

        if (poll.getVotedUsers() == null) {
            poll.setVotedUsers(new ArrayList<>());
        }
        poll.getVotedUsers().add(userId);

        // Save the updated votedUsers list to the database
        votedUsersReference.setValue(poll.getVotedUsers());
    }
    private void showSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Vote Cast Successfully");
        builder.setMessage("Thank you for casting your vote!");

        // Add a button to navigate back to the home page
        builder.setPositiveButton("Go Back to Home", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Implement the navigation logic to go back to the home page
                // For example, you can use Intent to navigate to the HomeActivity
                Intent intent = new Intent(VoteActivity.this, HomeActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish(); // Close the current activity
            }
        });

        // Show the dialog
        builder.show();
    }
}
