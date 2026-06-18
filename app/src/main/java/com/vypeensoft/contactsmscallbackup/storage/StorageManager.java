package com.vypeensoft.contactsmscallbackup.storage;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import androidx.documentfile.provider.DocumentFile;

import com.vypeensoft.contactsmscallbackup.utils.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class StorageManager {
    private static final String PREFS_NAME = "BackupPrefs";
    private static final String KEY_DEST_TYPE = "backup_dest_type"; // "internal" or "external"
    private static final String KEY_EXTERNAL_URI = "backup_external_uri";
    
    public static final String DEST_INTERNAL = "internal";
    public static final String DEST_EXTERNAL = "external";

    private static StorageManager instance;
    private final Context context;
    private final SharedPreferences prefs;

    private StorageManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized StorageManager getInstance(Context context) {
        if (instance == null) {
            instance = new StorageManager(context);
        }
        return instance;
    }

    public String getDestinationType() {
        return prefs.getString(KEY_DEST_TYPE, DEST_INTERNAL);
    }

    public void setDestinationType(String type) {
        prefs.edit().putString(KEY_DEST_TYPE, type).apply();
    }

    public String getExternalUri() {
        return prefs.getString(KEY_EXTERNAL_URI, null);
    }

    public void setExternalUri(String uri) {
        prefs.edit().putString(KEY_EXTERNAL_URI, uri).apply();
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
                Uri uri = Uri.parse(uriStr);
                DocumentFile folder = DocumentFile.fromTreeUri(context, uri);
                if (folder != null && folder.exists()) {
                    return "External SAF: " + folder.getName();
                }
            }
            return "External SAF: (Selected folder unavailable, fallback to internal)";
        }
        File internalDir = context.getExternalFilesDir(null);
        if (internalDir == null) {
            internalDir = context.getFilesDir();
        }
        return "Internal Storage: " + internalDir.getAbsolutePath() + "/BackupManager";
    }

    /**
     * Writes backup file to the selected storage type under category folder (Contacts, SMS, CallLogs).
     * @return Path description of the created file, or null if writing fails.
     */
    public String writeBackupFile(String category, String filename, String content) throws IOException {
        String destType = getDestinationType();
        if (DEST_EXTERNAL.equals(destType)) {
            String uriStr = getExternalUri();
            if (uriStr != null) {
                Uri treeUri = Uri.parse(uriStr);
                DocumentFile treeRoot = DocumentFile.fromTreeUri(context, treeUri);
                if (treeRoot != null && treeRoot.exists()) {
                    // Create BackupManager/ folder if not exists
                    DocumentFile managerDir = treeRoot.findFile("BackupManager");
                    if (managerDir == null) {
                        managerDir = treeRoot.createDirectory("BackupManager");
                    }
                    if (managerDir != null) {
                        // Create Category/ folder if not exists
                        DocumentFile categoryDir = managerDir.findFile(category);
                        if (categoryDir == null) {
                            categoryDir = managerDir.createDirectory(category);
                        }
                        if (categoryDir != null) {
                            // Determine MIME type
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
            // If external failed, fall back or throw exception
            throw new IOException("External SAF storage folder is unavailable.");
        } else {
            // Write to Internal App storage
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
            try (FileOutputStream out = new FileOutputStream(file);
                 OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                writer.write(content);
                writer.flush();
                return file.getAbsolutePath();
            }
        }
    }

    public List<String> listBackupFiles() {
        List<String> results = new ArrayList<>();
        // 1. List internal files
        File root = context.getExternalFilesDir(null);
        if (root == null) root = context.getFilesDir();
        File managerDir = new File(root, "BackupManager");
        if (managerDir.exists()) {
            scanInternalFolder(managerDir, results);
        }

        // 2. List external SAF files if available
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
