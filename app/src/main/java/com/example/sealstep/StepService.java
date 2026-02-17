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

import java.util.Calendar;

public class StepService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepCounter;
    private int awardstep = 0;
    public boolean fishAward = false;
    private float initialSteps = -1;
    public static final String STEP_BROADCAST = "STEP_UPDATE";
    public static final String STEP_COUNT = "step_count";

    @Override
    public void onCreate() {
        super.onCreate();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepCounter != null) {
            sensorManager.registerListener(this, stepCounter,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        Log.d("STEP_SERVICE", "Service created");
        startForegroundService();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d("STEP_SERVICE", "Sensor event fired");
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) return;
        int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (time > 6 || time < 22){
            float total = event.values[0];
            if (initialSteps < 0) {
                initialSteps = total;
            }
            int currentSteps = (int) (total - initialSteps);
            awardstep++;
            if (awardstep == 1500){
                fishAward = true;
                awardstep = 0;
            }
            else{
                fishAward = false;
            }
            Log.d("STEP_SERVICE", "Raw value: " + event.values[0]);
            Intent intent = new Intent(STEP_BROADCAST);
            intent.putExtra(STEP_COUNT, currentSteps);
            sendBroadcast(intent);
        }
    }

    public boolean isFishAward() {
        return fishAward;
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