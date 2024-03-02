package com.example.sikika;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class PollListAdapter extends ArrayAdapter<Poll> {
    private Context context;
    private ArrayList<Poll> polls;

    public PollListAdapter(Context context, ArrayList<Poll> polls) {
        super(context, 0, polls);
        this.context = context;
        this.polls = polls;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position
        Poll poll = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.poll_item, parent, false);
        }

        TextView pollNameTextView = convertView.findViewById(R.id.pollNameTextView);

        // Populate data into the template view using the data object
        if (poll != null) {
            // Populate the data into the template view using the data object
            pollNameTextView.setText(poll.getPollName());
        }

        // Return the completed view to render on screen
        return convertView;
    }

    // Helper method to update the adapter's data
    public void setPolls(ArrayList<Poll> polls) {
        this.polls.clear();
        this.polls.addAll(polls);
        notifyDataSetChanged();
    }
}
