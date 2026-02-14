package com.example.sealstep;

import retrofit2.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class API {
    private WeatherAPI weatherAPI;

    public interface WeatherCallback {
        void onSuccess(Weather weather);
        void onError(String error);
    }

    public API() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.open-meteo.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        weatherAPI = retrofit.create(WeatherAPI.class);
    }

    public void fetchWeather(double lat, double lon, WeatherCallback callback) {

        Call<Weather> call = weatherAPI.getForecast(lat, lon);

        call.enqueue(new Callback<Weather>() {
            @Override
            public void onResponse(Call<Weather> call, Response<Weather> response) {

                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Response error");
                }
            }

            @Override
            public void onFailure(Call<Weather> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }
}
