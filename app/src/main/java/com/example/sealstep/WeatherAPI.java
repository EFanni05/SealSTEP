package com.example.sealstep;
//for fetch
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherAPI {
    @GET("forecast?current=is_day,weather_code&timezone=auto")
    Call<Weather> getForecast(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude
    );
}
