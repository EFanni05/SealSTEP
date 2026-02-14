package com.example.sealstep;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.location.Geocoder;
import android.location.Address;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<Intent> settingsLauncher;
    private MediaPlayer player;
    FrameLayout back;
    FrameLayout sound;
    ImageView soundPic;
    SealVariables sealVariables = new SealVariables();
    ImageView github;
    ImageView youtube;

    TextView addressView;
    double latitude;
    double longitude;
    //and add the dropdown
    String addresstext;

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
            List<Address> addresses =
                    geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                //get the current address
                Address address = addresses.get(0);
                //for the textview
                String city = address.getLocality();
                String street = address.getThoroughfare();
                String country = address.getCountryName();
                addresstext = street + ", " + city + "\n" + country;
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
        back = findViewById(R.id.backbutton);
        sound = findViewById(R.id.soundbutton);
        soundPic = findViewById(R.id.soundpic);
        github = findViewById(R.id.github);
        youtube = findViewById(R.id.youtube);
        addressView = findViewById(R.id.address);
        //dropdown
        player = MediaPlayer.create(this, R.raw.kk_bashment);
        player.setLooping(true);
        player.start();
    }
}