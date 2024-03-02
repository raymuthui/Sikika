package com.example.sikika;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;

import android.os.Bundle;
import android.content.Intent;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class SignInActivity extends AppCompatActivity {


    protected static final int RC_SIGN_IN = 123;
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        //initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        //Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        //Set a click listener for the Google Sign-In Button
        SignInButton googleSignInButton = findViewById(R.id.googleSignInButton);
        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInWithGoogle();
            }
        });


    }

    //Perform Google Sign-In
    private void signInWithGoogle() {
        // Show loader
        ProgressBar progressBar = findViewById(R.id.signInProgressBar);
        progressBar.setVisibility(View.VISIBLE);

        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    //Handle the result of the Google Sign-In process
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            task.addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
                @Override
                public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                    // Hide progress bar
                    ProgressBar progressBar = findViewById(R.id.signInProgressBar);
                    progressBar.setVisibility(View.GONE);
                    try {
                        // Google Sign-In was successful, you can now use the user's account.
                        GoogleSignInAccount account = task.getResult(ApiException.class);

                        // Authenticate with Firebase using the GoogleSignInAccount
                        firebaseAuthWithGoogle(account);
                    } catch (ApiException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's info
                            FirebaseUser user = mAuth.getCurrentUser();

                            // Save the user ID to Firebase Realtime Database
                            saveUserIdToDatabase(user.getUid(), account.getGivenName(), account.getPhotoUrl().toString());

                            // Redirect to HomeActivity
                            Intent homeIntent = new Intent(SignInActivity.this, HomeActivity.class);
                            homeIntent.putExtra("userFirstName", account.getGivenName());
                            homeIntent.putExtra("userPhotoUrl", account.getPhotoUrl().toString());
                            homeIntent.putExtra("userId", user.getUid());
                            startActivity(homeIntent);
                            finish();
                        } else {
                            // Sign in fails
                            Toast.makeText(SignInActivity.this, "Authentication failed." + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserIdToDatabase(String userId, String userFirstName, String userPhotoUrl) {
        // Save user details to SharedPreferences
        SharedPreferences preferences = getSharedPreferences("user_details", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("userId", userId);
        editor.putString("userFirstName", userFirstName);
        editor.putString("userPhotoUrl", userPhotoUrl);
        editor.apply();

        // Save the user ID to Firebase Realtime Database
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child("users").child(userId).setValue(true);

    }
}
