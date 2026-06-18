package com.vypeensoft.contactsmscallbackup.workers;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.vypeensoft.contactsmscallbackup.models.CallLogModel;
import com.vypeensoft.contactsmscallbackup.models.ContactModel;
import com.vypeensoft.contactsmscallbackup.models.SmsModel;
import com.vypeensoft.contactsmscallbackup.services.CallLogBackupService;
import com.vypeensoft.contactsmscallbackup.services.ContactBackupService;
import com.vypeensoft.contactsmscallbackup.services.SmsBackupService;
import com.vypeensoft.contactsmscallbackup.storage.StorageManager;
import com.vypeensoft.contactsmscallbackup.utils.CsvExportUtil;
import com.vypeensoft.contactsmscallbackup.utils.JsonExportUtil;
import com.vypeensoft.contactsmscallbackup.utils.Logger;
import com.vypeensoft.contactsmscallbackup.utils.XmlExportUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FullBackupWorker extends Worker {

    private static final String CHANNEL_ID = "BackupServiceChannel";
    private static final int NOTIFICATION_ID = 1004;

    public FullBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Logger.logBackupStart("FullBackup");
        createNotificationChannel();
        setForegroundAsync(createForegroundInfo("Preparing Full Backup...", 0));

        Context context = getApplicationContext();
        boolean backupCsv = getInputData().getBoolean("format_csv", true);
        boolean backupJson = getInputData().getBoolean("format_json", false);
        boolean backupXml = getInputData().getBoolean("format_xml", false);

        if (!backupCsv && !backupJson && !backupXml) {
            Logger.logBackupFailure("FullBackup", "No backup format selected");
            showFinalNotification("Full Backup Failed", "No format selected");
            return Result.failure();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        StorageManager storage = StorageManager.getInstance(context);
        List<String> savedPaths = new ArrayList<>();
        int totalItemsBackedUp = 0;

        // 1. Contacts Backup
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            try {
                setForegroundAsync(createForegroundInfo("Backing up Contacts...", 15));
                ContactBackupService service = new ContactBackupService();
                List<ContactModel> contacts = service.readContacts(context);
                if (!contacts.isEmpty()) {
                    if (backupCsv) {
                        String csv = CsvExportUtil.exportContacts(contacts);
                        String path = storage.writeBackupFile("Contacts", "contacts_" + timeStamp + ".csv", csv);
                        if (path != null) savedPaths.add(path);
                    }
                    if (backupJson) {
                        String json = JsonExportUtil.exportToJson(contacts);
                        String path = storage.writeBackupFile("Contacts", "contacts_" + timeStamp + ".json", json);
                        if (path != null) savedPaths.add(path);
                    }
                    if (backupXml) {
                        String xml = XmlExportUtil.exportContacts(contacts);
                        String path = storage.writeBackupFile("Contacts", "contacts_" + timeStamp + ".xml", xml);
                        if (path != null) savedPaths.add(path);
                    }
                    totalItemsBackedUp += contacts.size();
                }
            } catch (Exception e) {
                Logger.e("Error backing up contacts during full backup", e);
            }
        }

        // 2. SMS Backup
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                setForegroundAsync(createForegroundInfo("Backing up SMS...", 50));
                SmsBackupService service = new SmsBackupService();
                List<SmsModel> smsList = service.readSms(context);
                if (!smsList.isEmpty()) {
                    if (backupCsv) {
                        String csv = CsvExportUtil.exportSms(smsList);
                        String path = storage.writeBackupFile("SMS", "sms_" + timeStamp + ".csv", csv);
                        if (path != null) savedPaths.add(path);
                    }
                    if (backupJson) {
                        String json = JsonExportUtil.exportToJson(smsList);
                        String path = storage.writeBackupFile("SMS", "sms_" + timeStamp + ".json", json);
                        if (path != null) savedPaths.add(path);
                    }
                    if (backupXml) {
                        String xml = XmlExportUtil.exportSms(smsList);
                        String path = storage.writeBackupFile("SMS", "sms_" + timeStamp + ".xml", xml);
                        if (path != null) savedPaths.add(path);
                    }
                    totalItemsBackedUp += smsList.size();
                }
            } catch (Exception e) {
                Logger.e("Error backing up SMS during full backup", e);
            }
        }

        // 3. Call Log Backup
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            try {
                setForegroundAsync(createForegroundInfo("Backing up Call Logs...", 80));
                CallLogBackupService service = new CallLogBackupService();
                List<CallLogModel> callLogs = service.readCallLogs(context);
                if (!callLogs.isEmpty()) {
                    if (backupCsv) {
                        String csv = CsvExportUtil.exportCallLogs(callLogs);
                        String path = storage.writeBackupFile("CallLogs", "calllogs_" + timeStamp + ".csv", csv);
                        if (path != null) savedPaths.add(path);
                    }
                    if (backupJson) {
                        String json = JsonExportUtil.exportToJson(callLogs);
                        String path = storage.writeBackupFile("CallLogs", "calllogs_" + timeStamp + ".json", json);
                        if (path != null) savedPaths.add(path);
                    }
                    if (backupXml) {
                        String xml = XmlExportUtil.exportCallLogs(callLogs);
                        String path = storage.writeBackupFile("CallLogs", "calllogs_" + timeStamp + ".xml", xml);
                        if (path != null) savedPaths.add(path);
                    }
                    totalItemsBackedUp += callLogs.size();
                }
            } catch (Exception e) {
                Logger.e("Error backing up call logs during full backup", e);
            }
        }

        setForegroundAsync(createForegroundInfo("Finishing Full Backup...", 95));
        Logger.logBackupSuccess("FullBackup", totalItemsBackedUp, savedPaths.toString());
        showFinalNotification("Full Backup Completed", "Backed up " + totalItemsBackedUp + " total items successfully");

        Data outputData = new Data.Builder()
                .putString("type", "Full")
                .putBoolean("success", true)
                .putInt("count", totalItemsBackedUp)
                .putStringArray("paths", savedPaths.toArray(new String[0]))
                .build();

        return Result.success(outputData);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Backup Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private ForegroundInfo createForegroundInfo(String text, int progress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Running Full Backup")
                .setContentText(text)
                .setOngoing(true)
                .setProgress(100, progress, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new ForegroundInfo(NOTIFICATION_ID, builder.build(), android.content.pm.ActivityInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            return new ForegroundInfo(NOTIFICATION_ID, builder.build());
        }
    }

    private void showFinalNotification(String title, String content) {
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setOngoing(false)
                    .setAutoCancel(true);
            manager.notify(NOTIFICATION_ID + 10, builder.build());
        }
    }
}
