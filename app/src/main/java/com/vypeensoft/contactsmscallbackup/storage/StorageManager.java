package com.vypeensoft.contactsmscallbackup.storage;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import androidx.documentfile.provider.DocumentFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vypeensoft.contactsmscallbackup.utils.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
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
    private SettingsData settingsData;

    public static class SettingsData {
        public String backupDestType = DEST_INTERNAL;
        public String backupExternalUri = null;
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

    private void loadSettings() {
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

    public String getExternalUri() {
        return settingsData.backupExternalUri;
    }

    public void setExternalUri(String uri) {
        settingsData.backupExternalUri = uri;
    }

    public void persistUriPermission(Uri uri) {
        try {
            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
            setExternalUri(uri.toString());
            setDestinationType(DEST_EXTERNAL);
            Logger.i("Persisted SAF URI permission: " + uri);
        } catch (Exception e) {
            Logger.e("Failed to persist SAF URI permission", e);
        }
    }

    public String getBackupFolderDescription() {
        if (DEST_EXTERNAL.equals(getDestinationType())) {
            String uriStr = getExternalUri();
            if (uriStr != null) {
                String decoded = Uri.decode(uriStr);
                String prefix = "content://com.android.externalstorage.documents/tree/primary:";
                if (decoded.startsWith(prefix)) {
                    return "External SAF Path: " + decoded.replace(prefix, "/sdcard/");
                }
                return "External SAF Path: " + decoded;
            }
            return "External SAF: (Selected folder unavailable, fallback to internal)";
        }
        File internalDir = context.getExternalFilesDir(null);
        if (internalDir == null) {
            internalDir = context.getFilesDir();
        }
        return "Internal Storage: " + internalDir.getAbsolutePath() + "/BackupManager";
    }

    public String writeBackupFile(String category, String filename, String content) throws IOException {
        // Ensure settings are loaded fresh
        loadSettings();
        String destType = getDestinationType();
        if (DEST_EXTERNAL.equals(destType)) {
            String uriStr = getExternalUri();
            if (uriStr != null) {
                Uri treeUri = Uri.parse(uriStr);
                DocumentFile treeRoot = DocumentFile.fromTreeUri(context, treeUri);
                if (treeRoot != null && treeRoot.exists()) {
                    DocumentFile managerDir = treeRoot.findFile("BackupManager");
                    if (managerDir == null) {
                        managerDir = treeRoot.createDirectory("BackupManager");
                    }
                    if (managerDir != null) {
                        DocumentFile categoryDir = managerDir.findFile(category);
                        if (categoryDir == null) {
                            categoryDir = managerDir.createDirectory(category);
                        }
                        if (categoryDir != null) {
                            String mimeType = "text/plain";
                            if (filename.endsWith(".json")) mimeType = "application/json";
                            else if (filename.endsWith(".xml")) mimeType = "text/xml";
                            else if (filename.endsWith(".csv")) mimeType = "text/comma-separated-values";

                            DocumentFile backupFile = categoryDir.findFile(filename);
                            if (backupFile == null) {
                                backupFile = categoryDir.createFile(mimeType, filename);
                            }
                            if (backupFile != null) {
                                try (OutputStream out = context.getContentResolver().openOutputStream(backupFile.getUri());
                                     OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                                    writer.write(content);
                                    writer.flush();
                                    return backupFile.getUri().toString();
                                }
                            }
                        }
                    }
                }
            }
            throw new IOException("External SAF storage folder is unavailable.");
        } else {
            File root = context.getExternalFilesDir(null);
            if (root == null) {
                root = context.getFilesDir();
            }
            File backupDir = new File(root, "BackupManager/" + category);
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
    }

    public List<String> listBackupFiles() {
        loadSettings();
        List<String> results = new ArrayList<>();
        File root = context.getExternalFilesDir(null);
        if (root == null) root = context.getFilesDir();
        File managerDir = new File(root, "BackupManager");
        if (managerDir.exists()) {
            scanInternalFolder(managerDir, results);
        }

        String uriStr = getExternalUri();
        if (uriStr != null) {
            try {
                Uri treeUri = Uri.parse(uriStr);
                DocumentFile treeRoot = DocumentFile.fromTreeUri(context, treeUri);
                if (treeRoot != null && treeRoot.exists()) {
                    DocumentFile safManager = treeRoot.findFile("BackupManager");
                    if (safManager != null && safManager.isDirectory()) {
                        scanDocumentFolder(safManager, results);
                    }
                }
            } catch (Exception e) {
                Logger.e("Error scanning SAF folder", e);
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

    private void scanDocumentFolder(DocumentFile dir, List<String> results) {
        DocumentFile[] files = dir.listFiles();
        if (files != null) {
            for (DocumentFile file : files) {
                if (file.isDirectory()) {
                    scanDocumentFolder(file, results);
                } else {
                    results.add(file.getName() + " (Tree: " + file.getUri().toString() + ")");
                }
            }
        }
    }
}
