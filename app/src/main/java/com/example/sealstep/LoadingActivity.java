package com.example.sealstep;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.icu.util.Calendar;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.util.UnstableApi;

public class LoadingActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;

    ImageView gif;
    MediaPlayer sound;
    Current c = new Current();
    int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    private boolean alreadyOpened = false;

    double latitude;
    double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_loading);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Log.d("LOADING", "LoadingActivity created");
        init();
        //gif
        Glide.with(this)
                .asGif()
                .load(R.drawable.loading)
                .into(new CustomTarget<GifDrawable>() {
                    @Override
                    public void onResourceReady(GifDrawable resource,
                                                Transition<? super GifDrawable> transition) {

                        resource.setLoopCount(GifDrawable.LOOP_FOREVER);
                        resource.start();
                        gif.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {}
                });
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        } else {
            getLocation();
        }
        API api = new API();
        api.fetchWeather(latitude, longitude, new API.WeatherCallback() {
            @Override
            public void onSuccess(Weather weather) {
                //move to main
                new android.os.Handler().postDelayed(() -> {
                    openMain();
                }, 3000);
            }

            @Override
            public void onError(String error) {
                    c.setWeather_code(100);
                    if (time <= 18 || time < 6){
                        c.setIs_day(1);
                    }
                    else {
                        c.setIs_day(0);
                    }
                new android.os.Handler().postDelayed(() -> {
                    openMain();
                }, 3000);
            }
        });
    }

    private boolean GeoPerm() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
            return false;
        }
        return true;

    }

    private boolean StepPerm() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    2001);
            return false;
        }
        return true;
    }

    private boolean Notifperm() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    3001);
            return false;
        }
        return true;
    }

    private void openMain() {
        boolean allPremsGot = false;
        //loop until get the perms!!!
        while (allPremsGot == false){
            boolean geo = GeoPerm();
            boolean step = StepPerm();
            boolean notif = Notifperm();
            if (geo && step && notif){
                allPremsGot = true;
            }
        }
        //goin to main
        if (alreadyOpened) return;
        alreadyOpened = true;
        sound.stop();
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sound != null){
            sound.release();
            sound = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                getLocation();
            }
        }
    }

    private void loadData() {
        new android.os.Handler().postDelayed(() -> {
            openMain();
        }, 4000);
    }

    private void getLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            latitude = 52.52;
            longitude = 13.41;
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latitude = 52.52;
                        longitude = 13.41;
                    }
                });
    }

    private void init(){
        //gif
        gif = findViewById(R.id.loading);
        Glide.with(this)
                .asGif()
                .load(R.drawable.loading)
                .into(new CustomTarget<GifDrawable>() {
                    @Override
                    public void onResourceReady(GifDrawable resource, Transition<? super GifDrawable> transition) {
                        resource.setLoopCount(GifDrawable.LOOP_FOREVER);
                        gif.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {}
                });
        loadData();
        //sound
        sound = MediaPlayer.create(this, R.raw.kk_bashment);
        sound.setLooping(true);
        sound.start();
    }
}