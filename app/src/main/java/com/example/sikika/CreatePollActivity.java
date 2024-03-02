package com.example.sikika;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.Manifest;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.net.Uri;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.sikika.Poll.Candidate;

public class CreatePollActivity extends AppCompatActivity {
    private EditText pollNameEditText;
    private Button createCandidatesButton;
    private LinearLayout candidatesLayout;
    private ArrayList<CandidateView> candidateViews = new ArrayList<>();
    private Button createPollButton;
    private String userId;
    private TextInputEditText timelineEditText;
    private Calendar selectedDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_poll);

        pollNameEditText = findViewById(R.id.pollNameEditText);
        createCandidatesButton = findViewById(R.id.createCandidatesButton);
        candidatesLayout = findViewById(R.id.candidatesLayout);
        createPollButton = findViewById(R.id.createPollButton);
        timelineEditText = findViewById(R.id.timelineEditText);
        selectedDateTime = Calendar.getInstance();

        createCandidatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCandidateInputDialog();
            }
        });

        createPollButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPoll();
            }
        });

        timelineEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateTimePicker();
            }
        });
        retrieveUserIdFromAuthentication(new Callback<String>() {
            @Override
            public void onSuccess(String result) {
                userId = result;
            }

            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void showDateTimePicker() {
        Calendar currentDateTime = selectedDateTime;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        selectedDateTime.set(Calendar.YEAR, year);
                        selectedDateTime.set(Calendar.MONTH, month);
                        selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        showTimePicker();
                    }
                },
                currentDateTime.get(Calendar.YEAR),
                currentDateTime.get(Calendar.MONTH),
                currentDateTime.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }
    private void showTimePicker() {
        Calendar currentDateTime = selectedDateTime;

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedDateTime.set(Calendar.MINUTE, minute);

                        updateTimelineEditText();
                    }
                },
                currentDateTime.get(Calendar.HOUR_OF_DAY),
                currentDateTime.get(Calendar.MINUTE),
                false
        );

        timePickerDialog.show();
    }
    private void updateTimelineEditText() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        timelineEditText.setText(dateFormat.format(selectedDateTime.getTime()));
    }
    private void showCandidateInputDialog() {
        // Inflate the CandidateView layout
        View candidateView = getLayoutInflater().inflate(R.layout.candidate_view, null);

        // Find the views in the inflated layout
        EditText candidateNameEditText = candidateView.findViewById(R.id.candidateNameEditText);
        ImageView candidatePictureImageView = candidateView.findViewById(R.id.candidatePictureImageView);

        // Add the CandidateView to the candidatesLayout
        candidatesLayout.addView(candidateView);

        //Pass the activity instance to the CandidateView constructor
        candidateViews.add(new CandidateView(candidateNameEditText, candidatePictureImageView, this));
    }


    private void createPoll() {
        String pollName = pollNameEditText.getText().toString();
        String timeline = timelineEditText.getText().toString();

        if (pollName.isEmpty()) {
            pollNameEditText.setError("Poll name is required");
            pollNameEditText.requestFocus();
            return;
        }

        ArrayList<Candidate> candidates = new ArrayList<>();
        for (CandidateView candidateView : candidateViews) {
            String candidateName = candidateView.getCandidateNameEditText().getText().toString();
            String pictureUrl = candidateView.getPictureUrl();
            if (!candidateName.isEmpty()) {
                candidates.add(new Candidate(candidateName, pictureUrl));
            }
        }

        // Ensure at least one candidate is provided
        if (candidates.isEmpty()) {
            // Handle the case where no candidates are provided
            return;
        }

        //Validate if a timeline is selected
        if (timeline.isEmpty()) {
            Toast.makeText(this, "Please select a timeline for your poll", Toast.LENGTH_SHORT).show();
            return;
        }


        // Generate a unique poll ID
        String pollId = generateUniquePollId();

        // Retrieve the stored user ID from Firebase Realtime Database
        retrieveUserIdFromAuthentication(new Callback<String>() {
            @Override
            public void onSuccess(String uid) {
                //Pass the user's ID as the creatorId
                Poll poll = new Poll(pollId, pollName, candidates, uid, timeline);

                //Create a ref to the Firebase Realtime Database
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

                //Create a new poll in the database
                databaseReference.child("polls").child(pollId).setValue(poll);

                //TODO: Generate and display the sharable link based on the poll ID
                String shareableLink = "https://sikika-6c1f3-default-rtdb.firebaseio.com/polls/" + pollId;

                //Display the Sharable link
                showDialogWithSharableLink(shareableLink);
            }

            @Override
            public void onFailure(Exception e) {
                //Handle the failure
                e.printStackTrace();
            }
        });

    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    private void retrieveUserIdFromAuthentication(Callback<String> callback) {
        // Get the UID of the authenticated user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String uid = user.getUid();
            callback.onSuccess(uid);
        } else {
            callback.onFailure(new Exception("User not authenticated"));
        }
    }

    public class CandidateView {
        private EditText candidateNameEditText;
        private ImageView candidatePictureImageView;
        private Activity activity;
        private Uri selectedImageUri;
        private String pictureUrl;
        private static final int REQUEST_IMAGE_CAPTURE = 1;
        private static final int REQUEST_IMAGE_GALLERY = 2;
        private String pictureFilePath;

        public CandidateView(EditText nameEditText, ImageView pictureImageView, Activity activity) {
            this.activity = activity;
            this.candidateNameEditText = nameEditText;
            this.candidatePictureImageView = pictureImageView;
            this.candidatePictureImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectImageSource();
                }
            });
        }

        public EditText getCandidateNameEditText() {
            return candidateNameEditText;
        }
        public void setPictureFilePath(String path) {
            this.pictureFilePath = path;
        }

        public ImageView getCandidatePictureImageView() {
            return candidatePictureImageView;
        }
        public String getPictureUrl() {
            // Return the URL or path of the selected image
            if (pictureUrl != null) {
                return pictureUrl;
            } else {
                // Return a placeholder URL if no image is selected
                return "https://example.com/placeholder.jpg";
            }
        }

        private void selectImageSource() {
            final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Add Photo");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    if (options[item].equals("Take Photo")) {
                        dispatchTakePictureIntent();
                    } else if (options[item].equals("Choose from Gallery")) {
                        dispatchChoosePictureIntent();
                    } else if (options[item].equals("Cancel")) {
                        dialog.dismiss();
                    }
                }
            });
            builder.show();
        }

        private void uploadPictureToStorage(Bitmap imageBitmap, final CandidateView candidateView) {
            // Create a unique filename (e.g., candidate_<timestamp>.jpg)
            String fileName = "candidate_" + System.currentTimeMillis() + ".jpg";

            // Get a reference to the Firebase Storage location
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("candidates/" + fileName);

            // Convert the bitmap to a byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();

            // Upload the byte array to Firebase Storage
            UploadTask uploadTask = storageRef.putBytes(data);

            // Listen for the success or failure of the upload
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Get the download URL of the uploaded picture
                    storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri downloadUri) {
                            // Use the download URL to set the picture URL for the Candidate
                            candidateView.setPictureUrl(downloadUri.toString());

                            // Save the file path of the uploaded image
                            candidateView.setPictureFilePath(downloadUri.toString());
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Handle the failure of the picture upload
                }
            });
        }

        public void setPictureUrl(String url) {
            if (url != null) {
                this.pictureUrl = url;
            }
        }

    }

    private String generateUniquePollId() {
        // Generate a unique poll ID based on a timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    private void showDialogWithSharableLink(String link) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflate the custom dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.share_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Find the TextViews and buttons in the custom dialog layout
        TextView headingTextView = dialogView.findViewById(R.id.dialog_heading);
        TextView messageTextView = dialogView.findViewById(R.id.dialog_message);
        TextView linkTextView = dialogView.findViewById(R.id.pollLink);
        Button shareButton = dialogView.findViewById(R.id.shareButton);
        Button okButton = dialogView.findViewById(R.id.okButton);

        // Set the text for the heading, message, and link
        headingTextView.setText("Poll created successfully");
        messageTextView.setText("Here's the link to your poll:");
        linkTextView.setText(link);

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Implement sharing functionality here (e.g., using Intent.createChooser)
                sharePoll(link);
                dialog.dismiss();
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle the "OK" button functionality here
                Intent pollsIntent = new Intent(CreatePollActivity.this, MyPollsActivity.class);
                pollsIntent.putExtra("userId", userId);
                startActivity(pollsIntent);
                dialog.dismiss();
            }
        });

        dialog.show();
    }



    private void sharePoll(String link) {
        // Create an Intent to share the link
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, link);

        // Create a Chooser dialog to display sharing options
        Intent chooser = Intent.createChooser(shareIntent, "Share via");

        // Add additional sharing options (e.g., copy link)
        Intent copyLinkIntent = new Intent(Intent.ACTION_SEND);
        copyLinkIntent.setType("text/plain");
        copyLinkIntent.putExtra(Intent.EXTRA_TEXT, link);
        copyLinkIntent.setPackage("com.example.sikika"); // Replace with your package name for the copy link option

        // Create a list of Intents for the sharing options
        List<Intent> targetIntents = new ArrayList<>();
        targetIntents.add(copyLinkIntent);

        // Check if social apps are available on the device and add them to the list
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> socialApps = packageManager.queryIntentActivities(shareIntent, 0);
        for (ResolveInfo resolveInfo : socialApps) {
            String packageName = resolveInfo.activityInfo.packageName;
            Intent socialAppIntent = new Intent(Intent.ACTION_SEND);
            socialAppIntent.setType("text/plain");
            socialAppIntent.putExtra(Intent.EXTRA_TEXT, link);
            socialAppIntent.setPackage(packageName);
            targetIntents.add(socialAppIntent);
        }

        // Convert the list of Intents to an array
        Intent[] intentsArray = targetIntents.toArray(new Intent[targetIntents.size()]);
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentsArray);

        // Show the Chooser dialog
        startActivity(chooser);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            CandidateView candidateView = null;
            if (!candidateViews.isEmpty()) {
                candidateView = candidateViews.get(candidateViews.size() - 1); // Assuming you want to update the last added candidate
            }

            if (candidateView != null) {
                if (requestCode == CandidateView.REQUEST_IMAGE_CAPTURE) {
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    candidateView.getCandidatePictureImageView().setImageBitmap(imageBitmap);
                    // Upload the picture to Firebase Storage
                    candidateView.uploadPictureToStorage(imageBitmap, candidateView);
                } else if (requestCode == CandidateView.REQUEST_IMAGE_GALLERY) {
                    Uri selectedImage = data.getData();
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(selectedImage);
                        Bitmap imageBitmap = BitmapFactory.decodeStream(inputStream);
                        candidateView.getCandidatePictureImageView().setImageBitmap(imageBitmap);
                        // Upload the picture to Firebase Storage
                        candidateView.uploadPictureToStorage(imageBitmap, candidateView);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    private String saveImageToFile(Bitmap bitmap) {
        // Create a file in the app's cache directory
        File file = new File(getCacheDir(), "candidate_image.jpg");

        try {
            // Convert the bitmap to a ByteArrayOutputStream
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);

            // Write the bytes to the file
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(byteArrayOutputStream.toByteArray());
            fileOutputStream.flush();
            fileOutputStream.close();

            // Return the file path
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
    protected void launchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void launchChoosePictureIntent() {
        Intent choosePictureIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (choosePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(choosePictureIntent, REQUEST_IMAGE_PICK);
        }
    }

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;


// ...

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            // Permission has already been granted
            launchTakePictureIntent();
        }
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        } else {
            // Permission has already been granted
            launchChoosePictureIntent();
        }
    }

    private void dispatchChoosePictureIntent() {
        checkStoragePermission();
    }
    private void dispatchTakePictureIntent() {
        checkCameraPermission();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
                // If request is cancelled, the result arrays are empty
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted, proceed with the camera operation
                    launchTakePictureIntent();
                } else {
                    // Permission denied, handle accordingly (show a message, etc.)
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case REQUEST_STORAGE_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted, proceed with the storage operation
                    launchChoosePictureIntent();
                } else {
                    // Permission denied, handle accordingly
                    Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            // Add more cases if you have multiple permissions to handle
        }
    }



}
