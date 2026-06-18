package com.vypeensoft.contactsmscallbackup.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BackupResult implements Serializable {
    private String backupType; // Contacts, SMS, Call Logs, All
    private long timestamp;
    private boolean success;
    private List<String> filePaths = new ArrayList<>();
    private int itemCount;
    private String message;

    public BackupResult() {}

    public BackupResult(String backupType, long timestamp, boolean success, List<String> filePaths, int itemCount, String message) {
        this.backupType = backupType;
        this.timestamp = timestamp;
        this.success = success;
        this.filePaths = filePaths;
        this.itemCount = itemCount;
        this.message = message;
    }

    public String getBackupType() {
        return backupType != null ? backupType : "";
    }

    public void setBackupType(String backupType) {
        this.backupType = backupType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<String> getFilePaths() {
        if (filePaths == null) {
            filePaths = new ArrayList<>();
        }
        return filePaths;
    }

    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public String getMessage() {
        return message != null ? message : "";
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
