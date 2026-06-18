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
import com.vypeensoft.contactsmscallbackup.services.CallLogBackupService;
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

public class CallLogBackupWorker extends Worker {

    private static final String CHANNEL_ID = "BackupServiceChannel";
    private static final int NOTIFICATION_ID = 1003;

    public CallLogBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Logger.logBackupStart("CallLogs");
        createNotificationChannel();
        setForegroundAsync(createForegroundInfo("Preparing Call Log Backup...", 0));

        Context context = getApplicationContext();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Logger.logBackupFailure("CallLogs", "READ_CALL_LOG permission not granted");
            showFinalNotification("Backup Failed", "Missing Call Log permission");
            return Result.failure();
        }

        boolean backupCsv = getInputData().getBoolean("format_csv", true);
        boolean backupJson = getInputData().getBoolean("format_json", false);
        boolean backupXml = getInputData().getBoolean("format_xml", false);

        if (!backupCsv && !backupJson && !backupXml) {
            Logger.logBackupFailure("CallLogs", "No backup format selected");
            showFinalNotification("Backup Failed", "No format selected");
            return Result.failure();
        }

        try {
            setForegroundAsync(createForegroundInfo("Reading call history...", 30));
            CallLogBackupService service = new CallLogBackupService();
            List<CallLogModel> callLogs = service.readCallLogs(context);

            if (callLogs.isEmpty()) {
                Logger.logBackupFailure("CallLogs", "No call logs found on device");
                showFinalNotification("Backup Completed", "No call logs to backup");
                return Result.success(new Data.Builder().putString("message", "No call logs to backup").build());
            }

            setForegroundAsync(createForegroundInfo("Serializing call history...", 60));
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            StorageManager storage = StorageManager.getInstance(context);
            List<String> savedPaths = new ArrayList<>();

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

            setForegroundAsync(createForegroundInfo("Finishing backup...", 90));
            Logger.logBackupSuccess("CallLogs", callLogs.size(), savedPaths.toString());
            showFinalNotification("Backup Completed", "Successfully backed up " + callLogs.size() + " call logs");

            Data outputData = new Data.Builder()
                    .putString("type", "CallLogs")
                    .putBoolean("success", true)
                    .putInt("count", callLogs.size())
                    .putStringArray("paths", savedPaths.toArray(new String[0]))
                    .build();

            return Result.success(outputData);

        } catch (Exception e) {
            Logger.logBackupFailure("CallLogs", e.getMessage());
            showFinalNotification("Backup Failed", e.getMessage());
            return Result.failure(new Data.Builder().putString("error", e.getMessage()).build());
        }
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
                .setContentTitle("Backing up Call Logs")
                .setContentText(text)
                .setOngoing(true)
                .setProgress(100, progress, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new ForegroundInfo(NOTIFICATION_ID, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
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
