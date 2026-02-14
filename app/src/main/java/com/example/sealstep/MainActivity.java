package com.example.sealstep;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.sealstep.WeatherAPI;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.Calendar;

import retrofit2.Call;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private static final String URL  = "https://api.open-meteo.com/v1/";
    private ActivityResultLauncher<Intent> settingsLauncher;
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    private MediaPlayer player;
    private SoundPool sealsound;
    int sealSoundID;
    ImageView background;
    ImageView backgroundgif;
    //hungerbar
    ImageView hunger1;
    ImageView hunger2;
    ImageView hunger3;
    ImageView hunger4;
    TextView step;
    FrameLayout settings;
    ImageView seal;
    FrameLayout feeding;
    FrameLayout fishbutton;

    //regular variables
    SealVariables sealvar = new SealVariables();
    Weather weather = new Weather();
    int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    private Handler handler = new Handler();
    private Runnable weatherRunnable;
    private API api;

    double latitude;
    double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        init();
        Log.d("MAIN", "MainActivity created");
        //seal sleep
        boolean sleep = SleepCheck();
        //gifs
        Glide.with(this)
                .asGif()
                .load(R.drawable.snow)
                .into(new CustomTarget<GifDrawable>() {
                    @Override
                    public void onResourceReady(GifDrawable resource,
                                                Transition<? super GifDrawable> transition) {

                        resource.setLoopCount(GifDrawable.LOOP_FOREVER);
                        resource.start();
                        backgroundgif.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {}
                });
        //geo
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
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                i.putExtra("sound" , sealvar.isSound());
                player.pause();
                settingsLauncher.launch(i);
            }
        });
        settingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        sealvar.setSound(data.getBooleanExtra("sound", true));
                        //sealvar.setLang(data.getStringExtra("lang"));
                        if (player != null){
                            if (sealvar.isSound()){
                                player.start();
                            }
                            else{
                                player.pause();
                            }
                        }
                    }
                }
        );
        fishbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sealvar.getFishcount() > 0){
                    String msg = getString(R.string.haveFish, sealvar.getFishcount());
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(MainActivity.this, R.string.nofish, Toast.LENGTH_SHORT).show();
                }
            }
        });
        feeding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sleep){
                    Toast.makeText(MainActivity.this, R.string.sleeping, Toast.LENGTH_SHORT).show();
                }
                else{
                    if (sealvar.getFishcount() == 0){
                        Toast.makeText(MainActivity.this, R.string.nofish, Toast.LENGTH_SHORT).show();
                    }
                    else{
                        //feeding
                    }
                }
            }
        });
        seal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sealsound.play(sealSoundID, 1f, 1f, 1, 0, 1f);
            }
        });
        //Weather update!!
        api = new API();
        weatherRunnable = new Runnable() {
            @Override
            public void run() {
                api.fetchWeather(latitude, longitude,
                        new API.WeatherCallback() {

                            @Override
                            public void onSuccess(Weather weather) {
                                ChangeBG();
                            }

                            @Override
                            public void onError(String error) {
                                trycount++;
                                if (trycount < 2){
                                    TimeCheck();
                                }
                                else{
                                    trycount = 0;
                                    weatherCode = 100; //clear
                                    if (time <= 18 || time < 6){
                                        isDay = 1;
                                    }
                                    else {
                                        isDay = 0;
                                    }
                                }
                            }
                        });

                handler.postDelayed(this, 30 * 60 * 1000); // 30 minutes
            }
        };

        handler.post(weatherRunnable);
    }

    int trycount = 0;
    int isDay;
    int weatherCode;
    int gifNeed = 0;
    private void TimeCheck() {
        //get these from gps
        double lat = 52.52;
        double lon = 13.41;
        WeatherAPI weatherAPI = retrofit.create(WeatherAPI.class);
        Call<Weather> call = weatherAPI.getForecast(lat, lon);
        call.enqueue(new Callback<Weather>() {
            @Override
            public void onResponse(Call<Weather> call, Response<Weather> response) {
                trycount++;
                if (response.isSuccessful() && response.body() != null) {
                    Weather weather = response.body();
                    isDay = weather.getCurrent().getIs_day();
                    weatherCode = weather.getCurrent().getWeather_code();
                    trycount = 0;
                    ChangeBG();
                }
            }
            @Override
            public void onFailure(Call<Weather> call, Throwable t) {
                trycount++;
                if (trycount < 2){
                    TimeCheck();
                }
                else{
                    trycount = 0;
                    weatherCode = 100; //clear
                    if (time <= 18 || time < 6){
                        isDay = 1;
                    }
                    else {
                        isDay = 0;
                    }
                }
            }
        });
    }

    private void ChangeBG() {
        if (isDay == 0){ //day
            if (weatherCode == 0) {
                background.setImageResource(R.drawable.sunny);
                backgroundgif.setVisibility(View.INVISIBLE);
                gifNeed = 0;
            }
            else if (weatherCode >= 1 && weatherCode <= 3) {
                background.setImageResource(R.drawable.cloudysun);
                backgroundgif.setVisibility(View.INVISIBLE);
                gifNeed = 0;
            }
            else if ((weatherCode >= 51 && weatherCode <= 65) ||
                    (weatherCode >= 80 && weatherCode <= 82)) {
                background.setImageResource(R.drawable.rainday);
                backgroundgif.setImageResource(R.drawable.rain);
                backgroundgif.setVisibility(View.VISIBLE);
                gifNeed = 1;
                Glide.with(MainActivity.this)
                        .asGif()
                        .load(R.drawable.rain)
                        .into(backgroundgif);
            }
            else if ((weatherCode >= 71 && weatherCode <= 75) ||
                    (weatherCode >= 85 && weatherCode <= 86)) {
                background.setImageResource(R.drawable.nowday);
                backgroundgif.setImageResource(R.drawable.snow);
                backgroundgif.setVisibility(View.VISIBLE);
                gifNeed = 2;
                Glide.with(MainActivity.this)
                        .asGif()
                        .load(R.drawable.snow)
                        .into(backgroundgif);
            }
            else{
                background.setImageResource(R.drawable.nighrclear);
                backgroundgif.setVisibility(View.INVISIBLE);
                gifNeed = 0;
            }
        }
        else{ //night
            if (weatherCode == 1) {
                background.setImageResource(R.drawable.nighrclear);
                backgroundgif.setVisibility(View.INVISIBLE);
                gifNeed = 0;
            }
            else if (weatherCode >= 1 && weatherCode <= 3) {
                background.setImageResource(R.drawable.cloudnight);
                backgroundgif.setVisibility(View.INVISIBLE);
                gifNeed = 0;
            }
            else if ((weatherCode >= 51 && weatherCode <= 65) ||
                    (weatherCode >= 80 && weatherCode <= 82)) {
                background.setImageResource(R.drawable.rainnight);
                backgroundgif.setImageResource(R.drawable.rain);
                backgroundgif.setVisibility(View.VISIBLE);
                gifNeed = 1;
                Glide.with(MainActivity.this)
                        .asGif()
                        .load(R.drawable.rain)
                        .into(backgroundgif);
            }
            else if ((weatherCode >= 71 && weatherCode <= 75) ||
                    (weatherCode >= 85 && weatherCode <= 86)) {
                background.setImageResource(R.drawable.cloudnight);
                backgroundgif.setImageResource(R.drawable.snow);
                backgroundgif.setVisibility(View.VISIBLE);
                gifNeed = 2;
                Glide.with(MainActivity.this)
                        .asGif()
                        .load(R.drawable.snow)
                        .into(backgroundgif);
            }
            else{
                background.setImageResource(R.drawable.nighrclear);
                backgroundgif.setVisibility(View.VISIBLE);
                gifNeed = 0;
            }
        }
    }

    private boolean SleepCheck() {
        if (time >= 20 || time < 6){
            seal.setImageResource(R.drawable.sleep);
            return true;
        }
        return false;
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

                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
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
        handler.removeCallbacks(weatherRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (player != null && sealvar.isSound()) {
            player.start();
        }
        if (gifNeed == 1){
            Glide.with(this)
                    .asGif()
                    .load(R.drawable.rain)
                    .into(backgroundgif);
        }
        if (gifNeed == 2){
            Glide.with(this)
                    .asGif()
                    .load(R.drawable.snow)
                    .into(backgroundgif);
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

    private void init(){
        background = findViewById(R.id.background);
        backgroundgif = findViewById(R.id.backgroundgif);
        Glide.with(this)
                .asGif()
                .load(R.drawable.snow)
                .into(new CustomTarget<GifDrawable>() {
                    @Override
                    public void onResourceReady(GifDrawable resource, Transition<? super GifDrawable> transition) {
                        resource.setLoopCount(GifDrawable.LOOP_FOREVER);
                        backgroundgif.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {}
                });
        hunger1 = findViewById(R.id.hunger1);
        hunger2 = findViewById(R.id.hunger2);
        hunger3 = findViewById(R.id.hunger3);
        hunger4 = findViewById(R.id.hunger4);
        step = findViewById(R.id.stepcounttext);
        settings = findViewById(R.id.setting);
        seal = findViewById(R.id.seal);
        feeding = findViewById(R.id.feedingbutton);
        fishbutton = findViewById(R.id.fishbutton);
        if(time >= 22 || time < 6){
            player = MediaPlayer.create(this, R.raw.stalecupcake);
        }
        else{
            player = MediaPlayer.create(this, R.raw.kk_soul);
        }
        sealsound = new SoundPool.Builder().setMaxStreams(2).build();
        sealSoundID = sealsound.load(this, R.raw.clapping_seal, 1);
        player.setLooping(true);
        player.start();
        //read base data;
    }
}