package com.example.sikika;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class PollStatisticsActivity extends AppCompatActivity {

    private TextView pollNameTextView;
    private TextView statisticsTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll_statistics);

        pollNameTextView = findViewById(R.id.pollNameTextView);
        statisticsTextView = findViewById(R.id.statisticsTextView);

        // Get the poll ID from the intent
        Poll poll = (Poll) getIntent().getSerializableExtra("selectedPoll");
        String pollId = poll.getPollId();

        // Load and display poll statistics
        loadPollStatistics(pollId);
    }

    private void loadPollStatistics(String pollId) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("polls").child(pollId);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Poll poll = dataSnapshot.getValue(Poll.class);
                    if (poll != null) {
                        // Set the poll name in the TextView
                        pollNameTextView.setText(poll.getPollName());

                        // Build and display the statistics
                        String statistics = buildStatisticsString(poll.getPollStatistics());
                        statisticsTextView.setText(statistics);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database error
            }
        });
    }

    private String buildStatisticsString(Map<String, Integer> pollStatistics) {
        StringBuilder statistics = new StringBuilder("Poll Statistics:\n");
        for (Map.Entry<String, Integer> entry : pollStatistics.entrySet()) {
            statistics.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return statistics.toString();
    }
}
