package com.mrtan.qiniu_push.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Cache {
    public static void saveURL(Context context, String url) {
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
        e.putString("URL", url);
        e.commit();
    }

    public static String retrieveURL(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("URL", "");
    }

    public static void setAudioFile(Context context, String audioFile) {
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
        e.putString("audioFile", audioFile);
        e.commit();
    }

    public static String getAudioFile(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("audioFile", null);
    }
}
