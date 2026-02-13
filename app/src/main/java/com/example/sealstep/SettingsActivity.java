package com.example.sealstep;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> settingsLauncher;
    private MediaPlayer player;
    FrameLayout back;
    FrameLayout sound;
    ImageView soundPic;
    SealVariables sealVariables = new SealVariables();
    ImageView github;
    ImageView youtube;
    //and add the dropdown

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        init();
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                //extra data
                intent.putExtra("sound", sealVariables.isSound());
                //intent.putExtra("lang", sealVariables.getLang());
                setResult(RESULT_OK, intent);
                player.pause();
                finish();
            }
        });
        settingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        sealVariables.setSound(data.getBooleanExtra("sound", true));
                        //sealvar.setLang(data.getStringExtra("lang"));
                        if (player != null){
                            if (sealVariables.isSound()){
                                player.start();
                            }
                            else{
                                player.pause();
                            }
                        }
                    }
                }
        );
        sound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sealVariables.isSound()){
                    sealVariables.setSound(false);
                    soundPic.setImageResource(R.drawable.mute);
                    player.pause();
                }
                else{
                    sealVariables.setSound(true);
                    soundPic.setImageResource(R.drawable.sound);
                    player.start();
                }
            }
        });
        youtube.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://youtu.be/h9uFQv3t1AU?si=VoLlCmzm7-cfH2A0"));
                startActivity(i);
            }
        });
        github.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://github.com/EFanni05/SealSTEP"));
                startActivity(i);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null){
            player.release();
            player = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (player != null && sealVariables.isSound()) {
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
    private void init(){
        back = findViewById(R.id.backbutton);
        sound = findViewById(R.id.soundbutton);
        soundPic = findViewById(R.id.soundpic);
        github = findViewById(R.id.github);
        youtube = findViewById(R.id.youtube);
        //dropdown
        player = MediaPlayer.create(this, R.raw.kk_bashment);
        player.setLooping(true);
        player.start();
    }
}