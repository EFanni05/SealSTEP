package com.example.sealstep;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
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

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    private SharedPreferences prefsMain;
    //sensors
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private BroadcastReceiver stepReceiver;
    private static final int STEP_PERMISSION_CODE = 2001;
    //location
    private static final int LOCATION_PERMISSION_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    //weather API
    private static final String URL  = "https://api.open-meteo.com/v1/";
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    //sound
    private MediaPlayer player;
    private SoundPool sealsound;
    private SoundPool sealsoundEater;
    int sealSoundID;
    int sealEatID;
    //xml elements
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
    FrameLayout statbutton;
    //regular variables
    SealVariables sealvar = new SealVariables();
    //weather
    Weather weather = new Weather();
    //Time + date
    int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
    private String getTodayDate() {
        return new java.text.SimpleDateFormat("yyyyMMdd",
                java.util.Locale.getDefault()).format(new java.util.Date());
    }
    //weather again
    private Handler handler = new Handler();
    private Runnable weatherRunnable;
    private API api;
    //step count
    private float totalSteps = 0f;
    private int base;
    public static final String STEP_COUNT = "step_count";
    StepService stepService = new StepService();
    //gps
    double latitude;
    double longitude;
    //notifs
    NotificationManager notif;
    Notification notification;
    private static final int SLEEP_NOTIFICATION_ID = 100;
    private static final int DAILY_RECAP_NOTIFICATION_ID = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefsMain  = getSharedPreferences("App_Pref", MODE_PRIVATE);
        applySavedLanguage();
        sealvar.setSound(prefsMain.getBoolean("sound", true));
        sealvar.setFishcount(prefsMain.getInt("fishcount", 1));
        sealvar.setHunger((double) (prefsMain.getFloat("hunger", 4)));
        Log.d("feeding", "Huger stat" + String.valueOf(sealvar.getHunger()));
        base = prefsMain.getInt("base_steps", -1);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        init();
        //for img
        feedSet();
        Log.d("MAIN", "MainActivity created");
        if (sealvar.isSound()){
            player.start();
        }
        else{
            player.pause();
        }
        //seal sleep
        boolean sleep = SleepCheck();
        Log.d("SLEEP_CHECK", String.valueOf(sleep));
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
        //notifs
        if (Build.VERSION.SDK_INT >= 33) {
            Notifperm();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Notifperm()) {
                NotificationChannel channel =
                        new NotificationChannel(
                                "main_channel",
                                "Main Notifications",
                                NotificationManager.IMPORTANCE_DEFAULT
                        );

                NotificationManager manager =
                        getSystemService(NotificationManager.class);

                manager.createNotificationChannel(channel);
            }
        }
        //step
        if (StepPerm()){
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            /**sensorManager.registerListener(
                    this,
                    stepSensor,
                    SensorManager.SENSOR_DELAY_FASTEST
            );**/
            Intent intent = new Intent(this, StepService.class);
            ContextCompat.startForegroundService(this, intent);
            stepReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int steps = intent.getIntExtra(STEP_COUNT, 0);
                    Log.d("STEP_DEBUG", "Broadcast sent: " + steps);
                    step.setText(String.valueOf(steps));
                }
            };
            Log.d("STEP_SERVICE", "Total: " + step);
            sealvar.StepBaseHunger();
        }
        //geo
        if (GeoPerm()){
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            getLocation();
        }
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                player.pause();
                startActivity(i);
            }
        });
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
                        int res = FeedSeal();
                        Log.d("feeding stat", String.valueOf(res));
                        switch (res){
                            case 0:
                                Toast.makeText(MainActivity.this, getString(R.string.fullHunger), Toast.LENGTH_SHORT).show();
                                break;
                            case 1:
                                Log.d("feeding", "1");
                                seal.setImageResource(R.drawable.eat);
                                sealsoundEater.play(sealEatID, 1f, 1f, 1, 0, 1f);
                                new android.os.Handler().postDelayed(() -> {
                                    seal.setImageResource(R.drawable.seal);
                                }, 2000);
                                feedSet();
                                break;
                            default:
                                break;
                        }
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
        statbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, StatActivity.class);
                //extra data if needed
                startActivity(i);
            }
        });
        //Weather update!!
        if (GeoPerm()) {
            api = new API();
            weatherRunnable = new Runnable() {
                @Override
                public void run() {
                    api.fetchWeather(latitude, longitude,
                            new API.WeatherCallback() {
                                @Override
                                public void onSuccess(Weather weather) {
                                    if (weather != null && weather.getCurrent() != null) {
                                        isDay = weather.getCurrent().getIs_day();
                                        weatherCode = weather.getCurrent().getWeather_code();
                                    }
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
        }
    }

    private void feedSet() {
        //img setting
        int h = (int)(sealvar.getHunger() / 0.5);
        Log.d("h", "H VALUE " + String.valueOf(h % 2));
        if (h % 2 == 0 && sealvar.getHunger() <= 8){
            //full
            switch (h){
                case 2:
                    hunger1.setImageResource(R.drawable.fish);
                    hunger2.setImageResource(R.drawable.emptyfish);
                    hunger3.setImageResource(R.drawable.emptyfish);
                    hunger4.setImageResource(R.drawable.emptyfish);
                    break;
                case 4:
                    hunger1.setImageResource(R.drawable.fish);
                    hunger2.setImageResource(R.drawable.fish);
                    hunger3.setImageResource(R.drawable.emptyfish);
                    hunger4.setImageResource(R.drawable.emptyfish);
                    break;
                case 6:
                    hunger1.setImageResource(R.drawable.fish);
                    hunger2.setImageResource(R.drawable.fish);
                    hunger3.setImageResource(R.drawable.fish);
                    hunger4.setImageResource(R.drawable.emptyfish);
                    break;
                case 8:
                    hunger1.setImageResource(R.drawable.fish);
                    hunger2.setImageResource(R.drawable.fish);
                    hunger3.setImageResource(R.drawable.fish);
                    hunger4.setImageResource(R.drawable.fish);
                    break;
            }
        }
        else{
            switch (h){
                case 1:
                    hunger1.setImageResource(R.drawable.half);
                    hunger2.setImageResource(R.drawable.emptyfish);
                    hunger3.setImageResource(R.drawable.emptyfish);
                    hunger4.setImageResource(R.drawable.emptyfish);
                    break;
                case 3:
                    hunger1.setImageResource(R.drawable.fish);
                    hunger2.setImageResource(R.drawable.half);
                    hunger3.setImageResource(R.drawable.emptyfish);
                    hunger4.setImageResource(R.drawable.emptyfish);
                    break;
                case 5:
                    hunger1.setImageResource(R.drawable.fish);
                    hunger2.setImageResource(R.drawable.fish);
                    hunger3.setImageResource(R.drawable.half);
                    hunger4.setImageResource(R.drawable.emptyfish);
                    break;
                case 7:
                    hunger1.setImageResource(R.drawable.fish);
                    hunger2.setImageResource(R.drawable.fish);
                    hunger3.setImageResource(R.drawable.fish);
                    hunger4.setImageResource(R.drawable.half);
                    break;
            }
        }
        if (h == 0){
            hunger1.setImageResource(R.drawable.emptyfish);
            hunger2.setImageResource(R.drawable.emptyfish);
            hunger3.setImageResource(R.drawable.emptyfish);
            hunger4.setImageResource(R.drawable.emptyfish);
        }
        Log.d("hunger", "NEW HUNGER " + String.valueOf(sealvar.getHunger()));

    }

    private int FeedSeal() {
        return sealvar.setHunger();
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
                    STEP_PERMISSION_CODE);
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

    int trycount = 0;
    int isDay;
    int weatherCode;
    int gifNeed = 0;
    private void TimeCheck() {
        WeatherAPI weatherAPI = retrofit.create(WeatherAPI.class);
        Call<Weather> call = weatherAPI.getForecast(latitude, longitude);
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
        if (isDay == 1){ //day
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
            if (weatherCode == 0) {
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
            notification =
                    new NotificationCompat.Builder(this, "main_channel")
                            .setContentTitle(getString(R.string.sleepSmall))
                            .setContentText(getString(R.string.sleepMain))
                            .setSmallIcon(R.drawable.notif_fish)
                            .build();
            notif.notify(SLEEP_NOTIFICATION_ID, notification);
            SetExtra();
            return true;
        }
        notif.cancel(SLEEP_NOTIFICATION_ID);
        return false;
    }

    private void SetExtra(){
        SharedPreferences.Editor e = prefsMain.edit();
        e.putFloat("hunger", (float) sealvar.getHunger());
        e.putInt("fishcount", sealvar.getFishcount());
        e.putInt("base_steps", -1);
        e.apply();
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
                        handler.post(weatherRunnable);
                    }
                });
    }

    private BroadcastReceiver stepReceiverPause = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int steps = intent.getIntExtra(StepService.STEP_COUNT, 0);
            Log.d("STEP_DEBUG", "Broadcast sent: " + steps);
            step.setText(String.valueOf(steps));
        }
    };

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
        //when coming back from different activities
        if (player != null && sealvar.isSound()) {
            player.start();
        }
        if (gifNeed == 1){
            backgroundgif.setVisibility(backgroundgif.VISIBLE);
            Glide.with(this)
                    .asGif()
                    .load(R.drawable.rain)
                    .into(backgroundgif);
        }
        if (gifNeed == 2){
            backgroundgif.setVisibility(backgroundgif.VISIBLE);
            Glide.with(this)
                    .asGif()
                    .load(R.drawable.snow)
                    .into(backgroundgif);
        }
        if (stepSensor != null) {
            Log.d("step", "STEP REREGISTERD");
            sensorManager.registerListener(this, stepSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        IntentFilter filter = new IntentFilter(StepService.STEP_BROADCAST);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("step", "STEP REREGISTERD");
            registerReceiver(stepReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //when moving to different activites
        if (player != null && player.isPlaying()) {
            player.pause();
        }
        sensorManager.unregisterListener(this);
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
        if (requestCode == 3001) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Now safe to start service
                Intent serviceIntent = new Intent(this, StepService.class);
                ContextCompat.startForegroundService(this, serviceIntent);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //no need
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //no need
    }



    private int getCustomDayId() {
        return 0;
    }

    private void init(){
        background = findViewById(R.id.background);
        backgroundgif = findViewById(R.id.backgroundgif);
        backgroundgif.setVisibility(backgroundgif.INVISIBLE);
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
        statbutton = findViewById(R.id.statbutton);
        if(time >= 22 || time < 6){
            player = MediaPlayer.create(this, R.raw.stalecupcake);
        }
        else{
            player = MediaPlayer.create(this, R.raw.kk_soul);
        }
        sealsound = new SoundPool.Builder().setMaxStreams(1).build();
        sealSoundID = sealsound.load(this, R.raw.clapping_seal, 2);
        sealsoundEater = new SoundPool.Builder().setMaxStreams(1).build();
        sealEatID = sealsoundEater.load(this, R.raw.nomnom, 1);
        player.setLooping(true);
        player.start();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        notif = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }
}