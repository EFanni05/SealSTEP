package com.example.sealstep;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StatActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    SealVariables sealvar = new SealVariables();
    WeeklySteps steps = new WeeklySteps();
    MediaPlayer player;
    FrameLayout back;
    FrameLayout goal;

    //steps
    TextView mondayStep;
    TextView tuesdayStep;
    TextView wendesdayStep;
    TextView thursdayStep;
    TextView firdayStep;
    TextView saturdayStep;
    TextView sundaySteps;

    //rating
    TextView mondayRating;
    TextView tuesdayRating;
    TextView wendesdayRating;
    TextView thursdayRating;
    TextView fridayRating;
    TextView saturdayRating;
    TextView sundayRating;

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
        getWeek();
        setWeek();
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
        goal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pre = prefs.getInt("goal", 5000);
                AlertDialog.Builder builder = new AlertDialog.Builder(StatActivity.this);
                //dialog button custom
                builder.setCancelable(true);
                LayoutInflater i = getLayoutInflater();
                View customAlert = i.inflate(R.layout.custom_alert, null);
                builder.setView(customAlert);
                AlertDialog a = builder.create();
                //for transparent background!!!
                if (a.getWindow() != null){
                    a.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }
                a.show();
                a.setCanceledOnTouchOutside(true);
                Button ok = customAlert.findViewById(R.id.okay);
                EditText goalEdit = customAlert.findViewById(R.id.AlertEdit);
                goalEdit.setHint(String.valueOf(pre));
                //for a guaranteed keyboard pop up
                if (goalEdit != null){
                    goalEdit.requestFocus();
                    a.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //to hide the keyboard
                        InputMethodManager input =
                                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                        if (input != null) {
                            input.hideSoftInputFromWindow(goalEdit.getWindowToken(), 0);
                        }
                        try {
                            String srt = String.valueOf(goalEdit.getText());
                            if (srt.isEmpty()){
                                //no text
                                throw new Exception(getString(R.string.NotFilled));
                            }
                            int goal = Integer.parseInt(srt);
                            if (goal <= 0) {
                                //incorrect number
                                throw new Exception(getString(R.string.IncorrectValue));
                            }
                            Log.d("goal", String.valueOf(goal) + "goal var");
                            steps.setGoal(goal);
                            prefs.edit().putInt("goal", goal).apply();
                            a.dismiss();
                            Toast.makeText(StatActivity.this, getString(R.string.set), Toast.LENGTH_SHORT).show();
                        } catch (NumberFormatException e){
                            //not number
                            Toast.makeText(StatActivity.this, getString(R.string.Notnumber), Toast.LENGTH_SHORT).show();
                        }catch (Exception e){
                            Toast.makeText(StatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void setWeek() {
        //setting textviews for stat;
        if (isItNull(steps.getMonday())){
            mondayStep.setText(getString(R.string.NoStep));
            mondayRating.setText("-%");
        }
        else{
            mondayStep.setText(String.valueOf(steps.getMonday()));
            mondayRating.setText(String.valueOf(steps.GetTuna(steps.getMonday())) + "%");
            GetWalrus(steps.GetTuna(steps.getMonday()));
        }
        if (isItNull(steps.getTuesday())){
            tuesdayStep.setText(getString(R.string.NoStep));
            tuesdayRating.setText("-%");
        }
        else{
            tuesdayStep.setText(String.valueOf(steps.getTuesday()));
            tuesdayRating.setText(String.valueOf(steps.GetTuna(steps.getTuesday())) + "%");
            GetWalrus(steps.GetTuna(steps.getTuesday()));
        }
        if (isItNull(steps.getWendesday())){
            wendesdayStep.setText(getString(R.string.NoStep));
            wendesdayRating.setText("-%");
        }
        else{
            wendesdayStep.setText(String.valueOf(steps.getWendesday()));
            wendesdayRating.setText(String.valueOf(steps.GetTuna(steps.getWendesday())) + "%");
            GetWalrus(steps.GetTuna(steps.getWendesday()));
        }
        if (isItNull(steps.getThursday())){
            thursdayStep.setText(getString(R.string.NoStep));
            thursdayRating.setText("-%");
        }
        else{
            thursdayRating.setText(String.valueOf(steps.getTuesday()));
            thursdayStep.setText(String.valueOf(steps.GetTuna(steps.getThursday())) + "%");
            GetWalrus(steps.GetTuna(steps.getThursday()));
        }
        if (isItNull(steps.getFriday())){
            firdayStep.setText(getString(R.string.NoStep));
            fridayRating.setText("-%");
        }
        else{
            firdayStep.setText(String.valueOf(steps.getFriday()));
            fridayRating.setText(String.valueOf(steps.GetTuna(steps.getFriday())) + "%");
            GetWalrus(steps.GetTuna(steps.getFriday()));
        }
        if (isItNull(steps.getSaturday())){
            saturdayStep.setText(getString(R.string.NoStep));
            saturdayRating.setText("-%");
        }
        else{
            saturdayStep.setText(String.valueOf(steps.getSaturday()));
            saturdayRating.setText(String.valueOf(steps.GetTuna(steps.getSaturday())) + "%");
            GetWalrus(steps.GetTuna(steps.getSaturday()));
        }
        if (isItNull(steps.getSunday())){
            sundaySteps.setText(getString(R.string.NoStep));
            sundayRating.setText("-%");
        }
        else{
            sundaySteps.setText(String.valueOf(steps.getSunday()));
            sundayRating.setText(String.valueOf(steps.GetTuna(steps.getSunday())) + "%");
            GetWalrus(steps.GetTuna(steps.getSunday()));
        }
    }

    private void getWeek() {
        steps.setMonday(prefs.getInt("mon", -1));
        steps.setTuesday(prefs.getInt("tue", -1));
        steps.setWendesday(prefs.getInt("wen", -1));
        steps.setThursday(prefs.getInt("thu", -1));
        steps.setFriday(prefs.getInt("fri", -1));
        steps.setSaturday(prefs.getInt("sat", -1));
        steps.setSunday(prefs.getInt("sun", -1));
    }

    private boolean isItNull(int steps){
        if (steps == -1){
            return true;
        }
        return false;
    }

    private void GetWalrus(int rating){
        if (rating >= 100){
            prefs.edit().putBoolean("walrus", true).apply();
        }
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
        goal = findViewById(R.id.setgoal);
        //steps
        mondayStep = findViewById(R.id.MondayStep);
        tuesdayStep = findViewById(R.id.TuesdayStep);
        wendesdayStep = findViewById(R.id.WednesdayStep);
        thursdayStep = findViewById(R.id.ThursdayStep);
        firdayStep = findViewById(R.id.FridayStep);
        saturdayStep = findViewById(R.id.SaturdayStep);
        sundaySteps = findViewById(R.id.SundayStep);
        //ratings
        mondayRating = findViewById(R.id.MondayRating);
        tuesdayRating = findViewById(R.id.TuesdayRating);
        wendesdayRating = findViewById(R.id.WednesdayRating);
        thursdayRating = findViewById(R.id.ThursdayRating);
        fridayRating = findViewById(R.id.FridayRating);
        saturdayRating = findViewById(R.id.SaturdayRating);
        sundayRating = findViewById(R.id.SundayRating);
    }
}