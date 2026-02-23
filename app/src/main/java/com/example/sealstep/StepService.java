package com.example.sealstep;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.*;
import android.os.*;
import android.util.Log;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.app.Service;
import android.widget.Toast;

import java.util.Calendar;

public class StepService extends Service implements SensorEventListener {
    SharedPreferences prefs;
    WeeklySteps weekly = new WeeklySteps();
    private SensorManager sensorManager;
    private Sensor stepCounter;
    private int awardstep = 0;
    private int fishcount;
    private int lastRewardSteps = 0;
    private int lastHungerStep = 0;
    private float hunger = 0;
    private int offendedDays = 0;
    private int goal = 0;
    public static final String STEP_BROADCAST = "STEP_UPDATE";
    public static final String STEP_COUNT = "step_count";
    public static final String FISH_NOTIF = "fish_Notif";

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        prefs  = getSharedPreferences("App_Pref", MODE_PRIVATE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        fishcount = prefs.getInt("fishcount", 0);
        if (stepCounter != null) {
            sensorManager.registerListener(this, stepCounter,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        Log.d("STEP_SERVICE", "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("STEP_SERVICE", "onStartCommand");

        if (sensorManager == null) {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        }

        if (stepCounter == null) {
            stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }

        if (stepCounter != null) {
            sensorManager.unregisterListener(this); // prevent duplicates
            sensorManager.registerListener(this, stepCounter,
                    SensorManager.SENSOR_DELAY_FASTEST);

            Log.d("STEP_SERVICE", "Sensor registered");
        }
        prefs  = getSharedPreferences("App_Pref", MODE_PRIVATE);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    private int getCustomDayId() {
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.HOUR_OF_DAY) < 6) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR);
    }

    private void weeklyReset(){
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        boolean reset = prefs.getBoolean("reset", false);
        if(day == 3){
            if (reset){
                SaveDay(1, -1);
                for (int i = 3; i < 8; i++) {
                    SaveDay(i, -1);
                }
                reset = false;
            }
        }
        prefs.edit().putBoolean("reset", reset).apply();
    }

    private void SaveDay(int day, int steps) {
        SharedPreferences.Editor e = prefs.edit();
        switch (day){
            case 1: //Su
                weekly.setSunday(steps);
                e.putInt("sun", weekly.getSunday());
                SendTunaRatingNotif();
                break;
            case 2: //M
                weekly.setMonday(steps);
                e.putInt("mon", weekly.getMonday());
                e.putBoolean("reset", true);
                break;
            case 3: //Tu
                weekly.setTuesday(steps);
                e.putInt("tue", weekly.getTuesday());
                break;
            case 4: //w
                weekly.setWendesday(steps);
                e.putInt("wen", weekly.getWendesday());
                break;
            case 5: //Th
                weekly.setThursday(steps);
                e.putInt("thu", weekly.getThursday());
                break;
            case 6: //F
                weekly.setFriday(steps);
                e.putInt("fri", steps);
                break;
            case 7: //Sa
                weekly.setSaturday(steps);
                e.putInt("sat", weekly.getSaturday());
                break;
            default:
                Toast.makeText(this, getString(R.string.errorInLang), Toast.LENGTH_SHORT).show();
                break;
        }
        e.apply();
    }

    private void SendTunaRatingNotif() {
        Notification weekly = new NotificationCompat.Builder(this, "weekly_channel")
                .setContentTitle(getString(R.string.weeklySmall))
                .setContentText(getString(R.string.weeklyBig))
                .setSmallIcon(R.drawable.notif_calendar)
                .build();
        NotificationManager manager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel("TUNA_NOTIF",
                            "Tuna get",
                            NotificationManager.IMPORTANCE_DEFAULT);
            manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
            manager.notify(2002, weekly);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) return;
        int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (time >= 6 && time <= 22) {
            goal = prefs.getInt("goal", 5000);
            int savedRewardCount = prefs.getInt("rewardCount", 0);
            int hungerCount = prefs.getInt("hungercount", 0);
            hunger = prefs.getFloat("hunger", 0);
            lastHungerStep = prefs.getInt("lastHunger", 0);
            lastRewardSteps = prefs.getInt("lastRewardSteps", 0);
            int currentDay = getCustomDayId();
            int savedDay = prefs.getInt("resetDay", -1);
            int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
            int rawSteps = (int) event.values[0];
            int baseSteps = prefs.getInt("base_steps", -1);
            Log.d("STEP_DEBUG", "Base: " + baseSteps);
            // Reset at 6AM
            if (savedDay == currentDay) {
                Log.d("day" ,"DAYCHANGE");
                SaveDay(day, rawSteps);
                awardstep = 0;
                prefs.edit().putInt("resetDay", -1).apply();
                prefs.edit().putInt("base_steps", rawSteps).apply();
                prefs.edit().putInt("lastRewardSteps", lastRewardSteps).apply();
                prefs.edit().putInt("rewardCount", 0).apply();
                baseSteps = rawSteps;
                weeklyReset();
                if ((rawSteps - baseSteps) == 0){
                    offendedDays++;
                    if (offendedDays > 3){
                        OffendedNotif();
                    }
                }
            }
            if (baseSteps == -1) {
                Log.d("a", "INITIAL SET");
                baseSteps = rawSteps;
                prefs.edit().putInt("base_steps", baseSteps).apply();
            }
            if (baseSteps < 0) {
                Log.d("a", "INITIAL SET");
                baseSteps = rawSteps;
                prefs.edit().putInt("base_steps", baseSteps).apply();
            }
            int dailySteps = rawSteps - baseSteps;
            if (dailySteps <= 0) dailySteps = 0;
            int rewardCount = dailySteps / 1500;
            int hungercount = dailySteps / 3000;
            //getfish
            if (rewardCount > savedRewardCount) {
                Log.d("fish", "FISH SENT");
                fishcount++;
                lastRewardSteps += 1500;
                prefs.edit().putInt("fishcount", fishcount);
                prefs.edit().putInt("lastRewardSteps", lastRewardSteps);
                prefs.edit().putInt("rewardCount", rewardCount);
                prefs.edit().apply();
                Notification fish = new NotificationCompat.Builder(this, "fish_channel")
                        .setContentTitle(getString(R.string.getfishSmall))
                        .setContentText(getString(R.string.getfishMain, 1))
                        .setSmallIcon(R.drawable.notif_fish)
                        .build();
                NotificationManager manager;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel =
                            new NotificationChannel(FISH_NOTIF,
                                    "Fish get",
                                    NotificationManager.IMPORTANCE_LOW);

                    manager =
                            getSystemService(NotificationManager.class);
                    manager.createNotificationChannel(channel);
                    manager.notify(2000, fish);
                }
            }
            //minus hunger
            if (hungercount > hungerCount){
                Log.d("h", "HUNGER--");
                hungercount++;
                lastHungerStep += 3000;
                hunger -= 0.5;
                prefs.edit().putFloat("hunger", hunger);
                prefs.edit().putInt("lastHunger", lastHungerStep);
                prefs.edit().putInt("hungercount", hungercount);
                prefs.edit().apply();
            }
            //send goal hit
            if (goal == dailySteps){
                Log.d("goal", "GOAL HIT");
                SendGoal(dailySteps);
            }
            prefs.edit().putInt("base_steps", baseSteps).apply();
            // Send to activity
            Log.d("STEP_DEBUG", "Raw: " + rawSteps);
            Log.d("STEP_DEBUG", "Base: " + baseSteps);
            Log.d("STEP_DEBUG", "Daily: " + dailySteps);
            Log.d("current steps", String.valueOf(dailySteps));
            Intent intent = new Intent(STEP_BROADCAST);
            intent.setPackage(getPackageName());
            intent.putExtra(STEP_COUNT, dailySteps);
            Log.d("STEP_DEBUG", "Sensor Triggered");
            sendBroadcast(intent);
        }
    }

    private void SendGoal(int steps) {
        Notification goalHit = new NotificationCompat.Builder(this, "goal_channel")
                .setContentTitle(getString(R.string.goalHitSmall))
                .setContentText(getString(R.string.goalHitMain, steps))
                .setSmallIcon(R.drawable.notif_ribbon)
                .build();
        NotificationManager manager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel("GOAL_NOTIF",
                            "Goal hit",
                            NotificationManager.IMPORTANCE_DEFAULT);
            manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
            manager.notify(2005, goalHit);
        }
    }

    private void OffendedNotif() {
        Notification offended = new NotificationCompat.Builder(this, "steps_channel")
                .setContentTitle(getString(R.string.offendedSmall))
                .setContentText(getString(R.string.offendedMain))
                .setSmallIcon(R.drawable.notif_fish)
                .build();
        NotificationManager manager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel("OFFENDED_NOTIF",
                            "offended",
                            NotificationManager.IMPORTANCE_DEFAULT);
            manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
            manager.notify(2003, offended);
        }
        offendedDays = 0;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startForegroundService() {
        String channelId = "step_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(channelId,
                            "Step Counter",
                            NotificationManager.IMPORTANCE_LOW);

            NotificationManager manager =
                    getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        Notification notification =
                new NotificationCompat.Builder(this, channelId)
                        .setContentTitle(getString(R.string.walikingStratSmall))
                        .setContentText(getString(R.string.walkingStratMain))
                        .setSmallIcon(R.drawable.notif_fish)
                        .build();

        startForeground(1, notification);
    }
}