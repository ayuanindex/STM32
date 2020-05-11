package com.realmax.stm32.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.realmax.stm32.App;

/**
 * @ProjectName: Cars
 * @Package: com.realmax.cars.utils
 * @ClassName: SpUtil
 * @CreateDate: 2020/3/16 12:34
 */
@SuppressLint("ApplySharedPref")
public class SpUtil {
    private static SharedPreferences sharedPreferences = null;

    private static SharedPreferences getSP() {
        if (sharedPreferences == null) {
            sharedPreferences = App.getContext().getSharedPreferences("setting", Context.MODE_PRIVATE);
        }
        return sharedPreferences;
    }

    public static void putString(String key, String value) {
        getSP().edit().putString(key, value).commit();
    }

    public static String getString(String key, String defVal) {
        return getSP().getString(key, defVal);
    }

    public static void putBoolean(String key, boolean value) {
        getSP().edit().putBoolean(key, value).commit();
    }

    public static boolean getBoolean(String key, boolean defVal) {
        return getSP().getBoolean(key, defVal);
    }

    public static void putInt(String key, int value) {
        getSP().edit().putInt(key, value).commit();
    }

    public static int getInt(String key, int defVal) {
        return getSP().getInt(key, defVal);
    }
}
