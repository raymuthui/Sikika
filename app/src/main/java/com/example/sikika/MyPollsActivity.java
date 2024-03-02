package com.example.sikika;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MyPollsActivity extends AppCompatActivity {
    private ListView pollsListView;
    private PollListAdapter adapter;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_polls);

        // Retrieve the user ID from the intent
        String userId = getIntent().getStringExtra("userId");

        //Initialize your adapter and set it to the ListView
        adapter = new PollListAdapter(this, new ArrayList<>());
        pollsListView = findViewById(R.id.pollsListView);
        pollsListView.setAdapter(adapter);

        //Set the item click listener
        pollsListView.setOnItemClickListener((adapterView, view, position, id) -> {
            // Ensure that the position is within the bounds of the adapter's data
            if (position >= 0 && position < adapter.getCount()) {
                // Get the selected Poll object
                Poll selectedPoll = adapter.getItem(position);

                // Log the selected poll ID for debugging
                Log.d("MyPollsActivity", "Selected Poll ID: " + selectedPoll.getPollId());

                // Get the timeline from the selected Poll object
                String timeline = selectedPoll.getTimeline();

                // Create an intent to start PollStatisticsActivity
                Intent intent = new Intent(MyPollsActivity.this, PollStatisticsActivity.class);
                intent.putExtra("selectedPoll", selectedPoll);
                intent.putExtra("timeline", timeline);
                startActivity(intent);
            } else {
                Log.e("MyPollsActivity", "Invalid position: " + position);
            }
        });

        //Fetch and display polls for the given user ID
        fetchUserPolls(userId);
    }

    private void fetchUserPolls(String userId) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("polls");

        databaseReference.orderByChild("creatorId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<Poll> userPolls = new ArrayList<>();

                for (DataSnapshot pollSnapshot : dataSnapshot.getChildren()) {
                    // Populate the Poll object with data
                    Poll poll = new Poll();

                    // Manually map the data to the Poll class
                    if (pollSnapshot.child("pollId").exists()) {
                        poll.setPollId(pollSnapshot.child("pollId").getValue(String.class));
                    }
                    if (pollSnapshot.child("pollName").exists()) {
                        poll.setPollName(pollSnapshot.child("pollName").getValue(String.class));
                    }
                    if (pollSnapshot.child("creatorId").exists()) {
                        poll.setCreatorId(pollSnapshot.child("creatorId").getValue(String.class));
                    }
                    if (pollSnapshot.child("timeline").exists()) {
                        poll.setTimeline(pollSnapshot.child("timeline").getValue(String.class));
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

                    // Add the poll to the list
                    userPolls.add(poll);
                }

                adapter.setPolls(userPolls);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyPollsActivity.this, "Failed to fetch polls: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
