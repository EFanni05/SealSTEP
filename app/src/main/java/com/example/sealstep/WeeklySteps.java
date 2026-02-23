package com.example.sealstep;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;

public class WeeklySteps {
    //steps
    private int Monday = 0;
    private int Tuesday = 0;
    private int Wendesday = 0;
    private int Thursday = 0;
    private int Friday = 0;
    private int Saturday = 0;
    private int Sunday = 0;
    private int goal = 5000;


    //Tuna
    public int GetTuna(int steps){
        float f = (steps / goal) * 100;
        return Math.round(f);
    }

    //reset
    public void ResetWeek(){
        Monday = -1;
        Tuesday = -1;
        Wendesday = -1;
        Thursday = -1;
        Friday = -1;
        Saturday = -1;
        Sunday = -1;
    }

    //monday
    public int getMonday() {
        return Monday;
    }
    public void setMonday(int monday) {
        Monday = monday;
    }

    //tuesday
    public int getTuesday() {
        return Tuesday;
    }
    public void setTuesday(int tuesday) {
        Tuesday = tuesday;
    }

    //wednesday
    public int getWendesday() {
        return Wendesday;
    }
    public void setWendesday(int wendesday) {
        Wendesday = wendesday;
    }

    //thursday
    public int getThursday() {
        return Thursday;
    }
    public void setThursday(int thursday) {
        Thursday = thursday;
    }

    //friday
    public int getFriday() {
        return Friday;
    }
    public void setFriday(int friday) {
        Friday = friday;
    }

    //saturday
    public int getSaturday() {
        return Saturday;
    }
    public void setSaturday(int saturday) {
        Saturday = saturday;
    }

    //sunday
    public int getSunday() {
        return Sunday;
    }
    public void setSunday(int sunday) {
        Sunday = sunday;
    }

    //goal
    public int getGoal() {
        return goal;
    }
    public void setGoal(int goal) {
        if (goal > 0){
            this.goal = goal;
        }
    }
}
