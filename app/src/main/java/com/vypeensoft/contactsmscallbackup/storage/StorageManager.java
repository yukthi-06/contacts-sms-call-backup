package com.vypeensoft.contactsmscallbackup.storage;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vypeensoft.contactsmscallbackup.utils.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class StorageManager {
    public static final String DEST_INTERNAL = "internal";
    public static final String DEST_EXTERNAL = "external";

    private static final String SETTINGS_FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vypeensoft/Contact_SMS_CallLogs/settings/settings.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static StorageManager instance;
    private final Context context;
    public SettingsData settingsData;

    public static class SettingsData {
        public String backupDestType = DEST_INTERNAL;
        public String backupExternalPath = null;
    }

    private StorageManager(Context context) {
        this.context = context.getApplicationContext();
        loadSettings();
    }

    public static synchronized StorageManager getInstance(Context context) {
        if (instance == null) {
            instance = new StorageManager(context);
        }
        return instance;
    }

    public void loadSettings() {
        File file = new File(SETTINGS_FILE_PATH);
        if (!file.exists()) {
            this.settingsData = new SettingsData();
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            this.settingsData = gson.fromJson(reader, SettingsData.class);
            if (this.settingsData == null) {
                this.settingsData = new SettingsData();
            }
        } catch (Exception e) {
            Logger.e("Failed to load settings from JSON file", e);
            this.settingsData = new SettingsData();
        }
    }

    public void saveSettings() {
        File file = new File(SETTINGS_FILE_PATH);
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        try (FileWriter writer = new FileWriter(file)) {
            if (!DEST_EXTERNAL.equals(getDestinationType())) {
                File internalDir = context.getExternalFilesDir(null);
                if (internalDir == null) {
                    internalDir = context.getFilesDir();
                }
                settingsData.backupExternalPath = internalDir.getAbsolutePath();
            }
            gson.toJson(this.settingsData, writer);
            Logger.i("Saved settings successfully to " + SETTINGS_FILE_PATH);
        } catch (Exception e) {
            Logger.e("Failed to save settings to JSON file", e);
        }
    }

    public String getDestinationType() {
        return settingsData.backupDestType;
    }

    public void setDestinationType(String type) {
        settingsData.backupDestType = type;
    }

    public String getExternalPath() {
        return settingsData.backupExternalPath;
    }

    public void setExternalPath(String path) {
        settingsData.backupExternalPath = path;
    }

    public void persistUriPermission(Uri uri) {
        try {
            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
            
            String decoded = Uri.decode(uri.toString());
            String prefix = "content://com.android.externalstorage.documents/tree/primary:";
            String finalPath;
            if (decoded.startsWith(prefix)) {
                finalPath = decoded.replace(prefix, "/sdcard/");
            } else {
                finalPath = decoded;
            }
            
            setExternalPath(finalPath);
            setDestinationType(DEST_EXTERNAL);
            Logger.i("Persisted SAF directory path: " + finalPath);
        } catch (Exception e) {
            Logger.e("Failed to persist SAF URI permission", e);
        }
    }

    public String getBackupFolderDescription() {
        if (DEST_EXTERNAL.equals(getDestinationType()) && getExternalPath() != null) {
            return getExternalPath();
        }
        File internalDir = context.getExternalFilesDir(null);
        if (internalDir == null) {
            internalDir = context.getFilesDir();
        }
        return internalDir.getAbsolutePath();
    }

    public String writeBackupFile(String category, String filename, String content) throws IOException {
        loadSettings();
        String destType = getDestinationType();
        File backupDir;

        if (DEST_EXTERNAL.equals(destType) && getExternalPath() != null) {
            File root = new File(getExternalPath());
            backupDir = new File(root, category);
        } else {
            File root = context.getExternalFilesDir(null);
            if (root == null) {
                root = context.getFilesDir();
            }
            backupDir = new File(root, category);
        }

        if (!backupDir.exists()) {
            if (!backupDir.mkdirs()) {
                throw new IOException("Could not create directories: " + backupDir.getAbsolutePath());
            }
        }

        File file = new File(backupDir, filename);
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            writer.write(content);
            writer.flush();
            return file.getAbsolutePath();
        }
    }

    public List<String> listBackupFiles() {
        loadSettings();
        List<String> results = new ArrayList<>();
        
        // Scan internal folder
        File root = context.getExternalFilesDir(null);
        if (root == null) root = context.getFilesDir();
        if (root.exists()) {
            scanInternalFolder(root, results);
        }

        // Scan external folder using File API
        if (DEST_EXTERNAL.equals(getDestinationType()) && getExternalPath() != null) {
            File extRoot = new File(getExternalPath());
            if (extRoot.exists()) {
                scanInternalFolder(extRoot, results);
            }
        }
        return results;
    }

    private void scanInternalFolder(File dir, List<String> results) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanInternalFolder(file, results);
                } else {
                    results.add(file.getAbsolutePath());
                }
            }
        }
    }
}
