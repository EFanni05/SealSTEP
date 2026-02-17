package com.example.sealstep;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StatActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    SealVariables sealvar = new SealVariables();
    MediaPlayer player;
    FrameLayout back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("App_Pref", MODE_PRIVATE);
        applySavedLanguage();
        sealvar.setSound(prefs.getBoolean("sound", true));
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_stat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        init();
        if (sealvar.isSound()){
            player.start();
        }
        else{
            player.pause();
        }
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(StatActivity.this, MainActivity.class);
                startActivity(i);
            }
        });
    }

    private void applySavedLanguage() {

        SharedPreferences prefs =
                getSharedPreferences("Settings", MODE_PRIVATE);

        String languageCode =
                prefs.getString("app_language", "en");

        LocaleListCompat appLocale =
                LocaleListCompat.forLanguageTags(languageCode);

        AppCompatDelegate.setApplicationLocales(appLocale);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null && sealvar.isSound()) {
            player.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null){
            player.release();
            player = null;
        }
    }

    private void init(){
        player = MediaPlayer.create(this, R.raw.kk_bashment);
        player.setLooping(true);
        player.start();
        back = findViewById(R.id.back);
    }
}