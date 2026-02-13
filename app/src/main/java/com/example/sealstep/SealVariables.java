package com.example.sealstep;

public class SealVariables {
    private int stepcount = 0;
    private int fishcount = 0;
    private boolean sound = true;
    private String lang;

   //gameplay variables
    private double hunger = 4;

    //props

    public void setFishcount(int fishcount) {
        this.fishcount = fishcount;
    }

    public int getFishcount() {
        return fishcount;
    }

    public void setStepcount(int stepcount) {
        this.stepcount = stepcount;
    }

    public int getStepcount() {
        return stepcount;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getLang(String lang) {
        return this.lang;
    }

    public void setHunger(double hunger) {
        this.hunger = hunger;
    }

    public double getHunger() {
        return hunger;
    }

    public void setSound(boolean sound) {
        this.sound = sound;
    }

    public boolean isSound() {
        return sound;
    }
}
