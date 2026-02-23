package com.example.sealstep;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.location.Geocoder;
import android.location.Address;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private static final int LOCATION_PERMISSION_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private MediaPlayer player;
    TextView settingText;
    FrameLayout back;
    FrameLayout sound;
    ImageView soundPic;
    SealVariables sealVariables = new SealVariables();
    ImageView github;
    ImageView youtube;
    AutoCompleteTextView dropdown;
    TextView addressView;
    String[] codes = {
            "hu",
            "en"
    };
    String currentLang;
    double latitude;
    double longitude;
    String addresstext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs  = getSharedPreferences("App_Pref", MODE_PRIVATE);
        loadLanguage();
        applySavedLanguage();
        sealVariables.setSound(prefs.getBoolean("sound", true));
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        init();
        if (sealVariables.isSound()){
            player.start();
        }
        else{
            player.pause();
        }
        if (currentLang.equals("hu")){
            ResizingHU();
        }
        String[] dropdownMenu = {
                getString(R.string.ENemoji),
                getString(R.string.HUemoji)
                //and any more of the language
        };
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //not given perms
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        } else {
            getLocation();
        }

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                player.pause();
                startActivity(intent);
                finish();
            }
        });
        sound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor e = prefs.edit();
                if (sealVariables.isSound()){
                    sealVariables.setSound(false);
                    soundPic.setImageResource(R.drawable.mute);
                    player.pause();
                    e.putBoolean("sound", false);
                }
                else{
                    sealVariables.setSound(true);
                    soundPic.setImageResource(R.drawable.sound);
                    player.start();
                    e.putBoolean("sound", true);
                }
                e.apply();
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
        //dropdown
        dropdown.setText(dropdownMenu[findLang()], false);
        ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, dropdownMenu
        );
        dropdown.setAdapter(dropdownAdapter);
        dropdown.setThreshold(Integer.MAX_VALUE);
        dropdown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedCode = dropdownMenu[position];
                saveLanguage(codes[position]);
                setLang(codes[position]);
                recreate();
            }
        });
    }

    private int findLang(){
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String savedCode = prefs.getString("app_language", "en");
        for (int i = 0; i < codes.length; i++) {
            if (savedCode.equals(codes[i])){
                return i;
            }
        }
        return -1;
    }

    private void setLang(String select){
        if (!select.isEmpty()){
            SharedPreferences pref = getSharedPreferences("Settings", MODE_PRIVATE);
            //now te setting it
            SharedPreferences.Editor edit = pref.edit();
            edit.putString("app_language", select);
            edit.apply();
        }
        else{
            Toast.makeText(this, getString(R.string.errorInLang), Toast.LENGTH_SHORT).show();
        }
    }
    private void loadLanguage() {

        SharedPreferences prefs =
                getSharedPreferences("Settings", MODE_PRIVATE);
        String languageCode =
                prefs.getString("app_language", "en"); // default
        setLang(languageCode);
    }
    private void saveLanguage(String select) {
        Locale l = new Locale(select);
        Locale.setDefault(l);
        Configuration config = new Configuration();
        config.setLocale(l);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void applySavedLanguage() {

        SharedPreferences prefs =
                getSharedPreferences("Settings", MODE_PRIVATE);

        String languageCode =
                prefs.getString("app_language", "en");

        LocaleListCompat appLocale =
                LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocale);
        currentLang = languageCode;
    }

    public void ResizingHU(){
        settingText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 39);
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
        else{
            addresstext = String.valueOf(R.string.noData);
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

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        getAddress(latitude, longitude);
                    }
                });
    }

    private void getAddress(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            //getting the address
            List<Address> addresses =
                    geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                //get the current address
                Address address = addresses.get(0);
                //for the textview
                String city = address.getLocality();
                String street = address.getThoroughfare();
                String country = address.getCountryName();
                //no street name
                //TODO: fix this
                if (street.isEmpty()){
                    addresstext = city + ", " + country;
                } else if (street.isEmpty() && city.isEmpty()) {
                    //on highway or forest type of shit
                    addresstext = getString(R.string.somewhere)+ ", " + country;
                }
                else{
                    //base
                    addresstext = street + ", " + city + "\n" + country;
                }
                //for testing
                Log.d("ADDRESS", "City: " + city);
                Log.d("ADDRESS", "Street: " + street);
                Log.d("ADDRESS", "Country: " + country);
                Log.d("ADDRESS", "getAddress: " + addresstext);

                //set address on view
                addressView.setText(addresstext);
            }

        } catch (Exception e) {
            //unavailable data
            addresstext = String.valueOf(R.string.noData);
            addressView.setText(addresstext);
            e.printStackTrace();
        }
    }
    private void init(){
        settingText = findViewById(R.id.settingtext);
        back = findViewById(R.id.backbutton);
        sound = findViewById(R.id.soundbutton);
        soundPic = findViewById(R.id.soundpic);
        if (sealVariables.isSound()){
            soundPic.setImageResource(R.drawable.sound);
        }
        else{
            soundPic.setImageResource(R.drawable.mute);
        }
        github = findViewById(R.id.github);
        youtube = findViewById(R.id.youtube);
        addressView = findViewById(R.id.address);
        //dropdown
        player = MediaPlayer.create(this, R.raw.kk_bashment);
        player.setLooping(true);
        player.start();
        dropdown = findViewById(R.id.dropdownInner);
    }
}