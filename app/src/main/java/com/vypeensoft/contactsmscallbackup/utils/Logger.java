package com.vypeensoft.contactsmscallbackup.utils;

import android.util.Log;

public class Logger {
    private static final String TAG = "BackupManager";

    public static void i(String message) {
        Log.i(TAG, message);
    }

    public static void d(String message) {
        Log.d(TAG, message);
    }

    public static void w(String message) {
        Log.w(TAG, message);
    }

    public static void e(String message) {
        Log.e(TAG, message);
    }

    public static void e(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
    }

    public static void logBackupStart(String type) {
        i("Backup STARTED for type: " + type);
    }

    public static void logBackupSuccess(String type, int count, String path) {
        i("Backup SUCCESS for type: " + type + ". Items backed up: " + count + ". Path: " + path);
    }

    public static void logBackupFailure(String type, String error) {
        e("Backup FAILED for type: " + type + ". Error: " + error);
    }

    public static void logPermissionStatus(String permission, boolean granted) {
        i("Permission: " + permission + " is " + (granted ? "GRANTED" : "DENIED"));
    }
}
