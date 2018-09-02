package com.harispraba.voicediary.config;

import android.os.Environment;

import java.io.File;

public class AppConfig {
    private String localDir;
    private Boolean isStorageWriteable;

    public AppConfig() {
        if(isExternalStorageWritable()){
            localDir = Environment.getExternalStorageDirectory() + File.separator + "VoiceDiary";
            isStorageWriteable = true;
        }
        else{
            localDir = "";
            isStorageWriteable = false;
        }
    }

    public String getLocalDir() {
        return localDir;
    }

    public Boolean getStorageWriteable() {
        return isStorageWriteable;
    }

    private Boolean isExternalStorageWritable(){
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
