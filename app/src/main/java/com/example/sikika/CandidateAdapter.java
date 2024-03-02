package com.example.sikika;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class CandidateAdapter extends RecyclerView.Adapter<CandidateAdapter.ViewHolder> {


    private List<Poll.Candidate> candidates = new ArrayList<>();
    private OnVoteButtonClickListener onVoteButtonClickListener;

    public CandidateAdapter(ArrayList<Poll.Candidate> candidates) {
        this.candidates = candidates;
    }

    public void setCandidates(List<Poll.Candidate> candidates) {
        this.candidates = candidates;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_candidate, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Poll.Candidate candidate = candidates.get(position);

        Log.d("CandidateAdapter", "Binding candidate: " + candidate.getName());

        holder.candidateNameTextView.setText(candidate.getName());

        // Load candidate photo using Picasso
        Picasso.get()
                .load(candidate.getPictureUrl())
                .placeholder(R.drawable.default_profile_image)
                .error(R.drawable.default_profile_image)
                .into(holder.candidateImageView);

        // Set up the click listener for the entire item
        holder.voteButton.setOnClickListener(v -> {
            // Handle item click, you can also implement a more detailed logic here
            Log.d("CandidateAdapter", "Vote button clicked for candidate: " + candidate.getName());
            // For now, let's just call the voteForCandidate method
            voteForCandidate(position);
        });
    }

    @Override
    public int getItemCount() {
        return candidates.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView candidateImageView;
        public TextView candidateNameTextView;
        public Button voteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            candidateImageView = itemView.findViewById(R.id.candidateImageView);
            candidateNameTextView = itemView.findViewById(R.id.candidateNameTextView);
            voteButton = itemView.findViewById(R.id.voteButton);
        }
    }

    private void voteForCandidate(int position) {
        // TODO: Implement the logic for registering the user's vote
        // You can use the position as the candidate ID or pass it to your voting logic
        // Notify the activity or fragment by calling the listener (if set)
        if (onVoteButtonClickListener != null) {
            onVoteButtonClickListener.onVoteButtonClick(String.valueOf(position));
        }
    }
    public interface OnVoteButtonClickListener {
        void onVoteButtonClick(String candidateId);
    }

    public void setOnVoteButtonClickListener(OnVoteButtonClickListener onVoteButtonClickListener) {
        this.onVoteButtonClickListener = onVoteButtonClickListener;
    }
}