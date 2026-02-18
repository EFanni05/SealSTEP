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
    public static final String STEP_BROADCAST = "STEP_UPDATE";
    public static final String STEP_COUNT = "step_count";

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

    private void SaveDay(int day, int steps) {
        SharedPreferences.Editor e = prefs.edit();
        switch (day){
            case 1: //Su
                weekly.setSunday(steps);
                e.putInt("sun", weekly.getSunday());
                break;
            case 2: //M
                weekly.setMonday(steps);
                e.putInt("mon", weekly.getMonday());
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
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) return;
        int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (time >= 6 && time <= 22) {
            int savedRewardCount = prefs.getInt("rewardCount", 0);
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
            if (rewardCount > savedRewardCount) {
                fishcount++;
                lastRewardSteps += 1500;
                prefs.edit().putInt("fishcount", fishcount);
                prefs.edit().putInt("lastRewardSteps", lastRewardSteps);
                prefs.edit().putInt("rewardCount", rewardCount);
                prefs.edit().apply();
            }
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