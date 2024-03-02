package com.example.sikika;

// HomeActivity.java
import static com.example.sikika.SignInActivity.RC_SIGN_IN;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

public class HomeActivity extends AppCompatActivity {
    private String userId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ImageView userAvatar = findViewById(R.id.userAvatar);
        TextView welcomeText = findViewById(R.id.welcomeText);
        Button createPollButton = findViewById(R.id.createPollButton);
        Button voteButton = findViewById(R.id.voteButton);
        Button myPollsButton = findViewById(R.id.myPollsButton);

        // Retrieve user's information from SharedPreferences
        SharedPreferences preferences = getSharedPreferences("user_details", MODE_PRIVATE);
        String firstName = preferences.getString("userFirstName", "");
        String photoUrl = preferences.getString("userPhotoUrl", "");

        userId = getIntent().getStringExtra("userId");

        // Display user's details
        welcomeText.setText("Hello, " + firstName);

        // Load the user's profile picture into the ImageView
        // You can use Picasso or Glide to load the image from the URL
        Picasso.get().load(photoUrl).into(userAvatar);

        userAvatar.setOnClickListener(v -> {
            // Display the Google account menu
            showGoogleAccountMenu();
        });
        createPollButton.setOnClickListener(v -> {
            // Start the CreatePollActivity when the button is clicked
            Intent createPollIntent = new Intent(HomeActivity.this, CreatePollActivity.class);
            startActivity(createPollIntent);
        });
        voteButton.setOnClickListener(v -> {
            // Show an AlertDialog to get the poll URL
            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            builder.setTitle("Enter Poll URL");

            final EditText input = new EditText(HomeActivity.this);
            builder.setView(input);

            builder.setPositiveButton("OK", (dialog, which) -> {
                String pollUrl = input.getText().toString();
                // Verify poll URL
                isValidPollUrl(pollUrl);
            });

            builder.setNegativeButton("Cancel", ((dialog, which) -> dialog.cancel()));
            builder.show();
        });
        myPollsButton.setOnClickListener(v -> {
            Intent pollIntent = new Intent(HomeActivity.this, MyPollsActivity.class);
            pollIntent.putExtra("userId", userId);
            startActivity(pollIntent);
        });
    }

    // Display the Google account menu
    private void showGoogleAccountMenu() {
        // Create a GoogleSignInClient for the current user
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Open the account picker
        googleSignInClient.signOut()
                .addOnCompleteListener(this, task -> {
                    // Successfully signed out, show the Google account picker
                    Intent signInIntent = googleSignInClient.getSignInIntent();
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                });
    }

    private boolean isValidPollUrl(String pollUrl) {
        // Extract the pollId from the entered URL
        String pollId = extractPollId(pollUrl);

        // Check if the extracted pollId is valid
        if (pollId != null && !pollId.isEmpty()) {
            // Check validity of the poll URL
            checkPollUrlValidity(pollId);
            return true;
        } else {
            // Display an error for an invalid URL format
            Toast.makeText(HomeActivity.this, "Invalid poll URL format", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private String extractPollId(String pollUrl) {
        // Extract the pollId from the URL
        // Assuming the URL format is "https://sikika-6c1f3-default-rtdb.firebaseio.com/polls/{pollId}"
        // You can adjust this logic based on the actual URL format
        String prefix = "https://sikika-6c1f3-default-rtdb.firebaseio.com/polls/";
        if (pollUrl.startsWith(prefix) && pollUrl.length() > prefix.length()) {
            return pollUrl.substring(prefix.length());
        } else {
            return null; // Invalid URL format
        }
    }

    private void checkPollUrlValidity(String pollId) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("polls");

        //Display a progress dialog loader
        ProgressDialog progressDialog = new ProgressDialog(HomeActivity.this);
        progressDialog.setMessage("Validating poll URL...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        databaseReference.orderByChild("pollId").equalTo(pollId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Dismiss the progress dialog
                progressDialog.dismiss();

                // If dataSnapshot exists, the poll URL is valid
                boolean isValid = dataSnapshot.exists();
                if (isValid) {
                    Intent voteIntent = new Intent(HomeActivity.this, VoteActivity.class);
                    voteIntent.putExtra("pollUrl", pollId); // Pass the pollId instead of the full URL
                    voteIntent.putExtra("userId", userId);
                    startActivity(voteIntent);
                } else {
                    // Display an error
                    Toast.makeText(HomeActivity.this, "Invalid poll URL", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Dismiss the progress dialog
                progressDialog.dismiss();

                // Handle the error
                Toast.makeText(HomeActivity.this, "Error checking poll URL", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
