package com.example.sealstep;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.media3.common.Player;

public class LoadingActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private ExoPlayer player;
    private PlayerView playerView;
    MediaPlayer sound;
    Current c = new Current();
    int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    private boolean alreadyOpened = false;

    double latitude;
    double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        init();
        Log.d("LOADING", "LoadingActivity created");
        setContentView(R.layout.activity_loading);
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

    private void openMain() {

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
        if (player != null){
            player.release();
            player = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
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

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
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

    @OptIn(markerClass = UnstableApi.class)
    private void init(){
        //video
        playerView = findViewById(R.id.loading);
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.loading);
        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        player.prepare();
        player.play();
        loadData();
        //sound
        sound = MediaPlayer.create(this, R.raw.kk_bashment);
        sound.setLooping(true);
        sound.start();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                Log.d("EXO", "State: " + state);
            }
        });
    }
}