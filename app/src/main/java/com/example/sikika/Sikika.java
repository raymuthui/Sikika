package com.example.sikika;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.FirebaseApp;

public class Sikika extends Application {
    private FirebaseAnalytics mFirebaseAnalytics;
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }
}

